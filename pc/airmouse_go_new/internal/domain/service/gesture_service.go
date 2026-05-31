package service

import (
	"errors"
	"sort"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// GestureService handles gesture recognition and template management.
type GestureService interface {
	// Recognize uses the current motion data to detect a gesture.
	Recognize(gyro, accel []float64) (*entity.Gesture, error)

	// RecognizeFromPoints matches a list of points against templates.
	RecognizeFromPoints(points []entity.Point) (*entity.Gesture, error)

	// SaveTemplate stores a new gesture template.
	SaveTemplate(template *entity.GestureTemplate) error

	// GetTemplate retrieves a template by ID.
	GetTemplate(id string) (*entity.GestureTemplate, error)

	// ListTemplates returns all templates, optionally filtered by type.
	ListTemplates(gestureType entity.GestureType) ([]*entity.GestureTemplate, error)

	// DeleteTemplate removes a template.
	DeleteTemplate(id string) error

	// TrainFromBuffer performs online training using recent movement samples.
	TrainFromBuffer(samples []entity.TrainingSample) error

	// CompareTemplates returns a similarity score (0‑1) between two templates.
	CompareTemplates(t1, t2 *entity.GestureTemplate) (float64, error)

	// AutoGenerateTemplates clusters similar unlabelled samples into new templates.
	AutoGenerateTemplates(samples []entity.TrainingSample, minConfidence float64) ([]*entity.GestureTemplate, error)
}

type gestureService struct {
	repo      repository.GestureRepository
	threshold float64
}

func NewGestureService(repo repository.GestureRepository, confidenceThreshold float64) GestureService {
	return &gestureService{
		repo:      repo,
		threshold: confidenceThreshold,
	}
}

// Recognize stubbed – in production you would call a TensorFlow Lite model.
func (s *gestureService) Recognize(gyro, accel []float64) (*entity.Gesture, error) {
	// Placeholder: return "none" gesture
	return &entity.Gesture{
		Type:       entity.GestureNone,
		Confidence: 0.0,
		Timestamp:  time.Now().UnixMilli(),
	}, nil
}

func (s *gestureService) RecognizeFromPoints(points []entity.Point) (*entity.Gesture, error) {
	if len(points) < 5 {
		return nil, errors.New("insufficient points")
	}
	// Template matching logic would go here.
	return &entity.Gesture{
		Type:       entity.GestureNone,
		Confidence: 0.0,
		Timestamp:  time.Now().UnixMilli(),
	}, nil
}

func (s *gestureService) SaveTemplate(template *entity.GestureTemplate) error {
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
	// In real implementation: collect samples, call external Python trainer,
	// then update the local model.
	return nil
}

func (s *gestureService) CompareTemplates(t1, t2 *entity.GestureTemplate) (float64, error) {
	if t1 == nil || t2 == nil {
		return 0, errors.New("nil template")
	}
	// Use dynamic time warping or cosine similarity on feature vectors.
	return 0.5, nil
}

func (s *gestureService) AutoGenerateTemplates(samples []entity.TrainingSample, minConfidence float64) ([]*entity.GestureTemplate, error) {
	// Clustering (k‑means) on feature vectors.
	return nil, nil
}