package control

import (
    "math"
    "sync/atomic"
    "time"

    "airmouse-go/internal/adaptivesmoothing"
    "airmouse-go/internal/config"
    "airmouse-go/internal/predictiveml"
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
    EnablePredictive(enabled bool)
    SetPredictiveBlendFactor(factor float64)
    EnableAISmoothing(enabled bool)
    SetAISmoother(s *AISmoother)
    EnableMLPrediction(enabled bool)
    SetMLBlendFactor(factor float64)
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
    clickWindow     int

    // Predictive (Kalman)
    predictor  *MovementPredictor
    predEnabled bool

    // AI smoother (ONNX)
    aiSmoother    *AISmoother
    aiEnabled     bool
    aiBlendFactor float64

    // ML prediction (LSTM)
    mlPredictor   *predictiveml.Predictor
    mlEnabled     bool
    mlBlend       float64

    // Humanizer
    humanizer   *adaptivesmoothing.Humanizer
    humanizerEnabled bool

    lastCursorX, lastCursorY float64
}

const (
    minMoveDelta    = 0.01
    rateLimitPerSec = 60
)

func NewMouseController(sensitivity float64) MouseController {
    m := &mouseController{
        sensitivity:    sensitivity,
        smoothing:      true,
        acceleration:   false,
        accelFactor:    1.5,
        moveRateLimit:  rateLimitPerSec,
        predEnabled:    true,
        aiEnabled:      false,
        aiBlendFactor:  0.6,
        mlEnabled:      false,
        mlBlend:        0.6,
    }
    cfg := config.Get()
    if cfg.EnableHumanizer {
        humanizerCfg := adaptivesmoothing.DefaultHumanizerConfig()
        humanizerCfg.TremorAmplitude = cfg.HumanizerTremorAmplitude
        humanizerCfg.BSplineSegments = cfg.HumanizerBSplineSegments
        m.humanizer = adaptivesmoothing.NewHumanizer(humanizerCfg)
        m.humanizerEnabled = true
    }
    return m
}

func (m *mouseController) SetSmoothing(enabled bool) { m.smoothing = enabled }
func (m *mouseController) SetAcceleration(enabled bool, factor float64) {
    m.acceleration = enabled
    m.accelFactor = factor
}
func (m *mouseController) GetSensitivity() float64 { return m.sensitivity }
func (m *mouseController) SetSensitivity(s float64) { m.sensitivity = s }
func (m *mouseController) EnablePredictive(enabled bool) { m.predEnabled = enabled }
func (m *mouseController) SetPredictiveBlendFactor(factor float64) {
    if m.predictor != nil {
        m.predictor.SetBlendFactor(factor)
    }
}
func (m *mouseController) EnableAISmoothing(enabled bool) {
    m.aiEnabled = enabled
    if m.aiSmoother != nil {
        m.aiSmoother.SetEnabled(enabled)
    }
}
func (m *mouseController) SetAISmoother(s *AISmoother) { m.aiSmoother = s; m.aiEnabled = true }
func (m *mouseController) EnableMLPrediction(enabled bool) { m.mlEnabled = enabled }
func (m *mouseController) SetMLBlendFactor(factor float64) { m.mlBlend = factor }

func (m *mouseController) applySmoothing(dx, dy float64) (float64, float64) {
    if !m.smoothing {
        return dx, dy
    }
    const alpha = 0.3
    m.lastX = alpha*dx + (1-alpha)*m.lastX
    m.lastY = alpha*dy + (1-alpha)*m.lastY
    return m.lastX, m.lastY
}

func (m *mouseController) Move(dx, dy float64) {
    now := time.Now()
    if now.Sub(m.lastMoveTime) > time.Second {
        m.moveCount = 0
        m.lastMoveTime = now
    }
    m.moveCount++
    if m.moveCount > m.moveRateLimit {
        return
    }

    dx *= m.sensitivity
    dy *= m.sensitivity

    // Predictive filter (Kalman)
    if m.predEnabled && m.predictor != nil {
        dx, dy = m.predictor.AddMovement(dx, dy)
    } else {
        dx, dy = m.applySmoothing(dx, dy)
    }

    // AI smoothing (ONNX)
    if m.aiEnabled && m.aiSmoother != nil {
        m.aiSmoother.AddPoint(m.lastCursorX, m.lastCursorY)
        predDx, predDy, err := m.aiSmoother.PredictDelta()
        if err == nil && (predDx != 0 || predDy != 0) {
            dx = (1-m.aiBlendFactor)*dx + m.aiBlendFactor*predDx
            dy = (1-m.aiBlendFactor)*dy + m.aiBlendFactor*predDy
        }
    }

    // ML prediction (LSTM)
    if m.mlEnabled && m.mlPredictor != nil {
        m.mlPredictor.AddPoint(float32(m.lastCursorX), float32(m.lastCursorY))
        predDx, predDy, err := m.mlPredictor.PredictDelta()
        if err == nil {
            dx = (1-m.mlBlend)*dx + m.mlBlend*float64(predDx)
            dy = (1-m.mlBlend)*dy + m.mlBlend*float64(predDy)
        }
    }

    // Humanizer (tremor, B‑spline, velocity profile)
    if m.humanizerEnabled && m.humanizer != nil {
        dx, dy = m.humanizer.Process(dx, dy, m.lastCursorX, m.lastCursorY)
    }

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
    m.lastCursorX += dx
    m.lastCursorY += dy
    if m.humanizer != nil {
        m.humanizer.UpdatePosition(m.lastCursorX, m.lastCursorY)
    }
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
    if button == "left" {
        atomic.AddInt64(&m.clickCount, 1)
    } else {
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

func (m *mouseController) GetPredictor() *MovementPredictor { return m.predictor }
func (m *mouseController) SetPredictor(p *MovementPredictor) { m.predictor = p; m.predEnabled = true }

// Platform‑specific methods are implemented in mouse_windows.go, mouse_darwin.go, mouse_linux.go
// The following are stubs that will be overridden by build tags.
func (m *mouseController) executeMove(dx, dy float64)   {}
func (m *mouseController) executeClick(button string)   {}
func (m *mouseController) executeDoubleClick()          {}
func (m *mouseController) executeScroll(delta int)      {}