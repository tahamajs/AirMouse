package protocol

import (
	"bytes"
	"encoding/json"
	"fmt"
	"math"
	"sync"
	"time"
)

// ------------------------------------------------------------
// Protocol V2 Envelope & Constants
// ------------------------------------------------------------

// ProtocolVersion is the canonical protocol version (V2).
const ProtocolVersion = 2

// ReplayWindowMs is the sliding window size in milliseconds (+/-5000ms).
const ReplayWindowMs int64 = 5000

// MaxCoordinateLimit is the maximum absolute coordinate value allowed for move (|dx|, |dy| <= 500).
const MaxCoordinateLimit float64 = 500.0

// MaxScrollLimit is the maximum absolute scroll delta allowed (|delta| <= 10).
const MaxScrollLimit float64 = 10.0

// EnvelopeV2 represents the standard frozen Protocol V2 wire envelope.
type EnvelopeV2 struct {
	V       int             `json:"v"`
	Type    string          `json:"type"`
	Seq     uint64          `json:"seq"`
	Ts      int64           `json:"ts"`
	ID      *string         `json:"id,omitempty"`
	Payload json.RawMessage `json:"payload"`
}

// ------------------------------------------------------------
// WireMessage - Legacy message format (backward compatibility)
// ------------------------------------------------------------

type WireMessage struct {
	Type    string           `json:"type"`
	Payload json.RawMessage  `json:"payload,omitempty"`
	ID      *json.RawMessage `json:"id,omitempty"`
}

// MessageHandler interface
type MessageHandler interface {
	HandleMessage(msgType string, payload map[string]interface{}, clientID string) error
}

// ------------------------------------------------------------
// Strict V2 Decoding with DisallowUnknownFields
// ------------------------------------------------------------

// DecodeWireMessageStrictV2 decodes and validates a Protocol V2 message with DisallowUnknownFields.
func DecodeWireMessageStrictV2(line []byte) (*EnvelopeV2, error) {
	dec := json.NewDecoder(bytes.NewReader(line))
	dec.DisallowUnknownFields()

	var env EnvelopeV2
	if err := dec.Decode(&env); err != nil {
		return nil, fmt.Errorf("invalid envelope: %w", err)
	}

	if dec.More() {
		return nil, fmt.Errorf("invalid envelope: trailing data after JSON message")
	}

	if env.V != ProtocolVersion {
		return nil, fmt.Errorf("protocol_v1_deprecated: invalid protocol version %d, expected %d", env.V, ProtocolVersion)
	}

	if env.Type == "" {
		return nil, fmt.Errorf("missing type field")
	}

	if !IsValidType(env.Type) {
		return nil, fmt.Errorf("unsupported message type: %s", env.Type)
	}

	trimmed := bytes.TrimSpace(env.Payload)
	if len(trimmed) == 0 || trimmed[0] != '{' {
		return nil, fmt.Errorf("invalid payload: payload must be a non-empty JSON object")
	}

	return &env, nil
}

