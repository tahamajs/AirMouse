package entity

import (
	"math"
	"time"
)

// MouseButton represents a clickable button.
type MouseButton string

const (
	LeftButton    MouseButton = "left"
	RightButton   MouseButton = "right"
	MiddleButton  MouseButton = "middle"
	BackButton    MouseButton = "back"
	ForwardButton MouseButton = "forward"
)

// ScrollDirection represents wheel direction.
type ScrollDirection int

const (
	ScrollUp   ScrollDirection = 1
	ScrollDown ScrollDirection = -1
)

// Movement represents a relative cursor movement delta.
type Movement struct {
	DX, DY float64
	Time   time.Time
}

// ClickEvent represents a single click with optional repeat.
type ClickEvent struct {
	Button    MouseButton
	Repeat    int // number of clicks (1 = single, 2 = double)
	Timestamp int64
}

// ScrollEvent represents a wheel scroll.
type ScrollEvent struct {
	Delta     int // positive = up/forward, negative = down/backward
	Timestamp int64
}

// MovementProfile contains advanced motion parameters.
type MovementProfile struct {
	Sensitivity       float64 `json:"sensitivity"` // 0.2 – 2.0
	Acceleration      bool    `json:"acceleration"`
	AccelerationCurve float64 `json:"acceleration_curve"` // 1.0 = linear, >1 = aggressive
	Deadband          float64 `json:"deadband"`           // minimum movement to ignore (pixels)
	SmoothingAlpha    float64 `json:"smoothing_alpha"`    // EMA factor (0..1)
	PredictiveBlend   float64 `json:"predictive_blend"`   // 0 = raw, 1 = full prediction
}

// DefaultMovementProfile returns a sane default profile.
func DefaultMovementProfile() MovementProfile {
	return MovementProfile{
		Sensitivity:       0.5,
		Acceleration:      true,
		AccelerationCurve: 1.5,
		Deadband:          0.8,
		SmoothingAlpha:    0.3,
		PredictiveBlend:   0.6,
	}
}

// Statistics holds aggregated mouse usage.
type Statistics struct {
	TotalMovement    float64   `json:"total_movement"` // total Euclidean distance (pixels)
	MovementCount    int64     `json:"movement_count"` // number of move commands
	ClickCount       int64     `json:"click_count"`
	DoubleClickCount int64     `json:"double_click_count"`
	RightClickCount  int64     `json:"right_click_count"`
	ScrollCount      int64     `json:"scroll_count"`
	TotalScrollDelta int64     `json:"total_scroll_delta"` // sum of all scroll deltas
	LastReset        time.Time `json:"last_reset"`
}

// NewStatistics creates a fresh stats record.
func NewStatistics() *Statistics {
	return &Statistics{
		LastReset: time.Now(),
	}
}

// AddMovement adds a movement delta to the statistics.
func (s *Statistics) AddMovement(dx, dy float64) {
	s.MovementCount++
	s.TotalMovement += math.Hypot(dx, dy)
}

// AddClick increments the appropriate click counter.
func (s *Statistics) AddClick(button MouseButton, isDouble bool) {
	if isDouble {
		s.DoubleClickCount++
	} else if button == LeftButton {
		s.ClickCount++
	} else if button == RightButton {
		s.RightClickCount++
	}
}

// AddScroll adds a scroll delta.
func (s *Statistics) AddScroll(delta int) {
	s.ScrollCount++
	s.TotalScrollDelta += int64(delta)
}

// Reset zeroes all counters.
func (s *Statistics) Reset() {
	s.TotalMovement = 0
	s.MovementCount = 0
	s.ClickCount = 0
	s.DoubleClickCount = 0
	s.RightClickCount = 0
	s.ScrollCount = 0
	s.TotalScrollDelta = 0
	s.LastReset = time.Now()
}
