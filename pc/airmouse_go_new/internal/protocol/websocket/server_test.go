package websocket

import (
    "encoding/json"
    "testing"
)

func TestWebSocketMessageTypes(t *testing.T) {
    messageTypes := []string{"move", "click", "doubleclick", "rightclick", "scroll", "gesture", "proximity", "control", "hello"}
    
    for _, msgType := range messageTypes {
        t.Run(msgType, func(t *testing.T) {
            msg := map[string]string{"type": msgType}
            data, err := json.Marshal(msg)
            if err != nil {
                t.Errorf("Failed to marshal %s: %v", msgType, err)
            }
            
            var parsed map[string]interface{}
            if err := json.Unmarshal(data, &parsed); err != nil {
                t.Errorf("Failed to unmarshal %s: %v", msgType, err)
            }
            
            if parsed["type"] != msgType {
                t.Errorf("Expected type %s, got %v", msgType, parsed["type"])
            }
        })
    }
}

func TestWebSocketGestureMessages(t *testing.T) {
    gestures := []struct {
        name       string
        confidence float64
    }{
        {"ThumbsUp", 0.95},
        {"SwipeLeft", 0.88},
        {"SwipeRight", 0.92},
        {"CircleCW", 0.85},
        {"ZoomIn", 0.91},
        {"ZoomOut", 0.89},
        {"Tap", 0.94},
        {"DoubleTap", 0.87},
        {"LongPress", 0.83},
    }
    
    for _, g := range gestures {
        t.Run(g.name, func(t *testing.T) {
            msg := map[string]interface{}{
                "type": "gesture",
                "payload": map[string]interface{}{
                    "gesture":    g.name,
                    "confidence": g.confidence,
                },
            }
            
            data, err := json.Marshal(msg)
            if err != nil {
                t.Errorf("Failed to marshal gesture %s: %v", g.name, err)
            }
            
            var parsed map[string]interface{}
            json.Unmarshal(data, &parsed)
            
            if parsed["type"] != "gesture" {
                t.Errorf("Expected gesture type, got %v", parsed["type"])
            }
        })
    }
}

func TestWebSocketProximityMessages(t *testing.T) {
    testCases := []struct {
        isNear   bool
        distance float64
    }{
        {true, 0.5},
        {true, 1.2},
        {false, 2.5},
        {false, 4.0},
        {true, 0.8},
        {false, 10.0},
    }
    
    for _, tc := range testCases {
        label := "false"
        if tc.isNear {
            label = "true"
        }
        t.Run("near="+label, func(t *testing.T) {
            msg := map[string]interface{}{
                "type": "proximity",
                "payload": map[string]interface{}{
                    "device_id": "test-device",
                    "is_near":   tc.isNear,
                    "distance":  tc.distance,
                },
            }
            
            data, err := json.Marshal(msg)
            if err != nil {
                t.Errorf("Failed to marshal proximity: %v", err)
            }
            
            var parsed map[string]interface{}
            json.Unmarshal(data, &parsed)
            
            if parsed["type"] != "proximity" {
                t.Errorf("Expected proximity type, got %v", parsed["type"])
            }
        })
    }
}

func TestWebSocketControlCommands(t *testing.T) {
    commands := []string{"pause_movement", "resume_movement", "reset", "calibrate", "shutdown"}
    
    for _, cmd := range commands {
        t.Run(cmd, func(t *testing.T) {
            msg := map[string]interface{}{
                "type": "control",
                "payload": map[string]string{
                    "command": cmd,
                },
            }
            
            data, err := json.Marshal(msg)
            if err != nil {
                t.Errorf("Failed to marshal command %s: %v", cmd, err)
            }
            
            var parsed map[string]interface{}
            json.Unmarshal(data, &parsed)
            
            if parsed["type"] != "control" {
                t.Errorf("Expected control type, got %v", parsed["type"])
            }
        })
    }
}
