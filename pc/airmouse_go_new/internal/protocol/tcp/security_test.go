package tcp

import (
	"bufio"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"net"
	"testing"
	"time"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/tlsutil"
)

func TestTCPMandatoryAuthRejection(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	authMgr := auth.NewManager(secret)
	defer authMgr.StopCleanup()

	cfg := config.Get()
	cfg.AuthEnabled = true
	cfg.TLSEnabled = false

	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer("127.0.0.1", 0, mouse, deviceMgr, authMgr)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start TCP server: %v", err)
	}
	defer server.Stop()

	port := server.listener.Addr().(*net.TCPAddr).Port

	// 1. Connection with NO token -> MUST BE REJECTED
	conn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 2*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial TCP: %v", err)
	}
	defer conn.Close()

	reader := bufio.NewReader(conn)
	noTokenHello := `{"type":"hello","payload":{"name":"UnauthPhone","version":"2.0.0"}}` + "\n"
	_, _ = conn.Write([]byte(noTokenHello))

	_ = conn.SetReadDeadline(time.Now().Add(2 * time.Second))
	respLine, err := reader.ReadString('\n')
	if err != nil {
		t.Fatalf("Expected server to send rejection error before closing: %v", err)
	}

	var resp map[string]any
	if err := json.Unmarshal([]byte(respLine), &resp); err != nil {
		t.Fatalf("Failed to parse rejection error: %v", err)
	}
	if resp["type"] != "error" {
		t.Errorf("Expected response type 'error', got %v", resp["type"])
	}

	// 2. Connection with INVALID token -> MUST BE REJECTED
	conn2, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 2*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial TCP: %v", err)
	}
	defer conn2.Close()

	reader2 := bufio.NewReader(conn2)
	badTokenHello := `{"type":"hello","payload":{"name":"UnauthPhone","token":"bogus.token.sig"}}` + "\n"
	_, _ = conn2.Write([]byte(badTokenHello))

	_ = conn2.SetReadDeadline(time.Now().Add(2 * time.Second))
	respLine2, err := reader2.ReadString('\n')
	if err != nil {
		t.Fatalf("Expected server to send rejection error for bad token: %v", err)
	}
	var resp2 map[string]any
	_ = json.Unmarshal([]byte(respLine2), &resp2)
	if resp2["type"] != "error" {
		t.Errorf("Expected error response for bogus token, got %v", resp2)
	}

	// 3. Connection with VALID HMAC-SHA256 Token -> MUST BE ACCEPTED
	validToken, err := authMgr.GenerateAuthToken("dev-trusted-1", "ValidPhone")
	if err != nil {
		t.Fatalf("Failed to generate auth token: %v", err)
	}

	conn3, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 2*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial TCP: %v", err)
	}
	defer conn3.Close()

	reader3 := bufio.NewReader(conn3)
	validHello := fmt.Sprintf(`{"type":"hello","payload":{"name":"ValidPhone","device_id":"dev-trusted-1","token":"%s"}}`+"\n", validToken)
	_, _ = conn3.Write([]byte(validHello))

	time.Sleep(50 * time.Millisecond)
	_ = server.ApproveDevice("dev-trusted-1")

	_ = conn3.SetReadDeadline(time.Now().Add(2 * time.Second))
	respLine3, err := reader3.ReadString('\n')
	if err != nil {
		t.Fatalf("Expected welcome message for valid token: %v", err)
	}
	var resp3 map[string]any
	_ = json.Unmarshal([]byte(respLine3), &resp3)
	if resp3["type"] != "welcome" {
		t.Errorf("Expected 'welcome' response for valid token, got %v", resp3)
	}
}

