package personalization

import (
    "encoding/json"
    "fmt"
    "math"
    "os"
    "path/filepath"
    "sync"
    "time"

    "airmouse-go/internal/config"
    "airmouse-go/internal/utils"
)

type MovementSample struct {
    X, Y, VX, VY float64
    AX, AY       float64 // Acceleration
    Timestamp    time.Time
    SessionID    string
    Confidence   float64
}

type DataCollector struct {
    buffer         []MovementSample
    maxSize        int
    mu             sync.Mutex
    trainer        *TrainerClient
    lastFineTune   time.Time
    sessionID      string
    callbacks      []func(event CollectionEvent)
    sessionStart   time.Time
    totalSamples   int64
}

type CollectionEvent struct {
    Type      string // "sample_added", "buffer_full", "training_triggered"
    Count     int
    Timestamp time.Time
}

func NewDataCollector() *DataCollector {
    cfg := config.Get()
    return &DataCollector{
        buffer:       make([]MovementSample, 0, cfg.PersonalizationBuffer),
        maxSize:      cfg.PersonalizationBuffer,
        trainer:      NewTrainerClient(cfg.PersonalizationServerURL),
        sessionID:    utils.GenerateID(),
        sessionStart: time.Now(),
        callbacks:    make([]func(CollectionEvent), 0),
    }
}

func (dc *DataCollector) AddSample(x, y, vx, vy float64) {
    dc.AddSampleWithAcceleration(x, y, vx, vy, 0, 0)
}

func (dc *DataCollector) AddSampleWithAcceleration(x, y, vx, vy, ax, ay float64) {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    
    sample := MovementSample{
        X:          x,
        Y:          y,
        VX:         vx,
        VY:         vy,
        AX:         ax,
        AY:         ay,
        Timestamp:  time.Now(),
        SessionID:  dc.sessionID,
        Confidence: dc.calculateConfidence(vx, vy),
    }
    
    if len(dc.buffer) >= dc.maxSize {
        dc.buffer = dc.buffer[1:]
    }
    dc.buffer = append(dc.buffer, sample)
    dc.totalSamples++
    
    dc.triggerEvent(CollectionEvent{
        Type:      "sample_added",
        Count:     len(dc.buffer),
        Timestamp: time.Now(),
    })
    
    cfg := config.Get()
    if cfg.EnablePersonalization && 
       time.Since(dc.lastFineTune) > time.Duration(cfg.PersonalizationInterval)*time.Second && 
       len(dc.buffer) >= cfg.PersonalizationBuffer {
        dc.triggerEvent(CollectionEvent{
            Type:      "training_triggered",
            Count:     len(dc.buffer),
            Timestamp: time.Now(),
        })
        go dc.triggerFineTune()
    }
}

func (dc *DataCollector) calculateConfidence(vx, vy float64) float64 {
    speed := math.Sqrt(vx*vx + vy*vy)
    if speed < 0.1 {
        return 0.3 // Low confidence for very slow movements
    }
    if speed > 100 {
        return 0.5 // Too fast, might be noise
    }
    return 0.7 + (speed/100)*0.3 // Higher confidence for natural speeds
}

func (dc *DataCollector) SampleCount() int {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    return len(dc.buffer)
}

func (dc *DataCollector) TotalSamples() int64 {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    return dc.totalSamples
}

func (dc *DataCollector) LastFineTune() time.Time {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    return dc.lastFineTune
}

func (dc *DataCollector) GetBuffer() []MovementSample {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    
    result := make([]MovementSample, len(dc.buffer))
    copy(result, dc.buffer)
    return result
}

func (dc *DataCollector) Clear() {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    dc.buffer = make([]MovementSample, 0, dc.maxSize)
    dc.sessionID = utils.GenerateID()
    dc.sessionStart = time.Now()
}

func (dc *DataCollector) ForceFineTune() error {
    dc.mu.Lock()
    if len(dc.buffer) == 0 {
        dc.mu.Unlock()
        return fmt.Errorf("no samples to train on")
    }
    
    bufferCopy := make([]MovementSample, len(dc.buffer))
    copy(bufferCopy, dc.buffer)
    dc.mu.Unlock()
    
    return dc.performTraining(bufferCopy)
}

