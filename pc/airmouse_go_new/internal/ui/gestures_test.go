package ui

import (
	"testing"

	"fyne.io/fyne/v2/widget"
)

func TestGesturesTabFiltering(t *testing.T) {
	tab := &GesturesTab{
		templates: []GestureTemplate{
			{Name: "ThumbsUp", Type: "Hand", Action: "Play/Pause"},
			{Name: "LeftSwipe", Type: "Swipe", Action: "Previous Track"},
			{Name: "CircleCW", Type: "Circular", Action: "Volume Up"},
		},
		searchEntry: widget.NewEntry(),
		filterType:  widget.NewSelect([]string{"All", "Hand", "Swipe", "Circular", "Pinch"}, nil),
	}
	tab.filterType.SetSelected("All")

	if got := len(tab.getFilteredTemplates()); got != 3 {
		t.Fatalf("expected 3 templates, got %d", got)
	}

	tab.filterType.SetSelected("Swipe")
	if got := len(tab.getFilteredTemplates()); got != 1 {
		t.Fatalf("expected 1 swipe template, got %d", got)
	}

	tab.searchEntry.SetText("volume")
	tab.filterType.SetSelected("All")
	if got := len(tab.getFilteredTemplates()); got != 1 {
		t.Fatalf("expected 1 template matching search, got %d", got)
	}
}
