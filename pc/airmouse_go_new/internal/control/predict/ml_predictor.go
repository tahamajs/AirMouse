//go:build ml

package predict

import (
	"fmt"
	"math"
	"sync"
	"time"

	"airmouse-go/internal/predictiveml"
)

// MLPredictor uses an ONNX LSTM model for movement prediction.
type MLPredictor struct {
	predictor   *predictiveml.Predictor
	cfg         predictiveml.PredictionConfig
	mu          sync.Mutex
	enabled     bool
	stats       MLPredictorStats
	fallback    *MovementPredictor
	history     []Point
	maxHistory  int
	confidence  float64
}

// MLPredictorStats holds statistics.
type MLPredictorStats struct {
	TotalPredictions      int64
	SuccessfulPredictions int64
	AverageConfidence     float64
	AverageLatencyMs      float64
	LastPrediction        time.Time
	ModelAccuracy         float64
}

// Point is a simple 2D point.
type Point struct {
	X, Y      float64
	Timestamp time.Time
}

// MLPredictionConfig holds configuration for the ML predictor.
type MLPredictionConfig struct {
	ModelPath      string
	SequenceLength int
	BlendFactor    float64
	MinConfidence  float64
	UseFallback    bool
	MaxHistory     int
}

// NewMLPredictor creates a new ML predictor.
func NewMLPredictor(cfg MLPredictionConfig) (*MLPredictor, error) {
	if cfg.SequenceLength <= 0 {
		cfg.SequenceLength = 16
	}
	if cfg.BlendFactor < 0 {
		cfg.BlendFactor = 0
	}
	if cfg.BlendFactor > 1 {
		cfg.BlendFactor = 1
	}
	if cfg.MinConfidence <= 0 {
		cfg.MinConfidence = 0.3
	}
	if cfg.MaxHistory <= 0 {
		cfg.MaxHistory = 100
	}

	predictorCfg := predictiveml.PredictionConfig{
		ModelPath:      cfg.ModelPath,
		SequenceLength: cfg.SequenceLength,
		EnableML:       true,
		BlendFactor:    cfg.BlendFactor,
	}
	predictor, err := predictiveml.NewPredictor(predictorCfg)
	if err != nil {
		return nil, fmt.Errorf("failed to create ML predictor: %w", err)
	}

	mlp := &MLPredictor{
		predictor:  predictor,
		cfg:        predictorCfg,
		enabled:    true,
		fallback:   NewMovementPredictor(0.02, 0.6),
		history:    make([]Point, 0, cfg.MaxHistory),
		maxHistory: cfg.MaxHistory,
		confidence: 0.5,
	}
	return mlp, nil
}

// AddPoint adds a new point to the history.
func (m *MLPredictor) AddPoint(x, y float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.history = append(m.history, Point{X: x, Y: y, Timestamp: time.Now()})
	if len(m.history) > m.maxHistory {
		m.history = m.history[1:]
	}
	if m.predictor != nil && m.enabled {
		m.predictor.AddPoint(float32(x), float32(y))
	}
	if m.fallback != nil {
		m.fallback.AddMovement(x, y)
	}
}

// PredictDelta returns the predicted delta.
func (m *MLPredictor) PredictDelta() (dx, dy float64, confidence float64, err error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if !m.enabled {
		if m.fallback != nil {
			predDx, predDy := m.fallback.AddMovement(0, 0)
			return predDx, predDy, 0.3, nil
		}
		return 0, 0, 0, nil
	}

	startTime := time.Now()
	var predDx, predDy float32
	var mlConfidence float64

	if m.predictor != nil {
		predDx, predDy, mlConfidence, err = m.predictor.PredictDelta()
		if err != nil {
			if m.fallback != nil {
				fallbackDx, fallbackDy := m.fallback.AddMovement(0, 0)
				m.stats.TotalPredictions++
				return fallbackDx, fallbackDy, 0.3, nil
			}
			return 0, 0, 0, err
		}
	} else {
		return 0, 0, 0, fmt.Errorf("predictor not initialized")
	}

	confidence = mlConfidence
	if confidence < m.cfg.MinConfidence {
		if m.fallback != nil {
			fallbackDx, fallbackDy := m.fallback.AddMovement(0, 0)
			m.stats.TotalPredictions++
			return fallbackDx, fallbackDy, confidence, nil
		}
		return float64(predDx), float64(predDy), confidence, nil
	}

	latency := time.Since(startTime)
	m.stats.TotalPredictions++
	m.stats.SuccessfulPredictions++
	m.stats.AverageConfidence = (m.stats.AverageConfidence*float64(m.stats.TotalPredictions-1) + confidence) / float64(m.stats.TotalPredictions)
	m.stats.AverageLatencyMs = (m.stats.AverageLatencyMs*float64(m.stats.TotalPredictions-1) + float64(latency.Microseconds())/1000.0) / float64(m.stats.TotalPredictions)
	m.stats.LastPrediction = time.Now()
	m.confidence = confidence

	return float64(predDx), float64(predDy), confidence, nil
}

// SetEnabled enables or disables ML prediction.
func (m *MLPredictor) SetEnabled(enabled bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.enabled = enabled
	if m.predictor != nil {
		m.predictor.SetEnabled(enabled)
	}
}

// IsEnabled returns whether ML prediction is enabled.
func (m *MLPredictor) IsEnabled() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.enabled
}

// SetBlendFactor updates the blend factor.
func (m *MLPredictor) SetBlendFactor(factor float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.cfg.BlendFactor = factor
	if m.predictor != nil {
		m.predictor.SetBlendFactor(factor)
	}
}

// GetBlendFactor returns the current blend factor.
func (m *MLPredictor) GetBlendFactor() float64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.cfg.BlendFactor
}

// GetStats returns statistics.
func (m *MLPredictor) GetStats() MLPredictorStats {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.stats
}

// GetConfidence returns the current confidence.
func (m *MLPredictor) GetConfidence() float64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.confidence
}

// Reset clears history and stats.
func (m *MLPredictor) Reset() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.predictor != nil {
		m.predictor.Reset()
	}
	if m.fallback != nil {
		m.fallback.Reset()
	}
	m.history = make([]Point, 0, m.maxHistory)
	m.stats = MLPredictorStats{}
	m.confidence = 0.5
}

// Close releases resources.
func (m *MLPredictor) Close() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.predictor != nil {
		return m.predictor.Close()
	}
	return nil
}//go:build ml

package predict

import ( ... ) // full implementation

// Stub:
//go:build !ml
package predict

type MLPredictor struct{}
// ... all stub methods
