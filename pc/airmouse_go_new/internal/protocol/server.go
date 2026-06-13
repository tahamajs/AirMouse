package protocol

import (
	"sync"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol/bluetooth"
	"airmouse-go/internal/protocol/tcp"
	"airmouse-go/internal/protocol/udp"
	"airmouse-go/internal/protocol/usb"
	"airmouse-go/internal/protocol/websocket"
	"airmouse-go/internal/utils"
	    "time"
    "github.com/gorilla/websocket"

)
type Client struct {
    ID       string
    Name     string
    Conn     *websocket.Conn
    LastSeen time.Time
    IsActive bool
}

type startStopStats interface {
	Start() error
	Stop()
	GetStats() map[string]interface{}
}

type websocketStartStopStats interface {
	Start() error
	Stop() error
	GetStats() map[string]interface{}
}

type bluetoothStartStopStats interface {
	Start() error
	Stop()
	GetStats() map[string]interface{}
}

var (
	newTCPServer       = func(host string, port int, mouse control.MouseController, deviceMgr *device.Manager) startStopStats { return tcp.NewServer(host, port, mouse, deviceMgr) }
	newWebSocketServer = func(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) websocketStartStopStats { return websocket.NewServer(port, mouse, deviceMgr, authMgr) }
	newUDPServer       = func(port int, deviceMgr *device.Manager) startStopStats { return udp.NewServer(port, deviceMgr) }
	newBluetoothMgr    = func(adapter string, mouse control.MouseController, deviceMgr *device.Manager) bluetoothStartStopStats { return bluetooth.NewManager(adapter, mouse, deviceMgr) }
	newUSBServer       = func(mouse control.MouseController, deviceMgr *device.Manager) startStopStats { return usb.NewServer(mouse, deviceMgr) }
)

type ProtocolServer struct {
	tcpServer    startStopStats
	wsServer     websocketStartStopStats
	udpServer    startStopStats
	usbServer    startStopStats
	bluetoothMgr bluetoothStartStopStats

	mouseCtrl control.MouseController
	deviceMgr *device.Manager
	authMgr   *auth.Manager

	mu      sync.RWMutex
	running bool
}

func NewProtocolServer(mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *ProtocolServer {
	return &ProtocolServer{
		mouseCtrl: mouse,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
	}
}

func (s *ProtocolServer) Start() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	cfg := config.Get()

	if cfg.EnableTCP {
		s.tcpServer = newTCPServer(cfg.Host, cfg.Port, s.mouseCtrl, s.deviceMgr)
		if err := s.tcpServer.Start(); err != nil {
			utils.LogError("TCP server start failed", "error", err)
		} else {
			utils.LogInfo("TCP server started", "port", cfg.Port)
		}
	}
	if cfg.EnableWebSocket {
		s.wsServer = newWebSocketServer(cfg.WebSocketPort, s.mouseCtrl, s.deviceMgr, s.authMgr)
		if err := s.wsServer.Start(); err != nil {
			utils.LogError("WebSocket server start failed", "error", err)
		} else {
			utils.LogInfo("WebSocket server started", "port", cfg.WebSocketPort)
		}
	}
	if cfg.EnableUDP {
		s.udpServer = newUDPServer(cfg.UDPPort, s.deviceMgr)
		if err := s.udpServer.Start(); err != nil {
			utils.LogError("UDP server start failed", "error", err)
		} else {
			utils.LogInfo("UDP discovery started", "port", cfg.UDPPort)
		}
	}
	if cfg.EnableBluetooth {
		s.bluetoothMgr = newBluetoothMgr(cfg.BluetoothAdapter, s.mouseCtrl, s.deviceMgr)
		if err := s.bluetoothMgr.Start(); err != nil {
			utils.LogError("Bluetooth manager start failed", "error", err)
		} else {
			utils.LogInfo("Bluetooth manager started")
		}
	}
	if cfg.EnableSerial {
		s.usbServer = newUSBServer(s.mouseCtrl, s.deviceMgr)
		if err := s.usbServer.Start(); err != nil {
			utils.LogError("USB server start failed", "error", err)
		} else {
			utils.LogInfo("USB server started")
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
		_ = s.wsServer.Stop()
	}
	if s.udpServer != nil {
		s.udpServer.Stop()
	}
	if s.bluetoothMgr != nil {
		s.bluetoothMgr.Stop()
	}
	if s.usbServer != nil {
		s.usbServer.Stop()
	}
	s.running = false
	utils.LogInfo("All servers stopped")
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
	if s.usbServer != nil {
		stats["usb"] = s.usbServer.GetStats()
	}
	stats["devices"] = len(s.deviceMgr.GetAllDevices())
	return stats
}
