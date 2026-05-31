//go:build !ai

package control

import "fmt"

type AISmoother struct{}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
	fmt.Println("AI smoothing not enabled (build with -tags ai)")
	return &AISmoother{}, nil
}

func (a *AISmoother) AddPoint(x, y float64)                       {}
func (a *AISmoother) PredictDelta() (float64, float64, error)     { return 0, 0, nil }
func (a *AISmoother) SetEnabled(enabled bool)                     {}
func (a *AISmoother) Close() error                                { return nil }