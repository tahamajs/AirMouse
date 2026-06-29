package tcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/predict"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
)

// ============================================================
// Test helpers
// ============================================================

// mockClient wraps a TCP connection and provides convenient send/receive helpers
// that mirror the Android ConnectionManager protocol.
type mockClient struct {
	conn   net.Conn
	reader *bufio.Reader
	t      *testing.T
}

func dialMock(t *testing.T, port int) *mockClient {
	t.Helper()
	conn, err := net.DialTimeout("tcp", fmt.Sprintf("127.0.0.1:%d", port), 5*time.Second)
	if err != nil {
		t.Fatalf("Failed to dial TCP server: %v", err)
	}
	return &mockClient{conn: conn, reader: bufio.NewReader(conn), t: t}
}

func (c *mockClient) close() { _ = c.conn.Close() }

func (c *mockClient) sendJSON(msg map[string]any) {
	c.t.Helper()
	data, err := json.Marshal(msg)
	if err != nil {
		c.t.Fatalf("Marshal error: %v", err)
	}
	data = append(data, '\n')
	if _, err := c.conn.Write(data); err != nil {
		c.t.Fatalf("Write error: %v", err)
	}
}

func (c *mockClient) readMsg(timeout time.Duration) map[string]any {
	c.t.Helper()
	_ = c.conn.SetReadDeadline(time.Now().Add(timeout))
	line, err := c.reader.ReadString('\n')
	if err != nil {
		c.t.Fatalf("Read error: %v", err)
	}
	var msg map[string]any
	if err := json.Unmarshal([]byte(line), &msg); err != nil {
		c.t.Fatalf("Invalid JSON from server: %v\nRaw: %s", err, line)
	}
	return msg
}

func (c *mockClient) tryReadMsg(timeout time.Duration) (map[string]any, error) {
	_ = c.conn.SetReadDeadline(time.Now().Add(timeout))
	line, err := c.reader.ReadString('\n')
	if err != nil {
		return nil, err
	}
	var msg map[string]any
	if err := json.Unmarshal([]byte(line), &msg); err != nil {
		return nil, err
	}
	return msg, nil
}

func (c *mockClient) sendRaw(s string) {
	c.t.Helper()
	if !strings.HasSuffix(s, "\n") {
		s += "\n"
	}
	if _, err := c.conn.Write([]byte(s)); err != nil {
		c.t.Fatalf("Write error: %v", err)
	}
}

// startTestServer creates and starts a TCP server on a random port.
func startTestServer(t *testing.T) (*Server, *MockMouseController, int) {
	t.Helper()
	cfg := config.Get()
	cfg.AuthEnabled = false
	cfg.ConfigPath = filepath.Join(t.TempDir(), "config.json")
	cfg.TrustedDevices = []string{}

	mouse := &MockMouseController{}
	deviceMgr := device.NewManager()
	server := NewServer("127.0.0.1", 0, mouse, deviceMgr, nil)
	if err := server.Start(); err != nil {
		t.Fatalf("Failed to start TCP server: %v", err)
	}
	port := server.listener.Addr().(*net.TCPAddr).Port
	return server, mouse, port
}

// approveFirstClient finds and approves the first client in the server.
func approveFirstClient(t *testing.T, server *Server) string {
	t.Helper()
	time.Sleep(100 * time.Millisecond)
	server.mu.RLock()
	var clientID string
	for id := range server.clients {
		clientID = id
		break
	}
	server.mu.RUnlock()
	if clientID == "" {
		t.Fatalf("No client found in server")
	}
	if err := server.ApproveDevice(clientID); err != nil {
		t.Fatalf("Failed to approve device: %v", err)
	}
	return clientID
}

// ============================================================
// Test: Android-format hello with nested payload
// ============================================================

func TestAndroidHelloWithNestedPayload(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Send hello exactly as Android ConnectionManager does (nested payload)
	client.sendJSON(map[string]any{
		"type": "hello",
		"payload": map[string]any{
			"name":            "Pixel 7",
			"version":         "4.9.9",
			"device":          "Google Pixel 7",
			"android_version": "14",
			"model":           "Pixel 7",
			"manufacturer":    "Google",
			"brand":           "google",
			"device_name":     "panther",
			"sdk_int":         "34",
			"device_id":       "test-android-id-001",
			"protocol":        "TCP",
			"transport":       "tcp",
		},
	})

	// Approve and read welcome
	clientID := approveFirstClient(t, server)
	welcome := client.readMsg(5 * time.Second)

	if welcome["type"] != "welcome" {
		t.Errorf("Expected welcome, got: %v", welcome)
	}
	payload, _ := welcome["payload"].(map[string]any)
	if payload == nil {
		t.Fatalf("Welcome payload is nil")
	}
	if _, ok := payload["server"]; !ok {
		t.Errorf("Welcome missing server field")
	}

	// Verify client metadata was stored
	server.mu.RLock()
	c := server.clients[clientID]
	server.mu.RUnlock()
	c.mu.RLock()
	if !c.Approved {
		t.Errorf("Client should be approved")
	}
	c.mu.RUnlock()
}

