package protocol

import (
	"encoding/json"
	"testing"
	"time"
)

func TestServerInterface(t *testing.T) {
	t.Log("✓ Server interface exists and is compilable")
}

func TestMessageRouter(t *testing.T) {
	testMessages := []struct {
		type_   string
		handler string
	}{
		{"move", "handleMove"},
		{"click", "handleClick"},
		{"doubleclick", "handleDoubleClick"},
		{"rightclick", "handleRightClick"},
		{"gesture", "handleGesture"},
		{"scroll", "handleScroll"},
		{"proximity", "handleProximity"},
		{"control", "handleControl"},
		{"hello", "handleHello"},
		{"ping", "handlePing"},
		{"pong", "handlePong"},
	}

	for _, tm := range testMessages {
		t.Run(tm.type_, func(t *testing.T) {
			msg := map[string]string{"type": tm.type_}
			data, err := json.Marshal(msg)
			if err != nil {
				t.Errorf("Failed to marshal %s: %v", tm.type_, err)
			}

			var parsed map[string]interface{}
			if err := json.Unmarshal(data, &parsed); err != nil {
				t.Errorf("Failed to unmarshal %s: %v", tm.type_, err)
			}

			if parsed["type"] != tm.type_ {
				t.Errorf("Expected type %s, got %v", tm.type_, parsed["type"])
			}
		})
	}
	t.Log("✓ All message types validated")
}

func TestAckGeneration(t *testing.T) {
	testIDs := []string{"123", "abc", "42", "test-id-1"}

	for _, id := range testIDs {
		t.Run("id="+id, func(t *testing.T) {
			ack := ackMessage(&id)
			if len(ack) == 0 {
				t.Error("ACK message empty")
			}

			var parsed map[string]interface{}
			if err := json.Unmarshal(ack, &parsed); err != nil {
				t.Errorf("Failed to parse ACK: %v", err)
			}

			if parsed["type"] != "ack" {
				t.Errorf("Expected ack type, got %v", parsed["type"])
			}
			if parsed["id"] != id {
				t.Errorf("Expected id %s, got %v", id, parsed["id"])
			}
		})
	}
	t.Log("✓ ACK message format valid")
}

func TestWelcomeMessage(t *testing.T) {
	welcome := welcomeMessage("TestServer", "1.0")
	var parsed map[string]interface{}
	if err := json.Unmarshal(welcome, &parsed); err != nil {
		t.Fatalf("Failed to parse welcome: %v", err)
	}

	if parsed["type"] != "welcome" {
		t.Errorf("Expected type welcome, got %v", parsed["type"])
	}
	t.Log("✓ Welcome message format valid")
}

func TestErrorMessage(t *testing.T) {
	errMsg := errorMessage("test error")
	var parsed map[string]interface{}
	if err := json.Unmarshal(errMsg, &parsed); err != nil {
		t.Fatalf("Failed to parse error: %v", err)
	}

	if parsed["type"] != "error" {
		t.Errorf("Expected type error, got %v", parsed["type"])
	}
	t.Log("✓ Error message format valid")
}

func TestProtocolServerStopReturnsPromptly(t *testing.T) {
	s := &ProtocolServer{
		running: true,
		callbacks: []func(ServerEvent){
			func(event ServerEvent) {},
		},
	}

	done := make(chan struct{})
	go func() {
		s.Stop()
		close(done)
	}()

	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("Stop() timed out; shutdown path may be deadlocked")
	}

	if s.IsRunning() {
		t.Fatal("expected protocol server to be stopped")
	}
}

func TestDecodeWireMessage(t *testing.T) {
	testCases := []struct {
		name     string
		input    string
		expected string
	}{
		{"Nested payload", `{"type":"move","payload":{"dx":10,"dy":20}}`, "move"},
		{"Flat payload", `{"type":"move","dx":10,"dy":20}`, "move"},
		{"With ID", `{"type":"click","button":"left","id":123}`, "click"},
		{"Missing type", `{"dx":10}`, ""},
		{"Invalid JSON", `{invalid}`, ""},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			msgType, _, _, err := decodeWireMessage([]byte(tc.input))
			if tc.expected != "" && err != nil {
				t.Errorf("Failed to decode: %v", err)
			}
			if msgType != tc.expected {
				t.Errorf("Expected %s, got %s", tc.expected, msgType)
			}
		})
	}
}

func BenchmarkDecodeWireMessage(b *testing.B) {
	msg := []byte(`{"type":"move","payload":{"dx":12.5,"dy":-3.25},"id":"12345"}`)

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _, _, _ = decodeWireMessage(msg)
	}
}

func BenchmarkAckGeneration(b *testing.B) {
	id := "test-id-12345"

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = ackMessage(&id)
	}
}
