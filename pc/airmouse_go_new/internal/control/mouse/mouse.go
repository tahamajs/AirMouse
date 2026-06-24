// Package mouse provides platform‑agnostic mouse control with smoothing and prediction.
package mouse

import (
	"math"
	"sync"
	"sync/atomic"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/predict"
)

// Controller is the interface for mouse operations.
type Controller interface {
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
	SetAISmoother(s *predict.AISmoother)
	EnableMLPrediction(enabled bool)
	SetMLBlendFactor(factor float64)
	ResetStats()
	GetPosition() (x, y float64)
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

	lastX, lastY  float64
	lastMoveTime  time.Time
	moveCount     int
	moveRateLimit int
	moveMu        sync.Mutex

	lastClickTime time.Time
	clickWindow   int

	predictor   *predict.MovementPredictor
	predEnabled bool

	aiSmoother    *predict.AISmoother
	aiEnabled     bool
	aiBlendFactor float64

	mlPredictor   *predict.MLPredictor
	mlEnabled     bool
	mlBlend       float64

	lastCursorX, lastCursorY float64
}

const (
	minMoveDelta    = 0.01
	rateLimitPerSec = 200
)

// NewController creates a new mouse controller with default settings.
func NewController(sensitivity float64) Controller {
	if sensitivity <= 0 {
		sensitivity = 1.0
	}
	return &mouseController{
		sensitivity:   sensitivity,
		smoothing:     true,
		acceleration:  true,
		accelFactor:   1.5,
		moveRateLimit: rateLimitPerSec,
		predEnabled:   true,
		aiEnabled:     false,
		aiBlendFactor: 0.6,
		mlEnabled:     false,
		mlBlend:       0.6,
		predictor:     predict.NewMovementPredictor(0.02, 0.6),
	}
}

// --- Setters and getters ---

func (m *mouseController) SetSmoothing(enabled bool) {
	m.smoothing = enabled
}

func (m *mouseController) SetAcceleration(enabled bool, factor float64) {
	m.acceleration = enabled
	if factor > 0 {
		m.accelFactor = factor
	}
}

func (m *mouseController) GetSensitivity() float64 {
	return m.sensitivity
}

func (m *mouseController) SetSensitivity(s float64) {
	if s < 0.1 {
		s = 0.1
	}
	if s > 3.0 {
		s = 3.0
	}
	m.sensitivity = s
}

func (m *mouseController) EnablePredictive(enabled bool) {
	m.predEnabled = enabled
	if m.predictor != nil {
		m.predictor.SetEnabled(enabled)
	}
}

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

func (m *mouseController) SetAISmoother(s *predict.AISmoother) {
	m.aiSmoother = s
	if s != nil {
		m.aiEnabled = true
	}
}

func (m *mouseController) EnableMLPrediction(enabled bool) {
	m.mlEnabled = enabled
	if m.mlPredictor != nil {
		m.mlPredictor.SetEnabled(enabled)
	}
}

func (m *mouseController) SetMLBlendFactor(factor float64) {
	m.mlBlend = factor
}

func (m *mouseController) ResetStats() {
	atomic.StoreInt64(&m.clickCount, 0)
	atomic.StoreInt64(&m.doubleClickCnt, 0)
	atomic.StoreInt64(&m.rightClickCnt, 0)
	atomic.StoreInt64(&m.scrollCount, 0)
}

func (m *mouseController) GetPosition() (x, y float64) {
	return m.lastCursorX, m.lastCursorY
}

// --- Movement ---

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
	if common.IsMovementPaused() {
		return
	}

	// Rate limiting
	m.moveMu.Lock()
	now := time.Now()
	if now.Sub(m.lastMoveTime) > time.Second {
		m.moveCount = 0
		m.lastMoveTime = now
	}
	m.moveCount++
	if m.moveCount > m.moveRateLimit {
		m.moveMu.Unlock()
		return
	}
	m.moveMu.Unlock()

	// Sensitivity
	dx *= m.sensitivity
	dy *= m.sensitivity

	// Deadband
	if math.Abs(dx) < 0.5 {
		dx = 0
	}
	if math.Abs(dy) < 0.5 {
		dy = 0
	}
	if dx == 0 && dy == 0 {
		return
	}

	// Predictive (Kalman)
	if m.predEnabled && m.predictor != nil {
		dx, dy = m.predictor.AddMovement(dx, dy)
	} else {
		dx, dy = m.applySmoothing(dx, dy)
	}

	// AI smoothing (ONNX)
	if m.aiEnabled && m.aiSmoother != nil {
		m.aiSmoother.AddPoint(m.lastCursorX, m.lastCursorY)
		predDx, predDy, _ := m.aiSmoother.PredictDelta()
		if predDx != 0 || predDy != 0 {
			dx = (1-m.aiBlendFactor)*dx + m.aiBlendFactor*predDx
			dy = (1-m.aiBlendFactor)*dy + m.aiBlendFactor*predDy
		}
	}

	// ML prediction (LSTM)
	if m.mlEnabled && m.mlPredictor != nil {
		m.mlPredictor.AddPoint(m.lastCursorX, m.lastCursorY)
		predDx, predDy, _, _ := m.mlPredictor.PredictDelta()
		dx = (1-m.mlBlend)*dx + m.mlBlend*predDx
		dy = (1-m.mlBlend)*dy + m.mlBlend*predDy
	}

	// Acceleration
	if m.acceleration {
		speed := math.Hypot(dx, dy)
		if speed > 5 {
			factor := 1.0 + m.accelFactor*(speed/50.0)
			if factor > 3.0 {
				factor = 3.0
			}
			dx *= factor
			dy *= factor
		}
	}

	// Minimum threshold
	if math.Abs(dx) < minMoveDelta && math.Abs(dy) < minMoveDelta {
		return
	}

	m.executeMove(dx, dy)
	m.lastCursorX += dx
	m.lastCursorY += dy
}

// --- Click and scroll ---

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
	switch button {
	case "left":
		atomic.AddInt64(&m.clickCount, 1)
	case "right":
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

// --- Statistics ---

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}

// --- Platform-specific execution (implemented in *_darwin.go, *_linux.go, *_windows.go) ---

func (m *mouseController) executeMove(dx, dy float64) {
	// Stub – platform implementation overrides this.
}

func (m *mouseController) executeClick(button string) {
	// Stub
}

func (m *mouseController) executeDoubleClick() {
	// Stub
}

func (m *mouseController) executeScroll(delta int) {
	// Stub
}