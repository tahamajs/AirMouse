package ui

import (
	"image/color"
	"math"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ------------------------------------------------------------
// SpeedChart – real‑time movement speed graph
// ------------------------------------------------------------

type SpeedChart struct {
	widget.BaseWidget
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

// NewSpeedChart creates a chart that plots cursor speed over time.
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
	chart.ExtendBaseWidget(chart)
	go chart.updater()
	return chart
}

func (c *SpeedChart) CreateRenderer() fyne.WidgetRenderer {
	return widget.NewSimpleRenderer(c.container)
}

func (c *SpeedChart) MinSize() fyne.Size {
	return fyne.NewSize(500, 200)
}

func (c *SpeedChart) updater() {
	ticker := time.NewTicker(500 * time.Millisecond)
	defer ticker.Stop()

	for range ticker.C {
		speed := 20 + 30*math.Abs(math.Sin(float64(time.Now().UnixNano())/1e9))
		c.AddDataPoint(speed)
	}
}

// AddDataPoint adds a data point to the chart.
func (c *SpeedChart) AddDataPoint(speed float64) {
	c.mu.Lock()
	defer c.mu.Unlock()

	if len(c.history) >= c.maxPoints {
		c.history = c.history[1:]
	}
	c.history = append(c.history, speed)

	if c.autoScale && speed > c.maxSpeed {
		c.maxSpeed = speed
	}
	if c.autoScale && c.maxSpeed > 10 {
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
	c.Refresh()
}

func (c *SpeedChart) redraw() {
	c.mu.RLock()
	defer c.mu.RUnlock()

	c.container.Objects = nil
	if len(c.history) == 0 {
		return
	}

	width := float32(500)
	height := float32(200)

	step := width / float32(c.maxPoints)
	if c.maxSpeed == 0 {
		c.maxSpeed = 1
	}

	// Background
	bg := canvas.NewRectangle(c.bgColor)
	bg.Resize(fyne.NewSize(width, height))
	bg.Move(fyne.NewPos(0, 0))
	c.container.Add(bg)

	// Grid lines
	for i := 0; i <= 4; i++ {
		y := height - (float32(i) * height / 4)
		gridLine := canvas.NewLine(c.gridColor)
		gridLine.Position1 = fyne.NewPos(0, y)
		gridLine.Position2 = fyne.NewPos(width, y)
		gridLine.StrokeWidth = 1
		c.container.Add(gridLine)
	}

	// Speed line
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

	// Fill under curve
	fillColor := color.RGBA{99, 102, 241, 50}
	for i := 0; i < len(c.history)-1; i++ {
		x1 := float32(i) * step
		x2 := float32(i+1) * step
		y1 := height - float32(c.history[i]/c.maxSpeed)*height
		y2 := height - float32(c.history[i+1]/c.maxSpeed)*height

		for x := x1; x < x2; x += 1.5 {
			t := (x - x1) / (x2 - x1)
			y := y1 + t*(y2-y1)
			point := canvas.NewCircle(fillColor)
			point.Move(fyne.NewPos(x, y))
			point.Resize(fyne.NewSize(1.5, 1.5))
			c.container.Add(point)
		}
	}
	c.container.Refresh()
}

// SetColor changes the line colour.
func (c *SpeedChart) SetColor(col color.Color) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.color = col
	c.redraw()
	c.Refresh()
}

// SetAutoScale enables/disables auto‑scaling.
func (c *SpeedChart) SetAutoScale(enabled bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.autoScale = enabled
	if !enabled {
		c.maxSpeed = 100
	}
	c.redraw()
	c.Refresh()
}

// Clear removes all data.
func (c *SpeedChart) Clear() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.history = make([]float64, 0, c.maxPoints)
	c.maxSpeed = 100
	c.redraw()
	c.Refresh()
}

// GetMaxSpeed returns the current maximum speed shown.
func (c *SpeedChart) GetMaxSpeed() float64 {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.maxSpeed
}

// ------------------------------------------------------------
// PingChart – latency graph
// ------------------------------------------------------------

type PingChart struct {
	widget.BaseWidget
	container     *fyne.Container
	history       []float64
	maxPoints     int
	maxLatency    float64
	mu            sync.RWMutex
	goodThreshold float64
	warnThreshold float64
}