// DecodeWireMessage decodes a wire message from JSON bytes (supporting both V2 and legacy envelope).
func DecodeWireMessage(line []byte) (msgType string, payload map[string]any, id *string, err error) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(line, &raw); err != nil {
		return "", nil, nil, fmt.Errorf("failed to unmarshal: %w", err)
	}

	// Check if this has a version tag
	if vRaw, ok := raw["v"]; ok {
		var ver int
		if err := json.Unmarshal(vRaw, &ver); err == nil && ver != ProtocolVersion {
			return "", nil, nil, fmt.Errorf("protocol_v1_deprecated: protocol version %d is not supported", ver)
		}
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
		// Flat format: all non-envelope fields are payload
		for k, v := range raw {
			if k == "type" || k == "id" || k == "v" || k == "seq" || k == "ts" {
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

// Backward-compatible alias
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
// Replay Protection
// ------------------------------------------------------------

// ValidateTimestamp checks if the timestamp in milliseconds is within +/-5000ms of nowMs.
func ValidateTimestamp(ts int64, nowMs int64) error {
	if ts <= 0 {
		return fmt.Errorf("replay_rejected: invalid non-positive timestamp %d", ts)
	}
	diff := nowMs - ts
	if diff < 0 {
		diff = -diff
	}
	if diff < 0 || diff > ReplayWindowMs {
		return fmt.Errorf("replay_rejected: timestamp delta %d ms exceeds sliding window +/- %d ms (ts=%d, now=%d)", diff, ReplayWindowMs, ts, nowMs)
	}
	return nil
}

// ReplayDetector enforces strictly monotonic sequence numbers and sliding timestamp windows.
type ReplayDetector struct {
	mu          sync.Mutex
	lastSeq     uint64
	initialized bool
	windowMs    int64
}

// NewReplayDetector creates a new ReplayDetector instance.
func NewReplayDetector() *ReplayDetector {
	return &ReplayDetector{
		windowMs: ReplayWindowMs,
	}
}

// CheckAndAdvanceSeq ensures seq is strictly greater than lastSeq.
func (rd *ReplayDetector) CheckAndAdvanceSeq(seq uint64) error {
	rd.mu.Lock()
	defer rd.mu.Unlock()

	if !rd.initialized {
		rd.lastSeq = seq
		rd.initialized = true
		return nil
	}

	if seq <= rd.lastSeq {
		return fmt.Errorf("replay_detected: sequence %d is not strictly greater than %d", seq, rd.lastSeq)
	}
	rd.lastSeq = seq
	return nil
}

// ValidateMessage checks both timestamp sliding window and strictly monotonic sequence.
func (rd *ReplayDetector) ValidateMessage(env *EnvelopeV2, nowMs int64) error {
	if err := ValidateTimestamp(env.Ts, nowMs); err != nil {
		return err
	}
	return rd.CheckAndAdvanceSeq(env.Seq)
}

// Reset resets the detector sequence tracking.
func (rd *ReplayDetector) Reset() {
	rd.mu.Lock()
	defer rd.mu.Unlock()
	rd.lastSeq = 0
	rd.initialized = false
}

// LastSeq returns the highest valid sequence number observed.
func (rd *ReplayDetector) LastSeq() uint64 {
	rd.mu.Lock()
	defer rd.mu.Unlock()
	return rd.lastSeq
}

// ------------------------------------------------------------
// Typed Payloads with Bounds Checking & DisallowUnknownFields
// ------------------------------------------------------------

// MovePayloadV2 represents a movement message payload with strict schema and bounds validation.
type MovePayloadV2 struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
	DT float64 `json:"dt,omitempty"`
}

// DecodeMovePayloadV2 decodes and validates a movement payload enforcing |dx|, |dy| <= 500 and no NaN/Inf.
func DecodeMovePayloadV2(raw json.RawMessage) (*MovePayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p MovePayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid move payload: %w", err)
	}
	if math.IsNaN(p.DX) || math.IsInf(p.DX, 0) || math.IsNaN(p.DY) || math.IsInf(p.DY, 0) {
		return nil, fmt.Errorf("move coordinates cannot be NaN or Inf")
	}
	if math.Abs(p.DX) > MaxCoordinateLimit || math.Abs(p.DY) > MaxCoordinateLimit {
		return nil, fmt.Errorf("move coordinates out of bounds (|dx|, |dy| <= %.1f): dx=%.2f, dy=%.2f", MaxCoordinateLimit, p.DX, p.DY)
	}
	if math.IsNaN(p.DT) || math.IsInf(p.DT, 0) || p.DT < 0 {
		return nil, fmt.Errorf("invalid dt: dt must be non-negative finite number")
	}
	return &p, nil
}

// ClickPayloadV2 represents a click message payload.
type ClickPayloadV2 struct {
	Button string `json:"button"`
}

// DecodeClickPayloadV2 decodes click payload with button validation.
func DecodeClickPayloadV2(raw json.RawMessage) (*ClickPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p ClickPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid click payload: %w", err)
	}
	switch p.Button {
	case "left", "right", "middle", "double":
		return &p, nil
	default:
		return nil, fmt.Errorf("invalid click button %q (allowed: left, right, middle, double)", p.Button)
	}
}

// ScrollPayloadV2 represents a scroll message payload with bounds check (|delta| <= 10.0).
type ScrollPayloadV2 struct {
	Delta float64 `json:"delta"`
}

// DecodeScrollPayloadV2 decodes scroll payload with numeric limits.
func DecodeScrollPayloadV2(raw json.RawMessage) (*ScrollPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p ScrollPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid scroll payload: %w", err)
	}
	if math.IsNaN(p.Delta) || math.IsInf(p.Delta, 0) {
		return nil, fmt.Errorf("scroll delta cannot be NaN or Inf")
	}
	if math.Abs(p.Delta) > MaxScrollLimit {
		return nil, fmt.Errorf("scroll delta out of bounds (|delta| <= %.1f): %.2f", MaxScrollLimit, p.Delta)
	}
	return &p, nil
}

// KeyPayloadV2 represents a keystroke payload.
type KeyPayloadV2 struct {
	Key       string   `json:"key"`
	Modifiers []string `json:"modifiers,omitempty"`
}

