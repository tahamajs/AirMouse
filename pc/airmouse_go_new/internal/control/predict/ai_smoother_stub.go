//go:build !ai

package predict

// AISmoother is a no-op stub when AI is disabled.
type AISmoother struct{}

// NewAISmoother returns a stub.
func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
	return &AISmoother{}, nil
}

// AddPoint does nothing.
func (a *AISmoother) AddPoint(x, y float64) {}

// PredictDelta returns zero.
func (a *AISmoother) PredictDelta() (dx, dy float64, err error) { return 0, 0, nil }

// SetEnabled does nothing.
func (a *AISmoother) SetEnabled(enabled bool) {}

// IsEnabled returns false.
func (a *AISmoother) IsEnabled() bool { return false }

// Reset does nothing.
func (a *AISmoother) Reset() {}

// GetStats returns empty stats.
func (a *AISmoother) GetStats() AISmootherStats { return AISmootherStats{} }

// GetModelInfo returns basic info.
func (a *AISmoother) GetModelInfo() map[string]interface{} {
	return map[string]interface{}{"enabled": false}
}

// Close does nothing.
func (a *AISmoother) Close() error { return nil }