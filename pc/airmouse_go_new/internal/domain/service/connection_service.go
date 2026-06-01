package service

import (
	"errors"
	"sync"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// ConnectionService manages client lifecycle, heartbeat, and broadcasts.
type ConnectionService interface {
	// RegisterClient adds a new client.
	RegisterClient(client *entity.Client) error

	// UnregisterClient removes a client.
	UnregisterClient(id string) error

	// GetClient returns client information.
	GetClient(id string) (*entity.Client, error)

	// ListClients returns all active clients.
	ListClients() ([]*entity.Client, error)

	// Heartbeat updates a client's last active time and optionally latency.
	Heartbeat(id string, latencyMs int) error

	// UpdateTraffic increments byte counters.
	UpdateTraffic(id string, sent, recv int64) error

	// Broadcast sends a message to all connected clients.
	Broadcast(message []byte) error

	// BroadcastTo sends a message to clients matching a specific status.
	BroadcastTo(status entity.ClientStatus, message []byte) error

	// CountActive returns the number of clients with status "connected".
	CountActive() (int, error)

	// PruneIdle removes clients that have been idle for a given duration.
	PruneIdle(maxIdle time.Duration) (int, error)

	// GetClientHealth returns a health summary (average latency, jitter, etc.).
	GetClientHealth(id string) (map[string]interface{}, error)
}

type connectionService struct {
	repo repository.ClientRepository
	mu   sync.RWMutex
}

func NewConnectionService(repo repository.ClientRepository) ConnectionService {
	return &connectionService{repo: repo}
}

func (s *connectionService) RegisterClient(client *entity.Client) error {
	return s.repo.Add(client)
}

func (s *connectionService) UnregisterClient(id string) error {
	return s.repo.Remove(id)
}

func (s *connectionService) GetClient(id string) (*entity.Client, error) {
	return s.repo.Get(id)
}

func (s *connectionService) ListClients() ([]*entity.Client, error) {
	return s.repo.List()
}

func (s *connectionService) Heartbeat(id string, latencyMs int) error {
	if err := s.repo.UpdateHeartbeat(id, latencyMs); err != nil {
		return err
	}
	return s.repo.UpdateLastActive(id)
}

func (s *connectionService) UpdateTraffic(id string, sent, recv int64) error {
	return s.repo.UpdateBytes(id, sent, recv)
}

func (s *connectionService) Broadcast(message []byte) error {
	_, err := s.repo.List()
	if err != nil {
		return err
	}
	// In real implementation, you would push to each client’s write channel.
	// Here we simulate a no‑op.
	return nil
}

func (s *connectionService) BroadcastTo(status entity.ClientStatus, message []byte) error {
	clients, err := s.repo.ListByStatus(status)
	if err != nil {
		return err
	}
	_ = clients
	return nil
}

func (s *connectionService) CountActive() (int, error) {
	clients, err := s.repo.ListByStatus(entity.StatusConnected)
	if err != nil {
		return 0, err
	}
	return len(clients), nil
}

func (s *connectionService) PruneIdle(maxIdle time.Duration) (int, error) {
	return s.repo.PruneInactive(maxIdle)
}

func (s *connectionService) GetClientHealth(id string) (map[string]interface{}, error) {
	client, err := s.repo.Get(id)
	if err != nil || client == nil {
		return nil, errors.New("client not found")
	}
	return map[string]interface{}{
		"ping_ms":       client.PingLatency.Milliseconds(),
		"last_active":   client.LastActive.Unix(),
		"connected_at":  client.ConnectedAt.Unix(),
		"messages_sent": client.MessagesSent,
		"messages_recv": client.MessagesRecv,
	}, nil
}
