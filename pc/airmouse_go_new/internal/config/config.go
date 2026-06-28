package config

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// Config holds all application settings.
type Config struct {
	// Server settings
	Port          int    `json:"port"`
	WebSocketPort int    `json:"websocket_port"`
	UDPPort       int    `json:"udp_port"`
	Host          string `json:"host"`
	ServerName    string `json:"server_name"`
	UserName      string `json:"user_name"`
	Version       string `json:"version"`
	Language      string `json:"language"`

	// Protocol settings
	EnableTCP       bool `json:"enable_tcp"`
	EnableWebSocket bool `json:"enable_websocket"`
	EnableUDP       bool `json:"enable_udp"`
	EnableBluetooth bool `json:"enable_bluetooth"`
	EnableSerial    bool `json:"enable_serial"`

	// Mouse settings
	Sensitivity         float64 `json:"sensitivity"`
	SmoothingEnabled    bool    `json:"smoothing_enabled"`
	AccelerationEnabled bool    `json:"acceleration_enabled"`
	AccelerationFactor  float64 `json:"acceleration_factor"`
	MaxAcceleration     float64 `json:"max_acceleration"`
	HapticEnabled       bool    `json:"haptic_enabled"`

	// AI settings
	EnableAISmoothing        bool          `json:"enable_ai_smoothing"`
	EnablePredictive         bool          `json:"enable_predictive"`
	PredictiveBlendFactor    float64       `json:"predictive_blend_factor"`
	PredictiveDt             float64       `json:"predictive_dt"`
	EnableJitterCompensation bool          `json:"enable_jitter_compensation"`
	JitterBlendFactor        float64       `json:"jitter_blend_factor"`
	JitterMaxLatency         time.Duration `json:"jitter_max_latency"`
	JitterPredictionWindow   time.Duration `json:"jitter_prediction_window"`
	JitterUseKalman          bool          `json:"jitter_use_kalman"`
	JitterUseAcceleration    bool          `json:"jitter_use_acceleration"`

	// Gesture settings
	GestureConfidenceThreshold float64 `json:"gesture_confidence_threshold"`
	GestureCooldown            int     `json:"gesture_cooldown_ms"`
	ClickThreshold             float64 `json:"click_threshold"`
	ScrollThreshold            float64 `json:"scroll_threshold"`
	DoubleClickInterval        int64   `json:"double_click_interval"`
	RightClickTilt             float64 `json:"right_click_tilt"`

	// Proximity settings
	ProximityEnabled       bool    `json:"proximity_enabled"`
	ProximityNearThreshold float64 `json:"proximity_near_threshold"`
	ProximityFarThreshold  float64 `json:"proximity_far_threshold"`
	ProximityScanInterval  int     `json:"proximity_scan_interval_ms"`
	BluetoothAdapter       string  `json:"bluetooth_adapter"`
	BLEEnabled             bool    `json:"ble_enabled"`
	HIDProxyEnabled        bool    `json:"hid_proxy_enabled"`

	// Personalization
	EnablePersonalization    bool   `json:"enable_personalization"`
	PersonalizationBuffer    int    `json:"personalization_buffer"`
	PersonalizationInterval  int    `json:"personalization_interval"`
	PersonalizationServerURL string `json:"personalization_server_url"`
	AutoSwapModel            bool   `json:"auto_swap_model"`

	// ML settings
	EnableMLPrediction  bool    `json:"enable_ml_prediction"`
	MLModelPath         string  `json:"ml_model_path"`
	MLSequenceLength    int     `json:"ml_sequence_length"`
	MLBlendFactor       float64 `json:"ml_blend_factor"`
	MLInferenceInterval int     `json:"ml_inference_interval_ms"`

	// Humanizer
	EnableHumanizer            bool    `json:"enable_humanizer"`
	HumanizerTremorAmplitude   float64 `json:"humanizer_tremor_amplitude"`
	HumanizerBSplineSegments   int     `json:"humanizer_bspline_segments"`
	HumanizerNoiseAmplitude    float64 `json:"humanizer_noise_amplitude"`
	HumanizerVelocityPeakRatio float64 `json:"humanizer_velocity_peak_ratio"`

	// Particle filter
	EnableParticleFilter       bool `json:"enable_particle_filter"`
	ParticleFilterNumParticles int  `json:"particle_filter_num_particles"`

	// Authentication
	AuthEnabled     bool     `json:"auth_enabled"`
	AuthSecret      string   `json:"auth_secret"`
	AuthToken       string   `json:"auth_token"`
	AuthTokens      []string `json:"auth_tokens"`
	EncryptionKey   string   `json:"encryption_key"`
	EnablePairingUI bool     `json:"enable_pairing_ui"`
	TokenTTL        int      `json:"token_ttl_hours"`

	// Trusted devices (auto‑approval)
	TrustedDevices []string `json:"trusted_devices"`

	// Performance
	MoveRateLimit     int `json:"move_rate_limit"`
	HeartbeatInterval int `json:"heartbeat_interval"`
	ConnectionTimeout int `json:"connection_timeout"`
	BufferSize        int `json:"buffer_size"`
	InactiveTimeout   int `json:"inactive_timeout_seconds"`

	// Device
	MaxClients     int  `json:"max_clients"`
	ClientNames    bool `json:"client_names"`
	DeviceRegistry bool `json:"device_registry"`

	// Discovery
	DiscoveryPort int    `json:"discovery_port"`
	MDNSName      string `json:"mdns_name"`

	// Logging
	LogLevel       string `json:"log_level"`
	LogFile        string `json:"log_file"`
	LogColor       bool   `json:"log_color"`
	DebugMode      bool   `json:"debug_mode"`
	MetricsEnabled bool   `json:"metrics_enabled"`

	// UI settings
	Theme           string `json:"theme"`
	AccentColor     string `json:"accent_color"`
	AlwaysOnTop     bool   `json:"always_on_top"`
	ShowTrayIcon    bool   `json:"show_tray_icon"`
	WindowWidth     int    `json:"window_width"`
	WindowHeight    int    `json:"window_height"`
	AutoStartServer bool   `json:"auto_start_server"`

	// USB Gadget
	USBGadgetEnabled bool   `json:"usb_gadget_enabled"`
	USBVendorID      string `json:"usb_vendor_id"`
	USBProductID     string `json:"usb_product_id"`
	USBManufacturer  string `json:"usb_manufacturer"`
	USBProduct       string `json:"usb_product"`

	// Paths
	ModelPath  string `json:"model_path"`
	ConfigPath string `json:"-"`
	LogPath    string `json:"-"`

	// First launch flag (for onboarding)
	FirstLaunch bool `json:"first_launch"`

	mu *sync.RWMutex `json:"-"`
}

