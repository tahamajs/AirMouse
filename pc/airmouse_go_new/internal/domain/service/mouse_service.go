package service

import (
	"errors"
	"math"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// MouseService contains the business logic for mouse control.
type MouseService struct {
	repo      repository.MouseRepository
	mouse     *entity.Mouse
}

// NewMouseService creates a new mouse service.
func NewMouseService(repo repository.MouseRepository, sensitivity float64) *MouseService {
	return &MouseService{
		repo:  repo,
		mouse: entity.NewMouse(sensitivity),
	}
}

// Move processes raw movement deltas and returns the final movement to apply.
func (s *MouseService) Move(dx, dy float64, dt float64) (finalDx, finalDy float64, err error) {
	if s.mouse == nil {
		return 0, 0, errors.New("mouse not initialised")
	}
	return s.mouse.Move(dx, dy, dt), nil
}

// Click registers a click action.
func (s *MouseService) Click(button string) error {
	if err := s.repo.IncrementClick(); err != nil {
		return err
	}
	// Additional business logic (e.g., log click)
	return nil
}

// DoubleClick registers a double click action.
func (s *MouseService) DoubleClick() error {
	return s.repo.IncrementDoubleClick()
}

// RightClick registers a right click action.
func (s *MouseService) RightClick() error {
	return s.repo.IncrementRightClick()
}

// Scroll registers a scroll action.
func (s *MouseService) Scroll(delta int) error {
	return s.repo.IncrementScroll()
}

// GetStatistics returns the current statistics.
func (s *MouseService) GetStatistics() (clicks, double, right, scroll int64, err error) {
	return s.repo.GetStats()
}

// SetSensitivity updates the cursor sensitivity.
func (s *MouseService) SetSensitivity(sensitivity float64) {
	if s.mouse != nil {
		s.mouse.Sensitivity = sensitivity
	}
}

// SetSmoothing enables or disables movement smoothing.
func (s *MouseService) SetSmoothing(enabled bool) {
	if s.mouse != nil {
		s.mouse.Smoothing = enabled
	}
}

// SetAcceleration configures acceleration.
func (s *MouseService) SetAcceleration(enabled bool, factor float64) {
	if s.mouse != nil {
		s.mouse.Acceleration = enabled
		s.mouse.AccelFactor = factor
	}
}

// Reset resets the mouse state.
func (s *MouseService) Reset() {
	if s.mouse != nil {
		s.mouse.Reset()
	}
}