// DecodeKeyPayloadV2 decodes a key payload.
func DecodeKeyPayloadV2(raw json.RawMessage) (*KeyPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p KeyPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid key payload: %w", err)
	}
	if p.Key == "" {
		return nil, fmt.Errorf("missing key field")
	}
	return &p, nil
}

// HelloPayloadV2 represents a hello handshake payload with mandatory authentication token support.
type HelloPayloadV2 struct {
	DeviceID       string `json:"device_id,omitempty"`
	Name           string `json:"name,omitempty"`
	Version        string `json:"version,omitempty"`
	Token          string `json:"token,omitempty"`
	DeviceName     string `json:"device_name,omitempty"`
	Model          string `json:"model,omitempty"`
	Manufacturer   string `json:"manufacturer,omitempty"`
	Brand          string `json:"brand,omitempty"`
	AndroidVersion string `json:"android_version,omitempty"`
	SDKInt         string `json:"sdk_int,omitempty"`
	Protocol       string `json:"protocol,omitempty"`
	Transport      string `json:"transport,omitempty"`
}

// DecodeHelloPayloadV2 decodes a hello payload with strict unknown field rejection.
func DecodeHelloPayloadV2(raw json.RawMessage) (*HelloPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p HelloPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid hello payload: %w", err)
	}
	return &p, nil
}

// WelcomePayloadV2 represents a welcome message payload.
type WelcomePayloadV2 struct {
	Server   string `json:"server"`
	Version  string `json:"version"`
	ClientID string `json:"client_id,omitempty"`
	Approved bool   `json:"approved,omitempty"`
	Status   string `json:"status,omitempty"`
	Approval string `json:"approval,omitempty"`
}

// AckPayloadV2 represents an acknowledgment payload.
type AckPayloadV2 struct {
	AckID  string `json:"ack_id"`
	Status string `json:"status"`
}

// NackPayloadV2 represents a negative acknowledgment payload.
type NackPayloadV2 struct {
	NackID      string `json:"nack_id,omitempty"`
	Error       string `json:"error"`
	ExpectedSeq uint64 `json:"expected_seq,omitempty"`
	MissingFrom uint64 `json:"missing_from,omitempty"`
	MissingTo   uint64 `json:"missing_to,omitempty"`
}

// DiscoverPayloadV2 represents discovery broadcast payload.
type DiscoverPayloadV2 struct {
	ClientName string `json:"client_name,omitempty"`
	Nonce      string `json:"nonce,omitempty"`
}

// OfferPayloadV2 represents discovery unicast offer payload.
type OfferPayloadV2 struct {
	ServerName      string `json:"server_name"`
	InstanceID      string `json:"instance_id"`
	TCPPort         int    `json:"tcp_port"`
	WSPort          int    `json:"ws_port"`
	TLS             bool   `json:"tls"`
	CertFingerprint string `json:"cert_fingerprint,omitempty"`
	EchoNonce       string `json:"echo_nonce,omitempty"`
}

// ------------------------------------------------------------
// Legacy Types and Functions (Preserved with Bounds Checks)
// ------------------------------------------------------------

// MovePayload represents a movement message payload
type MovePayload struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
}

