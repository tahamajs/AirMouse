import torch
import onnx
import pickle
import numpy as np

# Load the trained model
from train_lstm import LSTMPredictor

model = LSTMPredictor(input_size=2, hidden_size=64, num_layers=2, output_size=2)
model.load_state_dict(torch.load("models/lstm_best.pth"))
model.eval()

# Create dummy input (batch=1, seq_len=16, features=2)
dummy_input = torch.randn(1, 16, 2)

# Export to ONNX
torch.onnx.export(
    model,
    dummy_input,
    "models/lstm_predictor.onnx",
    input_names=['input_sequence'],
    output_names=['predicted_delta'],
    dynamic_axes={'input_sequence': {0: 'batch_size', 1: 'sequence_len'}},
    opset_version=14
)

print("ONNX model saved to models/lstm_predictor.onnx")

# Verify the model
onnx_model = onnx.load("models/lstm_predictor.onnx")
onnx.checker.check_model(onnx_model)
print("ONNX model is valid")