"""
airmouse_py.core.jitter_buffer
------------------------------
Adaptive jitter compensation buffer for UDP motion datagram streams.
NOTE: TCP is inherently reliable and in-order; putting a jitter buffer on TCP causes head-of-line
queueing delays and latency bloat. This buffer is designed strictly for UDP datagram streams where
out-of-order packets and packet transit variance (jitter) occur naturally.
"""

import heapq
import time
from typing import Any, List, Optional, Tuple


class UDPJitterBuffer:
    """
    Adaptive jitter compensation buffer for unreliable/unordered UDP motion streams.
    Tracks transit delay variation (RFC 3550 style) to dynamically size buffer depth
    and discards stale packets older than the highest dispatched sequence number.
    """

    def __init__(self, min_depth: int = 1, max_depth: int = 8) -> None:
        self.min_depth = min_depth
        self.max_depth = max_depth
        # Min-heap storing: (sequence_number, arrival_timestamp, packet_payload)
        self.queue: List[Tuple[int, float, Any]] = []
        self.jitter: float = 0.0
        self.last_arrival_time: Optional[float] = None
        self.last_delay: Optional[float] = None
        self.highest_popped_seq: int = -1

    def push(self, seq: int, packet: Any) -> None:
        """
        Pushes a new UDP packet into the priority queue and updates jitter statistics.
        Stale packets with seq <= highest_popped_seq are immediately dropped.
        """
        now = time.time()

        # Update jitter calculation: J(i) = J(i-1) + (|D(i-1, i)| - J(i-1)) / 16
        if self.last_arrival_time is not None:
            delay = now - self.last_arrival_time
            if self.last_delay is not None:
                diff = abs(delay - self.last_delay)
                self.jitter += (diff - self.jitter) / 16.0
            self.last_delay = delay
        self.last_arrival_time = now

        # Reject out-of-order packets that arrived too late (already passed in time)
        if seq > self.highest_popped_seq:
            heapq.heappush(self.queue, (seq, now, packet))

    def get_target_depth(self) -> int:
        """
        Dynamically derives optimal queue depth based on estimated jitter.
        Higher jitter network -> larger depth to re-order packets.
        Clean local Wi-Fi -> depth 1 for immediate dispatch.
        """
        # Convert jitter seconds into estimated packet slots (assuming ~60Hz / 16ms packets)
        target = int((self.jitter / 0.016) * 1.5)
        return max(self.min_depth, min(target, self.max_depth))

    def pop(self) -> Optional[Any]:
        """
        Pops the oldest in-sequence packet once target buffering depth is satisfied.
        """
        if len(self.queue) < self.get_target_depth():
            return None

        if self.queue:
            seq, _, packet = heapq.heappop(self.queue)
            self.highest_popped_seq = seq
            return packet
        return None

    def flush(self) -> List[Any]:
        """Flushes and returns all buffered packets in sequence order."""
        items = []
        while self.queue:
            seq, _, packet = heapq.heappop(self.queue)
            if seq > self.highest_popped_seq:
                self.highest_popped_seq = seq
                items.append(packet)
        return items

    def reset(self) -> None:
        """Clears buffer state for a new stream."""
        self.queue.clear()
        self.jitter = 0.0
        self.last_arrival_time = None
        self.last_delay = None
        self.highest_popped_seq = -1
