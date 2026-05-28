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
        self.root.title("Air Mouse Server")
        self.root.geometry("1180x820")
        self.root.minsize(1020, 720)

        self.bg_color = "#0f1115"
        self.surface = "#171b22"
        self.surface_alt = "#1d2430"
        self.card_bg = "#202734"
        self.fg_color = "#e5e7eb"
        self.muted_color = "#96a0ae"
        self.border_color = "#2b3341"
        self.log_bg = "#0c1016"
        self.accent = CONFIG["accent_color"]
        self.success = "#2ecc71"
        self.warning = "#f5a524"
        self.danger = "#ef5b5b"

        self.root.configure(bg=self.bg_color)
        self.root.option_add("*Font", "Segoe UI 10")
        self.root.option_add("*TCombobox*Listbox.font", "Segoe UI 10")
        self.root.option_add("*TButton.padding", 8)
        self._setup_styles()
        self.setup_ui()

        self.tcp_server = AirMouseTCPServer(self.log, self.update_stats_display)
        # IP provider function for UDP discovery (will be set after UI is ready)
        self.udp_server = UDPDiscoveryServer(CONFIG["discovery_port"], self.log,
                                             ip_provider=self.get_selected_ip)
        self.loop = None
        self.tcp_task = None

        self.refresh_ip_list()
        self.update_qr_code()

    def _setup_styles(self):
        style = ttk.Style(self.root)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass

        style.configure("TFrame", background=self.bg_color)
        style.configure("Card.TFrame", background=self.card_bg, relief="flat")
        style.configure("Header.TFrame", background=self.surface)
        style.configure("TLabel", background=self.bg_color, foreground=self.fg_color)
        style.configure("Title.TLabel", background=self.surface, foreground=self.fg_color, font=("Segoe UI Semibold", 24))
        style.configure("Subtitle.TLabel", background=self.surface, foreground=self.muted_color, font=("Segoe UI", 11))
        style.configure("Section.TLabel", background=self.card_bg, foreground=self.fg_color, font=("Segoe UI Semibold", 12))
        style.configure("Metric.TLabel", background=self.card_bg, foreground=self.fg_color, font=("Segoe UI Semibold", 13))
        style.configure("Hint.TLabel", background=self.card_bg, foreground=self.muted_color, font=("Segoe UI", 9))
        style.configure("Status.TLabel", background=self.surface, foreground=self.fg_color, font=("Segoe UI Semibold", 10))
        style.configure("TCheckbutton", background=self.card_bg, foreground=self.fg_color)
        style.configure("TButton", background=self.card_bg, foreground=self.fg_color, borderwidth=0, focusthickness=0)
        style.map("TButton", background=[("active", self.surface_alt)])
        style.configure("Primary.TButton", background=self.accent, foreground="white", font=("Segoe UI Semibold", 10))
        style.map("Primary.TButton", background=[("active", self._adjust_color(self.accent, -18))])
        style.configure("Danger.TButton", background=self.danger, foreground="white", font=("Segoe UI Semibold", 10))
        style.map("Danger.TButton", background=[("active", self._adjust_color(self.danger, -18))])
        style.configure("Accent.TButton", background=self.surface_alt, foreground=self.fg_color)
        style.map("Accent.TButton", background=[("active", self.card_bg)])
        style.configure("Dark.Horizontal.TScale", background=self.card_bg, troughcolor=self.surface_alt, sliderthickness=18)
        style.configure("Dark.TCombobox", fieldbackground=self.surface_alt, background=self.surface_alt, foreground=self.fg_color, arrowcolor=self.fg_color)
        style.map("Dark.TCombobox", fieldbackground=[("readonly", self.surface_alt)], foreground=[("readonly", self.fg_color)])

    def _adjust_color(self, hex_color: str, offset: int) -> str:
        hex_color = hex_color.lstrip("#")
        red = max(0, min(255, int(hex_color[0:2], 16) + offset))
        green = max(0, min(255, int(hex_color[2:4], 16) + offset))
        blue = max(0, min(255, int(hex_color[4:6], 16) + offset))
        return f"#{red:02x}{green:02x}{blue:02x}"

    def _card(self, parent, title: str, subtitle: Optional[str] = None):
        frame = tk.Frame(parent, bg=self.card_bg, highlightthickness=1, highlightbackground=self.border_color)
        frame.columnconfigure(0, weight=1)
        header = tk.Frame(frame, bg=self.card_bg)
        header.grid(row=0, column=0, sticky="ew", padx=18, pady=(16, 0))
        ttk.Label(header, text=title, style="Section.TLabel").pack(anchor="w")
        if subtitle:
            ttk.Label(header, text=subtitle, style="Hint.TLabel").pack(anchor="w", pady=(4, 0))
        body = tk.Frame(frame, bg=self.card_bg)
        body.grid(row=1, column=0, sticky="nsew", padx=18, pady=16)
        return frame, body

    def _status_pill(self, parent, text: str, color: str):
        pill = tk.Label(parent, text=text, bg=color, fg="white", font=("Segoe UI Semibold", 10), padx=12, pady=5)
        return pill

    def setup_ui(self):
        shell = tk.Frame(self.root, bg=self.bg_color)
        shell.pack(fill=tk.BOTH, expand=True)

        header = tk.Frame(shell, bg=self.surface, highlightthickness=1, highlightbackground=self.border_color)
        header.pack(fill=tk.X, padx=18, pady=(18, 12))

        header_inner = tk.Frame(header, bg=self.surface)
        header_inner.pack(fill=tk.X, padx=22, pady=20)

        left_header = tk.Frame(header_inner, bg=self.surface)
        left_header.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        ttk.Label(left_header, text="Air Mouse Server", style="Title.TLabel").pack(anchor="w")
        ttk.Label(left_header, text="Desktop endpoint, discovery responder, and live motion control dashboard", style="Subtitle.TLabel").pack(anchor="w", pady=(6, 0))

        self.status_pill = self._status_pill(header_inner, "Server stopped", self.danger)
        self.status_pill.pack(side=tk.RIGHT, anchor="e")

        self.root.rowconfigure(1, weight=1)
        self.root.columnconfigure(0, weight=1)

        content = tk.Frame(shell, bg=self.bg_color)
        content.pack(fill=tk.BOTH, expand=True, padx=18, pady=(0, 18))
        content.columnconfigure(0, weight=1)
        content.columnconfigure(1, weight=2)
        content.rowconfigure(0, weight=1)

        left_col = tk.Frame(content, bg=self.bg_color)
        left_col.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        left_col.columnconfigure(0, weight=1)

        right_col = tk.Frame(content, bg=self.bg_color)
        right_col.grid(row=0, column=1, sticky="nsew")
        right_col.rowconfigure(1, weight=1)
        right_col.columnconfigure(0, weight=1)

        summary_card, summary_body = self._card(left_col, "Runtime Summary", "Live server status and counters")
        summary_card.grid(row=0, column=0, sticky="ew", pady=(0, 12))

        summary_top = tk.Frame(summary_body, bg=self.card_bg)
        summary_top.pack(fill=tk.X)
        summary_top.columnconfigure((0, 1, 2), weight=1)

        self.status_label = ttk.Label(summary_top, text="Server stopped", style="Metric.TLabel")
        self.status_label.grid(row=0, column=0, sticky="w")
        self.conn_label = ttk.Label(summary_top, text="Connections: 0", style="Metric.TLabel")
        self.conn_label.grid(row=0, column=1, sticky="w")
        self.stats_label = ttk.Label(summary_top, text="Clicks: 0  •  Dbl: 0  •  Right: 0  •  Scroll: 0", style="Metric.TLabel")
        self.stats_label.grid(row=0, column=2, sticky="w")

        self.current_ip_label = ttk.Label(summary_body, text="Selected endpoint will appear here", style="Hint.TLabel")
        self.current_ip_label.pack(anchor="w", pady=(10, 0))

        ip_card, ip_body = self._card(left_col, "Network Endpoint", "Choose the IP address to advertise to the Android app")
        ip_card.grid(row=1, column=0, sticky="ew", pady=(0, 12))
        ip_body.columnconfigure(0, weight=1)

        selection_row = tk.Frame(ip_body, bg=self.card_bg)
        selection_row.pack(fill=tk.X)

        ttk.Label(selection_row, text="Interface", style="Hint.TLabel").pack(anchor="w")

        self.ip_var = tk.StringVar()
        self.manual_ip_var = tk.StringVar()
        self.manual_ip_var.trace_add("write", self.on_manual_ip_changed)
        self.ip_combo = ttk.Combobox(selection_row, textvariable=self.ip_var, state="readonly", width=34, style="Dark.TCombobox")
        self.ip_combo.pack(fill=tk.X, pady=(6, 8))
        self.ip_combo.bind("<<ComboboxSelected>>", self.on_ip_selected)

        action_row = tk.Frame(ip_body, bg=self.card_bg)
        action_row.pack(fill=tk.X, pady=(6, 0))
        self.refresh_btn = ttk.Button(action_row, text="Refresh", command=self.refresh_ip_list, style="Accent.TButton")
        self.refresh_btn.pack(side=tk.LEFT)
        self.copy_btn = ttk.Button(action_row, text="Copy Endpoint", command=self.copy_ip, style="Accent.TButton")
        self.copy_btn.pack(side=tk.LEFT, padx=(10, 0))

        manual_row = tk.Frame(ip_body, bg=self.card_bg)
        manual_row.pack(fill=tk.X, pady=(12, 0))
        self.manual_check = tk.IntVar()
        self.manual_cb = ttk.Checkbutton(manual_row, text="Use manual IP", variable=self.manual_check, command=self.toggle_manual_ip)
        self.manual_cb.pack(anchor="w")
        self.manual_entry = tk.Entry(manual_row, textvariable=self.manual_ip_var, bg=self.surface_alt, fg=self.fg_color, insertbackground=self.fg_color, relief=tk.FLAT, disabledbackground=self.surface_alt, disabledforeground=self.muted_color)
        self.manual_entry.pack(fill=tk.X, pady=(8, 0))

        qr_card, qr_body = self._card(left_col, "Pairing QR", "Scan this endpoint from the Android app")
        qr_card.grid(row=2, column=0, sticky="ew", pady=(0, 12))

        qr_panel = tk.Frame(qr_body, bg=self.card_bg)
        qr_panel.pack(fill=tk.BOTH, expand=True)
        qr_panel.columnconfigure(0, weight=1)

        self.qr_label = tk.Label(qr_panel, bg=self.card_bg)
        self.qr_label.pack(pady=(6, 10))
        self.qr_text = ttk.Label(qr_panel, text="", style="Hint.TLabel", justify=tk.CENTER)
        self.qr_text.pack(fill=tk.X)

        controls_card, controls_body = self._card(left_col, "Server Controls", "Start or stop the TCP and UDP services")
        controls_card.grid(row=3, column=0, sticky="ew", pady=(0, 12))

        button_row = tk.Frame(controls_body, bg=self.card_bg)
        button_row.pack(fill=tk.X)
        self.start_btn = ttk.Button(button_row, text="Start Server", command=self.start_servers, style="Primary.TButton")
        self.start_btn.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.stop_btn = ttk.Button(button_row, text="Stop Server", command=self.stop_servers, style="Danger.TButton")
        self.stop_btn.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(12, 0))

        sens_card, sens_body = self._card(left_col, "Cursor Sensitivity", "Tune pointer speed for smooth cursor control")
        sens_card.grid(row=4, column=0, sticky="ew")

        sens_body.columnconfigure(0, weight=1)
        sens_top = tk.Frame(sens_body, bg=self.card_bg)
        sens_top.pack(fill=tk.X)
        ttk.Label(sens_top, text="Sensitivity", style="Hint.TLabel").pack(side=tk.LEFT)
        self.sens_value = ttk.Label(sens_top, text=f"{CONFIG['sensitivity']:.2f}", style="Metric.TLabel")
        self.sens_value.pack(side=tk.RIGHT)

        self.sens_slider = ttk.Scale(sens_body, from_=0.2, to=2.0, orient=tk.HORIZONTAL, style="Dark.Horizontal.TScale")
        self.sens_slider.set(CONFIG["sensitivity"])
        self.sens_slider.pack(fill=tk.X, pady=(10, 4))
        self.sens_slider.bind("<ButtonRelease-1>", self.update_sensitivity)

        log_card, log_body = self._card(right_col, "Live Log", "Connections, gestures, discovery responses, and errors")
        log_card.grid(row=0, column=0, sticky="nsew")
        log_body.rowconfigure(0, weight=1)
        log_body.columnconfigure(0, weight=1)

        self.log_area = scrolledtext.ScrolledText(log_body, height=18, bg=self.log_bg, fg=self.fg_color, insertbackground=self.fg_color, font=("SF Mono", 10), relief=tk.FLAT, wrap=tk.WORD, padx=12, pady=12)
        self.log_area.grid(row=0, column=0, sticky="nsew")

        diagnostics_card, diagnostics_body = self._card(right_col, "Server Diagnostics", "Quick status and maintenance actions")
        diagnostics_card.grid(row=1, column=0, sticky="ew", pady=(12, 0))

        diagnostics_top = tk.Frame(diagnostics_body, bg=self.card_bg)
        diagnostics_top.pack(fill=tk.X)
        self.clear_logs_btn = ttk.Button(diagnostics_top, text="Clear Logs", command=self._clear_logs, style="Accent.TButton")
        self.clear_logs_btn.pack(side=tk.LEFT)
        self.qr_hint_label = ttk.Label(diagnostics_top, text="Ready for pairing", style="Hint.TLabel")
        self.qr_hint_label.pack(side=tk.RIGHT)

        live_summary = tk.Frame(diagnostics_body, bg=self.card_bg)
        live_summary.pack(fill=tk.X, pady=(12, 0))
        ttk.Label(live_summary, text="Compatibility", style="Hint.TLabel").pack(anchor="w")
        ttk.Label(live_summary, text="Works best on the same Wi-Fi network with Android 10+ and cleartext traffic enabled.", style="Hint.TLabel", wraplength=500).pack(anchor="w", pady=(4, 0))

        footer = ttk.Label(shell, text="University of Tehran • Embedded Systems Exercise", style="Subtitle.TLabel")
        footer.pack(anchor="center", pady=(0, 12))

        self.log_area.configure(state=tk.NORMAL)
        self.log_area.insert(tk.END, "Air Mouse server is ready. Select an endpoint, then start the server.\n")
        self.log_area.see(tk.END)

    def _clear_logs(self):
        self.log_area.delete("1.0", tk.END)
        self.log_area.insert(tk.END, "Logs cleared.\n")
        self.log_area.see(tk.END)

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
        def append():
            self.log_area.insert(tk.END, f"{msg}\n")
            self.log_area.see(tk.END)

        if self.root.winfo_exists():
            self.root.after(0, append)

    def update_stats_display(self, stats: Dict):
        self.stats_label.config(text=f"Clicks: {stats['clicks']}  •  Dbl: {stats['double_clicks']}  •  Right: {stats['right_clicks']}  •  Scroll: {stats['scrolls']}")
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
        self.status_label.config(text="Server running")
        self.status_pill.config(text="Server running", bg=self.success)

        def run_loop():
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            try:
                self.udp_server.start()
                self.loop.run_until_complete(self.tcp_server.start())
            except OSError as exc:
                self.log(f"❌ Could not start server: {exc}")
                self.root.after(0, lambda: self.status_label.config(text="Server error"))
                self.root.after(0, lambda: self.status_pill.config(text="Server error", bg=self.danger))
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
        self.status_label.config(text="Server stopped")
        self.status_pill.config(text="Server stopped", bg=self.danger)
        self.log("🛑 Servers stopped")

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    gui = AirMouseGUI()
    gui.run()
