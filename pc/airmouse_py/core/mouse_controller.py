import logging
import time
from typing import Optional, Set
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
        self._held_buttons: Set[str] = set()

    def _to_button(self, name: str):
        if not PYNPUT_AVAILABLE:
            return None
        name_lower = name.lower()
        if name_lower in ("right", "r", "right_click"):
            return Button.right
        elif name_lower in ("middle", "m", "middle_click", "middleclick"):
            return Button.middle
        return Button.left

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

    def move_abs(self, x: float, y: float) -> None:
        if self._controller:
            try:
                self._controller.position = (int(x), int(y))
            except Exception as e:
                logger.error(f"Mouse move_abs error: {e}")
        else:
            logger.debug(f"[Virtual Mouse] move_abs x={x:.2f}, y={y:.2f}")

    def click(self, button_name: str = "left") -> None:
        if not self._controller:
            logger.debug(f"[Virtual Mouse] Click: {button_name}")
            return

        if button_name in ("double", "double_click"):
            self._controller.click(Button.left, 2)
        else:
            btn = self._to_button(button_name)
            self._controller.click(btn, 1)

    def mouse_down(self, button_name: str = "left") -> None:
        self._held_buttons.add(button_name)
        if not self._controller:
            logger.debug(f"[Virtual Mouse] MouseDown: {button_name}")
            return
        btn = self._to_button(button_name)
        try:
            self._controller.press(btn)
        except Exception as e:
            logger.error(f"Mouse press error: {e}")

    def mouse_up(self, button_name: str = "left") -> None:
        self._held_buttons.discard(button_name)
        if not self._controller:
            logger.debug(f"[Virtual Mouse] MouseUp: {button_name}")
            return
        btn = self._to_button(button_name)
        try:
            self._controller.release(btn)
        except Exception as e:
            logger.error(f"Mouse release error: {e}")

    def double_click(self) -> None:
        self.click("double")

    def scroll(self, delta: float) -> None:
        if not self._controller:
            logger.debug(f"[Virtual Mouse] Scroll: {delta}")
            return
        self._controller.scroll(0, int(delta))

    def hscroll(self, delta: float) -> None:
        if not self._controller:
            logger.debug(f"[Virtual Mouse] HScroll: {delta}")
            return
        self._controller.scroll(int(delta), 0)

    def release_all(self) -> None:
        """Release all currently held mouse buttons."""
        if not self._held_buttons:
            return
        buttons_to_release = list(self._held_buttons)
        self._held_buttons.clear()
        for b in buttons_to_release:
            if self._controller:
                btn = self._to_button(b)
                try:
                    self._controller.release(btn)
                except Exception as e:
                    logger.debug(f"Error releasing button {b}: {e}")
            else:
                logger.debug(f"[Virtual Mouse] Released button: {b}")

    def reset(self) -> None:
        self.filter_x.reset()
        self.filter_y.reset()