// DecodeMovePayload decodes a movement payload and enforces bounds checks
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

	if math.IsNaN(dx) || math.IsInf(dx, 0) || math.IsNaN(dy) || math.IsInf(dy, 0) {
		return 0, 0, fmt.Errorf("dx/dy cannot be NaN or Inf")
	}
	if math.Abs(dx) > MaxCoordinateLimit || math.Abs(dy) > MaxCoordinateLimit {
		return 0, 0, fmt.Errorf("dx/dy out of bounds (|dx|, |dy| <= %.1f)", MaxCoordinateLimit)
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

// DecodeScrollPayload decodes a scroll payload and enforces bounds
func DecodeScrollPayload(payload map[string]any) (int, error) {
	deltaVal, ok := payload["delta"]
	if !ok {
		return 0, fmt.Errorf("missing delta field")
	}
	var delta int
	switch v := deltaVal.(type) {
	case float64:
		if math.IsNaN(v) || math.IsInf(v, 0) {
			return 0, fmt.Errorf("delta cannot be NaN or Inf")
		}
		if math.Abs(v) > MaxScrollLimit {
			return 0, fmt.Errorf("delta out of bounds (|delta| <= %.1f): %f", MaxScrollLimit, v)
		}
		delta = int(v)
	case int:
		delta = v
	case int64:
		delta = int(v)
	case json.Number:
		i, err := v.Int64()
		if err != nil {
			return 0, fmt.Errorf("invalid delta: %w", err)
		}
		delta = int(i)
	default:
		return 0, fmt.Errorf("delta is not a number")
	}

	if math.Abs(float64(delta)) > MaxScrollLimit {
		return 0, fmt.Errorf("delta out of bounds (|delta| <= %.1f): %d", MaxScrollLimit, delta)
	}

	return delta, nil
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

// Vector3D represents a 3D vector.
type Vector3D struct {
	X float64 `json:"x"`
	Y float64 `json:"y"`
	Z float64 `json:"z"`
}

// CalibrationPayload represents calibration data payload.
type CalibrationPayload struct {
	Gyro      Vector3D `json:"gyro"`
	Accel     Vector3D `json:"accel"`
	Mag       Vector3D `json:"mag"`
	Quality   string   `json:"quality"`
	Timestamp int64    `json:"timestamp"`
}

// DecodeCalibrationPayload decodes calibration data from the payload.
func DecodeCalibrationPayload(payload map[string]any) (CalibrationPayload, error) {
	var cp CalibrationPayload

	toFloat64 := func(v any) float64 {
		switch t := v.(type) {
		case float64:
			return t
		case float32:
			return float64(t)
		case int:
			return float64(t)
		case int64:
			return float64(t)
		case json.Number:
			f, _ := t.Float64()
			return f
		default:
			return 0
		}
	}

	parseVector := func(v map[string]any) Vector3D {
		var vec Vector3D
		if x, ok := v["bias_x"]; ok {
			vec.X = toFloat64(x)
		} else if x, ok := v["offset_x"]; ok {
			vec.X = toFloat64(x)
		} else if x, ok := v["x"]; ok {
			vec.X = toFloat64(x)
		}
		if y, ok := v["bias_y"]; ok {
			vec.Y = toFloat64(y)
		} else if y, ok := v["offset_y"]; ok {
			vec.Y = toFloat64(y)
		} else if y, ok := v["y"]; ok {
			vec.Y = toFloat64(y)
		}
		if z, ok := v["bias_z"]; ok {
			vec.Z = toFloat64(z)
		} else if z, ok := v["offset_z"]; ok {
			vec.Z = toFloat64(z)
		} else if z, ok := v["z"]; ok {
			vec.Z = toFloat64(z)
		}
		return vec
	}

	if gyroVal, ok := payload["gyro"]; ok {
		if gyroMap, ok := gyroVal.(map[string]any); ok {
			cp.Gyro = parseVector(gyroMap)
		}
	} else if gyroVal, ok := payload["gyro_bias"]; ok {
		if gyroMap, ok := gyroVal.(map[string]any); ok {
			cp.Gyro = parseVector(gyroMap)
		}
	}

	if accelVal, ok := payload["accel"]; ok {
		if accelMap, ok := accelVal.(map[string]any); ok {
			cp.Accel = parseVector(accelMap)
		}
	} else if accelVal, ok := payload["accel_bias"]; ok {
		if accelMap, ok := accelVal.(map[string]any); ok {
			cp.Accel = parseVector(accelMap)
		}
	}

	if magVal, ok := payload["mag"]; ok {
		if magMap, ok := magVal.(map[string]any); ok {
			cp.Mag = parseVector(magMap)
		}
	} else if magVal, ok := payload["mag_bias"]; ok {
		if magMap, ok := magVal.(map[string]any); ok {
			cp.Mag = parseVector(magMap)
		}
	}

	if q, ok := payload["quality"].(string); ok {
		cp.Quality = q
	}
	if t, ok := payload["timestamp"]; ok {
		cp.Timestamp = int64(toFloat64(t))
	}

	return cp, nil
}

// IsValidType checks if a message type is valid
func IsValidType(msgType string) bool {
	validTypes := map[string]bool{
		"move":             true,
		"move_rel":         true,
		"move_abs":         true,
		"click":            true,
		"left_click":       true,
		"doubleclick":      true,
		"double_click":     true,
		"rightclick":       true,
		"right_click":      true,
		"middleclick":      true,
		"middle_click":     true,
		"mouse_down":       true,
		"mouse_up":         true,
		"scroll":           true,
		"hscroll":          true,
		"key":              true,
		"keypress":         true,
		"key_press":        true,
		"keydown":          true,
		"key_down":         true,
		"keyup":            true,
		"key_up":           true,
		"hotkey":           true,
		"type":             true,
		"text":             true,
		"hello":            true,
		"device_info":      true,
		"gesture":          true,
		"proximity":        true,
		"control":          true,
		"ping":             true,
		"pong":             true,
		"heartbeat":        true,
		"ack":              true,
		"nack":             true,
		"error":            true,
		"welcome":          true,
		"status":           true,
		"file":             true,
		"discover":         true,
		"offer":            true,
		"presentation":     true,
		"slide_next":       true,
		"slide_prev":       true,
		"media":            true,
		"volume_up":        true,
		"volume_down":      true,
		"mute":             true,
		"play_pause":       true,
		"lock_screen":      true,
		"power_sleep":      true,
		"brightness":       true,
		"orientation":      true,
		"battery":          true,
		"battery_report":   true,
		"log":              true,
		"custom":           true,
		"calibration_data": true,
		"stats":            true,
		"stats_req":        true,
		"stats_resp":       true,
		"notification":     true,
		"config":           true,
		"laser":            true,
		"draw":             true,
		"annotation":       true,
		"voice_command":    true,
		"steering":         true,
		"shooter":          true,
		"smarthome":        true,
		"server_closing":   true,
	}
	return validTypes[msgType]
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
		"v":    ProtocolVersion,
		"type": "ack",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"ack_id": *id,
			"status": "ok",
		},
	})
	return append(body, '\n')
}

