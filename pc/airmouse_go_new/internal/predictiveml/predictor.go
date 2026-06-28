// Package predictiveml provides machine‑learning prediction for cursor movement.
// It uses an ONNX runtime model to predict future cursor deltas.
package predictiveml

import (
	"fmt"
	"math"
	"sync"
	"time"

	ort "github.com/yalue/onnxruntime_go"
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

// Predictor uses an ONNX model to predict movement deltas.
type Predictor struct {
	session       *ort.AdvancedSession
	inputTensor   *ort.Tensor[float32]
	outputTensor  *ort.Tensor[float32]
	history       [][]float32
	velocities    [][]float32
	maxHistory    int
	enabled       bool
	blendFactor   float64
	minConfidence float64
	mu            sync.RWMutex
	initialized   bool
	stats         PredictionStats
	callbacks     []func(result PredictionResult)
}

// PredictionStats holds runtime statistics.
type PredictionStats struct {
	TotalPredictions   int64
	AverageConfidence  float64
	AverageLatency     time.Duration
	LastPredictionTime time.Time
}

// NewPredictor creates a new ONNX‑based predictor.
func NewPredictor(cfg PredictionConfig) (*Predictor, error) {
	if err := ort.InitializeEnvironment(); err != nil {
		return nil, fmt.Errorf("failed to initialise ONNX runtime: %w", err)
	}

	inputShape := []int64{1, int64(cfg.SequenceLength), 2}
	inputTensor, err := ort.NewEmptyTensor[float32](inputShape)
	if err != nil {
		ort.DestroyEnvironment()
		return nil, fmt.Errorf("failed to create input tensor: %w", err)
	}

	outputShape := []int64{1, 2}
	outputTensor, err := ort.NewEmptyTensor[float32](outputShape)
	if err != nil {
		inputTensor.Destroy()
		ort.DestroyEnvironment()
		return nil, fmt.Errorf("failed to create output tensor: %w", err)
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
		inputTensor.Destroy()
		outputTensor.Destroy()
		ort.DestroyEnvironment()
		return nil, fmt.Errorf("failed to create session: %w", err)
	}

	return &Predictor{
		session:       session,
		inputTensor:   inputTensor,
		outputTensor:  outputTensor,
		history:       make([][]float32, 0, cfg.SequenceLength),
		velocities:    make([][]float32, 0, cfg.SequenceLength),
		maxHistory:    cfg.SequenceLength,
		enabled:       cfg.EnableML,
		blendFactor:   cfg.BlendFactor,
		minConfidence: cfg.MinConfidence,
		callbacks:     make([]func(PredictionResult), 0),
		initialized:   true,
	}, nil
}

// AddPoint adds a new cursor position to the history.
func (p *Predictor) AddPoint(x, y float32) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if !p.enabled {
		return
	}
	point := []float32{x, y}
	p.history = append(p.history, point)

	if len(p.history) >= 2 {
		prev := p.history[len(p.history)-2]
		velX := x - prev[0]
		velY := y - prev[1]
		p.velocities = append(p.velocities, []float32{velX, velY})
	}

	if len(p.history) > p.maxHistory {
		p.history = p.history[1:]
	}
	if len(p.velocities) > p.maxHistory {
		p.velocities = p.velocities[1:]
	}
}

// PredictDelta returns the predicted movement delta and confidence.
func (p *Predictor) PredictDelta() (dx, dy float32, confidence float64, err error) {
	p.mu.RLock()
	defer p.mu.RUnlock()

	if !p.enabled || len(p.history) < p.maxHistory {
		return 0, 0, 0, nil
	}

	startTime := time.Now()

	buf := make([]float32, p.maxHistory*2)
	for i, point := range p.history {
		buf[i*2] = point[0]
		buf[i*2+1] = point[1]
	}
	buf = p.normalizeSequence(buf)

	copy(p.inputTensor.GetData(), buf)

	if err := p.session.Run(); err != nil {
		p.stats.TotalPredictions++
		return 0, 0, 0, fmt.Errorf("prediction failed: %w", err)
	}

	output := p.outputTensor.GetData()
	if len(output) < 2 {
		return 0, 0, 0, fmt.Errorf("unexpected output size")
	}

	latency := time.Since(startTime)
	confidence = p.calculateConfidence(output, latency)

	// Apply blend with velocity
	if len(p.velocities) > 0 {
		lastVel := p.velocities[len(p.velocities)-1]
		if len(lastVel) >= 2 {
			output[0] = output[0]*float32(p.blendFactor) + lastVel[0]*(1-float32(p.blendFactor))
			output[1] = output[1]*float32(p.blendFactor) + lastVel[1]*(1-float32(p.blendFactor))
		}
	}

	p.stats.TotalPredictions++
	p.stats.AverageConfidence = (p.stats.AverageConfidence*float64(p.stats.TotalPredictions-1) + confidence) / float64(p.stats.TotalPredictions)
	p.stats.AverageLatency = (p.stats.AverageLatency*time.Duration(p.stats.TotalPredictions-1) + latency) / time.Duration(p.stats.TotalPredictions)
	p.stats.LastPredictionTime = time.Now()

	result := PredictionResult{
		DX:         output[0],
		DY:         output[1],
		Confidence: confidence,
		Latency:    latency,
		Timestamp:  time.Now(),
	}
	p.triggerCallbacks(result)

	return output[0], output[1], confidence, nil
}

