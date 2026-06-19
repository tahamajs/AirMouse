//go:build !ml

package control

// MLPredictor is a no-op fallback when the optional ML build tag is disabled.
type MLPredictor struct{}

type MLPredictorStats struct{}

type MLPredictionConfig struct {
	ModelPath     string
	SequenceLength int
	BlendFactor    float64
	MinConfidence  float64
	UseFallback    bool
	MaxHistory     int
}

func NewMLPredictor(cfg MLPredictionConfig) (*MLPredictor, error) {
	return &MLPredictor{}, nil
}

func (m *MLPredictor) AddPoint(x, y float64) {}

func (m *MLPredictor) PredictDelta() (dx, dy float64, confidence float64, err error) {
	return 0, 0, 0, nil
}

func (m *MLPredictor) PredictWithBlend(rawDx, rawDy float64) (dx, dy float64) {
	return rawDx, rawDy
}

func (m *MLPredictor) SetEnabled(enabled bool) {}

func (m *MLPredictor) IsEnabled() bool { return false }

func (m *MLPredictor) SetBlendFactor(factor float64) {}

func (m *MLPredictor) GetBlendFactor() float64 { return 0 }

func (m *MLPredictor) GetStats() MLPredictorStats { return MLPredictorStats{} }

func (m *MLPredictor) GetConfidence() float64 { return 0 }

func (m *MLPredictor) Reset() {}

type TrainingSample struct{}

func (m *MLPredictor) Train(samples []TrainingSample) error { return nil }

func (m *MLPredictor) GetModelInfo() map[string]interface{} {
	return map[string]interface{}{
		"enabled": false,
	}
}

func (m *MLPredictor) Close() error { return nil }
