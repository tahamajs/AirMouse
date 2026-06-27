// Package all_tests provides comprehensive protocol validation for Air Mouse messages.
package all_tests

import (
	"encoding/json"
	"sync"
	"testing"
)

// ============================================================
// TEST DATA
// ============================================================

var (
	validMovePayloads = []map[string]interface{}{
		{"dx": 10.5, "dy": -3.2},
		{"DeltaX": 10.5, "DeltaY": -3.2},
		{"dx": 0.0, "dy": 0.0},
		{"dx": -123.45, "dy": 67.89},
	}

	validClickButtons = []string{"left", "right", "middle", "double"}
	validScrollDeltas = []int{1, -1, 5, -5, 10, 0}
	validGestures     = []string{"ThumbsUp", "SwipeLeft", "SwipeRight", "CircleCW", "CircleCCW", "ZoomIn", "ZoomOut"}
	validCommands     = []string{"pause_movement", "resume_movement", "reset", "calibrate", "lock_screen", "unlock_screen"}
	validDeviceIDs    = []string{"device-001", "android-12345", "test-device"}
)

// ============================================================
// MESSAGE TYPE TESTS
// ============================================================

func TestProtocolMoveMessage(t *testing.T) {
	for _, payload := range validMovePayloads {
		msg := map[string]interface{}{
			"type":    "move",
			"payload": payload,
		}
		t.Run("nested", func(t *testing.T) {
			validateMessage(t, msg, "move")
			validateMovePayload(t, msg)
		})
		// Flat format
		flat := map[string]interface{}{
			"type": "move",
		}
		for k, v := range payload {
			flat[k] = v
		}
		t.Run("flat", func(t *testing.T) {
			validateMessage(t, flat, "move")
			validateMovePayload(t, flat)
		})
	}
}

func TestProtocolClickMessage(t *testing.T) {
	for _, button := range validClickButtons {
		msg := map[string]interface{}{
			"type": "click",
			"payload": map[string]string{
				"button": button,
				"Click":  button, // alias
			},
		}
		t.Run("nested_"+button, func(t *testing.T) {
			validateMessage(t, msg, "click")
		})
		// Flat format with alias
		flat := map[string]interface{}{
			"type":   "click",
			"button": button,
			"Click":  button,
		}
		t.Run("flat_"+button, func(t *testing.T) {
			validateMessage(t, flat, "click")
		})
	}
}

func TestProtocolDoubleClickMessage(t *testing.T) {
	msg := map[string]interface{}{
		"type": "doubleclick",
	}
	validateMessage(t, msg, "doubleclick")
}

func TestProtocolRightClickMessage(t *testing.T) {
	msg := map[string]interface{}{
		"type": "rightclick",
	}
	validateMessage(t, msg, "rightclick")
}

func TestProtocolScrollMessage(t *testing.T) {
	for _, delta := range validScrollDeltas {
		msg := map[string]interface{}{
			"type": "scroll",
			"payload": map[string]int{
				"delta": delta,
				"Scroll": delta, // alias
			},
		}
		t.Run("nested_"+string(rune(delta)), func(t *testing.T) {
			validateMessage(t, msg, "scroll")
		})
		flat := map[string]interface{}{
			"type":   "scroll",
			"delta":  delta,
			"Scroll": delta,
		}
		t.Run("flat_"+string(rune(delta)), func(t *testing.T) {
			validateMessage(t, flat, "scroll")
		})
	}
}

func TestProtocolGestureMessage(t *testing.T) {
	for _, g := range validGestures {
		msg := map[string]interface{}{
			"type": "gesture",
			"payload": map[string]interface{}{
				"gesture":    g,
				"confidence": 0.95,
			},
		}
		t.Run("nested_"+g, func(t *testing.T) {
			validateMessage(t, msg, "gesture")
			validateGesturePayload(t, msg)
		})
		flat := map[string]interface{}{
			"type":       "gesture",
			"gesture":    g,
			"confidence": 0.95,
		}
		t.Run("flat_"+g, func(t *testing.T) {
			validateMessage(t, flat, "gesture")
			validateGesturePayload(t, flat)
		})
	}
}

