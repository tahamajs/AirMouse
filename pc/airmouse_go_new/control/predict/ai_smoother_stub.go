//go:build !ai

package predict

// AISmootherStats is a stub for statistics.
type AISmootherStats struct{}

// AISmoother is a no-op stub when AI is disabled.
type AISmoother struct{}

// NewAISmoother returns a stub.
func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
	return &AISmoother{}, nil
}

func (a *AISmoother) AddPoint(x, y float64)                     {}
func (a *AISmoother) PredictDelta() (dx, dy float64, err error) { return 0, 0, nil }
func (a *AISmoother) SetEnabled(enabled bool)                   {}
func (a *AISmoother) IsEnabled() bool                           { return false }
func (a *AISmoother) Reset()                                    {}
func (a *AISmoother) GetStats() AISmootherStats                 { return AISmootherStats{} }
func (a *AISmoother) GetModelInfo() map[string]interface{} {
	return map[string]interface{}{"enabled": false}
}
func (a *AISmoother) Close() error { return nil }
