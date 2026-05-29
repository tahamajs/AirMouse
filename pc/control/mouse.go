package control

import (
	"math"
	"sync/atomic"
	"github.com/go-vgo/robotgo"
)

type MouseController struct {
	sensitivity     float64
	clickCount      int64
	doubleClickCnt  int64
	rightClickCnt   int64
	scrollCount     int64
}

func NewMouseController(sensitivity float64) *MouseController {
	return &MouseController{sensitivity: sensitivity}
}

func (m *MouseController) Move(dx, dy float64) {
	dx = math.Max(-50, math.Min(50, dx*m.sensitivity))
	dy = math.Max(-50, math.Min(50, dy*m.sensitivity))
	if math.Abs(dx) < 0.15 && math.Abs(dy) < 0.15 {
		return
	}
	x, y := robotgo.Location()
	robotgo.Move(int(float64(x)+dx), int(float64(y)+dy))
}

func (m *MouseController) Click(button string) {
	robotgo.Click(button)
	if button == "left" {
		atomic.AddInt64(&m.clickCount, 1)
	} else if button == "right" {
		atomic.AddInt64(&m.rightClickCnt, 1)
	}
}

func (m *MouseController) DoubleClick() {
	robotgo.Click("left", true)
	atomic.AddInt64(&m.doubleClickCnt, 1)
}

func (m *MouseController) Scroll(delta int) {
	robotgo.Scroll(0, delta)
	atomic.AddInt64(&m.scrollCount, 1)
}

func (m *MouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}