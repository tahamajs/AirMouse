package particlefilter

import (
    "math"
    "sync"
    "time"
)

type GestureModel struct {
    Name       string
    Templates  [][]float64
    Tolerance  float64
    MinLength  int
    MaxLength  int
    Velocity   float64
}

type Recognizer struct {
    filter      *Filter
    gestures    []GestureModel
    history     [][2]float64
    velocities  [][2]float64
    maxHistory  int
    mu          sync.Mutex
    lastGesture string
    lastTime    time.Time
    cooldown    time.Duration
    callbacks   []func(event GestureEvent)
}

type GestureEvent struct {
    Gesture    string
    Confidence float64
    Velocity   float64
    Duration   time.Duration
    Timestamp  time.Time
}

func NewRecognizer() *Recognizer {
    return &Recognizer{
        filter:      NewFilter(500),
        gestures:    predefinedGestures(),
        maxHistory:  100,
        lastTime:    time.Now(),
        cooldown:    500 * time.Millisecond,
        callbacks:   make([]func(GestureEvent), 0),
    }
}

func predefinedGestures() []GestureModel {
    return []GestureModel{
        {
            Name:      "swipe_left",
            Templates: [][]float64{{-15,0},{-15,0},{-15,0},{-10,0},{-10,0}},
            Tolerance: 8.0,
            MinLength: 3,
            MaxLength: 8,
            Velocity:  50,
        },
        {
            Name:      "swipe_right",
            Templates: [][]float64{{15,0},{15,0},{15,0},{10,0},{10,0}},
            Tolerance: 8.0,
            MinLength: 3,
            MaxLength: 8,
            Velocity:  50,
        },
        {
            Name:      "swipe_up",
            Templates: [][]float64{{0,-15},{0,-15},{0,-15},{0,-10},{0,-10}},
            Tolerance: 8.0,
            MinLength: 3,
            MaxLength: 8,
            Velocity:  50,
        },
        {
            Name:      "swipe_down",
            Templates: [][]float64{{0,15},{0,15},{0,15},{0,10},{0,10}},
            Tolerance: 8.0,
            MinLength: 3,
            MaxLength: 8,
            Velocity:  50,
        },
        {
            Name:      "circle_cw",
            Templates: [][]float64{
                {5,5},{0,10},{-5,5},{-10,0},{-5,-5},{0,-10},{5,-5},{10,0},{5,5},
            },
            Tolerance: 5.0,
            MinLength: 8,
            MaxLength: 15,
            Velocity:  30,
        },
        {
            Name:      "circle_ccw",
            Templates: [][]float64{
                {-5,5},{0,10},{5,5},{10,0},{5,-5},{0,-10},{-5,-5},{-10,0},{-5,5},
            },
            Tolerance: 5.0,
            MinLength: 8,
            MaxLength: 15,
            Velocity:  30,
        },
        {
            Name:      "zigzag",
            Templates: [][]float64{
                {10,0},{0,10},{-10,0},{0,-10},{10,0},{0,10},{-10,0},
            },
            Tolerance: 7.0,
            MinLength: 6,
            MaxLength: 12,
            Velocity:  40,
        },
        {
            Name:      "check_mark",
            Templates: [][]float64{
                {10,5},{5,10},{-5,-5},{-10,-10},
            },
            Tolerance: 6.0,
            MinLength: 3,
            MaxLength: 6,
            Velocity:  35,
        },
    }
}

func (r *Recognizer) AddMotion(dx, dy float64) {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    now := time.Now()
    dt := now.Sub(r.lastTime).Seconds()
    if dt > 0 {
        vx := dx / dt
        vy := dy / dt
        r.velocities = append(r.velocities, [2]float64{vx, vy})
    }
    
    r.history = append(r.history, [2]float64{dx, dy})
    if len(r.history) > r.maxHistory {
        r.history = r.history[1:]
    }
    if len(r.velocities) > r.maxHistory {
        r.velocities = r.velocities[1:]
    }
    
    r.filter.Predict(dt)
    r.filter.Update(dx, dy)
    r.lastTime = now
}

