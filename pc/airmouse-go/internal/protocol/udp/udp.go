package udp

import (
    "log"
    "net"
    "time"
    "airmouse-go/internal/device"
)

type Server struct {
    port int
    running bool
    deviceMgr *device.Manager
    conn *net.UDPConn
}

func NewServer(port int, deviceMgr *device.Manager) *Server {
    return &Server{port: port, deviceMgr: deviceMgr}
}

func (s *Server) Start() error {
    addr := net.UDPAddr{Port: s.port, IP: net.ParseIP("0.0.0.0")}
    conn, err := net.ListenUDP("udp", &addr)
    if err != nil {
        return err
    }
    s.conn = conn
    s.running = true

    go func() {
        buf := make([]byte, 1024)
        for s.running {
            s.conn.SetReadDeadline(time.Now().Add(2 * time.Second))
            n, remote, err := s.conn.ReadFromUDP(buf)
            if err != nil {
                continue
            }
            log.Printf("[udp] received %d bytes from %s", n, remote.String())
            // Optionally register device
            s.deviceMgr.RegisterDevice(remote.String(), "udp", "UDP Device")
        }
    }()

    return nil
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