var (
	instance *Config
	once     sync.Once
)

// Get returns the singleton config instance.
func Get() *Config {
	once.Do(func() {
		instance = loadOrDefault()
	})
	return instance
}

// loadOrDefault loads config from file or returns defaults.
func loadOrDefault() *Config {
	cfg := &Config{
		Port:          8080,
		WebSocketPort: 8081,
		UDPPort:       8082,
		Host:          "0.0.0.0",
		ServerName:    "AirMouse Pro",
		UserName:      "User",
		Version:       "3.0.0",
		Language:      "en",

		EnableTCP:       true,
		EnableWebSocket: true,
		EnableUDP:       true,
		EnableBluetooth: false,
		EnableSerial:    false,

		Sensitivity:         1.0,
		SmoothingEnabled:    true,
		AccelerationEnabled: true,
		AccelerationFactor:  1.5,
		MaxAcceleration:     3.0,
		HapticEnabled:       true,

		EnableAISmoothing:        false,
		EnablePredictive:         true,
		PredictiveBlendFactor:    0.6,
		PredictiveDt:             0.02,
		EnableJitterCompensation: true,
		JitterBlendFactor:        0.3,
		JitterMaxLatency:         100 * time.Millisecond,
		JitterPredictionWindow:   20 * time.Millisecond,
		JitterUseKalman:          true,
		JitterUseAcceleration:    false,

		GestureConfidenceThreshold: 0.7,
		GestureCooldown:            500,
		ClickThreshold:             5.0,
		ScrollThreshold:            3.0,
		DoubleClickInterval:        300,
		RightClickTilt:             45.0,

		ProximityEnabled:       false,
		ProximityNearThreshold: 1.5,
		ProximityFarThreshold:  3.0,
		ProximityScanInterval:  1000,
		BluetoothAdapter:       "default",
		BLEEnabled:             true,
		HIDProxyEnabled:        false,

		EnablePersonalization:    false,
		PersonalizationBuffer:    2000,
		PersonalizationInterval:  3600,
		PersonalizationServerURL: "http://localhost:5001",
		AutoSwapModel:            false,

		EnableMLPrediction:  false,
		MLModelPath:         "models/lstm_predictor.onnx",
		MLSequenceLength:    16,
		MLBlendFactor:       0.6,
		MLInferenceInterval: 20,

		EnableHumanizer:            false,
		HumanizerTremorAmplitude:   3.5,
		HumanizerBSplineSegments:   15,
		HumanizerNoiseAmplitude:    2.0,
		HumanizerVelocityPeakRatio: 0.55,

		EnableParticleFilter:       false,
		ParticleFilterNumParticles: 500,

		AuthEnabled:     false,
		AuthSecret:      "",
		AuthToken:       "",
		AuthTokens:      []string{},
		EncryptionKey:   "",
		EnablePairingUI: true,
		TokenTTL:        24,

		TrustedDevices: []string{},

		MoveRateLimit:     60,
		HeartbeatInterval: 10,
		ConnectionTimeout: 30,
		BufferSize:        1024,
		InactiveTimeout:   300,

		MaxClients:     10,
		ClientNames:    true,
		DeviceRegistry: true,

		DiscoveryPort: 8083,
		MDNSName:      "airmouse",

		LogLevel:       "info",
		LogFile:        "",
		LogColor:       true,
		DebugMode:      false,
		MetricsEnabled: true,

		Theme:           "dark",
		AccentColor:     "#6366f1",
		AlwaysOnTop:     false,
		ShowTrayIcon:    true,
		WindowWidth:     1400,
		WindowHeight:    900,
		AutoStartServer: false,

		USBGadgetEnabled: false,
		USBVendorID:      "0x1d6b",
		USBProductID:     "0x0104",
		USBManufacturer:  "AirMouse",
		USBProduct:       "AirMouse HID",

		ModelPath:  "models/mouse_smoothing.onnx",
		ConfigPath: getConfigPath(),
		LogPath:    getLogPath(),

		FirstLaunch: true,

		mu: &sync.RWMutex{},
	}

	// Try to load from file
	if data, err := os.ReadFile(cfg.ConfigPath); err == nil {
		if err := json.Unmarshal(data, cfg); err != nil {
			fmt.Printf("Warning: Failed to parse config: %v\n", err)
		}
	}
	return cfg
}

