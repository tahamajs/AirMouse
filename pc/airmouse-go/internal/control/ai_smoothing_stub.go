package control

import "fmt"

// Default stub for AISmoother when AI build tag is not used.
type AISmoother struct{}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
    // Return a harmless stub so callers can optionally use AI smoothing.
    fmt.Println("AI smoothing not enabled in this build (use -tags ai to enable real implementation)")
    return &AISmoother{}, nil
}

func (a *AISmoother) AddPoint(x, y float64) {}
func (a *AISmoother) PredictDelta() (float64, float64, error) { return 0, 0, nil }
func (a *AISmoother) SetEnabled(enabled bool) {}
func (a *AISmoother) Close() error { return nil }
