// internal/control/ai_smoothing.go
package control

import (
	"fmt"
	"sync"
	"time"

	ort "github.com/yalue/onnxruntime_go"
)

// AISmoother uses an ONNX RNN model to predict smooth mouse movements.
type AISmoother struct {
	session     *ort.AdvancedSession
	inputTensor  *ort.Tensor[float32]
	outputTensor *ort.Tensor[float32]
	history      [][4]float32 // x, y, vx, vy
	maxHistory   int
	enabled      bool
	mu           sync.Mutex
}

// NewAISmoother loads the ONNX model and prepares the session.
func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
	// Initialize ONNX runtime environment (call once)
	if err := ort.InitializeEnvironment(); err != nil {
		return nil, fmt.Errorf("failed to init ONNX runtime: %w", err)
	}

	// Input shape: (batch=1, sequence=historySize, features=4)
	inputShape := []int64{1, int64(historySize), 4}
	inputTensor, err := ort.NewEmptyTensor[float32](inputShape)
	if err != nil {
		return nil, err
	}

	// Output shape: (batch=1, sequence=1, output=2) – but we only need last step
	outputShape := []int64{1, 2}
	outputTensor, err := ort.NewEmptyTensor[float32](outputShape)
	if err != nil {
		return nil, err
	}

	// Create session
	session, err := ort.NewAdvancedSession(modelPath,
		[]string{"input_sequence"}, []string{"movement_delta"},
		[]ort.ArbitraryTensor{inputTensor}, []ort.ArbitraryTensor{outputTensor}, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create session: %w", err)
	}

	return &AISmoother{
		session:     session,
		inputTensor: inputTensor,
		outputTensor: outputTensor,
		history:     make([][4]float32, 0, historySize),
		maxHistory:  historySize,
		enabled:     true,
	}, nil
}

// AddPoint adds a new cursor position with velocity to the history buffer.
func (a *AISmoother) AddPoint(x, y float64) {
	a.mu.Lock()
	defer a.mu.Unlock()
	if !a.enabled {
		return
	}
	var vx, vy float32
	if len(a.history) > 0 {
		last := a.history[len(a.history)-1]
		vx = float32(x) - last[0]
		vy = float32(y) - last[1]
	}
	point := [4]float32{float32(x), float32(y), vx, vy}
	a.history = append(a.history, point)
	if len(a.history) > a.maxHistory {
		a.history = a.history[1:]
	}
}

// PredictDelta uses the RNN to predict the next movement delta (dx, dy).
func (a *AISmoother) PredictDelta() (dx, dy float64, err error) {
	a.mu.Lock()
	defer a.mu.Unlock()
	if !a.enabled || len(a.history) < a.maxHistory {
		return 0, 0, nil
	}
	// Prepare input buffer: (1, historySize, 4)
	buf := make([]float32, a.maxHistory*4)
	for i, p := range a.history {
		buf[i*4] = p[0]
		buf[i*4+1] = p[1]
		buf[i*4+2] = p[2]
		buf[i*4+3] = p[3]
	}
	copy(a.inputTensor.GetData(), buf)

	// Run inference
	if err := a.session.Run(); err != nil {
		return 0, 0, err
	}
	output := a.outputTensor.GetData()
	if len(output) < 2 {
		return 0, 0, fmt.Errorf("unexpected output size")
	}
	return float64(output[0]), float64(output[1]), nil
}

// Enable or disable AI smoothing at runtime.
func (a *AISmoother) SetEnabled(enabled bool) {
	a.mu.Lock()
	defer a.mu.Unlock()
	a.enabled = enabled
}

// Close releases ONNX resources.
func (a *AISmoother) Close() error {
	a.session.Destroy()
	ort.DestroyEnvironment()
	return nil
}