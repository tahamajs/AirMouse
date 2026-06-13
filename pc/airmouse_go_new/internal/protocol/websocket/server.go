package websocket

import (
    "fmt"
    "net/http"
    "sync"
    "time"

    gorilla "github.com/gorilla/websocket"

    "airmouse-go/internal/auth"
    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/utils"
)

var upgrader = gorilla.Upgrader{
    CheckOrigin:     func(r *http.Request) bool { return true },
    ReadBufferSize:  4096,
    WriteBufferSize: 4096,
    EnableCompression: true,
}

type WSClient struct {
    ID          string
    Name        string
    Conn        *gorilla.Conn
    Send        chan []byte
    ConnectedAt time.Time
    LastActive  time.Time
    BytesSent   int64
    BytesRecv   int64
    UserAgent   string
    IP          string
}

type Server struct {
    port         int
    clients      map[string]*WSClient
    mouse        control.MouseController
    deviceMgr    *device.Manager
    authMgr      *auth.Manager
    mu           sync.RWMutex
    server       *http.Server
    running      bool
    totalClients int64
}

func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
    return &Server{
        port:      port,
        clients:   make(map[string]*WSClient),
        mouse:     mouse,
        deviceMgr: deviceMgr,
        authMgr:   authMgr,
    }
}

func (s *Server) Start() error {
    mux := http.NewServeMux()
    mux.HandleFunc("/ws", s.handleWebSocket)
    mux.HandleFunc("/health", s.handleHealth)
    mux.HandleFunc("/stats", s.handleStats)
    
    s.server = &http.Server{
        Addr:         fmt.Sprintf(":%d", s.port),
        Handler:      mux,
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    
    s.running = true
    go func() {
        utils.LogInfo("WebSocket server starting", "port", s.port)
        if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            utils.LogError("WebSocket server error", "error", err)
        }
    }()
    return nil
}

func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
    // Authenticate if enabled
    cfg := config.Get()
    if cfg.AuthEnabled {
        token := r.URL.Query().Get("token")
        if token == "" || !s.authMgr.ValidateToken(token) {
            w.WriteHeader(http.StatusUnauthorized)
            w.Write([]byte(`{"error":"unauthorized"}`))
            return
        }
    }
    
    conn, err := upgrader.Upgrade(w, r, nil)
    if err != nil {
        utils.LogError("WebSocket upgrade failed", "error", err)
        return
    }
    
    id := utils.GenerateID()
    client := &WSClient{
        ID:          id,
        Name:        "Unknown",
        Conn:        conn,
        Send:        make(chan []byte, 256),
        ConnectedAt: time.Now(),
        LastActive:  time.Now(),
        UserAgent:   r.UserAgent(),
        IP:          r.RemoteAddr,
    }
    
    s.mu.Lock()
    s.clients[id] = client
    s.totalClients++
    s.mu.Unlock()
    
    s.deviceMgr.RegisterDevice(id, device.TypeWebSocket, "Android")
    utils.LogInfo("WebSocket client connected", "id", id, "ip", client.IP)
    
    // Send welcome message
    welcome := welcomeMessage(cfg.ServerName, cfg.Version)
    client.Send <- welcome
    
    go s.readLoop(client)
    go s.writeLoop(client)
}

