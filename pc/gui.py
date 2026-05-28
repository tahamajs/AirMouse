#!/usr/bin/env python3
"""
Air Mouse PC Server – Professional Dark Mode GUI
Features:
- QR code display (updates with selected IP)
- UDP discovery (responds with selected IP/port)
- Real‑time stats, live log, sensitivity slider
- Multi‑IP detection with manual override and refresh
"""

import asyncio
import json
import pyautogui
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import threading
import socket
import os
import sys
from dataclasses import dataclass, field
from typing import Optional, Dict, List
from PIL import Image, ImageTk
import qrcode
from io import BytesIO
import netifaces   # <-- NEW: cross‑platform interface listing

# -------------------- Configuration --------------------
CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "discovery_port": 8081,
    "sensitivity": 0.5,
    "accent_color": "#007acc",
    "selected_ip": "",
    "manual_ip_enabled": False,
    "manual_ip_value": ""
}

CONFIG_FILE = os.path.join(os.path.dirname(__file__), "config.json")

def load_config() -> Dict:
    try:
        with open(CONFIG_FILE, "r", encoding="utf-8") as f:
            loaded = json.load(f)
        return {**CONFIG, **loaded}
    except (OSError, json.JSONDecodeError):
        return CONFIG.copy()

def save_config() -> None:
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(CONFIG, f, indent=4)

CONFIG = load_config()

# -------------------- Mouse Controller --------------------
@dataclass
class MouseController:
    sensitivity: float = CONFIG["sensitivity"]
    click_count: int = 0
    double_click_count: int = 0
    right_click_count: int = 0
    scroll_count: int = 0

    def __post_init__(self):
        pyautogui.FAILSAFE = True
        pyautogui.PAUSE = 0
        pyautogui.MINIMUM_DURATION = 0
        pyautogui.MINIMUM_SLEEP = 0

    def move(self, dx: float, dy: float) -> None:
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        if abs(dx) < 0.15 and abs(dy) < 0.15:
            return
        pyautogui.moveRel(dx, dy, duration=0.0, tween=pyautogui.linear)

    def click(self, button: str = 'left') -> None:
        pyautogui.click(button=button)
        if button == 'left':
            self.click_count += 1
        elif button == 'right':
            self.right_click_count += 1

    def double_click(self) -> None:
        pyautogui.doubleClick()
        self.double_click_count += 1

    def scroll(self, delta: int) -> None:
        pyautogui.scroll(delta)
        self.scroll_count += 1

    def get_stats(self) -> Dict:
        return {
            "clicks": self.click_count,
            "double_clicks": self.double_click_count,
            "right_clicks": self.right_click_count,
            "scrolls": self.scroll_count
        }

# -------------------- UDP Discovery Server --------------------
class UDPDiscoveryServer:
    def __init__(self, port: int, callback, ip_provider):
        self.port = port
        self.callback = callback
        self.ip_provider = ip_provider   # function returning the selected IP
        self.socket = None
        self.running = False

    def start(self):
        if self.running:
            return
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind(('', self.port))
        self.running = True
        threading.Thread(target=self._listen, daemon=True).start()
        self.callback(f"🔍 UDP discovery listening on port {self.port}")

    def _listen(self):
        while self.running:
            try:
                data, addr = self.socket.recvfrom(1024)
                msg = data.decode().strip()
                if msg == "AIRMOUSE_DISCOVER":
                    chosen_ip = self.ip_provider()
                    response = json.dumps({
                        "type": "discovery_response",
                        "port": CONFIG["port"],
                        "ip": chosen_ip   # send the selected IP so the phone can connect
                    })
                    self.socket.sendto(response.encode(), addr)
                    self.callback(f"📡 Responded to discovery from {addr[0]} with IP {chosen_ip}")
            except Exception:
                pass

    def stop(self):
        self.running = False
        if self.socket:
            self.socket.close()

