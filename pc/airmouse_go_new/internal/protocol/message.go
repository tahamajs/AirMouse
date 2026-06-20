package protocol

import (
	"encoding/json"
	"fmt"
)

// ------------------------------------------------------------
// WireMessage - Standard message format for all protocols
// ------------------------------------------------------------

type WireMessage struct {
	Type    string           `json:"type"`
	Payload json.RawMessage  `json:"payload,omitempty"`
	ID      *json.RawMessage `json:"id,omitempty"`
}

// ------------------------------------------------------------
// MessageHandler interface
// ------------------------------------------------------------

type MessageHandler interface {
	HandleMessage(msgType string, payload map[string]interface{}, clientID string) error
}

// ------------------------------------------------------------
// Message decoding
// ------------------------------------------------------------

// DecodeWireMessage decodes a wire message from JSON bytes
func DecodeWireMessage(line []byte) (msgType string, payload map[string]any, id *string, err error) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(line, &raw); err != nil {
		return "", nil, nil, fmt.Errorf("failed to unmarshal: %w", err)
	}

	// Extract type
	if t, ok := raw["type"]; ok {
		if err := json.Unmarshal(t, &msgType); err != nil {
			return "", nil, nil, fmt.Errorf("invalid type field: %w", err)
		}
	} else {
		return "", nil, nil, fmt.Errorf("missing type field")
	}

	// Extract payload (supports both nested and flat formats)
	payload = make(map[string]any)
	if p, ok := raw["payload"]; ok {
		if err := json.Unmarshal(p, &payload); err != nil {
			return "", nil, nil, fmt.Errorf("invalid payload: %w", err)
		}
	} else {
		// Flat format: all non-type, non-id fields are payload
		for k, v := range raw {
			if k == "type" || k == "id" {
				continue
			}
			var value any
			if err := json.Unmarshal(v, &value); err != nil {
				continue
			}
			payload[k] = value
		}
	}

	// Extract ID if present
	if rawID, ok := raw["id"]; ok {
		if s, err := rawMessageToString(rawID); err == nil {
			id = &s
		}
	}

	return msgType, payload, id, nil
}

// Backward-compatible aliases used by older tests and internal call sites.
func decodeWireMessage(line []byte) (string, map[string]any, *string, error) {
	return DecodeWireMessage(line)
}

// rawMessageToString converts a json.RawMessage to string
func rawMessageToString(raw json.RawMessage) (string, error) {
	var s string
	if err := json.Unmarshal(raw, &s); err == nil {
		return s, nil
	}
	var n json.Number
	if err := json.Unmarshal(raw, &n); err == nil {
		return n.String(), nil
	}
	var i int64
	if err := json.Unmarshal(raw, &i); err == nil {
		return fmt.Sprintf("%d", i), nil
	}
	return "", fmt.Errorf("unsupported id format")
}

// ------------------------------------------------------------
// Message encoding helpers
// ------------------------------------------------------------

// AckMessage returns an ACK message for a given ID
func AckMessage(id *string) []byte {
	if id == nil || *id == "" {
		return nil
	}
	body, _ := json.Marshal(map[string]any{
		"type": "ack",
		"id":   *id,
	})
	return append(body, '\n')
}

func ackMessage(id *string) []byte {
	return AckMessage(id)
}

// ErrorMessage returns an error message
func ErrorMessage(errMsg string) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":  "error",
		"error": errMsg,
	})
	return append(body, '\n')
}

func errorMessage(errMsg string) []byte {
	return ErrorMessage(errMsg)
}

// WelcomeMessage returns a welcome message
func WelcomeMessage(serverName, version string) []byte {
	body, _ := json.Marshal(map[string]any{
		"type": "welcome",
		"payload": map[string]string{
			"server":  serverName,
			"version": version,
		},
	})
	return append(body, '\n')
}

func welcomeMessage(serverName, version string) []byte {
	return WelcomeMessage(serverName, version)
}

// PongMessage returns a pong message
func PongMessage() []byte {
	return []byte(`{"type":"pong"}` + "\n")
}

// PingMessage returns a ping message
func PingMessage() []byte {
	return []byte(`{"type":"ping"}` + "\n")
}

// ------------------------------------------------------------
// Convenience constructors for common messages
// ------------------------------------------------------------

// MoveMessage creates a movement message
func MoveMessage(dx, dy float64) []byte {
	body, _ := json.Marshal(map[string]any{
		"type": "move",
		"dx":   dx,
		"dy":   dy,
	})
	return append(body, '\n')
}

// ClickMessage creates a click message
func ClickMessage(button string) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":   "click",
		"button": button,
	})
	return append(body, '\n')
}

// DoubleClickMessage creates a double click message
func DoubleClickMessage() []byte {
	return []byte(`{"type":"doubleclick"}` + "\n")
}

// RightClickMessage creates a right click message
func RightClickMessage() []byte {
	return []byte(`{"type":"rightclick"}` + "\n")
}

// ScrollMessage creates a scroll message
func ScrollMessage(delta int) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":  "scroll",
		"delta": delta,
	})
	return append(body, '\n')
}

