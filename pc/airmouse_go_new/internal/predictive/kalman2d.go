package predictive

import (
    "math"
    "sync"
    "time"  // Add this import
)

type KalmanFilter2D struct {
    // State vector [x, y, vx, vy]
    x  [4]float64
    P  [4][4]float64  // Covariance matrix
    F  [4][4]float64  // State transition matrix
    H  [4][4]float64  // Observation matrix
    Q  [4][4]float64  // Process noise covariance
    R  [4][4]float64  // Measurement noise covariance
    dt float64
    mu sync.RWMutex
    initialized bool
    stats       KalmanStats
}

type KalmanStats struct {
    TotalUpdates    int64
    AvgInnovation   float64
    LastUpdateTime  time.Time
    ConvergenceTime time.Duration
}

func NewKalmanFilter2D(dt float64) *KalmanFilter2D {
    kf := &KalmanFilter2D{
        dt:          dt,
        initialized: true,
    }
    
    // Initialize state
    kf.x = [4]float64{0, 0, 0, 0}
    
    // Initialize covariance matrix (high uncertainty)
    kf.P = [4][4]float64{
        {100, 0, 0, 0},
        {0, 100, 0, 0},
        {0, 0, 100, 0},
        {0, 0, 0, 100},
    }
    
    // State transition matrix
    kf.F = [4][4]float64{
        {1, 0, dt, 0},
        {0, 1, 0, dt},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }
    
    // Observation matrix
    kf.H = [4][4]float64{
        {1, 0, 0, 0},
        {0, 1, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }
    
    // Process noise
    kf.Q = [4][4]float64{
        {0.1, 0, 0, 0},
        {0, 0.1, 0, 0},
        {0, 0, 0.5, 0},
        {0, 0, 0, 0.5},
    }
    
    // Measurement noise
    kf.R = [4][4]float64{
        {5, 0, 0, 0},
        {0, 5, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    }
    
    return kf
}

func (kf *KalmanFilter2D) Predict() {
    kf.mu.Lock()
    defer kf.mu.Unlock()
    
    if !kf.initialized {
        return
    }
    
    // State prediction: x = F * x
    var newX [4]float64
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += kf.F[i][j] * kf.x[j]
        }
        newX[i] = sum
    }
    kf.x = newX
    
    // Covariance prediction: P = F * P * F' + Q
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
    kf.mu.Lock()
    defer kf.mu.Unlock()
    
    if !kf.initialized {
        kf.x[2] = dx / kf.dt
        kf.x[3] = dy / kf.dt
        kf.initialized = true
        return
    }
    
    // Measurement vector [dx, dy, vx, vy]
    z := [4]float64{dx, dy, dx / kf.dt, dy / kf.dt}
    
    // Innovation: y = z - H*x
    var Hx [4]float64
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += kf.H[i][j] * kf.x[j]
        }
        Hx[i] = sum
    }
    y := [4]float64{z[0] - Hx[0], z[1] - Hx[1], z[2] - Hx[2], z[3] - Hx[3]}
    
    // Innovation covariance: S = H*P*H' + R
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
    
    // Calculate Kalman gain: K = P*H'*inv(S)
    invS := kf.invertMatrix4x4(S)
    
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
    
    // State update: x = x + K*y
    for i := 0; i < 4; i++ {
        sum := 0.0
        for j := 0; j < 4; j++ {
            sum += K[i][j] * y[j]
        }
        kf.x[i] += sum
    }
    
    // Covariance update: P = (I - K*H)*P
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
    
    // Update statistics
    kf.stats.TotalUpdates++
    innovation := math.Sqrt(y[0]*y[0] + y[1]*y[1])
    kf.stats.AvgInnovation = (kf.stats.AvgInnovation*float64(kf.stats.TotalUpdates-1) + innovation) / float64(kf.stats.TotalUpdates)
    kf.stats.LastUpdateTime = time.Now()
}

func (kf *KalmanFilter2D) invertMatrix4x4(m [4][4]float64) [4][4]float64 {
    // Compute determinant
    det := kf.determinant4x4(m)
    if math.Abs(det) < 1e-12 {
        // Return identity if singular
        return [4][4]float64{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
        }
    }
    
    // Compute adjugate matrix
    adj := kf.adjugate4x4(m)
    
    // Inverse = adjugate / determinant
    var inv [4][4]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 4; j++ {
            inv[i][j] = adj[i][j] / det
        }
    }
    return inv
}

