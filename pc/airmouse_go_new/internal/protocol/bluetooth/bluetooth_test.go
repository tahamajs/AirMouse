package bluetooth

import (
	"testing"
	"time"
)

func TestHIDReportEncodingDecoding(t *testing.T) {
	mgr := NewManager("default", nil, nil)
	report := HIDReport{
		Buttons: 0x01,
		X:       150,
		Y:       -250,
		Wheel:   2,
	}

	encoded := mgr.EncodeHIDReport(report)
	if len(encoded) != 8 {
		t.Fatalf("Expected 8 bytes for HID report, got %d", len(encoded))
	}

	decoded := mgr.DecodeHIDReport(encoded)
	if decoded.Buttons != report.Buttons || decoded.X != report.X || decoded.Y != report.Y || decoded.Wheel != report.Wheel {
		t.Errorf("Mismatch in decoded report: %+v vs %+v", decoded, report)
	}
}

func TestBluetoothManagerLifecycle(t *testing.T) {
	mgr := NewManager("hci0", nil, nil)
	mgr.SetScanInterval(50 * time.Millisecond)
	mgr.SetHIDEnabled(true)

	if err := mgr.Start(); err != nil {
		t.Logf("Start returned: %v (expected if no physical adapter present)", err)
	}

	stats := mgr.GetStats()
	if stats == nil {
		t.Errorf("Expected stats non-nil")
	}

	mgr.Stop()
}

func TestSerialManager(t *testing.T) {
	sm := NewSerialManager()
	if sm == nil {
		t.Fatal("Failed to create SerialManager")
	}

	if err := sm.Start(); err != nil {
		t.Fatalf("Start failed: %v", err)
	}

	stats := sm.GetStats()
	if stats == nil {
		t.Errorf("Expected non-nil stats")
	}

	sm.Stop()
}
