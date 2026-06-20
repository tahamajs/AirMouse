
package ui

import (
    "image/color"
    "math"
    "sync"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
)

// SpeedChart displays a real-time movement speed graph
type SpeedChart struct {
    container *fyne.Container
    history   []float64
    maxPoints int
    maxSpeed  float64
    color     color.Color
    mu        sync.RWMutex
    autoScale bool
    gridColor color.Color
    bgColor   color.Color
}

// NewSpeedChart creates a chart that plots cursor speed over time
func NewSpeedChart() fyne.CanvasObject {
    chart := &SpeedChart{
        history:   make([]float64, 0, 100),
        maxPoints: 100,
        maxSpeed:  100,
        autoScale: true,
        color:     color.RGBA{99, 102, 241, 255},
        gridColor: color.RGBA{60, 60, 70, 100},
        bgColor:   color.RGBA{30, 30, 40, 255},
    }
    chart.container = container.NewWithoutLayout()
    go chart.updater()
    return chart.container
}

func (c *SpeedChart) updater() {
    ticker := time.NewTicker(500 * time.Millisecond)
    defer ticker.Stop()
    
    for range ticker.C {
        // In production, fetch current speed from mouse controller
        speed := math.Abs(math.Sin(float64(time.Now().UnixNano()))) * 50
        c.AddDataPoint(speed)
    }
}

// AddDataPoint adds a data point to the chart
func (c *SpeedChart) AddDataPoint(speed float64) {
    c.mu.Lock()
    defer c.mu.Unlock()
    
    if len(c.history) >= c.maxPoints {
        c.history = c.history[1:]
    }
    c.history = append(c.history, speed)
    
    // Update max speed for scaling
    if c.autoScale && speed > c.maxSpeed {
        c.maxSpeed = speed
    }
    
    // Gradually reduce max speed if no recent high values
    if c.autoScale && c.maxSpeed > 10 {
        // Check if max speed is still relevant
        found := false
        for _, s := range c.history {
            if s > c.maxSpeed*0.8 {
                found = true
                break
            }
        }
        if !found {
            c.maxSpeed = c.maxSpeed * 0.95
            if c.maxSpeed < 10 {
                c.maxSpeed = 10
            }
        }
    }
    
    c.redraw()
}

func (c *SpeedChart) redraw() {
    c.mu.RLock()
    defer c.mu.RUnlock()
    
    c.container.Objects = nil
    if len(c.history) == 0 {
        return
    }
    
    width := c.container.Size().Width
    height := c.container.Size().Height
    if width < 10 || height < 10 {
        return
    }
    
    step := width / float32(c.maxPoints)
    if c.maxSpeed == 0 {
        c.maxSpeed = 1
    }
    
    // Draw background
    bg := canvas.NewRectangle(c.bgColor)
    bg.Resize(fyne.NewSize(width, height))
    bg.Move(fyne.NewPos(0, 0))
    c.container.Add(bg)
    
    // Draw grid lines
    for i := 0; i <= 4; i++ {
        y := height - (float32(i) * height / 4)
        gridLine := canvas.NewLine(c.gridColor)
        gridLine.Position1 = fyne.NewPos(0, y)
        gridLine.Position2 = fyne.NewPos(width, y)
        gridLine.StrokeWidth = 1
        c.container.Add(gridLine)
    }
    
    // Draw the speed line
    for i := 1; i < len(c.history); i++ {
        x1 := float32(i-1) * step
        y1 := height - float32(c.history[i-1]/c.maxSpeed)*height
        x2 := float32(i) * step
        y2 := height - float32(c.history[i]/c.maxSpeed)*height
        
        line := canvas.NewLine(c.color)
        line.Position1 = fyne.NewPos(x1, y1)
        line.Position2 = fyne.NewPos(x2, y2)
        line.StrokeWidth = 2
        c.container.Add(line)
    }
    
    // Fill area under curve
    fillColor := color.RGBA{99, 102, 241, 50}
    for i := 0; i < len(c.history)-1; i++ {
        x1 := float32(i) * step
        x2 := float32(i+1) * step
        y1 := height - float32(c.history[i]/c.maxSpeed)*height
        y2 := height - float32(c.history[i+1]/c.maxSpeed)*height
        
        // Create polygon fill
        points := []fyne.Position{
            {X: x1, Y: y1},
            {X: x2, Y: y2},
            {X: x2, Y: height},
            {X: x1, Y: height},
        }
        
        // Use multiple lines for fill approximation
        for x := x1; x < x2; x += 1 {
            t := (x - x1) / (x2 - x1)
            y := y1 + t*(y2-y1)
            point := canvas.NewCircle(fillColor)
            point.Position = fyne.NewPos(x, y)
            point.Resize(fyne.NewSize(1, 1))
            c.container.Add(point)
        }
    }
    
    c.container.Refresh()
}

