from PIL import Image, ImageDraw
import pystray
from pystray import MenuItem as item

class TrayManager:
    def __init__(self, gui):
        self.gui = gui
        self.icon = None
        self.running = False

    def start(self):
        if self.running:
            return
        self._build_icon()
        self.icon.run_detached()
        self.running = True

    def stop(self):
        if self.icon:
            self.icon.stop()
        self.running = False

    def update_status(self, active: bool):
        if not self.icon:
            return
        self.icon.icon = self._make_icon_image(active)

    def _build_icon(self):
        self.icon = pystray.Icon(
            "airmouse",
            self._make_icon_image(False),
            menu=pystray.Menu(
                item('Show Window', self._restore_window, default=True),
                item('Start Server', self.gui.start_servers),
                item('Stop Server', self.gui.stop_servers),
                item('Exit', self.gui._quit_app)
            )
        )

    def _restore_window(self):
        self.gui.root.deiconify()
        self.gui.root.lift()

    def _make_icon_image(self, active):
        size = 64
        img = Image.new('RGBA', (size, size), color=0)
        d = ImageDraw.Draw(img)
        color = self.gui.success if active else self.gui.danger
        d.ellipse((8, 8, size-8, size-8), fill=color)
        d.ellipse((16, 16, size-16, size-16), fill='white')
        return img.resize((16, 16), Image.LANCZOS)