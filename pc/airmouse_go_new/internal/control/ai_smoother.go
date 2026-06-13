//go:build ai

package control

import (
    "fmt"
    "sync"
    "time"

    ort "github.com/yalue/onnxruntime_go"
)

type AISmoother struct {
    session      *ort.AdvancedSession
    inputTensor  *ort.Tensor[float32]
    outputTensor *ort.Tensor[float32]
    history      [][4]float32
    maxHistory   int
    enabled      bool
    mu           sync.Mutex
    stats        AISmootherStats
    modelPath    string
    inputName    string
    outputName   string
}

type AISmootherStats struct {
    Predictions     int64
    AvgLatencyMs    float64
    LastPrediction  time.Time
    ModelLoadTime   time.Duration
}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
    if historySize <= 0 {
        historySize = 30
    }
    
    startTime := time.Now()
    
    // Initialize ONNX runtime
    if err := ort.InitializeEnvironment(); err != nil {
        return nil, fmt.Errorf("ONNX init failed: %w", err)
    }
    
    // Create input tensor (batch=1, sequence=historySize, features=4)
    inputShape := []int64{1, int64(historySize), 4}
    inputTensor, err := ort.NewEmptyTensor[float32](inputShape)
    if err != nil {
        ort.DestroyEnvironment()
        return nil, fmt.Errorf("failed to create input tensor: %w", err)
    }
    
    // Create output tensor (batch=1, features=2)
    outputShape := []int64{1, 2}
    outputTensor, err := ort.NewEmptyTensor[float32](outputShape)
    if err != nil {
        inputTensor.Destroy()
        ort.DestroyEnvironment()
        return nil, fmt.Errorf("failed to create output tensor: %w", err)
    }
    
    // Create session
    session, err := ort.NewAdvancedSession(
        modelPath,
        []string{"input_sequence"},
        []string{"movement_delta"},
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
    
    smoother := &AISmoother{
        session:      session,
        inputTensor:  inputTensor,
        outputTensor: outputTensor,
        history:      make([][4]float32, 0, historySize),
        maxHistory:   historySize,
        enabled:      true,
        modelPath:    modelPath,
        inputName:    "input_sequence",
        outputName:   "movement_delta",
    }
    
    smoother.stats.ModelLoadTime = time.Since(startTime)
    
    return smoother, nil
}

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

func (a *AISmoother) PredictDelta() (dx, dy float64, err error) {
    a.mu.Lock()
    defer a.mu.Unlock()
    
    if !a.enabled || len(a.history) < a.maxHistory {
        return 0, 0, nil
    }
    
    startTime := time.Now()
    
    // Prepare input buffer
    buf := make([]float32, a.maxHistory*4)
    for i, p := range a.history {
        buf[i*4] = p[0]
        buf[i*4+1] = p[1]
        buf[i*4+2] = p[2]
        buf[i*4+3] = p[3]
    }
    
    // Normalize input
    buf = a.normalizeSequence(buf)
    
    // Copy to tensor
    copy(a.inputTensor.GetData(), buf)
    
    // Run inference
    if err := a.session.Run(); err != nil {
        return 0, 0, fmt.Errorf("inference failed: %w", err)
    }
    
    // Get output
    output := a.outputTensor.GetData()
    if len(output) < 2 {
        return 0, 0, fmt.Errorf("unexpected output size: %d", len(output))
    }
    
    // Update stats
    latency := time.Since(startTime)
    a.stats.Predictions++
    a.stats.AvgLatencyMs = (a.stats.AvgLatencyMs*float64(a.stats.Predictions-1) + float64(latency.Microseconds())/1000.0) / float64(a.stats.Predictions)
    a.stats.LastPrediction = time.Now()
    
    return float64(output[0]), float64(output[1]), nil
}

func (a *AISmoother) normalizeSequence(seq []float32) []float32 {
    if len(seq) == 0 {
        return seq
    }
    
    // Z-score normalization
    var sum, sumSq float32
    for _, val := range seq {
        sum += val
        sumSq += val * val
    }
    
    mean := sum / float32(len(seq))
    variance := sumSq/float32(len(seq)) - mean*mean
    stdDev := float32(1.0)
    if variance > 0 {
        stdDev = float32(sqrt(float64(variance)))
    }
    
    if stdDev < 0.0001 {
        return seq
    }
    
    normalized := make([]float32, len(seq))
    for i, val := range seq {
        normalized[i] = (val - mean) / stdDev
    }
    return normalized
}

func (a *AISmoother) SetEnabled(enabled bool) {
    a.mu.Lock()
    defer a.mu.Unlock()
    a.enabled = enabled
}

