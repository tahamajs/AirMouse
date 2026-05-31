package control

import (
	"fmt"
	"sync"

	"airmouse-go/internal/predictiveml"
)

type MLPredictor struct {
	predictor *predictiveml.Predictor
	cfg       predictiveml.Config
	mu        sync.Mutex
}

func NewMLPredictor(cfg predictiveml.Config) (*MLPredictor, error) {
	p, err := predictiveml.NewPredictor(cfg)
	if err != nil {
		return nil, err
	}
	return &MLPredictor{
		predictor: p,
		cfg:       cfg,
	}, nil
}

func (m *MLPredictor) AddPoint(x, y float32) {
	if m.predictor != nil {
		m.predictor.AddPoint(x, y)
	}
}

func (m *MLPredictor) PredictDelta() (dx, dy float32, err error) {
	if m.predictor == nil {
		return 0, 0, nil
	}
	return m.predictor.PredictDelta()
}

func (m *MLPredictor) SetEnabled(enabled bool) {
	if m.predictor != nil {
		m.predictor.SetEnabled(enabled)
	}
}

func (m *MLPredictor) SetBlendFactor(factor float64) {
	if m.predictor != nil {
		m.predictor.SetBlendFactor(factor)
	}
}

func (m *MLPredictor) Close() error {
	if m.predictor != nil {
		return m.predictor.Close()
	}
	return nil
}