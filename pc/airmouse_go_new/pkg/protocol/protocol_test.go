package protocol

import (
	"encoding/json"
	"strings"
	"testing"
	"time"
)

func TestProtocolV2EnvelopeStrictDecoding(t *testing.T) {
	// Valid message
	validJSON := `{"v":2,"type":"move","seq":1,"ts":1730000000000,"payload":{"dx":12.5,"dy":-5.0}}`
	env, err := DecodeWireMessageStrictV2([]byte(validJSON))
	if err != nil {
		t.Fatalf("Failed to decode valid envelope: %v", err)
	}
	if env.V != 2 || env.Type != "move" || env.Seq != 1 {
		t.Errorf("Decoded envelope fields mismatch: %+v", env)
	}

	movePayload, err := DecodeMovePayloadV2(env.Payload)
	if err != nil {
		t.Fatalf("Failed to decode move payload: %v", err)
	}
	if movePayload.DX != 12.5 || movePayload.DY != -5.0 {
		t.Errorf("Move coordinates mismatch: %+v", movePayload)
	}
}

func TestProtocolV2DisallowUnknownFields(t *testing.T) {
	// Unknown field in envelope
	injectedEnvelope := `{"v":2,"type":"move","seq":1,"ts":1730000000000,"evil_field":"injected","payload":{"dx":10,"dy":10}}`
	_, err := DecodeWireMessageStrictV2([]byte(injectedEnvelope))
	if err == nil {
		t.Fatalf("Expected error for unknown field in envelope, but got none")
	}

	// Unknown field in payload
	validEnv := `{"v":2,"type":"move","seq":1,"ts":1730000000000,"payload":{"dx":10,"dy":10,"unexpected":123}}`
	env, err := DecodeWireMessageStrictV2([]byte(validEnv))
	if err != nil {
		t.Fatalf("Envelope decode failed: %v", err)
	}
	_, err = DecodeMovePayloadV2(env.Payload)
	if err == nil {
		t.Fatalf("Expected error for unknown field in move payload, but got none")
	}
}

func TestProtocolV1Rejection(t *testing.T) {
	v1Message := `{"v":1,"type":"move","seq":1,"ts":1730000000000,"payload":{"dx":10,"dy":10}}`
	_, err := DecodeWireMessageStrictV2([]byte(v1Message))
	if err == nil || !strings.Contains(err.Error(), "protocol_v1_deprecated") {
		t.Fatalf("Expected protocol_v1_deprecated rejection, got: %v", err)
	}
}

func TestMoveBoundsValidation(t *testing.T) {
	// Extreme jump exceeding 500
	raw := json.RawMessage(`{"dx":600.0,"dy":10.0}`)
	_, err := DecodeMovePayloadV2(raw)
	if err == nil {
		t.Fatalf("Expected out-of-bounds error for dx=600, got none")
	}

	// Valid boundary
	rawOk := json.RawMessage(`{"dx":500.0,"dy":-500.0}`)
	p, err := DecodeMovePayloadV2(rawOk)
	if err != nil {
		t.Fatalf("Boundary dx=500, dy=-500 should pass: %v", err)
	}
	if p.DX != 500.0 || p.DY != -500.0 {
		t.Errorf("Unexpected payload: %+v", p)
	}
}

func TestClickValidation(t *testing.T) {
	rawOk := json.RawMessage(`{"button":"left"}`)
	c, err := DecodeClickPayloadV2(rawOk)
	if err != nil || c.Button != "left" {
		t.Fatalf("Valid button failed: %v", err)
	}

	rawBad := json.RawMessage(`{"button":"invalid_btn"}`)
	_, err = DecodeClickPayloadV2(rawBad)
	if err == nil {
		t.Fatalf("Expected error for invalid button name, got none")
	}
}

