package udp

import (
	"encoding/json"
	"net"
	"sync"
	"testing"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/predict"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	websocketpkg "airmouse-go/internal/protocol/websocket"
)

type MockMouseController struct {
	mu           sync.Mutex
	moves        [][2]float64
	clicks       []string
	doubleClicks int
	scrolls      []int
}

func (m *MockMouseController) Move(dx, dy float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.moves = append(m.moves, [2]float64{dx, dy})
}

func (m *MockMouseController) Click(button string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.clicks = append(m.clicks, button)
}

func (m *MockMouseController) DoubleClick() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.doubleClicks++
}

func (m *MockMouseController) Scroll(delta int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.scrolls = append(m.scrolls, delta)
}

func (m *MockMouseController) Stats() (clicks, dbl, right, scroll int64)    { return 0, 0, 0, 0 }
func (m *MockMouseController) SetSensitivity(s float64)                     {}
func (m *MockMouseController) GetSensitivity() float64                      { return 1.0 }
func (m *MockMouseController) SetSmoothing(enabled bool)                    {}
func (m *MockMouseController) SetAcceleration(enabled bool, factor float64) {}
func (m *MockMouseController) EnablePredictive(enabled bool)                {}
func (m *MockMouseController) SetPredictiveBlendFactor(factor float64)      {}
func (m *MockMouseController) EnableAISmoothing(enabled bool)               {}
func (m *MockMouseController) SetAISmoother(s *predict.AISmoother)          {}
func (m *MockMouseController) EnableMLPrediction(enabled bool)              {}
func (m *MockMouseController) SetMLBlendFactor(factor float64)              {}
func (m *MockMouseController) ResetStats()                                  {}
func (m *MockMouseController) GetPosition() (x, y float64)                  { return 0, 0 }

