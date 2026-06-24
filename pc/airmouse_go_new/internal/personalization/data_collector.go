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

// MovementSample represents a single sample of cursor movement.
type MovementSample struct {
	X, Y       float64   // Position
	VX, VY     float64   // Velocity
	AX, AY     float64   // Acceleration
	Timestamp  time.Time // When the sample was taken
	SessionID  string    // Unique session identifier
	Confidence float64   // Confidence in the sample (0–1)
}

// DataCollector collects movement samples and triggers fine‑tuning.
type DataCollector struct {
	buffer       []MovementSample
	maxSize      int
	mu           sync.Mutex
	trainer      *TrainerClient
	lastFineTune time.Time
	sessionID    string
	callbacks    []func(event CollectionEvent)
	sessionStart time.Time
	totalSamples int64
}

// CollectionEvent is emitted when samples are added or training is triggered.
type CollectionEvent struct {
	Type      string // "sample_added", "buffer_full", "training_triggered"
	Count     int
	Timestamp time.Time
}

// NewDataCollector creates a new data collector.
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

// AddSample adds a movement sample (with zero acceleration).
func (dc *DataCollector) AddSample(x, y, vx, vy float64) {
	dc.AddSampleWithAcceleration(x, y, vx, vy, 0, 0)
}

// AddSampleWithAcceleration adds a sample with acceleration data.
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

// calculateConfidence returns a confidence score based on velocity.
func (dc *DataCollector) calculateConfidence(vx, vy float64) float64 {
	speed := math.Sqrt(vx*vx + vy*vy)
	if speed < 0.1 {
		return 0.3
	}
	if speed > 100 {
		return 0.5
	}
	return 0.7 + (speed/100)*0.3
}

// SampleCount returns the current number of samples in the buffer.
func (dc *DataCollector) SampleCount() int {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	return len(dc.buffer)
}

// TotalSamples returns the total number of samples collected (ever).
func (dc *DataCollector) TotalSamples() int64 {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	return dc.totalSamples
}

// LastFineTune returns the timestamp of the last fine‑tuning.
func (dc *DataCollector) LastFineTune() time.Time {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	return dc.lastFineTune
}

// GetBuffer returns a copy of the current buffer.
func (dc *DataCollector) GetBuffer() []MovementSample {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	result := make([]MovementSample, len(dc.buffer))
	copy(result, dc.buffer)
	return result
}

// Clear empties the buffer and starts a new session.
func (dc *DataCollector) Clear() {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	dc.buffer = make([]MovementSample, 0, dc.maxSize)
	dc.sessionID = utils.GenerateID()
	dc.sessionStart = time.Now()
}

// ForceFineTune forces an immediate training run on the current buffer.
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

// performTraining sends the samples to the trainer service.
func (dc *DataCollector) performTraining(samples []MovementSample) error {
	cfg := config.Get()

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
		ModelPath:  cfg.MLModelPath,
		Buffer:     trainingData,
		OutputPath: filepath.Join(filepath.Dir(cfg.MLModelPath), "personalized_model.onnx"),
		Config: map[string]interface{}{
			"epochs":           10,
			"batch_size":       32,
			"learning_rate":    0.001,
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
		utils.LogInfo("Model updated with personalized weights")
	}
	dc.mu.Unlock()
	return nil
}

// triggerFineTune runs training asynchronously.
func (dc *DataCollector) triggerFineTune() {
	_ = dc.performTraining(dc.GetBuffer())
}

// ExportData saves the current buffer to a JSON file.
func (dc *DataCollector) ExportData(filePath string) error {
	dc.mu.Lock()
	defer dc.mu.Unlock()

	data := map[string]interface{}{
		"session_id":     dc.sessionID,
		"session_start":  dc.sessionStart,
		"total_samples":  dc.totalSamples,
		"samples":        dc.buffer,
	}
	jsonData, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filePath, jsonData, 0644)
}

// ImportData loads samples from a JSON file and adds them to the buffer.
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

// AddEventListener registers a callback for collection events.
func (dc *DataCollector) AddEventListener(callback func(event CollectionEvent)) {
	dc.mu.Lock()
	defer dc.mu.Unlock()
	dc.callbacks = append(dc.callbacks, callback)
}

// triggerEvent invokes all registered callbacks.
func (dc *DataCollector) triggerEvent(event CollectionEvent) {
	dc.mu.Lock()
	callbacks := make([]func(CollectionEvent), len(dc.callbacks))
	copy(callbacks, dc.callbacks)
	dc.mu.Unlock()
	for _, cb := range callbacks {
		go cb(event)
	}
}

// GetStatistics returns statistics about the collector.
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
		"buffer_size":       count,
		"max_size":          dc.maxSize,
		"total_samples":     dc.totalSamples,
		"avg_velocity_x":    avgVX,
		"avg_velocity_y":    avgVY,
		"avg_confidence":    avgConf,
		"session_id":        dc.sessionID,
		"session_duration":  time.Since(dc.sessionStart).Seconds(),
		"last_training":     dc.lastFineTune,
	}
}