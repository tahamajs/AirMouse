package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type Config struct {
	// Server
	Host            string `json:"host"`
	Port            int    `json:"port"`
	WebSocketPort   int    `json:"websocket_port"`
	UDPPort         int    `json:"udp_port"`
	EnableTCP       bool   `json:"enable_tcp"`
	EnableWebSocket bool   `json:"enable_websocket"`
	EnableUDP       bool   `json:"enable_udp"`
	EnableBluetooth bool   `json:"enable_bluetooth"`

	// Security
	AuthEnabled bool     `json:"auth_enabled"`
	AuthSecret  string   `json:"auth_secret"`
	AuthTokens  []string `json:"auth_tokens"`

	// Performance
	Sensitivity       float64 `json:"sensitivity"`
	MoveRateLimit     int     `json:"move_rate_limit"`
	HeartbeatInterval int     `json:"heartbeat_interval"`
	ConnectionTimeout int     `json:"connection_timeout"`

	// Device
	MaxClients  int  `json:"max_clients"`
	ClientNames bool `json:"client_names"`

	// Discovery
	DiscoveryPort int    `json:"discovery_port"`
	MDNSName      string `json:"mdns_name"`

	// UI
	Theme        string `json:"theme"`
	AlwaysOnTop  bool   `json:"always_on_top"`
	ShowTrayIcon bool   `json:"show_tray_icon"`

	// Logging
	LogLevel string `json:"log_level"`
	LogFile  string `json:"log_file"`

	// AI & Personalization
	EnableAISmoothing       bool    `json:"enable_ai_smoothing"`
	AIModelPath             string  `json:"ai_model_path"`
	AIBlendFactor           float64 `json:"ai_blend_factor"`
	EnablePersonalization   bool    `json:"enable_personalization"`
	PersonalizationBuffer   int     `json:"personalization_buffer"`
	PersonalizationInterval int     `json:"personalization_interval"`
	AutoSwapModel           bool    `json:"auto_swap_model"`
	EnablePredictive        bool    `json:"enable_predictive"`
	PredictiveBlendFactor   float64 `json:"predictive_blend_factor"`

	mu sync.RWMutex
}

var instance *Config
var once sync.Once

func Get() *Config {
	once.Do(func() {
		instance = loadOrDefault()
	})
	return instance
}

func loadOrDefault() *Config {
	cfg := &Config{
		Host:                    "0.0.0.0",
		Port:                    8080,
		WebSocketPort:           8081,
		UDPPort:                 8082,
		EnableTCP:               true,
		EnableWebSocket:         true,
		EnableUDP:               true,
		EnableBluetooth:         true,
		AuthEnabled:             false,
		AuthSecret:              "",
		AuthTokens:              []string{},
		Sensitivity:             0.5,
		MoveRateLimit:           60,
		HeartbeatInterval:       10,
		ConnectionTimeout:       30,
		MaxClients:              10,
		ClientNames:             true,
		DiscoveryPort:           8083,
		MDNSName:                "airmouse",
		Theme:                   "dark",
		AlwaysOnTop:             false,
		ShowTrayIcon:            true,
		LogLevel:                "info",
		LogFile:                 "airmouse.log",
		EnableAISmoothing:       false,
		AIModelPath:             "models/mouse_smoothing.onnx",
		AIBlendFactor:           0.6,
		EnablePersonalization:   false,
		PersonalizationBuffer:   2000,
		PersonalizationInterval: 3600,
		AutoSwapModel:           false,
		EnablePredictive:        true,
		PredictiveBlendFactor:   0.6,
	}
	path := getConfigPath()
	if data, err := os.ReadFile(path); err == nil {
		_ = json.Unmarshal(data, cfg)
	}
	return cfg
}

func (c *Config) Save() error {
	c.mu.RLock()
	defer c.mu.RUnlock()
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(getConfigPath(), data, 0644)
}

func (c *Config) SetSensitivity(s float64) {
	c.mu.Lock()
	c.Sensitivity = s
	c.mu.Unlock()
	_ = c.Save()
}

func (c *Config) SetTheme(theme string) {
	c.mu.Lock()
	c.Theme = theme
	c.mu.Unlock()
	_ = c.Save()
}

func getConfigPath() string {
	configDir, _ := os.UserConfigDir()
	return filepath.Join(configDir, "airmouse", "config.json")
}
