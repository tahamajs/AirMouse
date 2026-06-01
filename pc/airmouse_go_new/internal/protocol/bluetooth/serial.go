package bluetooth

import "airmouse-go/internal/utils"

type SerialConnection struct {
	Port string
}

func NewSerialConnection(port string) *SerialConnection {
	return &SerialConnection{Port: port}
}

func (s *SerialConnection) Connect() error {
	utils.LogInfo("Serial connection opened", "port", s.Port)
	return nil
}

func (s *SerialConnection) Disconnect() {
	utils.LogInfo("Serial connection closed", "port", s.Port)
}