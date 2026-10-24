import logging
import time
from typing import Optional
from core.one_euro import OneEuroFilter

logger = logging.getLogger("MouseController")

try:
    from pynput.mouse import Button, Controller
    PYNPUT_AVAILABLE = True
except Exception:
    PYNPUT_AVAILABLE = False


class MouseController:
    """Controls OS cursor displacement and buttons using pynput with integrated 1€ filtering."""

    def __init__(self, sensitivity: float = 1.0, smoothing: bool = True):
        self.sensitivity = sensitivity
        self.smoothing = smoothing
        self.filter_x = OneEuroFilter(min_cutoff=1.0, beta=0.007, d_cutoff=1.0)
        self.filter_y = OneEuroFilter(min_cutoff=1.0, beta=0.007, d_cutoff=1.0)
        self._controller = Controller() if PYNPUT_AVAILABLE else None

    def move(self, dx: float, dy: float) -> None:
        now = time.time()
        if self.smoothing:
            filtered_dx = self.filter_x.filter(dx, now)
            filtered_dy = self.filter_y.filter(dy, now)
        else:
            filtered_dx = dx
            filtered_dy = dy

        final_x = filtered_dx * self.sensitivity
        final_y = filtered_dy * self.sensitivity

        if self._controller:
            try:
                self._controller.move(final_x, final_y)
            except Exception as e:
                logger.error(f"Mouse move error: {e}")
        else:
            logger.debug(f"[Virtual Mouse] dx={final_x:.2f}, dy={final_y:.2f}")

    def click(self, button_name: str = "left") -> None:
        if not self._controller:
            logger.debug(f"[Virtual Mouse] Click: {button_name}")
            return

        btn = Button.left
        if button_name == "right":
            btn = Button.right
        elif button_name == "middle":
            btn = Button.middle

        if button_name == "double":
            self._controller.click(Button.left, 2)
        else:
            self._controller.click(btn, 1)

    def scroll(self, delta: float) -> None:
        if not self._controller:
            logger.debug(f"[Virtual Mouse] Scroll: {delta}")
            return
        self._controller.scroll(0, int(delta))

    def reset(self) -> None:
        self.filter_x.reset()
        self.filter_y.reset()
