package config

import (
    "encoding/json"
    "os"
    "path/filepath"
    "sync"
    "time"
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
    EnableSerial    bool   `json:"enable_serial"`

    // Bluetooth
    BluetoothAdapter string `json:"bluetooth_adapter"`
    BLEEnabled       bool   `json:"ble_enabled"`
    HIDProxyEnabled  bool   `json:"hid_proxy_enabled"`

    // Security & Pairing
    AuthEnabled     bool     `json:"auth_enabled"`
    AuthSecret      string   `json:"auth_secret"`
    AuthTokens      []string `json:"auth_tokens"`
    EncryptionKey   string   `json:"encryption_key"`
    EnablePairingUI bool     `json:"enable_pairing_ui"`
    TokenTTL        int      `json:"token_ttl_hours"`

    // Performance
    Sensitivity         float64 `json:"sensitivity"`
    ClickThreshold      float64 `json:"click_threshold"`
    ScrollThreshold     float64 `json:"scroll_threshold"`
    DoubleClickInterval int64   `json:"double_click_interval"`
    RightClickTilt      float64 `json:"right_click_tilt"`
    HapticEnabled       bool    `json:"haptic_enabled"`
    MoveRateLimit       int     `json:"move_rate_limit"`
    BufferSize          int     `json:"buffer_size"`
    HeartbeatInterval   int     `json:"heartbeat_interval"`
    ConnectionTimeout   int     `json:"connection_timeout"`

    // Device
    MaxClients     int  `json:"max_clients"`
    ClientNames    bool `json:"client_names"`
    DeviceRegistry bool `json:"device_registry"`
    InactiveTimeout int  `json:"inactive_timeout_seconds"`

    // Discovery
    DiscoveryPort int    `json:"discovery_port"`
    MDNSName      string `json:"mdns_name"`

    // UI
    AccentColor  string `json:"accent_color"`
    Theme        string `json:"theme"`
    AlwaysOnTop  bool   `json:"always_on_top"`
    ShowTrayIcon bool   `json:"show_tray_icon"`
    WindowWidth  int    `json:"window_width"`
    WindowHeight int    `json:"window_height"`

    // Logging
    LogLevel string `json:"log_level"`
    LogFile  string `json:"log_file"`
    LogColor bool   `json:"log_color"`

    // AI / Prediction
    EnableAISmoothing         bool    `json:"enable_ai_smoothing"`
    AIModelPath               string  `json:"ai_model_path"`
    AIBlendFactor             float64 `json:"ai_blend_factor"`
    EnablePersonalization     bool    `json:"enable_personalization"`
    PersonalizationBuffer     int     `json:"personalization_buffer"`
    PersonalizationInterval   int     `json:"personalization_interval"`
    PersonalizationServerURL  string  `json:"personalization_server_url"`
    AutoSwapModel             bool    `json:"auto_swap_model"`
    EnablePredictive          bool    `json:"enable_predictive"`
    PredictiveBlendFactor     float64 `json:"predictive_blend_factor"`
    PredictiveDt              float64 `json:"predictive_dt"`

    // ML Prediction
    EnableMLPrediction    bool    `json:"enable_ml_prediction"`
    MLModelPath           string  `json:"ml_model_path"`
    MLSequenceLength      int     `json:"ml_sequence_length"`
    MLBlendFactor         float64 `json:"ml_blend_factor"`
    MLInferenceInterval   int     `json:"ml_inference_interval_ms"`

    // Humanizer
    EnableHumanizer              bool    `json:"enable_humanizer"`
    HumanizerTremorAmplitude    float64 `json:"humanizer_tremor_amplitude"`
    HumanizerBSplineSegments    int     `json:"humanizer_bspline_segments"`
    HumanizerNoiseAmplitude     float64 `json:"humanizer_noise_amplitude"`
    HumanizerVelocityPeakRatio  float64 `json:"humanizer_velocity_peak_ratio"`

    // Particle filter / Gesture
    EnableParticleFilter         bool    `json:"enable_particle_filter"`
    ParticleFilterNumParticles   int     `json:"particle_filter_num_particles"`
    GestureConfidenceThreshold   float64 `json:"gesture_confidence_threshold"`
    GestureCooldown              int     `json:"gesture_cooldown_ms"`

    // Jitter compensation
    EnableJitterCompensation   bool          `json:"enable_jitter_compensation"`
    JitterMaxLatency           time.Duration `json:"jitter_max_latency"`
    JitterPredictionWindow     time.Duration `json:"jitter_prediction_window"`
    JitterBlendFactor          float64       `json:"jitter_blend_factor"`
    JitterUseKalman            bool          `json:"jitter_use_kalman"`
    JitterUseAcceleration      bool          `json:"jitter_use_acceleration"`

    // Proximity
    ProximityEnabled        bool    `json:"proximity_enabled"`
    ProximityNearThreshold  float64 `json:"proximity_near_threshold"`
    ProximityFarThreshold   float64 `json:"proximity_far_threshold"`
    ProximityScanInterval   int     `json:"proximity_scan_interval_ms"`

    // USB Gadget
    USBGadgetEnabled bool   `json:"usb_gadget_enabled"`
    USBVendorID      string `json:"usb_vendor_id"`
    USBProductID     string `json:"usb_product_id"`
    USBManufacturer  string `json:"usb_manufacturer"`
    USBProduct       string `json:"usb_product"`

    // Server Identity
    ServerName string `json:"server_name"`
    UserName   string `json:"user_name"`
    Version    string `json:"version"`
    Language   string `json:"language"`

    mu sync.RWMutex `json:"-"`
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
        // Server defaults
        Host:            "0.0.0.0",
        Port:            8080,
        WebSocketPort:   8081,
        UDPPort:         8082,
        EnableTCP:       true,
        EnableWebSocket: true,
        EnableUDP:       true,
        EnableBluetooth: true,
        EnableSerial:    true,

        // Bluetooth
        BluetoothAdapter: "default",
        BLEEnabled:       true,
        HIDProxyEnabled:  false,

        // Security
        AuthEnabled:     false,
        AuthSecret:      "",
        AuthTokens:      []string{},
        EncryptionKey:   "",
        EnablePairingUI: true,
        TokenTTL:        24,

        // Performance
        Sensitivity:         1.0,
        ClickThreshold:      5.0,
        ScrollThreshold:     3.0,
        DoubleClickInterval: 300,
        RightClickTilt:      45.0,
        HapticEnabled:       true,
        MoveRateLimit:       60,
        BufferSize:          1024,
        HeartbeatInterval:   10,
        ConnectionTimeout:   30,

        // Device
        MaxClients:      10,
        ClientNames:     true,
        DeviceRegistry:  true,
        InactiveTimeout: 300,

        // Discovery
        DiscoveryPort: 8083,
        MDNSName:      "airmouse",

        // UI
        AccentColor:  "#007acc",
        Theme:        "dark",
        AlwaysOnTop:  false,
        ShowTrayIcon: true,
        WindowWidth:  1200,
        WindowHeight: 800,

        // Logging
        LogLevel: "info",
        LogFile:  "",
        LogColor: true,

        // AI
        EnableAISmoothing:        false,
        AIModelPath:              "models/mouse_smoothing.onnx",
        AIBlendFactor:            0.6,
        EnablePersonalization:    false,
        PersonalizationBuffer:    2000,
        PersonalizationInterval:  3600,
        PersonalizationServerURL: "http://localhost:5001",
        AutoSwapModel:            false,
        EnablePredictive:         true,
        PredictiveBlendFactor:    0.6,
        PredictiveDt:             0.02,

        // ML
        EnableMLPrediction:  false,
        MLModelPath:         "models/lstm_predictor.onnx",
        MLSequenceLength:    16,
        MLBlendFactor:       0.6,
        MLInferenceInterval: 20,

        // Humanizer
        EnableHumanizer:             false,
        HumanizerTremorAmplitude:    3.5,
        HumanizerBSplineSegments:    15,
        HumanizerNoiseAmplitude:     2.0,
        HumanizerVelocityPeakRatio:  0.55,

        // Particle filter
        EnableParticleFilter:       false,
        ParticleFilterNumParticles: 500,
        GestureConfidenceThreshold: 0.7,
        GestureCooldown:            500,

        // Jitter
        EnableJitterCompensation: true,
        JitterMaxLatency:         100 * time.Millisecond,
        JitterPredictionWindow:   20 * time.Millisecond,
        JitterBlendFactor:        0.7,
        JitterUseKalman:          true,
        JitterUseAcceleration:    false,

        // Proximity
        ProximityEnabled:       false,
        ProximityNearThreshold: 1.0,
        ProximityFarThreshold:  3.0,
        ProximityScanInterval:  1000,

        // USB Gadget
        USBGadgetEnabled: false,
        USBVendorID:      "0x1d6b",
        USBProductID:     "0x0104",
        USBManufacturer:  "AirMouse",
        USBProduct:       "AirMouse HID",

        // Identity
        ServerName: "AirMouse Pro",
        UserName:   "User",
        Version:    "3.0.0",
        Language:   "en",
    }

    if data, err := os.ReadFile(getConfigPath()); err == nil {
        if err := json.Unmarshal(data, cfg); err != nil {
            // Log error but continue with defaults
        }
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

    path := getConfigPath()
    if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
        return err
    }

    return os.WriteFile(path, data, 0644)
}

