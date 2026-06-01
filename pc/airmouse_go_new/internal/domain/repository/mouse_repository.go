package repository

import (
	"airmouse-go/internal/domain/entity"
)

// MouseRepository abstracts the actual mouse control hardware.
type MouseRepository interface {
	// Move moves the cursor by delta pixels.
	Move(dx, dy float64) error

	// MoveSmooth moves the cursor along a bezier curve (for human‑like motion).
	MoveSmooth(points []entity.Point, durationMs int) error

	// Click simulates a mouse button click.
	Click(button entity.MouseButton) error

	// DoubleClick simulates a double click on the left button.
	DoubleClick() error

	// ClickAt moves to a position and clicks.
	ClickAt(x, y int, button entity.MouseButton) error

	// Scroll simulates wheel movement.
	Scroll(delta int) error

	// GetPosition returns the current cursor coordinates.
	GetPosition() (x, y int, err error)

	// SetPosition moves the cursor to absolute screen coordinates.
	SetPosition(x, y int) error

	// GetMovementProfile returns the current advanced settings.
	GetMovementProfile() (*entity.MovementProfile, error)

	// SetMovementProfile updates all advanced settings at once.
	SetMovementProfile(profile *entity.MovementProfile) error

	// GetStatistics returns usage counters.
	GetStatistics() (*entity.Statistics, error)

	// ResetStatistics zeroes all counters.
	ResetStatistics() error
}