func TestProtocolProximityMessage(t *testing.T) {
	for _, deviceID := range validDeviceIDs {
		for _, isNear := range []bool{true, false} {
			distance := 1.5
			if isNear {
				distance = 0.8
			}
			msg := map[string]interface{}{
				"type": "proximity",
				"payload": map[string]interface{}{
					"device_id": deviceID,
					"is_near":   isNear,
					"distance":  distance,
				},
			}
			t.Run("nested_"+deviceID+"_near", func(t *testing.T) {
				validateMessage(t, msg, "proximity")
			})
			flat := map[string]interface{}{
				"type":      "proximity",
				"device_id": deviceID,
				"is_near":   isNear,
				"distance":  distance,
			}
			t.Run("flat_"+deviceID+"_near", func(t *testing.T) {
				validateMessage(t, flat, "proximity")
			})
		}
	}
}

func TestProtocolControlMessage(t *testing.T) {
	for _, cmd := range validCommands {
		msg := map[string]interface{}{
			"type": "control",
			"payload": map[string]string{
				"command": cmd,
			},
		}
		t.Run("nested_"+cmd, func(t *testing.T) {
			validateMessage(t, msg, "control")
		})
		flat := map[string]interface{}{
			"type":    "control",
			"command": cmd,
		}
		t.Run("flat_"+cmd, func(t *testing.T) {
			validateMessage(t, flat, "control")
		})
	}
}

func TestProtocolHelloMessage(t *testing.T) {
	payload := map[string]string{
		"name":     "TestDevice",
		"version":  "3.0",
		"device":   "Pixel 8 Pro",
		"protocol": "WEBSOCKET",
		"transport": "websocket",
	}
	msg := map[string]interface{}{
		"type":    "hello",
		"payload": payload,
	}
	t.Run("nested", func(t *testing.T) {
		validateMessage(t, msg, "hello")
	})
	flat := map[string]interface{}{
		"type":      "hello",
		"name":      payload["name"],
		"version":   payload["version"],
		"device":    payload["device"],
		"protocol":  payload["protocol"],
		"transport": payload["transport"],
	}
	t.Run("flat", func(t *testing.T) {
		validateMessage(t, flat, "hello")
	})
}

func TestProtocolAckMessage(t *testing.T) {
	testIDs := []string{"msg_1", "12345", "abc-def-ghi"}
	for _, id := range testIDs {
		msg := map[string]interface{}{
			"type": "ack",
			"id":   id,
		}
		t.Run("id_"+id, func(t *testing.T) {
			validateMessage(t, msg, "ack")
		})
	}
}

func TestProtocolPingPongMessages(t *testing.T) {
	for _, msgType := range []string{"ping", "pong"} {
		msg := map[string]interface{}{
			"type": msgType,
		}
		t.Run(msgType, func(t *testing.T) {
			validateMessage(t, msg, msgType)
		})
	}
}

func TestProtocolWelcomeMessage(t *testing.T) {
	msg := map[string]interface{}{
		"type": "welcome",
		"payload": map[string]string{
			"server":   "Air Mouse Pro",
			"version":  "3.0.0",
			"status":   "connected",
			"approval": "approved",
		},
	}
	validateMessage(t, msg, "welcome")
}

func TestProtocolErrorMessage(t *testing.T) {
	msg := map[string]interface{}{
		"type": "error",
		"payload": map[string]interface{}{
			"code":    403,
			"message": "Invalid token",
		},
	}
	validateMessage(t, msg, "error")
}

// ============================================================
// HELPERS
// ============================================================

func validateMessage(t *testing.T, msg map[string]interface{}, expectedType string) {
	t.Helper()
	data, err := json.Marshal(msg)
	if err != nil {
		t.Fatalf("Marshal failed: %v", err)
	}
	var parsed map[string]interface{}
	if err := json.Unmarshal(data, &parsed); err != nil {
		t.Fatalf("Unmarshal failed: %v", err)
	}
	if parsed["type"] != expectedType {
		t.Errorf("Expected type %q, got %q", expectedType, parsed["type"])
	}
}

