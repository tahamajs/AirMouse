package common

import (
	"testing"
	"time"
)

func TestMovementPause(t *testing.T) {
	ClearPause()
	if IsMovementPaused() {
		t.Fatal("Expected movement not paused initially")
	}

	SetMovementPaused(true)
	if !IsMovementPaused() {
		t.Fatal("Expected movement paused after SetMovementPaused(true)")
	}

	SetMovementPaused(false)
	if IsMovementPaused() {
		t.Fatal("Expected movement resumed after SetMovementPaused(false)")
	}

	PauseForDuration(50 * time.Millisecond)
	if !IsMovementPaused() {
		t.Fatal("Expected movement paused during timed pause")
	}

	time.Sleep(60 * time.Millisecond)
	if IsMovementPaused() {
		t.Fatal("Expected timed pause to expire")
	}

	PauseForDuration(1 * time.Second)
	ClearPause()
	if IsMovementPaused() {
		t.Fatal("Expected ClearPause to cancel timed pause")
	}
}

func TestActiveStateAndTheme(t *testing.T) {
	SetActiveMode("presentation")
	if mode := GetActiveMode(); mode != "presentation" {
		t.Fatalf("Expected mode 'presentation', got %s", mode)
	}

	SetActiveTheme("light")
	if theme := GetActiveTheme(); theme != "light" {
		t.Fatalf("Expected theme 'light', got %s", theme)
	}

	SetActiveTheme("matrix")
	if theme := GetActiveTheme(); theme != "matrix" {
		t.Fatalf("Expected theme 'matrix', got %s", theme)
	}
}