func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.Write([]byte(`{"status":"ok","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}

func (s *Server) handleStats(w http.ResponseWriter, r *http.Request) {
    s.mu.RLock()
    clientCount := len(s.clients)
    s.mu.RUnlock()
    
    w.Header().Set("Content-Type", "application/json")
    fmt.Fprintf(w, `{"clients":%d,"running":%v}`, clientCount, s.running)
}

func (s *Server) readLoop(client *WSClient) {
    defer func() {
        client.Conn.Close()
        s.mu.Lock()
        delete(s.clients, client.ID)
        s.mu.Unlock()
        s.deviceMgr.UnregisterDevice(client.ID)
        utils.LogInfo("WebSocket client disconnected", "id", client.ID)
    }()
    
    for {
        _, data, err := client.Conn.ReadMessage()
        if err != nil {
            return
        }
        
        client.LastActive = time.Now()
        client.BytesRecv += int64(len(data))
        
        msgType, payload, id, err := decodeWireMessage(data)
        if err != nil {
            utils.LogDebug("Invalid WebSocket message", "error", err)
            continue
        }
        
        s.processMessage(client, msgType, payload, id)
    }
}

func (s *Server) writeLoop(client *WSClient) {
    for msg := range client.Send {
        if err := client.Conn.WriteMessage(gorilla.TextMessage, msg); err != nil {
            return
        }
        client.BytesSent += int64(len(msg))
    }
}

func (s *Server) processMessage(client *WSClient, msgType string, payload map[string]any, id *string) {
    // Send ACK for commands that expect it
    if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "scroll") {
        if ack := ackMessage(id); len(ack) > 0 {
            client.Send <- ack
        }
    }
    
    switch msgType {
    case "move":
        dx := number(payload["dx"])
        dy := number(payload["dy"])
        s.mouse.Move(dx, dy)
        
    case "click":
        button, _ := payload["button"].(string)
        if button == "" {
            button = "left"
        }
        s.mouse.Click(button)
        utils.LogDebug("Click received", "device", client.ID, "button", button)
        
    case "doubleclick":
        s.mouse.DoubleClick()
        utils.LogDebug("Double click received", "device", client.ID)
        
    case "rightclick":
        s.mouse.Click("right")
        utils.LogDebug("Right click received", "device", client.ID)
        
    case "scroll":
        delta := int(number(payload["delta"]))
        s.mouse.Scroll(delta)
        utils.LogDebug("Scroll received", "device", client.ID, "delta", delta)
        
    case "hello":
        name, _ := payload["name"].(string)
        if name == "" {
            name = "Unknown"
        }
        client.Name = name
        s.deviceMgr.UpdateDeviceName(client.ID, name)
        utils.LogInfo("Device identified", "id", client.ID, "name", name)
        
        // Send welcome response
        cfg := config.Get()
        welcome := welcomeMessage(cfg.ServerName, cfg.Version)
        client.Send <- welcome
        
    case "gesture":
        gesture, _ := payload["gesture"].(string)
        confidence := number(payload["confidence"])
        utils.LogInfo("Gesture received", "device", client.ID, "gesture", gesture, "confidence", confidence)
        
    case "proximity":
        isNear, _ := payload["is_near"].(bool)
        distance := number(payload["distance"])
        utils.LogInfo("Proximity update", "device", client.ID, "near", isNear, "distance", distance)
        
    case "control":
        command, _ := payload["command"].(string)
        switch command {
        case "pause_movement":
            control.SetMovementPaused(true)
            utils.LogInfo("Movement paused", "device", client.ID)
        case "resume_movement":
            control.SetMovementPaused(false)
            utils.LogInfo("Movement resumed", "device", client.ID)
        }
        
    case "ping":
        client.Send <- pongMessage()
        
    case "pong":
        // Heartbeat received - update latency
        // Would calculate RTT in real implementation
        
    default:
        utils.LogDebug("Unknown message type", "type", msgType, "device", client.ID)
    }
}

func (s *Server) Stop() error {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if !s.running {
        return nil
    }
    
    s.running = false
    if s.server != nil {
        if err := s.server.Close(); err != nil {
            return err
        }
    }
    
    for _, c := range s.clients {
        close(c.Send)
        c.Conn.Close()
    }
    s.clients = make(map[string]*WSClient)
    
    utils.LogInfo("WebSocket server stopped")
    return nil
}

func (s *Server) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    var totalSent, totalRecv int64
    for _, c := range s.clients {
        totalSent += c.BytesSent
        totalRecv += c.BytesRecv
    }
    
    return map[string]interface{}{
        "clients":          len(s.clients),
        "total_clients":    s.totalClients,
        "bytes_sent":       totalSent,
        "bytes_recv":       totalRecv,
        "running":          s.running,
        "port":             s.port,
    }
}