func (c *Config) Reload() error {
    newCfg := loadOrDefault()
    c.mu.Lock()
    defer c.mu.Unlock()
    *c = *newCfg
    return nil
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

func (c *Config) SetLogLevel(level string) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.LogLevel = level
    c.Save()
}

func (c *Config) SetPredictiveEnabled(enabled bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.EnablePredictive = enabled
    c.Save()
}

func (c *Config) SetPredictiveBlendFactor(factor float64) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.PredictiveBlendFactor = factor
    c.Save()
}

func (c *Config) SetAISmoothingEnabled(enabled bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.EnableAISmoothing = enabled
    c.Save()
}

func (c *Config) SetPersonalizationEnabled(enabled bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.EnablePersonalization = enabled
    c.Save()
}

func (c *Config) ToJSON() string {
    c.mu.RLock()
    defer c.mu.RUnlock()
    data, _ := json.MarshalIndent(c, "", "  ")
    return string(data)
}

func (c *Config) FromJSON(jsonStr string) error {
    var newCfg Config
    if err := json.Unmarshal([]byte(jsonStr), &newCfg); err != nil {
        return err
    }
    c.mu.Lock()
    *c = newCfg
    c.mu.Unlock()
    return c.Save()
}

func (c *Config) ResetToDefaults() {
    defaultCfg := loadOrDefault()
    c.mu.Lock()
    *c = *defaultCfg
    c.mu.Unlock()
    c.Save()
}

