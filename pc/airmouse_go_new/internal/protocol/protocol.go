package protocol

import (
	"fmt"
	"sync"
	"time"

	"airmouse-go/control/mouse"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol/bluetooth"
	"airmouse-go/internal/protocol/tcp"
	"airmouse-go/internal/protocol/udp"
	"airmouse-go/internal/protocol/usb"
	websocketpkg "airmouse-go/internal/protocol/websocket"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/utils"
)

// Client represents a connected client (legacy, kept for backward compatibility).
type Client struct {
	ID          string
	Name        string
	Type        string
	Conn        interface{}
	LastSeen    time.Time
	IsActive    bool
	ConnectedAt time.Time
	BytesSent   int64
	BytesRecv   int64
}

// startStopStats is implemented by TCP, UDP, USB, and Bluetooth managers.
type startStopStats interface {
	Start() error
	Stop()
	GetStats() map[string]interface{}
}

// websocketStartStopStats is implemented by the WebSocket server.
type websocketStartStopStats interface {
	Start() error
	Stop() error
	GetStats() map[string]interface{}
}

// approvableServer is implemented by servers that support device approval.
type approvableServer interface {
	ApproveDevice(deviceID string) error
}

// bluetoothStartStopStats is a subset used by the Bluetooth manager.
type bluetoothStartStopStats interface {
	Start() error
	Stop()
	GetStats() map[string]interface{}
}

// ServerEvent is emitted when the server starts, stops, or a client connects/disconnects.
type ServerEvent struct {
	Type      string // "start", "stop", "client_connected", "client_disconnected"
	ClientID  string
	Timestamp time.Time
}

// ProtocolServer is the main server that manages all protocol servers.
type ProtocolServer struct {
	tcpServer    startStopStats
	wsServer     websocketStartStopStats
	udpServer    startStopStats
	usbServer    startStopStats
	bluetoothMgr bluetoothStartStopStats

	mouseCtrl mouse.Controller
	deviceMgr *device.Manager
	authMgr   *auth.Manager
	proxMgr   *proximity.Manager // proximity manager (optional)

	mu            sync.RWMutex
	running       bool
	startTime     time.Time
	totalMessages int64
	totalBytes    int64
	callbacks     []func(event ServerEvent)
}

// NewProtocolServer creates a new protocol server (without proximity).
func NewProtocolServer(mouseCtrl mouse.Controller, deviceMgr *device.Manager, authMgr *auth.Manager) *ProtocolServer {
	return NewProtocolServerWithProximity(mouseCtrl, deviceMgr, authMgr, nil)
}

// NewProtocolServerWithProximity creates a new protocol server with an optional proximity manager.
func NewProtocolServerWithProximity(
	mouseCtrl mouse.Controller,
	deviceMgr *device.Manager,
	authMgr *auth.Manager,
	proxMgr *proximity.Manager,
) *ProtocolServer {
	return &ProtocolServer{
		mouseCtrl: mouseCtrl,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
		proxMgr:   proxMgr,
		callbacks: make([]func(event ServerEvent), 0),
	}
}

