#! /usr/bin/env python3
"""Minimal PyQt6 scaffold for Air Mouse Server UI

This file is a starting point for evaluating a PyQt6 migration. It is intentionally small and
shows how to create a main window, a theme toggle, IP selection, and a log area.

Run:
    python pyqt_scaffold/main.py

Install:
    pip install PyQt6 qasync

Notes:
- This scaffold does not start the TCP server; integrate `server.AirMouseTCPServer` in a QThread
  or use `qasync` to run asyncio inside PyQt's event loop.
"""

import sys
from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QLabel, QPushButton, QTextEdit, QComboBox, QCheckBox
)
from PyQt6.QtGui import QIcon
from PyQt6.QtCore import Qt

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle('Air Mouse Server - PyQt6 Preview')
        self.setMinimumSize(900, 640)
        central = QWidget()
        self.setCentralWidget(central)
        layout = QVBoxLayout()
        central.setLayout(layout)

        # Header
        header = QHBoxLayout()
        title = QLabel('Air Mouse Server')
        title.setStyleSheet('font-size: 20pt; font-weight: 600;')
        header.addWidget(title)
        header.addStretch()
        self.theme_toggle = QComboBox()
        self.theme_toggle.addItems(['Dark', 'Light', 'Pure Black', 'High Contrast'])
        self.theme_toggle.currentTextChanged.connect(self.on_theme_changed)
        header.addWidget(self.theme_toggle)
        layout.addLayout(header)

        # Body: left controls and right log
        body = QHBoxLayout()
        # Left column
        left = QVBoxLayout()
        left.addWidget(QLabel('Selected IP'))
        self.ip_combo = QComboBox()
        self.ip_combo.addItems(['127.0.0.1', '192.168.1.2'])
        left.addWidget(self.ip_combo)
        self.start_btn = QPushButton('Start Server')
        self.stop_btn = QPushButton('Stop Server')
        self.stop_btn.setEnabled(False)
        left.addWidget(self.start_btn)
        left.addWidget(self.stop_btn)
        left.addStretch()
        body.addLayout(left, 1)

        # Right column: log
        right = QVBoxLayout()
        right.addWidget(QLabel('Live Log'))
        self.log_area = QTextEdit()
        self.log_area.setReadOnly(True)
        right.addWidget(self.log_area)
        body.addLayout(right, 2)

        layout.addLayout(body)

        # Footer
        footer = QHBoxLayout()
        self.always_on_top = QCheckBox('Always on top')
        self.always_on_top.stateChanged.connect(self.on_always_on_top)
        footer.addWidget(self.always_on_top)
        footer.addStretch()
        layout.addLayout(footer)

        # Connect signals
        self.start_btn.clicked.connect(self.on_start)
        self.stop_btn.clicked.connect(self.on_stop)

    def on_theme_changed(self, txt: str):
        if txt == 'Light':
            self.setStyleSheet('QWidget { background: #f5f7fa; color: #111; }')
        elif txt == 'Pure Black':
            self.setStyleSheet('QWidget { background: #000; color: #fff; }')
        elif txt == 'High Contrast':
            self.setStyleSheet('QWidget { background: #fff; color: #000; }')
        else:
            self.setStyleSheet('QWidget { background: #0f1115; color: #e5e7eb; }')

    def on_always_on_top(self, state):
        self.setWindowFlag(Qt.WindowType.WindowStaysOnTopHint, bool(state))
        self.show()

    def on_start(self):
        self.log_area.append('Starting server (scaffold only)')
        self.start_btn.setEnabled(False)
        self.stop_btn.setEnabled(True)

    def on_stop(self):
        self.log_area.append('Stopping server (scaffold only)')
        self.start_btn.setEnabled(True)
        self.stop_btn.setEnabled(False)


def main():
    app = QApplication(sys.argv)
    w = MainWindow()
    w.show()
    sys.exit(app.exec())

if __name__ == '__main__':
    main()