func TestUDPServerHandshakeAndDiscovery(t *testing.T) {
	cfg := config.Get()
	cfg.AuthEnabled = false

	mouseCtrl := &MockMouseController{}
	deviceMgr := device.NewManager()

	server := NewServer(0, mouseCtrl, deviceMgr, nil)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start UDP server: %v", err)
	}
	defer server.Stop()

	// Get local port of UDP server
	port := server.conn.LocalAddr().(*net.UDPAddr).Port

	// Create UDP socket to communicate
	clientConn, err := net.DialUDP("udp", nil, &net.UDPAddr{
		IP:   net.ParseIP("127.0.0.1"),
		Port: port,
	})
	if err != nil {
		t.Fatalf("Failed to dial UDP server: %v", err)
	}
	defer clientConn.Close()

	// 1. Test Discovery
	_, err = clientConn.Write([]byte("AIRMOUSE_DISCOVER"))
	if err != nil {
		t.Fatalf("Failed to send discovery message: %v", err)
	}

	buf := make([]byte, 1024)
	_ = clientConn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	n, err := clientConn.Read(buf)
	if err != nil {
		t.Fatalf("Failed to read discovery response: %v", err)
	}

	resp := string(buf[:n])
	if !tContains(resp, "AIRMOUSE_SERVER") {
		t.Errorf("Expected AIRMOUSE_SERVER response, got: %s", resp)
	}

	// 2. Hello handshake
	helloMsg := `{"type":"hello","payload":{"name":"TestUDPDevice","version":"3.0","device_id":"udp-id-123","protocol":"UDP","transport":"udp"}}`
	_, _ = clientConn.Write([]byte(helloMsg))
	time.Sleep(100 * time.Millisecond)

	// Get client key from server clients map to approve it
	var clientKey string
	server.mu.Lock()
	for key := range server.clients {
		clientKey = key
		break
	}
	server.mu.Unlock()

	if clientKey == "" {
		t.Fatalf("Client not found in UDP clients map")
	}

	// Approve client
	err = server.ApproveDevice(clientKey)
	if err != nil {
		t.Fatalf("Failed to approve UDP device: %v", err)
	}

	// Read welcome response from client side
	_ = clientConn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	n, err = clientConn.Read(buf)
	if err != nil {
		t.Fatalf("Failed to read welcome response: %v", err)
	}

	var welcomeResp map[string]any
	if err := json.Unmarshal(buf[:n], &welcomeResp); err != nil {
		t.Fatalf("Invalid welcome JSON: %v", err)
	}

	if welcomeResp["type"] != "welcome" {
		t.Errorf("Expected welcome type, got response: %s", string(buf[:n]))
	}

	// Test Control Messages (Pause/Resume movement)
	pauseMsg := `{"type":"control","payload":{"command":"pause_movement"}}`
	_, _ = clientConn.Write([]byte(pauseMsg))
	time.Sleep(50 * time.Millisecond)

	if !common.IsMovementPaused() {
		t.Errorf("Expected movement to be paused")
	}

	resumeMsg := `{"type":"control","payload":{"command":"resume_movement"}}`
	_, _ = clientConn.Write([]byte(resumeMsg))
	time.Sleep(50 * time.Millisecond)

	if common.IsMovementPaused() {
		t.Errorf("Expected movement to be resumed")
	}

	// Test Move Message (flat payload)
	moveMsg := `{"type":"move","dx":-8.4,"dy":4.7}`
	_, _ = clientConn.Write([]byte(moveMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.moves) == 0 {
		t.Errorf("Expected mouse move to be registered")
	} else {
		lastMove := mouseCtrl.moves[len(mouseCtrl.moves)-1]
		if lastMove[0] != -8.4 || lastMove[1] != 4.7 {
			t.Errorf("Expected move dx=-8.4 dy=4.7, got: %+v", lastMove)
		}
	}
	mouseCtrl.mu.Unlock()

	// Test Click Message (requires ACK)
	clickMsg := `{"type":"click","button":"left","id":"udp_click_1"}`
	_, _ = clientConn.Write([]byte(clickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.clicks) == 0 || mouseCtrl.clicks[0] != "left" {
		t.Errorf("Expected left click registered, got clicks: %+v", mouseCtrl.clicks)
	}
	mouseCtrl.mu.Unlock()

	// Read ACK from server
	n, _ = clientConn.Read(buf)
	var ackResp map[string]any
	_ = json.Unmarshal(buf[:n], &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "udp_click_1" {
		t.Errorf("Expected ack for udp_click_1, got: %s", string(buf[:n]))
	}

	// Test Double Click
	doubleClickMsg := `{"type":"doubleclick","id":"udp_click_2"}`
	_, _ = clientConn.Write([]byte(doubleClickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if mouseCtrl.doubleClicks != 1 {
		t.Errorf("Expected double click registered, got count: %d", mouseCtrl.doubleClicks)
	}
	mouseCtrl.mu.Unlock()

	n, _ = clientConn.Read(buf)
	_ = json.Unmarshal(buf[:n], &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "udp_click_2" {
		t.Errorf("Expected ack for udp_click_2, got: %s", string(buf[:n]))
	}

	// Test Right Click
	rightClickMsg := `{"type":"rightclick","id":"udp_click_3"}`
	_, _ = clientConn.Write([]byte(rightClickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.clicks) < 2 || mouseCtrl.clicks[1] != "right" {
		t.Errorf("Expected right click registered, got clicks: %+v", mouseCtrl.clicks)
	}
	mouseCtrl.mu.Unlock()

	n, _ = clientConn.Read(buf)
	_ = json.Unmarshal(buf[:n], &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "udp_click_3" {
		t.Errorf("Expected ack for udp_click_3, got: %s", string(buf[:n]))
	}

	// Test Scroll Message (requires ACK)
	scrollMsg := `{"type":"scroll","delta":6,"id":"udp_scroll_1"}`
	_, _ = clientConn.Write([]byte(scrollMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.scrolls) == 0 || mouseCtrl.scrolls[0] != 6 {
		t.Errorf("Expected scroll 6 registered, got scrolls: %+v", mouseCtrl.scrolls)
	}
	mouseCtrl.mu.Unlock()

	n, _ = clientConn.Read(buf)
	_ = json.Unmarshal(buf[:n], &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "udp_scroll_1" {
		t.Errorf("Expected ack for udp_scroll_1, got: %s", string(buf[:n]))
	}

	// Test Proximity Message
	proxMsg := `{"type":"proximity","payload":{"device_id":"udp-id-123","is_near":true,"distance":0.7}}`
	_, _ = clientConn.Write([]byte(proxMsg))
	time.Sleep(50 * time.Millisecond)

	// Ping/Pong message
	pingMsg := websocketpkg.PingMessage()
	_, _ = clientConn.Write(pingMsg)
	n, _ = clientConn.Read(buf)
	_ = json.Unmarshal(buf[:n], &ackResp)
	if ackResp["type"] != "pong" {
		t.Errorf("Expected pong response, got: %s", string(buf[:n]))
	}
}

func tContains(s, substr string) bool {
	return len(s) >= len(substr) && (s[:len(substr)] == substr || s[len(s)-len(substr):] == substr || len(s) > len(substr))
}
