import psutil
import threading
import time

class PerformanceMonitor:
    def __init__(self, interval=2.0):
        self.interval = interval
        self._cpu = 0.0
        self._mem = 0.0
        self._running = False
        self._thread = None

    def start(self):
        self._running = True
        self._thread = threading.Thread(target=self._loop, daemon=True)
        self._thread.start()

    def stop(self):
        self._running = False

    def _loop(self):
        while self._running:
            self._cpu = psutil.cpu_percent(interval=None)
            self._mem = psutil.virtual_memory().percent
            time.sleep(self.interval)

    @property
    def cpu_usage(self) -> float:
        return self._cpu

    @property
    def memory_usage(self) -> float:
        return self._mem