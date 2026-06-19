package repository

import (
    "fmt"
    "math"
    "sync"
    "sync/atomic"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/infra/mouse"
    "airmouse-go/internal/utils"
)

type mouseRepositoryImpl struct {
    ctrl          mouse.MouseController
    stats         *entity.Statistics
    profile       *entity.MovementProfile
    clicks        int64
    doubleClicks  int64
    rightClicks   int64
    middleClicks  int64
    scrolls       int64
    totalMovement int64
    movementCount int64
    totalScroll   int64
    lastPosition  struct {
        x, y int
        mu   sync.RWMutex
    }
    movementHistory []MovementSample
    historyMu       sync.RWMutex
    maxHistorySize  int
    callbacks       []func(event MouseEvent)
    callbackMu      sync.RWMutex
}

type MovementSample struct {
    Timestamp time.Time
    DX, DY    float64
    Velocity  float64
    Distance  float64
}

type MouseEvent struct {
    Type      string    // "move", "click", "doubleclick", "scroll"
    Button    string    // for clicks
    Delta     int       // for scroll
    DX, DY    float64   // for move
    Timestamp time.Time
}

func NewMouseRepository(ctrl mouse.MouseController) repository.MouseRepository {
    return &mouseRepositoryImpl{
        ctrl:           ctrl,
        stats:          entity.NewStatistics(),
        profile:        entity.DefaultMovementProfile(),
        movementHistory: make([]MovementSample, 0, 1000),
        maxHistorySize: 1000,
        callbacks:      make([]func(event MouseEvent), 0),
    }
}

func (r *mouseRepositoryImpl) Move(dx, dy float64) error {
    // Apply sensitivity and acceleration
    dx, dy = r.applySensitivity(dx, dy)
    dx, dy = r.applyAcceleration(dx, dy)
    
    // Apply smoothing if enabled
    if r.profile.SmoothingEnabled {
        dx, dy = r.applySmoothing(dx, dy)
    }
    
    // Execute movement
    r.ctrl.Move(dx, dy)
    
    // Update statistics
    distance := math.Hypot(dx, dy)
    velocity := distance / 0.016 // Assuming 16ms between movements
    
    atomic.AddInt64(&r.movementCount, 1)
    atomic.AddInt64(&r.totalMovement, int64(distance*1000))
    
    // Record movement history
    r.recordMovement(dx, dy, velocity, distance)
    
    // Trigger callback
    r.triggerCallback(MouseEvent{
        Type:      "move",
        DX:        dx,
        DY:        dy,
        Timestamp: time.Now(),
    })
    
    return nil
}

func (r *mouseRepositoryImpl) MoveSmooth(points []entity.Point, durationMs int) error {
    if len(points) == 0 {
        return nil
    }
    
    // Calculate time per point
    if durationMs <= 0 {
        durationMs = 100
    }
    timePerPoint := time.Duration(durationMs/len(points)) * time.Millisecond
    
    for i, point := range points {
        // Calculate delta from previous point
        var dx, dy float64
        if i == 0 {
            dx = point.X
            dy = point.Y
        } else {
            dx = point.X - points[i-1].X
            dy = point.Y - points[i-1].Y
        }
        
        // Apply smoothing
        dx, dy = r.applySmoothing(dx, dy)
        
        // Execute movement
        r.ctrl.Move(dx, dy)
        
        // Wait for next point
        if i < len(points)-1 {
            time.Sleep(timePerPoint)
        }
    }
    
    return nil
}

func (r *mouseRepositoryImpl) Click(button entity.MouseButton) error {
    buttonStr := string(button)
    r.ctrl.Click(buttonStr)
    
    switch button {
    case entity.LeftButton:
        atomic.AddInt64(&r.clicks, 1)
    case entity.RightButton:
        atomic.AddInt64(&r.rightClicks, 1)
    case entity.MiddleButton:
        atomic.AddInt64(&r.middleClicks, 1)
    }
    
    r.triggerCallback(MouseEvent{
        Type:      "click",
        Button:    buttonStr,
        Timestamp: time.Now(),
    })
    
    return nil
}

func (r *mouseRepositoryImpl) DoubleClick() error {
    r.ctrl.DoubleClick()
    atomic.AddInt64(&r.doubleClicks, 1)
    
    r.triggerCallback(MouseEvent{
        Type:      "doubleclick",
        Timestamp: time.Now(),
    })
    
    return nil
}

func (r *mouseRepositoryImpl) ClickAt(x, y int, button entity.MouseButton) error {
    if err := r.SetPosition(x, y); err != nil {
        return fmt.Errorf("failed to set position: %w", err)
    }
    return r.Click(button)
}

func (r *mouseRepositoryImpl) Scroll(delta int) error {
    r.ctrl.Scroll(delta)
    atomic.AddInt64(&r.scrolls, 1)
    atomic.AddInt64(&r.totalScroll, int64(delta))
    
    r.triggerCallback(MouseEvent{
        Type:      "scroll",
        Delta:     delta,
        Timestamp: time.Now(),
    })
    
    return nil
}

func (r *mouseRepositoryImpl) GetPosition() (int, int, error) {
    r.lastPosition.mu.RLock()
    defer r.lastPosition.mu.RUnlock()
    return r.lastPosition.x, r.lastPosition.y, nil
}

func (r *mouseRepositoryImpl) SetPosition(x, y int) error {
    // Get screen size (would come from OS in real implementation)
    screenWidth, screenHeight := 1920, 1080
    
    // Clamp to screen bounds
    if x < 0 {
        x = 0
    }
    if x > screenWidth {
        x = screenWidth
    }
    if y < 0 {
        y = 0
    }
    if y > screenHeight {
        y = screenHeight
    }
    
    r.lastPosition.mu.Lock()
    r.lastPosition.x = x
    r.lastPosition.y = y
    r.lastPosition.mu.Unlock()
    
    return nil
}

