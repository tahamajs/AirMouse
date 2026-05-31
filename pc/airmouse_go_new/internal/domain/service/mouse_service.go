package service

import (
	"errors"
	"math"
	"sync"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// MouseController defines the interface for platform‑specific mouse actions.
type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
}

// MouseService contains business logic for mouse control and delegates to infra.
type MouseService struct {
	repo       repository.MouseRepository
	controller MouseController
	mouse      *entity.Mouse
	mu         sync.Mutex
}

// NewMouseService creates a new mouse service.
func NewMouseService(repo repository.MouseRepository, ctrl MouseController, sensitivity float64) *MouseService {
	return &MouseService{
		repo:       repo,
		controller: ctrl,
		mouse:      entity.NewMouse(sensitivity),
	}
}

// Move processes raw deltas, applies smoothing/acceleration, and moves the cursor.
func (s *MouseService) Move(rawDx, rawDy float64, dt float64) (finalDx, finalDy float64, err error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.mouse == nil {
		return 0, 0, errors.New("mouse not initialised")
	}
	dx, dy := s.mouse.Move(rawDx, rawDy, dt)
	s.controller.Move(dx, dy)
	return dx, dy, nil
}

// Click performs a left or right click.
func (s *MouseService) Click(button string) error {
	s.controller.Click(button)
	return s.repo.IncrementClick()
}

// DoubleClick performs a double click.
func (s *MouseService) DoubleClick() error {
	s.controller.DoubleClick()
	return s.repo.IncrementDoubleClick()
}

// RightClick performs a right click.
func (s *MouseService) RightClick() error {
	s.controller.Click("right")
	return s.repo.IncrementRightClick()
}

// Scroll performs a scroll action.
func (s *MouseService) Scroll(delta int) error {
	s.controller.Scroll(delta)
	return s.repo.IncrementScroll()
}

// GetStatistics returns the current statistics.
func (s *MouseService) GetStatistics() (clicks, dbl, right, scroll int64, err error) {
	return s.repo.GetStats()
}

// SetSensitivity updates the cursor sensitivity.
func (s *MouseService) SetSensitivity(sensitivity float64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.mouse != nil {
		s.mouse.Sensitivity = sensitivity
	}
}

// SetSmoothing enables or disables movement smoothing.
func (s *MouseService) SetSmoothing(enabled bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.mouse != nil {
		s.mouse.Smoothing = enabled
	}
}

// SetAcceleration configures acceleration.
func (s *MouseService) SetAcceleration(enabled bool, factor float64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.mouse != nil {
		s.mouse.Acceleration = enabled
		s.mouse.AccelFactor = factor
	}
}