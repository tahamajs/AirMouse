//go:build ml

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

// TrainingSample (duplicated here for local trainer; same structure as above).
// We redeclare to avoid import cycles.
// type TrainingSample struct { ... }  // already defined in trainer_client.go

// OnlineTrainer performs local training using a Python script.
type OnlineTrainer struct {
	modelPath    string
	pythonScript string
	buffer       []TrainingSample
	maxSize      int
	trainingChan chan []TrainingSample
	stopChan     chan struct{}
	mu           sync.Mutex
	lastSample   *TrainingSample
	isTraining   bool
	trainingMu   sync.Mutex
	stats        TrainerStats
	callbacks    []func(event TrainingEvent)
}

// TrainerStats holds statistics for the online trainer.
type TrainerStats struct {
	TotalSamples     int64
	TrainingCount    int64
	LastTrainingTime time.Time
	AverageLoss      float64
	ModelVersion     int
}

// TrainingEvent is emitted when training starts/completes/fails.
type TrainingEvent struct {
	Type      string // "training_started", "training_completed", "training_failed"
	Samples   int
	Loss      float64
	Timestamp time.Time
	Error     string
}

// NewOnlineTrainer creates a new online trainer.
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
	trainer.loadStats()
	go trainer.backgroundWorker()
	go trainer.statsUpdater()
	return trainer
}

// AddSample adds a new sample to the buffer and triggers training when full.
func (t *OnlineTrainer) AddSample(x, y float32) {
	t.mu.Lock()
	defer t.mu.Unlock()

	var velocityX, velocityY, accelX, accelY float32
	var dt float32 = 0.016 // assume 60 Hz

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
		Timestamp:     time.Now().UnixNano(),
	}
	t.buffer = append(t.buffer, sample)
	t.lastSample = &sample
	t.stats.TotalSamples++

	if len(t.buffer) >= t.maxSize {
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
			// channel full – skip this training
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
	if t.isTraining {
		t.trainingMu.Unlock()
		return
	}
	t.isTraining = true
	t.trainingMu.Unlock()
	defer func() { t.isTraining = false }()

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

// writeTrainingCSV writes samples to a CSV file for the Python script.
func (t *OnlineTrainer) writeTrainingCSV(samples []TrainingSample, path string) error {
	file, err := os.Create(path)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := csv.NewWriter(file)
	defer writer.Flush()

	writer.Write([]string{"x", "y", "vel_x", "vel_y", "accel_x", "accel_y", "dt"})
	var lastTime time.Time
	for i, s := range samples {
		dt := float32(0.016)
		if i > 0 {
			ts := time.Unix(0, s.Timestamp)
			dt = float32(ts.Sub(lastTime).Seconds())
		}
		lastTime = time.Unix(0, s.Timestamp)
		writer.Write([]string{
			fmt.Sprintf("%f", s.X),
			fmt.Sprintf("%f", s.Y),
			fmt.Sprintf("%f", s.VelocityX),
			fmt.Sprintf("%f", s.VelocityY),
			fmt.Sprintf("%f", s.AccelerationX),
			fmt.Sprintf("%f", s.AccelerationY),
			fmt.Sprintf("%f", dt),
		})
	}
	return nil
}

// parseLossFromOutput extracts loss from the Python script output.
func (t *OnlineTrainer) parseLossFromOutput(output string) float64 {
	// Simple stub – in production, parse JSON output from the script.
	var loss float64 = 0.01
	return loss
}

// ForceTraining triggers immediate training with the current buffer.
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

// GetStats returns a copy of the trainer statistics.
func (t *OnlineTrainer) GetStats() TrainerStats {
	t.mu.Lock()
	defer t.mu.Unlock()
	return t.stats
}

// AddEventListener registers a callback for training events.
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
		_ = json.Unmarshal(data, &t.stats)
	}
}

func (t *OnlineTrainer) saveStats() {
	statsPath := filepath.Join(filepath.Dir(t.modelPath), "training_stats.json")
	data, _ := json.MarshalIndent(t.stats, "", "  ")
	_ = os.WriteFile(statsPath, data, 0644)
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

// Stop shuts down the trainer and saves stats.
func (t *OnlineTrainer) Stop() {
	close(t.stopChan)
	t.saveStats()
}