func (r *mouseRepositoryImpl) GetMovementProfile() (*entity.MovementProfile, error) {
    return r.profile, nil
}

func (r *mouseRepositoryImpl) SetMovementProfile(profile *entity.MovementProfile) error {
    if profile == nil {
        return fmt.Errorf("movement profile cannot be nil")
    }
    r.profile = profile
    return nil
}

func (r *mouseRepositoryImpl) GetStatistics() (*entity.Statistics, error) {
    // Calculate average speed
    var avgSpeed float64
    r.historyMu.RLock()
    if len(r.movementHistory) > 0 {
        var totalVelocity float64
        for _, sample := range r.movementHistory {
            totalVelocity += sample.Velocity
        }
        avgSpeed = totalVelocity / float64(len(r.movementHistory))
    }
    r.historyMu.RUnlock()
    
    return &entity.Statistics{
        TotalMovement:     float64(atomic.LoadInt64(&r.totalMovement)) / 1000,
        MovementCount:     atomic.LoadInt64(&r.movementCount),
        ClickCount:        atomic.LoadInt64(&r.clicks),
        DoubleClickCount:  atomic.LoadInt64(&r.doubleClicks),
        RightClickCount:   atomic.LoadInt64(&r.rightClicks),
        MiddleClickCount:  atomic.LoadInt64(&r.middleClicks),
        ScrollCount:       atomic.LoadInt64(&r.scrolls),
        TotalScrollDelta:  atomic.LoadInt64(&r.totalScroll),
        AverageSpeed:      avgSpeed,
        LastUpdateTime:    time.Now(),
    }, nil
}

func (r *mouseRepositoryImpl) ResetStatistics() error {
    atomic.StoreInt64(&r.clicks, 0)
    atomic.StoreInt64(&r.doubleClicks, 0)
    atomic.StoreInt64(&r.rightClicks, 0)
    atomic.StoreInt64(&r.middleClicks, 0)
    atomic.StoreInt64(&r.scrolls, 0)
    atomic.StoreInt64(&r.totalMovement, 0)
    atomic.StoreInt64(&r.movementCount, 0)
    atomic.StoreInt64(&r.totalScroll, 0)
    
    r.historyMu.Lock()
    r.movementHistory = make([]MovementSample, 0, r.maxHistorySize)
    r.historyMu.Unlock()
    
    utils.LogInfo("Mouse statistics reset")
    return nil
}

// Additional helper methods
func (r *mouseRepositoryImpl) applySensitivity(dx, dy float64) (float64, float64) {
    sensitivity := r.profile.Sensitivity
    if sensitivity <= 0 {
        sensitivity = 1.0
    }
    return dx * sensitivity, dy * sensitivity
}

func (r *mouseRepositoryImpl) applyAcceleration(dx, dy float64) (float64, float64) {
    if !r.profile.AccelerationEnabled {
        return dx, dy
    }
    
    distance := math.Hypot(dx, dy)
    factor := 1.0 + (distance/100.0)*r.profile.AccelerationFactor
    if factor > r.profile.MaxAcceleration {
        factor = r.profile.MaxAcceleration
    }
    
    return dx * factor, dy * factor
}

func (r *mouseRepositoryImpl) applySmoothing(dx, dy float64) (float64, float64) {
    if !r.profile.SmoothingEnabled {
        return dx, dy
    }
    
    // Simple EMA smoothing
    const alpha = 0.3
    var lastDx, lastDy float64
    
    smoothedDx := alpha*dx + (1-alpha)*lastDx
    smoothedDy := alpha*dy + (1-alpha)*lastDy
    
    lastDx, lastDy = smoothedDx, smoothedDy
    return smoothedDx, smoothedDy
}

func (r *mouseRepositoryImpl) recordMovement(dx, dy, velocity, distance float64) {
    r.historyMu.Lock()
    defer r.historyMu.Unlock()
    
    sample := MovementSample{
        Timestamp: time.Now(),
        DX:        dx,
        DY:        dy,
        Velocity:  velocity,
        Distance:  distance,
    }
    
    r.movementHistory = append(r.movementHistory, sample)
    if len(r.movementHistory) > r.maxHistorySize {
        r.movementHistory = r.movementHistory[1:]
    }
}

func (r *mouseRepositoryImpl) triggerCallback(event MouseEvent) {
    r.callbackMu.RLock()
    callbacks := make([]func(MouseEvent), len(r.callbacks))
    copy(callbacks, r.callbacks)
    r.callbackMu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (r *mouseRepositoryImpl) AddEventListener(callback func(event MouseEvent)) {
    r.callbackMu.Lock()
    defer r.callbackMu.Unlock()
    r.callbacks = append(r.callbacks, callback)
}

func (r *mouseRepositoryImpl) GetMovementHistory(count int) []MovementSample {
    r.historyMu.RLock()
    defer r.historyMu.RUnlock()
    
    if count <= 0 || count > len(r.movementHistory) {
        count = len(r.movementHistory)
    }
    
    result := make([]MovementSample, count)
    copy(result, r.movementHistory[len(r.movementHistory)-count:])
    return result
}

func (r *mouseRepositoryImpl) GetAverageSpeed() float64 {
    r.historyMu.RLock()
    defer r.historyMu.RUnlock()
    
    if len(r.movementHistory) == 0 {
        return 0
    }
    
    var total float64
    for _, sample := range r.movementHistory {
        total += sample.Velocity
    }
    return total / float64(len(r.movementHistory))
}