func getConfigPath() string {
    configDir, err := os.UserConfigDir()
    if err != nil {
        configDir = "."
    }
    return filepath.Join(configDir, "airmouse", "config.json")
}package config

import (
    "encoding/json"
    "os"
    "path/filepath"
    "sync"
    "time"
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
    EnableSerial    bool   `json:"enable_serial"`

    // Security
    AuthEnabled bool     `json:"auth_enabled"`
    AuthSecret  string   `json:"auth_secret"`
    AuthTokens  []string `json:"auth_tokens"`

    // Mouse
    Sensitivity        float64 `json:"sensitivity"`
    SmoothingEnabled   bool    `json:"smoothing_enabled"`
    AccelerationEnabled bool   `json:"acceleration_enabled"`
    AccelerationFactor float64 `json:"acceleration_factor"`

    // Performance
    MoveRateLimit     int `json:"move_rate_limit"`
    HeartbeatInterval int `json:"heartbeat_interval"`
    ConnectionTimeout int `json:"connection_timeout"`

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
    LogColor bool   `json:"log_color"`

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

    // Gesture
    GestureConfidenceThreshold float64 `json:"gesture_confidence_threshold"`
    GestureCooldown            int     `json:"gesture_cooldown"`

    // Proximity
    ProximityEnabled       bool    `json:"proximity_enabled"`
    ProximityNearThreshold float64 `json:"proximity_near_threshold"`
    ProximityFarThreshold  float64 `json:"proximity_far_threshold"`

    // USB Gadget
    USBGadgetEnabled bool   `json:"usb_gadget_enabled"`
    USBVendorID      string `json:"usb_vendor_id"`
    USBProductID     string `json:"usb_product_id"`

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
        Host:                       "0.0.0.0",
        Port:                       8080,
        WebSocketPort:              8081,
        UDPPort:                    8082,
        EnableTCP:                  true,
        EnableWebSocket:            true,
        EnableUDP:                  true,
        EnableBluetooth:            true,
        EnableSerial:               false,
        AuthEnabled:                false,
        AuthSecret:                 "",
        AuthTokens:                 []string{},
        Sensitivity:                1.0,
        SmoothingEnabled:           true,
        AccelerationEnabled:        true,
        AccelerationFactor:         1.5,
        MoveRateLimit:              60,
        HeartbeatInterval:          10,
        ConnectionTimeout:          30,
        MaxClients:                 10,
        ClientNames:                true,
        DiscoveryPort:              8083,
        MDNSName:                   "airmouse",
        Theme:                      "dark",
        AlwaysOnTop:                false,
        ShowTrayIcon:               true,
        LogLevel:                   "info",
        LogFile:                    "",
        LogColor:                   true,
        EnableAISmoothing:          false,
        AIModelPath:                "models/mouse_smoothing.onnx",
        AIBlendFactor:              0.6,
        EnablePersonalization:      false,
        PersonalizationBuffer:      2000,
        PersonalizationInterval:    3600,
        AutoSwapModel:              false,
        EnablePredictive:           true,
        PredictiveBlendFactor:      0.6,
        GestureConfidenceThreshold: 0.7,
        GestureCooldown:            500,
        ProximityEnabled:           false,
        ProximityNearThreshold:     1.0,
        ProximityFarThreshold:      3.0,
        USBGadgetEnabled:           false,
        USBVendorID:                "0x1d6b",
        USBProductID:               "0x0104",
    }

    path := getConfigPath()
    if data, err := os.ReadFile(path); err == nil {
        if err := json.Unmarshal(data, cfg); err != nil {
            // Log error but continue with defaults
        }
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
    configDir := filepath.Dir(getConfigPath())
    os.MkdirAll(configDir, 0755)
    return os.WriteFile(getConfigPath(), data, 0644)
}

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