func (dc *DataCollector) performTraining(samples []MovementSample) error {
    cfg := config.Get()
    
    // Prepare training data
    trainingData := make([]map[string]interface{}, len(samples))
    for i, s := range samples {
        trainingData[i] = map[string]interface{}{
            "x":     s.X,
            "y":     s.Y,
            "vx":    s.VX,
            "vy":    s.VY,
            "ax":    s.AX,
            "ay":    s.AY,
            "time":  s.Timestamp.UnixNano(),
            "conf":  s.Confidence,
        }
    }
    
    req := &FineTuneRequest{
        ModelPath:   cfg.MLModelPath,
        Buffer:      trainingData,
        OutputPath:  filepath.Join(filepath.Dir(cfg.MLModelPath), "personalized_model.onnx"),
        Config: map[string]interface{}{
            "epochs":        10,
            "batch_size":    32,
            "learning_rate": 0.001,
            "validation_split": 0.2,
        },
    }
    
    if err := dc.trainer.FineTune(req); err != nil {
        utils.LogError("Training failed: %v", err)
        return err
    }
    
    dc.mu.Lock()
    dc.lastFineTune = time.Now()
    if cfg.AutoSwapModel {
        // Signal model reload
        utils.LogInfo("Model updated with personalized weights")
    }
    dc.mu.Unlock()
    
    return nil
}

func (dc *DataCollector) triggerFineTune() {
    dc.performTraining(dc.GetBuffer())
}

func (dc *DataCollector) ExportData(filePath string) error {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    
    data := map[string]interface{}{
        "session_id":    dc.sessionID,
        "session_start": dc.sessionStart,
        "total_samples": dc.totalSamples,
        "samples":       dc.buffer,
    }
    
    jsonData, err := json.MarshalIndent(data, "", "  ")
    if err != nil {
        return err
    }
    
    return os.WriteFile(filePath, jsonData, 0644)
}

func (dc *DataCollector) ImportData(filePath string) error {
    data, err := os.ReadFile(filePath)
    if err != nil {
        return err
    }
    
    var importData struct {
        Samples []MovementSample `json:"samples"`
    }
    
    if err := json.Unmarshal(data, &importData); err != nil {
        return err
    }
    
    dc.mu.Lock()
    defer dc.mu.Unlock()
    
    for _, sample := range importData.Samples {
        if len(dc.buffer) >= dc.maxSize {
            dc.buffer = dc.buffer[1:]
        }
        dc.buffer = append(dc.buffer, sample)
        dc.totalSamples++
    }
    
    return nil
}

func (dc *DataCollector) AddEventListener(callback func(event CollectionEvent)) {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    dc.callbacks = append(dc.callbacks, callback)
}

func (dc *DataCollector) triggerEvent(event CollectionEvent) {
    dc.mu.Lock()
    callbacks := make([]func(CollectionEvent), len(dc.callbacks))
    copy(callbacks, dc.callbacks)
    dc.mu.Unlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (dc *DataCollector) GetStatistics() map[string]interface{} {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    
    var avgVX, avgVY, avgConf float64
    for _, s := range dc.buffer {
        avgVX += s.VX
        avgVY += s.VY
        avgConf += s.Confidence
    }
    count := len(dc.buffer)
    if count > 0 {
        avgVX /= float64(count)
        avgVY /= float64(count)
        avgConf /= float64(count)
    }
    
    return map[string]interface{}{
        "buffer_size":    count,
        "max_size":       dc.maxSize,
        "total_samples":  dc.totalSamples,
        "avg_velocity_x": avgVX,
        "avg_velocity_y": avgVY,
        "avg_confidence": avgConf,
        "session_id":     dc.sessionID,
        "session_duration": time.Since(dc.sessionStart).Seconds(),
        "last_training":  dc.lastFineTune,
    }
}
