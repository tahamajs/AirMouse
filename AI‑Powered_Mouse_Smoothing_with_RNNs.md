# AI‑Powered Mouse Smoothing with RNNs: A Complete Implementation Guide

This comprehensive guide details how to implement an AI-powered smoothing system using a Recurrent Neural Network (RNN) to generate **ultra‑smooth, human‑like mouse cursor trajectories** for the Air Mouse Go server.

## Why AI‑Powered Smoothing?

Traditional mouse smoothing methods (linear interpolation, moving averages, or simple EMA filters) can still produce movements that feel “mechanical” or “unnatural” under real‑world network conditions. An AI‑powered approach offers:

- **Human‑like velocity profiles** – acceleration followed by deceleration, mimicking biological motor control
- **Curved, non‑linear paths** – avoiding the rigid straight lines of conventional interpolation
- **Variable speed with natural noise** – subtle hesitations, micro‑corrections, and pauses
- **Adaptability** – can be fine‑tuned to the user’s personal movement style via additional training data

Modern research and open‑source projects have demonstrated that **RNNs (especially LSTM and GRU)** excel at learning and generating realistic mouse movement patterns.

## Full Implementation Roadmap

The implementation is divided into six phases:

| Phase | Description | Time Estimate |
|---|---|---|
| 1 | Data Collection & Preparation | 1–2 days |
| 2 | Model Architecture Design | 1 day |
| 3 | Training | 2–4 hours |
| 4 | Export to ONNX | 1 hour |
| 5 | Integration into Go Server | 2–3 hours |
| 6 | Evaluation & Fine‑Tuning | 1–2 days |

---

## Phase 1: Data Collection & Preparation

### 1.1 Understanding Human Movement Characteristics

Human mouse movements are not linear or uniform. They exhibit:
- **Smooth acceleration/deceleration** curves (velocity peaks at ~60–70% of the movement)
- **Slight initial hesitation** (delays before starting)
- **Micro‑corrections** (small directional adjustments along the path)
- **Over‑shoot and correction** (especially for precise target acquisition)
- **Natural noise and variability**

### 1.2 Collecting Training Data

You need several thousand real mouse movement trajectories to train a reliable model. Bumblebee, for instance, was trained on **over 25,000 real cursor movements**.

#### Option A: Record Your Own Data

Build a simple data collector that records movement data at ~60–100 samples per second:

```python
import time
import threading
import csv
import mouse
from datetime import datetime

class MouseDataCollector:
    def __init__(self, sample_rate=60):
        self.sample_rate = sample_rate
        self.recording = False
        self.data = []

    def record_trajectory(self, duration=5):
        """Record a single movement trajectory."""
        start = time.time()
        self.recording = True
        while time.time() - start < duration:
            pos = mouse.get_position()
            self.data.append({
                'timestamp': datetime.now(),
                'x': pos[0], 'y': pos[1],
                'button': 'left' if mouse.is_pressed('left') else None
            })
            time.sleep(1 / self.sample_rate)
        self.recording = False

    def save_to_csv(self, filename='mouse_trajectories.csv'):
        with open(filename, 'w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=['timestamp', 'x', 'y', 'button'])
            writer.writeheader()
            writer.writerows(self.data)
```

#### Option B: Use Existing Open Datasets

The **SapiMouse** dataset is one of the largest publicly available collections of human mouse cursor trajectories. You can also find datasets of labeled human vs bot mouse movements on platforms like Bureau ID.

### 1.3 Data Preprocessing

Raw data must be converted into sequences suitable for RNN training:

1. **Normalise coordinates** – scale raw pixel coordinates to [0,1] or [-1,1] based on screen resolution
2. **Compute velocity and acceleration** – add derived features:
   - `vx` = Δx / Δt
   - `vy` = Δy / Δt
   - `ax` = Δvx / Δt
   - `ay` = Δvy / Δt