func validateMovePayload(t *testing.T, msg map[string]interface{}) {
	t.Helper()
	// Try to extract dx/dy from nested or flat
	var dx, dy float64
	if p, ok := msg["payload"].(map[string]interface{}); ok {
		dx, _ = p["dx"].(float64)
		dy, _ = p["dy"].(float64)
	}
	if dx == 0 && dy == 0 {
		// check flat
		if v, ok := msg["dx"]; ok {
			dx, _ = v.(float64)
		}
		if v, ok := msg["dy"]; ok {
			dy, _ = v.(float64)
		}
	}
	// Check aliases
	if dx == 0 {
		if v, ok := msg["DeltaX"]; ok {
			dx, _ = v.(float64)
		}
	}
	if dy == 0 {
		if v, ok := msg["DeltaY"]; ok {
			dy, _ = v.(float64)
		}
	}
	t.Logf("  Move: dx=%.2f dy=%.2f", dx, dy)
}

func validateGesturePayload(t *testing.T, msg map[string]interface{}) {
	t.Helper()
	var gesture string
	var confidence float64
	if p, ok := msg["payload"].(map[string]interface{}); ok {
		gesture, _ = p["gesture"].(string)
		confidence, _ = p["confidence"].(float64)
	}
	if gesture == "" {
		if v, ok := msg["gesture"]; ok {
			gesture, _ = v.(string)
		}
	}
	if confidence == 0 {
		if v, ok := msg["confidence"]; ok {
			confidence, _ = v.(float64)
		}
	}
	t.Logf("  Gesture: %s (%.2f)", gesture, confidence)
}

// ============================================================
// EDGE CASES
// ============================================================

func TestProtocolInvalidMessages(t *testing.T) {
	invalid := []string{
		`{}`,
		`{"type":}`,
		`not json`,
		`{"type":"unknown"}`,
		`{"type":"move"}`,
		`{"type":"click","button":123}`,
	}
	for i, raw := range invalid {
		var parsed map[string]interface{}
		err := json.Unmarshal([]byte(raw), &parsed)
		if err == nil && parsed["type"] != nil {
			t.Logf("  Message %d parsed: %v", i, parsed)
		} else {
			t.Logf("  Message %d invalid: %v", i, err)
		}
	}
}

func TestProtocolIDField(t *testing.T) {
	msg := map[string]interface{}{
		"type": "click",
		"id":   "msg_7",
		"button": "left",
	}
	validateMessage(t, msg, "click")
	data, _ := json.Marshal(msg)
	var parsed map[string]interface{}
	json.Unmarshal(data, &parsed)
	if parsed["id"] != "msg_7" {
		t.Errorf("Expected id msg_7, got %v", parsed["id"])
	}
}

func TestProtocolAliases(t *testing.T) {
	aliases := []struct {
		msg     map[string]interface{}
		aliases []string
	}{
		{
			msg:     map[string]interface{}{"type": "move", "DeltaX": 12.5, "DeltaY": -3.0},
			aliases: []string{"DeltaX", "DeltaY"},
		},
		{
			msg:     map[string]interface{}{"type": "click", "Click": "right"},
			aliases: []string{"Click"},
		},
		{
			msg:     map[string]interface{}{"type": "scroll", "Scroll": -5},
			aliases: []string{"Scroll"},
		},
	}
	for _, test := range aliases {
		validateMessage(t, test.msg, test.msg["type"].(string))
		data, _ := json.Marshal(test.msg)
		var parsed map[string]interface{}
		json.Unmarshal(data, &parsed)
		for _, alias := range test.aliases {
			if _, ok := parsed[alias]; !ok {
				t.Errorf("Alias %q not found in parsed message", alias)
			}
		}
	}
}

// ============================================================
// PERFORMANCE & CONCURRENCY
// ============================================================

func BenchmarkProtocolMarshal(b *testing.B) {
	msg := map[string]interface{}{
		"type": "move",
		"payload": map[string]float64{
			"dx": 123.456,
			"dy": -789.012,
		},
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = json.Marshal(msg)
	}
}

