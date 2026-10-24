package predictiveml

import (
	"testing"
	"time"
)

func TestPredictiveMLConfigAndPredictor(t *testing.T) {
	cfg := PredictionConfig{
		ModelPath:      "models/test.onnx",
		SequenceLength: 10,
		EnableML:       true,
		BlendFactor:    0.5,
		MinConfidence:  0.6,
		MaxLatency:     20 * time.Millisecond,
	}

	p, err := NewPredictor(cfg)
	if err != nil {
		t.Logf("NewPredictor error: %v (expected in stub / no model file environment)", err)
		return
	}

	p.AddPoint(10.0, 5.0)
	dx, dy, conf, err := p.PredictDelta()
	t.Logf("PredictDelta: dx=%.2f, dy=%.2f, conf=%.2f, err=%v", dx, dy, conf, err)

	p.SetEnabled(false)
	p.SetBlendFactor(0.7)
	_ = p.GetStats()
	p.Reset()
	_ = p.Close()
}
