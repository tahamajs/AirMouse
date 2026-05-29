package control

type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
	Stats() (clicks, dbl, right, scroll int64)
	SetSensitivity(s float64)   // <-- new
}