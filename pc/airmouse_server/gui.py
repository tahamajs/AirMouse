import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, filedialog
import tkinter.font as tkfont
import threading
import asyncio
import socket
import os
import json
import time
from typing import List, Dict, Optional
import netifaces
from PIL import Image

from .config import CONFIG, save_config
from .mouse_controller import MouseController
from .udp_discovery import UDPDiscoveryServer
from .mdns_advertiser import MDNSAdvertiser
from .tcp_server import AirMouseTCPServer
from .qr_manager import QRManager
from .tray_manager import TrayManager
from .performance_monitor import PerformanceMonitor

class AirMouseGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Air Mouse Server")
        self.root.geometry("1280x850")
        self.root.minsize(1020, 720)

        # Color palette
        self.bg_color = "#0f1115"
        self.surface = "#171b22"
        self.surface_alt = "#1d2430"
        self.card_bg = "#202734"
        self.fg_color = "#e5e7eb"
        self.muted_color = "#96a0ae"
        self.border_color = "#2b3341"
        self.log_bg = "#0c1016"
        self.accent = CONFIG.get("accent_color", "#007acc")
        self.success = "#2ecc71"
        self.warning = "#f5a524"
        self.danger = "#ef5b5b"

        self.root.configure(bg=self.bg_color)
        self._setup_styles()
        self.setup_ui()

        # Backend services
        self.qr_manager = QRManager()
        self.tcp_server = AirMouseTCPServer(
            self.log, self.update_stats_display,
            connections_callback=self._update_connection_list,
            move_callback=None
        )
        self.udp_server = UDPDiscoveryServer(self.log, ip_provider=self.get_selected_ip)
        self.mdns_advertiser = MDNSAdvertiser(self.log)
        self.loop = None
        self.tcp_task = None
        self.tray = TrayManager(self)
        self.perf_monitor = PerformanceMonitor()

        self.refresh_ip_list()
        self.update_qr_code()
        self._apply_always_on_top()
        self.perf_monitor.start()
        self._update_performance_label()
        self._init_keyboard_shortcuts()
        self._load_connection_history()

    # ---------- Style & Helpers ----------
    def _setup_styles(self):
        style = ttk.Style(self.root)
        try: style.theme_use("clam")
        except: pass
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

    def _adjust_color(self, hex_color, offset):
        hex_color = hex_color.lstrip("#")
        r = max(0, min(255, int(hex_color[0:2],16)+offset))
        g = max(0, min(255, int(hex_color[2:4],16)+offset))
        b = max(0, min(255, int(hex_color[4:6],16)+offset))
        return f"#{r:02x}{g:02x}{b:02x}"

    def _card(self, parent, title, subtitle=None):
        frame = tk.Frame(parent, bg=self.card_bg, highlightthickness=1, highlightbackground=self.border_color)
        frame.columnconfigure(0, weight=1)
        header = tk.Frame(frame, bg=self.card_bg)
        header.grid(row=0, column=0, sticky="ew", padx=18, pady=(16,0))
        ttk.Label(header, text=title, style="Section.TLabel").pack(anchor="w")
        if subtitle:
            ttk.Label(header, text=subtitle, style="Hint.TLabel").pack(anchor="w", pady=(4,0))
        body = tk.Frame(frame, bg=self.card_bg)
        body.grid(row=1, column=0, sticky="nsew", padx=18, pady=16)
        return frame, body

    # ---------- UI Construction ----------
    def setup_ui(self):
        shell = tk.Frame(self.root, bg=self.bg_color)
        shell.pack(fill=tk.BOTH, expand=True)

        # Menu bar
        menubar = tk.Menu(self.root)
        filemenu = tk.Menu(menubar, tearoff=0)
        filemenu.add_command(label="Start Server   Ctrl+S", command=self.start_servers)
        filemenu.add_command(label="Stop Server    Ctrl+T", command=self.stop_servers, state="disabled")
        filemenu.add_separator()
        filemenu.add_command(label="Exit           Ctrl+Q", command=self._quit_app)
        menubar.add_cascade(label="File", menu=filemenu)

        viewmenu = tk.Menu(menubar, tearoff=0)
        viewmenu.add_command(label="Refresh IP List Ctrl+R", command=self.refresh_ip_list)
        viewmenu.add_command(label="Clear Logs", command=self._clear_logs)
        viewmenu.add_checkbutton(label="Always on Top", variable=tk.BooleanVar(value=CONFIG.get("always_on_top", False)), command=self._toggle_always_on_top)
        menubar.add_cascade(label="View", menu=viewmenu)

        helpmenu = tk.Menu(menubar, tearoff=0)
        helpmenu.add_command(label="Connection Wizard", command=self._show_connection_wizard)
        helpmenu.add_command(label="About", command=self._show_about)
        menubar.add_cascade(label="Help", menu=helpmenu)
        self.root.config(menu=menubar)

        # Header
        header = tk.Frame(shell, bg=self.surface, highlightthickness=1, highlightbackground=self.border_color)
        header.pack(fill=tk.X, padx=18, pady=(18,12))
        header_inner = tk.Frame(header, bg=self.surface)
        header_inner.pack(fill=tk.X, padx=22, pady=20)
        left_header = tk.Frame(header_inner, bg=self.surface)
        left_header.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        ttk.Label(left_header, text="Air Mouse Server", style="Title.TLabel").pack(anchor="w")
        ttk.Label(left_header, text="Desktop endpoint, discovery responder, and live motion control dashboard", style="Subtitle.TLabel").pack(anchor="w", pady=(6,0))
        self.status_pill = tk.Label(header_inner, text="Server stopped", bg=self.danger, fg="white", font=("Segoe UI Semibold", 10), padx=12, pady=5)
        self.status_pill.pack(side=tk.RIGHT, anchor="e")

        # Main content area
        content = tk.Frame(shell, bg=self.bg_color)
        content.pack(fill=tk.BOTH, expand=True, padx=18, pady=(0,18))
        content.columnconfigure(0, weight=1)
        content.columnconfigure(1, weight=2)
        content.rowconfigure(0, weight=1)

        # Left column
        left_col = tk.Frame(content, bg=self.bg_color)
        left_col.grid(row=0, column=0, sticky="nsew", padx=(0,12))
        left_col.columnconfigure(0, weight=1)

        # Right column
        right_col = tk.Frame(content, bg=self.bg_color)
        right_col.grid(row=0, column=1, sticky="nsew")
        right_col.rowconfigure(1, weight=1)
        right_col.columnconfigure(0, weight=1)

        # ---- LEFT COLUMN CARDS ----
        self.build_summary_card(left_col)
        self.build_ip_card(left_col)
        self.build_qr_card(left_col)
        self.build_controls_card(left_col)
        self.build_sensitivity_card(left_col)
        self.build_connections_card(left_col)

        # ---- RIGHT COLUMN ----
        self.build_log_card(right_col)
        self.build_diagnostics_card(right_col)

        # Status bar at bottom
        status_bar = tk.Frame(self.root, bg=self.surface, height=28)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)
        self.status_left = ttk.Label(status_bar, text="Server stopped", style="Status.TLabel")
        self.status_left.pack(side=tk.LEFT, padx=(12,6))
        self.status_middle = ttk.Label(status_bar, text="No IP selected", style="Hint.TLabel")
        self.status_middle.pack(side=tk.LEFT, padx=(6,12))
        self.status_right = ttk.Label(status_bar, text="Connections: 0", style="Hint.TLabel")
        self.status_right.pack(side=tk.RIGHT, padx=(6,12))
        self.perf_label = ttk.Label(status_bar, text="CPU: ---%  MEM: ---%", style="Hint.TLabel")
        self.perf_label.pack(side=tk.RIGHT, padx=(6,12))

        # Initialize log
        self.log_area.configure(state=tk.NORMAL)
        self.log_area.insert(tk.END, "Air Mouse server is ready. Select an endpoint, then start the server.\n")
        self.log_area.see(tk.END)

    def build_summary_card(self, parent):
        card, body = self._card(parent, "Runtime Summary", "Live server status and counters")
        card.grid(row=0, column=0, sticky="ew", pady=(0,12))
        top = tk.Frame(body, bg=self.card_bg)
        top.pack(fill=tk.X)
        top.columnconfigure((0,1,2), weight=1)
        self.status_label = ttk.Label(top, text="Server stopped", style="Metric.TLabel")
        self.status_label.grid(row=0, column=0, sticky="w")
        self.conn_label = ttk.Label(top, text="Connections: 0", style="Metric.TLabel")
        self.conn_label.grid(row=0, column=1, sticky="w")
        self.stats_label = ttk.Label(top, text="Clicks: 0  •  Dbl: 0  •  Right: 0  •  Scroll: 0", style="Metric.TLabel")
        self.stats_label.grid(row=0, column=2, sticky="w")
        self.current_ip_label = ttk.Label(body, text="Selected endpoint will appear here", style="Hint.TLabel")
        self.current_ip_label.pack(anchor="w", pady=(10,0))

    def build_ip_card(self, parent):
        card, body = self._card(parent, "Network Endpoint", "Choose the IP address to advertise to the Android app")
        card.grid(row=1, column=0, sticky="ew", pady=(0,12))
        body.columnconfigure(0, weight=1)
        selection_row = tk.Frame(body, bg=self.card_bg)
        selection_row.pack(fill=tk.X)
        ttk.Label(selection_row, text="Interface", style="Hint.TLabel").pack(anchor="w")
        self.ip_var = tk.StringVar()
        self.manual_ip_var = tk.StringVar()
        self.manual_ip_var.trace_add("write", self.on_manual_ip_changed)
        self.ip_combo = ttk.Combobox(selection_row, textvariable=self.ip_var, state="readonly", width=34, style="Dark.TCombobox")
        self.ip_combo.pack(fill=tk.X, pady=(6,8))
        self.ip_combo.bind("<<ComboboxSelected>>", self.on_ip_selected)
        self.ip_combo.bind("<Button-1>", self._copy_ip_to_clipboard)  # auto-copy on click
        action_row = tk.Frame(body, bg=self.card_bg)
        action_row.pack(fill=tk.X, pady=(6,0))
        self.refresh_btn = ttk.Button(action_row, text="Refresh", command=self.refresh_ip_list, style="Accent.TButton")
        self.refresh_btn.pack(side=tk.LEFT)
        self.copy_btn = ttk.Button(action_row, text="Copy Endpoint", command=self.copy_ip, style="Accent.TButton")
        self.copy_btn.pack(side=tk.LEFT, padx=(10,0))
        manual_row = tk.Frame(body, bg=self.card_bg)
        manual_row.pack(fill=tk.X, pady=(12,0))
        self.manual_check = tk.IntVar()
        self.manual_cb = ttk.Checkbutton(manual_row, text="Use manual IP", variable=self.manual_check, command=self.toggle_manual_ip)
        self.manual_cb.pack(anchor="w")
        self.manual_entry = tk.Entry(manual_row, textvariable=self.manual_ip_var, bg=self.surface_alt, fg=self.fg_color, insertbackground=self.fg_color, relief=tk.FLAT, disabledbackground=self.surface_alt, disabledforeground=self.muted_color)
        self.manual_entry.pack(fill=tk.X, pady=(8,0))
        # mDNS row
        mdns_row = tk.Frame(body, bg=self.card_bg)
        mdns_row.pack(fill=tk.X, pady=(10,0))
        ttk.Label(mdns_row, text="mDNS hostname:", style="Hint.TLabel").pack(side=tk.LEFT)
        self.mdns_label = ttk.Label(mdns_row, text=f"{CONFIG.get('mDNS_name', 'airmouse')}.local", style="Metric.TLabel")
        self.mdns_label.pack(side=tk.LEFT, padx=(6,0))
        ttk.Button(mdns_row, text="Copy", command=self._copy_mdns, style="Accent.TButton").pack(side=tk.RIGHT)

    def build_qr_card(self, parent):
        card, body = self._card(parent, "Pairing QR", "Scan this endpoint from the Android app")
        card.grid(row=2, column=0, sticky="ew", pady=(0,12))
        qr_panel = tk.Frame(body, bg=self.card_bg)
        qr_panel.pack(fill=tk.BOTH, expand=True)
        self.qr_label = tk.Label(qr_panel, bg=self.card_bg)
        self.qr_label.pack(pady=(6,10))
        self.qr_text = ttk.Label(qr_panel, text="", style="Hint.TLabel", justify=tk.CENTER)
        self.qr_text.pack(fill=tk.X)
        self.save_qr_btn = ttk.Button(qr_panel, text="Save QR", command=self.save_qr_image, style="Accent.TButton")
        self.save_qr_btn.pack(pady=(8,0))

    def build_controls_card(self, parent):
        card, body = self._card(parent, "Server Controls", "Start or stop the TCP and UDP services")
        card.grid(row=3, column=0, sticky="ew", pady=(0,12))
        button_row = tk.Frame(body, bg=self.card_bg)
        button_row.pack(fill=tk.X)
        self.start_btn = ttk.Button(button_row, text="Start Server", command=self.start_servers, style="Primary.TButton")
        self.start_btn.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.stop_btn = ttk.Button(button_row, text="Stop Server", command=self.stop_servers, style="Danger.TButton")
        self.stop_btn.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(12,0))

    def build_sensitivity_card(self, parent):
        card, body = self._card(parent, "Cursor Sensitivity", "Tune pointer speed for smooth cursor control")
        card.grid(row=4, column=0, sticky="ew")
        sens_top = tk.Frame(body, bg=self.card_bg)
        sens_top.pack(fill=tk.X)
        ttk.Label(sens_top, text="Sensitivity", style="Hint.TLabel").pack(side=tk.LEFT)
        self.sens_value = ttk.Label(sens_top, text=f"{CONFIG.get('sensitivity', 0.5):.2f}", style="Metric.TLabel")
        self.sens_value.pack(side=tk.RIGHT)
        self.sens_slider = ttk.Scale(body, from_=0.2, to=2.0, orient=tk.HORIZONTAL, style="Dark.Horizontal.TScale")
        self.sens_slider.set(CONFIG.get("sensitivity", 0.5))
        self.sens_slider.pack(fill=tk.X, pady=(10,4))
        self.sens_slider.bind("<ButtonRelease-1>", self.update_sensitivity)

    def build_connections_card(self, parent):
        card, body = self._card(parent, "Connected Clients", "Active client addresses")
        card.grid(row=5, column=0, sticky="ew", pady=(0,12))
        list_frame = tk.Frame(body, bg=self.card_bg)
        list_frame.pack(fill=tk.BOTH, expand=True)
        scrollbar = tk.Scrollbar(list_frame, orient=tk.VERTICAL, bg=self.surface_alt, troughcolor=self.card_bg)
        self.conn_listbox = tk.Listbox(list_frame, bg=self.card_bg, fg=self.fg_color, highlightthickness=0, bd=0,
                                       activestyle='none', selectbackground=self.surface_alt,
                                       yscrollcommand=scrollbar.set)
        scrollbar.config(command=self.conn_listbox.yview)
        self.conn_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        btn_row = tk.Frame(body, bg=self.card_bg)
        btn_row.pack(fill=tk.X, pady=(8,0))
        self.disconnect_btn = ttk.Button(btn_row, text="Disconnect Selected", command=self.disconnect_selected, style="Danger.TButton")
        self.disconnect_btn.pack(side=tk.LEFT)
        self.disconnect_btn['state'] = tk.DISABLED

    def build_log_card(self, parent):
        card, body = self._card(parent, "Live Log", "Connections, gestures, discovery responses, and errors")
        card.grid(row=0, column=0, sticky="nsew")
        # Configure body for grid
        body.rowconfigure(0, weight=0)  # toolbar row
        body.rowconfigure(1, weight=1)  # log area row
        body.columnconfigure(0, weight=1)

        # Log toolbar: search, filters, export (using grid)
        toolbar = tk.Frame(body, bg=self.card_bg)
        toolbar.grid(row=0, column=0, sticky="ew", pady=(0, 6))
        toolbar.columnconfigure(1, weight=1)  # search entry expands

        ttk.Label(toolbar, text="Filter:", style="Hint.TLabel").grid(row=0, column=0, padx=(0, 4))
        self.log_filter_var = tk.StringVar(value="")
        self.log_search_entry = tk.Entry(toolbar, textvariable=self.log_filter_var, bg=self.surface_alt,
                                        fg=self.fg_color, insertbackground=self.fg_color, relief=tk.FLAT)
        self.log_search_entry.grid(row=0, column=1, sticky="ew", padx=4)
        self.log_search_entry.bind("<KeyRelease>", self._apply_log_filter)

        self.log_info_var = tk.BooleanVar(value=True)
        self.log_warn_var = tk.BooleanVar(value=True)
        self.log_error_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(toolbar, text="Info", variable=self.log_info_var,
                        command=self._apply_log_filter).grid(row=0, column=2, padx=2)
        ttk.Checkbutton(toolbar, text="Warn", variable=self.log_warn_var,
                        command=self._apply_log_filter).grid(row=0, column=3, padx=2)
        ttk.Checkbutton(toolbar, text="Error", variable=self.log_error_var,
                        command=self._apply_log_filter).grid(row=0, column=4, padx=2)

        ttk.Button(toolbar, text="Export Log", command=self._export_log,
                style="Accent.TButton").grid(row=0, column=5, padx=2)

        # Log text area (using grid)
        self.log_area = scrolledtext.ScrolledText(
            body, height=18, bg=self.log_bg, fg=self.fg_color,
            insertbackground=self.fg_color, font=("SF Mono", 10),
            relief=tk.FLAT, wrap=tk.WORD, padx=12, pady=12
        )
        self.log_area.grid(row=1, column=0, sticky="nsew")
        self.log_lines = []

    def build_diagnostics_card(self, parent):
        card, body = self._card(parent, "Server Diagnostics", "Quick status and maintenance actions")
        card.grid(row=1, column=0, sticky="ew", pady=(12,0))
        top = tk.Frame(body, bg=self.card_bg)
        top.pack(fill=tk.X)
        self.clear_logs_btn = ttk.Button(top, text="Clear Logs", command=self._clear_logs, style="Accent.TButton")
        self.clear_logs_btn.pack(side=tk.LEFT)
        self.qr_hint_label = ttk.Label(top, text="Ready for pairing", style="Hint.TLabel")
        self.qr_hint_label.pack(side=tk.RIGHT)
        live_summary = tk.Frame(body, bg=self.card_bg)
        live_summary.pack(fill=tk.X, pady=(12,0))
        ttk.Label(live_summary, text="Connection Options", style="Section.TLabel").pack(anchor="w")
        # Future transports: Bluetooth, USB
        btn_frame = tk.Frame(live_summary, bg=self.card_bg)
        btn_frame.pack(fill=tk.X, pady=(4,0))
        ttk.Button(btn_frame, text="Wi-Fi (TCP)", command=lambda: None, state="disabled").pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_frame, text="Bluetooth (coming soon)", command=lambda: messagebox.showinfo("Bluetooth", "Bluetooth support is planned for a future update.")).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_frame, text="USB (ADB reverse)", command=lambda: messagebox.showinfo("USB", "Use 'adb reverse tcp:8080 tcp:8080' then connect to 127.0.0.1:8080 on phone.")).pack(side=tk.LEFT, padx=2)

    # ---------- Helper methods for UI actions ----------
    def log(self, msg: str, level: str = "info"):
        """Log message with level: info, warning, error"""
        prefix = {"info": "ℹ️", "warning": "⚠️", "error": "❌"}.get(level, "ℹ️")
        full_msg = f"{prefix} {msg}"
        self.log_lines.append((level, full_msg))
        self._render_filtered_log()

    def _apply_log_filter(self, event=None):
        self._render_filtered_log()

    def _render_filtered_log(self):
        self.log_area.configure(state=tk.NORMAL)
        self.log_area.delete("1.0", tk.END)
        keyword = self.log_filter_var.get().lower()
        show_info = self.log_info_var.get()
        show_warn = self.log_warn_var.get()
        show_error = self.log_error_var.get()
        for level, line in self.log_lines:
            if keyword and keyword not in line.lower():
                continue
            if level == "info" and not show_info:
                continue
            if level == "warning" and not show_warn:
                continue
            if level == "error" and not show_error:
                continue
            self.log_area.insert(tk.END, line + "\n")
        self.log_area.see(tk.END)
        self.log_area.configure(state=tk.DISABLED)

    def _clear_logs(self):
        self.log_lines.clear()
        self._render_filtered_log()
        self.log("Logs cleared", "info")

    def _export_log(self):
        filename = filedialog.asksaveasfilename(defaultextension=".log", filetypes=[("Log files", "*.log"), ("Text files", "*.txt")])
        if not filename:
            return
        with open(filename, "w", encoding="utf-8") as f:
            for _, line in self.log_lines:
                f.write(line + "\n")
        messagebox.showinfo("Export", f"Log saved to {filename}")

    def save_qr_image(self):
        if not self.qr_manager.last_pil_image:
            messagebox.showwarning("Save QR", "No QR image available.")
            return
        filename = filedialog.asksaveasfilename(defaultextension='.png', filetypes=[('PNG Image', '*.png')], title='Save QR code')
        if filename:
            try:
                self.qr_manager.last_pil_image.save(filename)
                self.log(f"💾 Saved QR to {filename}")
                messagebox.showinfo("Save QR", f"Saved QR code to {filename}")
            except Exception as e:
                messagebox.showerror("Save QR", f"Failed: {e}")

    def _show_about(self):
        messagebox.showinfo("About Air Mouse", "Air Mouse Server\n\nProfessional desktop companion for the Air Mouse Android app.\nUniversity of Tehran — Embedded Systems")

    def _show_connection_wizard(self):
        wizard_text = (
            "=== Connection Wizard ===\n\n"
            "1. Ensure your PC and Android phone are on the same Wi-Fi network.\n"
            "2. Select the correct network interface (IP) from the dropdown, or scan the QR code.\n"
            "3. Click 'Start Server'.\n"
            "4. On the Android app, either:\n"
            "   - Scan the QR code, or\n"
            "   - Enter the IP:Port manually, or\n"
            "   - Use mDNS: {}.local:{}".format(CONFIG.get("mDNS_name", "airmouse"), CONFIG.get("port", 8080))
        )
        messagebox.showinfo("Connection Wizard", wizard_text)

    def get_all_ips(self) -> List[str]:
        ips = []
        try:
            for iface in netifaces.interfaces():
                addrs = netifaces.ifaddresses(iface)
                for addr in addrs.get(netifaces.AF_INET, []):
                    ip = addr.get('addr')
                    if ip and not ip.startswith('127.'):
                        ips.append(f"{ip} ({iface})")
        except Exception as e:
            self.log(f"IP scan error: {e}", "error")
        return sorted(set(ips), key=lambda x: x.split()[0])

    def refresh_ip_list(self):
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
            self.current_ip_label.config(text=f"Current IP: {manual_ip}:{CONFIG.get('port', 8080)}")
            self.update_qr_code()
            return
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
        if self.manual_check.get():
            manual_ip = self.manual_ip_var.get().strip()
            if manual_ip:
                return manual_ip
        sel = self.ip_var.get()
        return sel.split()[0] if sel else "127.0.0.1"

    def on_ip_selected(self, event=None):
        if self.manual_check.get():
            return
        sel = self.get_selected_ip()
        CONFIG["selected_ip"] = sel
        CONFIG["manual_ip_enabled"] = False
        save_config(CONFIG)
        self.current_ip_label.config(text=f"Current IP: {sel}:{CONFIG.get('port', 8080)}")
        self.update_qr_code()
        # Auto-copy to clipboard for convenience
        self._copy_ip_to_clipboard()

    def _copy_ip_to_clipboard(self, event=None):
        ip = self.get_selected_ip()
        ip_port = f"airmouse://{ip}:{CONFIG.get('port', 8080)}"
        self.root.clipboard_clear()
        self.root.clipboard_append(ip_port)
        self.log(f"📋 Copied to clipboard: {ip_port}")

    def toggle_manual_ip(self):
        if self.manual_check.get():
            self.manual_entry.config(state=tk.NORMAL)
            self.ip_combo.config(state=tk.DISABLED)
            self.manual_ip_var.set(self.get_selected_ip())
            CONFIG["manual_ip_enabled"] = True
            CONFIG["manual_ip_value"] = self.manual_ip_var.get().strip()
            save_config(CONFIG)
        else:
            self.manual_entry.config(state=tk.DISABLED)
            self.ip_combo.config(state="readonly")
            CONFIG["manual_ip_enabled"] = False
            save_config(CONFIG)
            self.on_ip_selected()

    def on_manual_ip_changed(self, *_):
        if not self.manual_check.get():
            return
        manual_ip = self.manual_ip_var.get().strip()
        if not manual_ip:
            return
        CONFIG["manual_ip_enabled"] = True
        CONFIG["manual_ip_value"] = manual_ip
        save_config(CONFIG)
        self.current_ip_label.config(text=f"Current IP: {manual_ip}:{CONFIG.get('port', 8080)}")
        self.update_qr_code()

    def copy_ip(self):
        self._copy_ip_to_clipboard()

    def _copy_mdns(self):
        self.root.clipboard_clear()
        self.root.clipboard_append(f"{CONFIG.get('mDNS_name', 'airmouse')}.local")
        self.log(f"📋 Copied mDNS hostname to clipboard")

    def update_qr_code(self):
        ip = self.get_selected_ip()
        if not ip:
            self.qr_text.config(text="No IP selected")
            return
        img = self.qr_manager.generate(ip, CONFIG.get("port", 8080))
        self.qr_label.config(image=img)
        self.qr_text.config(text=f"airmouse://{ip}:{CONFIG.get('port', 8080)}")

    def update_stats_display(self, stats: Dict):
        self.stats_label.config(text=f"Clicks: {stats['clicks']}  •  Dbl: {stats['double_clicks']}  •  Right: {stats['right_clicks']}  •  Scroll: {stats['scrolls']}")
        self.conn_label.config(text=f"Connections: {len(self.tcp_server.active_connections)}")
        self.status_right.config(text=f"Connections: {len(self.tcp_server.active_connections)}")

    def update_sensitivity(self, event=None):
        CONFIG["sensitivity"] = self.sens_slider.get()
        self.sens_value.config(text=f"{CONFIG['sensitivity']:.2f}")
        self.tcp_server.mouse.sensitivity = CONFIG["sensitivity"]
        save_config(CONFIG)
        self.log(f"⚙️ Sensitivity changed to {CONFIG['sensitivity']:.2f}")
        self.status_middle.config(text=f"Sensitivity: {CONFIG['sensitivity']:.2f}")

    def _update_connection_list(self, addresses: list):
        def _update():
            self.conn_listbox.delete(0, tk.END)
            for addr_str in addresses:
                self.conn_listbox.insert(tk.END, addr_str)
            self.disconnect_btn['state'] = tk.NORMAL if self.conn_listbox.size() > 0 else tk.DISABLED
        self.root.after(0, _update)


    
    def disconnect_selected(self):
        selection = self.conn_listbox.curselection()
        if not selection:
            messagebox.showinfo("Disconnect", "No client selected.")
            return
        addr_str = self.conn_listbox.get(selection[0])
        # Extract IP:Port from the string (remove extra details)
        ip_port = addr_str.split(' ')[0]
        try:
            ip, port_str = ip_port.rsplit(":", 1)
            port = int(port_str)
            addr = (ip, port)
            writer = self.tcp_server.active_connections.get(addr, {}).get('writer')
            if writer:
                if self.loop and self.loop.is_running():
                    asyncio.run_coroutine_threadsafe(self._close_writer(writer), self.loop)
                else:
                    writer.close()
                self.log(f"🔌 Requesting disconnect of {ip_port}")
            else:
                self.log(f"⚠️ No active writer for {ip_port}", "warning")
        except Exception as e:
            self.log(f"❌ Error disconnecting {addr_str}: {e}", "error")

    async def _close_writer(self, writer):
        try:
            writer.close()
            await writer.wait_closed()
        except Exception:
            pass

    def start_servers(self):
        self.start_btn.config(state=tk.DISABLED)
        self.stop_btn.config(state=tk.NORMAL)
        self.status_label.config(text="Server running")
        self.status_pill.config(text="Server running", bg=self.success)

        def run_loop():
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            try:
                ip = self.get_selected_ip()
                self.mdns_advertiser.start(ip, CONFIG.get("port", 8080))
                self.udp_server.start()
                self.loop.run_until_complete(self.tcp_server.start())
            except OSError as exc:
                self.log(f"Could not start server: {exc}", "error")
                self.root.after(0, lambda: self.status_label.config(text="Server error"))
                self.root.after(0, lambda: self.status_pill.config(text="Server error", bg=self.danger))
                self.root.after(0, lambda: self.start_btn.config(state=tk.NORMAL))
                self.root.after(0, lambda: self.stop_btn.config(state=tk.DISABLED))
            finally:
                self.mdns_advertiser.stop()
                self.udp_server.stop()

        self.tcp_task = threading.Thread(target=run_loop, daemon=True)
        self.tcp_task.start()
        self.log("🚀 Servers started (TCP + UDP + mDNS)")
        self.tray.update_status(True)
        self.status_left.config(text="Server running")
        # Notify sound (simple bell)
        self.root.bell()

    def stop_servers(self):
        if self.loop:
            asyncio.run_coroutine_threadsafe(self.tcp_server.stop(), self.loop)
        self.mdns_advertiser.stop()
        self.udp_server.stop()
        self.start_btn.config(state=tk.NORMAL)
        self.stop_btn.config(state=tk.DISABLED)
        self.status_label.config(text="Server stopped")
        self.status_pill.config(text="Server stopped", bg=self.danger)
        self.log("🛑 Servers stopped")
        self.tray.update_status(False)
        self.status_left.config(text="Server stopped")
        self.root.bell()

    def _apply_always_on_top(self):
        self.root.attributes("-topmost", CONFIG.get("always_on_top", False))

    def _toggle_always_on_top(self):
        CONFIG["always_on_top"] = not CONFIG.get("always_on_top", False)
        save_config(CONFIG)
        self._apply_always_on_top()

    def _update_performance_label(self):
        cpu = self.perf_monitor.cpu_usage
        mem = self.perf_monitor.memory_usage
        self.perf_label.config(text=f"CPU: {cpu:.0f}%  MEM: {mem:.0f}%")
        self.root.after(2000, self._update_performance_label)

    def _init_keyboard_shortcuts(self):
        self.root.bind_all('<Control-s>', lambda e: self.start_servers())
        self.root.bind_all('<Control-t>', lambda e: self.stop_servers())
        self.root.bind_all('<Control-r>', lambda e: self.refresh_ip_list())
        self.root.bind_all('<Control-q>', lambda e: self._quit_app())

    def _load_connection_history(self):
        # Could be extended to remember last connected clients
        pass

    def _on_close(self):
        self.root.withdraw()  # minimize to tray if tray is running
        if not self.tray.running:
            self._quit_app()

    def _quit_app(self):
        self.stop_servers()
        self.perf_monitor.stop()
        self.tray.stop()
        self.root.quit()

    def run(self):
        self.root.protocol('WM_DELETE_WINDOW', self._on_close)
        self.tray.start()
        self.root.mainloop()