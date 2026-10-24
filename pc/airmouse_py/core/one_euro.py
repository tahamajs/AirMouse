import math
from typing import Optional


class LowPassFilter:
    def __init__(self, alpha: float = 1.0):
        self.alpha = alpha
        self.y: Optional[float] = None
        self.s: Optional[float] = None

    def filter(self, value: float, alpha: Optional[float] = None) -> float:
        if alpha is not None:
            self.alpha = alpha
        if self.y is None:
            self.s = value
            self.y = value
        else:
            self.y = self.alpha * value + (1.0 - self.alpha) * self.s
            self.s = self.y
        return self.y


class OneEuroFilter:
    """
    1€ (One Euro) adaptive low-pass filter (Casiez et al. 2012).
    Dynamically adjusts cutoff frequency based on instantaneous speed to eliminate jitter at rest
    and minimize lag during rapid cursor motions.
    """

    def __init__(self, min_cutoff: float = 1.0, beta: float = 0.007, d_cutoff: float = 1.0):
        self.min_cutoff = min_cutoff
        self.beta = beta
        self.d_cutoff = d_cutoff
        self.x_filter = LowPassFilter()
        self.dx_filter = LowPassFilter()
        self.last_time: Optional[float] = None

    def _alpha(self, rate: float, cutoff: float) -> float:
        if cutoff <= 0.0 or math.isnan(cutoff) or math.isinf(cutoff):
            cutoff = 1e-4
        if rate <= 0.0 or math.isnan(rate) or math.isinf(rate):
            rate = 60.0
        tau = 1.0 / (2.0 * math.pi * cutoff)
        te = 1.0 / rate
        return 1.0 / (1.0 + tau / te)

    def filter(self, x: float, timestamp: float) -> float:
        if self.last_time is None:
            self.last_time = timestamp
            return self.x_filter.filter(x, 1.0)

        dt = timestamp - self.last_time
        self.last_time = timestamp
        if dt <= 0.0:
            dt = 1e-3

        rate = 1.0 / dt
        dx = (x - self.x_filter.y) * rate if self.x_filter.y is not None else 0.0
        edx = self.dx_filter.filter(dx, self._alpha(rate, self.d_cutoff))
        cutoff = self.min_cutoff + self.beta * abs(edx)

        return self.x_filter.filter(x, self._alpha(rate, cutoff))

    def reset(self) -> None:
        self.x_filter = LowPassFilter()
        self.dx_filter = LowPassFilter()
        self.last_time = None
