//go:build !ai

package control

// Stub AISmoother for builds without AI support.
type AISmoother struct{}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
    return &AISmoother{}, nil
}

func (a *AISmoother) AddPoint(x, y float64) {}

func (a *AISmoother) PredictDelta() (dx, dy float64, err error) { return 0, 0, nil }

func (a *AISmoother) SetEnabled(enabled bool) {}

func (a *AISmoother) Close() error { return nil }