// mouse.go
package mouse

import (
    "sync"
)

// MouseController defines the interface for controlling the mouse.
type MouseController interface {
    Move(dx, dy float64)
    Click(button string)
    DoubleClick()
    Scroll(delta int)
    SetSensitivity(sensitivity float64)
    GetSensitivity() float64
    SetSmoothing(enabled bool)
    SetAcceleration(enabled bool, factor float64)
    EnablePredictive(enabled bool)
    SetPredictiveBlendFactor(factor float64)
    EnableAISmoothing(enabled bool)
    Stats() (clicks, doubleClicks, rightClicks, scrolls int64)
    ResetStats()
}

// BaseMouse provides common fields and methods for all platforms.
type BaseMouse struct {
    sensitivity     float64
    smoothing       bool
    acceleration    bool
    accelFactor     float64
    predictive      bool
    predictiveBlend float64
    aiSmoothing     bool
    mu              sync.RWMutex
    clicks          int64
    doubleClicks    int64
    rightClicks     int64
    scrolls         int64
}

// NewBaseMouse creates a new BaseMouse with default settings.
func NewBaseMouse(sensitivity float64) *BaseMouse {
    return &BaseMouse{
        sensitivity:     sensitivity,
        smoothing:       true,
        acceleration:    true,
        accelFactor:     1.5,
        predictive:      true,
        predictiveBlend: 0.6,
        aiSmoothing:     false,
    }
}

// SetSensitivity updates the sensitivity value.
func (m *BaseMouse) SetSensitivity(sensitivity float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.sensitivity = sensitivity
}

// GetSensitivity returns the current sensitivity.
func (m *BaseMouse) GetSensitivity() float64 {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.sensitivity
}

// SetSmoothing enables or disables smoothing.
func (m *BaseMouse) SetSmoothing(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.smoothing = enabled
}

// SetAcceleration enables/disables acceleration and sets the factor.
func (m *BaseMouse) SetAcceleration(enabled bool, factor float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.acceleration = enabled
    m.accelFactor = factor
}

// EnablePredictive enables/disables predictive movement.
func (m *BaseMouse) EnablePredictive(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.predictive = enabled
}

// SetPredictiveBlendFactor sets the blend factor for prediction.
func (m *BaseMouse) SetPredictiveBlendFactor(factor float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.predictiveBlend = factor
}

// EnableAISmoothing enables/disables AI smoothing.
func (m *BaseMouse) EnableAISmoothing(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.aiSmoothing = enabled
}

// Stats returns the current usage statistics.
func (m *BaseMouse) Stats() (clicks, doubleClicks, rightClicks, scrolls int64) {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.clicks, m.doubleClicks, m.rightClicks, m.scrolls
}

// ResetStats resets all statistics to zero.
func (m *BaseMouse) ResetStats() {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.clicks = 0
    m.doubleClicks = 0
    m.rightClicks = 0
    m.scrolls = 0
}

// applySensitivity multiplies dx and dy by the sensitivity.
func (m *BaseMouse) applySensitivity(dx, dy float64) (float64, float64) {
    // Additional filtering (smoothing, acceleration) could be added here.
    return dx * m.sensitivity, dy * m.sensitivity
}