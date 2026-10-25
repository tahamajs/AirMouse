import logging
import platform
import subprocess
from typing import List, Optional, Set, Union

logger = logging.getLogger("KeyboardController")

try:
    from pynput.keyboard import Controller, Key, KeyCode
    PYNPUT_AVAILABLE = True
except Exception:
    PYNPUT_AVAILABLE = False


class KeyboardController:
    """Controls OS keyboard input, hotkeys, presentation controls, and system actions."""

    KEY_MAP = {
        "enter": Key.enter if PYNPUT_AVAILABLE else "enter",
        "return": Key.enter if PYNPUT_AVAILABLE else "return",
        "esc": Key.esc if PYNPUT_AVAILABLE else "esc",
        "escape": Key.esc if PYNPUT_AVAILABLE else "escape",
        "space": Key.space if PYNPUT_AVAILABLE else "space",
        "tab": Key.tab if PYNPUT_AVAILABLE else "tab",
        "backspace": Key.backspace if PYNPUT_AVAILABLE else "backspace",
        "delete": Key.delete if PYNPUT_AVAILABLE else "delete",
        "up": Key.up if PYNPUT_AVAILABLE else "up",
        "down": Key.down if PYNPUT_AVAILABLE else "down",
        "left": Key.left if PYNPUT_AVAILABLE else "left",
        "right": Key.right if PYNPUT_AVAILABLE else "right",
        "pageup": Key.page_up if PYNPUT_AVAILABLE else "pageup",
        "page_up": Key.page_up if PYNPUT_AVAILABLE else "page_up",
        "pagedown": Key.page_down if PYNPUT_AVAILABLE else "pagedown",
        "page_down": Key.page_down if PYNPUT_AVAILABLE else "page_down",
        "home": Key.home if PYNPUT_AVAILABLE else "home",
        "end": Key.end if PYNPUT_AVAILABLE else "end",
        "ctrl": Key.ctrl if PYNPUT_AVAILABLE else "ctrl",
        "control": Key.ctrl if PYNPUT_AVAILABLE else "control",
        "alt": Key.alt if PYNPUT_AVAILABLE else "alt",
        "option": Key.alt if PYNPUT_AVAILABLE else "option",
        "shift": Key.shift if PYNPUT_AVAILABLE else "shift",
        "cmd": Key.cmd if PYNPUT_AVAILABLE else "cmd",
        "command": Key.cmd if PYNPUT_AVAILABLE else "command",
        "super": Key.cmd if PYNPUT_AVAILABLE else "super",
        "win": Key.cmd if PYNPUT_AVAILABLE else "win",
        "windows": Key.cmd if PYNPUT_AVAILABLE else "windows",
        "capslock": Key.caps_lock if PYNPUT_AVAILABLE else "capslock",
        "f1": Key.f1 if PYNPUT_AVAILABLE else "f1",
        "f2": Key.f2 if PYNPUT_AVAILABLE else "f2",
        "f3": Key.f3 if PYNPUT_AVAILABLE else "f3",
        "f4": Key.f4 if PYNPUT_AVAILABLE else "f4",
        "f5": Key.f5 if PYNPUT_AVAILABLE else "f5",
        "f6": Key.f6 if PYNPUT_AVAILABLE else "f6",
        "f7": Key.f7 if PYNPUT_AVAILABLE else "f7",
        "f8": Key.f8 if PYNPUT_AVAILABLE else "f8",
        "f9": Key.f9 if PYNPUT_AVAILABLE else "f9",
        "f10": Key.f10 if PYNPUT_AVAILABLE else "f10",
        "f11": Key.f11 if PYNPUT_AVAILABLE else "f11",
        "f12": Key.f12 if PYNPUT_AVAILABLE else "f12",
        "volume_up": getattr(Key, "media_volume_up", None) if PYNPUT_AVAILABLE else "volume_up",
        "volume_down": getattr(Key, "media_volume_down", None) if PYNPUT_AVAILABLE else "volume_down",
        "media_mute": getattr(Key, "media_volume_mute", None) if PYNPUT_AVAILABLE else "media_mute",
        "media_play_pause": getattr(Key, "media_play_pause", None) if PYNPUT_AVAILABLE else "media_play_pause",
    }

    def __init__(self):
        self._controller = Controller() if PYNPUT_AVAILABLE else None
        self._held_keys: Set[Union[Key, KeyCode, str]] = set()

    def _resolve_key(self, key_str: str):
        if not PYNPUT_AVAILABLE:
            return key_str
        lower = key_str.lower().strip()
        if lower in self.KEY_MAP and self.KEY_MAP[lower] is not None:
            return self.KEY_MAP[lower]
        if len(key_str) == 1:
            try:
                return KeyCode.from_char(key_str)
            except Exception:
                pass
        return KeyCode.from_char(key_str[0]) if key_str else Key.space

    def key_down(self, key_str: str) -> None:
        key = self._resolve_key(key_str)
        self._held_keys.add(key)
        if self._controller:
            try:
                self._controller.press(key)
            except Exception as e:
                logger.error(f"Error in key_down {key_str}: {e}")
        else:
            logger.debug(f"[Virtual Keyboard] key_down: {key_str}")

    def key_up(self, key_str: str) -> None:
        key = self._resolve_key(key_str)
        self._held_keys.discard(key)
        if self._controller:
            try:
                self._controller.release(key)
            except Exception as e:
                logger.error(f"Error in key_up {key_str}: {e}")
        else:
            logger.debug(f"[Virtual Keyboard] key_up: {key_str}")

    def key_press(self, key_str: str, modifiers: Optional[List[str]] = None) -> None:
        modifiers = modifiers or []
        mod_keys = [self._resolve_key(m) for m in modifiers]
        if self._controller:
            try:
                for m in mod_keys:
                    self._controller.press(m)
                key = self._resolve_key(key_str)
                self._controller.press(key)
                self._controller.release(key)
                for m in reversed(mod_keys):
                    self._controller.release(m)
            except Exception as e:
                logger.error(f"Error in key_press {key_str} with {modifiers}: {e}")
        else:
            logger.debug(f"[Virtual Keyboard] key_press: {key_str} mods={modifiers}")

    def hotkey(self, modifiers: List[str], key_str: str) -> None:
        self.key_press(key_str, modifiers)

    def type_text(self, text: str) -> None:
        if self._controller:
            try:
                self._controller.type(text)
            except Exception as e:
                logger.error(f"Error typing text: {e}")
        else:
            logger.debug(f"[Virtual Keyboard] type: {text}")

    def slide_next(self) -> None:
        self.key_press("right")

    def slide_prev(self) -> None:
        self.key_press("left")

    def volume_up(self) -> None:
        if platform.system() == "Darwin":
            try:
                subprocess.run(
                    ["osascript", "-e", "set volume output volume ((output volume of (get volume settings)) + 5)"],
                    check=False,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
                return
            except Exception:
                pass
        self.key_press("volume_up")

    def volume_down(self) -> None:
        if platform.system() == "Darwin":
            try:
                subprocess.run(
                    ["osascript", "-e", "set volume output volume ((output volume of (get volume settings)) - 5)"],
                    check=False,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
                return
            except Exception:
                pass
        self.key_press("volume_down")

    def mute(self) -> None:
        if platform.system() == "Darwin":
            try:
                subprocess.run(
                    ["osascript", "-e", "set volume output muted not (output muted of (get volume settings))"],
                    check=False,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
                return
            except Exception:
                pass
        self.key_press("media_mute")

    def play_pause(self) -> None:
        self.key_press("media_play_pause")

    def lock_screen(self) -> None:
        sys_name = platform.system()
        if sys_name == "Darwin":
            try:
                subprocess.run(
                    ["pmset", "displaysleepnow"],
                    check=False,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            except Exception:
                pass
        elif sys_name == "Windows":
            try:
                subprocess.run(["rundll32.exe", "user32.dll,LockWorkStation"], check=False)
            except Exception:
                pass
        elif sys_name == "Linux":
            try:
                subprocess.run(["xdg-screensaver", "lock"], check=False)
            except Exception:
                pass

    def power_sleep(self) -> None:
        sys_name = platform.system()
        if sys_name == "Darwin":
            try:
                subprocess.run(
                    ["osascript", "-e", 'tell application "System Events" to sleep'],
                    check=False,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            except Exception:
                pass

    def adjust_brightness(self, delta: float = 0.05) -> None:
        logger.debug(f"Adjust brightness: delta={delta}")

    def release_all(self) -> None:
        """Release all currently pressed keys."""
        if not self._held_keys:
            return
        keys_to_release = list(self._held_keys)
        self._held_keys.clear()
        for k in keys_to_release:
            if self._controller:
                try:
                    self._controller.release(k)
                except Exception as e:
                    logger.debug(f"Error releasing key {k}: {e}")
            else:
                logger.debug(f"[Virtual Keyboard] Released key: {k}")
