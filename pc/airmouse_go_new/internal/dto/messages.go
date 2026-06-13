package dto

import "encoding/json"

// Message is the base WebSocket message structure
type Message struct {
    Type    string          `json:"type"`
    Payload json.RawMessage `json:"payload,omitempty"`
    ID      string          `json:"id,omitempty"`
}

// MovePayload represents a mouse movement delta
type MovePayload struct {
    DX float64 `json:"dx"`
    DY float64 `json:"dy"`
}

// ClickPayload represents a mouse click
type ClickPayload struct {
    Button string `json:"button"` // "left", "right", "middle"
}

// ScrollPayload represents a scroll action
type ScrollPayload struct {
    Delta int `json:"delta"` // positive = up, negative = down
}

// HelloPayload is sent by the client to identify itself
type HelloPayload struct {
    Name    string `json:"name"`
    Version string `json:"version"`
    Device  string `json:"device,omitempty"`
}

// ProximityPayload contains distance data from the phone
type ProximityPayload struct {
    Distance float32 `json:"distance"`
    IsNear   bool    `json:"is_near"`
    DeviceID string  `json:"device_id"`
}

// GesturePayload contains a recognised gesture
type GesturePayload struct {
    Gesture    string  `json:"gesture"`
    Confidence float64 `json:"confidence"`
}

// AckPayload is used to acknowledge receipt of a command
type AckPayload struct {
    ID      string `json:"id"`
    Status  string `json:"status"` // "ok" or "error"
    Message string `json:"message,omitempty"`
}

// WelcomeMessage is sent after a successful hello
type WelcomeMessage struct {
    Type    string         `json:"type"`
    Payload WelcomePayload `json:"payload"`
}

type WelcomePayload struct {
    Server  string `json:"server"`
    Version string `json:"version"`
    ID      string `json:"id,omitempty"`
}

// NewWelcome creates a welcome message
func NewWelcome() *WelcomeMessage {
    return &WelcomeMessage{
        Type: "welcome",
        Payload: WelcomePayload{
            Server:  "AirMouse",
            Version: "3.0.0",
        },
    }
}

// ErrorResponse is sent when an error occurs
type ErrorResponse struct {
    Type    string       `json:"type"`
    Payload ErrorPayload `json:"payload"`
}

type ErrorPayload struct {
    Code    int    `json:"code"`
    Message string `json:"message"`
}

// NewError creates an error response
func NewError(code int, message string) *ErrorResponse {
    return &ErrorResponse{
        Type: "error",
        Payload: ErrorPayload{
            Code:    code,
            Message: message,
        },
    }
}

// PongMessage is sent in response to ping
type PongMessage struct {
    Type string `json:"type"`
}

func NewPong() *PongMessage {
    return &PongMessage{Type: "pong"}
}

// StatsMessage contains server statistics
type StatsMessage struct {
    Type    string      `json:"type"`
    Payload StatsPayload `json:"payload"`
}

type StatsPayload struct {
    Clients   int     `json:"clients"`
    Uptime    float64 `json:"uptime"`
    Version   string  `json:"version"`
}