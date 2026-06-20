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

// ------------------------------------------------------------
//  Hub
// ------------------------------------------------------------

type Hub struct {
	clients        map[*Client]bool
	register       chan *Client
	unregister     chan *Client
	broadcast      chan []byte
	mu             sync.RWMutex
	mouseService   service.MouseService
	gestureService service.GestureService
	connService    service.ConnectionService
	handler        *Handler
	stats          HubStats
	callbacks      []func(event HubEvent)
	stopped        bool
	stopChan       chan struct{}
}

type HubStats struct {
	TotalConnections  int64
	ActiveConnections int64
	TotalMessages     int64
	TotalBytesSent    int64
	TotalBytesRecv    int64
	StartTime         time.Time
}

type HubEvent struct {
	Type      string // "client_connected", "client_disconnected", "broadcast"
	ClientID  string
	Timestamp time.Time
}

// NewHub creates a new WebSocket hub
func NewHub(mouseSvc service.MouseService, gestureSvc service.GestureService, connSvc service.ConnectionService) *Hub {
	hub := &Hub{
		clients:        make(map[*Client]bool),
		register:       make(chan *Client, 256),
		unregister:     make(chan *Client, 256),
		broadcast:      make(chan []byte, 256),
		mouseService:   mouseSvc,
		gestureService: gestureSvc,
		connService:    connSvc,
		stopChan:       make(chan struct{}),
		stopped:        false,
		stats: HubStats{
			StartTime: time.Now(),
		},
		callbacks: make([]func(HubEvent), 0),
	}

	// Create handler
	hub.handler = NewHandler(hub)

	return hub
}

// GetHandler returns the hub's handler
func (h *Hub) GetHandler() *Handler {
	return h.handler
}

// Run starts the hub's main loop
func (h *Hub) Run() {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	logger.Info("WebSocket hub started")

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

		case <-h.stopChan:
			logger.Info("WebSocket hub stopped")
			return
		}
	}
}

// Stop stops the hub
func (h *Hub) Stop() {
	if h.stopped {
		return
	}
	h.stopped = true
	close(h.stopChan)
}

// registerClient registers a new client
func (h *Hub) registerClient(client *Client) {
	if client == nil {
		return
	}

	h.mu.Lock()
	defer h.mu.Unlock()

	// Check if client already exists
	for existing := range h.clients {
		if existing.id == client.id {
			logger.Debug("Client already registered: %s", client.id)
			return
		}
	}

	h.clients[client] = true
	atomic.AddInt64(&h.stats.ActiveConnections, 1)
	atomic.AddInt64(&h.stats.TotalConnections, 1)

	// Set handler on client
	client.SetHandler(h.handler)

	if h.connService != nil {
		if err := h.connService.RegisterClient(client.entity); err != nil {
			logger.Error("Failed to register client: %v", err)
		}
	}

	h.triggerEvent(HubEvent{
		Type:      "client_connected",
		ClientID:  client.id,
		Timestamp: time.Now(),
	})

	logger.Info("WebSocket client connected: id=%s, active=%d", client.id, len(h.clients))
}

// unregisterClient unregisters a client
func (h *Hub) unregisterClient(client *Client) {
	if client == nil {
		return
	}

	h.mu.Lock()
	defer h.mu.Unlock()

	if _, ok := h.clients[client]; ok {
		delete(h.clients, client)
		atomic.AddInt64(&h.stats.ActiveConnections, -1)

		if client.send != nil {
			close(client.send)
		}

		if h.connService != nil {
			if err := h.connService.UnregisterClient(client.id); err != nil {
				logger.Error("Failed to unregister client: %v", err)
			}
		}

		h.triggerEvent(HubEvent{
			Type:      "client_disconnected",
			ClientID:  client.id,
			Timestamp: time.Now(),
		})

		logger.Info("WebSocket client disconnected: id=%s, active=%d", client.id, len(h.clients))
	}
}

// broadcastMessage broadcasts a message to all clients
func (h *Hub) broadcastMessage(message []byte) {
	if message == nil || len(message) == 0 {
		return
	}

	h.mu.RLock()
	clients := make([]*Client, 0, len(h.clients))
	for client := range h.clients {
		clients = append(clients, client)
	}
	h.mu.RUnlock()

	atomic.AddInt64(&h.stats.TotalMessages, 1)
	atomic.AddInt64(&h.stats.TotalBytesSent, int64(len(message)))

	for _, client := range clients {
		select {
		case client.send <- message:
		default:
			// Client's send buffer is full, disconnect them
			logger.Debug("Client send buffer full, disconnecting: %s", client.id)
			go h.unregisterClient(client)
		}
	}

	h.triggerEvent(HubEvent{
		Type:      "broadcast",
		Timestamp: time.Now(),
	})
}

// cleanupInactiveClients removes inactive clients
func (h *Hub) cleanupInactiveClients() {
	h.mu.RLock()
	clients := make([]*Client, 0, len(h.clients))
	for client := range h.clients {
		clients = append(clients, client)
	}
	h.mu.RUnlock()

	for _, client := range clients {
		if client == nil {
			continue
		}
		lastActive := client.GetLastActive()
		if time.Since(lastActive) > 60*time.Second {
			logger.Debug("Removing inactive client: %s (last active: %s)", client.id, lastActive)
			h.unregisterClient(client)
		}
	}
}

// GetStats returns hub statistics
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

// BroadcastToAll broadcasts a message to all connected clients
func (h *Hub) BroadcastToAll(message interface{}) error {
	if message == nil {
		return nil
	}
	data, err := json.Marshal(message)
	if err != nil {
		return err
	}
	select {
	case h.broadcast <- data:
		return nil
	default:
		logger.Warn("Broadcast channel full, dropping message")
		return nil
	}
}

// SendToClient sends a message to a specific client
func (h *Hub) SendToClient(clientID string, message interface{}) error {
	if clientID == "" || message == nil {
		return nil
	}

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
				logger.Debug("Client send buffer full, dropping message: %s", clientID)
				return nil
			}
		}
	}
	return nil
}

// GetConnectedClients returns a list of connected clients
func (h *Hub) GetConnectedClients() []*entity.Client {
	h.mu.RLock()
	defer h.mu.RUnlock()

	clients := make([]*entity.Client, 0, len(h.clients))
	for client := range h.clients {
		if client != nil && client.entity != nil {
			clients = append(clients, client.entity)
		}
	}
	return clients
}

// GetClient returns a client by ID
func (h *Hub) GetClient(clientID string) *Client {
	if clientID == "" {
		return nil
	}
	h.mu.RLock()
	defer h.mu.RUnlock()

	for client := range h.clients {
		if client.id == clientID {
			return client
		}
	}
	return nil
}

// AddEventListener adds an event listener
func (h *Hub) AddEventListener(callback func(event HubEvent)) {
	if callback == nil {
		return
	}
	h.mu.Lock()
	defer h.mu.Unlock()
	h.callbacks = append(h.callbacks, callback)
}

// triggerEvent triggers an event
func (h *Hub) triggerEvent(event HubEvent) {
	h.mu.RLock()
	callbacks := make([]func(HubEvent), len(h.callbacks))
	copy(callbacks, h.callbacks)
	h.mu.RUnlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

// IsRunning returns true if the hub is running
func (h *Hub) IsRunning() bool {
	return !h.stopped
}

// GetClientCount returns the number of connected clients
func (h *Hub) GetClientCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients)
}