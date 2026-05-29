package control

import (
	"math"
	"sync/atomic"
	"time"
)

type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
	Stats() (clicks, dbl, right, scroll int64)
	SetSensitivity(s float64)
	GetSensitivity() float64
	SetSmoothing(enabled bool)
	SetAcceleration(enabled bool, factor float64)
}

type GestureType int

const (
	GestureNone GestureType = iota
	GestureClick
	GestureDoubleClick
	GestureRightClick
	GestureScrollUp
	GestureScrollDown
	GestureSwipeLeft
	GestureSwipeRight
	GestureCustom
)

type Gesture struct {
	Type   GestureType
	Value  float64
	Custom string
}

type gestureDetector struct {
	lastX, lastY      float64
	lastTime          time.Time
	clickCount        int
	lastClickTime     time.Time
	doubleClickWindow time.Duration
	swipeThreshold    float64
	moveHistory       []movementSample
}

type movementSample struct {
	X, Y  float64
	Time  time.Time
}

type mouseController struct {
	sensitivity    float64
	clickCount     int64
	doubleClickCnt int64
	rightClickCnt  int64
	scrollCount    int64
	
	// Advanced features
	smoothing      bool
	acceleration   bool
	accelFactor    float64
	gesture        *gestureDetector
	
	// Performance
	lastMoveTime   time.Time
	moveRateLimit  int
	moveCount      int
}

const (
	MAX_MOVE_HISTORY = 10
	MIN_MOVE_DELTA   = 0.01
)

func NewMouseController(sensitivity float64) MouseController {
	m := &mouseController{
		sensitivity:    sensitivity,
		smoothing:      true,
		acceleration:   false,
		accelFactor:    1.5,
		moveRateLimit:  60,
		gesture: &gestureDetector{
			doubleClickWindow: 300 * time.Millisecond,
			swipeThreshold:    100.0,
			moveHistory:       make([]movementSample, 0, MAX_MOVE_HISTORY),
		},
	}
	return m
}

func (m *mouseController) SetSmoothing(enabled bool) {
	m.smoothing = enabled
}

func (m *mouseController) SetAcceleration(enabled bool, factor float64) {
	m.acceleration = enabled
	m.accelFactor = factor
}

func (m *mouseController) GetSensitivity() float64 {
	return m.sensitivity
}

func (m *mouseController) applySmoothing(dx, dy float64) (float64, float64) {
	if !m.smoothing {
		return dx, dy
	}
	
	// Apply exponential moving average
	const alpha = 0.3
	m.gesture.lastX = alpha*dx + (1-alpha)*m.gesture.lastX
	m.gesture.lastY = alpha*dy + (1-alpha)*m.gesture.lastY
	
	return m.gesture.lastX, m.gesture.lastY
}

func (m *mouseController) applyAcceleration(dx, dy float64) (float64, float64) {
	if !m.acceleration {
		return dx, dy
	}
	
	speed := math.Sqrt(dx*dx + dy*dy)
	if speed > 5 {
		factor := 1.0 + m.accelFactor*(speed/50.0)
		if factor > 3.0 {
			factor = 3.0
		}
		dx *= factor
		dy *= factor
	}
	
	return dx, dy
}

func (m *mouseController) rateLimit() bool {
	now := time.Now()
	if now.Sub(m.lastMoveTime) > time.Second {
		m.moveCount = 0
		m.lastMoveTime = now
	}
	
	if m.moveCount >= m.moveRateLimit {
		return true
	}
	
	m.moveCount++
	return false
}

func (m *mouseController) detectGesture(dx, dy float64) GestureType {
	// Add to history
	m.gesture.moveHistory = append(m.gesture.moveHistory, movementSample{
		X: dx, Y: dy, Time: time.Now(),
	})
	if len(m.gesture.moveHistory) > MAX_MOVE_HISTORY {
		m.gesture.moveHistory = m.gesture.moveHistory[1:]
	}
	
	// Detect swipe
	if math.Abs(dx) > m.gesture.swipeThreshold && math.Abs(dy) < m.gesture.swipeThreshold/2 {
		if dx > 0 {
			return GestureSwipeRight
		}
		return GestureSwipeLeft
	}
	
	return GestureNone
}

