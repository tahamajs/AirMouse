"""
Unit tests for MouseController, MouseActuatorThread, and UDPJitterBuffer edge cases.
"""

import queue
import time
from core.actuator import MouseActuatorThread
from core.jitter_buffer import UDPJitterBuffer
from core.mouse_controller import MouseController


def test_mouse_controller_without_pynput():
    # MouseController should operate without crashing in all modes
    ctrl = MouseController(sensitivity=1.5, smoothing=True)
    ctrl.move(10.0, 20.0)
    ctrl.click("left")
    ctrl.click("right")
    ctrl.click("middle")
    ctrl.click("double")
    ctrl.scroll(2.0)
    ctrl.reset()

    # Without smoothing
    ctrl_no_smooth = MouseController(sensitivity=1.0, smoothing=False)
    ctrl_no_smooth.move(5.0, -5.0)


def test_mouse_actuator_thread_lifecycle():
    q = queue.Queue()
    actuator = MouseActuatorThread(command_queue=q, sensitivity=1.0, smoothing=True)

    # Queue commands
    q.put(("move", 5.0, 5.0))
    q.put(("click", "left"))
    q.put(("scroll", 1.0))
    q.put(None)  # Poison pill

    # Start loop (will process queue and exit cleanly upon encountering None)
    actuator.start()

    assert q.empty()


def test_udp_jitter_buffer_full_flow():
    jb = UDPJitterBuffer(min_depth=2, max_depth=6)

    # Sequence out of order
    jb.push(seq=3, packet="p3")
    jb.push(seq=1, packet="p1")
    jb.push(seq=2, packet="p2")

    depth = jb.get_target_depth()
    assert depth >= 2

    # Pop item
    first = jb.pop()
    assert first == "p1"

    # Flush remaining
    flushed = jb.flush()
    assert "p2" in flushed
    assert "p3" in flushed

    # Reset
    jb.reset()
    assert len(jb.queue) == 0
    assert jb.jitter == 0.0
