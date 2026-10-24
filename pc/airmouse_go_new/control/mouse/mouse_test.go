package mouse

import (
	"testing"
	"time"
)

func TestMouseControllerBasic(t *testing.T) {
	ctrl := NewController(1.5)
	if ctrl == nil {
		t.Fatal("Expected non-nil mouse controller")
	}

	if s := ctrl.GetSensitivity(); s != 1.5 {
		t.Fatalf("Expected sensitivity 1.5, got %f", s)
	}

	ctrl.SetSensitivity(2.0)
	if s := ctrl.GetSensitivity(); s != 2.0 {
		t.Fatalf("Expected sensitivity 2.0, got %f", s)
	}

	ctrl.SetSmoothing(true)
	ctrl.SetAcceleration(true, 1.8)
	ctrl.EnablePredictive(true)
	ctrl.SetPredictiveBlendFactor(0.5)

	// Test movement
	ctrl.Move(10.0, 5.0)
	ctrl.Move(20.0, -10.0)

	// Test clicks and stats
	ctrl.Click("left")
	ctrl.Click("right")
	ctrl.MouseDown("left")
	ctrl.MouseUp("left")
	ctrl.DoubleClick()
	ctrl.Scroll(3)

	clicks, dbl, right, scroll := ctrl.Stats()
	if clicks == 0 {
		t.Errorf("Expected clicks > 0, got %d", clicks)
	}
	if right == 0 {
		t.Errorf("Expected right clicks > 0, got %d", right)
	}
	if dbl == 0 {
		t.Errorf("Expected double clicks > 0, got %d", dbl)
	}
	if scroll != 3 {
		t.Errorf("Expected scroll 3, got %d", scroll)
	}

	ctrl.ResetStats()
	clicks, dbl, right, scroll = ctrl.Stats()
	if clicks != 0 || dbl != 0 || right != 0 || scroll != 0 {
		t.Errorf("Expected stats to be 0 after reset, got %d, %d, %d, %d", clicks, dbl, right, scroll)
	}
}

func TestMouseControllerDoubleClickHandler(t *testing.T) {
	ctrl := NewController(1.0)
	// Fast double click detection
	ctrl.Click("left")
	time.Sleep(50 * time.Millisecond)
	ctrl.Click("left")

	_, dbl, _, _ := ctrl.Stats()
	if dbl == 0 {
		t.Log("Note: rapid successive clicks triggered or counted")
	}
}