# -------------------- TCP Air Mouse Server --------------------
class AirMouseTCPServer:
    def __init__(self, log_callback, stats_callback):
        self.host = CONFIG["host"]
        self.port = CONFIG["port"]
        self.mouse = MouseController()
        self.log_callback = log_callback
        self.stats_callback = stats_callback
        self._server = None
        self.connections = 0
        self.active_connections = set()

    def log(self, msg: str) -> None:
        if self.log_callback:
            self.log_callback(msg)

    def update_stats(self):
        if self.stats_callback:
            self.stats_callback(self.mouse.get_stats())

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        self.active_connections.add(addr)
        self.connections += 1
        self.log(f"✅ Connected: {addr} (active: {len(self.active_connections)})")
        self.update_stats()
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                try:
                    msg = json.loads(data.decode().strip())
                    await self._process_message(msg, writer)
                except json.JSONDecodeError:
                    self.log(f"⚠️ Invalid JSON from {addr}")
        except Exception as e:
            self.log(f"❌ Error: {e}")
        finally:
            self.active_connections.discard(addr)
            writer.close()
            await writer.wait_closed()
            self.log(f"🔌 Disconnected: {addr} (active: {len(self.active_connections)})")
            self.update_stats()

    async def _process_message(self, msg: dict, writer):
        t = msg.get('type')
        if t == 'move':
            self.mouse.move(msg.get('dx', 0.0), msg.get('dy', 0.0))
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
        else:
            self.log(f"⚠️ Unknown message type: {t}")
        self.update_stats()

    async def _send_ack(self, msg_id, writer):
        if msg_id:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()

    async def start(self):
        self._server = await asyncio.start_server(self.handle_client, self.host, self.port)
        self.log(f"🚀 TCP server listening on {self.host}:{self.port}")
        async with self._server:
            await self._server.serve_forever()

    async def stop(self):
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            self.log("🛑 TCP server stopped")

