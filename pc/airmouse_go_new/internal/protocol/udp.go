package udp

import (
	"encoding/json"
	"net"
	"strings"
	"time"

	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

type Server struct {
	port      int
	conn      *net.UDPConn
	deviceMgr *device.Manager
	running   bool
}

func NewServer(port int, deviceMgr *device.Manager) *Server {
	return &Server{port: port, deviceMgr: deviceMgr}
}

func (s *Server) Start() error {
	addr := net.UDPAddr{Port: s.port}
	conn, err := net.ListenUDP("udp4", &addr)
	if err != nil {
		return err
	}
	s.conn = conn
	s.running = true
	go s.listenLoop()
	utils.LogInfo("UDP discovery server started", "port", s.port)
	return nil
}

func (s *Server) listenLoop() {
	buf := make([]byte, 1024)
	for s.running {
		s.conn.SetReadDeadline(time.Now().Add(2 * time.Second))
		n, clientAddr, err := s.conn.ReadFromUDP(buf)
		if err != nil {
			continue
		}
		msg := strings.TrimSpace(string(buf[:n]))
		if msg == "AIRMOUSE_DISCOVER" {
			response := map[string]interface{}{
				"type": "discovery_response",
				"port": 8080,
				"ip":   getLocalIP(),
			}
			data, _ := json.Marshal(response)
			s.conn.WriteToUDP(data, clientAddr)
			utils.LogDebug("UDP discovery response sent", "to", clientAddr.IP)
		}
	}
}

func (s *Server) Stop() {
	s.running = false
	if s.conn != nil {
		s.conn.Close()
	}
}

func (s *Server) GetStats() map[string]interface{} {
	return map[string]interface{}{"running": s.running}
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			return ipnet.IP.String()
		}
	}
	return "127.0.0.1"
}