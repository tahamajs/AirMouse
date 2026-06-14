package udp

import (
    "encoding/json"
    "fmt"
    "net"
    "strings"
    "sync"
    "time"

    "airmouse-go/internal/device"
    "airmouse-go/internal/utils"
)

type Server struct {
    port      int
    conn      *net.UDPConn
    deviceMgr *device.Manager
    running   bool
    mu        sync.RWMutex
    clients   map[string]*UDPClient
    callbacks []func(event UDPEvent)
}

type UDPClient struct {
    Address     *net.UDPAddr
    LastSeen    time.Time
    DeviceID    string
    DeviceName  string
}

type UDPEvent struct {
    Type      string
    ClientIP  string
    Timestamp time.Time
}

func NewServer(port int, deviceMgr *device.Manager) *Server {
    return &Server{
        port:      port,
        deviceMgr: deviceMgr,
        clients:   make(map[string]*UDPClient),
        callbacks: make([]func(UDPEvent), 0),
    }
}

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
    
    utils.LogInfo("UDP discovery server started", "port", s.port)
    return nil
}

func (s *Server) listenLoop() {
    buf := make([]byte, 1024)
    
    for s.running {
        s.conn.SetReadDeadline(time.Now().Add(2 * time.Second))
        n, clientAddr, err := s.conn.ReadFromUDP(buf)
        if err != nil {
            if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
                continue
            }
            if s.running {
                utils.LogDebug("UDP read error", "error", err)
            }
            continue
        }
        
        msg := strings.TrimSpace(string(buf[:n]))
        s.handleMessage(msg, clientAddr)
    }
}

func (s *Server) handleMessage(msg string, clientAddr *net.UDPAddr) {
    clientIP := clientAddr.IP.String()
    
    // Update client info
    s.mu.Lock()
    client, exists := s.clients[clientIP]
    if !exists {
        client = &UDPClient{
            Address:  clientAddr,
            LastSeen: time.Now(),
        }
        s.clients[clientIP] = client
    }
    client.LastSeen = time.Now()
    s.mu.Unlock()
    
    // Handle different message types
    switch {
    case msg == "AIRMOUSE_DISCOVER" || msg == "AIRMOUSE_DISCOVERY":
        s.sendDiscoveryResponse(clientAddr)
        utils.LogDebug("UDP discovery request from", "ip", clientIP)
        
    case msg == "AIRMOUSE_HELLO":
        s.triggerEvent(UDPEvent{
            Type:      "hello",
            ClientIP:  clientIP,
            Timestamp: time.Now(),
        })
        
    default:
        // Parse JSON message
        var parsed map[string]interface{}
        if err := json.Unmarshal([]byte(msg), &parsed); err == nil {
            if msgType, ok := parsed["type"].(string); ok {
                switch msgType {
                case "proximity":
                    utils.LogDebug("UDP proximity update", "from", clientIP)
                case "ping":
                    // Respond to ping
                    s.sendPong(clientAddr)
                default:
                    utils.LogDebug("UDP message received", "type", msgType, "from", clientIP)
                }
            }
        } else {
            utils.LogDebug("UDP unknown message", "msg", msg, "from", clientIP)
        }
    }
}

func (s *Server) sendDiscoveryResponse(clientAddr *net.UDPAddr) {
    localIP := getLocalIP()
    response := map[string]interface{}{
        "type":    "discovery_response",
        "port":    8080,
        "ip":      localIP,
        "name":    "Air Mouse Server",
        "version": "3.0.0",
    }
    
    data, err := json.Marshal(response)
    if err != nil {
        utils.LogError("Failed to marshal discovery response", "error", err)
        return
    }
    
    _, err = s.conn.WriteToUDP(data, clientAddr)
    if err != nil {
        utils.LogDebug("Failed to send discovery response", "error", err)
    }
}

func (s *Server) sendPong(clientAddr *net.UDPAddr) {
    response := map[string]interface{}{
        "type": "pong",
        "time": time.Now().UnixMilli(),
    }
    
    data, err := json.Marshal(response)
    if err != nil {
        return
    }
    
    s.conn.WriteToUDP(data, clientAddr)
}

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

func (s *Server) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    activeClients := 0
    now := time.Now()
    for _, client := range s.clients {
        if now.Sub(client.LastSeen) < 30*time.Second {
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

func (s *Server) AddEventListener(callback func(event UDPEvent)) {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.callbacks = append(s.callbacks, callback)
}

func (s *Server) triggerEvent(event UDPEvent) {
    s.mu.RLock()
    callbacks := make([]func(UDPEvent), len(s.callbacks))
    copy(callbacks, s.callbacks)
    s.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

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

func (s *Server) GetConnectedClients() []*UDPClient {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    clients := make([]*UDPClient, 0, len(s.clients))
    for _, client := range s.clients {
        clients = append(clients, client)
    }
    return clients
}

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