// Package protocol provides Protocol V2 wire envelope definitions, strict JSON schemas,
// typed payload structures, and encoding/decoding helpers for AirMouse communication.
//
// The wire envelope format is:
//
//	{
//	  "v": 2,
//	  "type": "move",
//	  "seq": 1042,
//	  "ts": 1730000000000,
//	  "id": "optional-uuid",
//	  "payload": { ... }
//	}
//
// Strict decoding enforces:
//   - DisallowUnknownFields: rejects unexpected or injected fields.
//   - Replay protection: monotonically increasing sequences and sliding timestamp windows (+/- 5000ms).
//   - Numeric limit clamping (|dx|, |dy| <= 500, |scroll| <= 10, no NaN or Inf).
package protocol

import (
	"bytes"
	"encoding/json"
	"fmt"
	"math"
	"sync"
	"time"
)

// ProtocolVersion is the canonical protocol version (V2).
const ProtocolVersion = 2

// ReplayWindowMs is the sliding window size in milliseconds (+/- 5000ms).
const ReplayWindowMs int64 = 5000

// MaxCoordinateLimit is the maximum absolute coordinate value allowed for mouse movement (|dx|, |dy| <= 500).
const MaxCoordinateLimit float64 = 500.0

// MaxScrollLimit is the maximum absolute scroll delta allowed (|delta| <= 10).
const MaxScrollLimit float64 = 10.0

// EnvelopeV2 represents the canonical Protocol V2 wire envelope.
type EnvelopeV2 struct {
	V       int             `json:"v"`
	Type    string          `json:"type"`
	Seq     uint64          `json:"seq"`
	Ts      int64           `json:"ts"`
	ID      *string         `json:"id,omitempty"`
	Payload json.RawMessage `json:"payload"`
}

// ------------------------------------------------------------
// Typed Payloads
// ------------------------------------------------------------

// MovePayloadV2 represents a movement message payload.
type MovePayloadV2 struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
	DT float64 `json:"dt,omitempty"`
}

// ClickPayloadV2 represents a mouse click payload.
type ClickPayloadV2 struct {
	Button string `json:"button"` // "left", "right", "middle", "double"
}

// ScrollPayloadV2 represents a mouse scroll payload.
type ScrollPayloadV2 struct {
	Delta float64 `json:"delta"`
}

// KeyPayloadV2 represents a keyboard keypress event payload.
type KeyPayloadV2 struct {
	Key       string   `json:"key"`
	Modifiers []string `json:"modifiers,omitempty"`
}

// HelloPayloadV2 represents client handshake information.
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

// WelcomePayloadV2 represents the server's handshake response.
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

// DiscoverPayloadV2 represents UDP discovery broadcast payload.
type DiscoverPayloadV2 struct {
	ClientName string `json:"client_name,omitempty"`
	Nonce      string `json:"nonce,omitempty"`
}

// OfferPayloadV2 represents UDP discovery server response payload.
type OfferPayloadV2 struct {
	ServerName      string `json:"server_name"`
	InstanceID      string `json:"instance_id"`
	TCPPort         int    `json:"tcp_port"`
	WSPort          int    `json:"ws_port"`
	TLS             bool   `json:"tls"`
	CertFingerprint string `json:"cert_fingerprint,omitempty"`
	EchoNonce       string `json:"echo_nonce,omitempty"`
}

// GesturePayloadV2 represents a detected gesture event payload.
type GesturePayloadV2 struct {
	Gesture    string  `json:"gesture"`
	Confidence float64 `json:"confidence"`
}

// ProximityPayloadV2 represents a proximity distance update payload.
type ProximityPayloadV2 struct {
	DeviceID string  `json:"device_id"`
	IsNear   bool    `json:"is_near"`
	Distance float32 `json:"distance"`
}

// ------------------------------------------------------------
// Strict V2 Decoding
// ------------------------------------------------------------

// DecodeWireMessageStrictV2 decodes and validates a raw Protocol V2 JSON envelope.
// Unknown fields in the outer envelope are rejected.
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

	trimmed := bytes.TrimSpace(env.Payload)
	if len(trimmed) == 0 || trimmed[0] != '{' {
		return nil, fmt.Errorf("invalid payload: payload must be a non-empty JSON object")
	}

	return &env, nil
}

// DecodeMovePayloadV2 decodes and validates a movement payload enforcing |dx|, |dy| <= 500 and finite values.
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

// DecodeClickPayloadV2 decodes a click payload with button validation.
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

// DecodeScrollPayloadV2 decodes a scroll payload with numeric bounds checking (|delta| <= 10.0).
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

// DecodeKeyPayloadV2 decodes a keyboard key payload.
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

// DecodeHelloPayloadV2 decodes a hello handshake payload.
func DecodeHelloPayloadV2(raw json.RawMessage) (*HelloPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p HelloPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid hello payload: %w", err)
	}
	return &p, nil
}

