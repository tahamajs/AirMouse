package websocket

import (
	"sync"

	"airmouse-go/internal/domain/service"
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
	}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			h.clients[client] = true
			h.mu.Unlock()
			_ = h.connService.RegisterClient(client.entity)
		case client := <-h.unregister:
			h.mu.Lock()
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				close(client.send)
				_ = h.connService.UnregisterClient(client.id)
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