func TestTCPTLSEnforcement(t *testing.T) {
	tempDir := t.TempDir()
	certPath := tempDir + "/test_server.crt"
	keyPath := tempDir + "/test_server.key"

	tlsCfg, err := tlsutil.GetServerTLSConfig(certPath, keyPath, []string{"127.0.0.1"})
	if err != nil {
		t.Fatalf("Failed to get TLS config: %v", err)
	}

	cfg := config.Get()
	cfg.AuthEnabled = false
	cfg.TLSEnabled = true

	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer("127.0.0.1", 0, mouse, deviceMgr, nil)
	server.SetTLSConfig(tlsCfg)

	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start TCP TLS server: %v", err)
	}
	defer server.Stop()

	port := server.listener.Addr().(*net.TCPAddr).Port

	// 1. Cleartext client MUST be rejected
	clearConn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 2*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial cleartext: %v", err)
	}
	defer clearConn.Close()

	_, _ = clearConn.Write([]byte(`{"type":"hello"}` + "\n"))
	buf := make([]byte, 128)
	_ = clearConn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	n, readErr := clearConn.Read(buf)
	if readErr == nil && n > 0 {
		t.Errorf("Expected cleartext connection to be terminated on TLS server, but read %d bytes: %q", n, string(buf[:n]))
	}

	// 2. TLS client MUST succeed
	clientTLS := tlsutil.GetClientTLSConfig(true)
	tlsConn, err := tls.DialWithDialer(&net.Dialer{Timeout: 2 * time.Second}, "tcp", fmt.Sprintf("127.0.0.1:%d", port), clientTLS)
	if err != nil {
		t.Fatalf("TLS dial failed: %v", err)
	}
	defer tlsConn.Close()

	// Send hello over TLS
	_, err = tlsConn.Write([]byte(`{"type":"hello","payload":{"name":"TLSClient","device_id":"tls-dev-1"}}` + "\n"))
	if err != nil {
		t.Fatalf("TLS write failed: %v", err)
	}

	time.Sleep(50 * time.Millisecond)
	_ = server.ApproveDevice("tls-dev-1")

	reader := bufio.NewReader(tlsConn)
	_ = tlsConn.SetReadDeadline(time.Now().Add(2 * time.Second))
	line, err := reader.ReadString('\n')
	if err != nil {
		t.Fatalf("Failed to read welcome over TLS: %v", err)
	}
	var resp map[string]any
	_ = json.Unmarshal([]byte(line), &resp)
	if resp["type"] != "welcome" {
		t.Errorf("Expected welcome on TLS, got %v", resp)
	}
}

func TestTCPBoundsChecks(t *testing.T) {
	cfg := config.Get()
	cfg.AuthEnabled = false
	cfg.TLSEnabled = false

	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer("127.0.0.1", 0, mouse, deviceMgr, nil)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start TCP server: %v", err)
	}
	defer server.Stop()

	port := server.listener.Addr().(*net.TCPAddr).Port

	conn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 2*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial: %v", err)
	}
	defer conn.Close()

	_, _ = conn.Write([]byte(`{"type":"hello","payload":{"name":"Tester","device_id":"tester-dev"}}` + "\n"))
	time.Sleep(50 * time.Millisecond)
	_ = server.ApproveDevice("tester-dev")

	// 1. Send out-of-bounds move (|dx| > 500)
	_, _ = conn.Write([]byte(`{"type":"move","payload":{"dx":999.0,"dy":10.0}}` + "\n"))
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	moveCount := len(mouse.moves)
	mouse.mu.Unlock()
	if moveCount > 0 {
		t.Errorf("Expected out-of-bounds move (999.0) to be dropped, but got moves: %+v", mouse.moves)
	}

	// 2. Send in-bounds move (|dx| <= 500)
	_, _ = conn.Write([]byte(`{"type":"move","payload":{"dx":15.0,"dy":-10.0}}` + "\n"))
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	moveCount = len(mouse.moves)
	mouse.mu.Unlock()
	if moveCount == 0 {
		t.Errorf("Expected in-bounds move (15.0, -10.0) to be processed")
	}
}
