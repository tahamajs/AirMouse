package proximity

import (
	"testing"
	"time"
)

func TestProximityManager(t *testing.T) {
	mgr := NewManager()
	if mgr == nil {
		t.Fatal("Failed to create proximity Manager")
	}

	mgr.EnableAutoLock(false)
	mgr.EnableAutoUnlock(false)
	mgr.SetGlobalThresholds(1.0, 3.0)

	devID := "phone-prox-1"
	mgr.ProcessUpdate(ProximityUpdate{
		DeviceID:  devID,
		IsNear:    true,
		Distance:  0.8,
		RSSI:      -50,
		Timestamp: time.Now().Unix(),
	})

	state := mgr.GetProximityState(devID)
	if state == nil {
		t.Fatalf("Expected state for device %s", devID)
	}
	if !state.IsNear {
		t.Errorf("Expected IsNear to be true")
	}

	all := mgr.GetAllStates()
	if len(all) != 1 {
		t.Errorf("Expected 1 state, got %d", len(all))
	}

	stats := mgr.GetStatistics()
	if stats == nil {
		t.Errorf("Expected statistics non-nil")
	}

	mgr.RemoveDevice(devID)
	if mgr.GetProximityState(devID) != nil {
		t.Errorf("Expected device state to be removed")
	}
}

func TestRSSIFusion(t *testing.T) {
	fusion := NewRSSIFusion(-59, 2.0)
	if fusion == nil {
		t.Fatal("Failed to create RSSIFusion")
	}

	dist := fusion.RSSIToDistance(-59)
	if dist <= 0 {
		t.Errorf("Expected positive distance, got %f", dist)
	}

	rssi := fusion.DistanceToRSSI(1.0)
	if rssi == 0 {
		t.Errorf("Expected valid RSSI, got %d", rssi)
	}

	conf := fusion.GetConfidence(-60)
	if conf <= 0 || conf > 1.0 {
		t.Errorf("Expected confidence in (0, 1], got %f", conf)
	}

	smoothed := fusion.AdaptiveSmoothing(-60)
	if smoothed == 0 {
		t.Errorf("Expected smoothed value non-zero")
	}
}
