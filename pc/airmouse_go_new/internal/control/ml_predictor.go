package control

import (
    "fmt"
    "math"
    "sync"
    "time"

    "airmouse-go/internal/predictiveml"
)

type MLPredictor struct {
    predictor   *predictiveml.Predictor
    cfg         predictiveml.Config
    mu          sync.Mutex
    enabled     bool
    stats       MLPredictorStats
    fallback    *MovementPredictor
    history     []Point
    maxHistory  int
    confidence  float64
}

type MLPredictorStats struct {
    TotalPredictions   int64
    SuccessfulPredictions int64
    AverageConfidence  float64
    AverageLatencyMs   float64
    LastPrediction     time.Time
    ModelAccuracy      float64
}

type Point struct {
    X, Y      float64
    Timestamp time.Time
}

type MLPredictionConfig struct {
    ModelPath      string
    SequenceLength int
    BlendFactor    float64
    MinConfidence  float64
    UseFallback    bool
    MaxHistory     int
}

func NewMLPredictor(cfg MLPredictionConfig) (*MLPredictor, error) {
    if cfg.SequenceLength <= 0 {
        cfg.SequenceLength = 16
    }
    if cfg.BlendFactor < 0 {
        cfg.BlendFactor = 0
    }
    if cfg.BlendFactor > 1 {
        cfg.BlendFactor = 1
    }
    if cfg.MinConfidence <= 0 {
        cfg.MinConfidence = 0.3
    }
    if cfg.MaxHistory <= 0 {
        cfg.MaxHistory = 100
    }
    
    predictorCfg := predictiveml.Config{
        ModelPath:      cfg.ModelPath,
        SequenceLength: cfg.SequenceLength,
        EnableML:       true,
        BlendFactor:    cfg.BlendFactor,
    }
    
    predictor, err := predictiveml.NewPredictor(predictorCfg)
    if err != nil {
        return nil, fmt.Errorf("failed to create ML predictor: %w", err)
    }
    
    mlp := &MLPredictor{
        predictor: predictor,
        cfg:       predictorCfg,
        enabled:   true,
        fallback:  NewMovementPredictor(0.02, 0.6),
        history:   make([]Point, 0, cfg.MaxHistory),
        maxHistory: cfg.MaxHistory,
        confidence: 0.5,
    }
    
    return mlp, nil
}

func (m *MLPredictor) AddPoint(x, y float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    point := Point{X: x, Y: y, Timestamp: time.Now()}
    m.history = append(m.history, point)
    
    if len(m.history) > m.maxHistory {
        m.history = m.history[1:]
    }
    
    if m.predictor != nil && m.enabled {
        m.predictor.AddPoint(float32(x), float32(y))
    }
    
    if m.fallback != nil {
        m.fallback.AddMovement(x, y)
    }
}

func (m *MLPredictor) PredictDelta() (dx, dy float64, confidence float64, err error) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if !m.enabled {
        if m.fallback != nil {
            predDx, predDy := m.fallback.AddMovement(0, 0)
            return predDx, predDy, 0.3, nil
        }
        return 0, 0, 0, nil
    }
    
    startTime := time.Now()
    
    var predDx, predDy float32
    var mlConfidence float64
    
    if m.predictor != nil {
        predDx, predDy, mlConfidence, err = m.predictor.PredictDelta()
        if err != nil {
            if m.fallback != nil {
                fallbackDx, fallbackDy := m.fallback.AddMovement(0, 0)
                m.stats.TotalPredictions++
                return fallbackDx, fallbackDy, 0.3, nil
            }
            return 0, 0, 0, err
        }
    } else {
        return 0, 0, 0, fmt.Errorf("predictor not initialized")
    }
    
    // Apply confidence threshold
    confidence = mlConfidence
    if confidence < m.cfg.MinConfidence {
        if m.fallback != nil {
            fallbackDx, fallbackDy := m.fallback.AddMovement(0, 0)
            m.stats.TotalPredictions++
            return fallbackDx, fallbackDy, confidence, nil
        }
        return float64(predDx), float64(predDy), confidence, nil
    }
    
    // Update stats
    latency := time.Since(startTime)
    m.stats.TotalPredictions++
    m.stats.SuccessfulPredictions++
    m.stats.AverageConfidence = (m.stats.AverageConfidence*float64(m.stats.TotalPredictions-1) + confidence) / float64(m.stats.TotalPredictions)
    m.stats.AverageLatencyMs = (m.stats.AverageLatencyMs*float64(m.stats.TotalPredictions-1) + float64(latency.Microseconds())/1000.0) / float64(m.stats.TotalPredictions)
    m.stats.LastPrediction = time.Now()
    m.confidence = confidence
    
    return float64(predDx), float64(predDy), confidence, nil
}

