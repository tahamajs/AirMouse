"""
Train a 1D CNN gesture classifier from CSV dataset exported by the Android app.
Place this script and gestures_dataset.csv in the same folder, then run:
    python train_gesture.py
Outputs: gesture_model.tflite, gesture_labels.json
"""

import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Conv1D, MaxPooling1D, Flatten, Dense, Dropout
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping
import tensorflow as tf
import json
import os

def build_model(input_shape, num_classes):
    model = Sequential([
        Conv1D(64, 3, activation='relu', input_shape=input_shape),
        MaxPooling1D(2),
        Conv1D(128, 3, activation='relu'),
        MaxPooling1D(2),
        Conv1D(256, 3, activation='relu'),
        MaxPooling1D(2),
        Flatten(),
        Dense(128, activation='relu'),
        Dropout(0.5),
        Dense(num_classes, activation='softmax')
    ])
    model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
    return model

def main():
    # Load CSV
    if not os.path.exists('gestures_dataset.csv'):
        print("Error: gestures_dataset.csv not found in current directory.")
        return

    df = pd.read_csv('gestures_dataset.csv')
    print(f"Loaded {len(df)} rows.")
    print(f"Unique gestures: {df['gesture_name'].unique()}")

    # Group by session_id to create sequences
    sequences = []
    labels = []
    for session_id, group in df.groupby('session_id'):
        gesture = group['gesture_name'].iloc[0]
        # Use gyro + accel (6 features)
        data = group[['gyro_x', 'gyro_y', 'gyro_z', 'accel_x', 'accel_y', 'accel_z']].values
        if len(data) < 10:
            print(f"Skipping short session {session_id} (len={len(data)})")
            continue
        sequences.append(data)
        labels.append(gesture)

    if len(sequences) < 2:
        print("Need at least two gestures with sufficient samples. Record more.")
        return

    # Pad sequences to same length (use max length in dataset)
    max_len = max(len(seq) for seq in sequences)
    X = np.zeros((len(sequences), max_len, 6))
    for i, seq in enumerate(sequences):
        X[i, :len(seq), :] = seq

    # Encode labels
    le = LabelEncoder()
    y_encoded = le.fit_transform(labels)
    y = to_categorical(y_encoded)
    num_classes = y.shape[1]
    print(f"Number of classes: {num_classes} ({le.classes_})")

    # Train/validation split
    X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y_encoded)

    # Build and train
    model = build_model((max_len, 6), num_classes)
    early_stop = EarlyStopping(monitor='val_loss', patience=15, restore_best_weights=True)
    history = model.fit(X_train, y_train, validation_data=(X_val, y_val),
                        epochs=100, batch_size=32, callbacks=[early_stop], verbose=1)

    # Save as TensorFlow Lite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open('gesture_model.tflite', 'wb') as f:
        f.write(tflite_model)

    # Save labels JSON
    with open('gesture_labels.json', 'w') as f:
        json.dump(le.classes_.tolist(), f)

    print("Training complete.")
    print(f"Model saved as gesture_model.tflite")
    print(f"Labels saved as gesture_labels.json")
    print(f"Final validation accuracy: {max(history.history['val_accuracy']):.3f}")

if __name__ == "__main__":
    main()