// ============================================================
// Test: Move with both flat and DeltaX/DeltaY fields
// ============================================================

func TestMoveWithDualFieldFormat(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t1","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second) // welcome

	// Test 1: Flat format (as Go server natively supports)
	client.sendJSON(map[string]any{
		"type": "move",
		"dx":   15.5,
		"dy":   -8.3,
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if len(mouse.moves) != 1 || mouse.moves[0][0] != 15.5 || mouse.moves[0][1] != -8.3 {
		t.Errorf("Flat move failed, got: %v", mouse.moves)
	}
	mouse.mu.Unlock()

	// Test 2: Android format with both dx/dy AND DeltaX/DeltaY
	client.sendJSON(map[string]any{
		"type":   "move",
		"dx":     3.0,
		"dy":     -1.5,
		"DeltaX": 3.0,
		"DeltaY": -1.5,
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if len(mouse.moves) != 2 {
		t.Errorf("Expected 2 moves, got %d", len(mouse.moves))
	} else if mouse.moves[1][0] != 3.0 || mouse.moves[1][1] != -1.5 {
		t.Errorf("Android-format move failed, got: %v", mouse.moves[1])
	}
	mouse.mu.Unlock()
}

// ============================================================
// Test: Click with ACK response
// ============================================================

func TestClickWithACK(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t2","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second) // welcome

	// Send click with ID (as Android does via sendReliablePacket)
	client.sendJSON(map[string]any{
		"type":   "click",
		"id":     "msg_0",
		"button": "left",
		"Click":  "left",
	})
	time.Sleep(50 * time.Millisecond)

	// Verify click was executed
	mouse.mu.Lock()
	if len(mouse.clicks) != 1 || mouse.clicks[0] != "left" {
		t.Errorf("Expected left click, got: %v", mouse.clicks)
	}
	mouse.mu.Unlock()

	// Verify ACK was sent back
	ack := client.readMsg(2 * time.Second)
	if ack["type"] != "ack" {
		t.Errorf("Expected ack, got type: %v", ack["type"])
	}
	if ack["id"] != "msg_0" {
		t.Errorf("Expected ack id=msg_0, got: %v", ack["id"])
	}
}

// ============================================================
// Test: Scroll with ACK response
// ============================================================

func TestScrollWithACK(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t3","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second) // welcome

	// Send scroll with ID (as Android does via sendReliablePacket)
	client.sendJSON(map[string]any{
		"type":   "scroll",
		"id":     "msg_1",
		"delta":  -5,
		"Scroll": -5,
	})
	time.Sleep(50 * time.Millisecond)

	// Verify scroll was executed
	mouse.mu.Lock()
	if len(mouse.scrolls) != 1 || mouse.scrolls[0] != -5 {
		t.Errorf("Expected scroll -5, got: %v", mouse.scrolls)
	}
	mouse.mu.Unlock()

	// Verify ACK
	ack := client.readMsg(2 * time.Second)
	if ack["type"] != "ack" || ack["id"] != "msg_1" {
		t.Errorf("Expected ack for msg_1, got: %v", ack)
	}
}

// ============================================================
// Test: Double Click and Right Click ACKs
// ============================================================

func TestDoubleAndRightClickACKs(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t4","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second) // welcome

	// Double click
	client.sendJSON(map[string]any{
		"type":   "doubleclick",
		"id":     "msg_2",
		"button": "double",
		"Click":  "double",
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if mouse.doubleClicks != 1 {
		t.Errorf("Expected 1 double click, got: %d", mouse.doubleClicks)
	}
	mouse.mu.Unlock()

	ack := client.readMsg(2 * time.Second)
	if ack["type"] != "ack" || ack["id"] != "msg_2" {
		t.Errorf("Expected ack for msg_2, got: %v", ack)
	}

	// Right click
	client.sendJSON(map[string]any{
		"type":   "rightclick",
		"id":     "msg_3",
		"button": "right",
		"Click":  "right",
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if len(mouse.clicks) != 1 || mouse.clicks[0] != "right" {
		t.Errorf("Expected right click, got: %v", mouse.clicks)
	}
	mouse.mu.Unlock()

	ack = client.readMsg(2 * time.Second)
	if ack["type"] != "ack" || ack["id"] != "msg_3" {
		t.Errorf("Expected ack for msg_3, got: %v", ack)
	}
}

// ============================================================
// Test: Ping/Pong heartbeat
// ============================================================

func TestPingPong(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t5","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second) // welcome

	// Send ping (as Android does)
	client.sendJSON(map[string]any{"type": "ping"})

	// Read pong
	pong := client.readMsg(2 * time.Second)
	if pong["type"] != "pong" {
		t.Errorf("Expected pong, got: %v", pong)
	}
}

// ============================================================
// Test: Control commands (pause/resume movement)
// ============================================================

func TestControlPauseResume(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t6","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Ensure movement starts unpaused
	common.SetMovementPaused(false)

	// Pause
	client.sendJSON(map[string]any{
		"type": "control",
		"payload": map[string]any{
			"command": "pause_movement",
		},
	})
	time.Sleep(50 * time.Millisecond)
	if !common.IsMovementPaused() {
		t.Errorf("Expected movement paused")
	}

	// Resume
	client.sendJSON(map[string]any{
		"type": "control",
		"payload": map[string]any{
			"command": "resume_movement",
		},
	})
	time.Sleep(50 * time.Millisecond)
	if common.IsMovementPaused() {
		t.Errorf("Expected movement resumed")
	}
}

// ============================================================
// Test: ACK retransmission simulation
// ============================================================

func TestACKRetransmission(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t7","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Simulate retransmission: send same click message 3 times (as Android would retry)
	for i := 0; i < 3; i++ {
		client.sendJSON(map[string]any{
			"type":   "click",
			"id":     "retry_msg_1",
			"button": "left",
		})
		time.Sleep(30 * time.Millisecond)
	}
	time.Sleep(50 * time.Millisecond)

	// Server should have processed all 3 (no dedup on server side — that's fine,
	// the ACK mechanism is client-side to stop retransmitting)
	mouse.mu.Lock()
	clickCount := len(mouse.clicks)
	mouse.mu.Unlock()
	if clickCount < 1 {
		t.Errorf("Expected at least 1 click, got: %d", clickCount)
	}

	// Should get 3 ACKs back (one per click)
	for i := 0; i < clickCount; i++ {
		ack := client.readMsg(2 * time.Second)
		if ack["type"] != "ack" || ack["id"] != "retry_msg_1" {
			t.Errorf("Expected ack for retry_msg_1, got: %v", ack)
		}
	}
}

// ============================================================
// Test: Moves are ignored before approval
// ============================================================

func TestMovesIgnoredBeforeApproval(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Send hello but DON'T approve yet
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t8","protocol":"TCP","transport":"tcp"}}`)
	time.Sleep(100 * time.Millisecond)

	// Send move — should be ignored
	client.sendJSON(map[string]any{
		"type": "move",
		"dx":   100.0,
		"dy":   200.0,
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if len(mouse.moves) != 0 {
		t.Errorf("Expected 0 moves before approval, got: %d", len(mouse.moves))
	}
	mouse.mu.Unlock()

	// Now approve
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Send move again — should work now
	client.sendJSON(map[string]any{
		"type": "move",
		"dx":   10.0,
		"dy":   20.0,
	})
	time.Sleep(50 * time.Millisecond)

	mouse.mu.Lock()
	if len(mouse.moves) != 1 {
		t.Errorf("Expected 1 move after approval, got: %d", len(mouse.moves))
	}
	mouse.mu.Unlock()
}

// ============================================================
// Test: Latency measurement
// ============================================================

func TestLatencyMeasurement(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t9","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Measure round-trip ping/pong latency
	const iterations = 10
	var totalLatency time.Duration

	for i := 0; i < iterations; i++ {
		start := time.Now()
		client.sendJSON(map[string]any{"type": "ping"})
		pong := client.readMsg(2 * time.Second)
		elapsed := time.Since(start)

		if pong["type"] != "pong" {
			t.Errorf("Iteration %d: expected pong, got %v", i, pong["type"])
		}
		totalLatency += elapsed
	}

	avgLatency := totalLatency / iterations
	t.Logf("Average ping/pong latency over %d iterations: %v", iterations, avgLatency)

	// Localhost should be under 10ms
	if avgLatency > 10*time.Millisecond {
		t.Logf("WARNING: Average latency %.2fms is higher than expected for localhost", float64(avgLatency.Microseconds())/1000)
	}
}

// ============================================================
// Test: Burst of move messages (throughput test)
// ============================================================

func TestMoveBurst(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t10","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Send 100 move messages rapidly
	const moveCount = 100
	start := time.Now()
	for i := 0; i < moveCount; i++ {
		client.sendJSON(map[string]any{
			"type": "move",
			"dx":   float64(i) * 0.1,
			"dy":   float64(i) * -0.1,
		})
	}
	elapsed := time.Since(start)

	// Wait for server to process
	time.Sleep(200 * time.Millisecond)

	mouse.mu.Lock()
	received := len(mouse.moves)
	mouse.mu.Unlock()

	t.Logf("Sent %d moves in %v (%.0f msg/sec), server received %d",
		moveCount, elapsed, float64(moveCount)/elapsed.Seconds(), received)

	if received < moveCount*90/100 {
		t.Errorf("Expected at least 90%% of moves received, got %d/%d", received, moveCount)
	}
}

// ============================================================
// Test: Multiple concurrent clients
// ============================================================

func TestMultipleConcurrentClients(t *testing.T) {
	server, mouse, port := startTestServer(t)
	defer server.Stop()

	const clientCount = 3
	var wg sync.WaitGroup

	for i := 0; i < clientCount; i++ {
		wg.Add(1)
		go func(idx int) {
			defer wg.Done()
			client := dialMock(t, port)
			defer client.close()

			// Handshake
			client.sendJSON(map[string]any{
				"type": "hello",
				"payload": map[string]any{
					"name":      fmt.Sprintf("Client_%d", idx),
					"version":   "3.0",
					"device_id": fmt.Sprintf("device_%d", idx),
					"protocol":  "TCP",
					"transport": "tcp",
				},
			})
			time.Sleep(200 * time.Millisecond)

			// Approve
			server.mu.RLock()
			for id, c := range server.clients {
				c.mu.RLock()
				name := c.Name
				approved := c.Approved
				c.mu.RUnlock()
				if name == fmt.Sprintf("Client_%d", idx) && !approved {
					server.mu.RUnlock()
					_ = server.ApproveDevice(id)
					server.mu.RLock()
					break
				}
			}
			server.mu.RUnlock()

			// Read welcome
			_, _ = client.tryReadMsg(5 * time.Second)

			// Send a move
			client.sendJSON(map[string]any{
				"type": "move",
				"dx":   float64(idx + 1),
				"dy":   float64(idx + 1),
			})
			time.Sleep(100 * time.Millisecond)
		}(i)
	}

	wg.Wait()
	time.Sleep(200 * time.Millisecond)

	mouse.mu.Lock()
	moveCount := len(mouse.moves)
	mouse.mu.Unlock()

	t.Logf("Received %d moves from %d concurrent clients", moveCount, clientCount)
	if moveCount < 1 {
		t.Errorf("Expected at least 1 move from concurrent clients, got %d", moveCount)
	}
}

// ============================================================
// Test: Proximity message
// ============================================================

func TestProximityMessage(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t11","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Send proximity (as Android does — nested payload)
	client.sendJSON(map[string]any{
		"type": "proximity",
		"payload": map[string]any{
			"device_id": "t11",
			"is_near":   true,
			"distance":  1.5,
		},
	})
	time.Sleep(50 * time.Millisecond)
	// Should not crash — just verify no error
}

// ============================================================
// Test: Gesture message
// ============================================================

func TestGestureMessage(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Handshake
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t12","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Send gesture (as Android does — nested payload)
	client.sendJSON(map[string]any{
		"type": "gesture",
		"payload": map[string]any{
			"gesture":    "ThumbsUp",
			"confidence": 0.95,
		},
	})
	time.Sleep(50 * time.Millisecond)
	// Should not crash
}

// ============================================================
// Test: Invalid JSON handling
// ============================================================

func TestInvalidJSONHandling(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	// Send garbage
	client.sendRaw("not valid json at all\n")
	time.Sleep(50 * time.Millisecond)

	// Send valid hello after garbage — connection should still work
	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t13","protocol":"TCP","transport":"tcp"}}`)
	time.Sleep(100 * time.Millisecond)

	server.mu.RLock()
	clientCount := len(server.clients)
	server.mu.RUnlock()

	if clientCount < 1 {
		t.Errorf("Expected client to still be connected after invalid JSON")
	}
}

// ============================================================
// Test: Calibration data message
// ============================================================

func TestCalibrationData(t *testing.T) {
	server, _, port := startTestServer(t)
	defer server.Stop()

	client := dialMock(t, port)
	defer client.close()

	client.sendRaw(`{"type":"hello","payload":{"name":"TestDevice","version":"3.0","device_id":"t14","protocol":"TCP","transport":"tcp"}}`)
	approveFirstClient(t, server)
	_ = client.readMsg(5 * time.Second)

	// Send calibration data
	client.sendJSON(map[string]any{
		"type": "calibration_data",
		"payload": map[string]any{
			"gyro_bias":  map[string]any{"x": 0.01, "y": -0.02, "z": 0.005},
			"accel_bias": map[string]any{"x": 0.1, "y": 0.05, "z": -0.15},
		},
	})
	time.Sleep(50 * time.Millisecond)
	// Should not crash
}

// Ensure MockMouseController satisfies unused interface methods from predict package
var _ = (*predict.AISmoother)(nil)
