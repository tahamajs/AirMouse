//go:build !ml

package predict

// MLPredictor is a no-op stub when ML is disabled.
type MLPredictor struct{}

// MLPredictorStats is empty.
type MLPredictorStats struct{}

// MLPredictionConfig is the config struct (used only when enabled).
type MLPredictionConfig struct {
	ModelPath      string
	SequenceLength int
	BlendFactor    float64
	MinConfidence  float64
	UseFallback    bool
	MaxHistory     int
}

// NewMLPredictor returns a stub.
func NewMLPredictor(cfg MLPredictionConfig) (*MLPredictor, error) {
	return &MLPredictor{}, nil
}

// AddPoint does nothing.
func (m *MLPredictor) AddPoint(x, y float64) {}

// PredictDelta returns zero.
func (m *MLPredictor) PredictDelta() (dx, dy float64, confidence float64, err error) {
	return 0, 0, 0, nil
}

// SetEnabled does nothing.
func (m *MLPredictor) SetEnabled(enabled bool) {}

// IsEnabled returns false.
func (m *MLPredictor) IsEnabled() bool { return false }

// SetBlendFactor does nothing.
func (m *MLPredictor) SetBlendFactor(factor float64) {}

// GetBlendFactor returns 0.
func (m *MLPredictor) GetBlendFactor() float64 { return 0 }

// GetStats returns empty stats.
func (m *MLPredictor) GetStats() MLPredictorStats { return MLPredictorStats{} }

// GetConfidence returns 0.
func (m *MLPredictor) GetConfidence() float64 { return 0 }

// Reset does nothing.
func (m *MLPredictor) Reset() {}

// Close does nothing.
func (m *MLPredictor) Close() error { return nil }