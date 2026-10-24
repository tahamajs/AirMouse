package protocol

import (
	"encoding/json"
	"math"
	"strings"
	"testing"
	"time"
)

func TestStrictV2EnvelopeValid(t *testing.T) {
	raw := `{"v":2,"type":"move","seq":101,"ts":1740000000000,"id":"act-1","payload":{"dx":12.5,"dy":-4.0}}`
	env, err := DecodeWireMessageStrictV2([]byte(raw))
	if err != nil {
		t.Fatalf("Expected valid V2 envelope to decode successfully: %v", err)
	}

	if env.V != 2 {
		t.Errorf("Expected v=2, got %d", env.V)
	}
	if env.Type != "move" {
		t.Errorf("Expected type 'move', got %q", env.Type)
	}
	if env.Seq != 101 {
		t.Errorf("Expected seq=101, got %d", env.Seq)
	}
	if env.Ts != 1740000000000 {
		t.Errorf("Expected ts=1740000000000, got %d", env.Ts)
	}
	if env.ID == nil || *env.ID != "act-1" {
		t.Errorf("Expected id 'act-1', got %v", env.ID)
	}
}

func TestStrictV2DisallowUnknownFieldsInEnvelope(t *testing.T) {
	// Unknown field "extra_field": "injected" must be rejected by DisallowUnknownFields()
	raw := `{"v":2,"type":"move","seq":101,"ts":1740000000000,"extra_field":"injected","payload":{"dx":1.0,"dy":1.0}}`
	_, err := DecodeWireMessageStrictV2([]byte(raw))
	if err == nil {
		t.Fatalf("Expected DisallowUnknownFields to reject extra_field in envelope, but got nil")
	}
	if !strings.Contains(err.Error(), "unknown field") {
		t.Errorf("Expected error to mention unknown field, got: %v", err)
	}
}

func TestStrictV2ProtocolVersionRejection(t *testing.T) {
	// Version 1 is deprecated
	rawV1 := `{"v":1,"type":"move","seq":101,"ts":1740000000000,"payload":{"dx":1.0,"dy":1.0}}`
	_, err := DecodeWireMessageStrictV2([]byte(rawV1))
	if err == nil {
		t.Fatalf("Expected error for v=1, got nil")
	}
	if !strings.Contains(err.Error(), "protocol_v1_deprecated") {
		t.Errorf("Expected 'protocol_v1_deprecated' in error, got: %v", err)
	}

	// Missing version (v defaults to 0)
	rawV0 := `{"type":"move","seq":101,"ts":1740000000000,"payload":{"dx":1.0,"dy":1.0}}`
	_, err = DecodeWireMessageStrictV2([]byte(rawV0))
	if err == nil {
		t.Fatalf("Expected error for missing version, got nil")
	}
}

func TestStrictV2PayloadMustBeObject(t *testing.T) {
	// Flat format / non-object payload must be rejected
	rawArray := `{"v":2,"type":"move","seq":101,"ts":1740000000000,"payload":["dx",10]}`
	_, err := DecodeWireMessageStrictV2([]byte(rawArray))
	if err == nil {
		t.Fatalf("Expected error for non-object payload, got nil")
	}
}