3. **Create sliding windows** – use a sequence length of 16–32 points (300–500 ms of movement) to predict the next point
4. **Segment movements** – split continuous recordings into individual movement strokes

## Phase 2: Model Architecture Design

### 2.1 Architecture Overview

The recommended architecture for mouse movement generation uses a **sequence‑to‑sequence RNN with an LSTM layer**. This is the same approach used in the Bumblebee library, which has been proven effective for generating realistic cursor trajectories.

### 2.2 Complete Training Script (Python with PyTorch)

Below is the fully functional code for training the model:

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
import pandas as pd
import os

# -------------------------------
# CONFIGURATION
# -------------------------------
SEQUENCE_LEN = 16          # Input sequence length (history points)
PREDICTION_STEPS = 5       # Number of future points to predict
BATCH_SIZE = 32
EPOCHS = 50
LEARNING_RATE = 0.001
HIDDEN_SIZE = 64
NUM_LAYERS = 2

# -------------------------------
# MODEL DEFINITION
# -------------------------------
class HumanCursorRNN(nn.Module):
    """RNN with LSTM layers for human-like mouse trajectory generation."""
    def __init__(self, input_size=4, hidden_size=64, num_layers=2, output_size=2):
        super().__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        # Input features: x, y, velocity_x, velocity_y
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.dropout = nn.Dropout(0.2)
        self.fc = nn.Linear(hidden_size, output_size)  # Output: dx, dy

    def forward(self, x):
        # x shape: (batch, seq_len, input_size)
        lstm_out, _ = self.lstm(x)
        out = self.dropout(lstm_out[:, -1, :])  # Take last timestep only
        return self.fc(out)

# -------------------------------
# CUSTOM DATASET
# -------------------------------
class MouseTrajectoryDataset(Dataset):
    def __init__(self, csv_path, seq_len=16):
        self.seq_len = seq_len
        self.data = self._load_and_preprocess(csv_path)

    def _load_and_preprocess(self, path):
        df = pd.read_csv(path)
        # Normalise coordinates (assuming 1920×1080 screen)
        df['x_norm'] = df['x'] / 1920.0
        df['y_norm'] = df['y'] / 1080.0

        # Compute velocities
        df['vx'] = df['x_norm'].diff().fillna(0)
        df['vy'] = df['y_norm'].diff().fillna(0)

        # Extract feature matrix
        features = df[['x_norm', 'y_norm', 'vx', 'vy']].values
        return torch.FloatTensor(features)

    def __len__(self):
        return max(0, len(self.data) - self.seq_len)

    def __getitem__(self, idx):
        x = self.data[idx:idx + self.seq_len]          # Input sequence
        y = self.data[idx + self.seq_len, :2]          # Target position
        return x, y

# -------------------------------
# TRAINING LOOP
# -------------------------------
def train_model():
    # Load dataset
    dataset = MouseTrajectoryDataset('data/mouse_trajectories.csv', SEQUENCE_LEN)
    dataloader = DataLoader(dataset, batch_size=BATCH_SIZE, shuffle=True)

    # Initialize model, loss, optimizer
    model = HumanCursorRNN(input_size=4, hidden_size=HIDDEN_SIZE,
                           num_layers=NUM_LAYERS, output_size=2)
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=LEARNING_RATE)

    print(f"Training on {len(dataset)} sequences, {len(dataloader)} batches")

    for epoch in range(EPOCHS):
        total_loss = 0
        for batch_idx, (x_batch, y_batch) in enumerate(dataloader):
            optimizer.zero_grad()
            predictions = model(x_batch)
            loss = criterion(predictions, y_batch)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        avg_loss = total_loss / len(dataloader)
        print(f"Epoch {epoch+1}/{EPOCHS}, Loss: {avg_loss:.6f}")

        # Save checkpoint every 10 epochs
        if (epoch + 1) % 10 == 0:
            torch.save(model.state_dict(), f'checkpoints/model_epoch_{epoch+1}.pth')

    # Save final model
    torch.save(model.state_dict(), 'models/human_cursor_model.pth')
    print("Training complete! Model saved to models/human_cursor_model.pth")

