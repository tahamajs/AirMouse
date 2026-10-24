import threading
from typing import Callable, Optional
from PIL import Image, ImageDraw
import pystray


def create_tray_icon(is_running: bool = False) -> Image.Image:
    """Draws a 64x64 tray status badge (green for active, red for stopped)."""
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    color = "#10b981" if is_running else "#ef4444"
    draw.ellipse((8, 8, 56, 56), fill=color)
    draw.ellipse((22, 22, 42, 42), fill="#ffffff")
    return image


class TrayManager:
    """Manages cross-platform system tray icon and background controls."""

    def __init__(
        self,
        on_toggle_server: Optional[Callable[[], None]] = None,
        on_show_window: Optional[Callable[[], None]] = None,
        on_exit: Optional[Callable[[], None]] = None,
    ):
        self.on_toggle_server = on_toggle_server
        self.on_show_window = on_show_window
        self.on_exit = on_exit
        self.icon: Optional[pystray.Icon] = None
        self._thread: Optional[threading.Thread] = None

    def start(self, is_running: bool = False) -> None:
        menu = pystray.Menu(
            pystray.MenuItem("Show Window", lambda icon, item: self.on_show_window() if self.on_show_window else None),
            pystray.MenuItem("Toggle Server", lambda icon, item: self.on_toggle_server() if self.on_toggle_server else None),
            pystray.MenuItem("Quit", lambda icon, item: self._quit()),
        )
        self.icon = pystray.Icon("AirMouse", create_tray_icon(is_running), "AirMouse Server", menu)
        self._thread = threading.Thread(target=self.icon.run, daemon=True)
        self._thread.start()

    def update_status(self, is_running: bool) -> None:
        if self.icon:
            self.icon.icon = create_tray_icon(is_running)

    def _quit(self) -> None:
        if self.icon:
            self.icon.stop()
        if self.on_exit:
            self.on_exit()
