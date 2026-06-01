// internal/config/config.go – corrected section
type Config struct {
    // Server
    Host           string `json:"host"`
    Port           int    `json:"port"`
    WebSocketPort  int    `json:"websocket_port"`
    UDPPort        int    `json:"udp_port"`
    EnableTCP      bool   `json:"enable_tcp"`
    EnableWebSocket bool  `json:"enable_websocket"`
    EnableUDP      bool   `json:"enable_udp"`
    EnableBluetooth bool  `json:"enable_bluetooth"`
    EnableSerial   bool   `json:"enable_serial"`

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

    // AI / Prediction
    EnableAISmoothing         bool    `json:"enable_ai_smoothing"`
    AIModelPath               string  `json:"ai_model_path"`
    AIBlendFactor             float64 `json:"ai_blend_factor"`
    EnablePersonalization     bool    `json:"enable_personalization"`
    PersonalizationBuffer     int     `json:"personalization_buffer"`
    PersonalizationInterval   int     `json:"personalization_interval"`
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

    // Jitter compensation
    EnableJitterCompensation   bool          `json:"enable_jitter_compensation"`
    JitterMaxLatency           time.Duration `json:"jitter_max_latency"`
    JitterPredictionWindow     time.Duration `json:"jitter_prediction_window"`
    JitterBlendFactor          float64       `json:"jitter_blend_factor"`
    JitterUseKalman            bool          `json:"jitter_use_kalman"`
    JitterUseAcceleration      bool          `json:"jitter_use_acceleration"`

    mu sync.RWMutex `json:"-"`
}