package bluetooth

import "airmouse-go/internal/utils"

// SerialConnection manages a virtual serial port over BLE.
type SerialConnection struct {
	Port string
}

// NewSerialConnection creates a new serial connection.
func NewSerialConnection(port string) *SerialConnection {
	return &SerialConnection{Port: port}
}

// Connect establishes the connection.
func (s *SerialConnection) Connect() error {
	utils.LogInfo("Serial connection opened", "port", s.Port)
	return nil
}

// Disconnect closes the connection.
func (s *SerialConnection) Disconnect() {
	utils.LogInfo("Serial connection closed", "port", s.Port)
}