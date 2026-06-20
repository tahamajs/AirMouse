package websocket

import (
	"encoding/json"
	"testing"
)

func TestDecodeWireMessageFlatPayload(t *testing.T) {
	msgType, payload, id, err := DecodeWireMessage([]byte(`{"type":"click","button":"left","id":7}`))
	if err != nil {
		t.Fatalf("DecodeWireMessage returned error: %v", err)
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
	msgType, payload, id, err := DecodeWireMessage([]byte(`{"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.92}}`))
	if err != nil {
		t.Fatalf("DecodeWireMessage returned error: %v", err)
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

func TestDecodeWireMessageAliasedMotionFields(t *testing.T) {
	msgType, payload, _, err := DecodeWireMessage([]byte(`{"type":"scroll","Scroll":-3,"horizontal":true}`))
	if err != nil {
		t.Fatalf("DecodeWireMessage returned error: %v", err)
	}
	if msgType != "scroll" {
		t.Fatalf("msgType = %q, want scroll", msgType)
	}
	if got := payload["Scroll"]; got != float64(-3) {
		t.Fatalf("Scroll = %v, want -3", got)
	}
	if got := payload["horizontal"]; got != true {
		t.Fatalf("horizontal = %v, want true", got)
	}
}

func TestAckMessage(t *testing.T) {
	id := "42"
	got := AckMessage(&id)
	if len(got) == 0 {
		t.Fatal("AckMessage returned empty")
	}
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
