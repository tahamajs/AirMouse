package ui

import (
	"image/color"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// GlassCard - A glassmorphism card with blur effect
type GlassCard struct {
	widget.BaseWidget
	Content     fyne.CanvasObject
	BorderColor color.Color
	BgColor     color.Color
	CornerRadius float32
}

// NewGlassCard creates a new glass card
func NewGlassCard(content fyne.CanvasObject) *GlassCard {
	card := &GlassCard{
		Content:      content,
		BorderColor:   color.RGBA{148, 163, 184, 70},
		BgColor:       color.RGBA{15, 23, 42, 200},
		CornerRadius:  18,
	}
	card.ExtendBaseWidget(card)
	return card
}

func (c *GlassCard) CreateRenderer() fyne.WidgetRenderer {
	// Background rectangle with rounded corners
	bg := canvas.NewRectangle(c.BgColor)
	bg.CornerRadius = c.CornerRadius

	// Border
	border := canvas.NewRectangle(c.BorderColor)
	border.CornerRadius = c.CornerRadius
	border.StrokeWidth = 1

	// Shadow
	shadow := canvas.NewRectangle(color.RGBA{0, 0, 0, 30})
	shadow.CornerRadius = c.CornerRadius
	shadow.Move(fyne.NewPos(4, 4))

	// Main container
	container := container.NewWithoutLayout()
	container.Add(shadow)
	container.Add(bg)
	container.Add(border)

	// Add content
	if c.Content != nil {
		container.Add(c.Content)
	}

	return widget.NewSimpleRenderer(container)
}

func (c *GlassCard) MinSize() fyne.Size {
	if c.Content != nil {
		return c.Content.MinSize()
	}
	return fyne.NewSize(200, 100)
}
