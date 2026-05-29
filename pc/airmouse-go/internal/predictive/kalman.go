// internal/predictive/kalman.go
package predictive

import (
    "math"
)

// KalmanFilter2D implements a linear Kalman filter for 2D motion.
// State: [x, y, vx, vy]^T
// Control: [ax, ay] (acceleration – we treat as process noise)
type KalmanFilter2D struct {
    // State vector (4x1)
    x [4]float64

    // Covariance matrix (4x4)
    P [4][4]float64

    // State transition matrix F (4x4)
    F [4][4]float64

    // Control matrix B (4x2)
    B [4][2]float64

    // Measurement matrix H (2x4) – we measure position only
    H [2][4]float64

    // Process noise covariance Q (4x4)
    Q [4][4]float64

    // Measurement noise covariance R (2x2)
    R [2][2]float64

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
    // x' = x + vx*dt
    // y' = y + vy*dt
    // vx' = vx
    // vy' = vy
    kf.F = [4][4]float64{
        {1, 0, dt, 0},
        {0, 1, 0, dt},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }

    // Control matrix (we don't use direct acceleration, but keep identity for future)
    kf.B = [4][2]float64{
        {0, 0},
        {0, 0},
        {1, 0},
        {0, 1},
    }

    // Measurement matrix: we observe position (x,y)
    kf.H = [2][4]float64{
        {1, 0, 0, 0},
        {0, 1, 0, 0},
    }

    // Process noise: small acceleration noise
    qPos := 0.1  // position noise
    qVel := 0.5  // velocity noise
    kf.Q = [4][4]float64{
        {qPos, 0, 0, 0},
        {0, qPos, 0, 0},
        {0, 0, qVel, 0},
        {0, 0, 0, qVel},
    }

    // Measurement noise: sensor (network) noise
    rPos := 5.0 // measurement uncertainty (pixels)
    kf.R = [2][2]float64{
        {rPos, 0},
        {0, rPos},
    }

    return kf
}

// Predict runs the prediction step (no control input).
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
                sum += FP[i][k] * kf.F[j][k] // note: F^T = transpose
            }
            Pnew[i][j] = sum + kf.Q[i][j]
        }
    }
    kf.P = Pnew
}

// Update corrects the state using a measurement (dx, dy movement received).
// Measurement is the observed change in position.
func (kf *KalmanFilter2D) Update(dx, dy float64) {
    // Measurement vector z = [dx, dy] (assumed to be position change)
    z := [2]float64{dx, dy}

    // y = z - H * x
    var Hx [2]float64
    for i := 0; i < 2; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += kf.H[i][j] * kf.x[j]
        }
        Hx[i] = sum
    }
    y := [2]float64{z[0] - Hx[0], z[1] - Hx[1]}

    // S = H * P * H^T + R
    var HP [2][4]float64
    for i := 0; i < 2; i++ {
        for j := 0; j < 4; j++ {
            sum := 0.0
            for k := 0; k < 4; k++ {
                sum += kf.H[i][k] * kf.P[k][j]
            }
            HP[i][j] = sum
        }
    }
    var S [2][2]float64
    for i := 0; i < 2; i++ {
        for j := 0; j < 2; j++ {
            sum := 0.0
            for k := 0; k < 4; k++ {
                sum += HP[i][k] * kf.H[j][k]
            }
            S[i][j] = sum + kf.R[i][j]
        }
    }

    // Compute Kalman gain K = P * H^T * inv(S)
    // First compute PHt = P * H^T (4x2)
    var PHt [4][2]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 2; j++ {
            sum := 0.0
            for k := 0; k < 4; k++ {
                sum += kf.P[i][k] * kf.H[j][k] // H^T index: H[j][k]
            }
            PHt[i][j] = sum
        }
    }
    // inv(S) using 2x2 matrix inverse
    det := S[0][0]*S[1][1] - S[0][1]*S[1][0]
    invS := [2][2]float64{
        {S[1][1] / det, -S[0][1] / det},
        {-S[1][0] / det, S[0][0] / det},
    }
    var K [4][2]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 2; j++ {
            sum := 0.0
            for k := 0; k < 2; k++ {
                sum += PHt[i][k] * invS[k][j]
            }
            K[i][j] = sum
        }
    }

    // Update state: x = x + K * y
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 2; j++ {
            sum += K[i][j] * y[j]
        }
        kf.x[i] += sum
    }

    // Update covariance: P = (I - K*H) * P
    var KH [4][4]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 4; j++ {
            sum := 0.0
            for k := 0; k < 2; k++ {
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
// This is the movement that would happen in the next dt seconds.
func (kf *KalmanFilter2D) GetPredictedMovement() (dx, dy float64) {
    // velocity components are in state[2] and state[3]
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
    // Reset covariance to high uncertainty
    kf.P = [4][4]float64{
        {100, 0, 0, 0},
        {0, 100, 0, 0},
        {0, 0, 100, 0},
        {0, 0, 0, 100},
    }
}