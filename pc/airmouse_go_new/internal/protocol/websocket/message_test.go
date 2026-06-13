package websocket

import (
	"encoding/json"
	"testing"
)

func TestDecodeWireMessageFlatPayload(t *testing.T) {
	msgType, payload, id, err := decodeWireMessage([]byte(`{"type":"click","button":"left","id":7}`))
	if err != nil {
		t.Fatalf("decodeWireMessage returned error: %v", err)
	}
	if msgType != "click" {
		t.Fatalf("msgType = %q, want %q", msgType, "click")
	}
	if got := payload["button"]; got != "left" {
		t.Fatalf("button = %v, want left", got)
	}
	if id == nil || *id != "7" {
		if id == nil {
			t.Fatalf("id = nil, want 7")
		}
		t.Fatalf("id = %q, want 7", *id)
	}
}

func TestDecodeWireMessageNestedPayload(t *testing.T) {
	msgType, payload, id, err := decodeWireMessage([]byte(`{"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.92}}`))
	if err != nil {
		t.Fatalf("decodeWireMessage returned error: %v", err)
	}
	if msgType != "gesture" {
		t.Fatalf("msgType = %q, want %q", msgType, "gesture")
	}
	if got := payload["gesture"]; got != "ThumbsUp" {
		t.Fatalf("gesture = %v, want ThumbsUp", got)
	}
	if got := payload["confidence"]; got != 0.92 {
		t.Fatalf("confidence = %v, want 0.92", got)
	}
	if id != nil {
		t.Fatalf("id = %v, want nil", *id)
	}
}

func TestAckMessage(t *testing.T) {
	id := "42"
	got := ackMessage(&id)
	var decoded map[string]any
	if err := json.Unmarshal(got, &decoded); err != nil {
		t.Fatalf("json.Unmarshal returned error: %v", err)
	}
	if decoded["type"] != "ack" {
		t.Fatalf("type = %v, want ack", decoded["type"])
	}
	if decoded["id"] != "42" {
		t.Fatalf("id = %v, want 42", decoded["id"])
	}
}
