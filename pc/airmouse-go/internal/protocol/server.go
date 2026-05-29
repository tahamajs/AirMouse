package protocol

import (
	"sync"
	"time"
	
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol/tcp"
	"airmouse-go/internal/protocol/websocket"
	"airmouse-go/internal/protocol/udp"
	"airmouse-go/internal/protocol/bluetooth"
)

type ProtocolServer struct {
	tcpServer      *tcp.Server
	wsServer       *websocket.Server
	udpServer      *udp.Server
	bluetoothMgr   *bluetooth.Manager
	
	mouseCtrl      control.MouseController
	deviceMgr      *device.Manager
	
	mu             sync.RWMutex
	running        bool
}

func NewProtocolServer(mouse control.MouseController, deviceMgr *device.Manager) *ProtocolServer {
	return &ProtocolServer{
		mouseCtrl:      mouse,
		deviceMgr:      deviceMgr,
	}
}

func (s *ProtocolServer) Start() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	
	cfg := config.Get()
	
	// Start TCP server
	if cfg.EnableTCP {
		s.tcpServer = tcp.NewServer(cfg.Host, cfg.Port, s.mouseCtrl, s.deviceMgr)
		if err := s.tcpServer.Start(); err != nil {
			logger.Error("TCP server start failed", "error", err)
		}
	}
	
	// Start WebSocket server
	if cfg.EnableWebSocket {
		s.wsServer = websocket.NewServer(cfg.WebSocketPort, s.mouseCtrl, s.deviceMgr)
		if err := s.wsServer.Start(); err != nil {
			logger.Error("WebSocket server start failed", "error", err)
		}
	}
	
	// Start UDP discovery
	if cfg.EnableUDP {
		s.udpServer = udp.NewServer(cfg.UDPPort, s.deviceMgr)
		if err := s.udpServer.Start(); err != nil {
			logger.Error("UDP server start failed", "error", err)
		}
	}
	
	// Start Bluetooth
	if cfg.EnableBluetooth {
		s.bluetoothMgr = bluetooth.NewManager(cfg.BluetoothAdapter, s.mouseCtrl, s.deviceMgr)
		if err := s.bluetoothMgr.Start(); err != nil {
			logger.Error("Bluetooth manager start failed", "error", err)
		}
	}
	
	s.running = true
	return nil
}

func (s *ProtocolServer) Stop() {
	s.mu.Lock()
	defer s.mu.Unlock()
	
	if s.tcpServer != nil {
		s.tcpServer.Stop()
	}
	if s.wsServer != nil {
		s.wsServer.Stop()
	}
	if s.udpServer != nil {
		s.udpServer.Stop()
	}
	if s.bluetoothMgr != nil {
		s.bluetoothMgr.Stop()
	}
	
	s.running = false
}

func (s *ProtocolServer) GetConnectedDevices() []*device.DeviceInfo {
	return s.deviceMgr.GetAllDevices()
}

func (s *ProtocolServer) GetStatistics() map[string]interface{} {
	stats := make(map[string]interface{})
	
	if s.tcpServer != nil {
		stats["tcp"] = s.tcpServer.GetStats()
	}
	if s.wsServer != nil {
		stats["websocket"] = s.wsServer.GetStats()
	}
	if s.udpServer != nil {
		stats["udp"] = s.udpServer.GetStats()
	}
	if s.bluetoothMgr != nil {
		stats["bluetooth"] = s.bluetoothMgr.GetStats()
	}
	
	stats["devices"] = len(s.deviceMgr.GetAllDevices())
	
	return stats
}