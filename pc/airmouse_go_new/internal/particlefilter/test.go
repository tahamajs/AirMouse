package particlefilter

import "testing"

func TestFilter(t *testing.T) {
	f := NewFilter(100)
	for i := 0; i < 50; i++ {
		f.Predict(0.02)
		f.Update(1.0, 1.0)
	}
	x, y := f.GetBestEstimate()
	t.Logf("Best estimate: (%.2f, %.2f)", x, y)
}

func TestRecognizer(t *testing.T) {
	r := NewRecognizer()
	for i := 0; i < 10; i++ {
		r.AddMotion(-8.0, 0.0)
	}
	g, c := r.GetGesture()
	if g != "swipe_left" {
		t.Errorf("Expected swipe_left, got %s", g)
	}
	t.Logf("Recognised %s with confidence %.2f", g, c)
}
