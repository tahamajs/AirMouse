package ui

import (
	"bytes"
	"image"
	"image/color"
	"image/draw"
	"image/png"
	"sync"

	"fyne.io/fyne/v2"
)

// ---------------------------------------------------------------------
// Generated application icon (256×256)
// ---------------------------------------------------------------------

var (
	appIconOnce sync.Once
	appIconData []byte
)

// generateAppIcon creates a nice application icon as a PNG byte slice.
// It draws a glowing circular background with a white mouse cursor arrow.
func generateAppIcon() []byte {
	const size = 256
	img := image.NewRGBA(image.Rect(0, 0, size, size))

	// Fill with a dark blue‑grey background
	bg := color.RGBA{20, 25, 45, 255}
	draw.Draw(img, img.Bounds(), &image.Uniform{bg}, image.Point{}, draw.Src)

	cx, cy := size/2, size/2
	radius := size/2 - 8

	// Draw a radial gradient from centre to edge (simulated by layered circles)
	for r := radius; r > 0; r -= 2 {
		// Brightness increases towards centre
		alpha := uint8(180 - (radius-r)*180/radius)
		if alpha < 0 {
			alpha = 0
		}
		col := color.RGBA{100, 130, 255, alpha}
		drawCircle(img, cx, cy, r, col)
	}

	// Draw a pure white circle in the centre for a highlight
	drawCircle(img, cx, cy, radius/4, color.RGBA{255, 255, 255, 60})

	// Draw the mouse cursor arrow
	drawCursorArrow(img, cx, cy, size/3, color.RGBA{255, 255, 255, 255})

	var buf bytes.Buffer
	_ = png.Encode(&buf, img)
	return buf.Bytes()
}

// drawCircle fills a circle of radius r at (cx,cy) with colour c.
func drawCircle(img *image.RGBA, cx, cy, r int, c color.Color) {
	for y := -r; y <= r; y++ {
		for x := -r; x <= r; x++ {
			if x*x+y*y <= r*r {
				img.Set(cx+x, cy+y, c)
			}
		}
	}
}

// drawCursorArrow draws a simple arrow pointing up‑left (like a mouse cursor).
func drawCursorArrow(img *image.RGBA, cx, cy, size int, col color.Color) {
	// Arrow: a triangle and a short stem.
	// Points for the arrowhead (pointing up‑left)
	// Tip at (cx - size/2, cy - size/2)
	// Base at (cx + size/4, cy - size/2) and (cx + size/4, cy + size/2)
	// But we want a diagonal arrow: use a rotated triangle.
	// Instead, we draw a right triangle: base horizontal, tip up‑left.
	// Let's define three points:
	// P1 (tip): (cx - size/2, cy - size/2)
	// P2: (cx + size/4, cy - size/2)
	// P3: (cx - size/2, cy + size/4)
	// That gives an arrow pointing up‑left.

	x1, y1 := cx-size/2, cy-size/2
	x2, y2 := cx+size/3, cy-size/3
	x3, y3 := cx-size/3, cy+size/3

	fillTriangle(img, x1, y1, x2, y2, x3, y3, col)

	// Add a short stem (line) from the arrow base down to the right
	for i := 0; i < size/6; i++ {
		for j := 0; j < size/10; j++ {
			img.Set(cx+size/4+i, cy+size/3+j, col)
			img.Set(cx+size/4+i, cy-size/3-j, col)
		}
	}
}

// fillTriangle draws a filled triangle defined by three points.
func fillTriangle(img *image.RGBA, x1, y1, x2, y2, x3, y3 int, col color.Color) {
	minX := min3(x1, x2, x3)
	maxX := max3(x1, x2, x3)
	minY := min3(y1, y2, y3)
	maxY := max3(y1, y2, y3)

	for y := minY; y <= maxY; y++ {
		for x := minX; x <= maxX; x++ {
			if pointInTriangle(x, y, x1, y1, x2, y2, x3, y3) {
				img.Set(x, y, col)
			}
		}
	}
}

// pointInTriangle tests if point (px,py) is inside the triangle.
func pointInTriangle(px, py, x1, y1, x2, y2, x3, y3 int) bool {
	d1 := sign(px, py, x1, y1, x2, y2)
	d2 := sign(px, py, x2, y2, x3, y3)
	d3 := sign(px, py, x3, y3, x1, y1)
	hasNeg := (d1 < 0) || (d2 < 0) || (d3 < 0)
	hasPos := (d1 > 0) || (d2 > 0) || (d3 > 0)
	return !(hasNeg && hasPos)
}

func sign(px, py, x1, y1, x2, y2 int) int {
	return (px-x2)*(y1-y2) - (x1-x2)*(py-y2)
}

func min3(a, b, c int) int {
	m := a
	if b < m {
		m = b
	}
	if c < m {
		m = c
	}
	return m
}

func max3(a, b, c int) int {
	m := a
	if b > m {
		m = b
	}
	if c > m {
		m = c
	}
	return m
}

// ---------------------------------------------------------------------
// Embedded small icons (16×16 PNGs) – kept for compatibility
// ---------------------------------------------------------------------

