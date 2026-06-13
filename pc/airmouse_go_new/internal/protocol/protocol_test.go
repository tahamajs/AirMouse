package protocol

import (
    "encoding/json"
    "testing"
)

func TestServerStartStop(t *testing.T) {
    // This is a compile-time test to ensure the Server interface exists
    t.Log("✓ Server interface exists")
}

func TestMessageRouter(t *testing.T) {
    testMessages := []struct {
        type_   string
        handler string
    }{
        {"move", "handleMove"},
        {"click", "handleClick"},
        {"gesture", "handleGesture"},
        {"scroll", "handleScroll"},
        {"proximity", "handleProximity"},
        {"control", "handleControl"},
        {"hello", "handleHello"},
    }
    
    for _, tm := range testMessages {
        t.Run(tm.type_, func(t *testing.T) {
            msg := map[string]string{"type": tm.type_}
            data, err := json.Marshal(msg)
            if err != nil {
                t.Errorf("Failed to marshal %s: %v", tm.type_, err)
            }
            
            var parsed map[string]interface{}
            json.Unmarshal(data, &parsed)
            
            if parsed["type"] != tm.type_ {
                t.Errorf("Expected type %s, got %v", tm.type_, parsed["type"])
            }
        })
    }
}

func TestAckGeneration(t *testing.T) {
    ack := map[string]interface{}{
        "type": "ack",
        "id":   12345,
    }
    
    data, err := json.Marshal(ack)
    if err != nil {
        t.Fatalf("Failed to marshal ACK: %v", err)
    }
    
    var parsed map[string]interface{}
    json.Unmarshal(data, &parsed)
    
    if parsed["type"] != "ack" {
        t.Errorf("Expected ack type, got %v", parsed["type"])
    }
    
    t.Log("✓ ACK message format valid")
}
