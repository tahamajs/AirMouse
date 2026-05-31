package service

import (
	"errors"
	"sync"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// ConnectionService manages client connections and session state.
type ConnectionService struct {
	repo          repository.ClientRepository
	maxClients    int
	clients       map[string]*entity.Client
	mu            sync.RWMutex
}

// NewConnectionService creates a new connection service.
func NewConnectionService(repo repository.ClientRepository, maxClients int) *ConnectionService {
	return &ConnectionService{
		repo:       repo,
		maxClients: maxClients,
		clients:    make(map[string]*entity.Client),
	}
}

// AddClient registers a new client.
func (s *ConnectionService) AddClient(id, name, clientType string) (*entity.Client, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if len(s.clients) >= s.maxClients {
		return nil, errors.New("maximum number of clients reached")
	}
	if _, exists := s.clients[id]; exists {
		return nil, errors.New("client already exists")
	}
	client := entity.NewClient(id, name, clientType)
	s.clients[id] = client
	if err := s.repo.Save(client); err != nil {
		return nil, err
	}
	return client, nil
}

// RemoveClient disconnects a client.
func (s *ConnectionService) RemoveClient(id string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	client, exists := s.clients[id]
	if !exists {
		return errors.New("client not found")
	}
	delete(s.clients, id)
	return s.repo.Remove(id)
}

// GetClient returns a client by ID.
func (s *ConnectionService) GetClient(id string) (*entity.Client, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	if client, ok := s.clients[id]; ok {
		return client, nil
	}
	return s.repo.FindByID(id)
}

// GetAllClients returns all active clients.
func (s *ConnectionService) GetAllClients() []*entity.Client {
	s.mu.RLock()
	defer s.mu.RUnlock()
	clients := make([]*entity.Client, 0, len(s.clients))
	for _, c := range s.clients {
		clients = append(clients, c)
	}
	return clients
}

// UpdateClientActivity updates the last active timestamp.
func (s *ConnectionService) UpdateClientActivity(id string) error {
	client, err := s.GetClient(id)
	if err != nil {
		return err
	}
	client.UpdateActivity()
	return s.repo.UpdateLastActive(id)
}

// IsClientIdle checks if a client is idle.
func (s *ConnectionService) IsClientIdle(id string, timeout time.Duration) (bool, error) {
	client, err := s.GetClient(id)
	if err != nil {
		return false, err
	}
	return client.IsIdle(timeout), nil
}

// Count returns the number of active clients.
func (s *ConnectionService) Count() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.clients)
}

// SetClientName updates the name of a client.
func (s *ConnectionService) SetClientName(id, name string) error {
	client, err := s.GetClient(id)
	if err != nil {
		return err
	}
	client.Name = name
	return s.repo.Save(client)
}

// SetPaired marks a client as paired (authenticated).
func (s *ConnectionService) SetPaired(id string, paired bool) error {
	client, err := s.GetClient(id)
	if err != nil {
		return err
	}
	client.IsPaired = paired
	return s.repo.Save(client)
}