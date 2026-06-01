package repository

import (
	"sync"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

type clientRepositoryImpl struct {
	mu      sync.RWMutex
	clients map[string]*entity.Client
}

func NewClientRepository() repository.ClientRepository {
	return &clientRepositoryImpl{
		clients: make(map[string]*entity.Client),
	}
}

func (r *clientRepositoryImpl) Add(client *entity.Client) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.clients[client.ID] = client
	return nil
}

func (r *clientRepositoryImpl) Remove(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.clients, id)
	return nil
}

func (r *clientRepositoryImpl) Get(id string) (*entity.Client, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return r.clients[id], nil
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

func (r *clientRepositoryImpl) Count() (int, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return len(r.clients), nil
}

func (r *clientRepositoryImpl) UpdateLastActive(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if c, ok := r.clients[id]; ok {
		c.UpdateActivity()
	}
	return nil
}

func (r *clientRepositoryImpl) UpdateHeartbeat(id string, latencyMs int) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if c, ok := r.clients[id]; ok {
		c.UpdateHeartbeat(time.Duration(latencyMs) * time.Millisecond)
	}
	return nil
}

func (r *clientRepositoryImpl) UpdateBytes(id string, sent, recv int64) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if c, ok := r.clients[id]; ok {
		c.AddTraffic(sent, recv)
	}
	return nil
}

func (r *clientRepositoryImpl) UpdateStatus(id string, status entity.ClientStatus) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if c, ok := r.clients[id]; ok {
		c.SetStatus(status)
	}
	return nil
}

func (r *clientRepositoryImpl) PruneInactive(maxIdle time.Duration) (int, error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	removed := 0
	for id, c := range r.clients {
		if time.Since(c.LastActive) > maxIdle {
			delete(r.clients, id)
			removed++
		}
	}
	return removed, nil
}

func (r *clientRepositoryImpl) Exists(id string) (bool, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	_, ok := r.clients[id]
	return ok, nil
}
