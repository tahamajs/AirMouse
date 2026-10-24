package sysaction

import (
	"testing"
)

func TestGetAvailableActions(t *testing.T) {
	actions := GetAvailableActions()
	if len(actions) == 0 {
		t.Fatal("Expected available actions list to be non-empty")
	}

	for _, a := range actions {
		desc := GetActionDescription(a)
		if desc == "" {
			t.Errorf("Action %s has empty description", a)
		}
	}
}

func TestGetActionDescriptionUnknown(t *testing.T) {
	desc := GetActionDescription(Action("non_existent_action"))
	if desc != "non_existent_action" {
		t.Errorf("Expected 'non_existent_action', got %s", desc)
	}
}

func TestExecuteGestureConfidenceFilter(t *testing.T) {
	// Gesture below confidence threshold (0.5) should be ignored without error
	ExecuteGesture("circle_cw", 0.3)
	// Empty gesture should do nothing
	ExecuteGesture("", 0.9)
}
