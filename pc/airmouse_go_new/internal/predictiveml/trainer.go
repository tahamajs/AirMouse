package predictiveml

import (
    "encoding/csv"
    "encoding/json"
    "fmt"
    "os"
    "os/exec"
    "path/filepath"
    "sync"
    "time"
)

type TrainingSample struct {
    X, Y          float32
    VelocityX     float32
    VelocityY     float32
    AccelerationX float32
    AccelerationY float32
    Timestamp     time.Time
}

type OnlineTrainer struct {
    modelPath      string
    pythonScript   string
    buffer         []TrainingSample
    maxSize        int
    trainingChan   chan []TrainingSample
    stopChan       chan struct{}
    mu             sync.Mutex
    lastSample     *TrainingSample
    isTraining     bool
    trainingMu     sync.Mutex
    stats          TrainerStats
    callbacks      []func(event TrainingEvent)
}

type TrainerStats struct {
    TotalSamples     int64
    TrainingCount    int64
    LastTrainingTime time.Time
    AverageLoss      float64
    ModelVersion     int
}

type TrainingEvent struct {
    Type      string // "training_started", "training_completed", "training_failed"
    Samples   int
    Loss      float64
    Timestamp time.Time
    Error     string
}

func NewOnlineTrainer(modelPath, pythonScriptPath string, bufferSize int) *OnlineTrainer {
    trainer := &OnlineTrainer{
        modelPath:    modelPath,
        pythonScript: pythonScriptPath,
        buffer:       make([]TrainingSample, 0, bufferSize),
        maxSize:      bufferSize,
        trainingChan: make(chan []TrainingSample, 5),
        stopChan:     make(chan struct{}),
        callbacks:    make([]func(TrainingEvent), 0),
    }
    
    // Load existing stats if available
    trainer.loadStats()
    
    go trainer.backgroundWorker()
    go trainer.statsUpdater()
    
    return trainer
}

func (t *OnlineTrainer) AddSample(x, y float32) {
    t.mu.Lock()
    defer t.mu.Unlock()
    
    var velocityX, velocityY, accelX, accelY float32
    var dt float32 = 0.016 // Assume 60fps
    
    if t.lastSample != nil {
        dt = float32(time.Since(t.lastSample.Timestamp).Seconds())
        if dt > 0 {
            velocityX = (x - t.lastSample.X) / dt
            velocityY = (y - t.lastSample.Y) / dt
            
            if t.lastSample.VelocityX != 0 {
                accelX = (velocityX - t.lastSample.VelocityX) / dt
                accelY = (velocityY - t.lastSample.VelocityY) / dt
            }
        }
    }
    
    sample := TrainingSample{
        X:             x,
        Y:             y,
        VelocityX:     velocityX,
        VelocityY:     velocityY,
        AccelerationX: accelX,
        AccelerationY: accelY,
        Timestamp:     time.Now(),
    }
    
    t.buffer = append(t.buffer, sample)
    t.lastSample = &sample
    t.stats.TotalSamples++
    
    if len(t.buffer) >= t.maxSize {
        // Trigger training
        copyBuf := make([]TrainingSample, len(t.buffer))
        copy(copyBuf, t.buffer)
        t.buffer = t.buffer[:0]
        
        select {
        case t.trainingChan <- copyBuf:
            t.triggerEvent(TrainingEvent{
                Type:      "training_started",
                Samples:   len(copyBuf),
                Timestamp: time.Now(),
            })
        default:
            fmt.Println("Training channel full, skipping training")
        }
    }
}

func (t *OnlineTrainer) backgroundWorker() {
    for {
        select {
        case samples := <-t.trainingChan:
            t.runTraining(samples)
        case <-t.stopChan:
            return
        }
    }
}