func (c *Config) Reload() error {
    newCfg := loadOrDefault()
    c.mu.Lock()
    *c = *newCfg
    c.mu.Unlock()
    return nil
}

func getConfigPath() string {
    configDir, err := os.UserConfigDir()
    if err != nil {
        configDir = "."
    }
    return filepath.Join(configDir, "airmouse", "config.json")
}


package config

import (
    "encoding/json"
    "fmt"
    "os"
    "path/filepath"
    "sync"
    "time"
)

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
    
    // Mouse settings
    Sensitivity          float64 `json:"sensitivity"`
    SmoothingEnabled     bool    `json:"smoothing_enabled"`
    AccelerationEnabled  bool    `json:"acceleration_enabled"`
    AccelerationFactor   float64 `json:"acceleration_factor"`
    MaxAcceleration      float64 `json:"max_acceleration"`
    
    // AI settings
    EnableAISmoothing     bool    `json:"enable_ai_smoothing"`
    EnablePredictive      bool    `json:"enable_predictive"`
    PredictiveBlendFactor float64 `json:"predictive_blend_factor"`
    EnableJitterCompensation bool `json:"enable_jitter_compensation"`
    JitterBlendFactor     float64 `json:"jitter_blend_factor"`
    
    // Gesture settings
    GestureConfidenceThreshold float64 `json:"gesture_confidence_threshold"`
    ClickThreshold             float64 `json:"click_threshold"`
    ScrollThreshold            float64 `json:"scroll_threshold"`
    DoubleClickInterval        int64   `json:"double_click_interval"`
    RightClickTilt             float64 `json:"right_click_tilt"`
    
    // Proximity settings
    ProximityEnabled       bool    `json:"proximity_enabled"`
    ProximityNearThreshold float64 `json:"proximity_near_threshold"`
    ProximityFarThreshold  float64 `json:"proximity_far_threshold"`
    BluetoothAdapter       string  `json:"bluetooth_adapter"`
    
    // Protocol settings
    EnableTCP          bool `json:"enable_tcp"`
    EnableWebSocket    bool `json:"enable_websocket"`
    EnableUDP          bool `json:"enable_udp"`
    EnableBluetooth    bool `json:"enable_bluetooth"`
    EnableSerial       bool `json:"enable_serial"`
    
    // Personalization
    EnablePersonalization   bool   `json:"enable_personalization"`
    PersonalizationBuffer   int    `json:"personalization_buffer"`
    PersonalizationInterval int    `json:"personalization_interval"`
    PersonalizationServerURL string `json:"personalization_server_url"`
    AutoSwapModel           bool   `json:"auto_swap_model"`
    
    // ML settings
    EnableMLPrediction   bool    `json:"enable_ml_prediction"`
    MLModelPath          string  `json:"ml_model_path"`
    MLSequenceLength     int     `json:"ml_sequence_length"`
    MLBlendFactor        float64 `json:"ml_blend_factor"`
    MLInferenceInterval  int     `json:"ml_inference_interval_ms"`
    
    // Authentication
    AuthEnabled  bool   `json:"auth_enabled"`
    AuthSecret   string `json:"auth_secret"`
    AuthToken    string `json:"auth_token"`
    
    // Logging
    LogLevel    string `json:"log_level"`
    DebugMode   bool   `json:"debug_mode"`
    MetricsEnabled bool `json:"metrics_enabled"`
    
    // UI settings
    Theme         string `json:"theme"`
    AutoStartServer bool `json:"auto_start_server"`
    HapticEnabled bool   `json:"haptic_enabled"`
    
    // Paths
    ModelPath     string `json:"model_path"`
    ConfigPath    string `json:"config_path"`
    LogPath       string `json:"log_path"`
    
    mu sync.RWMutex
}

