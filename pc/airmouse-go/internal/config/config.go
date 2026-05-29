package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type Config struct {
	// Server
	Host          string `json:"host"`
	Port          int    `json:"port"`
	WebSocketPort int    `json:"websocket_port"`
	UDPPort       int    `json:"udp_port"`
	EnableTCP     bool   `json:"enable_tcp"`
	EnableWebSocket bool `json:"enable_websocket"`
	EnableUDP     bool   `json:"enable_udp"`
	EnableBluetooth bool `json:"enable_bluetooth"`
	EnableSerial  bool   `json:"enable_serial"`

	// Bluetooth
	BluetoothAdapter string `json:"bluetooth_adapter"`
	BLEEnabled       bool   `json:"ble_enabled"`
	HIDProxyEnabled  bool   `json:"hid_proxy_enabled"`

	// Security
	AuthEnabled   bool     `json:"auth_enabled"`
	AuthTokens    []string `json:"auth_tokens"`
	EncryptionKey string   `json:"encryption_key"`

	// Performance
	Sensitivity       float64 `json:"sensitivity"`
	MoveRateLimit     int     `json:"move_rate_limit"`
	BufferSize        int     `json:"buffer_size"`
	HeartbeatInterval int     `json:"heartbeat_interval"`
	ConnectionTimeout int     `json:"connection_timeout"`

	// Device
	MaxClients     int  `json:"max_clients"`
	ClientNames    bool `json:"client_names"`
	DeviceRegistry bool `json:"device_registry"`

	// Discovery
	DiscoveryPort int    `json:"discovery_port"`
	MDNSName      string `json:"mdns_name"`

	// UI
	AccentColor  string `json:"accent_color"`
	Theme        string `json:"theme"`
	AlwaysOnTop  bool   `json:"always_on_top"`
	ShowTrayIcon bool   `json:"show_tray_icon"`

	// Logging
	LogLevel string `json:"log_level"`
	LogFile  string `json:"log_file"`

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
		Host:              "0.0.0.0",
		Port:              8080,
		WebSocketPort:     8081,
		UDPPort:           8082,
		EnableTCP:         true,
		EnableWebSocket:   true,
		EnableUDP:         true,
		EnableBluetooth:   true,
		EnableSerial:      true,
		EnableAISmoothing: false,
		AIModelPath:       "models/mouse_smoothing.onnx",
		AIBlendFactor:     0.6,
		BluetoothAdapter:  "default",
		BLEEnabled:        true,
		HIDProxyEnabled:   false,
		AuthEnabled:       false,
		AuthTokens:        []string{},
		EncryptionKey:     "",
		Sensitivity:       0.5,
		MoveRateLimit:     60,
		BufferSize:        1024,
		HeartbeatInterval: 10,
		ConnectionTimeout: 30,
		MaxClients:        10,
		ClientNames:       true,
		DeviceRegistry:    true,
		DiscoveryPort:     8083,
		MDNSName:          "airmouse",
		AccentColor:       "#007acc",
		Theme:             "dark",
		AlwaysOnTop:       false,
		ShowTrayIcon:      true,
		LogLevel:          "info",
		LogFile:           "airmouse.log",
	}

	path := getConfigPath()
	if data, err := os.ReadFile(path); err == nil {
		json.Unmarshal(data, cfg)
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
	defer c.mu.Unlock()
	c.Sensitivity = s
	c.Save()
}

func (c *Config) SetTheme(theme string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.Theme = theme
	c.Save()
}

func getConfigPath() string {
	configDir, _ := os.UserConfigDir()
	return filepath.Join(configDir, "airmouse", "config.json")
}

// AI smoothing
EnableAISmoothing  bool    `json:"enable_ai_smoothing"`
AIModelPath        string  `json:"ai_model_path"`
AIBlendFactor      float64 `json:"ai_blend_factor"`



type Config struct {
    // ... existing fields ...
    EnablePersonalization   bool    `json:"enable_personalization"`
    PersonalizationBuffer   int     `json:"personalization_buffer"`
    PersonalizationInterval int     `json:"personalization_interval"`
    AutoSwapModel           bool    `json:"auto_swap_model"`
}


EnablePersonalization:   true,
PersonalizationBuffer:   2000,
PersonalizationInterval: 3600,
AutoSwapModel:           true,