import torch
import torch.nn as nn
import onnx
import onnxruntime as ort
import numpy as np
from torch.utils.data import DataLoader, TensorDataset

class HumanCursorRNN(nn.Module):
    def __init__(self, input_size=4, hidden_size=64, num_layers=2, output_size=2):
        super().__init__()
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        lstm_out, _ = self.lstm(x)
        return self.fc(lstm_out[:, -1, :])

def fine_tune_model(model_path, data_buffer, output_path, epochs=10, lr=0.0001):
    """Fine‑tunes an ONNX model using a small buffer of user data."""
    model = HumanCursorRNN()
    model.load_state_dict(torch.load(model_path))

    seq_len = 16
    features = data_buffer[['x_norm', 'y_norm', 'vx', 'vy']].values
    x, y = [], []
    for i in range(len(features) - seq_len):
        x.append(features[i:i+seq_len])
        y.append(features[i+seq_len, :2])
    dataset = TensorDataset(torch.FloatTensor(x), torch.FloatTensor(y))
    dataloader = DataLoader(dataset, batch_size=32, shuffle=True)

    optimizer = torch.optim.Adam(model.parameters(), lr=lr)
    criterion = nn.MSELoss()

    for epoch in range(epochs):
        for batch_x, batch_y in dataloader:
            optimizer.zero_grad()
            outputs = model(batch_x)
            loss = criterion(outputs, batch_y)
            loss.backward()
            optimizer.step()

    torch.save(model.state_dict(), output_path)
    onnx.export(model, torch.randn(1, seq_len, 4), output_path.replace('.pth', '.onnx'),
                input_names=['input_sequence'], output_names=['movement_delta'],
                dynamic_axes={'input_sequence': {0: 'batch_size', 1: 'sequence_len'}})
    return output_path