func (m *mouseController) Move(dx, dy float64) {
	if m.rateLimit() {
		return
	}
	
	// Apply sensitivity
	dx *= m.sensitivity
	dy *= m.sensitivity
	
	// Apply processing
	dx, dy = m.applySmoothing(dx, dy)
	dx, dy = m.applyAcceleration(dx, dy)
	
	// Minimum movement threshold
	if math.Abs(dx) < MIN_MOVE_DELTA && math.Abs(dy) < MIN_MOVE_DELTA {
		return
	}
	
	// Detect gestures
	gesture := m.detectGesture(dx, dy)
	if gesture != GestureNone {
		logger.Debug("Gesture detected", "type", gesture)
	}
	
	// Execute movement
	m.executeMove(dx, dy)
}

func (m *mouseController) Click(button string) {
	now := time.Now()
	
	// Detect double click
	if button == "left" {
		if now.Sub(m.gesture.lastClickTime) < m.gesture.doubleClickWindow {
			m.gesture.clickCount++
			if m.gesture.clickCount >= 2 {
				m.DoubleClick()
				m.gesture.clickCount = 0
				m.gesture.lastClickTime = time.Time{}
				return
			}
		} else {
			m.gesture.clickCount = 1
			m.gesture.lastClickTime = now
		}
	}
	
	m.executeClick(button)
	
	if button == "left" {
		atomic.AddInt64(&m.clickCount, 1)
	} else if button == "right" {
		atomic.AddInt64(&m.rightClickCnt, 1)
	}
}

func (m *mouseController) DoubleClick() {
	m.executeDoubleClick()
	atomic.AddInt64(&m.doubleClickCnt, 1)
}

func (m *mouseController) Scroll(delta int) {
	m.executeScroll(delta)
	atomic.AddInt64(&m.scrollCount, 1)
}

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}

func (m *mouseController) SetSensitivity(s float64) {
	m.sensitivity = s
}package control

import (
	"math"
	"sync/atomic"
	"time"
)

type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
	Stats() (clicks, dbl, right, scroll int64)
	SetSensitivity(s float64)
	GetSensitivity() float64
	SetSmoothing(enabled bool)
	SetAcceleration(enabled bool, factor float64)
}

type mouseController struct {
	sensitivity    float64
	clickCount     int64
	doubleClickCnt int64
	rightClickCnt  int64
	scrollCount    int64

	smoothing    bool
	acceleration bool
	accelFactor  float64

	lastX, lastY   float64
	lastMoveTime   time.Time
	moveCount      int
	moveRateLimit  int

	lastClickTime   time.Time
	clickCountWindow int
}

const (
	minMoveDelta   = 0.01
	rateLimitPerSec = 60
)

func NewMouseController(sensitivity float64) MouseController {
	return &mouseController{
		sensitivity:   sensitivity,
		smoothing:     true,
		acceleration:  false,
		accelFactor:   1.5,
		moveRateLimit: rateLimitPerSec,
	}
}

func (m *mouseController) SetSmoothing(enabled bool) {
	m.smoothing = enabled
}

func (m *mouseController) SetAcceleration(enabled bool, factor float64) {
	m.acceleration = enabled
	m.accelFactor = factor
}

func (m *mouseController) GetSensitivity() float64 {
	return m.sensitivity
}

func (m *mouseController) Move(dx, dy float64) {
	// Rate limiting
	now := time.Now()
	if now.Sub(m.lastMoveTime) > time.Second {
		m.moveCount = 0
		m.lastMoveTime = now
	}
	m.moveCount++
	if m.moveCount > m.moveRateLimit {
		return
	}

	// Apply sensitivity
	dx *= m.sensitivity
	dy *= m.sensitivity

	// Smoothing (EMA)
	if m.smoothing {
		const alpha = 0.3
		m.lastX = alpha*dx + (1-alpha)*m.lastX
		m.lastY = alpha*dy + (1-alpha)*m.lastY
		dx, dy = m.lastX, m.lastY
	}

	// Acceleration
	if m.acceleration {
		speed := math.Sqrt(dx*dx + dy*dy)
		if speed > 5 {
			factor := 1.0 + m.accelFactor*(speed/50.0)
			if factor > 3.0 {
				factor = 3.0
			}
			dx *= factor
			dy *= factor
		}
	}

	// Minimum movement
	if math.Abs(dx) < minMoveDelta && math.Abs(dy) < minMoveDelta {
		return
	}

	m.executeMove(dx, dy)
}

