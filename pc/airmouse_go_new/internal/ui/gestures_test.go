package ui

import "testing"

func TestGesturesTabFiltering(t *testing.T) {
	templates := []GestureTemplate{
		{Name: "ThumbsUp", Type: "Hand", Action: "Play/Pause"},
		{Name: "LeftSwipe", Type: "Swipe", Action: "Previous Track"},
		{Name: "CircleCW", Type: "Circular", Action: "Volume Up"},
	}

	if got := len(filterGestureTemplates(templates, "", "All")); got != 3 {
		t.Fatalf("expected 3 templates, got %d", got)
	}

	if got := len(filterGestureTemplates(templates, "", "Swipe")); got != 1 {
		t.Fatalf("expected 1 swipe template, got %d", got)
	}

	if got := len(filterGestureTemplates(templates, "volume", "All")); got != 1 {
		t.Fatalf("expected 1 template matching search, got %d", got)
	}
}