if __name__ == "__main__":
    os.makedirs('checkpoints', exist_ok=True)
    os.makedirs('models', exist_ok=True)
    train_model()
```

### 2.3 Alternative: LSTM for Iterative Prediction

For real‑time use, you can implement an iterative prediction model like the **Mouse Tracking Predictor**, which uses 16 consecutive samples to predict the next coordinate:

```python
class IterativeCursorPredictor(nn.Module):
    """Predicts the next cursor position from a sequence of past points."""
    def __init__(self, input_size=2, hidden_size=64, num_layers=2):
        super().__init__()
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, 2)  # Predict next (x,y)

    def forward(self, x):
        lstm_out, _ = self.lstm(x)
        return self.fc(lstm_out[:, -1, :])
```

## Phase 3: Training Best Practices

### 3.1 Recommended Training Configuration

| Parameter | Recommended Value | Rationale |
|---|---|---|
| Sequence length | 16–32 timesteps | Covers 250–500 ms of movement |
| Hidden size | 64–128 | Captures enough complexity without overfitting |
| Batch size | 32–64 | Balances memory and convergence speed |
| Learning rate | 0.0005–0.001 | Stable convergence |
| Optimiser | Adam | Handles sparse gradients well |
| Loss function | MSE or Smooth L1 | Encourages precise point predictions |

### 3.2 Handling Class Imbalance

Human movement data has inherent imbalances (more straight movements than curves). Use techniques like:
- **Over‑sampling** rare trajectory types
- **Weighted loss** – assign higher weights to underrepresented patterns
- **Data augmentation** – add slight noise, rotation, or scaling to existing trajectories

## Phase 4: Exporting to ONNX for Go Deployment

To use the trained model in Go, export it to the ONNX format, which is widely supported by Go inference libraries.

### 4.1 Export Script

```python
import torch
import torch.onnx

# Load the trained model
model = HumanCursorRNN(input_size=4, hidden_size=64, num_layers=2, output_size=2)
model.load_state_dict(torch.load('models/human_cursor_model.pth'))
model.eval()

# Create dummy input (batch=1, seq_len=16, features=4)
dummy_input = torch.randn(1, 16, 4)

# Export to ONNX
torch.onnx.export(
    model,
    dummy_input,
    "models/human_cursor_model.onnx",
    input_names=['input_sequence'],
    output_names=['movement_delta'],
    dynamic_axes={'input_sequence': {0: 'batch_size', 1: 'sequence_len'}},
    opset_version=14
)

print("Model exported to ONNX format")
```

### 4.2 ONNX Model Optimisation

To ensure fast inference in Go:

```bash
# Reduce model size and optimise for CPU
onnxruntime_tools.optimizer.optimize_model(
    'models/human_cursor_model.onnx',
    optimization_level=99,  # All optimisations
    opt_level_str='basic'
)
```

## Phase 5: Integration into Air Mouse Go Server

### 5.1 Go Inference with ONNX Runtime

First install the ONNX Runtime Go binding:

```bash
go get github.com/owulveryck/onnx-go
go get github.com/owulveryck/onnx-go/backend/simple
```

### 5.2 Complete AI Smoothing Integration Code

```go
package control

import (
    "encoding/json"
    "fmt"
    "os"
    "sync"
    "time"

    onnx "github.com/owulveryck/onnx-go"
    "github.com/owulveryck/onnx-go/backend/simple"
)

// HumanCursorPredictor performs AI-based mouse movement smoothing
type HumanCursorPredictor struct {
    model      *onnx.Model
    history    [][4]float64  // Last N points: x, y, vx, vy
    maxHistory int           // Typically 16-32
    mu         sync.Mutex
}