func (m *mouseController) Click(button string) {
	// Double-click detection
	if button == "left" {
		now := time.Now()
		if now.Sub(m.lastClickTime) < 300*time.Millisecond {
			m.clickCountWindow++
			if m.clickCountWindow >= 2 {
				m.DoubleClick()
				m.clickCountWindow = 0
				m.lastClickTime = time.Time{}
				return
			}
		} else {
			m.clickCountWindow = 1
			m.lastClickTime = now
		}
	}

	m.executeClick(button)
	if button == "left" {
		atomic.AddInt64(&m.clickCount, 1)
	} else if button == "right" {
		atomic.AddInt64(&m.rightClickCnt, 1)
	}
}

func (m *mouseController) DoubleClick() {
	m.executeDoubleClick()
	atomic.AddInt64(&m.doubleClickCnt, 1)
}

func (m *mouseController) Scroll(delta int) {
	m.executeScroll(delta)
	atomic.AddInt64(&m.scrollCount, 1)
}

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}

// The following methods are implemented per-platform (see mouse_*.go)
func (m *mouseController) SetSensitivity(s float64) {
	m.sensitivity = s
}


package control

import (
	"sync/atomic"
	"time"
)

type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
	Stats() (clicks, dbl, right, scroll int64)
	SetSensitivity(s float64)
	GetSensitivity() float64
	SetSmoothing(enabled bool)
	SetAcceleration(enabled bool, factor float64)
	// AI smoothing support
	EnableAISmoothing(enabled bool)
}

type mouseController struct {
	sensitivity     float64
	clickCount      int64
	doubleClickCnt  int64
	rightClickCnt   int64
	scrollCount     int64
	smoothing       bool
	acceleration    bool
	accelFactor     float64
	lastX, lastY    float64
	lastMoveTime    time.Time
	moveCount       int
	moveRateLimit   int
	lastClickTime   time.Time
	clickCountWin   int
	// AI smoothing
	aiSmoother      *AISmoother
	aiEnabled       bool
	aiBlendFactor   float64 // 0=raw, 1=AI only
}

const (
	minMoveDelta     = 0.01
	rateLimitPerSec  = 60
)

func NewMouseController(sensitivity float64) MouseController {
	m := &mouseController{
		sensitivity:    sensitivity,
		smoothing:      true,
		acceleration:   false,
		accelFactor:    1.5,
		moveRateLimit:  rateLimitPerSec,
		aiEnabled:      false,
		aiBlendFactor:  0.6,
	}
	return m
}

// SetAISmoother injects the AI smoother (must be called after creation)
func (m *mouseController) SetAISmoother(s *AISmoother) {
	m.aiSmoother = s
}

func (m *mouseController) EnableAISmoothing(enabled bool) {
	m.aiEnabled = enabled
}

func (m *mouseController) SetSmoothing(enabled bool) {
	m.smoothing = enabled
}

func (m *mouseController) SetAcceleration(enabled bool, factor float64) {
	m.acceleration = enabled
	m.accelFactor = factor
}

func (m *mouseController) GetSensitivity() float64 {
	return m.sensitivity
}

func (m *mouseController) Move(dx, dy float64) {
	// Rate limiting
	now := time.Now()
	if now.Sub(m.lastMoveTime) > time.Second {
		m.moveCount = 0
		m.lastMoveTime = now
	}
	m.moveCount++
	if m.moveCount > m.moveRateLimit {
		return
	}

	// Apply sensitivity
	dx *= m.sensitivity
	dy *= m.sensitivity

	// Apply smoothing (if no AI or fallback)
	if !m.aiEnabled {
		dx, dy = m.applySmoothing(dx, dy)
	} else {
		// AI path: blend with predicted delta
		if m.aiSmoother != nil {
			// Add current position to AI history (need to get actual cursor pos)
			// In a real implementation, we would get current cursor coordinates.
			// Here we simulate by accumulating deltas.
			// For demo, we call AddPoint with dummy coordinates.
			// In production, call m.aiSmoother.AddPoint(realX, realY) inside the platform-specific move.
		}
		predDx, predDy, err := m.aiSmoother.PredictDelta()
		if err == nil && (predDx != 0 || predDy != 0) {
			// Blend raw and AI predicted movement
			blendedDx := (1-m.aiBlendFactor)*dx + m.aiBlendFactor*predDx
			blendedDy := (1-m.aiBlendFactor)*dy + m.aiBlendFactor*predDy
			dx, dy = blendedDx, blendedDy
		}
	}

	// Acceleration
	if m.acceleration {
		speed := math.Sqrt(dx*dx + dy*dy)
		if speed > 5 {
			factor := 1.0 + m.accelFactor*(speed/50.0)
			if factor > 3.0 {
				factor = 3.0
			}
			dx *= factor
			dy *= factor
		}
	}

	if math.Abs(dx) < minMoveDelta && math.Abs(dy) < minMoveDelta {
		return
	}

	m.executeMove(dx, dy)
}

