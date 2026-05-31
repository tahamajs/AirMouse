#!/usr/bin/env python3
import pyautogui
import time
import csv
import keyboard
import threading
from datetime import datetime
import os

class MouseTracker:
    def __init__(self, filename="mouse_dataset.csv", interval=0.02):  # 20ms = 50Hz
        self.filename = filename
        self.interval = interval
        self.recording = False
        self.data = []
        self.lock = threading.Lock()
    
    def start_recording(self):
        self.recording = True
        self.data = []
        self._record()
    
    def stop_recording(self):
        self.recording = False
        self.save()
    
    def _record(self):
        while self.recording:
            x, y = pyautogui.position()
            self.data.append({
                'timestamp': time.time(),
                'x': x,
                'y': y
            })
            time.sleep(self.interval)
    
    def save(self):
        with open(self.filename, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['timestamp', 'x', 'y'])
            for point in self.data:
                writer.writerow([point['timestamp'], point['x'], point['y']])
        print(f"Saved {len(self.data)} points to {self.filename}")

def main():
    tracker = MouseTracker()
    print("Press 'r' to start/stop recording")
    print("Press 'q' to quit")
    
    def on_press(key):
        try:
            if key.name == 'r':
                if not tracker.recording:
                    tracker.start_recording()
                    print("Recording started...")
                else:
                    tracker.stop_recording()
                    print("Recording stopped")
            elif key.name == 'q':
                if tracker.recording:
                    tracker.stop_recording()
                exit(0)
        except AttributeError:
            pass
    
    keyboard.on_press(on_press)
    keyboard.wait()

if __name__ == "__main__":
    main()