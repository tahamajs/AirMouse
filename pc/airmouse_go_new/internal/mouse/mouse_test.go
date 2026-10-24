package mouse

import (
	"testing"
)

func TestInternalMouseController(t *testing.T) {
	ctrl := NewController(1.0)
	if ctrl == nil {
		t.Fatal("Expected non-nil controller")
	}

	ctrl.SetSensitivity(1.2)
	if ctrl.GetSensitivity() != 1.2 {
		t.Errorf("Expected sensitivity 1.2, got %f", ctrl.GetSensitivity())
	}

	ctrl.SetSmoothing(true)
	ctrl.SetAcceleration(true, 1.5)
	ctrl.EnablePredictive(true)
	ctrl.SetPredictiveBlendFactor(0.5)

	ctrl.Move(5.0, 5.0)
	ctrl.Click("left")
	ctrl.Click("right")
	ctrl.DoubleClick()
	ctrl.Scroll(2)

	clicks, dbl, right, scroll := ctrl.Stats()
	if clicks == 0 || right == 0 || dbl == 0 || scroll == 0 {
		t.Errorf("Unexpected stats: %d, %d, %d, %d", clicks, dbl, right, scroll)
	}

	ctrl.ResetStats()
	clicks, dbl, right, scroll = ctrl.Stats()
	if clicks != 0 || dbl != 0 || right != 0 || scroll != 0 {
		t.Errorf("Expected 0 stats after reset")
	}
}
