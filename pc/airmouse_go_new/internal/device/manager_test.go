package device

import (
	"testing"
)

func TestDeviceManagerLifecycle(t *testing.T) {
	mgr := NewManager()
	defer mgr.Stop()

	// Register device
	devID := "test-dev-1"
	err := mgr.RegisterDevice(devID, TypeWebSocket, "Phone")
	if err != nil {
		t.Fatalf("Failed to register device: %v", err)
	}

	dev := mgr.GetDevice(devID)
	if dev == nil {
		t.Fatalf("Expected device %s found", devID)
	}
	if dev.Name != "Phone" {
		t.Errorf("Expected name 'Phone', got %s", dev.Name)
	}

	// Update activity
	err = mgr.UpdateDeviceActivity(devID, 100, 200)
	if err != nil {
		t.Errorf("Failed to update activity: %v", err)
	}
	dev = mgr.GetDevice(devID)
	if dev.BytesSent != 100 || dev.BytesRecv != 200 {
		t.Errorf("Unexpected bytes: sent=%d, recv=%d", dev.BytesSent, dev.BytesRecv)
	}

	// Update status
	_ = mgr.UpdateDeviceStatus(devID, StatusConnected)
	dev = mgr.GetDevice(devID)
	if dev.Status != StatusConnected {
		t.Errorf("Expected status connected, got %v", dev.Status)
	}

	// Active devices
	active := mgr.GetActiveDevices()
	if len(active) == 0 {
		t.Errorf("Expected at least 1 active device")
	}

	// Block and unblock
	err = mgr.BlockDevice(devID)
	if err != nil {
		t.Errorf("Failed to block device: %v", err)
	}
	if !mgr.IsBlocked(devID) {
		t.Errorf("Expected device to be blocked")
	}

	err = mgr.UnblockDevice(devID)
	if err != nil {
		t.Errorf("Failed to unblock device: %v", err)
	}
	if mgr.IsBlocked(devID) {
		t.Errorf("Expected device to be unblocked")
	}

	// Stats
	stats := mgr.GetStatistics()
	if stats == nil {
		t.Errorf("Expected stats non-nil")
	}

	// Unregister
	err = mgr.UnregisterDevice(devID)
	if err != nil {
		t.Errorf("Failed to unregister: %v", err)
	}
	if mgr.GetDevice(devID) != nil {
		t.Errorf("Expected device to be removed")
	}
}

func TestStableDeviceID(t *testing.T) {
	id1 := StableDeviceID("mac-1", "user-1")
	id2 := StableDeviceID("mac-1", "user-1")
	id3 := StableDeviceID("mac-2", "user-1")

	if id1 != id2 {
		t.Errorf("Expected stable device IDs to match for identical inputs")
	}
	if id1 == id3 {
		t.Errorf("Expected distinct device IDs for different inputs")
	}
}
