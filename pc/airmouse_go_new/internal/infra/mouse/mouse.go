package mouse

type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
}