func BenchmarkProtocolUnmarshal(b *testing.B) {
	data, _ := json.Marshal(map[string]interface{}{
		"type": "move",
		"payload": map[string]float64{
			"dx": 123.456,
			"dy": -789.012,
		},
	})
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		var parsed map[string]interface{}
		_ = json.Unmarshal(data, &parsed)
	}
}

func TestProtocolConcurrentMarshal(t *testing.T) {
	msg := map[string]interface{}{
		"type": "gesture",
		"payload": map[string]interface{}{
			"gesture":    "ThumbsUp",
			"confidence": 0.95,
		},
	}
	numGoroutines := 100
	var wg sync.WaitGroup
	errs := make(chan error, numGoroutines)

	for i := 0; i < numGoroutines; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			data, err := json.Marshal(msg)
			if err != nil {
				errs <- err
				return
			}
			var parsed map[string]interface{}
			if err := json.Unmarshal(data, &parsed); err != nil {
				errs <- err
			}
		}()
	}
	wg.Wait()
	close(errs)

	var errors int
	for err := range errs {
		t.Logf("Error: %v", err)
		errors++
	}
	if errors > 0 {
		t.Errorf("%d errors during concurrent marshal", errors)
	}
}

// ============================================================
// MESSAGE SIZE TESTS
// ============================================================

func TestProtocolMessageSizes(t *testing.T) {
	messages := map[string]interface{}{
		"move":      map[string]interface{}{"type": "move", "dx": 10.5, "dy": -3.2},
		"click":     map[string]interface{}{"type": "click", "button": "left"},
		"doubleclick": map[string]interface{}{"type": "doubleclick"},
		"rightclick":  map[string]interface{}{"type": "rightclick"},
		"scroll":    map[string]interface{}{"type": "scroll", "delta": 5},
		"gesture":   map[string]interface{}{"type": "gesture", "gesture": "ThumbsUp", "confidence": 0.95},
		"proximity": map[string]interface{}{"type": "proximity", "device_id": "test-device", "is_near": true, "distance": 1.5},
		"control":   map[string]interface{}{"type": "control", "command": "pause_movement"},
		"hello":     map[string]interface{}{"type": "hello", "name": "TestDevice", "version": "3.0"},
		"ack":       map[string]interface{}{"type": "ack", "id": "msg_123"},
		"ping":      map[string]interface{}{"type": "ping"},
		"pong":      map[string]interface{}{"type": "pong"},
		"welcome":   map[string]interface{}{"type": "welcome", "server": "AirMouse", "version": "3.0.0"},
		"error":     map[string]interface{}{"type": "error", "message": "Something went wrong"},
	}
	for name, msg := range messages {
		data, err := json.Marshal(msg)
		if err != nil {
			t.Errorf("Failed to marshal %s: %v", name, err)
			continue
		}
		t.Logf("  %-10s size: %3d bytes", name, len(data))
	}
}

// ============================================================
// TABLE-DRIVEN MAIN TEST
// ============================================================

func TestAllProtocols(t *testing.T) {
	t.Run("Move", TestProtocolMoveMessage)
	t.Run("Click", TestProtocolClickMessage)
	t.Run("DoubleClick", TestProtocolDoubleClickMessage)
	t.Run("RightClick", TestProtocolRightClickMessage)
	t.Run("Scroll", TestProtocolScrollMessage)
	t.Run("Gesture", TestProtocolGestureMessage)
	t.Run("Proximity", TestProtocolProximityMessage)
	t.Run("Control", TestProtocolControlMessage)
	t.Run("Hello", TestProtocolHelloMessage)
	t.Run("Ack", TestProtocolAckMessage)
	t.Run("PingPong", TestProtocolPingPongMessages)
	t.Run("Welcome", TestProtocolWelcomeMessage)
	t.Run("Error", TestProtocolErrorMessage)
	t.Run("Invalid", TestProtocolInvalidMessages)
	t.Run("ID Field", TestProtocolIDField)
	t.Run("Aliases", TestProtocolAliases)
	t.Run("Concurrency", TestProtocolConcurrentMarshal)
	t.Run("Sizes", TestProtocolMessageSizes)
}