func ackMessage(id *string) []byte {
	return AckMessage(id)
}

// ErrorMessage returns an error message
func ErrorMessage(errMsg string) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "error",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"message": errMsg,
		},
	})
	return append(body, '\n')
}

func errorMessage(errMsg string) []byte {
	return ErrorMessage(errMsg)
}

// WelcomeMessage returns a welcome message
func WelcomeMessage(serverName, version string) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "welcome",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"server":   serverName,
			"version":  version,
			"approved": true,
		},
	})
	return append(body, '\n')
}

func welcomeMessage(serverName, version string) []byte {
	return WelcomeMessage(serverName, version)
}

// PongMessage returns a pong message
func PongMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"v":       ProtocolVersion,
		"type":    "pong",
		"ts":      time.Now().UnixMilli(),
		"payload": map[string]any{},
	})
	return append(body, '\n')
}

// PingMessage returns a ping message
func PingMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"v":       ProtocolVersion,
		"type":    "ping",
		"ts":      time.Now().UnixMilli(),
		"payload": map[string]any{},
	})
	return append(body, '\n')
}

// MoveMessage creates a movement message
func MoveMessage(dx, dy float64) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "move",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"dx": dx,
			"dy": dy,
		},
	})
	return append(body, '\n')
}

// ClickMessage creates a click message
func ClickMessage(button string) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "click",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"button": button,
		},
	})
	return append(body, '\n')
}

// DoubleClickMessage creates a double click message
func DoubleClickMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"v":       ProtocolVersion,
		"type":    "doubleclick",
		"ts":      time.Now().UnixMilli(),
		"payload": map[string]any{},
	})
	return append(body, '\n')
}

// RightClickMessage creates a right click message
func RightClickMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"v":       ProtocolVersion,
		"type":    "rightclick",
		"ts":      time.Now().UnixMilli(),
		"payload": map[string]any{},
	})
	return append(body, '\n')
}

// ScrollMessage creates a scroll message
func ScrollMessage(delta int) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "scroll",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"delta": delta,
		},
	})
	return append(body, '\n')
}

// GestureMessage creates a gesture message
func GestureMessage(gesture string, confidence float64) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "gesture",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"gesture":    gesture,
			"confidence": confidence,
		},
	})
	return append(body, '\n')
}

// ProximityMessage creates a proximity message
func ProximityMessage(deviceID string, isNear bool, distance float32) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "proximity",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"device_id": deviceID,
			"is_near":   isNear,
			"distance":  distance,
		},
	})
	return append(body, '\n')
}

// HelloMessage creates a hello message
func HelloMessage(name, version string) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "hello",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"name":    name,
			"version": version,
		},
	})
	return append(body, '\n')
}

// StatusMessage creates a status message
func StatusMessage(running bool, clients int) []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "status",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"running": running,
			"clients": clients,
		},
	})
	return append(body, '\n')
}

// ServerClosingMessage creates a clean server closing notification
func ServerClosingMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"v":    ProtocolVersion,
		"type": "server_closing",
		"ts":   time.Now().UnixMilli(),
		"payload": map[string]any{
			"reason": "shutdown",
		},
	})
	return append(body, '\n')
}

// StatsRespMessage creates a stats response message
func StatsRespMessage(stats map[string]any, reqID *string) []byte {
	env := map[string]any{
		"v":       ProtocolVersion,
		"type":    "stats_resp",
		"ts":      time.Now().UnixMilli(),
		"payload": stats,
	}
	if reqID != nil && *reqID != "" {
		env["id"] = *reqID
	}
	body, _ := json.Marshal(env)
	return append(body, '\n')
}
