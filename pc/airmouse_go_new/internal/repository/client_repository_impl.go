package repository

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"

	"airmouse-go/internal/domain/entity"
)

type ClientRepositoryImpl struct {
	filePath string
	clients  map[string]*entity.Client
	mu       sync.RWMutex
}

func NewClientRepository() *ClientRepositoryImpl {
	configDir, _ := os.UserConfigDir()
	path := filepath.Join(configDir, "airmouse", "clients.json")
	repo := &ClientRepositoryImpl{
		filePath: path,
		clients:  make(map[string]*entity.Client),
	}
	repo.load()
	return repo
}

func (r *ClientRepositoryImpl) Save(client *entity.Client) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.clients[client.ID] = client
	return r.save()
}

func (r *ClientRepositoryImpl) FindByID(id string) (*entity.Client, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	client, ok := r.clients[id]
	if !ok {
		return nil, nil
	}
	return client, nil
}

func (r *ClientRepositoryImpl) FindAll() ([]*entity.Client, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	list := make([]*entity.Client, 0, len(r.clients))
	for _, c := range r.clients {
		list = append(list, c)
	}
	return list, nil
}

func (r *ClientRepositoryImpl) Remove(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.clients, id)
	return r.save()
}

func (r *ClientRepositoryImpl) UpdateLastActive(id string) error {
	client, err := r.FindByID(id)
	if err != nil || client == nil {
		return err
	}
	client.UpdateActivity()
	return r.Save(client)
}

func (r *ClientRepositoryImpl) load() {
	data, err := os.ReadFile(r.filePath)
	if err != nil {
		return
	}
	_ = json.Unmarshal(data, &r.clients)
}

func (r *ClientRepositoryImpl) save() error {
	data, err := json.MarshalIndent(r.clients, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(r.filePath, data, 0644)
}