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
	BorderColor  color.Color
	BgColor      color.Color
	CornerRadius float32
}

// NewGlassCard creates a new glass card with default styling.
func NewGlassCard(content fyne.CanvasObject) *GlassCard {
	card := &GlassCard{
		Content:      content,
		BorderColor:  color.RGBA{148, 163, 184, 70},
		BgColor:      color.RGBA{9, 15, 30, 235},
		CornerRadius: 24,
	}
	card.ExtendBaseWidget(card)
	return card
}

func (c *GlassCard) CreateRenderer() fyne.WidgetRenderer {
	bg := canvas.NewRectangle(c.BgColor)
	bg.CornerRadius = c.CornerRadius

	border := canvas.NewRectangle(c.BorderColor)
	border.CornerRadius = c.CornerRadius
	border.StrokeWidth = 1

	shadow := canvas.NewRectangle(color.RGBA{0, 0, 0, 40})
	shadow.CornerRadius = c.CornerRadius

	items := []fyne.CanvasObject{shadow, bg, border}
	if c.Content != nil {
		items = append(items, container.NewPadded(container.NewMax(c.Content)))
	}

	root := container.NewMax(items...)
	return widget.NewSimpleRenderer(root)
}

func (c *GlassCard) MinSize() fyne.Size {
	if c.Content != nil {
		s := c.Content.MinSize()
		return fyne.NewSize(s.Width+24, s.Height+24)
	}
	return fyne.NewSize(200, 100)
}