// DecodeGesturePayloadV2 decodes a gesture payload.
func DecodeGesturePayloadV2(raw json.RawMessage) (*GesturePayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p GesturePayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid gesture payload: %w", err)
	}
	if p.Gesture == "" {
		return nil, fmt.Errorf("missing gesture field")
	}
	return &p, nil
}

// DecodeProximityPayloadV2 decodes a proximity payload.
func DecodeProximityPayloadV2(raw json.RawMessage) (*ProximityPayloadV2, error) {
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.DisallowUnknownFields()
	var p ProximityPayloadV2
	if err := dec.Decode(&p); err != nil {
		return nil, fmt.Errorf("invalid proximity payload: %w", err)
	}
	return &p, nil
}

// ------------------------------------------------------------
// Message Encoders
// ------------------------------------------------------------

// EncodeEnvelope serializes a complete Protocol V2 envelope with trailing newline.
func EncodeEnvelope(msgType string, seq uint64, id *string, payload any) ([]byte, error) {
	rawPayload, err := json.Marshal(payload)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize payload: %w", err)
	}

	env := EnvelopeV2{
		V:       ProtocolVersion,
		Type:    msgType,
		Seq:     seq,
		Ts:      time.Now().UnixMilli(),
		ID:      id,
		Payload: rawPayload,
	}

	data, err := json.Marshal(env)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize envelope: %w", err)
	}
	return append(data, '\n'), nil
}

// NewMoveMessage creates a signed movement message.
func NewMoveMessage(dx, dy, dt float64, seq uint64) ([]byte, error) {
	if math.Abs(dx) > MaxCoordinateLimit || math.Abs(dy) > MaxCoordinateLimit {
		return nil, fmt.Errorf("move coordinates out of bounds")
	}
	return EncodeEnvelope("move", seq, nil, MovePayloadV2{
		DX: dx,
		DY: dy,
		DT: dt,
	})
}

// NewClickMessage creates a click message ("left", "right", "middle", "double").
func NewClickMessage(button string, seq uint64) ([]byte, error) {
	switch button {
	case "left", "right", "middle", "double":
	default:
		return nil, fmt.Errorf("invalid click button %q", button)
	}
	return EncodeEnvelope("click", seq, nil, ClickPayloadV2{
		Button: button,
	})
}

// NewScrollMessage creates a scroll message.
func NewScrollMessage(delta float64, seq uint64) ([]byte, error) {
	if math.Abs(delta) > MaxScrollLimit {
		return nil, fmt.Errorf("scroll delta out of bounds")
	}
	return EncodeEnvelope("scroll", seq, nil, ScrollPayloadV2{
		Delta: delta,
	})
}

// NewKeyMessage creates a keystroke message.
func NewKeyMessage(key string, modifiers []string, seq uint64) ([]byte, error) {
	if key == "" {
		return nil, fmt.Errorf("key cannot be empty")
	}
	return EncodeEnvelope("key", seq, nil, KeyPayloadV2{
		Key:       key,
		Modifiers: modifiers,
	})
}

// NewHelloMessage creates a client hello handshake message.
func NewHelloMessage(deviceID, name, token string, seq uint64) ([]byte, error) {
	return EncodeEnvelope("hello", seq, nil, HelloPayloadV2{
		DeviceID: deviceID,
		Name:     name,
		Version:  "2.0.0",
		Token:    token,
	})
}

// NewWelcomeMessage creates a server welcome message.
func NewWelcomeMessage(serverName, version, clientID string, seq uint64) ([]byte, error) {
	return EncodeEnvelope("welcome", seq, nil, WelcomePayloadV2{
		Server:   serverName,
		Version:  version,
		ClientID: clientID,
		Approved: true,
		Status:   "approved",
	})
}

// NewAckMessage creates an acknowledgment message.
func NewAckMessage(ackID string, seq uint64) ([]byte, error) {
	return EncodeEnvelope("ack", seq, nil, AckPayloadV2{
		AckID:  ackID,
		Status: "ok",
	})
}

// NewNackMessage creates a negative acknowledgment message.
func NewNackMessage(nackID, errMsg string, expectedSeq uint64, seq uint64) ([]byte, error) {
	return EncodeEnvelope("nack", seq, nil, NackPayloadV2{
		NackID:      nackID,
		Error:       errMsg,
		ExpectedSeq: expectedSeq,
	})
}

// NewPingMessage creates a heartbeat ping message.
func NewPingMessage(seq uint64) ([]byte, error) {
	return EncodeEnvelope("ping", seq, nil, map[string]any{})
}

// NewPongMessage creates a heartbeat pong message.
func NewPongMessage(seq uint64) ([]byte, error) {
	return EncodeEnvelope("pong", seq, nil, map[string]any{})
}

// ------------------------------------------------------------
// Replay Protection
// ------------------------------------------------------------

// ValidateTimestamp checks if the timestamp in milliseconds is within +/- 5000ms of nowMs.
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

// Reset clears sequence history.
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
