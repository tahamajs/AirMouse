import tensorflow as tf
import json
import numpy as np

# Build a minimal CNN (for placeholder)
model = tf.keras.Sequential([
    tf.keras.layers.Conv1D(8, 3, activation='relu', input_shape=(30, 6)),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(4, activation='softmax')
])
model.compile(optimizer='adam', loss='categorical_crossentropy')

# Dummy training to set weights
dummy_x = np.random.randn(10, 30, 6).astype(np.float32)
dummy_y = tf.keras.utils.to_categorical([0,1,2,3,0,1,2,3,0,1], num_classes=4)
model.fit(dummy_x, dummy_y, epochs=1, verbose=0)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
with open('gesture_model.tflite', 'wb') as f:
    f.write(tflite_model)

# Labels (must match your planned gestures)
labels = ["LeftSwipe", "RightSwipe", "CircleCW", "ThumbsUp"]
with open('gesture_labels.json', 'w') as f:
    json.dump(labels, f)

print("Dummy model and labels created. Copy to Android assets/ folder.")