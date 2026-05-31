package predictiveml

import (
    "fmt"
    "log"
    "sync"
    "time"

    ort "github.com/yalue/onnxruntime_go"
)

// Config holds the ML prediction parameters.
type Config struct {
    ModelPath           string        `json:"ml_model_path"`
    SequenceLength      int           `json:"ml_sequence_length"`       // number of past points to keep (16)
    PredictionInterval  time.Duration `json:"ml_prediction_interval"`   // how often to run inference (20ms)
    EnableML            bool          `json:"enable_ml_prediction"`
    BlendFactor         float64       `json:"ml_blend_factor"`          // 0 = raw movement only, 1 = ML only
    MinConfidence       float64       `json:"ml_min_confidence"`        // not used in regression, kept for consistency
}

// Predictor runs the ONNX LSTM model to predict the next cursor movement.
type Predictor struct {
    session     *ort.AdvancedSession
    inputTensor  *ort.Tensor[float32]
    outputTensor *ort.Tensor[float32]
    history      [][]float32   // ring buffer of (x, y) positions
    maxHistory   int
    enabled      bool
    blendFactor  float64
    mu           sync.Mutex
    lastTime     time.Time
}

// NewPredictor loads the ONNX model and initialises the predictor.
func NewPredictor(cfg Config) (*Predictor, error) {
    if err := ort.InitializeEnvironment(); err != nil {
        return nil, fmt.Errorf("ONNX init failed: %w", err)
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
        return nil, fmt.Errorf("failed to create session: %w", err)
    }

    return &Predictor{
        session:     session,
        inputTensor:  inputTensor,
        outputTensor: outputTensor,
        history:      make([][]float32, 0, cfg.SequenceLength),
        maxHistory:   cfg.SequenceLength,
        enabled:      cfg.EnableML,
        blendFactor:  cfg.BlendFactor,
        lastTime:     time.Now(),
    }, nil
}

// AddPoint adds a new cursor position to the history buffer.
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

// PredictDelta runs the ONNX model to predict the next (dx, dy).
// If there is not enough history, it returns (0,0).
func (p *Predictor) PredictDelta() (dx, dy float32, err error) {
    p.mu.Lock()
    defer p.mu.Unlock()
    if !p.enabled || len(p.history) < p.maxHistory {
        return 0, 0, nil
    }

    // Prepare input buffer: (1, maxHistory, 2)
    buf := make([]float32, p.maxHistory*2)
    for i, point := range p.history {
        buf[i*2] = point[0]
        buf[i*2+1] = point[1]
    }
    copy(p.inputTensor.GetData(), buf)

    // Run inference
    if err := p.session.Run(); err != nil {
        return 0, 0, err
    }
    output := p.outputTensor.GetData()
    if len(output) < 2 {
        return 0, 0, fmt.Errorf("unexpected output size")
    }
    return output[0], output[1], nil
}

// SetEnabled turns ML prediction on/off.
func (p *Predictor) SetEnabled(enabled bool) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.enabled = enabled
}

// SetBlendFactor adjusts the influence of prediction.
func (p *Predictor) SetBlendFactor(factor float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.blendFactor = factor
}

// Close releases ONNX resources.
func (p *Predictor) Close() error {
    if p.session != nil {
        p.session.Destroy()
    }
    ort.DestroyEnvironment()
    return nil
}