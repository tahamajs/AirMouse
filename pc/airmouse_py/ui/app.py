import asyncio
import io
import logging
import socket
import threading
from typing import Optional
import customtkinter as ctk
import qrcode
from PIL import Image

from core.mouse_controller import MouseController
from network.tcp_server import TCPProtocolServer
from network.udp_discovery import UDPDiscoveryServer
from network.ws_server import WebSocketProtocolServer
from security.auth import AuthManager

logger = logging.getLogger("AirMouseGUI")
ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")


class AirMouseApp(ctk.CTk):
    """Modern dark-themed desktop dashboard with live controls, QR pairing, and scrolling logs."""

    def __init__(self):
        super().__init__()
        self.title("AirMouse Server v1.0.0 (Python Edition)")
        self.geometry("900x650")
        self.minsize(800, 550)

        # Core subsystems
        self.auth_mgr = AuthManager()
        self.mouse = MouseController(sensitivity=1.0, smoothing=True)
        self.loop: Optional[asyncio.AbstractEventLoop] = None
        self.server_thread: Optional[threading.Thread] = None

        self.tcp_srv: Optional[TCPProtocolServer] = None
        self.ws_srv: Optional[WebSocketProtocolServer] = None
        self.udp_srv: Optional[UDPDiscoveryServer] = None
        self.running = False

        self._build_ui()
        self._generate_qr()

    def _get_local_ip(self) -> str:
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

        # Left Sidebar
        self.sidebar = ctk.CTkFrame(self, width=220, corner_radius=0)
        self.sidebar.grid(row=0, column=0, sticky="nsew", padx=0, pady=0)
        self.sidebar.grid_rowconfigure(8, weight=1)

        self.logo_label = ctk.CTkLabel(
            self.sidebar, text="AirMouse Pro", font=ctk.CTkFont(size=20, weight="bold")
        )
        self.logo_label.grid(row=0, column=0, padx=20, pady=(20, 10))

        self.status_badge = ctk.CTkLabel(
            self.sidebar, text="● STOPPED", text_color="#ef4444", font=ctk.CTkFont(size=14, weight="bold")
        )
        self.status_badge.grid(row=1, column=0, padx=20, pady=5)

        self.start_btn = ctk.CTkButton(
            self.sidebar, text="Start Server", command=self.toggle_server, fg_color="#10b981", hover_color="#059669"
        )
        self.start_btn.grid(row=2, column=0, padx=20, pady=15)

        self.lbl_ip = ctk.CTkLabel(
            self.sidebar, text=f"IP: {self._get_local_ip()}", font=ctk.CTkFont(size=12)
        )
        self.lbl_ip.grid(row=3, column=0, padx=20, pady=5)

        self.lbl_ports = ctk.CTkLabel(
            self.sidebar, text="TCP: 8080 | WS: 8081", font=ctk.CTkFont(size=12)
        )
        self.lbl_ports.grid(row=4, column=0, padx=20, pady=5)

        # Sensitivity Slider
        self.lbl_sens = ctk.CTkLabel(self.sidebar, text="Sensitivity: 1.0x")
        self.lbl_sens.grid(row=5, column=0, padx=20, pady=(20, 5))
        self.sens_slider = ctk.CTkSlider(
            self.sidebar, from_=0.2, to=3.0, number_of_steps=28, command=self._on_sens_change
        )
        self.sens_slider.set(1.0)
        self.sens_slider.grid(row=6, column=0, padx=20, pady=5)

        # Smoothing Toggle
        self.smooth_switch = ctk.CTkSwitch(
            self.sidebar, text="1€ Filter", command=self._on_smooth_toggle
        )
        self.smooth_switch.select()
        self.smooth_switch.grid(row=7, column=0, padx=20, pady=15)

        # Right Main Panel
        self.main_panel = ctk.CTkFrame(self, corner_radius=10)
        self.main_panel.grid(row=0, column=1, sticky="nsew", padx=15, pady=15)
        self.main_panel.grid_columnconfigure(0, weight=1)
        self.main_panel.grid_rowconfigure(2, weight=1)

        # QR Code Display
        self.qr_label = ctk.CTkLabel(self.main_panel, text="")
        self.qr_label.grid(row=0, column=0, padx=20, pady=10)

        self.qr_sub = ctk.CTkLabel(
            self.main_panel, text="Scan with Android AirMouse app to pair securely", font=ctk.CTkFont(size=12)
        )
        self.qr_sub.grid(row=1, column=0, padx=20, pady=(0, 10))

        # Live scrolling log box
        self.log_box = ctk.CTkTextbox(self.main_panel, font=ctk.CTkFont(family="Courier", size=11))
        self.log_box.grid(row=2, column=0, sticky="nsew", padx=15, pady=10)
        self.log_box.configure(state="disabled")

    def _on_sens_change(self, val: float) -> None:
        self.mouse.sensitivity = val
        self.lbl_sens.configure(text=f"Sensitivity: {val:.1f}x")

    def _on_smooth_toggle(self) -> None:
        self.mouse.smoothing = bool(self.smooth_switch.get())

    def _generate_qr(self) -> None:
        ip = self._get_local_ip()
        token = self.auth_mgr.generate_token("android_client")
        qr_content = f"airmouse://{ip}:8080?token={token}&ws=8081&tls=1"

        qr = qrcode.QRCode(box_size=4, border=2)
        qr.add_data(qr_content)
        qr.make(fit=True)
        img = qr.make_image(fill_color="white", back_color="#1e293b").convert("RGB")

        ctk_img = ctk.CTkImage(light_image=img, dark_image=img, size=(160, 160))
        self.qr_label.configure(image=ctk_img)

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

    def _start_services(self) -> None:
        self.running = True
        self.status_badge.configure(text="● RUNNING", text_color="#10b981")
        self.start_btn.configure(text="Stop Server", fg_color="#ef4444", hover_color="#dc2626")

        self.tcp_srv = TCPProtocolServer(
            host="0.0.0.0", port=8080, enable_tls=True, auth_manager=self.auth_mgr, mouse_controller=self.mouse, on_log=self.log
        )
        self.ws_srv = WebSocketProtocolServer(
            host="0.0.0.0", port=8081, enable_tls=True, auth_manager=self.auth_mgr, mouse_controller=self.mouse, on_log=self.log
        )
        self.udp_srv = UDPDiscoveryServer(
            port=8083, tcp_port=8080, ws_port=8081, tls_enabled=True,
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
        self.log("All servers initialized (TCP/TLS:8080, WS/WSS:8081, UDP:8083)")

    def _stop_services(self) -> None:
        self.running = False
        self.status_badge.configure(text="● STOPPED", text_color="#ef4444")
        self.start_btn.configure(text="Start Server", fg_color="#10b981", hover_color="#059669")

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


if __name__ == "__main__":
    app = AirMouseApp()
    app.mainloop()