// Save saves the config to file.
func (c *Config) Save() error {
	c.mu.Lock()
	defer c.mu.Unlock()
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}
	configDir := filepath.Dir(c.ConfigPath)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory: %w", err)
	}
	if err := os.WriteFile(c.ConfigPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write config file: %w", err)
	}
	return nil
}

// Reload reloads config from file.
func (c *Config) Reload() error {
	newConfig := loadOrDefault()
	c.mu.Lock()
	defer c.mu.Unlock()
	newConfig.mu = c.mu
	*c = *newConfig
	return nil
}

// ------------------------------------------------------------
// Trusted Devices (for auto‑approval)
// ------------------------------------------------------------

// AddTrustedDevice adds a fingerprint to the trusted list and saves.
func (c *Config) AddTrustedDevice(fingerprint string) {
	if fingerprint == "" {
		return
	}
	c.mu.Lock()
	exists := false
	for _, f := range c.TrustedDevices {
		if f == fingerprint {
			exists = true
			break
		}
	}
	if !exists {
		c.TrustedDevices = append(c.TrustedDevices, fingerprint)
	}
	c.mu.Unlock()

	if !exists {
		_ = c.Save()
	}
}

// RemoveTrustedDevice removes a fingerprint from the trusted list and saves.
func (c *Config) RemoveTrustedDevice(fingerprint string) {
	if fingerprint == "" {
		return
	}
	c.mu.Lock()
	foundIndex := -1
	for i, f := range c.TrustedDevices {
		if f == fingerprint {
			foundIndex = i
			break
		}
	}
	if foundIndex != -1 {
		c.TrustedDevices = append(c.TrustedDevices[:foundIndex], c.TrustedDevices[foundIndex+1:]...)
	}
	c.mu.Unlock()

	if foundIndex != -1 {
		_ = c.Save()
	}
}

// IsTrustedDevice checks if a fingerprint is in the trusted list.
func (c *Config) IsTrustedDevice(fingerprint string) bool {
	if fingerprint == "" {
		return false
	}
	c.mu.RLock()
	defer c.mu.RUnlock()
	for _, f := range c.TrustedDevices {
		if f == fingerprint {
			return true
		}
	}
	return false
}

// GetTrustedDevices returns a copy of the trusted devices list.
func (c *Config) GetTrustedDevices() []string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	result := make([]string, len(c.TrustedDevices))
	copy(result, c.TrustedDevices)
	return result
}

// ------------------------------------------------------------
// Proximity helpers
// ------------------------------------------------------------

// GetProximityConfig returns a struct with proximity settings.
func (c *Config) GetProximityConfig() struct {
	Enabled       bool
	NearThreshold float64
	FarThreshold  float64
	ScanInterval  int
} {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return struct {
		Enabled       bool
		NearThreshold float64
		FarThreshold  float64
		ScanInterval  int
	}{
		Enabled:       c.ProximityEnabled,
		NearThreshold: c.ProximityNearThreshold,
		FarThreshold:  c.ProximityFarThreshold,
		ScanInterval:  c.ProximityScanInterval,
	}
}

// SetProximityEnabled enables/disables proximity features.
func (c *Config) SetProximityEnabled(enabled bool) {
	c.mu.Lock()
	c.ProximityEnabled = enabled
	c.mu.Unlock()
	c.Save()
}

