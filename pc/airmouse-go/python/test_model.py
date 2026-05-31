import torch
import numpy as np
from train_lstm import LSTMPredictor
import pickle
import matplotlib.pyplot as plt

def test():
    # Load model
    model = LSTMPredictor(input_size=2, hidden_size=64, num_layers=2, output_size=2)
    model.load_state_dict(torch.load("models/lstm_best.pth"))
    model.eval()

    # Load scaler
    with open("models/scaler.pkl", "rb") as f:
        scaler = pickle.load(f)

    # Simulate a test trajectory (diagonal line)
    test_seq = np.zeros((16, 2))
    for i in range(16):
        test_seq[i] = [i*10, i*10]  # increasing positions

    # Normalise
    test_norm = scaler.transform(test_seq)

    # Predict next step
    input_tensor = torch.FloatTensor(test_norm).unsqueeze(0)  # (1, 16, 2)
    with torch.no_grad():
        pred_norm = model(input_tensor)
        pred = scaler.inverse_transform(pred_norm.numpy())

    print(f"Predicted next (dx, dy): {pred[0][0] - test_seq[-1][0]:.2f}, {pred[0][1] - test_seq[-1][1]:.2f}")

    # Plot
    plt.plot(test_seq[:,0], test_seq[:,1], 'b-o', label='Actual')
    plt.scatter(pred[0][0], pred[0][1], color='red', s=100, label='Predicted')
    plt.legend()
    plt.title("LSTM Prediction Test")
    plt.show()

if __name__ == "__main__":
    test()