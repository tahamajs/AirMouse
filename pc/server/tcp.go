package server

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"sync"
	"time"
	"airmouse-go/control"
)

type TCPServer struct {
	host      string
	port      int
	mouse     *control.MouseController
	conns     map[string]*client
	mu        sync.Mutex
	logFunc   func(string)
	statsFunc func(int, int, int, int)
	connCb    func([]string)
}

type client struct {
	conn       net.Conn
	start      time.Time
	lastActive time.Time
	name       string
	bytesSent  int64
	bytesRecv  int64
}

type msg struct {
	Type    string         `json:"type"`
	Payload map[string]any `json:"payload"`
	ID      *string        `json:"id,omitempty"`
}

func NewTCPServer(host string, port int, mouse *control.MouseController, logFn func(string), statsFn func(int, int, int, int), connCb func([]string)) *TCPServer {
	return &TCPServer{
		host:      host,
		port:      port,
		mouse:     mouse,
		conns:     make(map[string]*client),
		logFunc:   logFn,
		statsFunc: statsFn,
		connCb:    connCb,
	}
}

func (s *TCPServer) Start() error {
	ln, err := net.Listen("tcp", fmt.Sprintf("%s:%d", s.host, s.port))
	if err != nil {
		return err
	}
	s.logFunc(fmt.Sprintf("🚀 TCP listening on %s:%d", s.host, s.port))
	go s.acceptLoop(ln)
	return nil
}

func (s *TCPServer) acceptLoop(ln net.Listener) {
	for {
		conn, err := ln.Accept()
		if err != nil {
			return
		}
		go s.handleClient(conn)
	}
}

func (s *TCPServer) handleClient(conn net.Conn) {
	addr := conn.RemoteAddr().String()
	start := time.Now()
	c := &client{conn: conn, start: start, lastActive: start}
	s.mu.Lock()
	s.conns[addr] = c
	s.mu.Unlock()
	s.logFunc(fmt.Sprintf("✅ Connected: %s", addr))
	s.sendStats()
	go s.watchdog(addr, conn)

	reader := bufio.NewReader(conn)
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			if err == io.EOF {
				break
			}
			s.logFunc(fmt.Sprintf("⚠️ Read error from %s: %v", addr, err))
			break
		}
		c.lastActive = time.Now()
		c.bytesRecv += int64(len(line))
		var m msg
		if err := json.Unmarshal([]byte(line), &m); err != nil {
			s.logFunc(fmt.Sprintf("⚠️ Invalid JSON from %s: %v", addr, err))
			continue
		}
		s.processMessage(m, conn, addr, c)
	}
	conn.Close()
	s.mu.Lock()
	delete(s.conns, addr)
	s.mu.Unlock()
	duration := time.Since(start)
	s.logFunc(fmt.Sprintf("🔌 Disconnected: %s (after %.0fs)", addr, duration.Seconds()))
	s.sendStats()
}

func (s *TCPServer) watchdog(addr string, conn net.Conn) {
	for {
		time.Sleep(10 * time.Second)
		s.mu.Lock()
		c, ok := s.conns[addr]
		s.mu.Unlock()
		if !ok {
			return
		}
		if time.Since(c.lastActive) > 10*time.Second {
			s.logFunc(fmt.Sprintf("🕒 Watchdog: %s idle too long, closing", addr))
			conn.Close()
			return
		}
	}
}

func (s *TCPServer) processMessage(m msg, conn net.Conn, addr string, c *client) {
	switch m.Type {
	case "move":
		dx, _ := m.Payload["dx"].(float64)
		dy, _ := m.Payload["dy"].(float64)
		s.mouse.Move(dx, dy)
	case "hello":
		name, _ := m.Payload["name"].(string)
		if name == "" {
			name = "Unknown"
		}
		c.name = name
		s.logFunc(fmt.Sprintf("🖥️ %s identified as '%s'", addr, name))
		s.sendStats()
	case "click", "doubleclick", "rightclick", "scroll":
		switch m.Type {
		case "click":
			s.mouse.Click("left")
		case "doubleclick":
			s.mouse.DoubleClick()
		case "rightclick":
			s.mouse.Click("right")
		case "scroll":
			delta, _ := m.Payload["delta"].(float64)
			s.mouse.Scroll(int(delta))
		}
		s.sendAck(m.ID, conn)
		s.logFunc(fmt.Sprintf("🖱️ %s from %s", m.Type, addr))
	default:
		s.logFunc(fmt.Sprintf("⚠️ Unknown type '%s' from %s", m.Type, addr))
	}
	s.sendStats()
}

func (s *TCPServer) sendAck(id *string, conn net.Conn) {
	if id == nil {
		return
	}
	ack := fmt.Sprintf(`{"type":"ack","id":%q}`+"\n", *id)
	conn.Write([]byte(ack))
}

func (s *TCPServer) sendStats() {
	clicks, dbl, right, scroll := s.mouse.Stats()
	if s.statsFunc != nil {
		s.statsFunc(int(clicks), int(dbl), int(right), int(scroll))
	}
	if s.connCb != nil {
		var list []string
		s.mu.Lock()
		for addr, c := range s.conns {
			name := c.name
			if name == "" {
				name = "Unknown"
			}
			uptime := int(time.Since(c.start).Seconds())
			idle := int(time.Since(c.lastActive).Seconds())
			list = append(list, fmt.Sprintf("%s | %s | up %ds | idle %ds", name, addr, uptime, idle))
		}
		s.mu.Unlock()
		s.connCb(list)
	}
}

func (s *TCPServer) Stop() {
	// In a full implementation, close the listener and all connections.
}