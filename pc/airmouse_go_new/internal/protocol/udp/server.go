package udp

import (
	"encoding/json"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	websocketpkg "airmouse-go/internal/protocol/websocket"
	"airmouse-go/internal/utils"
)

// Server – UDP discovery server
type Server struct {
	port      int
	conn      *net.UDPConn
	deviceMgr *device.Manager
	mouse     control.MouseController
	authMgr   *auth.Manager
	running   bool
	mu        sync.RWMutex
	clients   map[string]*UDPClient
	callbacks []func(event UDPEvent)
}

// UDPClient – a discovered client
type UDPClient struct {
	Address    *net.UDPAddr
	LastSeen   time.Time
	DeviceID   string
	DeviceName string
	Approved   bool
	BytesSent  int64
	BytesRecv  int64
}

// UDPEvent – event for callbacks
type UDPEvent struct {
	Type      string
	ClientIP  string
	Timestamp time.Time
}

func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		port:      port,
		mouse:     mouse,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
		clients:   make(map[string]*UDPClient),
		callbacks: make([]func(UDPEvent), 0),
	}
}

// Start the UDP discovery server
func (s *Server) Start() error {
	addr := net.UDPAddr{
		Port: s.port,
		IP:   net.ParseIP("0.0.0.0"),
	}

	conn, err := net.ListenUDP("udp4", &addr)
	if err != nil {
		return fmt.Errorf("failed to listen on UDP port %d: %w", s.port, err)
	}

	s.conn = conn
	s.running = true
	go s.listenLoop()

	utils.LogInfo("UDP discovery server started on port %d", s.port)
	utils.LogDebug("UDP discovery bound to %s", addr.String())
	return nil
}

// listenLoop – main receive loop
func (s *Server) listenLoop() {
	buf := make([]byte, 4096)
	for s.running {
		s.conn.SetReadDeadline(time.Now().Add(2 * time.Second))
		n, clientAddr, err := s.conn.ReadFromUDP(buf)
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}
			if s.running {
				utils.LogDebug("UDP read error: %v", err)
			}
			continue
		}

		msg := strings.TrimSpace(string(buf[:n]))
		s.handleMessage(msg, clientAddr)
	}
}

// handleMessage – process incoming messages
func (s *Server) handleMessage(msg string, clientAddr *net.UDPAddr) {
	clientIP := clientAddr.IP.String()
	utils.LogDebug("UDP inbound packet from %s payload=%q", clientIP, msg)

	clientKey := clientAddr.String()
	// Update/register client
	s.mu.Lock()
	client, exists := s.clients[clientKey]
	if !exists {
		client = &UDPClient{
			Address:  clientAddr,
			LastSeen: time.Now(),
		}
		s.clients[clientKey] = client
	}
	client.Address = clientAddr
	client.LastSeen = time.Now()
	client.BytesRecv += int64(len(msg))
	s.mu.Unlock()

	// Handle discovery requests (both spellings)
	if msg == "AIRMOUSE_DISCOVER" || msg == "AIRMOUSE_DISCOVERY" {
		s.sendDiscoveryResponse(clientAddr)
		utils.LogDebug("UDP discovery request from %s", clientIP)
		return
	}

	// Try JSON parsing for structured messages
	msgType, payload, id, err := websocketpkg.DecodeWireMessage([]byte(msg))
	if err != nil {
		if msg == "AIRMOUSE_HELLO" {
			s.triggerEvent(UDPEvent{
				Type:      "hello",
				ClientIP:  clientIP,
				Timestamp: time.Now(),
			})
		} else {
			utils.LogDebug("UDP unknown message from %s: %s", clientIP, msg)
		}
		return
	}

	if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "rightclick" || msgType == "scroll") {
		s.writeToClient(clientKey, clientAddr, websocketpkg.AckMessage(id))
	}

	if !client.Approved && msgType != "hello" && msgType != "ping" {
		utils.LogDebug("Ignoring UDP %s while waiting for approval: client=%s", msgType, clientIP)
		return
	}

	switch msgType {
	case "move":
		dx := firstNumber(payload, "dx", "DeltaX", "deltaX")
		dy := firstNumber(payload, "dy", "DeltaY", "deltaY")
		if s.mouse != nil {
			s.mouse.Move(dx, dy)
		}
	case "click":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
		utils.LogDebug("UDP click received: client=%s button=%s", clientIP, button)
	case "doubleclick":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
	case "rightclick":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
	case "scroll":
		delta := int(firstNumber(payload, "delta", "Scroll", "scroll"))
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
	case "hello":
		s.handleHello(clientKey, clientAddr, client, payload)
	case "gesture":
		gesture, _ := payload["gesture"].(string)
		confidence := firstNumber(payload, "confidence")
		utils.LogInfo("UDP gesture received: client=%s gesture=%s confidence=%.2f", clientIP, gesture, confidence)
	case "proximity":
		isNear, _ := payload["is_near"].(bool)
		distance := firstNumber(payload, "distance")
		utils.LogInfo("UDP proximity update: client=%s near=%v distance=%.2f", clientIP, isNear, distance)
	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "start", "touchpad_start", "resume", "resume_movement":
			control.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by UDP client: %s command=%s", clientIP, command)
		case "stop", "touchpad_stop", "pause", "pause_movement":
			control.SetMovementPaused(true)
			utils.LogInfo("Movement paused by UDP client: %s command=%s", clientIP, command)
		default:
			utils.LogDebug("Unknown UDP control command: %s", command)
		}
	case "ping":
		s.writeToClient(clientKey, clientAddr, websocketpkg.PongMessage())
	case "pong":
		utils.LogDebug("UDP pong received from %s", clientIP)
	default:
		utils.LogDebug("UDP message type=%s from %s", msgType, clientIP)
	}
}

