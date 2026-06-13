package service

import (
    "errors"
    "fmt"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/infra/logger"
)

// ConnectionService manages client lifecycle, heartbeat, and broadcasts
type ConnectionService interface {
    RegisterClient(client *entity.Client) error
    UnregisterClient(id string) error
    GetClient(id string) (*entity.Client, error)
    GetClientByName(name string) (*entity.Client, error)
    ListClients() ([]*entity.Client, error)
    ListActiveClients() ([]*entity.Client, error)
    Heartbeat(id string, latencyMs int) error
    UpdateTraffic(id string, sent, recv int64) error
    UpdateCapabilities(id string, caps entity.ClientCapabilities) error
    Broadcast(message []byte) error
    BroadcastTo(status entity.ClientStatus, message []byte) error
    BroadcastExcept(exceptID string, message []byte) error
    CountActive() (int, error)
    PruneIdle(maxIdle time.Duration) (int, error)
    GetClientHealth(id string) (map[string]interface{}, error)
    BlockClient(id string) error
    UnblockClient(id string) error
}

type connectionService struct {
    repo repository.ClientRepository
    mu   sync.RWMutex
}

func NewConnectionService(repo repository.ClientRepository) ConnectionService {
    return &connectionService{repo: repo}
}

func (s *connectionService) RegisterClient(client *entity.Client) error {
    if client == nil {
        return errors.New("client cannot be nil")
    }
    if exists, _ := s.repo.Exists(client.ID); exists {
        return fmt.Errorf("client already exists: %s", client.ID)
    }
    if s.repo.IsBlocked(client.ID) {
        return fmt.Errorf("client is blocked: %s", client.ID)
    }
    logger.Info("Client registered: %s (%s)", client.ID, client.RemoteAddr)
    return s.repo.Add(client)
}

func (s *connectionService) UnregisterClient(id string) error {
    logger.Info("Client unregistered: %s", id)
    return s.repo.Remove(id)
}

func (s *connectionService) GetClient(id string) (*entity.Client, error) {
    if s.repo.IsBlocked(id) {
        return nil, errors.New("client is blocked")
    }
    return s.repo.Get(id)
}

func (s *connectionService) GetClientByName(name string) (*entity.Client, error) {
    return s.repo.GetByName(name)
}

func (s *connectionService) ListClients() ([]*entity.Client, error) {
    return s.repo.List()
}

func (s *connectionService) ListActiveClients() ([]*entity.Client, error) {
    return s.repo.ListActive()
}

func (s *connectionService) Heartbeat(id string, latencyMs int) error {
    if s.repo.IsBlocked(id) {
        return errors.New("client is blocked")
    }
    if err := s.repo.UpdateHeartbeat(id, latencyMs); err != nil {
        return err
    }
    return s.repo.UpdateLastActive(id)
}

func (s *connectionService) UpdateTraffic(id string, sent, recv int64) error {
    if s.repo.IsBlocked(id) {
        return errors.New("client is blocked")
    }
    return s.repo.UpdateBytes(id, sent, recv)
}

func (s *connectionService) UpdateCapabilities(id string, caps entity.ClientCapabilities) error {
    if s.repo.IsBlocked(id) {
        return errors.New("client is blocked")
    }
    return s.repo.UpdateCapabilities(id, caps)
}

func (s *connectionService) Broadcast(message []byte) error {
    clients, err := s.repo.List()
    if err != nil {
        return err
    }
    // In real implementation, send to each client's write channel
    logger.Debug("Broadcasting to %d clients", len(clients))
    return nil
}

func (s *connectionService) BroadcastTo(status entity.ClientStatus, message []byte) error {
    clients, err := s.repo.ListByStatus(status)
    if err != nil {
        return err
    }
    logger.Debug("Broadcasting to %d clients with status %s", len(clients), status)
    return nil
}

func (s *connectionService) BroadcastExcept(exceptID string, message []byte) error {
    clients, err := s.repo.List()
    if err != nil {
        return err
    }
    count := 0
    for _, c := range clients {
        if c.ID != exceptID {
            count++
        }
    }
    logger.Debug("Broadcasting to %d clients (excluding %s)", count, exceptID)
    return nil
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
        "connected":      client.IsActive(),
        "healthy":        client.IsHealthy(),
        "ping_ms":        client.PingLatency.Milliseconds(),
        "jitter":         client.Jitter,
        "last_active":    client.LastActive.Unix(),
        "connected_at":   client.ConnectedAt.Unix(),
        "messages_sent":  client.MessagesSent,
        "messages_recv":  client.MessagesRecv,
        "bytes_sent":     client.BytesSent,
        "bytes_recv":     client.BytesRecv,
        "packet_loss":    client.PacketLoss,
        "errors":         client.Errors,
    }, nil
}

func (s *connectionService) BlockClient(id string) error {
    return s.repo.BlockDevice(id)
}

func (s *connectionService) UnblockClient(id string) error {
    return s.repo.UpdateStatus(id, entity.StatusConnected)
}