func (t *OnlineTrainer) runTraining(samples []TrainingSample) {
    t.trainingMu.Lock()
    defer t.trainingMu.Unlock()
    
    if t.isTraining {
        return
    }
    t.isTraining = true
    defer func() { t.isTraining = false }()
    
    // Prepare training data
    csvPath := filepath.Join(os.TempDir(), fmt.Sprintf("training_%d.csv", time.Now().Unix()))
    if err := t.writeTrainingCSV(samples, csvPath); err != nil {
        t.triggerEvent(TrainingEvent{
            Type:      "training_failed",
            Samples:   len(samples),
            Timestamp: time.Now(),
            Error:     err.Error(),
        })
        return
    }
    
    // Call Python training script
    cmd := exec.Command("python", t.pythonScript, 
        "--data", csvPath,
        "--model", t.modelPath,
        "--epochs", "10",
        "--batch-size", "32")
    
    output, err := cmd.CombinedOutput()
    if err != nil {
        t.triggerEvent(TrainingEvent{
            Type:      "training_failed",
            Samples:   len(samples),
            Timestamp: time.Now(),
            Error:     fmt.Sprintf("%s: %s", err.Error(), output),
        })
        os.Remove(csvPath)
        return
    }
    
    // Parse loss from output
    loss := t.parseLossFromOutput(string(output))
    
    t.stats.TrainingCount++
    t.stats.LastTrainingTime = time.Now()
    t.stats.AverageLoss = (t.stats.AverageLoss*float64(t.stats.TrainingCount-1) + loss) / float64(t.stats.TrainingCount)
    t.stats.ModelVersion++
    
    t.saveStats()
    
    t.triggerEvent(TrainingEvent{
        Type:      "training_completed",
        Samples:   len(samples),
        Loss:      loss,
        Timestamp: time.Now(),
    })
    
    os.Remove(csvPath)
}

func (t *OnlineTrainer) writeTrainingCSV(samples []TrainingSample, path string) error {
    file, err := os.Create(path)
    if err != nil {
        return err
    }
    defer file.Close()
    
    writer := csv.NewWriter(file)
    defer writer.Flush()
    
    // Write header
    writer.Write([]string{"x", "y", "vel_x", "vel_y", "accel_x", "accel_y", "dt"})
    
    var lastTime time.Time
    for i, sample := range samples {
        dt := float32(0.016)
        if i > 0 {
            dt = float32(sample.Timestamp.Sub(lastTime).Seconds())
        }
        lastTime = sample.Timestamp
        
        writer.Write([]string{
            fmt.Sprintf("%f", sample.X),
            fmt.Sprintf("%f", sample.Y),
            fmt.Sprintf("%f", sample.VelocityX),
            fmt.Sprintf("%f", sample.VelocityY),
            fmt.Sprintf("%f", sample.AccelerationX),
            fmt.Sprintf("%f", sample.AccelerationY),
            fmt.Sprintf("%f", dt),
        })
    }
    
    return nil
}

func (t *OnlineTrainer) parseLossFromOutput(output string) float64 {
    // Simple loss parsing - in production, parse JSON output
    var loss float64 = 0.01
    // Would parse actual loss from Python script output
    return loss
}

func (t *OnlineTrainer) ForceTraining() error {
    t.mu.Lock()
    if len(t.buffer) == 0 {
        t.mu.Unlock()
        return fmt.Errorf("no samples to train on")
    }
    copyBuf := make([]TrainingSample, len(t.buffer))
    copy(copyBuf, t.buffer)
    t.buffer = t.buffer[:0]
    t.mu.Unlock()
    
    select {
    case t.trainingChan <- copyBuf:
        return nil
    default:
        return fmt.Errorf("training already in progress")
    }
}

func (t *OnlineTrainer) GetStats() TrainerStats {
    t.mu.Lock()
    defer t.mu.Unlock()
    return t.stats
}

func (t *OnlineTrainer) AddEventListener(callback func(event TrainingEvent)) {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.callbacks = append(t.callbacks, callback)
}

func (t *OnlineTrainer) triggerEvent(event TrainingEvent) {
    t.mu.Lock()
    callbacks := make([]func(TrainingEvent), len(t.callbacks))
    copy(callbacks, t.callbacks)
    t.mu.Unlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (t *OnlineTrainer) loadStats() {
    statsPath := filepath.Join(filepath.Dir(t.modelPath), "training_stats.json")
    data, err := os.ReadFile(statsPath)
    if err == nil {
        json.Unmarshal(data, &t.stats)
    }
}

func (t *OnlineTrainer) saveStats() {
    statsPath := filepath.Join(filepath.Dir(t.modelPath), "training_stats.json")
    data, _ := json.MarshalIndent(t.stats, "", "  ")
    os.WriteFile(statsPath, data, 0644)
}

func (t *OnlineTrainer) statsUpdater() {
    ticker := time.NewTicker(5 * time.Second)
    defer ticker.Stop()
    
    for {
        select {
        case <-ticker.C:
            t.saveStats()
        case <-t.stopChan:
            return
        }
    }
}

func (t *OnlineTrainer) Stop() {
    close(t.stopChan)
    t.saveStats()
}