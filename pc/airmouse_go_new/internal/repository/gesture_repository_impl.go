package repository

import (
	"encoding/json"
	"errors"
	"sync"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

type gestureRepositoryImpl struct {
	mu        sync.RWMutex
	templates map[string]*entity.GestureTemplate
}

func NewGestureRepository() repository.GestureRepository {
	return &gestureRepositoryImpl{
		templates: make(map[string]*entity.GestureTemplate),
	}
}

func (r *gestureRepositoryImpl) SaveTemplate(template *entity.GestureTemplate) error {
	if template == nil || template.ID == "" {
		return errors.New("invalid template")
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	r.templates[template.ID] = template
	return nil
}

func (r *gestureRepositoryImpl) GetTemplate(idOrName string) (*entity.GestureTemplate, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	if t, ok := r.templates[idOrName]; ok {
		return t, nil
	}
	for _, t := range r.templates {
		if t.Name == idOrName {
			return t, nil
		}
	}
	return nil, nil
}

func (r *gestureRepositoryImpl) ListTemplates(filterType entity.GestureType) ([]*entity.GestureTemplate, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	list := make([]*entity.GestureTemplate, 0)
	for _, t := range r.templates {
		if filterType == "" || t.Type == filterType {
			list = append(list, t)
		}
	}
	return list, nil
}

func (r *gestureRepositoryImpl) DeleteTemplate(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.templates, id)
	return nil
}

func (r *gestureRepositoryImpl) DeleteAllByType(gestureType entity.GestureType) (int, error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	deleted := 0
	for id, t := range r.templates {
		if t.Type == gestureType {
			delete(r.templates, id)
			deleted++
		}
	}
	return deleted, nil
}

func (r *gestureRepositoryImpl) TemplateExists(name string) (bool, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	for _, t := range r.templates {
		if t.Name == name {
			return true, nil
		}
	}
	return false, nil
}

func (r *gestureRepositoryImpl) Count() (int, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return len(r.templates), nil
}

func (r *gestureRepositoryImpl) UpdateMetadata(id string, metadata map[string]interface{}) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	t, ok := r.templates[id]
	if !ok {
		return errors.New("template not found")
	}
	if t.Metadata == nil {
		t.Metadata = make(map[string]interface{})
	}
	for k, v := range metadata {
		t.Metadata[k] = v
	}
	return nil
}