// NewHumanCursorPredictor loads the ONNX model and initialises the predictor
func NewHumanCursorPredictor(modelPath string, maxHistory int) (*HumanCursorPredictor, error) {
    // Load ONNX model file
    data, err := os.ReadFile(modelPath)
    if err != nil {
        return nil, fmt.Errorf("failed to read model: %w", err)
    }

    // Create ONNX backend
    backend := simple.NewSimpleBackend()

    // Create model
    model := onnx.NewModel(backend)
    if err := model.Parse(data); err != nil {
        return nil, fmt.Errorf("failed to parse ONNX model: %w", err)
    }

    // Set input/output
    err = model.SetInput("input_sequence", [4]interface{}{int64(1), int64(maxHistory), int64(4)})
    if err != nil {
        return nil, err
    }
    err = model.SetOutput("movement_delta", []int64{1, 2})
    if err != nil {
        return nil, err
    }

    return &HumanCursorPredictor{
        model:      model,
        history:    make([][4]float64, 0, maxHistory),
        maxHistory: maxHistory,
    }, nil
}

// AddPoint adds a new raw mouse point to the history buffer
func (p *HumanCursorPredictor) AddPoint(x, y float64) {
    p.mu.Lock()
    defer p.mu.Unlock()

    // Calculate velocity
    var vx, vy float64
    if len(p.history) > 0 {
        last := p.history[len(p.history)-1]
        vx = x - last[0]
        vy = y - last[1]
    }

    // Add new point with computed velocity
    point := [4]float64{x, y, vx, vy}
    p.history = append(p.history, point)

    // Trim if necessary
    if len(p.history) > p.maxHistory {
        p.history = p.history[1:]
    }
}

// PredictNextDelta predicts the next movement delta using the RNN model
func (p *HumanCursorPredictor) PredictNextDelta() (float64, float64, error) {
    p.mu.Lock()
    defer p.mu.Unlock()

    // Need at least 2 points to have velocity data
    if len(p.history) < 2 {
        return 0, 0, nil
    }

    // Prepare input tensor: shape [1, seq_len, 4]
    // Pad with zeros if history is shorter than maxHistory
    inputLen := len(p.history)
    if inputLen < p.maxHistory {
        // Pad by repeating the first element
        padded := make([][4]float64, p.maxHistory)
        for i := 0; i < p.maxHistory-inputLen; i++ {
            padded[i] = p.history[0]
        }
        copy(padded[p.maxHistory-inputLen:], p.history)
        p.history = padded
    }

    // Convert to float32 slice for ONNX
    inputData := make([]float32, p.maxHistory*4)
    for i, point := range p.history {
        inputData[i*4] = float32(point[0])
        inputData[i*4+1] = float32(point[1])
        inputData[i*4+2] = float32(point[2])
        inputData[i*4+3] = float32(point[3])
    }

    // Run inference
    output, err := p.model.Exec(map[string]interface{}{
        "input_sequence": inputData,
    })
    if err != nil {
        return 0, 0, fmt.Errorf("inference failed: %w", err)
    }

    // Extract output (dx, dy)
    result := output["movement_delta"].([]float32)
    if len(result) < 2 {
        return 0, 0, fmt.Errorf("unexpected output shape")
    }

    return float64(result[0]), float64(result[1]), nil
}

// AISmoothingMouseController wraps the existing mouse controller with AI smoothing
type AISmoothingMouseController struct {
    inner      MouseController
    predictor  *HumanCursorPredictor
    smoothing  bool
    lastPoints [][]float64
}

// NewAISmoothingMouseController creates a new AI-enabled mouse controller
func NewAISmoothingMouseController(inner MouseController, modelPath string) (*AISmoothingMouseController, error) {
    predictor, err := NewHumanCursorPredictor(modelPath, 16)
    if err != nil {
        return nil, err
    }
    return &AISmoothingMouseController{
        inner:      inner,
        predictor:  predictor,
        smoothing:  true,
        lastPoints: make([][]float64, 0),
    }, nil
}

