package device

import "time"

type Client struct {
	ID          string
	Name        string
	Type        DeviceType
	ConnectedAt time.Time
	LastSeen    time.Time
	IP          string
	UserAgent   string
}

type ClientManager struct {
	clients map[string]*Client
	mu      sync.RWMutex
}

func NewClientManager() *ClientManager {
	return &ClientManager{clients: make(map[string]*Client)}
}

func (cm *ClientManager) AddClient(client *Client) {
	cm.mu.Lock()
	defer cm.mu.Unlock()
	cm.clients[client.ID] = client
}

func (cm *ClientManager) RemoveClient(id string) {
	cm.mu.Lock()
	defer cm.mu.Unlock()
	delete(cm.clients, id)
}

func (cm *ClientManager) GetClient(id string) *Client {
	cm.mu.RLock()
	defer cm.mu.RUnlock()
	return cm.clients[id]
}

func (cm *ClientManager) GetAllClients() []*Client {
	cm.mu.RLock()
	defer cm.mu.RUnlock()
	cl := make([]*Client, 0, len(cm.clients))
	for _, c := range cm.clients {
		cl = append(cl, c)
	}
	return cl
}