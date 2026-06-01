package ui

import (
	"image/color"
	"math"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
)

// SpeedChart displays a simple real‑time movement speed graph.
type SpeedChart struct {
	container *fyne.Container
	history   []float64
	maxPoints int
}

// NewSpeedChart creates a chart that plots cursor speed over time.
func NewSpeedChart() fyne.CanvasObject {
	chart := &SpeedChart{
		history:   make([]float64, 0, 100),
		maxPoints: 100,
	}
	chart.container = container.NewWithoutLayout()
	go chart.updater()
	return chart.container
}

func (c *SpeedChart) updater() {
	for {
		time.Sleep(500 * time.Millisecond)
		// TODO: In production, fetch current speed from mouse controller.
		// Example: speed := mouse.GetCurrentSpeed()
		speed := math.Abs(math.Sin(float64(time.Now().UnixNano()))) * 50 // dummy
		c.addPoint(speed)
		c.redraw()
	}
}

func (c *SpeedChart) addPoint(speed float64) {
	if len(c.history) >= c.maxPoints {
		c.history = c.history[1:]
	}
	c.history = append(c.history, speed)
}

func (c *SpeedChart) redraw() {
	c.container.Objects = nil
	if len(c.history) == 0 {
		return
	}
	width := float32(c.container.Size().Width)
	height := float32(c.container.Size().Height)
	if width < 10 || height < 10 {
		return
	}
	step := width / float32(c.maxPoints)
	maxSpeed := 100.0
	for _, s := range c.history {
		if s > maxSpeed {
			maxSpeed = s
		}
	}
	if maxSpeed == 0 {
		maxSpeed = 1
	}
	// Draw line segments
	for i := 1; i < len(c.history); i++ {
		x1 := float32(i-1) * step
		y1 := height - float32(c.history[i-1]/maxSpeed)*height
		x2 := float32(i) * step
		y2 := height - float32(c.history[i]/maxSpeed)*height
		line := canvas.NewLine(color.RGBA{0, 122, 204, 255})
		line.Position1 = fyne.NewPos(x1, y1)
		line.Position2 = fyne.NewPos(x2, y2)
		line.StrokeWidth = 2
		c.container.Add(line)
	}
	c.container.Refresh()
}