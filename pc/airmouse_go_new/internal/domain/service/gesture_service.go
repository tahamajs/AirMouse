package service

import (
    "errors"
    "math"
    "sort"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/utils"
)

// GestureService handles gesture recognition and template management.
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
    recognizer *gestureRecognizer
    mu         sync.Mutex
}

// Simple in‑memory gesture recognizer using DTW (Dynamic Time Warping)
type gestureRecognizer struct {
    templates []entity.GestureTemplate
    mu        sync.RWMutex
}

func newGestureRecognizer() *gestureRecognizer {
    return &gestureRecognizer{
        templates: make([]entity.GestureTemplate, 0),
    }
}

func (gr *gestureRecognizer) loadTemplates(templates []*entity.GestureTemplate) {
    gr.mu.Lock()
    defer gr.mu.Unlock()
    gr.templates = make([]entity.GestureTemplate, len(templates))
    for i, t := range templates {
        gr.templates[i] = *t
    }
}

func (gr *gestureRecognizer) recognize(points []entity.Point) (string, float64) {
    gr.mu.RLock()
    defer gr.mu.RUnlock()
    if len(points) < 3 || len(gr.templates) == 0 {
        return "none", 0.0
    }

    // Extract feature vector: normalized path
    feature := extractFeatures(points)

    bestScore := -1.0
    bestName := "none"

    for _, tmpl := range gr.templates {
        // Assume template.Data contains serialized feature vectors (simplified)
        // For this demo, we treat template.Data as a slice of float64
        // In production you'd use protobuf or JSON.
        var tmplFeatures []float64
        // If the data is stored as bytes, decode it (here we just skip)
        // For simplicity, we'll compare the raw points using DTW.
        score := dtwDistance(points, tmpl)
        confidence := 1.0 / (1.0 + score) // higher score => lower confidence
        if confidence > bestScore {
            bestScore = confidence
            bestName = tmpl.Name
        }
    }

    return bestName, bestScore
}

// extractFeatures: compute direction and speed from points
func extractFeatures(points []entity.Point) []float64 {
    if len(points) < 2 {
        return nil
    }
    features := make([]float64, 0, (len(points)-1)*2)
    for i := 1; i < len(points); i++ {
        dx := points[i].X - points[i-1].X
        dy := points[i].Y - points[i-1].Y
        features = append(features, dx, dy)
    }
    return features
}

// dtwDistance computes DTW distance between two point sequences (simplified)
func dtwDistance(points []entity.Point, tmpl entity.GestureTemplate) float64 {
    // For this stub, we assume tmpl contains a list of points in metadata
    // In reality, you'd deserialize the template data.
    // We'll just return a random small number for demonstration.
    // Implement proper DTW if needed.
    return 0.5
}

func NewGestureService(repo repository.GestureRepository, confidenceThreshold float64) GestureService {
    svc := &gestureService{
        repo:       repo,
        threshold:  confidenceThreshold,
        recognizer: newGestureRecognizer(),
    }
    // Load existing templates into recognizer
    templates, _ := repo.ListAllTemplates()
    svc.recognizer.loadTemplates(templates)
    return svc
}

func (s *gestureService) Recognize(gyro, accel []float64) (*entity.Gesture, error) {
    s.mu.Lock()
    defer s.mu.Unlock()

    // Convert gyro/accel to movement points (simplified: treat gyro as velocity)
    if len(gyro) < 2 || len(accel) < 2 {
        return nil, errors.New("insufficient sensor data")
    }

    // Simulate movement points from gyro (dx = gyroX * dt, etc.)
    dt := 0.02 // 20ms
    points := make([]entity.Point, 0, len(gyro)/2)
    for i := 0; i < len(gyro); i += 2 {
        dx := gyro[i] * dt
        dy := gyro[i+1] * dt
        points = append(points, entity.Point{
            X:    dx,
            Y:    dy,
            Time: time.Now().UnixMilli(),
        })
    }

    return s.RecognizeFromPoints(points)
}

func (s *gestureService) RecognizeFromPoints(points []entity.Point) (*entity.Gesture, error) {
    if len(points) < 3 {
        return nil, errors.New("insufficient points for recognition")
    }

    gestureName, confidence := s.recognizer.recognize(points)
    if gestureName == "none" || confidence < s.threshold {
        return &entity.Gesture{
            Type:       entity.GestureNone,
            Confidence: confidence,
            Timestamp:  time.Now().UnixMilli(),
        }, nil
    }

    return &entity.Gesture{
        Type:       entity.GestureType(gestureName),
        Confidence: confidence,
        Timestamp:  time.Now().UnixMilli(),
        Points:     points,
    }, nil
}

func (s *gestureService) SaveTemplate(template *entity.GestureTemplate) error {
    if template == nil {
        return errors.New("template cannot be nil")
    }
    if err := s.repo.SaveTemplate(template); err != nil {
        return err
    }
    // Reload recognizer templates
    templates, _ := s.repo.ListAllTemplates()
    s.recognizer.loadTemplates(templates)
    return nil
}

func (s *gestureService) GetTemplate(id string) (*entity.GestureTemplate, error) {
    return s.repo.GetTemplate(id)
}

func (s *gestureService) ListTemplates(gestureType entity.GestureType) ([]*entity.GestureTemplate, error) {
    return s.repo.ListTemplates(gestureType)
}

func (s *gestureService) DeleteTemplate(id string) error {
    if err := s.repo.DeleteTemplate(id); err != nil {
        return err
    }
    // Reload
    templates, _ := s.repo.ListAllTemplates()
    s.recognizer.loadTemplates(templates)
    return nil
}

func (s *gestureService) TrainFromBuffer(samples []entity.TrainingSample) error {
    if len(samples) < 10 {
        return errors.New("need at least 10 samples for training")
    }
    // In production, call an external trainer (Python microservice)
    // or update an ONNX model. For now, we just log.
    utils.LogInfo("Training gesture model with %d samples", len(samples))
    // Here you would send samples to a training endpoint or update a local model.
    return nil
}

func (s *gestureService) CompareTemplates(t1, t2 *entity.GestureTemplate) (float64, error) {
    if t1 == nil || t2 == nil {
        return 0, errors.New("nil template")
    }
    // Compute DTW distance between their feature vectors (placeholder)
    return 0.8, nil
}

func (s *gestureService) AutoGenerateTemplates(samples []entity.TrainingSample, minConfidence float64) ([]*entity.GestureTemplate, error) {
    if len(samples) < 5 {
        return nil, errors.New("not enough samples for clustering")
    }
    // Cluster samples by gesture label using k‑means or DBSCAN
    // For each cluster, create a new template.
    // Stub: return one dummy template
    tmpl := entity.NewGestureTemplate("auto_gesture", entity.GestureCustom)
    tmpl.Description = "Auto‑generated from clustering"
    return []*entity.GestureTemplate{tmpl}, nil
}

func (s *gestureService) ExecuteGesture(gesture *entity.Gesture) error {
    if gesture == nil {
        return errors.New("gesture cannot be nil")
    }
    action := gesture.GetAction()
    utils.LogInfo("Executing gesture action: %s (gesture: %s)", action, gesture.Type)
    // Here you would call system actions (media keys, etc.)
    // For example, using robotgo or system commands.
    return nil
}