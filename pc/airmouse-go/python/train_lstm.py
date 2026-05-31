import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader, random_split
from sklearn.preprocessing import StandardScaler
import os
import pickle

class CursorDataset(Dataset):
    def __init__(self, csv_file, sequence_length=30, predict_offset=1, normalize=True):
        """
        csv_file: CSV with 'x', 'y' columns
        sequence_length: number of past points to use as input (recommended 16–32)
        predict_offset: how many steps ahead to predict (1 for next step)
        """
        df = pd.read_csv(csv_file)
        self.data = df[['x', 'y']].values.astype(np.float32)
        self.sequence_length = sequence_length
        self.predict_offset = predict_offset
        
        if normalize:
            self.scaler = StandardScaler()
            self.data = self.scaler.fit_transform(self.data)
        else:
            self.scaler = None
        
        self.normalized = normalize
    
    def __len__(self):
        return len(self.data) - self.sequence_length - self.predict_offset + 1
    
    def __getitem__(self, idx):
        x = self.data[idx : idx + self.sequence_length]
        y = self.data[idx + self.sequence_length + self.predict_offset - 1]
        return torch.from_numpy(x), torch.from_numpy(y)
    
    def inverse_transform(self, data):
        if self.scaler:
            return self.scaler.inverse_transform(data)
        return data

class LSTMPredictor(nn.Module):
    def __init__(self, input_size=2, hidden_size=64, num_layers=2, output_size=2, dropout=0.2):
        super().__init__()
        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0
        )
        self.fc = nn.Linear(hidden_size, output_size)
    
    def forward(self, x):
        # x shape: (batch, seq_len, 2)
        lstm_out, _ = self.lstm(x)
        last_out = lstm_out[:, -1, :]
        return self.fc(last_out)

def train(model, train_loader, val_loader, epochs=100, lr=0.001, patience=10):
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=lr)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, patience=5, factor=0.5)
    
    best_val_loss = float('inf')
    patience_counter = 0
    
    for epoch in range(epochs):
        model.train()
        train_loss = 0
        for x, y in train_loader:
            optimizer.zero_grad()
            pred = model(x)
            loss = criterion(pred, y)
            loss.backward()
            optimizer.step()
            train_loss += loss.item()
        
        model.eval()
        val_loss = 0
        with torch.no_grad():
            for x, y in val_loader:
                pred = model(x)
                val_loss += criterion(pred, y).item()
        
        train_loss /= len(train_loader)
        val_loss /= len(val_loader)
        scheduler.step(val_loss)
        
        print(f"Epoch {epoch+1}/{epochs}, Train Loss: {train_loss:.6f}, Val Loss: {val_loss:.6f}")
        
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            patience_counter = 0
            torch.save(model.state_dict(), "models/lstm_best.pth")
        else:
            patience_counter += 1
            if patience_counter >= patience:
                print(f"Early stopping at epoch {epoch+1}")
                break

def main():
    print("Loading dataset...")
    dataset = CursorDataset("mouse_dataset.csv", sequence_length=16, predict_offset=1)
    train_size = int(0.8 * len(dataset))
    val_size = len(dataset) - train_size
    train_dataset, val_dataset = random_split(dataset, [train_size, val_size])
    
    batch_size = 64
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False)
    
    model = LSTMPredictor(input_size=2, hidden_size=64, num_layers=2, output_size=2)
    train(model, train_loader, val_loader, epochs=100)
    
    # Save scaler
    if dataset.scaler:
        with open("models/scaler.pkl", "wb") as f:
            pickle.dump(dataset.scaler, f)
    
    print("Training complete. Best model saved to models/lstm_best.pth")

if __name__ == "__main__":
    os.makedirs("models", exist_ok=True)
    main()