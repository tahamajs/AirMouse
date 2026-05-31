package dto

// WelcomeMessage is sent after a successful hello.
type WelcomeMessage struct {
	Type    string          `json:"type"`
	Payload WelcomePayload  `json:"payload"`
}

type WelcomePayload struct {
	Server  string `json:"server"`
	Version string `json:"version"`
}

// NewWelcome creates a welcome message.
func NewWelcome() *WelcomeMessage {
	return &WelcomeMessage{
		Type: "welcome",
		Payload: WelcomePayload{
			Server:  "AirMouse",
			Version: "3.0.0",
		},
	}
}

// ErrorResponse is sent when an error occurs.
type ErrorResponse struct {
	Type    string      `json:"type"`
	Payload ErrorPayload `json:"payload"`
}

type ErrorPayload struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

// NewError creates an error response.
func NewError(code int, message string) *ErrorResponse {
	return &ErrorResponse{
		Type: "error",
		Payload: ErrorPayload{
			Code:    code,
			Message: message,
		},
	}
}