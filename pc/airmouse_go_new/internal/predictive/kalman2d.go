package predictive

type KalmanFilter2D struct {
	x  [4]float64
	P  [4][4]float64
	F  [4][4]float64
	H  [4][4]float64
	Q  [4][4]float64
	R  [4][4]float64
	dt float64
}

func NewKalmanFilter2D(dt float64) *KalmanFilter2D {
	kf := &KalmanFilter2D{dt: dt}
	kf.x = [4]float64{0, 0, 0, 0}
	kf.P = [4][4]float64{
		{100, 0, 0, 0},
		{0, 100, 0, 0},
		{0, 0, 100, 0},
		{0, 0, 0, 100},
	}
	kf.F = [4][4]float64{
		{1, 0, dt, 0},
		{0, 1, 0, dt},
		{0, 0, 1, 0},
		{0, 0, 0, 1},
	}
	kf.H = [4][4]float64{
		{1, 0, 0, 0},
		{0, 1, 0, 0},
		{0, 0, 1, 0},
		{0, 0, 0, 1},
	}
	kf.Q = [4][4]float64{
		{0.1, 0, 0, 0},
		{0, 0.1, 0, 0},
		{0, 0, 0.5, 0},
		{0, 0, 0, 0.5},
	}
	kf.R = [4][4]float64{
		{5, 0, 0, 0},
		{0, 5, 0, 0},
		{0, 0, 1, 0},
		{0, 0, 0, 1},
	}
	return kf
}

func (kf *KalmanFilter2D) Predict() {
	var newX [4]float64
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += kf.F[i][j] * kf.x[j]
		}
		newX[i] = sum
	}
	kf.x = newX

	var FP [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += kf.F[i][k] * kf.P[k][j]
			}
			FP[i][j] = sum
		}
	}
	var Pnew [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += FP[i][k] * kf.F[j][k]
			}
			Pnew[i][j] = sum + kf.Q[i][j]
		}
	}
	kf.P = Pnew
}

func (kf *KalmanFilter2D) Update(dx, dy float64) {
	z := [4]float64{dx, dy, dx / kf.dt, dy / kf.dt}
	var Hx [4]float64
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += kf.H[i][j] * kf.x[j]
		}
		Hx[i] = sum
	}
	y := [4]float64{z[0] - Hx[0], z[1] - Hx[1], z[2] - Hx[2], z[3] - Hx[3]}

	var HP [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += kf.H[i][k] * kf.P[k][j]
			}
			HP[i][j] = sum
		}
	}
	var S [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += HP[i][k] * kf.H[j][k]
			}
			S[i][j] = sum + kf.R[i][j]
		}
	}
	// Diagonal approximation for inverse
	var invS [4][4]float64
	for i := 0; i < 4; i++ {
		invS[i][i] = 1.0 / S[i][i]
	}
	var K [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += HP[i][k] * invS[k][j]
			}
			K[i][j] = sum
		}
	}
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += K[i][j] * y[j]
		}
		kf.x[i] += sum
	}
	var KH [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += K[i][k] * kf.H[k][j]
			}
			KH[i][j] = sum
		}
	}
	var Pnew [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			Pnew[i][j] = kf.P[i][j] - KH[i][j]
		}
	}
	kf.P = Pnew
}

func (kf *KalmanFilter2D) GetPredictedMovement() (dx, dy float64) {
	dx = kf.x[2] * kf.dt
	dy = kf.x[3] * kf.dt
	return
}

func (kf *KalmanFilter2D) GetState() (x, y, vx, vy float64) {
	return kf.x[0], kf.x[1], kf.x[2], kf.x[3]
}

func (kf *KalmanFilter2D) Reset() {
	kf.x = [4]float64{0, 0, 0, 0}
	kf.P = [4][4]float64{
		{100, 0, 0, 0},
		{0, 100, 0, 0},
		{0, 0, 100, 0},
		{0, 0, 0, 100},
	}
}

// SetDT updates the internal time step and adjusts the state transition matrix.
func (kf *KalmanFilter2D) SetDT(dt float64) {
	kf.dt = dt
	kf.F[0][2] = dt
	kf.F[1][3] = dt
}