func (r *Recognizer) GetGesture() (gesture string, confidence float64) {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    if len(r.history) < 5 {
        return "none", 0.0
    }
    
    // Check cooldown
    if time.Since(r.lastTime) < r.cooldown && r.lastGesture != "" {
        return r.lastGesture, 0.8
    }
    
    bestScore := -1.0
    bestGesture := "none"
    
    for _, g := range r.gestures {
        if len(r.history) < g.MinLength || len(r.history) > g.MaxLength {
            continue
        }
        
        score := r.matchTemplate(g)
        if score > bestScore && score > 0.6 {
            bestScore = score
            bestGesture = g.Name
        }
    }
    
    // Check velocity consistency
    if bestGesture != "none" && len(r.velocities) > 0 {
        avgVel := r.calculateAverageVelocity()
        for _, g := range r.gestures {
            if g.Name == bestGesture && avgVel < g.Velocity*0.5 {
                bestScore *= 0.7
            }
        }
    }
    
    if bestScore > 0.7 {
        r.lastGesture = bestGesture
        r.triggerEvent(GestureEvent{
            Gesture:    bestGesture,
            Confidence: bestScore,
            Velocity:   r.calculateAverageVelocity(),
            Duration:   r.calculateGestureDuration(),
            Timestamp:  time.Now(),
        })
    }
    
    return bestGesture, bestScore
}

func (r *Recognizer) matchTemplate(gesture GestureModel) float64 {
    if len(r.history) < len(gesture.Templates) {
        return 0.0
    }
    
    // Dynamic time warping
    totalErr := 0.0
    step := float64(len(r.history)) / float64(len(gesture.Templates))
    
    for i, t := range gesture.Templates {
        idx := int(float64(i) * step)
        if idx >= len(r.history) {
            idx = len(r.history) - 1
        }
        hist := r.history[idx]
        
        errX := math.Abs(hist[0] - t[0])
        errY := math.Abs(hist[1] - t[1])
        totalErr += errX + errY
    }
    
    avgErr := totalErr / float64(len(gesture.Templates))
    confidence := 1.0 - (avgErr / gesture.Tolerance)
    if confidence < 0 {
        confidence = 0
    }
    if confidence > 1 {
        confidence = 1
    }
    
    return confidence
}

func (r *Recognizer) calculateAverageVelocity() float64 {
    if len(r.velocities) == 0 {
        return 0
    }
    
    var totalVel float64
    for _, v := range r.velocities {
        totalVel += math.Hypot(v[0], v[1])
    }
    return totalVel / float64(len(r.velocities))
}

func (r *Recognizer) calculateGestureDuration() time.Duration {
    if len(r.history) < 2 {
        return 0
    }
    return r.lastTime.Sub(r.lastTime.Add(-time.Duration(len(r.history)) * time.Second / 60))
}

func (r *Recognizer) AddGestureTemplate(gesture GestureModel) {
    r.mu.Lock()
    defer r.mu.Unlock()
    r.gestures = append(r.gestures, gesture)
}

func (r *Recognizer) RemoveGestureTemplate(name string) {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    for i, g := range r.gestures {
        if g.Name == name {
            r.gestures = append(r.gestures[:i], r.gestures[i+1:]...)
            break
        }
    }
}

func (r *Recognizer) Reset() {
    r.mu.Lock()
    defer r.mu.Unlock()
    
    r.history = make([][2]float64, 0)
    r.velocities = make([][2]float64, 0)
    r.filter.Reset()
    r.lastGesture = ""
    r.lastTime = time.Now()
}

func (r *Recognizer) AddEventListener(callback func(event GestureEvent)) {
    r.mu.Lock()
    defer r.mu.Unlock()
    r.callbacks = append(r.callbacks, callback)
}

func (r *Recognizer) triggerEvent(event GestureEvent) {
    r.mu.RLock()
    callbacks := make([]func(GestureEvent), len(r.callbacks))
    copy(callbacks, r.callbacks)
    r.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (r *Recognizer) GetAvailableGestures() []string {
    r.mu.RLock()
    defer r.mu.RUnlock()
    
    gestures := make([]string, len(r.gestures))
    for i, g := range r.gestures {
        gestures[i] = g.Name
    }
    return gestures
}