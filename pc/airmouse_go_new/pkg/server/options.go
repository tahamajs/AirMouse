package server

import (
	"airmouse-go/control/mouse"
)

// Option configures an AirMouse server.
type Option func(*options)

type options struct {
	cfg        Config
	customCtrl mouse.Controller
}

// WithConfig supplies a complete Config struct.
func WithConfig(cfg Config) Option {
	return func(o *options) {
		o.cfg = cfg
	}
}

// WithHost sets the listen host (e.g. "127.0.0.1" or "0.0.0.0").
func WithHost(host string) Option {
	return func(o *options) {
		o.cfg.Host = host
	}
}

// WithTCPPort sets the TCP protocol listening port.
func WithTCPPort(port int) Option {
	return func(o *options) {
		o.cfg.Port = port
	}
}

// WithWebSocketPort sets the WebSocket listening port.
func WithWebSocketPort(port int) Option {
	return func(o *options) {
		o.cfg.WebSocketPort = port
	}
}

// WithUDPPort sets the UDP discovery broadcast port.
func WithUDPPort(port int) Option {
	return func(o *options) {
		o.cfg.UDPPort = port
	}
}

// WithSensitivity configures the cursor sensitivity multiplier.
func WithSensitivity(s float64) Option {
	return func(o *options) {
		o.cfg.Sensitivity = s
	}
}

// WithLogLevel sets the logging verbosity ("debug", "info", "warn", "error").
func WithLogLevel(level string) Option {
	return func(o *options) {
		o.cfg.LogLevel = level
	}
}

// WithAuth configures token authentication.
func WithAuth(secret string, tokens ...string) Option {
	return func(o *options) {
		o.cfg.AuthEnabled = true
		o.cfg.AuthSecret = secret
		o.cfg.AuthTokens = tokens
	}
}

// WithVirtualMouse enables virtual mouse mode, intercepting events in callbacks without moving the OS cursor.
func WithVirtualMouse() Option {
	return func(o *options) {
		o.cfg.VirtualMouse = true
	}
}

// WithCustomMouse supplies a custom mouse controller implementation.
func WithCustomMouse(ctrl mouse.Controller) Option {
	return func(o *options) {
		o.customCtrl = ctrl
	}
}

// WithProtocols selectively toggles transport protocols.
func WithProtocols(enableTCP, enableWS, enableUDP bool) Option {
	return func(o *options) {
		o.cfg.EnableTCP = enableTCP
		o.cfg.EnableWebSocket = enableWS
		o.cfg.EnableUDP = enableUDP
	}
}
