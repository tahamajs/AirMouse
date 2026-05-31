package repository

import (
	"airmouse-go/internal/domain/entity"
)

// GestureRepository defines the interface for gesture management.
type GestureRepository interface {
	// SaveCustomGesture stores a custom gesture template.
	SaveCustomGesture(name string, template []float64) error

	// LoadCustomGesture retrieves a gesture template by name.
	LoadCustomGesture(name string) ([]float64, error)

	// ListCustomGestures returns all custom gesture names.
	ListCustomGestures() ([]string, error)

	// DeleteCustomGesture removes a custom gesture.
	DeleteCustomGesture(name string) error

	// GetGestureThresholds returns the current thresholds for click, scroll, etc.
	GetGestureThresholds() (clickThreshold, scrollThreshold, tiltThreshold float64, err error)

	// SetGestureThresholds updates the gesture thresholds.
	SetGestureThresholds(click, scroll, tilt float64) error
}