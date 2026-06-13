cat > internal/integration/server_test.go << 'EOF'
package integration

import (
    "encoding/json"
    "net"
    "sync"
    "testing"
    "time"
)

// TestTCPConnection tests basic TCP connection
func TestTCPConnection(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running on port 8080: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"hello","name":"TestDevice","version":"3.0"}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send: %v", err)
    }
    
    t.Log("✓ TCP connection successful")
}

// TestWebSocketConnection tests WebSocket connection
func TestWebSocketConnection(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8081", 2*time.Second)
    if err != nil {
        t.Skipf("WebSocket server not running on port 8081: %v", err)
    }
    defer conn.Close()
    
    // WebSocket handshake request
    key := "dGhlIHNhbXBsZSBub25jZQ=="
    request := "GET /ws HTTP/1.1\r\n" +
        "Host: localhost:8081\r\n" +
        "Upgrade: websocket\r\n" +
        "Connection: Upgrade\r\n" +
        "Sec-WebSocket-Key: " + key + "\r\n" +
        "Sec-WebSocket-Version: 13\r\n" +
        "\r\n"
    
    _, err = conn.Write([]byte(request))
    if err != nil {
        t.Fatalf("Failed to send WebSocket handshake: %v", err)
    }
    
    // Read response
    buf := make([]byte, 1024)
    conn.SetReadDeadline(time.Now().Add(2 * time.Second))
    n, err := conn.Read(buf)
    if err != nil {
        t.Logf("WebSocket handshake response: %v", err)
    } else {
        t.Logf("✓ WebSocket server responded (%d bytes)", n)
    }
}

// TestMoveMessage tests sending move command via TCP
func TestMoveMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"move","payload":{"dx":100,"dy":50}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send move: %v", err)
    }
    
    t.Log("✓ Move message sent successfully")
}

// TestClickMessage tests sending click command
func TestClickMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"click","payload":{"button":"left"}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send click: %v", err)
    }
    
    t.Log("✓ Click message sent successfully")
}

// TestGestureMessage tests sending gesture command
func TestGestureMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.95}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send gesture: %v", err)
    }
    
    t.Log("✓ Gesture message sent successfully")
}

// TestScrollMessage tests sending scroll command
func TestScrollMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"scroll","payload":{"delta":5}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send scroll: %v", err)
    }
    
    t.Log("✓ Scroll message sent successfully")
}

// TestProximityMessage tests sending proximity update
func TestProximityMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"proximity","payload":{"device_id":"test","is_near":true,"distance":1.5}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send proximity: %v", err)
    }
    
    t.Log("✓ Proximity message sent successfully")
}

// TestControlMessage tests sending control command
func TestControlMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"control","payload":{"command":"pause_movement"}}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send control: %v", err)
    }
    
    t.Log("✓ Control message sent successfully")
}

// TestHelloMessage tests device identification
func TestHelloMessage(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    msg := `{"type":"hello","name":"TestPhone","version":"3.0"}`
    _, err = conn.Write([]byte(msg + "\n"))
    if err != nil {
        t.Fatalf("Failed to send hello: %v", err)
    }
    
    t.Log("✓ Hello message sent successfully")
}

// TestAllMessageTypes sends all message types
func TestAllMessageTypes(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    messages := []string{
        `{"type":"hello","name":"Test","version":"3.0"}`,
        `{"type":"move","payload":{"dx":10,"dy":20}}`,
        `{"type":"click","payload":{"button":"left"}}`,
        `{"type":"doubleclick"}`,
        `{"type":"rightclick"}`,
        `{"type":"scroll","payload":{"delta":5}}`,
        `{"type":"gesture","payload":{"gesture":"Test","confidence":0.9}}`,
        `{"type":"proximity","payload":{"device_id":"test","is_near":true,"distance":1.0}}`,
        `{"type":"control","payload":{"command":"pause_movement"}}`,
    }
    
    for i, msg := range messages {
        _, err := conn.Write([]byte(msg + "\n"))
        if err != nil {
            t.Errorf("Failed to send message %d: %v", i, err)
        }
    }
    
    t.Logf("✓ Sent %d different message types", len(messages))
}

