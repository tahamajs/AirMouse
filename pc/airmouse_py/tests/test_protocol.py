"""
Unit tests for Protocol V2 parsing and session-scoped replay attack prevention.
"""

import json
import time
import pytest
from network.protocol import (
    BoundsViolationError,
    MovePayload,
    ProtocolParser,
    ReplayAttackError,
    SchemaViolationError,
)


def test_protocol_valid_move():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    packet = {
        "v": 2,
        "type": "move",
        "seq": 1,
        "ts": now_ms,
        "payload": {"dx": 10.5, "dy": -5.2, "dt": 0.016},
    }
    msg_type, payload, req_id, seq = parser.parse_message(json.dumps(packet), "session1")
    assert msg_type == "move"
    assert isinstance(payload, MovePayload)
    assert payload.dx == 10.5
    assert payload.dy == -5.2
    assert seq == 1


def test_protocol_replay_stale_timestamp():
    parser = ProtocolParser(replay_window_ms=5000)
    stale_ts = int((time.time() - 10) * 1000)  # 10s old
    packet = {
        "v": 2,
        "type": "move",
        "seq": 1,
        "ts": stale_ts,
        "payload": {"dx": 1.0, "dy": 1.0},
    }
    with pytest.raises(ReplayAttackError):
        parser.parse_message(json.dumps(packet), "session1")


def test_protocol_replay_duplicate_seq():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    packet1 = {
        "v": 2,
        "type": "move",
        "seq": 5,
        "ts": now_ms,
        "payload": {"dx": 1.0, "dy": 1.0},
    }
    packet2 = {
        "v": 2,
        "type": "move",
        "seq": 5,  # Duplicate non-monotonic seq in same session
        "ts": now_ms + 10,
        "payload": {"dx": 1.0, "dy": 1.0},
    }
    parser.parse_message(json.dumps(packet1), "session1")
    with pytest.raises(ReplayAttackError):
        parser.parse_message(json.dumps(packet2), "session1")


def test_protocol_out_of_bounds():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    packet = {
        "v": 2,
        "type": "move",
        "seq": 1,
        "ts": now_ms,
        "payload": {"dx": 999.0, "dy": 0.0},  # Bounds |dx| <= 500
    }
    with pytest.raises(BoundsViolationError):
        parser.parse_message(json.dumps(packet), "session1")


def test_protocol_disallowed_unknown_fields():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    packet = {
        "v": 2,
        "type": "move",
        "seq": 1,
        "ts": now_ms,
        "payload": {"dx": 1.0, "dy": 1.0, "unexpected_key": True},
    }
    with pytest.raises(SchemaViolationError):
        parser.parse_message(json.dumps(packet), "session1")