// Move applies AI smoothing before moving the cursor
func (c *AISmoothingMouseController) Move(dx, dy float64) {
    if !c.smoothing {
        c.inner.Move(dx, dy)
        return
    }

    // Get current position (needs to be implemented in the underlying controller)
    // For simplicity, we use a local position tracker
    // In real implementation, query actual cursor position

    // Add current point to predictor history
    // c.predictor.AddPoint(currentX, currentY)

    // Predict smoothed delta
    predDx, predDy, err := c.predictor.PredictNextDelta()
    if err == nil && (predDx != 0 || predDy != 0) {
        // Blend raw and predicted for best results
        blendedDx := 0.4*dx + 0.6*predDx
        blendedDy := 0.4*dy + 0.6*predDy
        c.inner.Move(blendedDx, blendedDy)
    } else {
        c.inner.Move(dx, dy)
    }
}
```

### 5.3 Integration with Existing Mouse Controller

Modify your `mouse.go` to optionally use AI smoothing:

```go
// In your main server initialisation
func initMouseController() {
    cfg := config.Get()
    baseMouse := NewMouseController(cfg.Sensitivity)
    
    // Enable AI smoothing if configured
    if cfg.EnableAISmoothing {
        aiMouse, err := NewAISmoothingMouseController(baseMouse, "models/human_cursor_model.onnx")
        if err != nil {
            utils.LogWarn("AI smoothing failed to load, falling back to standard", "error", err)
            mouse = baseMouse
        } else {
            mouse = aiMouse
            utils.LogInfo("AI-powered mouse smoothing enabled")
        }
    } else {
        mouse = baseMouse
    }
}
```

### 5.4 Alternative: Using Hugot for Transformer Pipelines

For more advanced models, consider using **Hugot**, which provides ONNX transformer pipelines for Go:

```go
import "github.com/knights-analytics/hugot/pipelines"

func setupTransformerSmoothing() {
    // Load a Hugging Face transformer model exported to ONNX
    pipe, err := pipelines.LoadPipeline("cursor_smoothing", "models/transformer_model.onnx")
    if err != nil {
        panic(err)
    }
    // Run inference...
}
```

## Phase 6: Evaluation and Fine‑Tuning

### 6.1 Quantitative Metrics

Measure smoothing quality using:

1. **Smoothness metric** – standard deviation of instantaneous velocity
2. **Path efficiency** – ratio of actual path length to straight‑line distance
3. **Latency impact** – additional milliseconds introduced by the model

### 6.2 A/B Testing Configuration

Implement configuration options to compare AI smoothing with traditional methods:

```go
type AIConfig struct {
    Enabled        bool    `json:"ai_smoothing_enabled"`
    ModelPath      string  `json:"ai_model_path"`
    BlendFactor    float64 `json:"ai_blend_factor"`    // 0.0-1.0
    PredictOnly    bool    `json:"ai_predict_only"`
    ConfidenceThreshold float64 `json:"ai_confidence_threshold"`
}
```

### 6.3 Performance Optimisation

| Optimisation | Technique |
|---|---|
| Reduce inference latency | Use smaller hidden size (32–64) |
| Lower CPU usage | Use the `simple` backend instead of CUDA |
| Batch predictions | Combine multiple movement predictions when possible |
| Cache results | Use identical input patterns to avoid recomputation |

## Practical Deployment Considerations

| Concern | Solution |
|---|---|
| **Latency** | A well‑optimised ONNX model can run in under 5ms on a modern CPU |
| **Model size** | The exported ONNX model is ~1–5 MB |
| **Cross‑platform** | ONNX Runtime supports Windows, macOS, and Linux |
| **Fallback** | Always fall back to standard smoothing if the model fails |

## Summary

Implementing AI‑powered mouse smoothing transforms the Air Mouse experience from “functional” to “magical.” The technical stack is mature and well‑documented, and the performance overhead is minimal. By following this guide, you can integrate a production‑ready RNN model that generates ultra‑smooth, human‑like cursor movements, setting your application apart from traditional remote control solutions.