func (kf *KalmanFilter2D) determinant4x4(m [4][4]float64) float64 {
    return m[0][0]*kf.determinant3x3(m, 0, 0) -
           m[0][1]*kf.determinant3x3(m, 0, 1) +
           m[0][2]*kf.determinant3x3(m, 0, 2) -
           m[0][3]*kf.determinant3x3(m, 0, 3)
}

func (kf *KalmanFilter2D) determinant3x3(m [4][4]float64, row, col int) float64 {
    // Build 3x3 submatrix
    var sub [3][3]float64
    r := 0
    for i := 0; i < 4; i++ {
        if i == row {
            continue
        }
        c := 0
        for j := 0; j < 4; j++ {
            if j == col {
                continue
            }
            sub[r][c] = m[i][j]
            c++
        }
        r++
    }
    
    // Compute 3x3 determinant
    return sub[0][0]*sub[1][1]*sub[2][2] +
           sub[0][1]*sub[1][2]*sub[2][0] +
           sub[0][2]*sub[1][0]*sub[2][1] -
           sub[0][2]*sub[1][1]*sub[2][0] -
           sub[0][1]*sub[1][0]*sub[2][2] -
           sub[0][0]*sub[1][2]*sub[2][1]
}

func (kf *KalmanFilter2D) adjugate4x4(m [4][4]float64) [4][4]float64 {
    var adj [4][4]float64
    for i := 0; i < 4; i++ {
        for j := 0; j < 4; j++ {
            sign := 1.0
            if (i+j)%2 == 1 {
                sign = -1.0
            }
            adj[j][i] = sign * kf.determinant3x3(m, i, j)
        }
    }
    return adj
}

func (kf *KalmanFilter2D) GetPredictedMovement() (dx, dy float64) {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    dx = kf.x[2] * kf.dt
    dy = kf.x[3] * kf.dt
    return
}

func (kf *KalmanFilter2D) GetState() (x, y, vx, vy float64) {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    return kf.x[0], kf.x[1], kf.x[2], kf.x[3]
}

func (kf *KalmanFilter2D) GetPosition() (x, y float64) {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    return kf.x[0], kf.x[1]
}

func (kf *KalmanFilter2D) GetVelocity() (vx, vy float64) {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    return kf.x[2], kf.x[3]
}

func (kf *KalmanFilter2D) GetConfidence() float64 {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    
    // Confidence based on covariance trace
    trace := kf.P[0][0] + kf.P[1][1] + kf.P[2][2] + kf.P[3][3]
    confidence := 1.0 / (1.0 + trace/100.0)
    if confidence > 1.0 {
        confidence = 1.0
    }
    if confidence < 0.0 {
        confidence = 0.0
    }
    return confidence
}

func (kf *KalmanFilter2D) Reset() {
    kf.mu.Lock()
    defer kf.mu.Unlock()
    
    kf.x = [4]float64{0, 0, 0, 0}
    kf.P = [4][4]float64{
        {100, 0, 0, 0},
        {0, 100, 0, 0},
        {0, 0, 100, 0},
        {0, 0, 0, 100},
    }
    kf.initialized = false
    kf.stats = KalmanStats{}
}

func (kf *KalmanFilter2D) SetDT(dt float64) {
    kf.mu.Lock()
    defer kf.mu.Unlock()
    
    kf.dt = dt
    kf.F[0][2] = dt
    kf.F[1][3] = dt
}

func (kf *KalmanFilter2D) SetNoise(processNoise, measurementNoise float64) {
    kf.mu.Lock()
    defer kf.mu.Unlock()
    
    for i := 0; i < 4; i++ {
        kf.Q[i][i] = processNoise
        kf.R[i][i] = measurementNoise
    }
}

func (kf *KalmanFilter2D) GetStats() KalmanStats {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    return kf.stats
}

func (kf *KalmanFilter2D) IsInitialized() bool {
    kf.mu.RLock()
    defer kf.mu.RUnlock()
    return kf.initialized
}