var (
	// RunningIconData – green icon for "running" state.
	RunningIconData = []byte{
		0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
		0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
		0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
		0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF,
		0x61, 0x00, 0x00, 0x00, 0x20, 0x49, 0x44, 0x41,
		0x54, 0x38, 0xCB, 0x63, 0xFC, 0xFF, 0xFF, 0x3F,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x00,
		0xD4, 0x5D, 0xAD, 0xB3, 0x01, 0xE4, 0x6C, 0x69,
		0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
		0xAE, 0x42, 0x60, 0x82,
	}

	// StoppedIconData – red/gray icon for "stopped" state.
	StoppedIconData = []byte{
		0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
		0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
		0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
		0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF,
		0x61, 0x00, 0x00, 0x00, 0x25, 0x49, 0x44, 0x41,
		0x54, 0x38, 0xCB, 0x63, 0xFC, 0xFF, 0xFF, 0x3F,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x00,
		0xDE, 0x72, 0xC4, 0x14, 0x11, 0x21, 0xA4, 0xFE,
		0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
		0xAE, 0x42, 0x60, 0x82,
	}

	// PlayIconData – play button icon.
	PlayIconData = []byte{
		0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
		0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
		0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
		0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF,
		0x61, 0x00, 0x00, 0x00, 0x1A, 0x49, 0x44, 0x41,
		0x54, 0x38, 0xCB, 0x63, 0xFC, 0xFF, 0xFF, 0x3F,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x83, 0x86, 0x01,
		0x00, 0x01, 0x77, 0x01, 0x07, 0x63, 0x33, 0x6D,
		0x13, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
		0x44, 0xAE, 0x42, 0x60, 0x82,
	}

	// StopIconData – stop button icon.
	StopIconData = []byte{
		0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
		0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
		0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
		0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF,
		0x61, 0x00, 0x00, 0x00, 0x1A, 0x49, 0x44, 0x41,
		0x54, 0x38, 0xCB, 0x63, 0xFC, 0xFF, 0xFF, 0x3F,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x83, 0x86, 0x01,
		0x00, 0xE2, 0xAD, 0x03, 0x04, 0x68, 0xD2, 0x5C,
		0x0D, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
		0x44, 0xAE, 0x42, 0x60, 0x82,
	}

	// RestartIconData – restart button icon.
	RestartIconData = []byte{
		0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
		0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
		0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
		0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF,
		0x61, 0x00, 0x00, 0x00, 0x24, 0x49, 0x44, 0x41,
		0x54, 0x38, 0xCB, 0x63, 0xFC, 0xFF, 0xFF, 0x3F,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
		0x03, 0x83, 0x86, 0x01, 0x00, 0xFB, 0x0C, 0x03,
		0x02, 0x9F, 0x02, 0xBB, 0xCF, 0x00, 0x00, 0x00,
		0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60,
		0x82,
	}
)

// ---------------------------------------------------------------------
// Public getters for fyne.Resource objects
// ---------------------------------------------------------------------

// GetAppIcon returns the main application icon as a fyne.Resource.
// The icon is generated once and cached.
func GetAppIcon() fyne.Resource {
	appIconOnce.Do(func() {
		appIconData = generateAppIcon()
	})
	return &fyne.StaticResource{
		StaticName:    "app_icon.png",
		StaticContent: appIconData,
	}
}

// GetRunningIcon returns the "running" state icon.
func GetRunningIcon() fyne.Resource {
	return &fyne.StaticResource{
		StaticName:    "running_icon.png",
		StaticContent: RunningIconData,
	}
}

// GetStoppedIcon returns the "stopped" state icon.
func GetStoppedIcon() fyne.Resource {
	return &fyne.StaticResource{
		StaticName:    "stopped_icon.png",
		StaticContent: StoppedIconData,
	}
}

// GetPlayIcon returns a play button icon.
func GetPlayIcon() fyne.Resource {
	return &fyne.StaticResource{
		StaticName:    "play_icon.png",
		StaticContent: PlayIconData,
	}
}

// GetStopIcon returns a stop button icon.
func GetStopIcon() fyne.Resource {
	return &fyne.StaticResource{
		StaticName:    "stop_icon.png",
		StaticContent: StopIconData,
	}
}

// GetRestartIcon returns a restart button icon.
func GetRestartIcon() fyne.Resource {
	return &fyne.StaticResource{
		StaticName:    "restart_icon.png",
		StaticContent: RestartIconData,
	}
}

// ---------------------------------------------------------------------
// Utility: generate a coloured circle icon programmatically
// ---------------------------------------------------------------------

// GenerateColoredIcon creates a PNG byte slice of a filled circle of the given
// colour and size (in pixels). The resulting image can be used as a
// fyne.Resource (e.g., for status indicators).
func GenerateColoredIcon(col color.Color, size int) ([]byte, error) {
	img := image.NewRGBA(image.Rect(0, 0, size, size))

	// Transparent background
	for x := 0; x < size; x++ {
		for y := 0; y < size; y++ {
			img.Set(x, y, color.Transparent)
		}
	}

	// Draw filled circle
	center := size / 2
	radius := size/2 - 1
	for x := 0; x < size; x++ {
		for y := 0; y < size; y++ {
			dx := x - center
			dy := y - center
			if dx*dx+dy*dy <= radius*radius {
				img.Set(x, y, col)
			}
		}
	}

	var buf bytes.Buffer
	if err := png.Encode(&buf, img); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}