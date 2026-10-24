"""
Unit tests for One Euro Filter and UDP Jitter Buffer.
"""

import time
from core.one_euro import OneEuroFilter
from core.jitter_buffer import UDPJitterBuffer


def test_one_euro_filter_reduces_noise():
    f = OneEuroFilter(min_cutoff=1.0, beta=0.007)
    noisy_sequence = [0.0, 0.5, -0.4, 0.6, -0.5, 0.4]
    t = time.time()
    outputs = []
    for val in noisy_sequence:
        outputs.append(f.filter(val, t))
        t += 0.016

    assert abs(outputs[-1]) < abs(noisy_sequence[-1])


def test_jitter_buffer_ordering():
    jb = UDPJitterBuffer(min_depth=1, max_depth=5)
    jb.push(2, "pkg2")
    jb.push(1, "pkg1")
    jb.push(3, "pkg3")

    first = jb.pop()
    assert first == "pkg1"
