package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type Config struct {
	// Server configuration
	Host             string   `json:"host"`
	Port             int      `json:"port"`
	WebSocketPort    int      `json:"websocket_port"`
	UDPPort          int      `json:"udp_port"`
	EnableWebSocket  bool     `json:"enable_websocket"`
	EnableUDP        bool     `json:"enable_udp"`
	EnableTCP        bool     `json:"enable_tcp"`
	EnableBluetooth  bool     `json:"enable_bluetooth"`
	EnableSerial     bool     `json:"enable_serial"`
	
	// Bluetooth configuration
	BluetoothAdapter string   `json:"bluetooth_adapter"`
	BLEEnabled       bool     `json:"ble_enabled"`
	HIDProxyEnabled  bool     `json:"hid_proxy_enabled"`
	
	// Security
	AuthEnabled      bool     `json:"auth_enabled"`
	AuthTokens       []string `json:"auth_tokens"`
	EncryptionKey    string   `json:"encryption_key"`
	
	// Performance
	Sensitivity          float64 `json:"sensitivity"`
	MoveRateLimit        int     `json:"move_rate_limit"`        // moves per second
	BufferSize           int     `json:"buffer_size"`
	HeartbeatInterval    int     `json:"heartbeat_interval"`     // seconds
	ConnectionTimeout    int     `json:"connection_timeout"`     // seconds
	
	// Device management
	MaxClients           int     `json:"max_clients"`
	ClientNames          bool    `json:"client_names"`
	DeviceRegistry       bool    `json:"device_registry"`
	
	// Discovery
	DiscoveryPort        int     `json:"discovery_port"`
	MDNSName             string  `json:"mdns_name"`
	
	// UI
	AccentColor          string  `json:"accent_color"`
	Theme                string  `json:"theme"` // dark, light, pure_black, high_contrast, ocean, sunset, forest, purple, cherry, neon, lavender, mint, peach, sky
	AlwaysOnTop          bool    `json:"always_on_top"`
	ShowTrayIcon         bool    `json:"show_tray_icon"`
	
	// Logging
	LogLevel             string  `json:"log_level"`
	LogFile              string  `json:"log_file"`
	
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
		Host:                "0.0.0.0",
		Port:                8080,
		WebSocketPort:       8081,
		UDPPort:             8082,
		EnableWebSocket:     true,
		EnableUDP:           true,
		EnableTCP:           true,
		EnableBluetooth:     true,
		EnableSerial:        true,
		BluetoothAdapter:    "default",
		BLEEnabled:          true,
		HIDProxyEnabled:     false,
		AuthEnabled:         false,
		AuthTokens:          []string{},
		EncryptionKey:       "",
		Sensitivity:         0.5,
		MoveRateLimit:       60,
		BufferSize:          1024,
		HeartbeatInterval:   10,
		ConnectionTimeout:   30,
		MaxClients:          10,
		ClientNames:         true,
		DeviceRegistry:      true,
		DiscoveryPort:       8083,
		MDNSName:            "airmouse",
		AccentColor:         "#007acc",
		Theme:               "dark",
		AlwaysOnTop:         false,
		ShowTrayIcon:        true,
		LogLevel:            "info",
		LogFile:             "airmouse.log",
	}
	
	configPath := getConfigPath()
	if data, err := os.ReadFile(configPath); err == nil {
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