package service

import (
    "errors"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/utils"
)

// Broadcaster defines the ability to send messages to clients.
type Broadcaster interface {
    Broadcast(message []byte) error
    BroadcastTo(status entity.ClientStatus, message []byte) error
}

// ConnectionService manages client lifecycle, heartbeat, and broadcasts.
type ConnectionService interface {
    RegisterClient(client *entity.Client) error
    UnregisterClient(id string) error
    GetClient(id string) (*entity.Client, error)
    ListClients() ([]*entity.Client, error)
    Heartbeat(id string, latencyMs int) error
    UpdateTraffic(id string, sent, recv int64) error
    Broadcast(message []byte) error
    BroadcastTo(status entity.ClientStatus, message []byte) error
    CountActive() (int, error)
    PruneIdle(maxIdle time.Duration) (int, error)
    GetClientHealth(id string) (map[string]interface{}, error)
}

type connectionService struct {
    repo        repository.ClientRepository
    broadcaster Broadcaster
    mu          sync.RWMutex
}

func NewConnectionService(repo repository.ClientRepository, broadcaster Broadcaster) ConnectionService {
    return &connectionService{
        repo:        repo,
        broadcaster: broadcaster,
    }
}

func (s *connectionService) RegisterClient(client *entity.Client) error {
    if client == nil {
        return errors.New("client cannot be nil")
    }
    if client.ID == "" {
        return errors.New("client ID cannot be empty")
    }
    // Check if client already exists
    exists, _ := s.repo.Exists(client.ID)
    if exists {
        return errors.New("client already registered")
    }
    // Set initial timestamps
    now := time.Now()
    client.ConnectedAt = now
    client.LastActive = now
    client.Status = entity.StatusConnected
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
    if s.broadcaster == nil {
        return errors.New("broadcaster not set")
    }
    return s.broadcaster.Broadcast(message)
}

func (s *connectionService) BroadcastTo(status entity.ClientStatus, message []byte) error {
    if s.broadcaster == nil {
        return errors.New("broadcaster not set")
    }
    return s.broadcaster.BroadcastTo(status, message)
}

func (s *connectionService) CountActive() (int, error) {
    return s.repo.CountActive()
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
        "jitter":        client.Jitter,
        "packet_loss":   client.PacketLoss,
        "status":        client.Status,
        "is_healthy":    client.IsHealthy(),
    }, nil
}