package usb

import (
	"encoding/json"
	"testing"
)

func TestUSBServerLifecycle(t *testing.T) {
	server := NewServer(nil, nil)
	if server == nil {
		t.Fatal("Failed to create USB Server")
	}

	server.SetSerialConfig(115200, 8, 1, "none")
	server.EnableAutoDetect(false)

	if err := server.Start(); err != nil {
		t.Fatalf("Start failed: %v", err)
	}

	stats := server.GetStats()
	if stats == nil {
		t.Errorf("Expected stats non-nil")
	}

	server.Stop()
}

func TestUSBGadgetConfig(t *testing.T) {
	cfg := DefaultGadgetConfig()
	if cfg.VendorID == "" || cfg.ProductID == "" {
		t.Errorf("Expected valid USB vendor/product IDs")
	}

	gadget := NewUSBGadget(cfg)
	if gadget == nil {
		t.Fatal("Failed to create USBGadget")
	}

	status := gadget.GetStatus()
	if status == nil {
		t.Errorf("Expected non-nil status")
	}
}

func TestUSBMessageSerialization(t *testing.T) {
	moveRaw, _ := json.Marshal(MovePayload{DX: 10.5, DY: -5.2})
	id := "msg-123"
	msg := USBMessage{
		Type:    "move",
		Payload: moveRaw,
		ID:      &id,
	}

	data, err := json.Marshal(msg)
	if err != nil {
		t.Fatalf("Failed to marshal USBMessage: %v", err)
	}

	var decoded USBMessage
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("Failed to unmarshal USBMessage: %v", err)
	}

	if decoded.Type != "move" || *decoded.ID != "msg-123" {
		t.Errorf("Mismatch in decoded USBMessage: %+v", decoded)
	}
}