// Start launches all enabled protocol servers.
func (s *ProtocolServer) Start() error {
	s.mu.Lock()
	if s.running {
		s.mu.Unlock()
		return fmt.Errorf("server already running")
	}
	cfg := config.Get()
	s.startTime = time.Now()
	var errors []error
	s.mu.Unlock()

	// Start TCP server
	if cfg.EnableTCP {
		utils.LogInfo("Protocol start: initializing TCP server host=%s port=%d", cfg.Host, cfg.Port)
		s.tcpServer = tcp.NewServer(cfg.Host, cfg.Port, s.mouseCtrl, s.deviceMgr, s.authMgr)
		if err := s.tcpServer.Start(); err != nil {
			errors = append(errors, fmt.Errorf("TCP: %w", err))
			utils.LogError("TCP server start failed: %v", err)
		} else {
			utils.LogInfo("TCP server started on port %d", cfg.Port)
		}
	}

	// Start WebSocket server (pass proximity manager if available)
	if cfg.EnableWebSocket {
		utils.LogInfo("Protocol start: initializing WebSocket server port=%d", cfg.WebSocketPort)
		s.wsServer = websocketpkg.NewServer(cfg.WebSocketPort, s.mouseCtrl, s.deviceMgr, s.authMgr, s.proxMgr)
		if err := s.wsServer.Start(); err != nil {
			errors = append(errors, fmt.Errorf("WebSocket: %w", err))
			utils.LogError("WebSocket server start failed: %v", err)
		} else {
			utils.LogInfo("WebSocket server started on port %d", cfg.WebSocketPort)
		}
	}

	// Start UDP discovery server
	if cfg.EnableUDP {
		utils.LogInfo("Protocol start: initializing UDP discovery port=%d", cfg.UDPPort)
		s.udpServer = udp.NewServer(cfg.UDPPort, s.mouseCtrl, s.deviceMgr, s.authMgr)
		if err := s.udpServer.Start(); err != nil {
			errors = append(errors, fmt.Errorf("UDP: %w", err))
			utils.LogError("UDP server start failed: %v", err)
		} else {
			utils.LogInfo("UDP discovery started on port %d", cfg.UDPPort)
		}
	}

	// Start Bluetooth manager
	if cfg.EnableBluetooth {
		utils.LogInfo("Protocol start: initializing Bluetooth manager adapter=%s", cfg.BluetoothAdapter)
		s.bluetoothMgr = bluetooth.NewManager(cfg.BluetoothAdapter, s.mouseCtrl, s.deviceMgr)
		if err := s.bluetoothMgr.Start(); err != nil {
			errors = append(errors, fmt.Errorf("Bluetooth: %w", err))
			utils.LogError("Bluetooth manager start failed: %v", err)
		} else {
			utils.LogInfo("Bluetooth manager started")
		}
	}

	// Start USB/serial server
	if cfg.EnableSerial {
		utils.LogInfo("Protocol start: initializing USB server")
		s.usbServer = usb.NewServer(s.mouseCtrl, s.deviceMgr)
		if err := s.usbServer.Start(); err != nil {
			errors = append(errors, fmt.Errorf("USB: %w", err))
			utils.LogError("USB server start failed: %v", err)
		} else {
			utils.LogInfo("USB server started")
		}
	}

	s.mu.Lock()
	s.running = true
	s.mu.Unlock()

	s.triggerEvent(ServerEvent{
		Type:      "start",
		Timestamp: time.Now(),
	})

	if len(errors) > 0 {
		return fmt.Errorf("partial start with errors: %v", errors)
	}
	return nil
}

// Stop gracefully stops all protocol servers.
func (s *ProtocolServer) Stop() {
	s.mu.Lock()
	if !s.running {
		s.mu.Unlock()
		return
	}
	// Capture servers and release the lock before stopping them to avoid deadlocks.
	tcpServer := s.tcpServer
	wsServer := s.wsServer
	udpServer := s.udpServer
	bluetoothMgr := s.bluetoothMgr
	usbServer := s.usbServer
	s.running = false
	s.mu.Unlock()

	if tcpServer != nil {
		tcpServer.Stop()
		utils.LogInfo("TCP server stopped")
	}
	if wsServer != nil {
		if err := wsServer.Stop(); err != nil {
			utils.LogError("WebSocket stop error: %v", err)
		} else {
			utils.LogInfo("WebSocket server stopped")
		}
	}
	if udpServer != nil {
		udpServer.Stop()
		utils.LogInfo("UDP server stopped")
	}
	if bluetoothMgr != nil {
		bluetoothMgr.Stop()
		utils.LogInfo("Bluetooth manager stopped")
	}
	if usbServer != nil {
		usbServer.Stop()
		utils.LogInfo("USB server stopped")
	}

	s.triggerEvent(ServerEvent{
		Type:      "stop",
		Timestamp: time.Now(),
	})
	utils.LogInfo("All protocol servers stopped")
}

// IsRunning returns true if the server is running.
func (s *ProtocolServer) IsRunning() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.running
}

// GetUptime returns the server uptime.
func (s *ProtocolServer) GetUptime() time.Duration {
	s.mu.RLock()
	defer s.mu.RUnlock()
	if !s.running {
		return 0
	}
	return time.Since(s.startTime)
}

// GetConnectedDevices returns all connected devices.
func (s *ProtocolServer) GetConnectedDevices() []*device.DeviceInfo {
	if s.deviceMgr == nil {
		return []*device.DeviceInfo{}
	}
	return s.deviceMgr.GetActiveDevices()
}

// GetDeviceCount returns the number of connected devices.
func (s *ProtocolServer) GetDeviceCount() int {
	if s.deviceMgr == nil {
		return 0
	}
	return len(s.deviceMgr.GetActiveDevices())
}

