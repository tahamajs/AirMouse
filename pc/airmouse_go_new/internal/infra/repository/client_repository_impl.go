package repository

import (
    "fmt"
    "sort"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/utils"
)

type clientRepositoryImpl struct {
    mu        sync.RWMutex
    clients   map[string]*entity.Client
    blocked   map[string]bool
    callbacks []func(event ClientEvent)
}

type ClientEvent struct {
    Type      string // "connected", "disconnected", "updated"
    ClientID  string
    ClientName string
    Timestamp time.Time
}

func NewClientRepository() repository.ClientRepository {
    repo := &clientRepositoryImpl{
        clients:   make(map[string]*entity.Client),
        blocked:   make(map[string]bool),
        callbacks: make([]func(ClientEvent), 0),
    }
    go repo.cleanupLoop()
    return repo
}

func (r *clientRepositoryImpl) Add(client *entity.Client) error {
    if client == nil {
        return fmt.Errorf("client cannot be nil")
    }
    if client.ID == "" {
        return fmt.Errorf("client ID cannot be empty")
    }
    r.mu.Lock()
    defer r.mu.Unlock()
    if _, exists := r.clients[client.ID]; exists {
        return fmt.Errorf("client already exists: %s", client.ID)
    }
    if r.blocked[client.ID] {
        return fmt.Errorf("client is blocked: %s", client.ID)
    }
    now := time.Now()
    client.ConnectedAt = now
    client.LastActive = now
    client.Status = entity.StatusConnected
    r.clients[client.ID] = client
    r.triggerCallback(ClientEvent{
        Type:       "connected",
        ClientID:   client.ID,
        ClientName: client.Name,
        Timestamp:  now,
    })
    utils.LogInfo("Client connected: %s (%s)", client.Name, client.ID)
    return nil
}

func (r *clientRepositoryImpl) Remove(id string) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    client, exists := r.clients[id]
    if !exists {
        return fmt.Errorf("client not found: %s", id)
    }
    delete(r.clients, id)
    r.triggerCallback(ClientEvent{
        Type:       "disconnected",
        ClientID:   id,
        ClientName: client.Name,
        Timestamp:  time.Now(),
    })
    utils.LogInfo("Client disconnected: %s (%s)", client.Name, id)
    return nil
}

func (r *clientRepositoryImpl) Get(id string) (*entity.Client, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    client, exists := r.clients[id]
    if !exists {
        return nil, fmt.Errorf("client not found: %s", id)
    }
    return client, nil
}

func (r *clientRepositoryImpl) GetByName(name string) (*entity.Client, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    for _, c := range r.clients {
        if c.Name == name {
            return c, nil
        }
    }
    return nil, nil
}

func (r *clientRepositoryImpl) List() ([]*entity.Client, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    list := make([]*entity.Client, 0, len(r.clients))
    for _, c := range r.clients {
        list = append(list, c)
    }
    sort.Slice(list, func(i, j int) bool {
        return list[i].ConnectedAt.After(list[j].ConnectedAt)
    })
    return list, nil
}

func (r *clientRepositoryImpl) ListByStatus(status entity.ClientStatus) ([]*entity.Client, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    list := make([]*entity.Client, 0)
    for _, c := range r.clients {
        if c.Status == status {
            list = append(list, c)
        }
    }
    return list, nil
}

func (r *clientRepositoryImpl) ListActive() ([]*entity.Client, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    active := make([]*entity.Client, 0)
    for _, c := range r.clients {
        if c.Status == entity.StatusConnected && time.Since(c.LastActive) < 30*time.Second {
            active = append(active, c)
        }
    }
    return active, nil
}

func (r *clientRepositoryImpl) Count() (int, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    return len(r.clients), nil
}

func (r *clientRepositoryImpl) CountActive() (int, error) {
    active, err := r.ListActive()
    if err != nil {
        return 0, err
    }
    return len(active), nil
}

func (r *clientRepositoryImpl) UpdateLastActive(id string) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    if c, ok := r.clients[id]; ok {
        c.UpdateActivity()
        return nil
    }
    return fmt.Errorf("client not found: %s", id)
}

func (r *clientRepositoryImpl) UpdateHeartbeat(id string, latencyMs int) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    if c, ok := r.clients[id]; ok {
        c.UpdateHeartbeat(time.Duration(latencyMs) * time.Millisecond)
        return nil
    }
    return fmt.Errorf("client not found: %s", id)
}

func (r *clientRepositoryImpl) UpdateBytes(id string, sent, recv int64) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    if c, ok := r.clients[id]; ok {
        c.AddTraffic(sent, recv)
        return nil
    }
    return fmt.Errorf("client not found: %s", id)
}

func (r *clientRepositoryImpl) UpdateStatus(id string, status entity.ClientStatus) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    if c, ok := r.clients[id]; ok {
        c.SetStatus(status)
        r.triggerCallback(ClientEvent{
            Type:       "updated",
            ClientID:   id,
            ClientName: c.Name,
            Timestamp:  time.Now(),
        })
        return nil
    }
    return fmt.Errorf("client not found: %s", id)
}

func (r *clientRepositoryImpl) UpdateCapabilities(id string, caps entity.ClientCapabilities) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    if c, ok := r.clients[id]; ok {
        c.SetCapabilities(caps)
        return nil
    }
    return fmt.Errorf("client not found: %s", id)
}

func (r *clientRepositoryImpl) BlockDevice(id string) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    r.blocked[id] = true
    if c, ok := r.clients[id]; ok {
        c.SetStatus(entity.StatusDisconnected)
    }
    return nil
}

func (r *clientRepositoryImpl) IsBlocked(id string) bool {
    r.mu.RLock()
    defer r.mu.RUnlock()
    return r.blocked[id]
}

func (r *clientRepositoryImpl) PruneInactive(maxIdle time.Duration) (int, error) {
    r.mu.Lock()
    defer r.mu.Unlock()
    removed := 0
    for id, c := range r.clients {
        if time.Since(c.LastActive) > maxIdle {
            delete(r.clients, id)
            removed++
            r.triggerCallback(ClientEvent{
                Type:       "disconnected",
                ClientID:   id,
                ClientName: c.Name,
                Timestamp:  time.Now(),
            })
        }
    }
    if removed > 0 {
        utils.LogInfo("Pruned %d inactive clients", removed)
    }
    return removed, nil
}

func (r *clientRepositoryImpl) Exists(id string) (bool, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    _, ok := r.clients[id]
    return ok, nil
}

func (r *clientRepositoryImpl) triggerCallback(event ClientEvent) {
    r.mu.RLock()
    callbacks := make([]func(ClientEvent), len(r.callbacks))
    copy(callbacks, r.callbacks)
    r.mu.RUnlock()
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (r *clientRepositoryImpl) cleanupLoop() {
    ticker := time.NewTicker(60 * time.Second)
    defer ticker.Stop()
    for range ticker.C {
        r.PruneInactive(5 * time.Minute)
    }
}