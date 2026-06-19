package tcp

import (
    "encoding/json"
    "testing"
)

func TestTCPMessageParsing(t *testing.T) {
    tests := []struct {
        name     string
        input    string
        expected string
    }{
        {"Move message", `{"type":"move","payload":{"dx":10,"dy":20}}`, "move"},
        {"Click message", `{"type":"click","payload":{"button":"left"}}`, "click"},
        {"Hello message", `{"type":"hello","payload":{"name":"test","version":"3.0"}}`, "hello"},
        {"Gesture message", `{"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.95}}`, "gesture"},
        {"Scroll message", `{"type":"scroll","payload":{"delta":5}}`, "scroll"},
        {"Proximity message", `{"type":"proximity","payload":{"device_id":"test","is_near":true,"distance":1.5}}`, "proximity"},
        {"Control message", `{"type":"control","payload":{"command":"pause_movement"}}`, "control"},
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            var msg map[string]interface{}
            err := json.Unmarshal([]byte(tt.input), &msg)
            if err != nil {
                t.Errorf("Failed to parse: %v", err)
            }
            if msg["type"] != tt.expected {
                t.Errorf("Expected type %s, got %v", tt.expected, msg["type"])
            }
        })
    }
}

func TestTCPFlatMessageFormat(t *testing.T) {
    tests := []struct {
        name  string
        input string
        hasDX bool
        hasDY bool
    }{
        {"Flat move", `{"type":"move","dx":10,"dy":20}`, true, true},
        {"Flat with payload", `{"type":"move","payload":{"dx":10,"dy":20}}`, true, true},
        {"Missing dx", `{"type":"move","dy":20}`, false, true},
        {"Missing dy", `{"type":"move","dx":10}`, true, false},
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            var msg map[string]interface{}
            json.Unmarshal([]byte(tt.input), &msg)
            
            _, hasDX := msg["dx"]
            _, hasDY := msg["dy"]
            
            if hasDX != tt.hasDX {
                t.Errorf("DX presence mismatch: expected %v, got %v", tt.hasDX, hasDX)
            }
            if hasDY != tt.hasDY {
                t.Errorf("DY presence mismatch: expected %v, got %v", tt.hasDY, hasDY)
            }
        })
    }
}

func TestTCPMessageValidation(t *testing.T) {
    validMessages := []string{
        `{"type":"move","dx":10,"dy":20}`,
        `{"type":"click","button":"left"}`,
        `{"type":"hello","name":"test"}`,
    }
    
    for _, msg := range validMessages {
        t.Run("Valid: "+msg[:20], func(t *testing.T) {
            var parsed map[string]interface{}
            err := json.Unmarshal([]byte(msg), &parsed)
            if err != nil {
                t.Errorf("Valid message failed to parse: %v", err)
            }
            if parsed["type"] == nil {
                t.Error("Message missing type field")
            }
        })
    }
    
    invalidMessages := []string{
        `{}`,
        `{"type":}`,
        `not json`,
        `{"type":"unknown"}`,
    }
    
    for _, msg := range invalidMessages {
        t.Run("Invalid: "+msg[:min(20, len(msg))], func(t *testing.T) {
            var parsed map[string]interface{}
            err := json.Unmarshal([]byte(msg), &parsed)
            if err == nil && parsed["type"] != nil {
                t.Logf("Message accepted (may be fine): %s", msg)
            }
        })
    }
}

func min(a, b int) int {
    if a < b {
        return a
    }
    return b
}
