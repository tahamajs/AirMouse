package tcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"sync"
	"testing"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/predict"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
)

// MockMouseController implements mouse.Controller for testing
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

func (m *MockMouseController) Stats() (clicks, dbl, right, scroll int64) { return 0, 0, 0, 0 }
func (m *MockMouseController) SetSensitivity(s float64) {}
func (m *MockMouseController) GetSensitivity() float64 { return 1.0 }
func (m *MockMouseController) SetSmoothing(enabled bool) {}
func (m *MockMouseController) SetAcceleration(enabled bool, factor float64) {}
func (m *MockMouseController) EnablePredictive(enabled bool) {}
func (m *MockMouseController) SetPredictiveBlendFactor(factor float64) {}
func (m *MockMouseController) EnableAISmoothing(enabled bool) {}
func (m *MockMouseController) SetAISmoother(s *predict.AISmoother) {}
func (m *MockMouseController) EnableMLPrediction(enabled bool) {}
func (m *MockMouseController) SetMLBlendFactor(factor float64) {}
func (m *MockMouseController) ResetStats() {}
func (m *MockMouseController) GetPosition() (x, y float64) { return 0, 0 }


func TestTCPServerHandshakeAndControl(t *testing.T) {
	// Initialize configuration
	cfg := config.Get()
	cfg.AuthEnabled = false // disable auth for easy testing

	mouseCtrl := &MockMouseController{}
	deviceMgr := device.NewManager()

	server := NewServer("127.0.0.1", 0, mouseCtrl, deviceMgr, nil)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start TCP server: %v", err)
	}
	defer server.Stop()

	// Get the dynamically allocated port
	port := server.listener.Addr().(*net.TCPAddr).Port

	// Connect mock client
	conn, err := net.Dial("tcp", fmt.Sprintf("127.0.0.1:%d", port))
	if err != nil {
		t.Fatalf("Failed to dial TCP server: %v", err)
	}
	defer conn.Close()

	reader := bufio.NewReader(conn)

	// Send Hello Message to start session and identify device
	helloMsg := `{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"test-id-123","protocol":"TCP","transport":"tcp"}}` + "\n"
	_, err = conn.Write([]byte(helloMsg))
	if err != nil {
		t.Fatalf("Failed to write hello message: %v", err)
	}

	// Approve client on server side
	time.Sleep(100 * time.Millisecond) // Allow server to register client
	var clientID string
	server.mu.Lock()
	for id := range server.clients {
		clientID = id
		break
	}
	server.mu.Unlock()

	if clientID == "" {
		t.Fatalf("Client not found in server clients map")
	}

	err = server.ApproveDevice(clientID)
	if err != nil {
		t.Fatalf("Failed to approve device: %v", err)
	}

	// Read welcome message
	welcomeLine, err := reader.ReadString('\n')
	if err != nil {
		t.Fatalf("Failed to read welcome message: %v", err)
	}

	var welcomeResp map[string]any
	if err := json.Unmarshal([]byte(welcomeLine), &welcomeResp); err != nil {
		t.Fatalf("Invalid welcome JSON: %v", err)
	}

	if welcomeResp["type"] != "welcome" {
		t.Errorf("Expected welcome message, got: %s", welcomeLine)
	}

	// Verify pairing status logic
	server.mu.RLock()
	clientObj := server.clients[clientID]
	server.mu.RUnlock()
	clientObj.mu.RLock()
	isApproved := clientObj.Approved
	clientObj.mu.RUnlock()
	if !isApproved {
		t.Errorf("Expected client to be approved")
	}

	// Test Control Messages (Pause/Resume movement)
	pauseMsg := `{"type":"control","payload":{"command":"pause_movement"}}` + "\n"
	_, _ = conn.Write([]byte(pauseMsg))
	time.Sleep(50 * time.Millisecond)

	if !common.IsMovementPaused() {
		t.Errorf("Expected movement to be paused")
	}

	resumeMsg := `{"type":"control","payload":{"command":"resume_movement"}}` + "\n"
	_, _ = conn.Write([]byte(resumeMsg))
	time.Sleep(50 * time.Millisecond)

	if common.IsMovementPaused() {
		t.Errorf("Expected movement to be resumed")
	}

	// Test Move Message (flat payload)
	moveMsg := `{"type":"move","dx":10.5,"dy":-5.2}` + "\n"
	_, _ = conn.Write([]byte(moveMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.moves) == 0 {
		t.Errorf("Expected mouse move to be registered")
	} else {
		lastMove := mouseCtrl.moves[len(mouseCtrl.moves)-1]
		if lastMove[0] != 10.5 || lastMove[1] != -5.2 {
			t.Errorf("Expected move dx=10.5 dy=-5.2, got: %+v", lastMove)
		}
	}
	mouseCtrl.mu.Unlock()

	// Test Click Message (requires ACK)
	clickMsg := `{"type":"click","button":"left","id":"click_1"}` + "\n"
	_, _ = conn.Write([]byte(clickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.clicks) == 0 || mouseCtrl.clicks[0] != "left" {
		t.Errorf("Expected left click registered, got clicks: %+v", mouseCtrl.clicks)
	}
	mouseCtrl.mu.Unlock()

	// Verify ACK message returned by server
	ackLine, err := reader.ReadString('\n')
	if err != nil {
		t.Fatalf("Failed to read ACK message: %v", err)
	}
	var ackResp map[string]any
	_ = json.Unmarshal([]byte(ackLine), &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "click_1" {
		t.Errorf("Expected ack for click_1, got: %s", ackLine)
	}

	// Test Double Click
	doubleClickMsg := `{"type":"doubleclick","id":"click_2"}` + "\n"
	_, _ = conn.Write([]byte(doubleClickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if mouseCtrl.doubleClicks != 1 {
		t.Errorf("Expected double click registered, got count: %d", mouseCtrl.doubleClicks)
	}
	mouseCtrl.mu.Unlock()

	ackLine, _ = reader.ReadString('\n')
	_ = json.Unmarshal([]byte(ackLine), &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "click_2" {
		t.Errorf("Expected ack for click_2, got: %s", ackLine)
	}

	// Test Right Click
	rightClickMsg := `{"type":"rightclick","id":"click_3"}` + "\n"
	_, _ = conn.Write([]byte(rightClickMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.clicks) < 2 || mouseCtrl.clicks[1] != "right" {
		t.Errorf("Expected right click registered, got clicks: %+v", mouseCtrl.clicks)
	}
	mouseCtrl.mu.Unlock()

	ackLine, _ = reader.ReadString('\n')
	_ = json.Unmarshal([]byte(ackLine), &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "click_3" {
		t.Errorf("Expected ack for click_3, got: %s", ackLine)
	}

	// Test Scroll Message (requires ACK)
	scrollMsg := `{"type":"scroll","delta":-3,"id":"scroll_1"}` + "\n"
	_, _ = conn.Write([]byte(scrollMsg))
	time.Sleep(50 * time.Millisecond)

	mouseCtrl.mu.Lock()
	if len(mouseCtrl.scrolls) == 0 || mouseCtrl.scrolls[0] != -3 {
		t.Errorf("Expected scroll -3 registered, got scrolls: %+v", mouseCtrl.scrolls)
	}
	mouseCtrl.mu.Unlock()

	ackLine, _ = reader.ReadString('\n')
	_ = json.Unmarshal([]byte(ackLine), &ackResp)
	if ackResp["type"] != "ack" || ackResp["id"] != "scroll_1" {
		t.Errorf("Expected ack for scroll_1, got: %s", ackLine)
	}

	// Test Proximity Message
	proxMsg := `{"type":"proximity","payload":{"device_id":"test-id-123","is_near":true,"distance":1.2}}` + "\n"
	_, _ = conn.Write([]byte(proxMsg))
	time.Sleep(50 * time.Millisecond)
	// Proximity message should be parsed and processed without crashing or errors.
}