// SetColor changes the chart line color
func (c *SpeedChart) SetColor(col color.Color) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.color = col
    c.redraw()
}

// SetAutoScale enables or disables auto-scaling
func (c *SpeedChart) SetAutoScale(enabled bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.autoScale = enabled
    if !enabled {
        c.maxSpeed = 100
    }
    c.redraw()
}

// Clear clears all data points
func (c *SpeedChart) Clear() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.history = make([]float64, 0, c.maxPoints)
    c.maxSpeed = 100
    c.redraw()
}

// GetMaxSpeed returns the current max speed
func (c *SpeedChart) GetMaxSpeed() float64 {
    c.mu.RLock()
    defer c.mu.RUnlock()
    return c.maxSpeed
}

// PingChart displays real-time ping latency
type PingChart struct {
    container  *fyne.Container
    history    []float64
    maxPoints  int
    maxLatency float64
    mu         sync.RWMutex
    goodThreshold float64
    warnThreshold float64
}

// NewPingChart creates a chart for ping latency
func NewPingChart() fyne.CanvasObject {
    chart := &PingChart{
        history:       make([]float64, 0, 60),
        maxPoints:     60,
        maxLatency:    100,
        goodThreshold: 50,
        warnThreshold: 100,
    }
    chart.container = container.NewWithoutLayout()
    return chart.container
}

// AddPing adds a ping data point
func (c *PingChart) AddPing(latencyMs float64) {
    c.mu.Lock()
    defer c.mu.Unlock()
    
    if len(c.history) >= c.maxPoints {
        c.history = c.history[1:]
    }
    c.history = append(c.history, latencyMs)
    
    if latencyMs > c.maxLatency {
        c.maxLatency = latencyMs
    }
    c.redraw()
}

func (c *PingChart) redraw() {
    c.mu.RLock()
    defer c.mu.RUnlock()
    
    c.container.Objects = nil
    if len(c.history) == 0 {
        return
    }
    
    width := c.container.Size().Width
    height := c.container.Size().Height
    if width < 10 || height < 10 {
        return
    }
    
    step := width / float32(c.maxPoints)
    if c.maxLatency == 0 {
        c.maxLatency = 1
    }
    
    // Draw background
    bg := canvas.NewRectangle(color.RGBA{30, 30, 40, 255})
    bg.Resize(fyne.NewSize(width, height))
    bg.Move(fyne.NewPos(0, 0))
    c.container.Add(bg)
    
    // Draw threshold lines
    goodY := height - float32(c.goodThreshold/c.maxLatency)*height
    warnY := height - float32(c.warnThreshold/c.maxLatency)*height
    
    goodLine := canvas.NewLine(color.RGBA{16, 185, 129, 100})
    goodLine.Position1 = fyne.NewPos(0, goodY)
    goodLine.Position2 = fyne.NewPos(width, goodY)
    goodLine.StrokeWidth = 1
    c.container.Add(goodLine)
    
    warnLine := canvas.NewLine(color.RGBA{245, 158, 11, 100})
    warnLine.Position1 = fyne.NewPos(0, warnY)
    warnLine.Position2 = fyne.NewPos(width, warnY)
    warnLine.StrokeWidth = 1
    c.container.Add(warnLine)
    
    // Draw ping line
    for i := 1; i < len(c.history); i++ {
        x1 := float32(i-1) * step
        y1 := height - float32(c.history[i-1]/c.maxLatency)*height
        x2 := float32(i) * step
        y2 := height - float32(c.history[i]/c.maxLatency)*height
        
        lineColor := color.RGBA{16, 185, 129, 255}
        if c.history[i] > c.warnThreshold {
            lineColor = color.RGBA{239, 68, 68, 255}
        } else if c.history[i] > c.goodThreshold {
            lineColor = color.RGBA{245, 158, 11, 255}
        }
        
        line := canvas.NewLine(lineColor)
        line.Position1 = fyne.NewPos(x1, y1)
        line.Position2 = fyne.NewPos(x2, y2)
        line.StrokeWidth = 2
        c.container.Add(line)
    }
    
    c.container.Refresh()
}

