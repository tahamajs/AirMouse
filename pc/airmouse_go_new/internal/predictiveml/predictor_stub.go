//go:build !onnx

package predictiveml

import (
	"time"
)

// PredictionConfig holds configuration for the predictor.
type PredictionConfig struct {
	ModelPath      string
	SequenceLength int
	EnableML       bool
	BlendFactor    float64
	MinConfidence  float64
	MaxLatency     time.Duration
}

// PredictionResult contains the prediction output and metadata.
type PredictionResult struct {
	DX         float32
	DY         float32
	Confidence float64
	Latency    time.Duration
	Timestamp  time.Time
}

// PredictionStats holds runtime statistics.
type PredictionStats struct {
	TotalPredictions   int64
	AverageConfidence  float64
	AverageLatency     time.Duration
	LastPredictionTime time.Time
}

// Predictor stub when ONNX is disabled.
type Predictor struct{}

// NewPredictor returns a stub predictor.
func NewPredictor(cfg PredictionConfig) (*Predictor, error) {
	return &Predictor{}, nil
}

func (p *Predictor) AddPoint(x, y float32) {}

func (p *Predictor) PredictDelta() (dx, dy float32, confidence float64, err error) {
	return 0, 0, 0, nil
}

func (p *Predictor) SetEnabled(enabled bool) {}

func (p *Predictor) SetBlendFactor(factor float64) {}

func (p *Predictor) GetStats() PredictionStats {
	return PredictionStats{}
}

func (p *Predictor) Reset() {}

func (p *Predictor) AddEventListener(callback func(result PredictionResult)) {}

func (p *Predictor) Close() error {
	return nil
}

func (p *Predictor) IsInitialized() bool {
	return false
}

func (p *Predictor) ExportModel(outputPath string) error {
	return nil
}
