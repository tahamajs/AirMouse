package server

// Config specifies the runtime settings for embedding the AirMouse server.
type Config struct {
	// Host to bind server interfaces (default "0.0.0.0").
	Host string `json:"host"`

	// Port for raw TCP protocol connection (default 8080).
	Port int `json:"port"`

	// WebSocketPort for WebSocket protocol and web dashboard (default 8081).
	WebSocketPort int `json:"websocket_port"`

	// UDPPort for UDP discovery broadcast / multicast (default 8082).
	UDPPort int `json:"udp_port"`

	// EnableTCP enables the TCP listener.
	EnableTCP bool `json:"enable_tcp"`

	// EnableWebSocket enables the WebSocket listener.
	EnableWebSocket bool `json:"enable_websocket"`

	// EnableUDP enables UDP broadcast discovery.
	EnableUDP bool `json:"enable_udp"`

	// EnableBluetooth enables Bluetooth SPP / BLE connectivity if supported.
	EnableBluetooth bool `json:"enable_bluetooth"`

	// EnableSerial enables USB serial communication.
	EnableSerial bool `json:"enable_serial"`

	// AuthEnabled enforces token-based authentication on incoming clients.
	AuthEnabled bool `json:"auth_enabled"`

	// AuthSecret is the master secret for JWT / token validation.
	AuthSecret string `json:"auth_secret"`

	// AuthTokens is an optional whitelist of pre-shared access tokens.
	AuthTokens []string `json:"auth_tokens"`

	// Sensitivity scales cursor movement (0.1 to 3.0, default 1.0).
	Sensitivity float64 `json:"sensitivity"`

	// LogLevel sets the log filter (debug, info, warn, error).
	LogLevel string `json:"log_level"`

	// ProximityEnabled enables RSSI proximity tracking and auto-lock.
	ProximityEnabled bool `json:"proximity_enabled"`

	// VirtualMouse disables OS cursor movements (useful for headless, test, or custom consumer apps).
	VirtualMouse bool `json:"virtual_mouse"`
}

// DefaultConfig returns recommended production defaults.
func DefaultConfig() Config {
	return Config{
		Host:             "0.0.0.0",
		Port:             8080,
		WebSocketPort:    8081,
		UDPPort:          8082,
		EnableTCP:        true,
		EnableWebSocket:  true,
		EnableUDP:        true,
		EnableBluetooth:  false,
		EnableSerial:     false,
		AuthEnabled:      false,
		AuthSecret:       "airmouse-default-secret",
		AuthTokens:       []string{},
		Sensitivity:      1.0,
		LogLevel:         "info",
		ProximityEnabled: false,
		VirtualMouse:     false,
	}
}