func (m *mouseController) applySmoothing(dx, dy float64) (float64, float64) {
	if !m.smoothing {
		return dx, dy
	}
	const alpha = 0.3
	m.lastX = alpha*dx + (1-alpha)*m.lastX
	m.lastY = alpha*dy + (1-alpha)*m.lastY
	return m.lastX, m.lastY
}

func (m *mouseController) Click(button string) {
	// double-click detection
	if button == "left" {
		now := time.Now()
		if now.Sub(m.lastClickTime) < 300*time.Millisecond {
			m.clickCountWin++
			if m.clickCountWin >= 2 {
				m.DoubleClick()
				m.clickCountWin = 0
				m.lastClickTime = time.Time{}
				return
			}
		} else {
			m.clickCountWin = 1
			m.lastClickTime = now
		}
	}
	m.executeClick(button)
	if button == "left" {
		atomic.AddInt64(&m.clickCount, 1)
	} else if button == "right" {
		atomic.AddInt64(&m.rightClickCnt, 1)
	}
}

func (m *mouseController) DoubleClick() {
	m.executeDoubleClick()
	atomic.AddInt64(&m.doubleClickCnt, 1)
}

func (m *mouseController) Scroll(delta int) {
	m.executeScroll(delta)
	atomic.AddInt64(&m.scrollCount, 1)
}

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}

func (m *mouseController) SetSensitivity(s float64) {
	m.sensitivity = s
}
// Add to mouseController struct
type mouseController struct {
    // ... existing fields ...
    aiSmoother    *AISmoother
    aiEnabled     bool
    aiBlendFactor float64
    lastCursorX   float64
    lastCursorY   float64
}

// Add these methods
func (m *mouseController) SetAISmoother(s *AISmoother) {
    m.aiSmoother = s
    m.aiEnabled = true
}

func (m *mouseController) EnableAISmoothing(enabled bool) {
    m.aiEnabled = enabled
    if m.aiSmoother != nil {
        m.aiSmoother.SetEnabled(enabled)
    }
}

// Modified Move method (replace existing)
func (m *mouseController) Move(dx, dy float64) {
    // Rate limiting
    now := time.Now()
    if now.Sub(m.lastMoveTime) > time.Second {
        m.moveCount = 0
        m.lastMoveTime = now
    }
    m.moveCount++
    if m.moveCount > m.moveRateLimit {
        return
    }
    
    // Apply sensitivity
    dx *= m.sensitivity
    dy *= m.sensitivity
    
    // Apply AI smoothing if enabled
    if m.aiEnabled && m.aiSmoother != nil {
        // Update AI history with current cursor position
        m.aiSmoother.AddPoint(m.lastCursorX, m.lastCursorY)
        predDx, predDy, err := m.aiSmoother.PredictDelta()
        if err == nil && (predDx != 0 || predDy != 0) {
            // Blend raw and AI predictions
            dx = (1-m.aiBlendFactor)*dx + m.aiBlendFactor*predDx
            dy = (1-m.aiBlendFactor)*dy + m.aiBlendFactor*predDy
        }
    } else {
        // Fall back to EMA smoothing
        dx, dy = m.applySmoothing(dx, dy)
    }
    
    // Apply acceleration
    if m.acceleration {
        speed := math.Sqrt(dx*dx + dy*dy)
        if speed > 5 {
            factor := 1.0 + m.accelFactor*(speed/50.0)
            if factor > 3.0 {
                factor = 3.0
            }
            dx *= factor
            dy *= factor
        }
    }
    
    if math.Abs(dx) < minMoveDelta && math.Abs(dy) < minMoveDelta {
        return
    }
    
    m.executeMove(dx, dy)
    // Update last cursor position (simplified)
    m.lastCursorX += dx
    m.lastCursorY += dy
}