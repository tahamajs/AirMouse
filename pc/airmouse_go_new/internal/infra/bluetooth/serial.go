package bluetooth

import "airmouse-go/internal/infra/logger"

type SerialConnection struct {
	Port string
}

func NewSerialConnection(port string) *SerialConnection {
	return &SerialConnection{Port: port}
}

func (s *SerialConnection) Connect() error {
	logger.Info("Serial connection opened: port=%v", s.Port)
	return nil
}

func (s *SerialConnection) Disconnect() {
	logger.Info("Serial connection closed: port=%v", s.Port)
}
