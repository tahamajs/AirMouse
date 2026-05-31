#!/usr/bin/env python3
import argparse
import torch
import numpy as np
import pickle
from train_lstm import LSTMPredictor

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', required=True, help='Path to .pth model')
    parser.add_argument('--data', required=True, help='Path to new data CSV')
    args = parser.parse_args()

    # Load existing model
    model = LSTMPredictor(input_size=2, hidden_size=64, num_layers=2, output_size=2)
    model.load_state_dict(torch.load(args.model))
    model.train()

    # Load scaler (must be saved during initial training)
    with open("models/scaler.pkl", "rb") as f:
        scaler = pickle.load(f)

    # Load new data
    data = np.loadtxt(args.data, delimiter=',')
    if len(data) < 32:
        return

    # Normalise using saved scaler
    data_norm = scaler.transform(data)

    # Create sequences (simplified: last 16 points predict next 1)
    X, y = [], []
    for i in range(len(data_norm) - 17):
        X.append(data_norm[i:i+16])
        y.append(data_norm[i+16])
    X = torch.FloatTensor(X)
    y = torch.FloatTensor(y)

    # Fine‑tune for a few epochs
    criterion = torch.nn.MSELoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)
    for epoch in range(5):
        pred = model(X)
        loss = criterion(pred, y)
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
        print(f"Online epoch {epoch+1}, loss: {loss.item():.6f}")

    torch.save(model.state_dict(), args.model)
    print("Model updated")

if __name__ == "__main__":
    main()