// GestureMessage creates a gesture message
func GestureMessage(gesture string, confidence float64) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":       "gesture",
		"gesture":    gesture,
		"confidence": confidence,
	})
	return append(body, '\n')
}

// ProximityMessage creates a proximity message
func ProximityMessage(deviceID string, isNear bool, distance float32) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":      "proximity",
		"device_id": deviceID,
		"is_near":   isNear,
		"distance":  distance,
	})
	return append(body, '\n')
}

// HelloMessage creates a hello message
func HelloMessage(name, version string) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":    "hello",
		"name":    name,
		"version": version,
	})
	return append(body, '\n')
}

// StatusMessage creates a status message
func StatusMessage(running bool, clients int) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":    "status",
		"running": running,
		"clients": clients,
	})
	return append(body, '\n')
}

// ------------------------------------------------------------
// Decoding functions for specific message types
// ------------------------------------------------------------

// MovePayload represents a movement message payload
type MovePayload struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
}

// DecodeMovePayload decodes a movement payload
func DecodeMovePayload(payload map[string]any) (dx, dy float64, err error) {
	dxVal, ok := payload["dx"]
	if !ok {
		return 0, 0, fmt.Errorf("missing dx field")
	}
	dx, ok = dxVal.(float64)
	if !ok {
		return 0, 0, fmt.Errorf("dx is not a number")
	}

	dyVal, ok := payload["dy"]
	if !ok {
		return 0, 0, fmt.Errorf("missing dy field")
	}
	dy, ok = dyVal.(float64)
	if !ok {
		return 0, 0, fmt.Errorf("dy is not a number")
	}

	return dx, dy, nil
}

// ClickPayload represents a click message payload
type ClickPayload struct {
	Button string `json:"button"`
}

// DecodeClickPayload decodes a click payload
func DecodeClickPayload(payload map[string]any) (string, error) {
	btnVal, ok := payload["button"]
	if !ok {
		return "left", nil // default to left click
	}
	btn, ok := btnVal.(string)
	if !ok {
		return "", fmt.Errorf("button is not a string")
	}
	return btn, nil
}

// ScrollPayload represents a scroll message payload
type ScrollPayload struct {
	Delta int `json:"delta"`
}

// DecodeScrollPayload decodes a scroll payload
func DecodeScrollPayload(payload map[string]any) (int, error) {
	deltaVal, ok := payload["delta"]
	if !ok {
		return 0, fmt.Errorf("missing delta field")
	}
	// Handle different number formats
	switch v := deltaVal.(type) {
	case float64:
		return int(v), nil
	case int:
		return v, nil
	case int64:
		return int(v), nil
	case json.Number:
		i, err := v.Int64()
		if err != nil {
			return 0, fmt.Errorf("invalid delta: %w", err)
		}
		return int(i), nil
	default:
		return 0, fmt.Errorf("delta is not a number")
	}
}

// HelloPayload represents a hello message payload
type HelloPayload struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

// DecodeHelloPayload decodes a hello payload
func DecodeHelloPayload(payload map[string]any) (name, version string, err error) {
	if n, ok := payload["name"]; ok {
		name, _ = n.(string)
	}
	if v, ok := payload["version"]; ok {
		version, _ = v.(string)
	}
	return name, version, nil
}

// GesturePayload represents a gesture message payload
type GesturePayload struct {
	Gesture    string  `json:"gesture"`
	Confidence float64 `json:"confidence"`
}

// DecodeGesturePayload decodes a gesture payload
func DecodeGesturePayload(payload map[string]any) (string, float64, error) {
	gesture, _ := payload["gesture"].(string)
	if gesture == "" {
		return "", 0, fmt.Errorf("missing gesture field")
	}
	confidence, _ := payload["confidence"].(float64)
	return gesture, confidence, nil
}

// ProximityPayload represents a proximity message payload
type ProximityPayload struct {
	DeviceID string  `json:"device_id"`
	IsNear   bool    `json:"is_near"`
	Distance float32 `json:"distance"`
}

// DecodeProximityPayload decodes a proximity payload
func DecodeProximityPayload(payload map[string]any) (deviceID string, isNear bool, distance float32, err error) {
	deviceID, _ = payload["device_id"].(string)
	isNear, _ = payload["is_near"].(bool)
	if d, ok := payload["distance"]; ok {
		switch v := d.(type) {
		case float64:
			distance = float32(v)
		case float32:
			distance = v
		default:
			distance = 0
		}
	}
	return deviceID, isNear, distance, nil
}

// IsValidType checks if a message type is valid
func IsValidType(msgType string) bool {
	validTypes := map[string]bool{
		"move":        true,
		"click":       true,
		"doubleclick": true,
		"rightclick":  true,
		"scroll":      true,
		"hello":       true,
		"gesture":     true,
		"proximity":   true,
		"control":     true,
		"ping":        true,
		"pong":        true,
		"ack":         true,
		"error":       true,
		"welcome":     true,
		"status":      true,
	}
	return validTypes[msgType]
}
