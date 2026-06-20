package tcp

import (
	"encoding/json"
	"testing"
)

func TestDecodeWireMessageFlatPayload(t *testing.T) {
	msgType, payload, id, err := decodeWireMessage([]byte(`{"type":"move","dx":12.5,"dy":-3.25}`))
	if err != nil {
		t.Fatalf("decodeWireMessage returned error: %v", err)
	}
	if msgType != "move" {
		t.Fatalf("msgType = %q, want %q", msgType, "move")
	}
	if got := payload["dx"]; got != 12.5 {
		t.Fatalf("dx = %v, want 12.5", got)
	}
	if got := payload["dy"]; got != -3.25 {
		t.Fatalf("dy = %v, want -3.25", got)
	}
	if id != nil {
		t.Fatalf("id = %v, want nil", *id)
	}
}

func TestDecodeWireMessageNestedPayloadAndStringID(t *testing.T) {
	msgType, payload, id, err := decodeWireMessage([]byte(`{"type":"scroll","payload":{"delta":-1},"id":"42"}`))
	if err != nil {
		t.Fatalf("decodeWireMessage returned error: %v", err)
	}
	if msgType != "scroll" {
		t.Fatalf("msgType = %q, want %q", msgType, "scroll")
	}
	if got := payload["delta"]; got != float64(-1) {
		t.Fatalf("delta = %v, want -1", got)
	}
	if id == nil || *id != "42" {
		if id == nil {
			t.Fatalf("id = nil, want 42")
		}
		t.Fatalf("id = %q, want 42", *id)
	}
}

func TestDecodeWireMessageAliasedMotionFields(t *testing.T) {
	msgType, payload, _, err := decodeWireMessage([]byte(`{"type":"move","DeltaX":4.5,"DeltaY":-2.25}`))
	if err != nil {
		t.Fatalf("decodeWireMessage returned error: %v", err)
	}
	if msgType != "move" {
		t.Fatalf("msgType = %q, want move", msgType)
	}
	if got := payload["DeltaX"]; got != 4.5 {
		t.Fatalf("DeltaX = %v, want 4.5", got)
	}
	if got := payload["DeltaY"]; got != -2.25 {
		t.Fatalf("DeltaY = %v, want -2.25", got)
	}
}

func TestAckMessage(t *testing.T) {
	id := "abc-123"
	got := ackMessage(&id)
	var decoded map[string]any
	if err := json.Unmarshal(got, &decoded); err != nil {
		t.Fatalf("json.Unmarshal returned error: %v", err)
	}
	if decoded["type"] != "ack" {
		t.Fatalf("type = %v, want ack", decoded["type"])
	}
	if decoded["id"] != "abc-123" {
		t.Fatalf("id = %v, want abc-123", decoded["id"])
	}
}

func TestNumber(t *testing.T) {
	cases := []struct {
		name string
		val  any
		want float64
	}{
		{"float64", float64(3.5), 3.5},
		{"int", 4, 4},
		{"json.Number", json.Number("9.25"), 9.25},
		{"string", "x", 0},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if got := number(tc.val); got != tc.want {
				t.Fatalf("number(%v) = %v, want %v", tc.val, got, tc.want)
			}
		})
	}
}
