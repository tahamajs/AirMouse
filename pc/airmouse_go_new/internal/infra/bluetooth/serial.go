package bluetooth

import "airmouse-go/internal/infra/logger"

// SerialConnection represents a Bluetooth serial (SPP) connection.
type SerialConnection struct {
	Port     string
	connected bool
}

// NewSerialConnection creates a new serial connection.
func NewSerialConnection(port string) *SerialConnection {
	return &SerialConnection{Port: port}
}

// Connect establishes the serial connection.
func (s *SerialConnection) Connect() error {
	s.connected = true
	logger.Info("Serial connection opened: port=%s", s.Port)
	return nil
}

// Disconnect closes the serial connection.
func (s *SerialConnection) Disconnect() {
	if s.connected {
		s.connected = false
		logger.Info("Serial connection closed: port=%s", s.Port)
	}
}

// IsConnected returns true if the connection is active.
func (s *SerialConnection) IsConnected() bool {
	return s.connected
}