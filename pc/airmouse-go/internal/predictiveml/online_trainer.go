package predictiveml

import (
    "encoding/json"
    "fmt"
    "log"
    "os"
    "sync"
    "time"
)

// Sample represents a single data point for online training.
type Sample struct {
    X, Y  float32
    Time  time.Time
}

// OnlineTrainer collects movement data and triggers background training.
type OnlineTrainer struct {
    buffer       []Sample
    maxSize      int
    trainingChan chan []Sample
    stopChan     chan struct{}
    wg           sync.WaitGroup
    mu           sync.Mutex
    modelPath    string
    pythonCmd    string
}

// NewOnlineTrainer creates a new trainer that periodically calls an external Python script.
func NewOnlineTrainer(modelPath, pythonScriptPath string, bufferSize int) *OnlineTrainer {
    t := &OnlineTrainer{
        buffer:       make([]Sample, 0, bufferSize),
        maxSize:      bufferSize,
        trainingChan: make(chan []Sample, 1),
        stopChan:     make(chan struct{}),
        modelPath:    modelPath,
        pythonCmd:    fmt.Sprintf("python %s", pythonScriptPath),
    }
    go t.backgroundWorker()
    return t
}

// AddSample adds a new cursor movement to the buffer.
func (t *OnlineTrainer) AddSample(x, y float32) {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.buffer = append(t.buffer, Sample{X: x, Y: y, Time: time.Now()})
    if len(t.buffer) >= t.maxSize {
        // Trigger training
        copyBuffer := make([]Sample, len(t.buffer))
        copy(copyBuffer, t.buffer)
        t.buffer = t.buffer[:0]
        select {
        case t.trainingChan <- copyBuffer:
        default:
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

func (t *OnlineTrainer) runTraining(samples []Sample) {
    // Save samples to a temporary CSV file
    csvPath := "/tmp/ml_train_data.csv"
    f, err := os.Create(csvPath)
    if err != nil {
        log.Printf("online trainer: failed to create CSV: %v", err)
        return
    }
    for _, s := range samples {
        fmt.Fprintf(f, "%f,%f\n", s.X, s.Y)
    }
    f.Close()

    // Call Python training script (runs in background)
    go func() {
        cmd := exec.Command("python", "python/online_train.py",
            "--model", t.modelPath,
            "--data", csvPath)
        if output, err := cmd.CombinedOutput(); err != nil {
            log.Printf("online trainer: training failed: %v\n%s", err, output)
        } else {
            log.Printf("online trainer: model fine‑tuned successfully")
        }
        os.Remove(csvPath)
    }()
}

// Stop terminates the background worker.
func (t *OnlineTrainer) Stop() {
    close(t.stopChan)
    t.wg.Wait()
}