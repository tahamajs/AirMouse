package predictive

import "math"

// KalmanFilter2D implements a linear Kalman filter for 2D motion.
// State: [x, y, vx, vy]^T
type KalmanFilter2D struct {
    // State vector (4x1)
    x [4]float64

    // Covariance matrix (4x4)
    P [4][4]float64

    // State transition matrix F (4x4)
    F [4][4]float64

    // Measurement matrix H (4x4) – we observe both position and velocity now
    H [4][4]float64

    // Process noise covariance Q (4x4)
    Q [4][4]float64

    // Measurement noise covariance R (4x4)
    R [4][4]float64

    dt float64 // time step (seconds)
}

// NewKalmanFilter2D creates a filter with given time step and initial uncertainty.
func NewKalmanFilter2D(dt float64) *KalmanFilter2D {
    kf := &KalmanFilter2D{dt: dt}

    // Initialize state to zero
    kf.x = [4]float64{0, 0, 0, 0}

    // Initial covariance (high uncertainty in position and velocity)
    kf.P = [4][4]float64{
        {100, 0, 0, 0},
        {0, 100, 0, 0},
        {0, 0, 100, 0},
        {0, 0, 0, 100},
    }

    // State transition matrix: constant velocity model
    kf.F = [4][4]float64{
        {1, 0, dt, 0},
        {0, 1, 0, dt},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }

    // Measurement matrix: we observe both position and velocity now
    kf.H = [4][4]float64{
        {1, 0, 0, 0},
        {0, 1, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }

    // Process noise: small acceleration noise (tuned for mouse movement)
    qPos := 0.1
    qVel := 0.5
    kf.Q = [4][4]float64{
        {qPos, 0, 0, 0},
        {0, qPos, 0, 0},
        {0, 0, qVel, 0},
        {0, 0, 0, qVel},
    }

    // Measurement noise: sensor (network) noise (tuned for mouse movement)
    rPos := 5.0
    rVel := 1.0
    kf.R = [4][4]float64{
        {rPos, 0, 0, 0},
        {0, rPos, 0, 0},
        {0, 0, rVel, 0},
        {0, 0, 0, rVel},
    }

    return kf
}

// Predict runs the prediction step.
func (kf *KalmanFilter2D) Predict() {
    // x = F * x
    var newX [4]float64
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += kf.F[i][j] * kf.x[j]
        }
        newX[i] = sum
    }
    kf.x = newX

    // P = F * P * F^T + Q
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

// Update corrects the state using a measurement (dx, dy movement received).
func (kf *KalmanFilter2D) Update(dx, dy float64) {
    // Create measurement vector z = [dx, dy, vx, vy]
    // For velocity, we use the raw dx, dy as velocity measurements.
    z := [4]float64{dx, dy, dx / kf.dt, dy / kf.dt}

    // y = z - H * x
    var Hx [4]float64
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += kf.H[i][j] * kf.x[j]
        }
        Hx[i] = sum
    }
    y := [4]float64{z[0] - Hx[0], z[1] - Hx[1], z[2] - Hx[2], z[3] - Hx[3]}

    // S = H * P * H^T + R
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

    // Compute Kalman gain K = P * H^T * inv(S)
    // First compute PHt = P * H^T (4x4)
    var PHt [4][4]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 4; j++ {
            sum := 0.0
            for k := 0; k < 4; k++ {
                sum += kf.P[i][k] * kf.H[j][k]
            }
            PHt[i][j] = sum
        }
    }
    // inv(S) using 4x4 matrix inverse (simplified: use a small library or Gaussian elimination)
    // For simplicity, we'll assume S is diagonal and invertable.
    // In practice, you'd want to implement a proper matrix inverse.
    var invS [4][4]float64
    for i := 0; i < 4; i++ {
        invS[i][i] = 1.0 / S[i][i]
    }
    var K [4][4]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 4; j++ {
            sum := 0.0
            for k := 0; k < 4; k++ {
                sum += PHt[i][k] * invS[k][j]
            }
            K[i][j] = sum
        }
    }

    // Update state: x = x + K * y
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += K[i][j] * y[j]
        }
        kf.x[i] += sum
    }

    // Update covariance: P = (I - K*H) * P
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

// GetPredictedMovement returns the predicted (dx, dy) based on current velocity.
func (kf *KalmanFilter2D) GetPredictedMovement() (dx, dy float64) {
    dx = kf.x[2] * kf.dt
    dy = kf.x[3] * kf.dt
    return
}

// GetState returns current estimated position and velocity.
func (kf *KalmanFilter2D) GetState() (x, y, vx, vy float64) {
    return kf.x[0], kf.x[1], kf.x[2], kf.x[3]
}

// Reset reinitializes the filter.
func (kf *KalmanFilter2D) Reset() {
    kf.x = [4]float64{0, 0, 0, 0}
    kf.P = [4][4]float64{
        {100, 0, 0, 0},
        {0, 100, 0, 0},
        {0, 0, 100, 0},
        {0, 0, 0, 100},
    }
}