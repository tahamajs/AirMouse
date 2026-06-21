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
	Content      fyne.CanvasObject
	BorderColor   color.Color
	BgColor       color.Color
	CornerRadius  float32
}

// NewGlassCard creates a new glass card
func NewGlassCard(content fyne.CanvasObject) *GlassCard {
	card := &GlassCard{
		Content:      content,
		BorderColor:   color.RGBA{148, 163, 184, 90},
		BgColor:       color.RGBA{15, 23, 42, 220},
		CornerRadius:  22,
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
	shadow := canvas.NewRectangle(color.RGBA{0, 0, 0, 55})
	shadow.CornerRadius = c.CornerRadius
	shadow.Move(fyne.NewPos(6, 6))

	// Main container
	root := container.NewWithoutLayout()
	root.Add(shadow)
	root.Add(bg)
	root.Add(border)

	// Add content
	if c.Content != nil {
		padded := container.NewPadded(c.Content)
		root.Add(padded)
	}

	return widget.NewSimpleRenderer(root)
}

func (c *GlassCard) MinSize() fyne.Size {
	if c.Content != nil {
		s := c.Content.MinSize()
		return fyne.NewSize(s.Width+24, s.Height+24)
	}
	return fyne.NewSize(200, 100)
}
