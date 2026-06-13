package predictiveml

import (
    "fmt"
    "sync"

    ort "github.com/yalue/onnxruntime_go"
)

type Config struct {
    ModelPath         string
    SequenceLength    int
    EnableML          bool
    BlendFactor       float64
}

type Predictor struct {
    session     *ort.AdvancedSession
    inputTensor *ort.Tensor[float32]
    outputTensor *ort.Tensor[float32]
    history     [][]float32
    maxHistory  int
    enabled     bool
    blendFactor float64
    mu          sync.Mutex
}

func NewPredictor(cfg Config) (*Predictor, error) {
    if err := ort.InitializeEnvironment(); err != nil {
        return nil, err
    }
    inputShape := []int64{1, int64(cfg.SequenceLength), 2}
    inputTensor, err := ort.NewEmptyTensor[float32](inputShape)
    if err != nil {
        return nil, err
    }
    outputShape := []int64{1, 2}
    outputTensor, err := ort.NewEmptyTensor[float32](outputShape)
    if err != nil {
        return nil, err
    }
    session, err := ort.NewAdvancedSession(
        cfg.ModelPath,
        []string{"input_sequence"},
        []string{"predicted_delta"},
        []ort.ArbitraryTensor{inputTensor},
        []ort.ArbitraryTensor{outputTensor},
        nil,
    )
    if err != nil {
        return nil, err
    }
    return &Predictor{
        session:      session,
        inputTensor:  inputTensor,
        outputTensor: outputTensor,
        history:      make([][]float32, 0, cfg.SequenceLength),
        maxHistory:   cfg.SequenceLength,
        enabled:      cfg.EnableML,
        blendFactor:  cfg.BlendFactor,
    }, nil
}

func (p *Predictor) AddPoint(x, y float32) {
    p.mu.Lock()
    defer p.mu.Unlock()
    if !p.enabled {
        return
    }
    point := []float32{x, y}
    p.history = append(p.history, point)
    if len(p.history) > p.maxHistory {
        p.history = p.history[1:]
    }
}

func (p *Predictor) PredictDelta() (dx, dy float32, err error) {
    p.mu.Lock()
    defer p.mu.Unlock()
    if !p.enabled || len(p.history) < p.maxHistory {
        return 0, 0, nil
    }
    buf := make([]float32, p.maxHistory*2)
    for i, point := range p.history {
        buf[i*2] = point[0]
        buf[i*2+1] = point[1]
    }
    copy(p.inputTensor.GetData(), buf)
    if err := p.session.Run(); err != nil {
        return 0, 0, err
    }
    output := p.outputTensor.GetData()
    if len(output) < 2 {
        return 0, 0, fmt.Errorf("unexpected output size")
    }
    return output[0], output[1], nil
}

func (p *Predictor) SetEnabled(enabled bool) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.enabled = enabled
}

func (p *Predictor) SetBlendFactor(factor float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.blendFactor = factor
}

func (p *Predictor) Close() error {
    if p.session != nil {
        p.session.Destroy()
    }
    ort.DestroyEnvironment()
    return nil
}
