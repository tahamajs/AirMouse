package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type Config struct {
	Port                      string  `json:"port"`
	WebSocketPort             int     `json:"websocket_port"`
	Sensitivity               float64 `json:"sensitivity"`
	PredictiveBlendFactor     float64 `json:"predictive_blend_factor"`
	GestureConfidenceThreshold float64 `json:"gesture_confidence_threshold"`
	LogLevel                  string  `json:"log_level"`
	EnableAI                  bool    `json:"enable_ai"`
	ModelPath                 string  `json:"model_path"`
	EnablePredictive          bool    `json:"enable_predictive"`
	EnablePersonalization     bool    `json:"enable_personalization"`
	PersonalizationBuffer     int     `json:"personalization_buffer"`
	PersonalizationInterval   int     `json:"personalization_interval"`
	AutoSwapModel             bool    `json:"auto_swap_model"`
	EnableMLPrediction        bool    `json:"enable_ml_prediction"`
	MLModelPath               string  `json:"ml_model_path"`
	MLSequenceLength          int     `json:"ml_sequence_length"`
	MLBlendFactor             float64 `json:"ml_blend_factor"`
	MLInferenceInterval       int     `json:"ml_inference_interval_ms"`
	AuthEnabled               bool    `json:"auth_enabled"`
	AuthSecret                string  `json:"auth_secret"`
	mu                        sync.RWMutex
}

var instance *Config
var once sync.Once

func Load() *Config {
	once.Do(func() {
		instance = loadOrDefault()
	})
	return instance
}

func loadOrDefault() *Config {
	cfg := &Config{
		Port:                      "8080",
		WebSocketPort:             8081,
		Sensitivity:               0.5,
		PredictiveBlendFactor:     0.6,
		GestureConfidenceThreshold: 0.7,
		LogLevel:                  "info",
		EnableAI:                  false,
		ModelPath:                 "models/mouse_smoothing.onnx",
		EnablePredictive:          true,
		EnablePersonalization:     false,
		PersonalizationBuffer:     2000,
		PersonalizationInterval:   3600,
		AutoSwapModel:             false,
		EnableMLPrediction:        false,
		MLModelPath:               "models/lstm_predictor.onnx",
		MLSequenceLength:          16,
		MLBlendFactor:             0.6,
		MLInferenceInterval:       20,
		AuthEnabled:               false,
		AuthSecret:                "",
	}
	path := getConfigPath()
	if data, err := os.ReadFile(path); err == nil {
		_ = json.Unmarshal(data, cfg)
	}
	return cfg
}

func (c *Config) Save() error {
	c.mu.RLock()
	snapshot := *c
	c.mu.RUnlock()
	data, err := json.MarshalIndent(&snapshot, "", "  ")
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(getConfigPath()), 0o755); err != nil {
		return err
	}
	return os.WriteFile(getConfigPath(), data, 0o644)
}

func getConfigPath() string {
	configDir, _ := os.UserConfigDir()
	return filepath.Join(configDir, "airmouse", "config.json")
}

func (c *Config) SetSensitivity(s float64) {
	c.mu.Lock()
	c.Sensitivity = s
	c.mu.Unlock()
	_ = c.Save()
}

func (c *Config) SetPredictiveEnabled(enabled bool) {
	c.mu.Lock()
	c.EnablePredictive = enabled
	c.mu.Unlock()
	_ = c.Save()
}

func (c *Config) SetPredictiveBlendFactor(factor float64) {
	c.mu.Lock()
	c.PredictiveBlendFactor = factor
	c.mu.Unlock()
	_ = c.Save()
}