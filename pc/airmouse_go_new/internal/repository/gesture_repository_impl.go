package repository

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type gestureData struct {
	Templates map[string][]float64 `json:"templates"`
	Thresholds struct {
		Click  float64 `json:"click"`
		Scroll float64 `json:"scroll"`
		Tilt   float64 `json:"tilt"`
	} `json:"thresholds"`
}

type GestureRepositoryImpl struct {
	filePath string
	data     gestureData
	mu       sync.RWMutex
}

func NewGestureRepository() *GestureRepositoryImpl {
	configDir, _ := os.UserConfigDir()
	path := filepath.Join(configDir, "airmouse", "gestures.json")
	repo := &GestureRepositoryImpl{filePath: path}
	repo.load()
	return repo
}

func (r *GestureRepositoryImpl) SaveCustomGesture(name string, template []float64) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.data.Templates == nil {
		r.data.Templates = make(map[string][]float64)
	}
	r.data.Templates[name] = template
	return r.save()
}

func (r *GestureRepositoryImpl) LoadCustomGesture(name string) ([]float64, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	tmpl, ok := r.data.Templates[name]
	if !ok {
		return nil, nil
	}
	return tmpl, nil
}

func (r *GestureRepositoryImpl) ListCustomGestures() ([]string, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	names := make([]string, 0, len(r.data.Templates))
	for n := range r.data.Templates {
		names = append(names, n)
	}
	return names, nil
}

func (r *GestureRepositoryImpl) DeleteCustomGesture(name string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.data.Templates, name)
	return r.save()
}

func (r *GestureRepositoryImpl) GetGestureThresholds() (click, scroll, tilt float64, err error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return r.data.Thresholds.Click, r.data.Thresholds.Scroll, r.data.Thresholds.Tilt, nil
}

func (r *GestureRepositoryImpl) SetGestureThresholds(click, scroll, tilt float64) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.data.Thresholds.Click = click
	r.data.Thresholds.Scroll = scroll
	r.data.Thresholds.Tilt = tilt
	return r.save()
}

func (r *GestureRepositoryImpl) load() {
	data, err := os.ReadFile(r.filePath)
	if err != nil {
		// Default values
		r.data.Thresholds.Click = 10.0
		r.data.Thresholds.Scroll = 5.0
		r.data.Thresholds.Tilt = 15.0
		r.data.Templates = make(map[string][]float64)
		return
	}
	_ = json.Unmarshal(data, &r.data)
}

func (r *GestureRepositoryImpl) save() error {
	data, err := json.MarshalIndent(r.data, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(r.filePath, data, 0644)
}