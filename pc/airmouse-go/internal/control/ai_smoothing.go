//go:build ai

package control

import (
    "fmt"
    "sync"

    ort "github.com/yalue/onnxruntime_go"
)

// AISmoother is an optional AI-based smoother that uses ONNX runtime.
type AISmoother struct {
    mu sync.Mutex
    // session and tensors kept private – initialized only when build tag 'ai' is used
    session *ort.AdvancedSession
}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
    // Placeholder: real implementation requires ONNX libs and build tag.
    fmt.Println("AISmoother stub created; compile with -tags ai for real implementation")
    return &AISmoother{}, nil
}

func (a *AISmoother) AddPoint(x, y float64) { /* noop */ }
func (a *AISmoother) PredictDelta() (float64, float64, error) { return 0, 0, nil }
func (a *AISmoother) SetEnabled(enabled bool) {}
func (a *AISmoother) Close() error { return nil }
