package predictiveml

import (
    "fmt"
    "os/exec"
    "sync"
    "time"
)

type OnlineTrainer struct {
    modelPath    string
    pythonScript string
    buffer       []Sample
    maxSize      int
    trainingChan chan []Sample
    stopChan     chan struct{}
    mu           sync.Mutex
}

type Sample struct {
    X, Y  float32
    Time  time.Time
}

func NewOnlineTrainer(modelPath, pythonScriptPath string, bufferSize int) *OnlineTrainer {
    t := &OnlineTrainer{
        modelPath:    modelPath,
        pythonScript: pythonScriptPath,
        buffer:       make([]Sample, 0, bufferSize),
        maxSize:      bufferSize,
        trainingChan: make(chan []Sample, 1),
        stopChan:     make(chan struct{}),
    }
    go t.backgroundWorker()
    return t
}

func (t *OnlineTrainer) AddSample(x, y float32) {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.buffer = append(t.buffer, Sample{X: x, Y: y, Time: time.Now()})
    if len(t.buffer) >= t.maxSize {
        copyBuf := make([]Sample, len(t.buffer))
        copy(copyBuf, t.buffer)
        t.buffer = t.buffer[:0]
        select {
        case t.trainingChan <- copyBuf:
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
    // In production, write CSV and call Python training script
    // For demo, just log
    fmt.Printf("Training with %d samples\n", len(samples))
    cmd := exec.Command("python", t.pythonScript, "--model", t.modelPath)
    go cmd.Run()
}

func (t *OnlineTrainer) Stop() {
    close(t.stopChan)
}