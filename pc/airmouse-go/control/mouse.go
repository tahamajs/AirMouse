package control

// MouseController defines the methods that any platform‑specific controller must implement.
type MouseController interface {
	Move(dx, dy float64)
	Click(button string)
	DoubleClick()
	Scroll(delta int)
	Stats() (clicks, dbl, right, scroll int64)
}