// sendDiscoveryResponse – sends plain‑text response compatible with Android app
func (s *Server) sendDiscoveryResponse(clientAddr *net.UDPAddr) {
	cfg := config.Get()
	// Android discovery clients currently accept the legacy text format:
	// AIRMOUSE_SERVER:<port>:<name>:<version>
	response := fmt.Sprintf("AIRMOUSE_SERVER:%d:%s:%s", cfg.Port, cfg.ServerName, cfg.Version)
	_, err := s.conn.WriteToUDP([]byte(response), clientAddr)
	if err != nil {
		utils.LogDebug("Failed to send discovery response: %v", err)
		return
	}
	utils.LogInfo("UDP discovery response sent to %s on port %d", clientAddr.IP.String(), cfg.Port)
}

func (s *Server) handleHello(clientKey string, clientAddr *net.UDPAddr, client *UDPClient, payload map[string]any) {
	cfg := config.Get()
	name, _ := payload["name"].(string)
	version, _ := payload["version"].(string)
	deviceName, _ := payload["device_name"].(string)
	model, _ := payload["model"].(string)
	manufacturer, _ := payload["manufacturer"].(string)
	brand, _ := payload["brand"].(string)
	androidVersion, _ := payload["android_version"].(string)
	sdkInt, _ := payload["sdk_int"].(string)
	deviceIDValue, _ := payload["device_id"].(string)
	protocolName, _ := payload["protocol"].(string)
	transport, _ := payload["transport"].(string)
	token, _ := payload["token"].(string)
	fingerprint := device.StableDeviceID(deviceIDValue, name, version, deviceName, model, manufacturer, brand, androidVersion, sdkInt, protocolName, transport)

	if name == "" {
		name = "Unknown"
	}

	if cfg.AuthEnabled {
		if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
			_ = s.writeToClient(clientKey, clientAddr, []byte(`{"type":"error","payload":{"message":"connection rejected: invalid pairing token"}}`+"\n"))
			utils.LogInfo("UDP client rejected: addr=%s reason=invalid token", clientAddr.String())
			return
		}
	}

	s.mu.Lock()
	client.Approved = true
	client.DeviceName = name
	client.DeviceID = fingerprint
	s.mu.Unlock()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeUDP, name, map[string]string{
			"fingerprint":     fingerprint,
			"ip_address":      clientAddr.IP.String(),
			"version":         version,
			"device_name":     deviceName,
			"device_model":    model,
			"manufacturer":    manufacturer,
			"brand":           brand,
			"android_version": androidVersion,
			"device_id":       deviceIDValue,
			"sdk_int":         sdkInt,
			"protocol":        protocolName,
			"transport":       transport,
		})
	}

	welcome := websocketpkg.WelcomeMessage(cfg.ServerName, cfg.Version)
	s.writeToClient(clientKey, clientAddr, welcome)
	utils.LogInfo("UDP approval accepted: addr=%s name=%s", clientAddr.String(), name)
}

func (s *Server) writeToClient(clientKey string, clientAddr *net.UDPAddr, data []byte) error {
	if len(data) == 0 {
		return nil
	}
	if _, err := s.conn.WriteToUDP(data, clientAddr); err != nil {
		utils.LogDebug("Failed to send UDP message to %s: %v", clientAddr.String(), err)
		return err
	}
	s.mu.Lock()
	if client, ok := s.clients[clientKey]; ok {
		client.BytesSent += int64(len(data))
	}
	s.mu.Unlock()
	return nil
}

// Stop the server
func (s *Server) Stop() {
	s.running = false
	if s.conn != nil {
		s.conn.Close()
	}
	s.mu.Lock()
	s.clients = make(map[string]*UDPClient)
	s.mu.Unlock()
	utils.LogInfo("UDP discovery server stopped")
}

// GetStats – server statistics
func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()

	activeClients := 0
	now := time.Now()
	for _, c := range s.clients {
		if now.Sub(c.LastSeen) < 30*time.Second {
			activeClients++
		}
	}

	return map[string]interface{}{
		"running":        s.running,
		"port":           s.port,
		"total_clients":  len(s.clients),
		"active_clients": activeClients,
	}
}

// AddEventListener – register a callback for events
func (s *Server) AddEventListener(callback func(event UDPEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if callback != nil {
		s.callbacks = append(s.callbacks, callback)
	}
}

// triggerEvent – fire an event
func (s *Server) triggerEvent(event UDPEvent) {
	s.mu.RLock()
	callbacks := make([]func(UDPEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

// BroadcastMessage – send a message to all known clients
func (s *Server) BroadcastMessage(msg interface{}) error {
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, client := range s.clients {
		s.conn.WriteToUDP(data, client.Address)
	}
	return nil
}

// GetConnectedClients – list all UDP clients
func (s *Server) GetConnectedClients() []*UDPClient {
	s.mu.RLock()
	defer s.mu.RUnlock()

	clients := make([]*UDPClient, 0, len(s.clients))
	for _, c := range s.clients {
		clients = append(clients, c)
	}
	return clients
}

func firstNumber(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			switch t := v.(type) {
			case float64:
				return t
			case float32:
				return float64(t)
			case int:
				return float64(t)
			case int64:
				return float64(t)
			case json.Number:
				f, _ := t.Float64()
				return f
			}
		}
	}
	return 0
}

// getLocalIP – utility to get the first non‑loopback IPv4 address
func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			return ipnet.IP.String()
		}
	}
	return "127.0.0.1"
}