func NewPingChart() fyne.CanvasObject {
	chart := &PingChart{
		history:       make([]float64, 0, 60),
		maxPoints:     60,
		maxLatency:    100,
		goodThreshold: 50,
		warnThreshold: 100,
	}
	chart.container = container.NewWithoutLayout()
	chart.ExtendBaseWidget(chart)
	return chart
}

func (c *PingChart) CreateRenderer() fyne.WidgetRenderer {
	return widget.NewSimpleRenderer(c.container)
}

func (c *PingChart) MinSize() fyne.Size {
	return fyne.NewSize(500, 150)
}

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
	c.Refresh()
}

func (c *PingChart) redraw() {
	c.mu.RLock()
	defer c.mu.RUnlock()
	c.container.Objects = nil
	if len(c.history) == 0 {
		return
	}
	width := float32(500)
	height := float32(150)

	step := width / float32(c.maxPoints)
	if c.maxLatency == 0 {
		c.maxLatency = 1
	}

	bg := canvas.NewRectangle(color.RGBA{30, 30, 40, 255})
	bg.Resize(fyne.NewSize(width, height))
	bg.Move(fyne.NewPos(0, 0))
	c.container.Add(bg)

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

	for i := 1; i < len(c.history); i++ {
		x1 := float32(i-1) * step
		y1 := height - float32(c.history[i-1]/c.maxLatency)*height
		x2 := float32(i) * step
		y2 := height - float32(c.history[i]/c.maxLatency)*height

		col := color.RGBA{16, 185, 129, 255}
		if c.history[i] > c.warnThreshold {
			col = color.RGBA{239, 68, 68, 255}
		} else if c.history[i] > c.goodThreshold {
			col = color.RGBA{245, 158, 11, 255}
		}
		line := canvas.NewLine(col)
		line.Position1 = fyne.NewPos(x1, y1)
		line.Position2 = fyne.NewPos(x2, y2)
		line.StrokeWidth = 2
		c.container.Add(line)
	}
	c.container.Refresh()
}

func (c *PingChart) SetThresholds(good, warn float64) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.goodThreshold = good
	c.warnThreshold = warn
	c.redraw()
	c.Refresh()
}

func (c *PingChart) Clear() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.history = make([]float64, 0, c.maxPoints)
	c.maxLatency = 100
	c.redraw()
	c.Refresh()
}

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

// ------------------------------------------------------------
// MovementChart – X/Y scatter trail
// ------------------------------------------------------------

type MovementChart struct {
	widget.BaseWidget
	container *fyne.Container
	points    []fyne.Position
	maxPoints int
	color     color.Color
	mu        sync.RWMutex
}

func NewMovementChart() fyne.CanvasObject {
	chart := &MovementChart{
		points:    make([]fyne.Position, 0, 200),
		maxPoints: 200,
		color:     color.RGBA{99, 102, 241, 255},
	}
	chart.container = container.NewWithoutLayout()
	chart.ExtendBaseWidget(chart)
	return chart
}

func (c *MovementChart) CreateRenderer() fyne.WidgetRenderer {
	return widget.NewSimpleRenderer(c.container)
}

func (c *MovementChart) MinSize() fyne.Size {
	return fyne.NewSize(400, 200)
}

func (c *MovementChart) AddPoint(x, y float32) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if len(c.points) >= c.maxPoints {
		c.points = c.points[1:]
	}
	c.points = append(c.points, fyne.NewPos(x, y))
	c.redraw()
	c.Refresh()
}

func (c *MovementChart) redraw() {
	c.mu.RLock()
	defer c.mu.RUnlock()
	c.container.Objects = nil
	if len(c.points) < 2 {
		return
	}
	width := float32(400)
	height := float32(200)

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
	if len(c.points) > 0 {
		last := c.points[len(c.points)-1]
		x := (last.X - minX) / rangeX * width
		y := height - (last.Y-minY)/rangeY*height
		dot := canvas.NewCircle(color.RGBA{239, 68, 68, 255})
		dot.Move(fyne.NewPos(x-4, y-4))
		dot.Resize(fyne.NewSize(8, 8))
		c.container.Add(dot)
	}
	c.container.Refresh()
}

func (c *MovementChart) Clear() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.points = make([]fyne.Position, 0, c.maxPoints)
	c.redraw()
	c.Refresh()
}