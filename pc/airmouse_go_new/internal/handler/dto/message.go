package dto

import "encoding/json"

// Message is the base WebSocket message structure.
type Message struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
	ID      string          `json:"id,omitempty"`
}

// MovePayload represents a mouse movement delta.
type MovePayload struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
}

// ClickPayload represents a mouse click.
type ClickPayload struct {
	Button string `json:"button"` // "left" or "right"
}

// ScrollPayload represents a scroll action.
type ScrollPayload struct {
	Delta int `json:"delta"`
}

// HelloPayload is sent by the client to identify itself.
type HelloPayload struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

// ProximityPayload contains distance data from the phone.
type ProximityPayload struct {
	Distance float32 `json:"distance"`
	IsNear   bool    `json:"is_near"`
	DeviceID string  `json:"device_id"`
}

// GesturePayload contains a recognised gesture.
type GesturePayload struct {
	Gesture    string  `json:"gesture"`
	Confidence float64 `json:"confidence"`
}

// AckPayload is used to acknowledge receipt of a command.
type AckPayload struct {
	ID      string `json:"id"`
	Status  string `json:"status"` // "ok" or "error"
	Message string `json:"message,omitempty"`
}
