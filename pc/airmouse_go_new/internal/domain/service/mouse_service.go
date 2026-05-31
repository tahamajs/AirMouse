package service

import (
	"math"
	"sync"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// MouseService implements advanced mouse control logic with smoothing, prediction, and deadband.
type MouseService interface {
	// Move sends a relative movement after applying sensitivity, deadband, smoothing, and acceleration.
	Move(dx, dy float64) error

	// MoveRaw bypasses all filtering (for direct control).
	MoveRaw(dx, dy float64) error

	// Click sends a button click.
	Click(button entity.MouseButton) error

	// DoubleClick sends a double click.
	DoubleClick() error

	// RightClick is a convenience method.
	RightClick() error

	// Scroll sends a wheel delta.
	Scroll(delta int) error

	// GetStats returns usage statistics.
	GetStats() (*entity.Statistics, error)

	// ResetStats resets usage counters.
	ResetStats() error

	// SetSensitivity changes the global sensitivity factor.
	SetSensitivity(sensitivity float64)

	// SetProfile replaces the entire movement profile.
	SetProfile(profile *entity.MovementProfile) error

	// GetProfile returns the current movement profile.
	GetProfile() (*entity.MovementProfile, error)
}

type mouseService struct {
	repo    repository.MouseRepository
	profile *entity.MovementProfile

	// Internal state for smoothing
	lastX, lastY float64
	mu           sync.Mutex
}

func NewMouseService(repo repository.MouseRepository, initialProfile *entity.MovementProfile) MouseService {
	if initialProfile == nil {
		initialProfile = entity.DefaultMovementProfile()
	}
	return &mouseService{
		repo:    repo,
		profile: initialProfile,
	}
}

func (s *mouseService) Move(dx, dy float64) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	// 1. Sensitivity
	dx *= s.profile.Sensitivity
	dy *= s.profile.Sensitivity

	// 2. Deadband
	if math.Abs(dx) < s.profile.Deadband {
		dx = 0
	}
	if math.Abs(dy) < s.profile.Deadband {
		dy = 0
	}
	if dx == 0 && dy == 0 {
		return nil
	}

	// 3. Smoothing (EMA)
	if s.profile.SmoothingAlpha > 0 {
		s.lastX = s.profile.SmoothingAlpha*dx + (1-s.profile.SmoothingAlpha)*s.lastX
		s.lastY = s.profile.SmoothingAlpha*dy + (1-s.profile.SmoothingAlpha)*s.lastY
		dx, dy = s.lastX, s.lastY
	}

	// 4. Acceleration (non‑linear scaling)
	if s.profile.Acceleration {
		speed := math.Hypot(dx, dy)
		if speed > 5 {
			factor := 1.0 + (s.profile.AccelerationCurve-1.0)*(speed/50.0)
			if factor > 3.0 {
				factor = 3.0
			}
			dx *= factor
			dy *= factor
		}
	}

	// 5. Predictive blending (placeholder – real implementation uses Kalman)
	if s.profile.PredictiveBlend > 0 {
		// predictedDx, predictedDy := predictor.Predict()
		// dx = (1 - blend)*dx + blend*predictedDx
	}

	return s.repo.Move(dx, dy)
}

func (s *mouseService) MoveRaw(dx, dy float64) error {
	return s.repo.Move(dx, dy)
}

func (s *mouseService) Click(button entity.MouseButton) error {
	return s.repo.Click(button)
}

func (s *mouseService) DoubleClick() error {
	return s.repo.DoubleClick()
}

func (s *mouseService) RightClick() error {
	return s.repo.Click(entity.RightButton)
}

func (s *mouseService) Scroll(delta int) error {
	return s.repo.Scroll(delta)
}

func (s *mouseService) GetStats() (*entity.Statistics, error) {
	return s.repo.GetStatistics()
}

func (s *mouseService) ResetStats() error {
	return s.repo.ResetStatistics()
}

func (s *mouseService) SetSensitivity(sensitivity float64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if sensitivity < 0.2 {
		sensitivity = 0.2
	}
	if sensitivity > 2.0 {
		sensitivity = 2.0
	}
	s.profile.Sensitivity = sensitivity
	_ = s.repo.SetMovementProfile(s.profile)
}

func (s *mouseService) SetProfile(profile *entity.MovementProfile) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.profile = profile
	return s.repo.SetMovementProfile(profile)
}

func (s *mouseService) GetProfile() (*entity.MovementProfile, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.profile, nil
}