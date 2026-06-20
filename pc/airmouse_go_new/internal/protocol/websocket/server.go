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

// ------------------------------------------------------------
// WSClient - WebSocket client
// ------------------------------------------------------------

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

// ------------------------------------------------------------
// Server - WebSocket server
// ------------------------------------------------------------

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
    startTime    time.Time
}

// NewServer creates a new WebSocket server.
func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
    return &Server{
        port:      port,
        clients:   make(map[string]*WSClient),
        mouse:     mouse,
        deviceMgr: deviceMgr,
        authMgr:   authMgr,
        startTime: time.Now(),
    }
}

// Start starts the WebSocket server.
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
    s.startTime = time.Now()

    go func() {
        utils.LogInfo("WebSocket server starting on port %d", s.port)
        if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            utils.LogError("WebSocket server error: %v", err)
        }
    }()
    return nil
}

// Stop stops the WebSocket server.
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
        _ = c.Conn.Close()
    }
    s.clients = make(map[string]*WSClient)

    utils.LogInfo("WebSocket server stopped on port %d", s.port)
    return nil
}

// handleWebSocket handles WebSocket connections.
func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
    cfg := config.Get()

    // Authenticate if enabled
    if cfg.AuthEnabled {
        token := r.URL.Query().Get("token")
        if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
            w.WriteHeader(http.StatusUnauthorized)
            _, _ = w.Write([]byte(`{"error":"unauthorized"}`))
            return
        }
    }

    conn, err := upgrader.Upgrade(w, r, nil)
    if err != nil {
        utils.LogError("WebSocket upgrade failed: %v", err)
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

    // Register device
    if s.deviceMgr != nil {
        _ = s.deviceMgr.RegisterDevice(id, device.TypeWebSocket, "Android")
    }

    utils.LogInfo("WebSocket client connected: id=%s, ip=%s", id, client.IP)

    // Send welcome message
    welcome := WelcomeMessage(cfg.ServerName, cfg.Version)
    client.Send <- welcome

    go s.readLoop(client)
    go s.writeLoop(client)
}

