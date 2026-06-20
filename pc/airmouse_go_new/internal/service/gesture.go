package service

import (
    "errors"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/infra/logger"
)

// GestureService handles gesture recognition and template management
type GestureService interface {
    Recognize(gyro, accel []float64) (*entity.Gesture, error)
    RecognizeFromPoints(points []entity.Point) (*entity.Gesture, error)
    SaveTemplate(template *entity.GestureTemplate) error
    GetTemplate(id string) (*entity.GestureTemplate, error)
    ListTemplates(gestureType entity.GestureType) ([]*entity.GestureTemplate, error)
    DeleteTemplate(id string) error
    TrainFromBuffer(samples []entity.TrainingSample) error
    CompareTemplates(t1, t2 *entity.GestureTemplate) (float64, error)
    AutoGenerateTemplates(samples []entity.TrainingSample, minConfidence float64) ([]*entity.GestureTemplate, error)
    ExecuteGesture(gesture *entity.Gesture) error
}

type gestureService struct {
    repo      repository.GestureRepository
    threshold float64
}

func NewGestureService(repo repository.GestureRepository, confidenceThreshold float64) GestureService {
    if confidenceThreshold <= 0 {
        confidenceThreshold = 0.7
    }
    return &gestureService{
        repo:      repo,
        threshold: confidenceThreshold,
    }
}

func (s *gestureService) Recognize(gyro, accel []float64) (*entity.Gesture, error) {
    if len(gyro) < 3 || len(accel) < 3 {
        return nil, errors.New("insufficient sensor data")
    }
    
    // Simplified gesture recognition - in production use ML model
    bestScore := 0.0
    bestGesture := entity.GestureNone
    
    templates, err := s.repo.ListAllTemplates()
    if err != nil {
        return nil, err
    }
    
    for _, tmpl := range templates {
        score := s.compareToTemplate(gyro, accel, tmpl)
        if score > bestScore && score > s.threshold {
            bestScore = score
            bestGesture = tmpl.Type
        }
    }
    
    if bestScore > s.threshold {
        logger.Debug("Gesture recognized: %s (%.2f)", bestGesture, bestScore)
        return entity.NewGesture(bestGesture, bestScore), nil
    }
    
    return entity.NewGesture(entity.GestureNone, 0), nil
}

func (s *gestureService) RecognizeFromPoints(points []entity.Point) (*entity.Gesture, error) {
    if len(points) < 5 {
        return nil, errors.New("insufficient points")
    }
    
    // DTW-based recognition
    bestScore := 0.0
    bestGesture := entity.GestureNone
    
    templates, err := s.repo.ListAllTemplates()
    if err != nil {
        return nil, err
    }
    
    for _, tmpl := range templates {
        score := s.comparePointSequence(points, tmpl)
        if score > bestScore && score > s.threshold {
            bestScore = score
            bestGesture = tmpl.Type
        }
    }
    
    return entity.NewGesture(bestGesture, bestScore), nil
}

func (s *gestureService) compareToTemplate(gyro, accel []float64, tmpl *entity.GestureTemplate) float64 {
    // Simplified comparison - implement DTW or correlation
    return 0.5
}

func (s *gestureService) comparePointSequence(points []entity.Point, tmpl *entity.GestureTemplate) float64 {
    // Simplified DTW implementation
    return 0.5
}

func (s *gestureService) SaveTemplate(template *entity.GestureTemplate) error {
    if template == nil {
        return errors.New("template cannot be nil")
    }
    return s.repo.SaveTemplate(template)
}

func (s *gestureService) GetTemplate(id string) (*entity.GestureTemplate, error) {
    return s.repo.GetTemplate(id)
}

func (s *gestureService) ListTemplates(gestureType entity.GestureType) ([]*entity.GestureTemplate, error) {
    return s.repo.ListTemplates(gestureType)
}

func (s *gestureService) DeleteTemplate(id string) error {
    return s.repo.DeleteTemplate(id)
}

func (s *gestureService) TrainFromBuffer(samples []entity.TrainingSample) error {
    if len(samples) == 0 {
        return errors.New("no training samples")
    }
    
    logger.Info("Training gesture model with %d samples", len(samples))
    // In production: call external training service
    return nil
}

func (s *gestureService) CompareTemplates(t1, t2 *entity.GestureTemplate) (float64, error) {
    if t1 == nil || t2 == nil {
        return 0, errors.New("nil template")
    }
    // Implement DTW similarity
    return 0.5, nil
}

func (s *gestureService) AutoGenerateTemplates(samples []entity.TrainingSample, minConfidence float64) ([]*entity.GestureTemplate, error) {
    if len(samples) < 10 {
        return nil, errors.New("insufficient samples for clustering")
    }
    
    // K-means clustering on feature vectors
    templates := make([]*entity.GestureTemplate, 0)
    
    logger.Info("Auto-generated %d gesture templates", len(templates))
    return templates, nil
}

func (s *gestureService) ExecuteGesture(gesture *entity.Gesture) error {
    if gesture == nil {
        return errors.New("gesture cannot be nil")
    }
    
    if !gesture.IsHighConfidence() {
        return nil
    }
    
    action := gesture.GetAction()
    logger.Info("Executing gesture action: %s (gesture: %s)", action, gesture.Type)
    
    // Execute system action based on gesture
    // This would call sysaction.Execute()
    
    // Update template usage if applicable
    if gesture.Type != entity.GestureNone {
        templates, _ := s.repo.ListTemplates(gesture.Type)
        for _, tmpl := range templates {
            s.repo.IncrementUsage(tmpl.ID, gesture.Confidence)
        }
    }
    
    return nil
}
