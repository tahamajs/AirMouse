package control

import (
    "math"
    "sync/atomic"
    "time"
)

// MouseController is a platform-agnostic controller for mouse actions.
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
    sensitivity   float64
    clickCount    int64
    doubleClickCnt int64
    rightClickCnt int64
    scrollCount   int64

    smoothing     bool
    acceleration  bool
    accelFactor   float64

    lastX         float64
    lastY         float64
    lastMoveTime  time.Time
    moveCount     int
    moveRateLimit int

    lastClickTime time.Time
    clickWindow   int
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

func (m *mouseController) SetSmoothing(enabled bool) { m.smoothing = enabled }
func (m *mouseController) SetAcceleration(enabled bool, factor float64) { m.acceleration = enabled; m.accelFactor = factor }
func (m *mouseController) GetSensitivity() float64 { return m.sensitivity }
func (m *mouseController) SetSensitivity(s float64) { m.sensitivity = s }

func (m *mouseController) applySmoothing(dx, dy float64) (float64, float64) {
    if !m.smoothing { return dx, dy }
    const alpha = 0.3
    m.lastX = alpha*dx + (1-alpha)*m.lastX
    m.lastY = alpha*dy + (1-alpha)*m.lastY
    return m.lastX, m.lastY
}

func (m *mouseController) Move(dx, dy float64) {
    // rate limiting
    now := time.Now()
    if now.Sub(m.lastMoveTime) > time.Second {
        m.moveCount = 0
        m.lastMoveTime = now
    }
    m.moveCount++
    if m.moveCount > m.moveRateLimit { return }

    dx *= m.sensitivity
    dy *= m.sensitivity

    dx, dy = m.applySmoothing(dx, dy)

    if m.acceleration {
        speed := math.Sqrt(dx*dx + dy*dy)
        if speed > 5 {
            factor := 1.0 + m.accelFactor*(speed/50.0)
            if factor > 3.0 { factor = 3.0 }
            dx *= factor; dy *= factor
        }
    }

    if math.Abs(dx) < minMoveDelta && math.Abs(dy) < minMoveDelta { return }
    m.executeMove(dx, dy)
}

func (m *mouseController) Click(button string) {
    now := time.Now()
    if button == "left" {
        if now.Sub(m.lastClickTime) < 300*time.Millisecond {
            m.clickWindow++
            if m.clickWindow >= 2 {
                m.DoubleClick()
                m.clickWindow = 0
                m.lastClickTime = time.Time{}
                return
            }
        } else {
            m.clickWindow = 1
            m.lastClickTime = now
        }
    }
    m.executeClick(button)
    if button == "left" { atomic.AddInt64(&m.clickCount, 1) } else { atomic.AddInt64(&m.rightClickCnt, 1) }
}

func (m *mouseController) DoubleClick() { m.executeDoubleClick(); atomic.AddInt64(&m.doubleClickCnt, 1) }
func (m *mouseController) Scroll(delta int) { m.executeScroll(delta); atomic.AddInt64(&m.scrollCount, 1) }

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
    return atomic.LoadInt64(&m.clickCount), atomic.LoadInt64(&m.doubleClickCnt), atomic.LoadInt64(&m.rightClickCnt), atomic.LoadInt64(&m.scrollCount)
}

// Platform-specific methods (implemented in mouse_*.go)
