package websocket

import (
    "time"
	"sync"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/service"
)

type HubStats struct {
    StartTime time.Time `json:"start_time"`
    Clients   int       `json:"clients"`
}

type Hub struct {
	clients        map[*Client]bool
	register       chan *Client
	unregister     chan *Client
	broadcast      chan []byte
	mu             sync.RWMutex
	mouseService   service.MouseService
	gestureService service.GestureService
	connService    service.ConnectionService
	startTime      time.Time
}

func NewHub(mouseSvc service.MouseService, gestureSvc service.GestureService, connSvc service.ConnectionService) *Hub {
	return &Hub{
		clients:        make(map[*Client]bool),
		register:       make(chan *Client),
		unregister:     make(chan *Client),
		broadcast:      make(chan []byte),
		mouseService:   mouseSvc,
		gestureService: gestureSvc,
		connService:    connSvc,
		startTime:      time.Now(),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			h.clients[client] = true
			h.mu.Unlock()
			if h.connService != nil {
				_ = h.connService.RegisterClient(client.entity)
			}
		case client := <-h.unregister:
			h.mu.Lock()
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				close(client.send)
				if h.connService != nil {
					_ = h.connService.UnregisterClient(client.id)
				}
			}
			h.mu.Unlock()
		case message := <-h.broadcast:
			h.mu.RLock()
			for client := range h.clients {
				select {
				case client.send <- message:
				default:
					close(client.send)
					delete(h.clients, client)
				}
			}
			h.mu.RUnlock()
		}
	}
}

func (h *Hub) GetStats() HubStats {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return HubStats{
		StartTime: h.startTime,
		Clients:   len(h.clients),
	}
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