func TestMoveBoundsChecking(t *testing.T) {
	// 1. Valid coordinates within bounds (|dx|, |dy| <= 500)
	validRaw := json.RawMessage(`{"dx":250.5,"dy":-499.9}`)
	p, err := DecodeMovePayloadV2(validRaw)
	if err != nil {
		t.Fatalf("Expected valid move payload to decode: %v", err)
	}
	if p.DX != 250.5 || p.DY != -499.9 {
		t.Errorf("Unexpected decoded values: dx=%f, dy=%f", p.DX, p.DY)
	}

	// 2. Out of bounds dx > 500
	oobXRaw := json.RawMessage(`{"dx":500.1,"dy":0}`)
	_, err = DecodeMovePayloadV2(oobXRaw)
	if err == nil {
		t.Fatalf("Expected out-of-bounds dx (> 500) to fail")
	}

	// 3. Out of bounds dy < -500
	oobYRaw := json.RawMessage(`{"dx":0,"dy":-500.5}`)
	_, err = DecodeMovePayloadV2(oobYRaw)
	if err == nil {
		t.Fatalf("Expected out-of-bounds dy (< -500) to fail")
	}

	// 4. Unknown field in MovePayload must be rejected
	unknownFieldRaw := json.RawMessage(`{"dx":10,"dy":10,"malicious_key":"foo"}`)
	_, err = DecodeMovePayloadV2(unknownFieldRaw)
	if err == nil {
		t.Fatalf("Expected unknown field in MovePayload to fail")
	}

	// 5. Special floats (NaN / Inf) in DecodeMovePayload
	nanMap := map[string]any{"dx": math.NaN(), "dy": 0.0}
	_, _, err = DecodeMovePayload(nanMap)
	if err == nil {
		t.Fatalf("Expected NaN dx to fail DecodeMovePayload")
	}

	infMap := map[string]any{"dx": 0.0, "dy": math.Inf(1)}
	_, _, err = DecodeMovePayload(infMap)
	if err == nil {
		t.Fatalf("Expected +Inf dy to fail DecodeMovePayload")
	}

	oobMap := map[string]any{"dx": 501.0, "dy": 0.0}
	_, _, err = DecodeMovePayload(oobMap)
	if err == nil {
		t.Fatalf("Expected dx > 500 to fail DecodeMovePayload")
	}
}

func TestScrollBoundsChecking(t *testing.T) {
	// 1. Valid delta within bounds (|delta| <= 10.0)
	validRaw := json.RawMessage(`{"delta":5.0}`)
	p, err := DecodeScrollPayloadV2(validRaw)
	if err != nil {
		t.Fatalf("Expected valid scroll payload: %v", err)
	}
	if p.Delta != 5.0 {
		t.Errorf("Expected delta 5.0, got %f", p.Delta)
	}

	// 2. Out of bounds delta > 10.0
	oobRaw := json.RawMessage(`{"delta":10.5}`)
	_, err = DecodeScrollPayloadV2(oobRaw)
	if err == nil {
		t.Fatalf("Expected delta > 10.0 to fail")
	}

	// 3. Out of bounds delta < -10.0
	oobNegRaw := json.RawMessage(`{"delta":-15.0}`)
	_, err = DecodeScrollPayloadV2(oobNegRaw)
	if err == nil {
		t.Fatalf("Expected delta < -10.0 to fail")
	}

	// 4. Unknown field in ScrollPayload
	unknownRaw := json.RawMessage(`{"delta":1.0,"extra":"bad"}`)
	_, err = DecodeScrollPayloadV2(unknownRaw)
	if err == nil {
		t.Fatalf("Expected unknown field in ScrollPayload to fail")
	}

	// 5. DecodeScrollPayload map bounds check
	oobMap := map[string]any{"delta": 25}
	_, err = DecodeScrollPayload(oobMap)
	if err == nil {
		t.Fatalf("Expected map delta > 10 to fail DecodeScrollPayload")
	}
}

func TestClickButtonValidation(t *testing.T) {
	allowed := []string{"left", "right", "middle", "double"}
	for _, btn := range allowed {
		raw := json.RawMessage(`{"button":"` + btn + `"}`)
		p, err := DecodeClickPayloadV2(raw)
		if err != nil {
			t.Errorf("Expected button %q to be allowed: %v", btn, err)
		}
		if p.Button != btn {
			t.Errorf("Expected button %q, got %q", btn, p.Button)
		}
	}

	invalidRaw := json.RawMessage(`{"button":"destructive_exploit"}`)
	_, err := DecodeClickPayloadV2(invalidRaw)
	if err == nil {
		t.Fatalf("Expected invalid button to fail")
	}

	extraFieldRaw := json.RawMessage(`{"button":"left","unauthorized":true}`)
	_, err = DecodeClickPayloadV2(extraFieldRaw)
	if err == nil {
		t.Fatalf("Expected unknown field in click payload to fail")
	}
}

func TestHelloPayloadV2Validation(t *testing.T) {
	validRaw := json.RawMessage(`{"device_id":"dev-123","name":"Pixel 8","version":"2.0.0","token":"secret_token"}`)
	p, err := DecodeHelloPayloadV2(validRaw)
	if err != nil {
		t.Fatalf("Expected valid hello payload: %v", err)
	}
	if p.Token != "secret_token" || p.Name != "Pixel 8" {
		t.Errorf("Decoded hello payload mismatch: %+v", p)
	}

	unknownRaw := json.RawMessage(`{"name":"Pixel 8","injected_field":"exploit"}`)
	_, err = DecodeHelloPayloadV2(unknownRaw)
	if err == nil {
		t.Fatalf("Expected unknown field in hello payload to fail")
	}
}

