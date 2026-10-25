import asyncio
import http.server
import io
import logging
from pathlib import Path
import socket
import threading
import urllib.request
import webbrowser
from typing import Optional

import customtkinter as ctk
from PIL import Image
import qrcode

from core.mouse_controller import MouseController
from network.tcp_server import TCPProtocolServer
from network.udp_discovery import UDPDiscoveryServer
from network.ws_server import WebSocketProtocolServer
from security.auth import AuthManager

logger = logging.getLogger("AirMouseGUI")
ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

VERSION = "1.0.0"
PROTOCOL_VERSION = "Protocol V2 (Canonical)"
GITHUB_REPO = "tahamajs/AirMouse"


class StaticWebHandler(http.server.SimpleHTTPRequestHandler):
    """Zero-dependency HTTP request handler for the AirMouse web trackpad."""

    def __init__(self, *args, directory=None, **kwargs):
        super().__init__(*args, directory=directory, **kwargs)

    def log_message(self, format, *args):
        # Suppress noisy HTTP asset logs in stdout
        pass


class AirMouseApp(ctk.CTk):
    """
    Modern Dark-Themed Desktop Dashboard for AirMouse Pro (v1.0.0).
    Features live telemetry, QR code instant mobile pairing, embedded HTTP web server,
    one-click browser launcher, sensitivity controls, and real-time logs.
    """

    def __init__(self):
        super().__init__()
        self.title("AirMouse Pro v1.0.0 — Desktop Dashboard")
        self.geometry("980x680")
        self.minsize(850, 580)

        # Core subsystems
        self.auth_mgr = AuthManager()
        self.mouse = MouseController(sensitivity=1.0, smoothing=True)
        self.loop: Optional[asyncio.AbstractEventLoop] = None
        self.server_thread: Optional[threading.Thread] = None

        # Ports configuration
        self.http_port = 8080
        self.ws_port = 8081
        self.tcp_port = 8084
        self.udp_port = 8083

        self.http_server: Optional[http.server.ThreadingHTTPServer] = None
        self.http_thread: Optional[threading.Thread] = None

        self.tcp_srv: Optional[TCPProtocolServer] = None
        self.ws_srv: Optional[WebSocketProtocolServer] = None
        self.udp_srv: Optional[UDPDiscoveryServer] = None
        self.running = False

        self._resolve_web_dir()
        self._build_ui()
        self._generate_qr()
        self._check_github_update_async()

    def _resolve_web_dir(self) -> Path:
        """Find the web-client directory across project layouts."""
        candidates = [
            Path.cwd() / "web-client",
            Path(__file__).resolve().parent.parent.parent / "web-client",
            Path(__file__).resolve().parent.parent / "airmouse" / "web_static",
        ]
        for p in candidates:
            if p.exists() and (p / "index.html").exists():
                self.web_dir = p
                return p
        self.web_dir = Path.cwd()
        return self.web_dir

    def _get_local_ip(self) -> str:
        """Detect the primary LAN IP address of this computer."""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    def _build_ui(self) -> None:
        self.grid_columnconfigure(1, weight=1)
        self.grid_rowconfigure(0, weight=1)

        # ======================================================================
        # LEFT SIDEBAR
        # ======================================================================
        self.sidebar = ctk.CTkFrame(self, width=260, corner_radius=0)
        self.sidebar.grid(row=0, column=0, sticky="nsew", padx=0, pady=0)
        self.sidebar.grid_rowconfigure(13, weight=1)

        self.logo_label = ctk.CTkLabel(
            self.sidebar, text="🖱️ AirMouse Pro", font=ctk.CTkFont(size=22, weight="bold")
        )
        self.logo_label.grid(row=0, column=0, padx=20, pady=(20, 5))

        self.version_badge = ctk.CTkLabel(
            self.sidebar, text=f"v{VERSION} • {PROTOCOL_VERSION}",
            font=ctk.CTkFont(size=11), text_color="#94a3b8"
        )
        self.version_badge.grid(row=1, column=0, padx=20, pady=(0, 10))

        self.status_badge = ctk.CTkLabel(
            self.sidebar, text="● STOPPED", text_color="#ef4444",
            font=ctk.CTkFont(size=14, weight="bold")
        )
        self.status_badge.grid(row=2, column=0, padx=20, pady=5)

        self.start_btn = ctk.CTkButton(
            self.sidebar, text="Start Server", command=self.toggle_server,
            fg_color="#10b981", hover_color="#059669", font=ctk.CTkFont(weight="bold")
        )
        self.start_btn.grid(row=3, column=0, padx=20, pady=12)

        # Open Web Client Button
        self.btn_open_browser = ctk.CTkButton(
            self.sidebar, text="🌐 Open Web Trackpad", command=self.open_browser,
            fg_color="#3b82f6", hover_color="#2563eb"
        )
        self.btn_open_browser.grid(row=4, column=0, padx=20, pady=6)

        # Network Info Card
        self.info_card = ctk.CTkFrame(self.sidebar, corner_radius=8, fg_color="#1e293b")
        self.info_card.grid(row=5, column=0, padx=15, pady=10, sticky="ew")

        self.lbl_ip = ctk.CTkLabel(
            self.info_card, text=f"LAN IP: {self._get_local_ip()}",
            font=ctk.CTkFont(size=12, weight="bold"), text_color="#38bdf8"
        )
        self.lbl_ip.pack(padx=10, pady=(8, 2), anchor="w")

        self.lbl_http = ctk.CTkLabel(
            self.info_card, text=f"HTTP Port: {self.http_port} (Main Page)",
            font=ctk.CTkFont(size=11), text_color="#94a3b8"
        )
        self.lbl_http.pack(padx=10, pady=2, anchor="w")

        self.lbl_ports = ctk.CTkLabel(
            self.info_card, text=f"WS: {self.ws_port} | TCP: {self.tcp_port} | UDP: {self.udp_port}",
            font=ctk.CTkFont(size=11), text_color="#94a3b8"
        )
        self.lbl_ports.pack(padx=10, pady=(2, 8), anchor="w")

        # Mouse Sensitivity Slider
        self.lbl_sens = ctk.CTkLabel(self.sidebar, text="Cursor Sensitivity: 1.0x", font=ctk.CTkFont(size=13))
        self.lbl_sens.grid(row=6, column=0, padx=20, pady=(15, 2))
        self.sens_slider = ctk.CTkSlider(
            self.sidebar, from_=0.2, to=3.0, number_of_steps=28, command=self._on_sens_change
        )
        self.sens_slider.set(1.0)
        self.sens_slider.grid(row=7, column=0, padx=20, pady=5)

        # 1€ Jitter Filter Switch
        self.smooth_switch = ctk.CTkSwitch(
            self.sidebar, text="1€ Jitter Smoothing", command=self._on_smooth_toggle
        )
        self.smooth_switch.select()
        self.smooth_switch.grid(row=8, column=0, padx=20, pady=10)

        # About Button
        self.btn_about = ctk.CTkButton(
            self.sidebar, text="ℹ️ About AirMouse", command=self.show_about,
            fg_color="#334155", hover_color="#475569"
        )
        self.btn_about.grid(row=9, column=0, padx=20, pady=(15, 10))

        # ======================================================================
        # RIGHT MAIN PANEL
        # ======================================================================
        self.main_panel = ctk.CTkFrame(self, corner_radius=12)
        self.main_panel.grid(row=0, column=1, sticky="nsew", padx=15, pady=15)
        self.main_panel.grid_columnconfigure(0, weight=1)
        self.main_panel.grid_rowconfigure(4, weight=1)

        # Update Banner (Hidden by default, shown if GitHub has newer release)
        self.update_banner = ctk.CTkFrame(self.main_panel, fg_color="#7c2d12", corner_radius=8)
        self.update_banner.grid_columnconfigure(0, weight=1)
        self.update_lbl = ctk.CTkLabel(
            self.update_banner, text="✨ Update Available!", font=ctk.CTkFont(size=12, weight="bold"),
            text_color="#fef08a"
        )
        self.update_lbl.grid(row=0, column=0, padx=12, pady=6, sticky="w")
        self.update_btn = ctk.CTkButton(
            self.update_banner, text="📥 Download", width=90, height=26,
            fg_color="#ea580c", hover_color="#c2410c", font=ctk.CTkFont(size=11, weight="bold")
        )
        self.update_btn.grid(row=0, column=1, padx=6, pady=6)
        self.update_close = ctk.CTkButton(
            self.update_banner, text="✕", width=26, height=26,
            fg_color="transparent", hover_color="#9a3412", command=self._dismiss_update_banner
        )
        self.update_close.grid(row=0, column=2, padx=(2, 8), pady=6)
        # Note: update_banner is not gridded until an update is found

        # Header with Status & Quick Actions
        self.top_bar = ctk.CTkFrame(self.main_panel, fg_color="transparent")
        self.top_bar.grid(row=1, column=0, sticky="ew", padx=20, pady=(15, 5))
        self.top_bar.grid_columnconfigure(1, weight=1)

        self.panel_title = ctk.CTkLabel(
            self.top_bar, text="📱 Instant Mobile Pairing", font=ctk.CTkFont(size=18, weight="bold")
        )
        self.panel_title.grid(row=0, column=0, sticky="w")

        self.btn_copy_link = ctk.CTkButton(
            self.top_bar, text="📋 Copy Mobile Link", width=140, command=self.copy_link,
            fg_color="#475569", hover_color="#64748b"
        )
        self.btn_copy_link.grid(row=0, column=2, padx=5)

        self.btn_clear_log = ctk.CTkButton(
            self.top_bar, text="🗑️ Clear Logs", width=110, command=self.clear_logs,
            fg_color="#475569", hover_color="#64748b"
        )
        self.btn_clear_log.grid(row=0, column=3, padx=5)

        # QR Code Card
        self.qr_card = ctk.CTkFrame(self.main_panel, fg_color="#0f172a", corner_radius=10)
        self.qr_card.grid(row=2, column=0, padx=20, pady=10, sticky="ew")

        self.qr_label = ctk.CTkLabel(self.qr_card, text="")
        self.qr_label.pack(pady=(12, 4))

        self.qr_url_label = ctk.CTkLabel(
            self.qr_card,
            text=f"http://{self._get_local_ip()}:{self.http_port}/?autoconnect=1",
            font=ctk.CTkFont(size=13, weight="bold"), text_color="#38bdf8"
        )
        self.qr_url_label.pack(pady=2)

        self.qr_sub = ctk.CTkLabel(
            self.qr_card,
            text="Point your iPhone or Android camera at the QR code to connect with zero taps!",
            font=ctk.CTkFont(size=12), text_color="#94a3b8"
        )
        self.qr_sub.pack(pady=(2, 12))

        # Logs Section Label
        self.logs_label = ctk.CTkLabel(
            self.main_panel, text="📡 Real-Time Protocol & Motion Activity:",
            font=ctk.CTkFont(size=13, weight="bold")
        )
        self.logs_label.grid(row=3, column=0, padx=20, pady=(10, 2), sticky="w")

        # Live scrolling log box
        self.log_box = ctk.CTkTextbox(
            self.main_panel, font=ctk.CTkFont(family="Courier", size=11),
            fg_color="#090d16", text_color="#e2e8f0"
        )
        self.log_box.grid(row=4, column=0, sticky="nsew", padx=20, pady=(2, 15))
        self.log_box.configure(state="disabled")

    def _check_github_update_async(self, manual: bool = False) -> None:
        """Asynchronously checks GitHub Releases API for new AirMouse releases."""
        def _worker():
            try:
                import json
                url = f"https://api.github.com/repos/{GITHUB_REPO}/releases/latest"
                req = urllib.request.Request(
                    url,
                    headers={
                        "User-Agent": "AirMouse-Desktop-Dashboard/1.0.0",
                        "Accept": "application/vnd.github.v3+json",
                    }
                )
                with urllib.request.urlopen(req, timeout=4.0) as resp:
                    if resp.status == 200:
                        data = json.loads(resp.read().decode("utf-8"))
                        remote_tag = data.get("tag_name", "").lstrip("v")
                        html_url = data.get("html_url", f"https://github.com/{GITHUB_REPO}/releases")
                        if self._is_newer_version(remote_tag, VERSION):
                            self.after(0, lambda: self._show_update_alert(remote_tag, html_url))
                        else:
                            msg = f"✅ GitHub Update Check: AirMouse Pro is up to date (v{VERSION})"
                            self.after(0, lambda: self.log(msg))
                            if manual:
                                self.after(0, lambda: self._show_toast("AirMouse is up to date!"))
            except Exception as e:
                msg = f"ℹ️ GitHub check offline / rate-limited ({type(e).__name__})"
                self.after(0, lambda: self.log(msg))

        threading.Thread(target=_worker, daemon=True).start()

    @staticmethod
    def _is_newer_version(remote: str, local: str) -> bool:
        """Compares semantic versions, returns True if remote is strictly newer."""
        try:
            r = [int(x) for x in remote.split("-")[0].split(".") if x.isdigit()]
            l = [int(x) for x in local.split("-")[0].split(".") if x.isdigit()]
            while len(r) < 3:
                r.append(0)
            while len(l) < 3:
                l.append(0)
            return r > l
        except Exception:
            return False

    def _show_update_alert(self, new_ver: str, release_url: str) -> None:
        self.log(f"🎉 New version v{new_ver} is available on GitHub! ({release_url})")
        self.version_badge.configure(text=f"v{VERSION} • Update v{new_ver} Available!", text_color="#f59e0b")
        if hasattr(self, "update_banner"):
            self.update_lbl.configure(text=f"✨ Update Available: AirMouse Pro v{new_ver}")
            self.update_btn.configure(command=lambda: webbrowser.open(release_url))
            self.update_banner.grid(row=0, column=0, sticky="ew", padx=20, pady=(10, 0))

    def _dismiss_update_banner(self) -> None:
        if hasattr(self, "update_banner"):
            self.update_banner.grid_forget()

    def _show_toast(self, text: str) -> None:
        self.log(f"🔔 {text}")
        self.mouse.sensitivity = val
        self.lbl_sens.configure(text=f"Cursor Sensitivity: {val:.1f}x")

    def _on_smooth_toggle(self) -> None:
        self.mouse.smoothing = bool(self.smooth_switch.get())

    def _generate_qr(self) -> None:
        ip = self._get_local_ip()
        url = f"http://{ip}:{self.http_port}/?autoconnect=1"

        qr = qrcode.QRCode(box_size=4, border=2)
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="white", back_color="#0f172a").convert("RGB")

        ctk_img = ctk.CTkImage(light_image=img, dark_image=img, size=(160, 160))
        self.qr_label.configure(image=ctk_img)
        self.qr_url_label.configure(text=url)

    def open_browser(self) -> None:
        url = f"http://localhost:{self.http_port}/?autoconnect=1"
        self.log(f"Opening main web page in browser: {url}")
        webbrowser.open(url)

    def copy_link(self) -> None:
        ip = self._get_local_ip()
        url = f"http://{ip}:{self.http_port}/?autoconnect=1"
        self.clipboard_clear()
        self.clipboard_append(url)
        self.btn_copy_link.configure(text="✅ Copied!")
        self.after(2000, lambda: self.btn_copy_link.configure(text="📋 Copy Mobile Link"))
        self.log(f"Copied mobile link to clipboard: {url}")

    def clear_logs(self) -> None:
        self.log_box.configure(state="normal")
        self.log_box.delete("1.0", "end")
        self.log_box.configure(state="disabled")

    def log(self, text: str) -> None:
        def _append():
            self.log_box.configure(state="normal")
            self.log_box.insert("end", text + "\n")
            self.log_box.see("end")
            self.log_box.configure(state="disabled")
        self.after(0, _append)

    def toggle_server(self) -> None:
        if self.running:
            self._stop_services()
        else:
            self._start_services()

    def _start_http_server(self) -> None:
        """Starts the static HTTP file server for the web client."""
        directory = str(self.web_dir)
        handler = lambda *args, **kwargs: StaticWebHandler(*args, directory=directory, **kwargs)

        for port_candidate in [8080, 8082, 8085, 9000]:
            try:
                self.http_server = http.server.ThreadingHTTPServer(("0.0.0.0", port_candidate), handler)
                self.http_port = port_candidate
                self.http_thread = threading.Thread(target=self.http_server.serve_forever, daemon=True)
                self.http_thread.start()
                self.lbl_http.configure(text=f"HTTP Port: {self.http_port} (Main Page)")
                self._generate_qr()
                self.log(f"🌐 HTTP Web Server active: http://{self._get_local_ip()}:{self.http_port}/")
                return
            except OSError:
                continue
        self.log("⚠️ Could not bind HTTP port. Static files may already be served by another process.")

    def _start_services(self) -> None:
        self.running = True
        self.status_badge.configure(text="● RUNNING", text_color="#10b981")
        self.start_btn.configure(text="Stop Server", fg_color="#ef4444", hover_color="#dc2626")

        # 1. Start HTTP Server for the Main Page on Mac
        self._start_http_server()

        # 2. Start TCP, WebSocket, and UDP Discovery Servers
        self.tcp_srv = TCPProtocolServer(
            host="0.0.0.0", port=self.tcp_port, enable_tls=True, auth_manager=self.auth_mgr,
            mouse_controller=self.mouse, on_log=self.log
        )
        self.ws_srv = WebSocketProtocolServer(
            host="0.0.0.0", port=self.ws_port, enable_tls=True, auth_manager=self.auth_mgr,
            mouse_controller=self.mouse, on_log=self.log
        )
        self.udp_srv = UDPDiscoveryServer(
            port=self.udp_port, tcp_port=self.tcp_port, ws_port=self.ws_port, tls_enabled=True,
            on_discovered=lambda name, ip: self.log(f"Discovered by {name} ({ip})")
        )
        self.udp_srv.start()

        def _run_async():
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            self.loop.create_task(self.tcp_srv.start())
            self.loop.create_task(self.ws_srv.start())
            self.loop.run_forever()

        self.server_thread = threading.Thread(target=_run_async, daemon=True)
        self.server_thread.start()
        self.log(f"⚡ Core protocols online (WS: {self.ws_port}, TCP: {self.tcp_port}, UDP: {self.udp_port})")

    def _stop_services(self) -> None:
        self.running = False
        self.status_badge.configure(text="● STOPPED", text_color="#ef4444")
        self.start_btn.configure(text="Start Server", fg_color="#10b981", hover_color="#059669")

        if self.http_server:
            try:
                self.http_server.shutdown()
                self.http_server.server_close()
            except Exception:
                pass
            self.http_server = None

        if self.udp_srv:
            self.udp_srv.stop()

        if self.loop and self.loop.is_running():
            future = asyncio.run_coroutine_threadsafe(
                asyncio.gather(self.tcp_srv.stop(), self.ws_srv.stop()), self.loop
            )
            try:
                future.result(timeout=2.0)
            except Exception:
                pass
            self.loop.call_soon_threadsafe(self.loop.stop())

        self.log("Server shutdown completed.")

    def show_about(self) -> None:
        """Displays the polished, canonical About Dialog Window."""
        win = ctk.CTkToplevel(self)
        win.title("About AirMouse Pro")
        win.geometry("520x440")
        win.resizable(False, False)
        win.grab_set()

        frame = ctk.CTkFrame(win, corner_radius=12, fg_color="#0f172a")
        frame.pack(fill="both", expand=True, padx=15, pady=15)

        lbl_title = ctk.CTkLabel(
            frame, text="🖱️ AirMouse Pro", font=ctk.CTkFont(size=22, weight="bold"),
            text_color="#f8fafc"
        )
        lbl_title.pack(pady=(20, 2))

        lbl_ver = ctk.CTkLabel(
            frame, text=f"Version {VERSION} (Release v1.0.0-final)",
            font=ctk.CTkFont(size=14, weight="bold"), text_color="#38bdf8"
        )
        lbl_ver.pack(pady=2)

        lbl_proto = ctk.CTkLabel(
            frame, text=f"Wire Engine: {PROTOCOL_VERSION}",
            font=ctk.CTkFont(size=12), text_color="#94a3b8"
        )
        lbl_proto.pack(pady=(0, 15))

        # Description Card
        card = ctk.CTkFrame(frame, fg_color="#1e293b", corner_radius=8)
        card.pack(fill="x", padx=20, pady=5)

        desc = (
            "AirMouse Pro converts any smartphone or tablet into a high-precision,\n"
            "low-latency spatial air mouse, presentation remote, and multi-touch trackpad.\n\n"
            "Key Engineering Capabilities:\n"
            "• Zero Velocity Update (ZUPT) drift cancellation\n"
            "• 1€ Speed-adaptive jitter low-pass filter\n"
            "• Kalman 2D Forward Trajectory Extrapolator\n"
            "• Madgwick AHRS 9-DOF MARG Sensor Fusion\n"
            "• Canonical Protocol V2 with TLS 1.3 encryption\n"
            "• Zero-install Safari & Chrome Web Trackpad with Audio Haptics"
        )
        lbl_desc = ctk.CTkLabel(
            card, text=desc, font=ctk.CTkFont(size=11), justify="left",
            text_color="#cbd5e1"
        )
        lbl_desc.pack(padx=15, pady=12, anchor="w")

        # Action Buttons
        btn_box = ctk.CTkFrame(frame, fg_color="transparent")
        btn_box.pack(pady=12)

        btn_github = ctk.CTkButton(
            btn_box, text="🔗 GitHub Repo", width=140,
            command=lambda: webbrowser.open(f"https://github.com/{GITHUB_REPO}"),
            fg_color="#3b82f6", hover_color="#2563eb"
        )
        btn_github.pack(side="left", padx=6)

        btn_check = ctk.CTkButton(
            btn_box, text="🔄 Check Updates", width=140,
            command=lambda: self._check_github_update_async(manual=True),
            fg_color="#10b981", hover_color="#059669"
        )
        btn_check.pack(side="left", padx=6)

        lbl_copy = ctk.CTkLabel(
            frame, text="© 2026 AirMouse Team • University of Tehran • MIT License",
            font=ctk.CTkFont(size=10), text_color="#64748b"
        )
        lbl_copy.pack(side="bottom", pady=10)


def main():
    app = AirMouseApp()
    app.mainloop()


if __name__ == "__main__":
    main()