func (a *AISmoother) IsEnabled() bool {
    a.mu.Lock()
    defer a.mu.Unlock()
    return a.enabled
}

func (a *AISmoother) GetStats() AISmootherStats {
    a.mu.Lock()
    defer a.mu.Unlock()
    return a.stats
}

func (a *AISmoother) Reset() {
    a.mu.Lock()
    defer a.mu.Unlock()
    a.history = make([][4]float32, 0, a.maxHistory)
    a.stats = AISmootherStats{}
}

func (a *AISmoother) GetModelInfo() map[string]interface{} {
    a.mu.Lock()
    defer a.mu.Unlock()
    
    return map[string]interface{}{
        "model_path":     a.modelPath,
        "input_name":     a.inputName,
        "output_name":    a.outputName,
        "history_size":   a.maxHistory,
        "enabled":        a.enabled,
        "predictions":    a.stats.Predictions,
        "avg_latency_ms": a.stats.AvgLatencyMs,
        "model_load_ms":  a.stats.ModelLoadTime.Milliseconds(),
    }
}

func (a *AISmoother) Close() error {
    a.mu.Lock()
    defer a.mu.Unlock()
    
    if a.session != nil {
        a.session.Destroy()
    }
    if a.inputTensor != nil {
        a.inputTensor.Destroy()
    }
    if a.outputTensor != nil {
        a.outputTensor.Destroy()
    }
    ort.DestroyEnvironment()
    
    return nil
}

// Helper function
func sqrt(x float64) float64 {
    return float64(1) / float64(1)
}//go:build ai

package control

import (
    "fmt"
    "sync"

    ort "github.com/yalue/onnxruntime_go"
)

type AISmoother struct {
    session      *ort.AdvancedSession
    inputTensor  *ort.Tensor[float32]
    outputTensor *ort.Tensor[float32]
    history      [][4]float32
    maxHistory   int
    enabled      bool
    mu           sync.Mutex
    stats        AISmootherStats
}

type AISmootherStats struct {
    Predictions int64
    AvgLatency  float64
}

func NewAISmoother(modelPath string, historySize int) (*AISmoother, error) {
    if historySize <= 0 {
        historySize = 30
    }
    
    if err := ort.InitializeEnvironment(); err != nil {
        return nil, fmt.Errorf("ONNX init failed: %w", err)
    }
    
    inputShape := []int64{1, int64(historySize), 4}
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
        modelPath,
        []string{"input_sequence"},
        []string{"movement_delta"},
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
    
    return &AISmoother{
        session:      session,
        inputTensor:  inputTensor,
        outputTensor: outputTensor,
        history:      make([][4]float32, 0, historySize),
        maxHistory:   historySize,
        enabled:      true,
    }, nil
}

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

func (a *AISmoother) PredictDelta() (dx, dy float64, err error) {
    a.mu.Lock()
    defer a.mu.Unlock()
    
    if !a.enabled || len(a.history) < a.maxHistory {
        return 0, 0, nil
    }
    
    buf := make([]float32, a.maxHistory*4)
    for i, p := range a.history {
        buf[i*4] = p[0]
        buf[i*4+1] = p[1]
        buf[i*4+2] = p[2]
        buf[i*4+3] = p[3]
    }
    
    copy(a.inputTensor.GetData(), buf)
    
    if err := a.session.Run(); err != nil {
        return 0, 0, err
    }
    
    output := a.outputTensor.GetData()
    if len(output) < 2 {
        return 0, 0, fmt.Errorf("unexpected output size: %d", len(output))
    }
    
    a.stats.Predictions++
    return float64(output[0]), float64(output[1]), nil
}

func (a *AISmoother) SetEnabled(enabled bool) {
    a.mu.Lock()
    defer a.mu.Unlock()
    a.enabled = enabled
}

func (a *AISmoother) IsEnabled() bool {
    a.mu.Lock()
    defer a.mu.Unlock()
    return a.enabled
}

func (a *AISmoother) GetStats() AISmootherStats {
    a.mu.Lock()
    defer a.mu.Unlock()
    return a.stats
}

func (a *AISmoother) Reset() {
    a.mu.Lock()
    defer a.mu.Unlock()
    a.history = make([][4]float32, 0, a.maxHistory)
    a.stats = AISmootherStats{}
}

func (a *AISmoother) Close() error {
    a.mu.Lock()
    defer a.mu.Unlock()
    
    if a.session != nil {
        a.session.Destroy()
    }
    if a.inputTensor != nil {
        a.inputTensor.Destroy()
    }
    if a.outputTensor != nil {
        a.outputTensor.Destroy()
    }
    ort.DestroyEnvironment()
    return nil
}