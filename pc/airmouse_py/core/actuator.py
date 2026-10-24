import asyncio
import logging
import queue
import time
from typing import Optional
from core.mouse_controller import MouseController

logger = logging.getLogger("AirMouse.Actuator")


class MouseActuatorThread:
    """
    Dedicated worker thread for OS mouse manipulation.
    Decouples network socket reader coroutines (asyncio) from OS windowing/mouse
    drivers (pynput/CoreGraphics/user32.dll) via a thread-safe queue.Queue.
    """

    def __init__(
        self,
        command_queue: queue.Queue,
        sensitivity: float = 1.0,
        smoothing: bool = True,
    ) -> None:
        self.command_queue = command_queue
        self.controller = MouseController(sensitivity=sensitivity, smoothing=smoothing)
        self._running = False

    def start(self) -> None:
        """Starts the blocking event loop on the current thread."""
        self._running = True
        logger.info("Mouse actuator worker thread started.")
        while self._running:
            try:
                # Wait up to 100ms for incoming command to allow responsive shutdown
                event = self.command_queue.get(timeout=0.1)
            except queue.Empty:
                continue

            if event is None:
                # Poison pill received for clean shutdown
                break

            cmd_type = event[0]
            try:
                if cmd_type == "move":
                    _, dx, dy = event
                    self.controller.move(dx, dy)
                elif cmd_type == "click":
                    _, button = event
                    self.controller.click(button)
                elif cmd_type == "scroll":
                    _, delta = event
                    self.controller.scroll(delta)
            except Exception as e:
                logger.error(f"Error actuating mouse command {cmd_type}: {e}")
            finally:
                self.command_queue.task_done()

        logger.info("Mouse actuator worker thread stopped.")

    def stop(self) -> None:
        """Signals worker loop to terminate."""
        self._running = False
        self.command_queue.put(None)