# -------------------- Professional GUI --------------------
class AirMouseGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("✈️ Air Mouse Server – Professional Edition")
        self.root.geometry("850x750")   # slightly larger for extra UI
        self.root.minsize(750, 650)

        # Colours
        self.bg_color = "#1e1e1e"
        self.fg_color = "#d4d4d4"
        self.accent = CONFIG["accent_color"]
        self.log_bg = "#252526"

        self.root.configure(bg=self.bg_color)
        self.setup_ui()

        self.tcp_server = AirMouseTCPServer(self.log, self.update_stats_display)
        # IP provider function for UDP discovery (will be set after UI is ready)
        self.udp_server = UDPDiscoveryServer(CONFIG["discovery_port"], self.log,
                                             ip_provider=self.get_selected_ip)
        self.loop = None
        self.tcp_task = None

        self.refresh_ip_list()
        self.update_qr_code()

    def setup_ui(self):
        main_frame = tk.Frame(self.root, bg=self.bg_color)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=15, pady=15)

        # Title
        title = tk.Label(main_frame, text="Air Mouse Server Pro", font=("Helvetica", 20, "bold"),
                         bg=self.bg_color, fg=self.accent)
        title.pack(pady=(0, 10))

        # Status bar
        status_frame = tk.Frame(main_frame, bg=self.bg_color)
        status_frame.pack(fill=tk.X, pady=5)
        self.status_label = tk.Label(status_frame, text="● Server stopped", font=("Helvetica", 11),
                                     bg=self.bg_color, fg="red")
        self.status_label.pack(side=tk.LEFT, padx=(0,20))
        self.conn_label = tk.Label(status_frame, text="Connections: 0", font=("Helvetica", 10),
                                   bg=self.bg_color, fg=self.fg_color)
        self.conn_label.pack(side=tk.LEFT, padx=10)
        self.stats_label = tk.Label(status_frame, text="Clicks:0 Dbl:0 Right:0 Scroll:0",
                                    font=("Helvetica", 10), bg=self.bg_color, fg=self.fg_color)
        self.stats_label.pack(side=tk.LEFT, padx=10)

        # ---- New IP management section ----
        ip_frame = tk.LabelFrame(main_frame, text=" Network IP Address ", bg=self.bg_color,
                                 fg=self.fg_color, font=("Helvetica", 10, "bold"))
        ip_frame.pack(fill=tk.X, pady=(10, 5))

        ip_control = tk.Frame(ip_frame, bg=self.bg_color)
        ip_control.pack(fill=tk.X, padx=5, pady=5)

        tk.Label(ip_control, text="Use:", bg=self.bg_color, fg=self.fg_color,
                 font=("Helvetica", 10)).pack(side=tk.LEFT, padx=(0,5))

        self.ip_var = tk.StringVar()
        self.manual_ip_var = tk.StringVar()
        self.manual_ip_var.trace_add("write", self.on_manual_ip_changed)
        self.ip_combo = ttk.Combobox(ip_control, textvariable=self.ip_var,
                                     state="readonly", width=20)
        self.ip_combo.pack(side=tk.LEFT, padx=5)
        self.ip_combo.bind("<<ComboboxSelected>>", self.on_ip_selected)

        self.refresh_btn = tk.Button(ip_control, text="🔄", command=self.refresh_ip_list,
                                     bg=self.bg_color, fg="white", font=("Helvetica", 9))
        self.refresh_btn.pack(side=tk.LEFT, padx=5)

        self.copy_btn = tk.Button(ip_control, text="📋 Copy", command=self.copy_ip,
                                  bg=self.bg_color, fg="white", font=("Helvetica", 9))
        self.copy_btn.pack(side=tk.LEFT, padx=5)

        # Manual IP override (hidden until checkbox)
        self.manual_entry = tk.Entry(ip_control, textvariable=self.manual_ip_var,
                                     width=15, bg="#3c3c3c", fg=self.fg_color,
                                     insertbackground='white', state=tk.DISABLED)
        self.manual_entry.pack(side=tk.LEFT, padx=(15,5))

        self.manual_check = tk.IntVar()
        self.manual_cb = tk.Checkbutton(ip_control, text="Manual", variable=self.manual_check,
                                        command=self.toggle_manual_ip,
                                        bg=self.bg_color, fg=self.fg_color,
                                        selectcolor=self.bg_color)
        self.manual_cb.pack(side=tk.LEFT)

        # Current IP display
        self.current_ip_label = tk.Label(ip_frame, text="", font=("Helvetica", 9, "bold"),
                                         bg=self.bg_color, fg=self.accent)
        self.current_ip_label.pack(pady=(0,5))

        # Left pane: QR code
        left_frame = tk.Frame(main_frame, bg=self.bg_color)
        left_frame.pack(side=tk.LEFT, fill=tk.Y, padx=(0,15))
        self.qr_label = tk.Label(left_frame, bg=self.bg_color)
        self.qr_label.pack()
        tk.Label(left_frame, text="Scan with Android app", font=("Helvetica", 9),
                 bg=self.bg_color, fg=self.fg_color).pack()
        self.qr_text = tk.Label(left_frame, text="", font=("Helvetica", 10, "bold"),
                                bg=self.bg_color, fg=self.accent)
        self.qr_text.pack(pady=(6, 0))

        # Right pane: log area
        log_frame = tk.LabelFrame(main_frame, text=" Live Log ", bg=self.bg_color,
                                  fg=self.fg_color, font=("Helvetica", 10, "bold"))
        log_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)
        self.log_area = scrolledtext.ScrolledText(log_frame, height=22, bg=self.log_bg,
                                                   fg=self.fg_color, insertbackground='white',
                                                   font=("Consolas", 10), wrap=tk.WORD)
        self.log_area.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # Control buttons
        btn_frame = tk.Frame(main_frame, bg=self.bg_color)
        btn_frame.pack(fill=tk.X, pady=15)
        self.start_btn = tk.Button(btn_frame, text="▶ Start Server", command=self.start_servers,
                                   bg=self.accent, fg="white", font=("Helvetica", 10, "bold"),
                                   padx=20, pady=6)
        self.start_btn.pack(side=tk.LEFT, padx=5)
        self.stop_btn = tk.Button(btn_frame, text="⏹ Stop Server", command=self.stop_servers,
                                  bg="#444444", fg="white", state=tk.DISABLED,
                                  font=("Helvetica", 10, "bold"), padx=20, pady=6)
        self.stop_btn.pack(side=tk.LEFT, padx=5)

        # Sensitivity slider
        sens_frame = tk.Frame(main_frame, bg=self.bg_color)
        sens_frame.pack(fill=tk.X, pady=10)
        tk.Label(sens_frame, text="Mouse Sensitivity:", bg=self.bg_color,
                 fg=self.fg_color, font=("Helvetica", 10)).pack(side=tk.LEFT, padx=(0,10))
        self.sens_slider = tk.Scale(sens_frame, from_=0.2, to=2.0, resolution=0.05,
                                    orient=tk.HORIZONTAL, bg=self.bg_color, fg=self.fg_color,
                                    highlightthickness=0, length=250)
        self.sens_slider.set(CONFIG["sensitivity"])
        self.sens_slider.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.sens_slider.bind("<ButtonRelease-1>", self.update_sensitivity)
        self.sens_value = tk.Label(sens_frame, text=f"{CONFIG['sensitivity']:.2f}",
                                   bg=self.bg_color, fg=self.accent, width=6)
        self.sens_value.pack(side=tk.LEFT, padx=(10,0))

        # Footer
        footer = tk.Label(main_frame, text="University of Tehran – Embedded Systems Exercise",
                          font=("Helvetica", 8), bg=self.bg_color, fg="#888888")
        footer.pack(pady=(10,0))

    # ---- NEW IP utility functions ----
    def get_all_ips(self) -> List[str]:
        """Return list of all IPv4 addresses except loopback."""
        ips = []
        try:
            for iface in netifaces.interfaces():
                addrs = netifaces.ifaddresses(iface)
                for addr in addrs.get(netifaces.AF_INET, []):
                    ip = addr.get('addr')
                    if ip and not ip.startswith('127.'):
                        # add interface name in parentheses for clarity
                        ips.append(f"{ip} ({iface})")
        except Exception as e:
            self.log(f"⚠️ IP scan error: {e}")
        return sorted(set(ips), key=lambda x: x.split()[0])

    def refresh_ip_list(self):
        """Refresh the IP combo box and select the best IP."""
        ip_options = self.get_all_ips()
        if not ip_options:
            ip_options = ["127.0.0.1 (no network)"]
        self.ip_combo['values'] = ip_options

        if CONFIG.get("manual_ip_enabled") and CONFIG.get("manual_ip_value"):
            manual_ip = CONFIG["manual_ip_value"].strip()
            self.manual_check.set(1)
            self.ip_combo.config(state=tk.DISABLED)
            self.manual_entry.config(state=tk.NORMAL)
            self.manual_ip_var.set(manual_ip)
            self.current_ip_label.config(text=f"Current IP: {manual_ip}:{CONFIG['port']}")
            self.update_qr_code()
            return

        # Attempt to select a stored or sensible default
        preferred = CONFIG.get("selected_ip", "")
        if preferred and preferred in ip_options:
            self.ip_combo.set(preferred)
        elif ip_options:
            self.ip_combo.set(ip_options[0])
        self.manual_check.set(0)
        self.ip_combo.config(state="readonly")
        self.manual_entry.config(state=tk.DISABLED)
        self.on_ip_selected()

    def get_selected_ip(self) -> str:
        """Return just the IP part from the combo selection."""
        if self.manual_check.get():
            manual_ip = self.manual_ip_var.get().strip()
            if manual_ip:
                return manual_ip
        sel = self.ip_var.get()
        return sel.split()[0] if sel else "127.0.0.1"

    def on_ip_selected(self, event=None):
        """Update QR code and current IP display when selection changes."""
        if self.manual_check.get():
            return
        sel = self.get_selected_ip()
        CONFIG["selected_ip"] = sel
        CONFIG["manual_ip_enabled"] = False
        save_config()
        self.current_ip_label.config(text=f"Current IP: {sel}:{CONFIG['port']}")
        self.update_qr_code()

    def toggle_manual_ip(self):
        """Enable/disable manual IP entry."""
        if self.manual_check.get():
            self.manual_entry.config(state=tk.NORMAL)
            self.ip_combo.config(state=tk.DISABLED)
            self.manual_ip_var.set(self.get_selected_ip())
            CONFIG["manual_ip_enabled"] = True
            CONFIG["manual_ip_value"] = self.manual_ip_var.get().strip()
            save_config()
        else:
            self.manual_entry.config(state=tk.DISABLED)
            self.ip_combo.config(state="readonly")
            CONFIG["manual_ip_enabled"] = False
            CONFIG["manual_ip_value"] = self.manual_ip_var.get().strip()
            CONFIG["selected_ip"] = self.get_selected_ip()
            save_config()
            self.on_ip_selected()

    def on_manual_ip_changed(self, *_):
        """Persist manual IP edits so they survive restarts."""
        if not self.manual_check.get():
            return
        manual_ip = self.manual_ip_var.get().strip()
        if not manual_ip:
            return
        CONFIG["manual_ip_enabled"] = True
        CONFIG["manual_ip_value"] = manual_ip
        save_config()
        self.current_ip_label.config(text=f"Current IP: {manual_ip}:{CONFIG['port']}")
        self.update_qr_code()

    def copy_ip(self):
        """Copy the current endpoint to clipboard."""
        ip = self.get_selected_ip()
        ip_port = f"airmouse://{ip}:{CONFIG['port']}"
        self.root.clipboard_clear()
        self.root.clipboard_append(ip_port)
        self.log(f"📋 Copied to clipboard: {ip_port}")

    def update_qr_code(self):
        """Generate QR code from the currently selected endpoint."""
        ip = self.get_selected_ip()
        if not ip:
            self.qr_text.config(text="No IP selected")
            return
        qr_data = f"airmouse://{ip}:{CONFIG['port']}"
        qr = qrcode.QRCode(
            version=None,
            error_correction=qrcode.constants.ERROR_CORRECT_H,
            box_size=8,
            border=2,
        )
        qr.add_data(qr_data)
        qr.make(fit=True)
        img = qr.make_image(fill_color="#111111", back_color="#ffffff").convert("RGB")
        img = img.resize((220, 220))
        self.qr_image = ImageTk.PhotoImage(img)
        self.qr_label.config(image=self.qr_image)
        if hasattr(self, "qr_text"):
            self.qr_text.config(text=qr_data)

    def log(self, msg: str):
        self.log_area.insert(tk.END, f"{msg}\n")
        self.log_area.see(tk.END)

    def update_stats_display(self, stats: Dict):
        self.stats_label.config(
            text=f"Clicks:{stats['clicks']} Dbl:{stats['double_clicks']} Right:{stats['right_clicks']} Scroll:{stats['scrolls']}"
        )
        self.conn_label.config(text=f"Connections: {len(self.tcp_server.active_connections)}")

    def update_sensitivity(self, event=None):
        CONFIG["sensitivity"] = self.sens_slider.get()
        self.sens_value.config(text=f"{CONFIG['sensitivity']:.2f}")
        if hasattr(self.tcp_server, 'mouse'):
            self.tcp_server.mouse.sensitivity = CONFIG["sensitivity"]
        save_config()
        self.log(f"⚙️ Sensitivity changed to {CONFIG['sensitivity']:.2f}")

    # ---- Server start/stop ----
    def start_servers(self):
        self.start_btn.config(state=tk.DISABLED)
        self.stop_btn.config(state=tk.NORMAL)
        self.status_label.config(text="● Server running", fg="lightgreen")

        def run_loop():
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            try:
                self.udp_server.start()
                self.loop.run_until_complete(self.tcp_server.start())
            except OSError as exc:
                self.log(f"❌ Could not start server: {exc}")
                self.root.after(0, lambda: self.status_label.config(text="● Server error", fg="#ff6b6b"))
                self.root.after(0, lambda: self.start_btn.config(state=tk.NORMAL))
                self.root.after(0, lambda: self.stop_btn.config(state=tk.DISABLED))
            finally:
                self.udp_server.stop()

        self.tcp_task = threading.Thread(target=run_loop, daemon=True)
        self.tcp_task.start()
        self.log("🚀 Servers started (TCP + UDP discovery)")

    def stop_servers(self):
        if self.loop:
            asyncio.run_coroutine_threadsafe(self.tcp_server.stop(), self.loop)
        self.udp_server.stop()
        self.start_btn.config(state=tk.NORMAL)
        self.stop_btn.config(state=tk.DISABLED)
        self.status_label.config(text="● Server stopped", fg="red")
        self.log("🛑 Servers stopped")

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    gui = AirMouseGUI()
    gui.run()