// SetProximityThresholds updates near and far thresholds.
func (c *Config) SetProximityThresholds(near, far float64) {
	c.mu.Lock()
	c.ProximityNearThreshold = near
	c.ProximityFarThreshold = far
	c.mu.Unlock()
	c.Save()
}

// ------------------------------------------------------------
// Setters
// ------------------------------------------------------------

func (c *Config) SetSensitivity(s float64) {
	c.mu.Lock()
	c.Sensitivity = s
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetTheme(theme string) {
	c.mu.Lock()
	c.Theme = theme
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetLogLevel(level string) {
	c.mu.Lock()
	c.LogLevel = level
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetPredictiveEnabled(enabled bool) {
	c.mu.Lock()
	c.EnablePredictive = enabled
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetPredictiveBlendFactor(factor float64) {
	c.mu.Lock()
	c.PredictiveBlendFactor = factor
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetAISmoothingEnabled(enabled bool) {
	c.mu.Lock()
	c.EnableAISmoothing = enabled
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetPersonalizationEnabled(enabled bool) {
	c.mu.Lock()
	c.EnablePersonalization = enabled
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetServerName(name string) {
	c.mu.Lock()
	c.ServerName = name
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetUserName(name string) {
	c.mu.Lock()
	c.UserName = name
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetPort(port int) {
	c.mu.Lock()
	c.Port = port
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetWebSocketPort(port int) {
	c.mu.Lock()
	c.WebSocketPort = port
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetUDPPort(port int) {
	c.mu.Lock()
	c.UDPPort = port
	c.mu.Unlock()
	c.Save()
}

func (c *Config) SetLanguage(lang string) {
	c.mu.Lock()
	c.Language = lang
	c.mu.Unlock()
	c.Save()
}

// ------------------------------------------------------------
// FirstLaunch helpers
// ------------------------------------------------------------

// IsFirstLaunch returns true if this is the first run of the application.
func (c *Config) IsFirstLaunch() bool {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.FirstLaunch
}

// SetFirstLaunchComplete marks the first launch as complete and saves the config.
func (c *Config) SetFirstLaunchComplete() error {
	c.mu.Lock()
	c.FirstLaunch = false
	c.mu.Unlock()
	return c.Save()
}

// ------------------------------------------------------------
// Serialisation helpers
// ------------------------------------------------------------

// ToJSON returns config as JSON string.
func (c *Config) ToJSON() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	data, _ := json.MarshalIndent(c, "", "  ")
	return string(data)
}

// FromJSON loads config from JSON string.
func (c *Config) FromJSON(jsonStr string) error {
	var newConfig Config
	if err := json.Unmarshal([]byte(jsonStr), &newConfig); err != nil {
		return err
	}
	c.mu.Lock()
	newConfig.mu = c.mu
	*c = newConfig
	c.mu.Unlock()
	return c.Save()
}

// ResetToDefaults resets config to default values.
func (c *Config) ResetToDefaults() {
	defaultConfig := loadOrDefault()
	c.mu.Lock()
	defaultConfig.mu = c.mu
	*c = *defaultConfig
	c.mu.Unlock()
	c.Save()
}

// Validate validates config values.
func (c *Config) Validate() error {
	if c.Port < 1 || c.Port > 65535 {
		return fmt.Errorf("invalid port: %d", c.Port)
	}
	if c.WebSocketPort < 1 || c.WebSocketPort > 65535 {
		return fmt.Errorf("invalid WebSocket port: %d", c.WebSocketPort)
	}
	if c.Sensitivity < 0.1 || c.Sensitivity > 5.0 {
		return fmt.Errorf("invalid sensitivity: %.2f", c.Sensitivity)
	}
	if c.GestureConfidenceThreshold < 0 || c.GestureConfidenceThreshold > 1 {
		return fmt.Errorf("invalid confidence threshold: %.2f", c.GestureConfidenceThreshold)
	}
	if c.ProximityNearThreshold < 0 || c.ProximityFarThreshold < 0 {
		return fmt.Errorf("invalid proximity thresholds")
	}
	return nil
}

// String returns a human-readable representation (for debugging).
func (c *Config) String() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return fmt.Sprintf("Config{ServerName:%s, Port:%d, WebSocketPort:%d, UDPPort:%d, Theme:%s}",
		c.ServerName, c.Port, c.WebSocketPort, c.UDPPort, c.Theme)
}

// ------------------------------------------------------------
// Path helpers
// ------------------------------------------------------------

// getConfigPath returns the config file path.
func getConfigPath() string {
	configDir, err := os.UserConfigDir()
	if err != nil {
		configDir = "."
	}
	return filepath.Join(configDir, "airmouse", "config.json")
}

// getLogPath returns the log directory path.
func getLogPath() string {
	configDir, err := os.UserConfigDir()
	if err != nil {
		configDir = "."
	}
	return filepath.Join(configDir, "airmouse", "logs")
}