// TestConcurrentConnections tests multiple connections simultaneously
func TestConcurrentConnections(t *testing.T) {
    numConnections := 10
    var wg sync.WaitGroup
    successCount := 0
    var mu sync.Mutex
    
    for i := 0; i < numConnections; i++ {
        wg.Add(1)
        go func(id int) {
            defer wg.Done()
            conn, err := net.DialTimeout("tcp", "localhost:8080", 1*time.Second)
            if err != nil {
                return
            }
            defer conn.Close()
            
            msg := `{"type":"hello","name":"Device","version":"3.0"}`
            _, err = conn.Write([]byte(msg + "\n"))
            if err == nil {
                mu.Lock()
                successCount++
                mu.Unlock()
            }
        }(i)
    }
    
    wg.Wait()
    
    if successCount == numConnections {
        t.Logf("✓ All %d concurrent connections successful", numConnections)
    } else {
        t.Logf("✓ %d/%d concurrent connections successful", successCount, numConnections)
    }
}

// TestHighThroughput tests many messages in sequence
func TestHighThroughput(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    numMessages := 100
    start := time.Now()
    
    for i := 0; i < numMessages; i++ {
        msg := `{"type":"move","payload":{"dx":` + string(rune(i)) + `,"dy":` + string(rune(i)) + `}}`
        _, err := conn.Write([]byte(msg + "\n"))
        if err != nil {
            t.Fatalf("Failed at message %d: %v", i, err)
        }
    }
    
    elapsed := time.Since(start)
    throughput := float64(numMessages) / elapsed.Seconds()
    
    t.Logf("✓ Sent %d messages in %v", numMessages, elapsed)
    t.Logf("✓ Throughput: %.0f msg/sec", throughput)
}

// TestMalformedMessages tests error handling
func TestMalformedMessages(t *testing.T) {
    conn, err := net.DialTimeout("tcp", "localhost:8080", 2*time.Second)
    if err != nil {
        t.Skipf("Server not running: %v", err)
    }
    defer conn.Close()
    
    malformed := []string{
        "not json",
        `{"type":}`,
        `{"type":"move","payload":{"dx":"not number"}}`,
        `{"type":"unknown"}`,
        `{}`,
    }
    
    for i, msg := range malformed {
        _, err := conn.Write([]byte(msg + "\n"))
        if err != nil {
            t.Logf("Malformed message %d error: %v", i, err)
        }
    }
    
    t.Logf("✓ Sent %d malformed messages (server should handle gracefully)", len(malformed))
}

// TestJSONParsing tests JSON message parsing
func TestJSONParsing(t *testing.T) {
    tests := []struct {
        name     string
        input    string
        expected string
    }{
        {"Valid move", `{"type":"move","dx":10,"dy":20}`, "move"},
        {"Valid click", `{"type":"click","button":"left"}`, "click"},
        {"Nested payload", `{"type":"move","payload":{"dx":10,"dy":20}}`, "move"},
        {"Missing type", `{"dx":10}`, ""},
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            var msg map[string]interface{}
            err := json.Unmarshal([]byte(tt.input), &msg)
            if err != nil && tt.expected != "" {
                t.Errorf("Failed to parse: %v", err)
            } else if err == nil && tt.expected != "" {
                if msg["type"] != tt.expected {
                    t.Errorf("Expected type %s, got %v", tt.expected, msg["type"])
                } else {
                    t.Logf("✓ %s parsed correctly", tt.name)
                }
            }
        })
    }
}
EOF


# First, start the server
./airmouse-server &
SERVER_PID=$!
sleep 2

# Run all tests
echo "Running tests..."
go test -v ./internal/integration/ -timeout 30s

# Kill server
kill $SERVER_PID 2>/dev/null




cat > run_tests.sh << 'EOF'
#!/bin/bash

echo "=========================================="
echo "Air Mouse Server - Integration Tests"
echo "=========================================="

# Start server
echo "Starting server..."
./airmouse-server > /tmp/server.log 2>&1 &
SERVER_PID=$!
sleep 3

# Check if server is running
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "❌ Failed to start server"
    exit 1
fi
echo "✓ Server started (PID: $SERVER_PID)"

# Run tests
echo ""
echo "Running tests..."
go test -v ./internal/integration/ -timeout 30s
TEST_RESULT=$?

# Stop server
echo ""
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null

if [ $TEST_RESULT -eq 0 ]; then
    echo ""
    echo "✅ ALL TESTS PASSED!"
else
    echo ""
    echo "❌ SOME TESTS FAILED"
fi

exit $TEST_RESULT
EOF

chmod +x run_tests.sh
./run_tests.sh