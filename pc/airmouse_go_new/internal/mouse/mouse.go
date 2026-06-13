package mouse

import (
    "sync"
    "time"
)

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

func (m *BaseMouse) SetSensitivity(sensitivity float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.sensitivity = sensitivity
}

func (m *BaseMouse) GetSensitivity() float64 {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.sensitivity
}

func (m *BaseMouse) SetSmoothing(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.smoothing = enabled
}

func (m *BaseMouse) SetAcceleration(enabled bool, factor float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.acceleration = enabled
    m.accelFactor = factor
}

func (m *BaseMouse) EnablePredictive(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.predictive = enabled
}

func (m *BaseMouse) SetPredictiveBlendFactor(factor float64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.predictiveBlend = factor
}

func (m *BaseMouse) EnableAISmoothing(enabled bool) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.aiSmoothing = enabled
}

func (m *BaseMouse) Stats() (clicks, doubleClicks, rightClicks, scrolls int64) {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.clicks, m.doubleClicks, m.rightClicks, m.scrolls
}

func (m *BaseMouse) ResetStats() {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.clicks = 0
    m.doubleClicks = 0
    m.rightClicks = 0
    m.scrolls = 0
}

func (m *BaseMouse) applySensitivity(dx, dy float64) (float64, float64) {
    return dx * m.sensitivity, dy * m.sensitivity
}