func TestScrollBoundsValidation(t *testing.T) {
	rawOk := json.RawMessage(`{"delta":5.0}`)
	s, err := DecodeScrollPayloadV2(rawOk)
	if err != nil || s.Delta != 5.0 {
		t.Fatalf("Valid scroll failed: %v", err)
	}

	rawBad := json.RawMessage(`{"delta":15.0}`)
	_, err = DecodeScrollPayloadV2(rawBad)
	if err == nil {
		t.Fatalf("Expected error for scroll delta > 10.0, got none")
	}
}

func TestReplayProtection(t *testing.T) {
	rd := NewReplayDetector()
	now := time.Now().UnixMilli()

	// 1. Initial message (seq=10)
	env1 := &EnvelopeV2{V: 2, Type: "move", Seq: 10, Ts: now}
	if err := rd.ValidateMessage(env1, now); err != nil {
		t.Fatalf("First message rejected: %v", err)
	}

	// 2. Monotonic advance (seq=11)
	env2 := &EnvelopeV2{V: 2, Type: "move", Seq: 11, Ts: now + 10}
	if err := rd.ValidateMessage(env2, now); err != nil {
		t.Fatalf("Second message rejected: %v", err)
	}

	// 3. Replay attack: repeated sequence (seq=11)
	if err := rd.ValidateMessage(env2, now); err == nil {
		t.Fatalf("Expected replay rejection for repeated seq=11, got none")
	}

	// 4. Stale sequence from the past (seq=5)
	envOld := &EnvelopeV2{V: 2, Type: "move", Seq: 5, Ts: now + 20}
	if err := rd.ValidateMessage(envOld, now); err == nil {
		t.Fatalf("Expected replay rejection for stale seq=5, got none")
	}

	// 5. Expired timestamp (> 5000ms delta)
	envStaleTime := &EnvelopeV2{V: 2, Type: "move", Seq: 12, Ts: now - 6000}
	if err := rd.ValidateMessage(envStaleTime, now); err == nil {
		t.Fatalf("Expected replay rejection for stale timestamp, got none")
	}
}

func TestMessageEncoders(t *testing.T) {
	// Move message
	moveBytes, err := NewMoveMessage(10.5, -20.0, 0.02, 1)
	if err != nil {
		t.Fatalf("Failed to encode move message: %v", err)
	}
	env, err := DecodeWireMessageStrictV2(moveBytes)
	if err != nil || env.Type != "move" {
		t.Fatalf("Decode encoded move failed: %v", err)
	}

	// Click message
	clickBytes, err := NewClickMessage("right", 2)
	if err != nil {
		t.Fatalf("Failed to encode click: %v", err)
	}
	env, err = DecodeWireMessageStrictV2(clickBytes)
	if err != nil || env.Type != "click" {
		t.Fatalf("Decode encoded click failed: %v", err)
	}

	// Hello message
	helloBytes, err := NewHelloMessage("dev-1", "MyPhone", "token-secret", 3)
	if err != nil {
		t.Fatalf("Failed to encode hello: %v", err)
	}
	env, err = DecodeWireMessageStrictV2(helloBytes)
	if err != nil || env.Type != "hello" {
		t.Fatalf("Decode encoded hello failed: %v", err)
	}
	hello, err := DecodeHelloPayloadV2(env.Payload)
	if err != nil || hello.DeviceID != "dev-1" {
		t.Fatalf("Hello payload mismatch: %v", err)
	}

	// Welcome message
	welcomeBytes, err := NewWelcomeMessage("AirMouseServer", "3.0.0", "client-99", 4)
	if err != nil {
		t.Fatalf("Failed to encode welcome: %v", err)
	}
	env, err = DecodeWireMessageStrictV2(welcomeBytes)
	if err != nil || env.Type != "welcome" {
		t.Fatalf("Decode encoded welcome failed: %v", err)
	}

	// Ack message
	ackBytes, err := NewAckMessage("msg-123", 5)
	if err != nil {
		t.Fatalf("Failed to encode ack: %v", err)
	}
	env, err = DecodeWireMessageStrictV2(ackBytes)
	if err != nil || env.Type != "ack" {
		t.Fatalf("Decode encoded ack failed: %v", err)
	}
}
