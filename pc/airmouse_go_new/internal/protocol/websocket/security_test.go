package websocket

import (
	"crypto/tls"
	"fmt"
	"net"
	"net/http"
	"strings"
	"sync"
	"testing"
	"time"

	gorilla "github.com/gorilla/websocket"

	"airmouse-go/control/predict"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/tlsutil"
)

type MockMouseController struct {
	mu     sync.Mutex
	moves  [][2]float64
	clicks []string
}

func (m *MockMouseController) Move(dx, dy float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.moves = append(m.moves, [2]float64{dx, dy})
}
func (m *MockMouseController) Click(button string)                   {}
func (m *MockMouseController) DoubleClick()                          {}
func (m *MockMouseController) Scroll(delta int)                      {}
func (m *MockMouseController) Stats() (clicks, dbl, right, sc int64) { return 0, 0, 0, 0 }
func (m *MockMouseController) SetSensitivity(s float64)              {}
func (m *MockMouseController) GetSensitivity() float64               { return 1.0 }
func (m *MockMouseController) SetSmoothing(enabled bool)             {}
func (m *MockMouseController) SetAcceleration(enabled bool, f float64) {}
func (m *MockMouseController) EnablePredictive(enabled bool)         {}
func (m *MockMouseController) SetPredictiveBlendFactor(f float64)    {}
func (m *MockMouseController) EnableAISmoothing(enabled bool)        {}
func (m *MockMouseController) SetAISmoother(s *predict.AISmoother)   {}
func (m *MockMouseController) EnableMLPrediction(enabled bool)       {}
func (m *MockMouseController) SetMLBlendFactor(f float64)           {}
func (m *MockMouseController) ResetStats()                           {}
func (m *MockMouseController) GetPosition() (x, y float64)           { return 0, 0 }
func (m *MockMouseController) MouseDown(button string)               {}
func (m *MockMouseController) MouseUp(button string)                 {}

func getFreePort(t *testing.T) int {
	t.Helper()
	l, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("Failed to get free port: %v", err)
	}
	port := l.Addr().(*net.TCPAddr).Port
	_ = l.Close()
	return port
}

func TestWebSocketMandatoryAuth(t *testing.T) {
	secret := "websocket-test-secret-32-bytes-long!!"
	authMgr := auth.NewManager(secret)
	defer authMgr.StopCleanup()

	cfg := config.Get()
	cfg.AuthEnabled = true
	cfg.TLSEnabled = false

	port := getFreePort(t)
	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer(port, mouse, deviceMgr, authMgr, nil)

	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start WebSocket server: %v", err)
	}
	defer server.Stop()
	time.Sleep(50 * time.Millisecond)

	// 1. Client connects without query token, sends hello without token -> REJECTED
	wsURL := fmt.Sprintf("ws://127.0.0.1:%d/ws", port)
	conn, _, err := gorilla.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("Failed to dial WS: %v", err)
	}
	defer conn.Close()

	helloNoToken := `{"type":"hello","payload":{"name":"BadClient","device_id":"bad-dev-1"}}`
	if err := conn.WriteMessage(gorilla.TextMessage, []byte(helloNoToken)); err != nil {
		t.Fatalf("Failed to write hello: %v", err)
	}

	_ = conn.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, msg, err := conn.ReadMessage()
	if err != nil {
		t.Fatalf("Expected rejection message from server: %v", err)
	}
	if !strings.Contains(string(msg), "invalid pairing token") {
		t.Errorf("Expected 'invalid pairing token' rejection, got: %s", string(msg))
	}

	// 2. Client connects with valid query token -> ACCEPTED
	validToken, err := authMgr.GenerateAuthToken("ws-dev-valid", "ValidPhone")
	if err != nil {
		t.Fatalf("Failed to generate token: %v", err)
	}

	wsAuthURL := fmt.Sprintf("ws://127.0.0.1:%d/ws?token=%s", port, validToken)
	connAuth, _, err := gorilla.DefaultDialer.Dial(wsAuthURL, nil)
	if err != nil {
		t.Fatalf("Failed to dial WS with valid query token: %v", err)
	}
	defer connAuth.Close()

	// 3. Client connects without query token, but provides valid token in hello payload -> ACCEPTED
	connHelloAuth, _, err := gorilla.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("Failed to dial WS: %v", err)
	}
	defer connHelloAuth.Close()

	validHello := fmt.Sprintf(`{"type":"hello","payload":{"name":"ValidClient","device_id":"ws-dev-valid-2","token":"%s"}}`, validToken)
	if err := connHelloAuth.WriteMessage(gorilla.TextMessage, []byte(validHello)); err != nil {
		t.Fatalf("Failed to write valid hello: %v", err)
	}
	time.Sleep(50 * time.Millisecond)
	_ = server.ApproveDevice("ws-dev-valid-2")

	// Send in-bounds move
	moveMsg := `{"type":"move","payload":{"dx":20.0,"dy":-10.0}}`
	if err := connHelloAuth.WriteMessage(gorilla.TextMessage, []byte(moveMsg)); err != nil {
		t.Fatalf("Failed to send move: %v", err)
	}

	time.Sleep(50 * time.Millisecond)
	mouse.mu.Lock()
	moveCount := len(mouse.moves)
	mouse.mu.Unlock()
	if moveCount == 0 {
		t.Errorf("Expected authenticated move to be processed")
	}
}

