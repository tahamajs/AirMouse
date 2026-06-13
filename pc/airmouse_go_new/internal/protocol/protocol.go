package protocol

import (
    "fmt"
    "sync"
    "time"

    "airmouse-go/internal/auth"
    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/protocol/bluetooth"
    "airmouse-go/internal/protocol/tcp"
    "airmouse-go/internal/protocol/udp"
    "airmouse-go/internal/protocol/usb"
    "airmouse-go/internal/protocol/websocket"
    "airmouse-go/internal/utils"
)

type Client struct {
    ID         string
    Name       string
    Type       string
    Conn       interface{}
    LastSeen   time.Time
    IsActive   bool
    ConnectedAt time.Time
    BytesSent  int64
    BytesRecv  int64
}

type startStopStats interface {
    Start() error
    Stop()
    GetStats() map[string]interface{}
}

type websocketStartStopStats interface {
    Start() error
    Stop() error
    GetStats() map[string]interface{}
}

type bluetoothStartStopStats interface {
    Start() error
    Stop()
    GetStats() map[string]interface{}
}

type ProtocolServer struct {
    tcpServer    startStopStats
    wsServer     websocketStartStopStats
    udpServer    startStopStats
    usbServer    startStopStats
    bluetoothMgr bluetoothStartStopStats

    mouseCtrl    control.MouseController
    deviceMgr    *device.Manager
    authMgr      *auth.Manager
    proximityMgr interface{}

    mu           sync.RWMutex
    running      bool
    startTime    time.Time
    totalMessages int64
    totalBytes   int64
    callbacks    []func(event ServerEvent)
}

type ServerEvent struct {
    Type      string    // "start", "stop", "client_connected", "client_disconnected"
    ClientID  string
    Timestamp time.Time
}

func NewProtocolServer(mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *ProtocolServer {
    return &ProtocolServer{
        mouseCtrl: mouse,
        deviceMgr: deviceMgr,
        authMgr:   authMgr,
        callbacks: make([]func(event ServerEvent), 0),
    }
}

func (s *ProtocolServer) Start() error {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if s.running {
        return fmt.Errorf("server already running")
    }
    
    cfg := config.Get()
    s.startTime = time.Now()
    var errors []error

    // Start TCP server
    if cfg.EnableTCP {
        s.tcpServer = newTCPServer(cfg.Host, cfg.Port, s.mouseCtrl, s.deviceMgr)
        if err := s.tcpServer.Start(); err != nil {
            errors = append(errors, fmt.Errorf("TCP: %w", err))
            utils.LogError("TCP server start failed", "error", err)
        } else {
            utils.LogInfo("TCP server started", "port", cfg.Port)
        }
    }

    // Start WebSocket server
    if cfg.EnableWebSocket {
        s.wsServer = newWebSocketServer(cfg.WebSocketPort, s.mouseCtrl, s.deviceMgr, s.authMgr)
        if err := s.wsServer.Start(); err != nil {
            errors = append(errors, fmt.Errorf("WebSocket: %w", err))
            utils.LogError("WebSocket server start failed", "error", err)
        } else {
            utils.LogInfo("WebSocket server started", "port", cfg.WebSocketPort)
        }
    }

    // Start UDP server
    if cfg.EnableUDP {
        s.udpServer = newUDPServer(cfg.UDPPort, s.deviceMgr)
        if err := s.udpServer.Start(); err != nil {
            errors = append(errors, fmt.Errorf("UDP: %w", err))
            utils.LogError("UDP server start failed", "error", err)
        } else {
            utils.LogInfo("UDP discovery started", "port", cfg.UDPPort)
        }
    }

    // Start Bluetooth manager
    if cfg.EnableBluetooth {
        s.bluetoothMgr = newBluetoothMgr(cfg.BluetoothAdapter, s.mouseCtrl, s.deviceMgr)
        if err := s.bluetoothMgr.Start(); err != nil {
            errors = append(errors, fmt.Errorf("Bluetooth: %w", err))
            utils.LogError("Bluetooth manager start failed", "error", err)
        } else {
            utils.LogInfo("Bluetooth manager started")
        }
    }

    // Start USB server
    if cfg.EnableSerial {
        s.usbServer = newUSBServer(s.mouseCtrl, s.deviceMgr)
        if err := s.usbServer.Start(); err != nil {
            errors = append(errors, fmt.Errorf("USB: %w", err))
            utils.LogError("USB server start failed", "error", err)
        } else {
            utils.LogInfo("USB server started")
        }
    }

    s.running = true
    s.triggerEvent(ServerEvent{
        Type:      "start",
        Timestamp: time.Now(),
    })

    if len(errors) > 0 {
        return fmt.Errorf("partial start with errors: %v", errors)
    }
    return nil
}

func (s *ProtocolServer) Stop() {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if !s.running {
        return
    }

    if s.tcpServer != nil {
        s.tcpServer.Stop()
        utils.LogInfo("TCP server stopped")
    }
    if s.wsServer != nil {
        if err := s.wsServer.Stop(); err != nil {
            utils.LogError("WebSocket stop error", "error", err)
        } else {
            utils.LogInfo("WebSocket server stopped")
        }
    }
    if s.udpServer != nil {
        s.udpServer.Stop()
        utils.LogInfo("UDP server stopped")
    }
    if s.bluetoothMgr != nil {
        s.bluetoothMgr.Stop()
        utils.LogInfo("Bluetooth manager stopped")
    }
    if s.usbServer != nil {
        s.usbServer.Stop()
        utils.LogInfo("USB server stopped")
    }

    s.running = false
    s.triggerEvent(ServerEvent{
        Type:      "stop",
        Timestamp: time.Now(),
    })
    utils.LogInfo("All protocol servers stopped")
}

func (s *ProtocolServer) IsRunning() bool {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return s.running
}

func (s *ProtocolServer) GetUptime() time.Duration {
    s.mu.RLock()
    defer s.mu.RUnlock()
    if !s.running {
        return 0
    }
    return time.Since(s.startTime)
}

func (s *ProtocolServer) GetConnectedDevices() []*device.DeviceInfo {
    return s.deviceMgr.GetAllDevices()
}

func (s *ProtocolServer) GetDeviceCount() int {
    return len(s.deviceMgr.GetAllDevices())
}

func (s *ProtocolServer) GetStatistics() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    stats := make(map[string]interface{})
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
    
    stats["devices"] = len(s.deviceMgr.GetAllDevices())
    stats["running"] = s.running
    stats["uptime_seconds"] = s.GetUptime().Seconds()
    stats["total_messages"] = s.totalMessages
    stats["total_bytes"] = s.totalBytes
    stats["start_time"] = s.startTime
    
    return stats
}

func (s *ProtocolServer) IncrementMessages(bytes int64) {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.totalMessages++
    s.totalBytes += bytes
}

func (s *ProtocolServer) AddEventListener(callback func(event ServerEvent)) {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.callbacks = append(s.callbacks, callback)
}

func (s *ProtocolServer) triggerEvent(event ServerEvent) {
    s.mu.RLock()
    callbacks := make([]func(ServerEvent), len(s.callbacks))
    copy(callbacks, s.callbacks)
    s.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (s *ProtocolServer) DisconnectDevice(deviceID string) error {
    return s.deviceMgr.UnregisterDevice(deviceID)
}

func (s *ProtocolServer) BlockDevice(deviceID string) error {
    return s.deviceMgr.BlockDevice(deviceID)
}

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