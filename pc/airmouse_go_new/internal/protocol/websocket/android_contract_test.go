package websocket

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestDecodeAndroidOutboundProtocolContract(t *testing.T) {
	tests := []struct {
		name   string
		raw    string
		assert func(t *testing.T, payload map[string]any, id *string)
	}{
		{
			name: "move flat payload",
			raw:  `{"type":"move","dx":12.5,"dy":-3.25,"DeltaX":12.5,"DeltaY":-3.25}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				dx, dy, err := DecodeMovePayload(payload)
				if err != nil {
					t.Fatalf("DecodeMovePayload returned error: %v", err)
				}
				if dx != 12.5 || dy != -3.25 {
					t.Fatalf("move = (%v,%v), want (12.5,-3.25)", dx, dy)
				}
				if id != nil {
					t.Fatalf("id = %q, want nil", *id)
				}
			},
		},
		{
			name: "reliable click flat payload",
			raw:  `{"type":"click","id":"msg_7","button":"left","Click":"left"}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				button, err := DecodeClickPayload(payload)
				if err != nil {
					t.Fatalf("DecodeClickPayload returned error: %v", err)
				}
				if button != "left" {
					t.Fatalf("button = %q, want left", button)
				}
				if id == nil || *id != "msg_7" {
					t.Fatalf("id = %v, want msg_7", id)
				}
			},
		},
		{
			name: "reliable scroll flat payload",
			raw:  `{"type":"scroll","id":"msg_8","delta":-4,"Scroll":-4}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				delta, err := DecodeScrollPayload(payload)
				if err != nil {
					t.Fatalf("DecodeScrollPayload returned error: %v", err)
				}
				if delta != -4 {
					t.Fatalf("delta = %d, want -4", delta)
				}
				if id == nil || *id != "msg_8" {
					t.Fatalf("id = %v, want msg_8", id)
				}
			},
		},
		{
			name: "gesture nested payload",
			raw:  `{"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.91}}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				gesture, confidence, err := DecodeGesturePayload(payload)
				if err != nil {
					t.Fatalf("DecodeGesturePayload returned error: %v", err)
				}
				if gesture != "ThumbsUp" || confidence != 0.91 {
					t.Fatalf("gesture = (%q,%v), want (ThumbsUp,0.91)", gesture, confidence)
				}
				if id != nil {
					t.Fatalf("id = %q, want nil", *id)
				}
			},
		},
		{
			name: "proximity nested payload",
			raw:  `{"type":"proximity","payload":{"device_id":"android-device","is_near":true,"distance":1.75}}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				deviceID, isNear, distance, err := DecodeProximityPayload(payload)
				if err != nil {
					t.Fatalf("DecodeProximityPayload returned error: %v", err)
				}
				if deviceID != "android-device" || !isNear || distance != 1.75 {
					t.Fatalf("proximity = (%q,%v,%v), want (android-device,true,1.75)", deviceID, isNear, distance)
				}
				if id != nil {
					t.Fatalf("id = %q, want nil", *id)
				}
			},
		},
		{
			name: "control nested payload",
			raw:  `{"type":"control","payload":{"command":"stop"}}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				command, err := DecodeControlPayload(payload)
				if err != nil {
					t.Fatalf("DecodeControlPayload returned error: %v", err)
				}
				if command != "stop" {
					t.Fatalf("command = %q, want stop", command)
				}
				if id != nil {
					t.Fatalf("id = %q, want nil", *id)
				}
			},
		},
		{
			name: "hello nested payload",
			raw:  `{"type":"hello","payload":{"name":"Pixel Test","version":"3.0","device":"Google Pixel Test","android_version":"14","protocol":"WEBSOCKET","transport":"websocket","token":"secret-token"}}`,
			assert: func(t *testing.T, payload map[string]any, id *string) {
				t.Helper()
				name, version, err := DecodeHelloPayload(payload)
				if err != nil {
					t.Fatalf("DecodeHelloPayload returned error: %v", err)
				}
				if name != "Pixel Test" || version != "3.0" {
					t.Fatalf("hello = (%q,%q), want (Pixel Test,3.0)", name, version)
				}
				if payload["transport"] != "websocket" {
					t.Fatalf("transport = %v, want websocket", payload["transport"])
				}
				if id != nil {
					t.Fatalf("id = %q, want nil", *id)
				}
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			msgType, payload, id, err := DecodeWireMessage([]byte(tt.raw))
			if err != nil {
				t.Fatalf("DecodeWireMessage returned error: %v", err)
			}
			if msgType == "" {
				t.Fatal("msgType is empty")
			}
			tt.assert(t, payload, id)
		})
	}
}

func TestGoServerRepliesMatchAndroidInboundContract(t *testing.T) {
	id := "msg_7"
	messages := [][]byte{
		WelcomeMessage("Air Mouse Go", "3.0"),
		AckMessage(&id),
		PongMessage(),
		StatusMessage(true, 2),
	}

	for _, raw := range messages {
		t.Run(strings.TrimSpace(string(raw)), func(t *testing.T) {
			var decoded map[string]any
			if err := json.Unmarshal(raw, &decoded); err != nil {
				t.Fatalf("json.Unmarshal returned error: %v", err)
			}
			if _, ok := decoded["type"].(string); !ok {
				t.Fatalf("server reply missing string type: %v", decoded)
			}
		})
	}
}