// GetStatistics aggregates statistics from all protocol servers.
func (s *ProtocolServer) GetStatistics() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()

	stats := make(map[string]interface{})
	stats["running"] = s.running

	if s.tcpServer != nil {
		stats["tcp"] = s.tcpServer.GetStats()
	}
	if s.wsServer != nil {
		stats["websocket"] = s.wsServer.GetStats()
	}
	if s.udpServer != nil {
		stats["udp"] = s.udpServer.GetStats()
	}
	if s.bluetoothMgr != nil {
		stats["bluetooth"] = s.bluetoothMgr.GetStats()
	}
	if s.usbServer != nil {
		stats["usb"] = s.usbServer.GetStats()
	}

	if s.deviceMgr != nil {
		stats["devices"] = len(s.deviceMgr.GetActiveDevices())
		stats["blocked"] = s.deviceMgr.GetBlockedCount()
	}

	stats["uptime_seconds"] = s.GetUptime().Seconds()
	stats["total_messages"] = s.totalMessages
	stats["total_bytes"] = s.totalBytes
	stats["start_time"] = s.startTime

	return stats
}

// IncrementMessages increments the global message counter.
func (s *ProtocolServer) IncrementMessages(bytes int64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.totalMessages++
	s.totalBytes += bytes
}

// AddEventListener registers a callback for server events.
func (s *ProtocolServer) AddEventListener(callback func(event ServerEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if callback != nil {
		s.callbacks = append(s.callbacks, callback)
	}
}

// triggerEvent fires all registered event callbacks.
func (s *ProtocolServer) triggerEvent(event ServerEvent) {
	s.mu.RLock()
	callbacks := make([]func(ServerEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()

	utils.LogDebug("Protocol event emitted: type=%s client=%s", event.Type, event.ClientID)
	for _, cb := range callbacks {
		go cb(event)
	}
}

// DisconnectDevice disconnects a device by ID.
func (s *ProtocolServer) DisconnectDevice(deviceID string) error {
	if s.deviceMgr == nil {
		return fmt.Errorf("device manager not initialized")
	}
	if deviceID == "" {
		return fmt.Errorf("device ID cannot be empty")
	}
	return s.deviceMgr.UnregisterDevice(deviceID)
}

// BlockDevice blocks a device by ID.
func (s *ProtocolServer) BlockDevice(deviceID string) error {
	if s.deviceMgr == nil {
		return fmt.Errorf("device manager not initialized")
	}
	if deviceID == "" {
		return fmt.Errorf("device ID cannot be empty")
	}
	return s.deviceMgr.BlockDevice(deviceID)
}

// ApproveDevice approves a pending device and sends the welcome response.
// It tries each protocol server that supports approval until one succeeds.
func (s *ProtocolServer) ApproveDevice(deviceID string) error {
	if deviceID == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

	servers := []approvableServer{}
	if s.tcpServer != nil {
		if srv, ok := s.tcpServer.(approvableServer); ok {
			servers = append(servers, srv)
		}
	}
	if s.wsServer != nil {
		if srv, ok := s.wsServer.(approvableServer); ok {
			servers = append(servers, srv)
		}
	}
	if s.udpServer != nil {
		if srv, ok := s.udpServer.(approvableServer); ok {
			servers = append(servers, srv)
		}
	}

	var lastErr error
	for _, srv := range servers {
		err := srv.ApproveDevice(deviceID)
		if err == nil {
			return nil
		}
		lastErr = err
	}
	if lastErr != nil {
		return lastErr
	}
	return fmt.Errorf("device not found: %s", deviceID)
}

// GetActiveProtocols returns a list of enabled protocol names.
func (s *ProtocolServer) GetActiveProtocols() []string {
	cfg := config.Get()
	protocols := make([]string, 0)
	if cfg.EnableTCP {
		protocols = append(protocols, "TCP")
	}
	if cfg.EnableWebSocket {
		protocols = append(protocols, "WebSocket")
	}
	if cfg.EnableUDP {
		protocols = append(protocols, "UDP")
	}
	if cfg.EnableBluetooth {
		protocols = append(protocols, "Bluetooth")
	}
	if cfg.EnableSerial {
		protocols = append(protocols, "USB")
	}
	return protocols
}

// GetMouseController returns the mouse controller.
func (s *ProtocolServer) GetMouseController() mouse.Controller {
	return s.mouseCtrl
}

// GetDeviceManager returns the device manager.
func (s *ProtocolServer) GetDeviceManager() *device.Manager {
	return s.deviceMgr
}

// GetAuthManager returns the authentication manager.
func (s *ProtocolServer) GetAuthManager() *auth.Manager {
	return s.authMgr
}

// GetProximityManager returns the proximity manager (may be nil).
func (s *ProtocolServer) GetProximityManager() *proximity.Manager {
	return s.proxMgr
}
