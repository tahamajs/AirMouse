//go:build darwin

package control

import "github.com/go-vgo/robotgo"

func (m *mouseController) executeMove(dx, dy float64) {
	robotgo.MoveRelative(int(dx), int(dy))
}

func (m *mouseController) executeClick(button string) {
	switch button {
	case "right":
		robotgo.Click("right", false)
	default:
		robotgo.Click("left", false)
	}
}

func (m *mouseController) executeDoubleClick() {
	robotgo.Click("left", true)
}

func (m *mouseController) executeScroll(delta int) {
	robotgo.Scroll(0, delta)
}