func TestWebSocketTLSEnforcement(t *testing.T) {
	tempDir := t.TempDir()
	certPath := tempDir + "/ws_server.crt"
	keyPath := tempDir + "/ws_server.key"

	tlsCfg, err := tlsutil.GetServerTLSConfig(certPath, keyPath, []string{"localhost", "127.0.0.1"})
	if err != nil {
		t.Fatalf("Failed to get TLS config: %v", err)
	}

	cfg := config.Get()
	cfg.AuthEnabled = false
	cfg.TLSEnabled = true

	port := getFreePort(t)
	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer(port, mouse, deviceMgr, nil, nil)
	server.SetTLSConfig(tlsCfg)

	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start WSS server: %v", err)
	}
	defer server.Stop()
	time.Sleep(50 * time.Millisecond)

	// 1. Plain cleartext HTTP/WS MUST fail
	clearWSURL := fmt.Sprintf("ws://127.0.0.1:%d/ws", port)
	_, _, err = gorilla.DefaultDialer.Dial(clearWSURL, nil)
	if err == nil {
		t.Errorf("Expected cleartext WS dial to fail on TLS server, but got nil")
	}

	clearURL := fmt.Sprintf("http://127.0.0.1:%d/health", port)
	client := &http.Client{Timeout: 1 * time.Second}
	resp, err := client.Get(clearURL)
	if err == nil && resp.StatusCode == http.StatusOK {
		t.Errorf("Expected cleartext HTTP /health to not return 200 OK on TLS server, got status %d", resp.StatusCode)
	}

	// 2. WSS (TLS) connection MUST succeed
	wssURL := fmt.Sprintf("wss://127.0.0.1:%d/ws", port)
	dialer := gorilla.Dialer{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	conn, resp, err := dialer.Dial(wssURL, nil)
	if err != nil {
		t.Fatalf("WSS dial failed: %v", err)
	}
	defer conn.Close()
	if resp.StatusCode != http.StatusSwitchingProtocols {
		t.Errorf("Expected 101 Switching Protocols, got %d", resp.StatusCode)
	}
}

func TestWebSocketBoundsChecks(t *testing.T) {
	cfg := config.Get()
	cfg.AuthEnabled = false
	cfg.TLSEnabled = false

	port := getFreePort(t)
	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer(port, mouse, deviceMgr, nil, nil)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start WS server: %v", err)
	}
	defer server.Stop()
	time.Sleep(50 * time.Millisecond)

	wsURL := fmt.Sprintf("ws://127.0.0.1:%d/ws", port)
	conn, _, err := gorilla.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("Failed to dial WS: %v", err)
	}
	defer conn.Close()

	hello := `{"type":"hello","payload":{"name":"Tester","device_id":"ws-bounds-dev"}}`
	_ = conn.WriteMessage(gorilla.TextMessage, []byte(hello))
	time.Sleep(50 * time.Millisecond)
	_ = server.ApproveDevice("ws-bounds-dev")

	// Out-of-bounds move (|dx| > 500)
	oobMove := `{"type":"move","payload":{"dx":600.0,"dy":0.0}}`
	_ = conn.WriteMessage(gorilla.TextMessage, []byte(oobMove))
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	moveCount := len(mouse.moves)
	mouse.mu.Unlock()
	if moveCount > 0 {
		t.Errorf("Expected out-of-bounds move (600.0) to be dropped")
	}
}
