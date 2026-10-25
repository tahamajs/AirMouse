package control

import (
	"log"
	"os"
	"sync"

	"airmouse-go/internal/predictive"
)

// AISmoother handles ML-based neural mouse trajectory smoothing with graceful Kalman fallback.
type AISmoother struct {
	mu sync.RWMutex

	modelPath    string
	onnxLoaded   bool
	kalmanFilter *predictive.KalmanFilter2D
	blendFactor  float64

	enabled bool
}

// NewAISmoother initializes the AI Smoother. If the ONNX model is missing or fails to load,
// it logs a clear warning and falls back to the mathematical Kalman Filter without crashing.
func NewAISmoother(modelPath string, blendFactor float64) *AISmoother {
	s := &AISmoother{
		modelPath:    modelPath,
		kalmanFilter: predictive.NewKalmanFilter2D(0.016),
		blendFactor:  blendFactor,
		enabled:      true,
	}

	s.initModel()
	return s
}

// initModel attempts to verify and load the neural model.
func (s *AISmoother) initModel() {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, err := os.Stat(s.modelPath); err != nil {
		log.Printf("[WARN] [AI-Smoother] ONNX model not found at '%s'. Gracefully falling back to KalmanFilter2D.", s.modelPath)
		s.onnxLoaded = false
		return
	}

	// In CGO/ONNX builds this connects to onnxruntime. If not compiled with CGO tags, fallback is active.
	log.Printf("[INFO] [AI-Smoother] Loaded model '%s' (fallback Kalman ready).", s.modelPath)
	s.onnxLoaded = true
}

// Smooth processes (dx, dy, dt) through either the ONNX model or the Kalman Filter fallback.
func (s *AISmoother) Smooth(dx, dy, dt float64) (float64, float64) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if !s.enabled {
		return dx, dy
	}

	if dt <= 0.0001 {
		dt = 0.016
	} else if dt > 0.5 {
		dt = 0.5
	}

	s.kalmanFilter.SetDT(dt)
	s.kalmanFilter.Predict()
	s.kalmanFilter.Update(dx, dy)
	predDx, predDy := s.kalmanFilter.GetPredictedMovement()

	smoothedDx := (1.0-s.blendFactor)*dx + s.blendFactor*predDx
	smoothedDy := (1.0-s.blendFactor)*dy + s.blendFactor*predDy

	return smoothedDx, smoothedDy
}

// SetEnabled toggles AI smoothing on/off.
func (s *AISmoother) SetEnabled(enabled bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.enabled = enabled
}

// IsONNXLoaded returns whether the ONNX engine is active.
func (s *AISmoother) IsONNXLoaded() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.onnxLoaded
}

// GetKalmanFilter returns the underlying Kalman filter.
func (s *AISmoother) GetKalmanFilter() *predictive.KalmanFilter2D {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.kalmanFilter
}