// normalizeSequence performs Z‑score normalisation on the input sequence.
func (p *Predictor) normalizeSequence(seq []float32) []float32 {
	var sum, sumSq float32
	for _, v := range seq {
		sum += v
		sumSq += v * v
	}
	mean := sum / float32(len(seq))
	stdDev := float32(math.Sqrt(float64(sumSq/float32(len(seq)) - mean*mean)))
	if stdDev < 0.0001 {
		return seq
	}
	normalized := make([]float32, len(seq))
	for i, v := range seq {
		normalized[i] = (v - mean) / stdDev
	}
	return normalized
}

// calculateConfidence computes a confidence score based on magnitude, latency, and history.
func (p *Predictor) calculateConfidence(output []float32, latency time.Duration) float64 {
	magnitude := math.Sqrt(float64(output[0]*output[0] + output[1]*output[1]))
	magConf := math.Min(1.0, magnitude/100.0)
	latConf := 1.0 - math.Min(1.0, float64(latency)/float64(50*time.Millisecond))
	histConf := p.calculateHistoryConfidence()
	return (magConf*0.3 + latConf*0.3 + histConf*0.4) * p.minConfidence
}

// calculateHistoryConfidence estimates confidence from movement consistency.
func (p *Predictor) calculateHistoryConfidence() float64 {
	if len(p.history) < 2 {
		return 0.5
	}
	var variances []float32
	for i := 1; i < len(p.history); i++ {
		dx := p.history[i][0] - p.history[i-1][0]
		dy := p.history[i][1] - p.history[i-1][1]
		variances = append(variances, dx*dx+dy*dy)
	}
	if len(variances) == 0 {
		return 0.5
	}
	var avg float32
	for _, v := range variances {
		avg += v
	}
	avg /= float32(len(variances))
	return 1.0 - math.Min(1.0, float64(avg)/100.0)
}

// SetEnabled enables or disables prediction.
func (p *Predictor) SetEnabled(enabled bool) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.enabled = enabled
}

// SetBlendFactor sets the blend factor for combining ML and velocity.
func (p *Predictor) SetBlendFactor(factor float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.blendFactor = factor
}

// GetStats returns a copy of the predictor statistics.
func (p *Predictor) GetStats() PredictionStats {
	p.mu.RLock()
	defer p.mu.RUnlock()
	return p.stats
}

// Reset clears the history and statistics.
func (p *Predictor) Reset() {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.history = make([][]float32, 0, p.maxHistory)
	p.velocities = make([][]float32, 0, p.maxHistory)
	p.stats = PredictionStats{}
}

// AddEventListener registers a callback for each prediction.
func (p *Predictor) AddEventListener(callback func(result PredictionResult)) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.callbacks = append(p.callbacks, callback)
}

func (p *Predictor) triggerCallbacks(result PredictionResult) {
	p.mu.RLock()
	callbacks := make([]func(PredictionResult), len(p.callbacks))
	copy(callbacks, p.callbacks)
	p.mu.RUnlock()
	for _, cb := range callbacks {
		go cb(result)
	}
}

// Close releases the ONNX session and tensors.
func (p *Predictor) Close() error {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.session != nil {
		p.session.Destroy()
	}
	if p.inputTensor != nil {
		p.inputTensor.Destroy()
	}
	if p.outputTensor != nil {
		p.outputTensor.Destroy()
	}
	ort.DestroyEnvironment()
	p.initialized = false
	return nil
}

// IsInitialized returns true if the predictor is ready.
func (p *Predictor) IsInitialized() bool {
	p.mu.RLock()
	defer p.mu.RUnlock()
	return p.initialized && p.enabled
}

// ExportModel is a placeholder for model export.
func (p *Predictor) ExportModel(outputPath string) error {
	return fmt.Errorf("model export not implemented")
}