func TestReplayTimestampSlidingWindow(t *testing.T) {
	nowMs := time.Now().UnixMilli()

	// 1. Current timestamp within window (+/- 5000ms)
	if err := ValidateTimestamp(nowMs, nowMs); err != nil {
		t.Errorf("Expected 0ms delta to pass: %v", err)
	}

	// 2. 3000ms in past -> valid
	if err := ValidateTimestamp(nowMs-3000, nowMs); err != nil {
		t.Errorf("Expected -3000ms delta to pass: %v", err)
	}

	// 3. 3000ms in future (clock skew within window) -> valid
	if err := ValidateTimestamp(nowMs+3000, nowMs); err != nil {
		t.Errorf("Expected +3000ms delta to pass: %v", err)
	}

	// 4. 5001ms in past -> EXPIRED / REJECTED
	if err := ValidateTimestamp(nowMs-5001, nowMs); err == nil {
		t.Errorf("Expected delta 5001ms past to be rejected")
	}

	// 5. 6000ms in future -> REJECTED
	if err := ValidateTimestamp(nowMs+6000, nowMs); err == nil {
		t.Errorf("Expected delta 6000ms future to be rejected")
	}
}

func TestReplayMonotonicSequence(t *testing.T) {
	rd := NewReplayDetector()

	// First sequence number establishes session baseline
	if err := rd.CheckAndAdvanceSeq(100); err != nil {
		t.Fatalf("Initial sequence 100 failed: %v", err)
	}
	if rd.LastSeq() != 100 {
		t.Errorf("Expected LastSeq 100, got %d", rd.LastSeq())
	}

	// Strictly greater sequence numbers must succeed
	if err := rd.CheckAndAdvanceSeq(101); err != nil {
		t.Fatalf("Advancing sequence to 101 failed: %v", err)
	}
	if err := rd.CheckAndAdvanceSeq(150); err != nil {
		t.Fatalf("Advancing sequence to 150 failed: %v", err)
	}

	// Repeated sequence number (replay attack) MUST fail
	if err := rd.CheckAndAdvanceSeq(150); err == nil {
		t.Fatalf("Expected repeated sequence 150 to fail (replay attack)")
	}

	// Out-of-order / smaller sequence number MUST fail
	if err := rd.CheckAndAdvanceSeq(149); err == nil {
		t.Fatalf("Expected older sequence 149 to fail (replay attack)")
	}

	if err := rd.CheckAndAdvanceSeq(100); err == nil {
		t.Fatalf("Expected older sequence 100 to fail (replay attack)")
	}

	// Sequence must still be 150
	if rd.LastSeq() != 150 {
		t.Errorf("Expected LastSeq to remain 150, got %d", rd.LastSeq())
	}

	// Next greater sequence succeeds
	if err := rd.CheckAndAdvanceSeq(151); err != nil {
		t.Fatalf("Advancing sequence to 151 failed: %v", err)
	}
}

func TestReplayDetectorValidateMessage(t *testing.T) {
	rd := NewReplayDetector()
	nowMs := time.Now().UnixMilli()

	// Valid message within window and monotonic seq
	env1 := &EnvelopeV2{V: 2, Type: "move", Seq: 1, Ts: nowMs}
	if err := rd.ValidateMessage(env1, nowMs); err != nil {
		t.Fatalf("Expected env1 to be valid: %v", err)
	}

	// Expired timestamp
	envExpired := &EnvelopeV2{V: 2, Type: "move", Seq: 2, Ts: nowMs - 10000}
	if err := rd.ValidateMessage(envExpired, nowMs); err == nil {
		t.Fatalf("Expected expired timestamp to be rejected")
	}

	// Replayed sequence
	envReplay := &EnvelopeV2{V: 2, Type: "move", Seq: 1, Ts: nowMs}
	if err := rd.ValidateMessage(envReplay, nowMs); err == nil {
		t.Fatalf("Expected replayed sequence 1 to be rejected")
	}
}
