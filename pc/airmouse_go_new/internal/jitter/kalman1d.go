package jitter

type Kalman1D struct {
	x float64
	P float64
	Q float64
	R float64
}

func NewKalman1D(processNoise, measurementNoise float64) *Kalman1D {
	return &Kalman1D{
		x: 0,
		P: 1,
		Q: processNoise,
		R: measurementNoise,
	}
}

func (k *Kalman1D) Update(z float64) float64 {
	k.P = k.P + k.Q
	K := k.P / (k.P + k.R)
	k.x = k.x + K*(z-k.x)
	k.P = (1 - K) * k.P
	return k.x
}

func (k *Kalman1D) GetState() float64 { return k.x }
func (k *Kalman1D) Reset()            { k.x = 0; k.P = 1 }
