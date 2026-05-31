package service

import (
	"errors"
	"math"
	"sync"
	"time"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
)

// GestureService handles gesture detection and custom gestures.
type GestureService struct {
	repo             repository.GestureRepository
	clickThreshold   float64
	scrollThreshold  float64
	tiltThreshold    float64
	lastClickTime    time.Time
	clickCount       int
	lastScrollTime   time.Time
	scrollDebounce   time.Duration
	mu               sync.Mutex
}

// NewGestureService creates a new gesture service.
func NewGestureService(repo repository.GestureRepository) (*GestureService, error) {
	click, scroll, tilt, err := repo.GetGestureThresholds()
	if err != nil {
		// Default values
		click, scroll, tilt = 10.0, 5.0, 15.0
	}
	return &GestureService{
		repo:            repo,
		clickThreshold:  click,
		scrollThreshold: scroll,
		tiltThreshold:   tilt,
		scrollDebounce:  200 * time.Millisecond,
	}, nil
}

// DetectGesture determines the gesture from gyroscope and accelerometer data.
func (s *GestureService) DetectGesture(gyroY, accelY float64, dt float64) entity.GestureType {
	s.mu.Lock()
	defer s.mu.Unlock()

	now := time.Now()

	// Click detection (quick rotation around Y axis)
	if math.Abs(gyroY) > s.clickThreshold {
		if now.Sub(s.lastClickTime) < 300*time.Millisecond {
			s.clickCount++
			if s.clickCount >= 2 {
				s.clickCount = 0
				s.lastClickTime = time.Time{}
				return entity.GestureDoubleClick
			}
		} else {
			s.clickCount = 1
			s.lastClickTime = now
			return entity.GestureClick
		}
	}

	// Scroll detection (quick linear movement along Y axis)
	if math.Abs(accelY) > s.scrollThreshold && now.Sub(s.lastScrollTime) > s.scrollDebounce {
		s.lastScrollTime = now
		if accelY > 0 {
			return entity.GestureScrollDown
		}
		return entity.GestureScrollUp
	}

	return entity.GestureNone
}

// DetectSwipe detects swipe gestures from large deltas (used in touchpad mode).
func (s *GestureService) DetectSwipe(dx, dy float64) entity.GestureType {
	if math.Abs(dx) > 50 && math.Abs(dy) < 20 {
		if dx > 0 {
			return entity.GestureSwipeRight
		}
		return entity.GestureSwipeLeft
	}
	return entity.GestureNone
}

// SaveCustomGesture stores a custom gesture template.
func (s *GestureService) SaveCustomGesture(name string, template []float64) error {
	if name == "" {
		return errors.New("gesture name cannot be empty")
	}
	if len(template) == 0 {
		return errors.New("template cannot be empty")
	}
	return s.repo.SaveCustomGesture(name, template)
}

// LoadCustomGesture retrieves a gesture template.
func (s *GestureService) LoadCustomGesture(name string) ([]float64, error) {
	return s.repo.LoadCustomGesture(name)
}

// ListCustomGestures returns all custom gesture names.
func (s *GestureService) ListCustomGestures() ([]string, error) {
	return s.repo.ListCustomGestures()
}

// DeleteCustomGesture removes a custom gesture.
func (s *GestureService) DeleteCustomGesture(name string) error {
	return s.repo.DeleteCustomGesture(name)
}

// UpdateThresholds updates the detection thresholds.
func (s *GestureService) UpdateThresholds(click, scroll, tilt float64) error {
	if click <= 0 || scroll <= 0 || tilt <= 0 {
		return errors.New("thresholds must be positive")
	}
	s.clickThreshold = click
	s.scrollThreshold = scroll
	s.tiltThreshold = tilt
	return s.repo.SetGestureThresholds(click, scroll, tilt)
}