// handleHealth handles health checks.
func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    _, _ = w.Write([]byte(`{"status":"ok","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}

// handleStats handles stats requests.
func (s *Server) handleStats(w http.ResponseWriter, r *http.Request) {
    s.mu.RLock()
    clientCount := len(s.clients)
    s.mu.RUnlock()

    w.Header().Set("Content-Type", "application/json")
    _, _ = fmt.Fprintf(w, `{"clients":%d,"running":%v,"uptime":%.0f}`, clientCount, s.running, time.Since(s.startTime).Seconds())
}

// readLoop reads messages from the client.
func (s *Server) readLoop(client *WSClient) {
    defer func() {
        _ = client.Conn.Close()
        s.mu.Lock()
        delete(s.clients, client.ID)
        s.mu.Unlock()
        if s.deviceMgr != nil {
            _ = s.deviceMgr.UnregisterDevice(client.ID)
        }
        utils.LogInfo("WebSocket client disconnected: id=%s", client.ID)
    }()

    for {
        _, data, err := client.Conn.ReadMessage()
        if err != nil {
            if !gorilla.IsUnexpectedCloseError(err, gorilla.CloseGoingAway, gorilla.CloseAbnormalClosure) {
                utils.LogDebug("WebSocket read error: %v", err)
            }
            return
        }

        client.LastActive = time.Now()
        client.BytesRecv += int64(len(data))

        msgType, payload, id, err := DecodeWireMessage(data)
        if err != nil {
            utils.LogDebug("Invalid WebSocket message: %v", err)
            continue
        }

        s.processMessage(client, msgType, payload, id)
    }
}

// writeLoop writes messages to the client.
func (s *Server) writeLoop(client *WSClient) {
    ticker := time.NewTicker(30 * time.Second)
    defer ticker.Stop()

    for {
        select {
        case msg, ok := <-client.Send:
            if !ok {
                return
            }
            _ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
            if err := client.Conn.WriteMessage(gorilla.TextMessage, msg); err != nil {
                utils.LogDebug("WebSocket write error: %v", err)
                return
            }
            client.BytesSent += int64(len(msg))

        case <-ticker.C:
            // Send ping to keep connection alive
            _ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
            if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
                return
            }
        }
    }
}

// processMessage processes incoming messages.
func (s *Server) processMessage(client *WSClient, msgType string, payload map[string]any, id *string) {
    // Send ACK for commands that expect it
    if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "scroll") {
        if ack := AckMessage(id); len(ack) > 0 {
            client.Send <- ack
        }
    }

    switch msgType {
    case "move":
        dx := toFloat64(payload["dx"])
        dy := toFloat64(payload["dy"])
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
        utils.LogDebug("Click received: device=%s, button=%s", client.ID, button)

    case "doubleclick":
        if s.mouse != nil {
            s.mouse.DoubleClick()
        }
        utils.LogDebug("Double click received: device=%s", client.ID)

    case "rightclick":
        if s.mouse != nil {
            s.mouse.Click("right")
        }
        utils.LogDebug("Right click received: device=%s", client.ID)

    case "scroll":
        delta := toInt(payload["delta"])
        if s.mouse != nil {
            s.mouse.Scroll(delta)
        }
        utils.LogDebug("Scroll received: device=%s, delta=%d", client.ID, delta)

    case "hello":
        name, _ := payload["name"].(string)
        if name == "" {
            name = "Unknown"
        }
        client.Name = name
        if s.deviceMgr != nil {
            _ = s.deviceMgr.UpdateDeviceName(client.ID, name)
        }
        utils.LogInfo("Device identified: id=%s, name=%s", client.ID, name)

        // Send welcome response
        cfg := config.Get()
        welcome := WelcomeMessage(cfg.ServerName, cfg.Version)
        client.Send <- welcome

    case "gesture":
        gesture, _ := payload["gesture"].(string)
        confidence := toFloat64(payload["confidence"])
        utils.LogInfo("Gesture received: device=%s, gesture=%s, confidence=%.2f", client.ID, gesture, confidence)

    case "proximity":
        isNear, _ := payload["is_near"].(bool)
        distance := toFloat64(payload["distance"])
        utils.LogInfo("Proximity update: device=%s, near=%v, distance=%.2f", client.ID, isNear, distance)

    case "control":
        command, _ := payload["command"].(string)
        switch command {
        case "pause_movement":
            control.SetMovementPaused(true)
            utils.LogInfo("Movement paused by device: %s", client.ID)
        case "resume_movement":
            control.SetMovementPaused(false)
            utils.LogInfo("Movement resumed by device: %s", client.ID)
        default:
            utils.LogDebug("Unknown control command: %s", command)
        }

    case "ping":
        client.Send <- PongMessage()

    case "pong":
        // Heartbeat received
        utils.LogDebug("Pong received from: %s", client.ID)

    default:
        utils.LogDebug("Unknown message type: %s from device: %s", msgType, client.ID)
    }
}

// GetStats returns server statistics.
func (s *Server) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()

    var totalSent, totalRecv int64
    for _, c := range s.clients {
        totalSent += c.BytesSent
        totalRecv += c.BytesRecv
    }

    return map[string]interface{}{
        "clients":        len(s.clients),
        "total_clients":  s.totalClients,
        "bytes_sent":     totalSent,
        "bytes_recv":     totalRecv,
        "running":        s.running,
        "port":           s.port,
        "uptime_seconds": time.Since(s.startTime).Seconds(),
    }
}

// GetClientCount returns the number of connected clients.
func (s *Server) GetClientCount() int {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return len(s.clients)
}

// IsRunning returns true if the server is running.
func (s *Server) IsRunning() bool {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return s.running
}