package repository

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"

	"airmouse-go/internal/domain/entity"
)

type MouseRepositoryImpl struct {
	filePath string
	mu       sync.RWMutex
	stats    struct {
		Clicks  int64 `json:"clicks"`
		Double  int64 `json:"double"`
		Right   int64 `json:"right"`
		Scroll  int64 `json:"scroll"`
	}
}

func NewMouseRepository() *MouseRepositoryImpl {
	configDir, _ := os.UserConfigDir()
	path := filepath.Join(configDir, "airmouse", "mouse_stats.json")
	repo := &MouseRepositoryImpl{filePath: path}
	repo.loadStats()
	return repo
}

func (r *MouseRepositoryImpl) Save(mouse *entity.Mouse) error {
	// Mouse state is ephemeral – not persisted.
	return nil
}

func (r *MouseRepositoryImpl) Load() (*entity.Mouse, error) {
	return entity.NewMouse(0.5), nil
}

func (r *MouseRepositoryImpl) GetStats() (clicks, double, right, scroll int64, err error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return r.stats.Clicks, r.stats.Double, r.stats.Right, r.stats.Scroll, nil
}

func (r *MouseRepositoryImpl) IncrementClick() error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.stats.Clicks++
	return r.saveStats()
}

func (r *MouseRepositoryImpl) IncrementDoubleClick() error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.stats.Double++
	return r.saveStats()
}

func (r *MouseRepositoryImpl) IncrementRightClick() error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.stats.Right++
	return r.saveStats()
}

func (r *MouseRepositoryImpl) IncrementScroll() error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.stats.Scroll++
	return r.saveStats()
}

func (r *MouseRepositoryImpl) loadStats() {
	data, err := os.ReadFile(r.filePath)
	if err != nil {
		return
	}
	_ = json.Unmarshal(data, &r.stats)
}

func (r *MouseRepositoryImpl) saveStats() error {
	data, err := json.MarshalIndent(r.stats, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(r.filePath, data, 0644)
}