package repository

import (
    "encoding/json"
    "errors"
    "fmt"
    "os"
    "sort"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/utils"
)

type gestureRepositoryImpl struct {
    mu          sync.RWMutex
    templates   map[string]*entity.GestureTemplate
    trainingData []GestureTrainingSample
    maxSamples  int
    callbacks   []func(event GestureEvent)
}

type GestureTrainingSample struct {
    ID         string
    GestureID  string
    Data       []float64
    Timestamp  time.Time
    Confidence float64
}

type GestureEvent struct {
    Type       string
    GestureID  string
    GestureName string
    Confidence float64
    Timestamp  time.Time
}

func NewGestureRepository() repository.GestureRepository {
    return &gestureRepositoryImpl{
        templates:    make(map[string]*entity.GestureTemplate),
        trainingData: make([]GestureTrainingSample, 0, 10000),
        maxSamples:   10000,
        callbacks:    make([]func(event GestureEvent), 0),
    }
}

func (r *gestureRepositoryImpl) SaveTemplate(template *entity.GestureTemplate) error {
    if template == nil {
        return errors.New("template cannot be nil")
    }
    if template.ID == "" {
        template.ID = utils.GenerateID()
    }
    if template.Name == "" {
        return errors.New("template name cannot be empty")
    }
    if template.CreatedAt.IsZero() {
        template.CreatedAt = time.Now()
    }
    template.UpdatedAt = time.Now()
    
    r.mu.Lock()
    defer r.mu.Unlock()
    
    // Check for duplicate name
    for id, t := range r.templates {
        if t.Name == template.Name && id != template.ID {
            return fmt.Errorf("template with name '%s' already exists", template.Name)
        }
    }
    
    r.templates[template.ID] = template
    
    // Trigger callback
    r.triggerCallback(GestureEvent{
        Type:       "saved",
        GestureID:  template.ID,
        GestureName: template.Name,
        Timestamp:  time.Now(),
    })
    
    utils.LogInfo(fmt.Sprintf("Gesture template saved: %s (%s)", template.Name, template.ID))
    return nil
}

func (r *gestureRepositoryImpl) GetTemplate(idOrName string) (*entity.GestureTemplate, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    
    // Try by ID first
    if t, ok := r.templates[idOrName]; ok {
        return t, nil
    }
    
    // Then by name (case-insensitive)
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
    
    // Sort by name
    sort.Slice(list, func(i, j int) bool {
        return list[i].Name < list[j].Name
    })
    
    return list, nil
}

func (r *gestureRepositoryImpl) DeleteTemplate(id string) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    template, exists := r.templates[id]
    if !exists {
        return errors.New("template not found")
    }
    
    delete(r.templates, id)
    
    r.triggerCallback(GestureEvent{
        Type:       "deleted",
        GestureID:  id,
        GestureName: template.Name,
        Timestamp:  time.Now(),
    })
    
    utils.LogInfo(fmt.Sprintf("Gesture template deleted: %s (%s)", template.Name, id))
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
    
    utils.LogInfo(fmt.Sprintf("Deleted %d gesture templates of type %s", deleted, gestureType))
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
    
    t.UpdatedAt = time.Now()
    return nil
}

// Additional methods for gesture recognition
func (r *gestureRepositoryImpl) AddTrainingSample(gestureID string, data []float64, confidence float64) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    sample := GestureTrainingSample{
        ID:         utils.GenerateID(),
        GestureID:  gestureID,
        Data:       data,
        Timestamp:  time.Now(),
        Confidence: confidence,
    }
    
    r.trainingData = append(r.trainingData, sample)
    
    // Trim if exceeds max
    if len(r.trainingData) > r.maxSamples {
        r.trainingData = r.trainingData[len(r.trainingData)-r.maxSamples:]
    }
    
    return nil
}

func (r *gestureRepositoryImpl) GetTrainingSamples(gestureID string, limit int) ([]GestureTrainingSample, error) {
    r.mu.RLock()
    defer r.mu.RUnlock()
    
    samples := make([]GestureTrainingSample, 0)
    for _, sample := range r.trainingData {
        if sample.GestureID == gestureID {
            samples = append(samples, sample)
            if limit > 0 && len(samples) >= limit {
                break
            }
        }
    }
    
    return samples, nil
}

func (r *gestureRepositoryImpl) ClearTrainingData(gestureID string) error {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    newData := make([]GestureTrainingSample, 0)
    for _, sample := range r.trainingData {
        if sample.GestureID != gestureID {
            newData = append(newData, sample)
        }
    }
    r.trainingData = newData
    
    return nil
}

func (r *gestureRepositoryImpl) ExportTemplates(filePath string) error {
    r.mu.RLock()
    defer r.mu.RUnlock()
    
    data, err := json.MarshalIndent(r.templates, "", "  ")
    if err != nil {
        return fmt.Errorf("failed to marshal templates: %w", err)
    }
    
    if err := os.WriteFile(filePath, data, 0644); err != nil {
        return fmt.Errorf("failed to write file: %w", err)
    }
    
    utils.LogInfo(fmt.Sprintf("Exported %d gesture templates to %s", len(r.templates), filePath))
    return nil
}

func (r *gestureRepositoryImpl) ImportTemplates(filePath string) error {
    data, err := os.ReadFile(filePath)
    if err != nil {
        return fmt.Errorf("failed to read file: %w", err)
    }
    
    var templates map[string]*entity.GestureTemplate
    if err := json.Unmarshal(data, &templates); err != nil {
        return fmt.Errorf("failed to unmarshal templates: %w", err)
    }
    
    r.mu.Lock()
    defer r.mu.Unlock()
    
    for id, template := range templates {
        r.templates[id] = template
    }
    
    utils.LogInfo(fmt.Sprintf("Imported %d gesture templates from %s", len(templates), filePath))
    return nil
}

func (r *gestureRepositoryImpl) AddEventListener(callback func(event GestureEvent)) {
    r.mu.Lock()
    defer r.mu.Unlock()
    r.callbacks = append(r.callbacks, callback)
}

func (r *gestureRepositoryImpl) triggerCallback(event GestureEvent) {
    r.mu.RLock()
    callbacks := make([]func(GestureEvent), len(r.callbacks))
    copy(callbacks, r.callbacks)
    r.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (r *gestureRepositoryImpl) GetStatistics() map[string]interface{} {
    r.mu.RLock()
    defer r.mu.RUnlock()
    
    typeCount := make(map[entity.GestureType]int)
    for _, t := range r.templates {
        typeCount[t.Type]++
    }
    
    return map[string]interface{}{
        "total_templates":   len(r.templates),
        "total_training":    len(r.trainingData),
        "by_type":          typeCount,
        "last_updated":     time.Now(),
    }
}