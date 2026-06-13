package websocket

import (
    "encoding/json"
    "sync"
    "sync/atomic"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/service"
    "airmouse-go/internal/infra/logger"
)

type Hub struct {
    clients        map[*Client]bool
    register       chan *Client
    unregister     chan *Client
    broadcast      chan []byte
    mu             sync.RWMutex
    mouseService   service.MouseService
    gestureService service.GestureService
    connService    service.ConnectionService
    stats          HubStats
    callbacks      []func(event HubEvent)
}

type HubStats struct {
    TotalConnections   int64
    ActiveConnections  int64
    TotalMessages      int64
    TotalBytesSent     int64
    TotalBytesRecv     int64
    StartTime          time.Time
}

type HubEvent struct {
    Type      string // "client_connected", "client_disconnected", "broadcast"
    ClientID  string
    Timestamp time.Time
}

func NewHub(mouseSvc service.MouseService, gestureSvc service.GestureService, connSvc service.ConnectionService) *Hub {
    return &Hub{
        clients:        make(map[*Client]bool),
        register:       make(chan *Client),
        unregister:     make(chan *Client),
        broadcast:      make(chan []byte, 256),
        mouseService:   mouseSvc,
        gestureService: gestureSvc,
        connService:    connSvc,
        stats: HubStats{
            StartTime: time.Now(),
        },
        callbacks: make([]func(HubEvent), 0),
    }
}

func (h *Hub) Run() {
    ticker := time.NewTicker(30 * time.Second)
    defer ticker.Stop()

    for {
        select {
        case client := <-h.register:
            h.registerClient(client)

        case client := <-h.unregister:
            h.unregisterClient(client)

        case message := <-h.broadcast:
            h.broadcastMessage(message)

        case <-ticker.C:
            h.cleanupInactiveClients()
            h.updateStats()
        }
    }
}

func (h *Hub) registerClient(client *Client) {
    h.mu.Lock()
    defer h.mu.Unlock()

    h.clients[client] = true
    atomic.AddInt64(&h.stats.ActiveConnections, 1)
    atomic.AddInt64(&h.stats.TotalConnections, 1)

    if err := h.connService.RegisterClient(client.entity); err != nil {
        logger.Error("Failed to register client: %v", err)
    }

    h.triggerEvent(HubEvent{
        Type:      "client_connected",
        ClientID:  client.id,
        Timestamp: time.Now(),
    })

    logger.Info("WebSocket client connected: id=%s, total=%d", client.id, h.stats.ActiveConnections)
}

func (h *Hub) unregisterClient(client *Client) {
    h.mu.Lock()
    defer h.mu.Unlock()

    if _, ok := h.clients[client]; ok {
        delete(h.clients, client)
        close(client.send)
        atomic.AddInt64(&h.stats.ActiveConnections, -1)

        if err := h.connService.UnregisterClient(client.id); err != nil {
            logger.Error("Failed to unregister client: %v", err)
        }

        h.triggerEvent(HubEvent{
            Type:      "client_disconnected",
            ClientID:  client.id,
            Timestamp: time.Now(),
        })

        logger.Info("WebSocket client disconnected: id=%s, active=%d", client.id, h.stats.ActiveConnections)
    }
}

func (h *Hub) broadcastMessage(message []byte) {
    h.mu.RLock()
    defer h.mu.RUnlock()

    atomic.AddInt64(&h.stats.TotalMessages, 1)
    atomic.AddInt64(&h.stats.TotalBytesSent, int64(len(message)))

    for client := range h.clients {
        select {
        case client.send <- message:
        default:
            // Client's send buffer is full, disconnect them
            go h.unregisterClient(client)
        }
    }

    h.triggerEvent(HubEvent{
        Type:      "broadcast",
        Timestamp: time.Now(),
    })
}

func (h *Hub) cleanupInactiveClients() {
    h.mu.RLock()
    clients := make([]*Client, 0, len(h.clients))
    for client := range h.clients {
        clients = append(clients, client)
    }
    h.mu.RUnlock()

    for _, client := range clients {
        if time.Since(client.GetLastActive()) > 60*time.Second {
            h.unregisterClient(client)
            logger.Debug("Removed inactive client: %s", client.id)
        }
    }
}

func (h *Hub) updateStats() {
    // Stats are updated atomically, nothing to do here
}

func (h *Hub) GetStats() HubStats {
    return HubStats{
        TotalConnections:  atomic.LoadInt64(&h.stats.TotalConnections),
        ActiveConnections: atomic.LoadInt64(&h.stats.ActiveConnections),
        TotalMessages:     atomic.LoadInt64(&h.stats.TotalMessages),
        TotalBytesSent:    atomic.LoadInt64(&h.stats.TotalBytesSent),
        TotalBytesRecv:    atomic.LoadInt64(&h.stats.TotalBytesRecv),
        StartTime:         h.stats.StartTime,
    }
}

func (h *Hub) BroadcastToAll(message interface{}) error {
    data, err := json.Marshal(message)
    if err != nil {
        return err
    }
    h.broadcast <- data
    return nil
}

func (h *Hub) SendToClient(clientID string, message interface{}) error {
    h.mu.RLock()
    defer h.mu.RUnlock()

    for client := range h.clients {
        if client.id == clientID {
            data, err := json.Marshal(message)
            if err != nil {
                return err
            }
            select {
            case client.send <- data:
                return nil
            default:
                return nil
            }
        }
    }
    return nil
}

func (h *Hub) GetConnectedClients() []*entity.Client {
    h.mu.RLock()
    defer h.mu.RUnlock()

    clients := make([]*entity.Client, 0, len(h.clients))
    for client := range h.clients {
        clients = append(clients, client.entity)
    }
    return clients
}

func (h *Hub) AddEventListener(callback func(event HubEvent)) {
    h.mu.Lock()
    defer h.mu.Unlock()
    h.callbacks = append(h.callbacks, callback)
}

func (h *Hub) triggerEvent(event HubEvent) {
    h.mu.RLock()
    callbacks := make([]func(HubEvent), len(h.callbacks))
    copy(callbacks, h.callbacks)
    h.mu.RUnlock()

    for _, cb := range callbacks {
        go cb(event)
    }
}