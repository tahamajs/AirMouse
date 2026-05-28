import pyautogui
from dataclasses import dataclass
from typing import Dict
from .config import CONFIG

@dataclass
class MouseController:
    sensitivity: float = CONFIG.get("sensitivity", 0.5)
    click_count: int = 0
    double_click_count: int = 0
    right_click_count: int = 0
    scroll_count: int = 0

    def __post_init__(self):
        pyautogui.FAILSAFE = True
        pyautogui.PAUSE = 0

    def move(self, dx: float, dy: float) -> None:
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        if abs(dx) < 0.15 and abs(dy) < 0.15:
            return
        pyautogui.moveRel(dx, dy, duration=0.0)

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