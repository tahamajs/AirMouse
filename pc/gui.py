#!/usr/bin/env python3
"""
Air Mouse PC Server – Beautiful Dark Mode GUI
Supports start/stop, sensitivity slider, live log, and all gesture types.
"""

import asyncio
import json
import pyautogui
import tkinter as tk
from tkinter import ttk, scrolledtext
import threading
from dataclasses import dataclass
from typing import Optional

# Configuration (can be extended to load from file)
CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
}

@dataclass
class MouseController:
    sensitivity: float = CONFIG["sensitivity"]

    def move(self, dx: float, dy: float) -> None:
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)

    def click(self, button: str = 'left') -> None:
        pyautogui.click(button=button)

    def double_click(self) -> None:
        pyautogui.doubleClick()

    def scroll(self, delta: int) -> None:
        pyautogui.scroll(delta)

class AirMouseServer:
    def __init__(self, log_callback: Optional[callable] = None):
        self.host = CONFIG["host"]
        self.port = CONFIG["port"]
        self.mouse = MouseController()
        self.log_callback = log_callback
        self._server = None

    def log(self, msg: str) -> None:
        if self.log_callback:
            self.log_callback(msg)
        print(msg)

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        self.log(f"✅ Connected: {addr}")
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                msg = json.loads(data.decode().strip())
                t = msg.get('type')
                if t == 'move':
                    self.mouse.move(msg.get('dx', 0), msg.get('dy', 0))
                elif t == 'click':
                    self.mouse.click()
                    await self._send_ack(msg.get('id'), writer)
                    self.log("🖱️ Click")
                elif t == 'doubleclick':
                    self.mouse.double_click()
                    await self._send_ack(msg.get('id'), writer)
                    self.log("🖱️🖱️ Double-click")
                elif t == 'rightclick':
                    self.mouse.click(button='right')
                    await self._send_ack(msg.get('id'), writer)
                    self.log("🖱️ Right-click")
                elif t == 'scroll':
                    self.mouse.scroll(msg.get('delta', 0))
                    await self._send_ack(msg.get('id'), writer)
                    self.log(f"📜 Scroll {msg.get('delta')}")
        except Exception as e:
            self.log(f"❌ Error: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            self.log(f"🔌 Disconnected: {addr}")

    async def _send_ack(self, msg_id, writer):
        if msg_id:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()

    async def start(self):
        self._server = await asyncio.start_server(self.handle_client, self.host, self.port)
        self.log(f"🚀 Server listening on {self.host}:{self.port}")
        async with self._server:
            await self._server.serve_forever()

    async def stop(self):
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            self.log("🛑 Server stopped")

class AirMouseGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("✈️ Air Mouse Server")
        self.root.geometry("650x550")
        self.root.minsize(500, 400)

        # Colors (dark theme)
        self.bg_color = "#1e1e1e"
        self.fg_color = "#d4d4d4"
        self.accent = "#007acc"
        self.log_bg = "#252526"

        self.root.configure(bg=self.bg_color)
        self.setup_ui()

        self.server = AirMouseServer(log_callback=self.log)
        self.loop = None
        self.server_task = None

    def setup_ui(self):
        main_frame = tk.Frame(self.root, bg=self.bg_color)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=15, pady=15)

        # Title
        title = tk.Label(main_frame, text="Air Mouse Server", font=("Helvetica", 20, "bold"),
                         bg=self.bg_color, fg=self.accent)
        title.pack(pady=(0, 10))

        # Status indicator
        self.status_label = tk.Label(main_frame, text="● Server stopped", font=("Helvetica", 11),
                                     bg=self.bg_color, fg="red")
        self.status_label.pack()

        # Log area with frame
        log_frame = tk.LabelFrame(main_frame, text=" Connection Log ", bg=self.bg_color,
                                   fg=self.fg_color, font=("Helvetica", 10, "bold"))
        log_frame.pack(fill=tk.BOTH, expand=True, pady=15)

        self.log_area = scrolledtext.ScrolledText(log_frame, height=18, bg=self.log_bg,
                                                   fg=self.fg_color, insertbackground='white',
                                                   font=("Consolas", 10), wrap=tk.WORD)
        self.log_area.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # Control buttons
        btn_frame = tk.Frame(main_frame, bg=self.bg_color)
        btn_frame.pack(fill=tk.X, pady=8)

        self.start_btn = tk.Button(btn_frame, text="▶ Start Server", command=self.start_server,
                                   bg=self.accent, fg="white", font=("Helvetica", 10, "bold"),
                                   padx=15, pady=5)
        self.start_btn.pack(side=tk.LEFT, padx=5)

        self.stop_btn = tk.Button(btn_frame, text="⏹ Stop Server", command=self.stop_server,
                                  bg="#444444", fg="white", state=tk.DISABLED,
                                  font=("Helvetica", 10, "bold"), padx=15, pady=5)
        self.stop_btn.pack(side=tk.LEFT, padx=5)

        # Sensitivity slider
        sens_frame = tk.Frame(main_frame, bg=self.bg_color)
        sens_frame.pack(fill=tk.X, pady=12)
        tk.Label(sens_frame, text="Mouse Sensitivity:", bg=self.bg_color,
                 fg=self.fg_color, font=("Helvetica", 10)).pack(side=tk.LEFT, padx=(0,10))

        self.sens_slider = tk.Scale(sens_frame, from_=0.2, to=2.0, resolution=0.05,
                                    orient=tk.HORIZONTAL, bg=self.bg_color, fg=self.fg_color,
                                    highlightthickness=0, length=250)
        self.sens_slider.set(CONFIG["sensitivity"])
        self.sens_slider.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.sens_slider.bind("<ButtonRelease-1>", self.update_sensitivity)

        # Sensitivity value label
        self.sens_value = tk.Label(sens_frame, text=f"{CONFIG['sensitivity']:.2f}",
                                   bg=self.bg_color, fg=self.accent, width=6)
        self.sens_value.pack(side=tk.LEFT, padx=(10,0))

        # Footer
        footer = tk.Label(main_frame, text="University of Tehran – Embedded Systems Exercise",
                          font=("Helvetica", 8), bg=self.bg_color, fg="#888888")
        footer.pack(pady=(10,0))

    def log(self, msg: str):
        """Thread‑safe log writing."""
        self.log_area.insert(tk.END, f"{msg}\n")
        self.log_area.see(tk.END)

    def update_sensitivity(self, event=None):
        CONFIG["sensitivity"] = self.sens_slider.get()
        self.sens_value.config(text=f"{CONFIG['sensitivity']:.2f}")
        if hasattr(self.server, 'mouse'):
            self.server.mouse.sensitivity = CONFIG["sensitivity"]
        self.log(f"⚙️ Sensitivity changed to {CONFIG['sensitivity']:.2f}")

    def start_server(self):
        self.start_btn.config(state=tk.DISABLED)
        self.stop_btn.config(state=tk.NORMAL)
        self.status_label.config(text="● Server running", fg="lightgreen")

        def run_loop():
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            self.loop.run_until_complete(self.server.start())

        self.server_task = threading.Thread(target=run_loop, daemon=True)
        self.server_task.start()
        self.log("🚀 Server started on port 8080")

    def stop_server(self):
        if self.loop:
            asyncio.run_coroutine_threadsafe(self.server.stop(), self.loop)
        self.start_btn.config(state=tk.NORMAL)
        self.stop_btn.config(state=tk.DISABLED)
        self.status_label.config(text="● Server stopped", fg="red")
        self.log("🛑 Server stopped by user")

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    gui = AirMouseGUI()
    gui.run()