// SetThresholds sets the good and warning thresholds
func (c *PingChart) SetThresholds(good, warn float64) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.goodThreshold = good
    c.warnThreshold = warn
    c.redraw()
}

// Clear clears all ping data
func (c *PingChart) Clear() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.history = make([]float64, 0, c.maxPoints)
    c.maxLatency = 100
    c.redraw()
}

// GetAverageLatency returns the average latency
func (c *PingChart) GetAverageLatency() float64 {
    c.mu.RLock()
    defer c.mu.RUnlock()
    
    if len(c.history) == 0 {
        return 0
    }
    
    var sum float64
    for _, v := range c.history {
        sum += v
    }
    return sum / float64(len(c.history))
}

// MovementChart displays movement patterns (X vs Y)
type MovementChart struct {
    container *fyne.Container
    points    []fyne.Position
    maxPoints int
    color     color.Color
    mu        sync.RWMutex
}

// NewMovementChart creates a chart for movement tracking
func NewMovementChart() fyne.CanvasObject {
    chart := &MovementChart{
        points:    make([]fyne.Position, 0, 200),
        maxPoints: 200,
        color:     color.RGBA{99, 102, 241, 255},
    }
    chart.container = container.NewWithoutLayout()
    return chart.container
}

// AddPoint adds a movement point
func (c *MovementChart) AddPoint(x, y float32) {
    c.mu.Lock()
    defer c.mu.Unlock()
    
    if len(c.points) >= c.maxPoints {
        c.points = c.points[1:]
    }
    c.points = append(c.points, fyne.NewPos(x, y))
    c.redraw()
}

func (c *MovementChart) redraw() {
    c.mu.RLock()
    defer c.mu.RUnlock()
    
    c.container.Objects = nil
    if len(c.points) < 2 {
        return
    }
    
    width := c.container.Size().Width
    height := c.container.Size().Height
    
    // Find bounds
    minX, maxX, minY, maxY := c.points[0].X, c.points[0].X, c.points[0].Y, c.points[0].Y
    for _, p := range c.points {
        if p.X < minX {
            minX = p.X
        }
        if p.X > maxX {
            maxX = p.X
        }
        if p.Y < minY {
            minY = p.Y
        }
        if p.Y > maxY {
            maxY = p.Y
        }
    }
    
    rangeX := maxX - minX
    rangeY := maxY - minY
    if rangeX == 0 {
        rangeX = 1
    }
    if rangeY == 0 {
        rangeY = 1
    }
    
    // Draw movement trail
    for i := 1; i < len(c.points); i++ {
        x1 := (c.points[i-1].X - minX) / rangeX * width
        y1 := height - (c.points[i-1].Y-minY)/rangeY*height
        x2 := (c.points[i].X - minX) / rangeX * width
        y2 := height - (c.points[i].Y-minY)/rangeY*height
        
        line := canvas.NewLine(c.color)
        line.Position1 = fyne.NewPos(x1, y1)
        line.Position2 = fyne.NewPos(x2, y2)
        line.StrokeWidth = 2
        c.container.Add(line)
    }
    
    // Draw current position
    if len(c.points) > 0 {
        last := c.points[len(c.points)-1]
        x := (last.X - minX) / rangeX * width
        y := height - (last.Y-minY)/rangeY*height
        dot := canvas.NewCircle(color.RGBA{239, 68, 68, 255})
        dot.Position = fyne.NewPos(x-4, y-4)
        dot.Resize(fyne.NewSize(8, 8))
        c.container.Add(dot)
    }
    
    c.container.Refresh()
}

// Clear clears all points
func (c *MovementChart) Clear() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.points = make([]fyne.Position, 0, c.maxPoints)
    c.redraw()
}