var (
    instance *Config
    once     sync.Once
)

func Get() *Config {
    once.Do(func() {
        instance = loadOrDefault()
    })
    return instance
}

func loadOrDefault() *Config {
    cfg := &Config{
        // Server defaults
        Port:          8080,
        WebSocketPort: 8081,
        UDPPort:       8082,
        Host:          "0.0.0.0",
        ServerName:    "AirMouse Pro",
        UserName:      "User",
        Version:       "3.0.0",
        Language:      "en",
        
        // Mouse defaults
        Sensitivity:          1.0,
        SmoothingEnabled:     true,
        AccelerationEnabled:  true,
        AccelerationFactor:   1.5,
        MaxAcceleration:      3.0,
        
        // AI defaults
        EnableAISmoothing:     false,
        EnablePredictive:      true,
        PredictiveBlendFactor: 0.6,
        EnableJitterCompensation: true,
        JitterBlendFactor:     0.3,
        
        // Gesture defaults
        GestureConfidenceThreshold: 0.7,
        ClickThreshold:             5.0,
        ScrollThreshold:            3.0,
        DoubleClickInterval:        300,
        RightClickTilt:             45.0,
        
        // Proximity defaults
        ProximityEnabled:       false,
        ProximityNearThreshold: 1.0,
        ProximityFarThreshold:  3.0,
        BluetoothAdapter:       "default",
        
        // Protocol defaults
        EnableTCP:        true,
        EnableWebSocket:  true,
        EnableUDP:        true,
        EnableBluetooth:  false,
        EnableSerial:     false,
        
        // Personalization defaults
        EnablePersonalization:    false,
        PersonalizationBuffer:    2000,
        PersonalizationInterval:  3600,
        PersonalizationServerURL: "http://localhost:5001",
        AutoSwapModel:            false,
        
        // ML defaults
        EnableMLPrediction:  false,
        MLModelPath:         "models/lstm_predictor.onnx",
        MLSequenceLength:    16,
        MLBlendFactor:       0.6,
        MLInferenceInterval: 20,
        
        // Auth defaults
        AuthEnabled: false,
        AuthSecret:  "",
        AuthToken:   "",
        
        // Logging defaults
        LogLevel:       "info",
        DebugMode:      false,
        MetricsEnabled: true,
        
        // UI defaults
        Theme:           "dark",
        AutoStartServer: false,
        HapticEnabled:   true,
        
        // Path defaults
        ModelPath:  "models/mouse_smoothing.onnx",
        ConfigPath: getConfigPath(),
        LogPath:    getLogPath(),
    }
    
    if data, err := os.ReadFile(cfg.ConfigPath); err == nil {
        if err := json.Unmarshal(data, cfg); err != nil {
            fmt.Printf("Warning: Failed to parse config: %v\n", err)
        }
    }
    
    return cfg
}

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

func (c *Config) Reload() error {
    newConfig := loadOrDefault()
    c.mu.Lock()
    defer c.mu.Unlock()
    *c = *newConfig
    return nil
}

func (c *Config) SetSensitivity(s float64) {
    c.mu.Lock()
    c.Sensitivity = s
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

func (c *Config) ToJSON() string {
    c.mu.RLock()
    defer c.mu.RUnlock()
    data, _ := json.MarshalIndent(c, "", "  ")
    return string(data)
}

func (c *Config) FromJSON(jsonStr string) error {
    var newConfig Config
    if err := json.Unmarshal([]byte(jsonStr), &newConfig); err != nil {
        return err
    }
    c.mu.Lock()
    *c = newConfig
    c.mu.Unlock()
    return c.Save()
}

func (c *Config) ResetToDefaults() {
    defaultConfig := loadOrDefault()
    c.mu.Lock()
    *c = *defaultConfig
    c.mu.Unlock()
    c.Save()
}

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
    return nil
}

func getConfigPath() string {
    configDir, err := os.UserConfigDir()
    if err != nil {
        configDir = "."
    }
    return filepath.Join(configDir, "airmouse", "config.json")
}

func getLogPath() string {
    configDir, err := os.UserConfigDir()
    if err != nil {
        configDir = "."
    }
    return filepath.Join(configDir, "airmouse", "logs")
}

func (c *Config) GetUpTime() time.Duration {
    return time.Since(c.GetStartTime())
}

func (c *Config) GetStartTime() time.Time {
    // Would be set when server starts
    return time.Now()
}