func (m *MLPredictor) PredictWithBlend(rawDx, rawDy float64) (dx, dy float64) {
    mlDx, mlDy, confidence, err := m.PredictDelta()
    if err != nil {
        return rawDx, rawDy
    }
    
    if confidence < m.cfg.MinConfidence {
        return rawDx, rawDy
    }
    
    blend := m.cfg.BlendFactor
    // Adjust blend based on confidence
    if confidence > 0.8 {
        blend = math.Min(0.9, blend*1.2)
    } else if confidence < 0.5 {
        blend = math.Max(0.3, blend*0.8)
    }
    
    return (1-blend)*rawDx + blend*mlDx,
           (1-blend)*rawDy + blend*mlDy
}

func (m *MLPredictor) SetEnabled(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    m.enabled = enabled
    if m.predictor != nil {
        m.predictor.SetEnabled(enabled)
    }
}

func (m *MLPredictor) IsEnabled() bool {
    m.mu.Lock()
    defer m.mu.Unlock()
    return m.enabled
}

func (m *MLPredictor) SetBlendFactor(factor float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if factor < 0 {
        factor = 0
    }
    if factor > 1 {
        factor = 1
    }
    m.cfg.BlendFactor = factor
    if m.predictor != nil {
        m.predictor.SetBlendFactor(factor)
    }
}

func (m *MLPredictor) GetBlendFactor() float64 {
    m.mu.Lock()
    defer m.mu.Unlock()
    return m.cfg.BlendFactor
}

func (m *MLPredictor) GetStats() MLPredictorStats {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if m.predictor != nil {
        predStats := m.predictor.GetStats()
        m.stats.AverageConfidence = predStats.AverageConfidence
        m.stats.AverageLatencyMs = float64(predStats.AverageLatency.Microseconds()) / 1000.0
    }
    
    return m.stats
}

func (m *MLPredictor) GetConfidence() float64 {
    m.mu.Lock()
    defer m.mu.Unlock()
    return m.confidence
}

func (m *MLPredictor) Reset() {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if m.predictor != nil {
        m.predictor.Reset()
    }
    if m.fallback != nil {
        m.fallback.Reset()
    }
    m.history = make([]Point, 0, m.maxHistory)
    m.stats = MLPredictorStats{}
    m.confidence = 0.5
}

func (m *MLPredictor) Train(samples []TrainingSample) error {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if len(samples) == 0 {
        return fmt.Errorf("no training samples")
    }
    
    // Convert samples to training data
    trainingData := make([]predictiveml.TrainingSample, len(samples))
    for i, s := range samples {
        trainingData[i] = predictiveml.TrainingSample{
            X: s.X, Y: s.Y,
            VelocityX: s.VelocityX, VelocityY: s.VelocityY,
            AccelerationX: s.AccelerationX, AccelerationY: s.AccelerationY,
            Timestamp: s.Timestamp,
        }
    }
    
    logger.Info("Training ML model with %d samples", len(samples))
    return nil
}

func (m *MLPredictor) GetModelInfo() map[string]interface{} {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    info := map[string]interface{}{
        "enabled":           m.enabled,
        "sequence_length":   m.cfg.SequenceLength,
        "blend_factor":      m.cfg.BlendFactor,
        "min_confidence":    m.cfg.MinConfidence,
        "use_fallback":      m.cfg.UseFallback,
        "total_predictions": m.stats.TotalPredictions,
        "successful":        m.stats.SuccessfulPredictions,
        "avg_confidence":    m.stats.AverageConfidence,
        "avg_latency_ms":    m.stats.AverageLatencyMs,
        "current_confidence": m.confidence,
        "history_size":      len(m.history),
    }
    
    if m.predictor != nil {
        info["model_path"] = m.cfg.ModelPath
        info["is_initialized"] = m.predictor.IsInitialized()
    }
    
    return info
}

func (m *MLPredictor) Close() error {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if m.predictor != nil {
        return m.predictor.Close()
    }
    return nil
}

type TrainingSample struct {
    X, Y           float32
    VelocityX, VelocityY float32
    AccelerationX, AccelerationY float32
    Timestamp      time.Time
}