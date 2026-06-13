package entity

import (
    "math"
    "sync"
    "time"
)

// MouseButton represents a clickable button
type MouseButton string

const (
    LeftButton    MouseButton = "left"
    RightButton   MouseButton = "right"
    MiddleButton  MouseButton = "middle"
    BackButton    MouseButton = "back"
    ForwardButton MouseButton = "forward"
)

// ScrollDirection represents wheel direction
type ScrollDirection int

const (
    ScrollUp   ScrollDirection = 1
    ScrollDown ScrollDirection = -1
)

// Movement represents a relative cursor movement delta
type Movement struct {
    DX, DY float64
    Time   time.Time
}

// ClickEvent represents a single click with optional repeat
type ClickEvent struct {
    Button    MouseButton
    Repeat    int
    Timestamp int64
}

// ScrollEvent represents a wheel scroll
type ScrollEvent struct {
    Delta     int
    Timestamp int64
}

// MovementProfile contains advanced motion parameters
type MovementProfile struct {
    Sensitivity        float64 `json:"sensitivity"`
    Acceleration       bool    `json:"acceleration"`
    AccelerationCurve  float64 `json:"acceleration_curve"`
    Deadband           float64 `json:"deadband"`
    SmoothingAlpha     float64 `json:"smoothing_alpha"`
    PredictiveBlend    float64 `json:"predictive_blend"`
    MaxSpeed           float64 `json:"max_speed"`
    MinSpeed           float64 `json:"min_speed"`
    InvertX            bool    `json:"invert_x"`
    InvertY            bool    `json:"invert_y"`
    SwapAxes           bool    `json:"swap_axes"`
    
    mu sync.RWMutex
}

// DefaultMovementProfile returns a sane default profile
func DefaultMovementProfile() *MovementProfile {
    return &MovementProfile{
        Sensitivity:       1.0,
        Acceleration:      true,
        AccelerationCurve: 1.5,
        Deadband:          0.5,
        SmoothingAlpha:    0.3,
        PredictiveBlend:   0.6,
        MaxSpeed:          100.0,
        MinSpeed:          0.5,
        InvertX:           false,
        InvertY:           false,
        SwapAxes:          false,
    }
}

// ApplyProfile applies movement transformations
func (p *MovementProfile) Apply(dx, dy float64) (float64, float64) {
    p.mu.RLock()
    defer p.mu.RUnlock()
    
    // Swap axes if needed
    if p.SwapAxes {
        dx, dy = dy, dx
    }
    
    // Invert axes if needed
    if p.InvertX {
        dx = -dx
    }
    if p.InvertY {
        dy = -dy
    }
    
    // Apply sensitivity
    dx *= p.Sensitivity
    dy *= p.Sensitivity
    
    // Apply deadband
    if math.Abs(dx) < p.Deadband {
        dx = 0
    }
    if math.Abs(dy) < p.Deadband {
        dy = 0
    }
    
    // Apply speed limits
    speed := math.Hypot(dx, dy)
    if speed > p.MaxSpeed {
        scale := p.MaxSpeed / speed
        dx *= scale
        dy *= scale
    }
    
    // Apply acceleration
    if p.Acceleration && speed > p.MinSpeed {
        factor := 1.0 + (p.AccelerationCurve-1.0)*(speed/50.0)
        if factor > 3.0 {
            factor = 3.0
        }
        dx *= factor
        dy *= factor
    }
    
    return dx, dy
}

// Statistics holds aggregated mouse usage
type Statistics struct {
    TotalMovement    float64   `json:"total_movement"`
    MovementCount    int64     `json:"movement_count"`
    ClickCount       int64     `json:"click_count"`
    DoubleClickCount int64     `json:"double_click_count"`
    RightClickCount  int64     `json:"right_click_count"`
    MiddleClickCount int64     `json:"middle_click_count"`
    ScrollCount      int64     `json:"scroll_count"`
    TotalScrollDelta int64     `json:"total_scroll_delta"`
    LastReset        time.Time `json:"last_reset"`
    SessionDuration  float64   `json:"session_duration"`
    
    mu sync.RWMutex
}

// NewStatistics creates a fresh stats record
func NewStatistics() *Statistics {
    return &Statistics{
        LastReset: time.Now(),
    }
}

// AddMovement adds a movement delta
func (s *Statistics) AddMovement(dx, dy float64) {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.MovementCount++
    s.TotalMovement += math.Hypot(dx, dy)
}

// AddClick increments click counter
func (s *Statistics) AddClick(button MouseButton, isDouble bool) {
    s.mu.Lock()
    defer s.mu.Unlock()
    if isDouble {
        s.DoubleClickCount++
    } else {
        switch button {
        case LeftButton:
            s.ClickCount++
        case RightButton:
            s.RightClickCount++
        case MiddleButton:
            s.MiddleClickCount++
        }
    }
}

// AddScroll adds scroll delta
func (s *Statistics) AddScroll(delta int) {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.ScrollCount++
    s.TotalScrollDelta += int64(delta)
}

// Reset zeroes all counters
func (s *Statistics) Reset() {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.TotalMovement = 0
    s.MovementCount = 0
    s.ClickCount = 0
    s.DoubleClickCount = 0
    s.RightClickCount = 0
    s.MiddleClickCount = 0
    s.ScrollCount = 0
    s.TotalScrollDelta = 0
    s.LastReset = time.Now()
}

// GetStats returns a copy of statistics
func (s *Statistics) GetStats() (totalMovement float64, movementCount, clickCount, doubleClickCount, rightClickCount, scrollCount int64) {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return s.TotalMovement, s.MovementCount, s.ClickCount, s.DoubleClickCount, s.RightClickCount, s.ScrollCount
}