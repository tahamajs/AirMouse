package repository

import (
	"airmouse-go/internal/domain/entity"
)

// MouseRepository defines the interface for accessing mouse state.
type MouseRepository interface {
	// Save persists the current mouse state.
	Save(mouse *entity.Mouse) error

	// Load retrieves the last saved mouse state.
	Load() (*entity.Mouse, error)

	// GetStats returns the current statistics (clicks, scrolls, etc.).
	GetStats() (clicks, double, right, scroll int64, err error)

	// IncrementClick increments the click counter.
	IncrementClick() error

	// IncrementDoubleClick increments the double click counter.
	IncrementDoubleClick() error

	// IncrementRightClick increments the right click counter.
	IncrementRightClick() error

	// IncrementScroll increments the scroll counter.
	IncrementScroll() error
}