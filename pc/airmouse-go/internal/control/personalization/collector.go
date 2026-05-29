package personalization

import (
    "sync"
    "time"
    "airmouse-go/internal/config"
)

type MovementSample struct {
    X, Y, VX, VY float64
    Timestamp    time.Time
}

type DataCollector struct {
    buffer      []MovementSample
    maxSize     int
    mu          sync.Mutex
    trainer     *TrainerClient
    lastFineTune time.Time
}

func NewDataCollector() *DataCollector {
    return &DataCollector{
        buffer:  make([]MovementSample, 0, config.Get().PersonalizationBuffer),
        maxSize: config.Get().PersonalizationBuffer,
        trainer: NewTrainerClient("http://localhost:5001"),
    }
}

func (dc *DataCollector) AddSample(x, y, vx, vy float64) {
    dc.mu.Lock()
    defer dc.mu.Unlock()
    if len(dc.buffer) >= dc.maxSize {
        dc.buffer = dc.buffer[1:]
    }
    dc.buffer = append(dc.buffer, MovementSample{X: x, Y: y, VX: vx, VY: vy, Timestamp: time.Now()})

    cfg := config.Get()
    if cfg.EnablePersonalization && time.Since(dc.lastFineTune) > time.Duration(cfg.PersonalizationInterval)*time.Second && len(dc.buffer) >= cfg.PersonalizationBuffer {
        go dc.triggerFineTune()
    }
}

func (dc *DataCollector) triggerFineTune() {
    dc.mu.Lock()
    bufferCopy := make([]MovementSample, len(dc.buffer))
    copy(bufferCopy, dc.buffer)
    dc.mu.Unlock()
    // Convert to JSON-serializable map
    data := make([]map[string]interface{}, len(bufferCopy))
    for i, sample := range bufferCopy {
        data[i] = map[string]interface{}{
            "x": sample.X, "y": sample.Y, "vx": sample.VX, "vy": sample.VY,
        }
    }
    req := &FineTuneRequest{
        ModelPath:  "models/base_model.pth",
        Buffer:     data,
        OutputPath: "models/personalized_model.pth",
    }
    if err := dc.trainer.FineTune(req); err == nil && config.Get().AutoSwapModel {
        // Signal the main application to reload the new model
    }
}