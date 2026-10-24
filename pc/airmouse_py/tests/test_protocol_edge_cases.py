"""
Extensive tests for edge cases and all payload schemas in Protocol V2.
"""

import json
import math
import time
import pytest
from pydantic import ValidationError

from network.protocol import (
    AckPayload,
    BoundsViolationError,
    ClickPayload,
    DiscoverPayload,
    HelloPayload,
    KeyPayload,
    MovePayload,
    NackPayload,
    OfferPayload,
    ProtocolParser,
    ReplayAttackError,
    SchemaViolationError,
    ScrollPayload,
    SessionTracker,
    WelcomePayload,
    WireEnvelope,
)


def test_session_tracker_reset():
    tracker = SessionTracker(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    env1 = WireEnvelope(v=2, type="ping", seq=10, ts=now_ms, payload={})
    tracker.validate_envelope(env1, "session_A")

    # Sequence 10 is known for session_A, so seq 9 should raise ReplayAttackError
    env_old = WireEnvelope(v=2, type="ping", seq=9, ts=now_ms, payload={})
    with pytest.raises(ReplayAttackError):
        tracker.validate_envelope(env_old, "session_A")

    # Reset session_A
    tracker.reset_session("session_A")

    # Now seq 1 should be accepted again
    env_new = WireEnvelope(v=2, type="ping", seq=1, ts=now_ms, payload={})
    tracker.validate_envelope(env_new, "session_A")


def test_move_payload_nan_and_inf_validation():
    with pytest.raises((ValidationError, BoundsViolationError)):
        MovePayload(dx=float("nan"), dy=0.0)

    with pytest.raises((ValidationError, BoundsViolationError)):
        MovePayload(dx=0.0, dy=float("inf"))

    with pytest.raises((ValidationError, BoundsViolationError)):
        MovePayload(dx=500.1, dy=0.0)


def test_scroll_payload_bounds():
    with pytest.raises((ValidationError, BoundsViolationError)):
        ScrollPayload(delta=float("nan"))

    with pytest.raises((ValidationError, BoundsViolationError)):
        ScrollPayload(delta=15.0)

    # Valid scroll
    s = ScrollPayload(delta=-3.5)
    assert s.delta == -3.5


def test_key_payload():
    k = KeyPayload(key="Enter", modifiers=["ctrl", "shift"])
    assert k.key == "Enter"
    assert "ctrl" in k.modifiers


def test_hello_and_welcome_payloads():
    h = HelloPayload(device_id="dev-1", name="Dev Phone", version="2.0.0", token="tok123")
    assert h.device_id == "dev-1"

    w = WelcomePayload(server="AirMouse", version="2.0.0", client_id="c1", approved=True)
    assert w.approved is True


def test_ack_and_nack_payloads():
    ack = AckPayload(ack_id="req-abc", status="ok")
    assert ack.status == "ok"

    nack = NackPayload(nack_id="req-abc", error="some error", expected_seq=5)
    assert nack.expected_seq == 5


def test_parser_all_message_types():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    session = "full_types_session"

    types_and_payloads = [
        ("click", {"button": "right"}),
        ("scroll", {"delta": 3.0}),
        ("key", {"key": "Escape", "modifiers": []}),
        ("ack", {"ack_id": "a1", "status": "ok"}),
        ("nack", {"nack_id": "a2", "error": "failed", "expected_seq": 2}),
        ("discover", {"client_name": "mobile", "nonce": "abc"}),
        ("offer", {
            "server_name": "srv",
            "instance_id": "id1",
            "tcp_port": 80,
            "ws_port": 81,
            "tls": False,
            "echo_nonce": "abc"
        }),
        ("ping", {}),
        ("pong", {}),
    ]

    for seq, (mtype, pload) in enumerate(types_and_payloads, start=1):
        raw = json.dumps({"v": 2, "type": mtype, "seq": seq, "ts": now_ms, "payload": pload})
        res_type, parsed_model, req_id, parsed_seq = parser.parse_message(raw, session)
        assert res_type == mtype
        assert parsed_seq == seq


def test_parser_unknown_type_and_invalid_envelope():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    # Unknown message type
    bad_type = json.dumps({"v": 2, "type": "alien_teleport", "seq": 1, "ts": now_ms, "payload": {}})
    with pytest.raises(SchemaViolationError):
        parser.parse_message(bad_type, "sess")

    # Invalid version
    bad_v = json.dumps({"v": 1, "type": "ping", "seq": 1, "ts": now_ms, "payload": {}})
    with pytest.raises(SchemaViolationError):
        parser.parse_message(bad_v, "sess")

    # Missing required fields
    bad_env = json.dumps({"seq": 1, "ts": now_ms})
    with pytest.raises(SchemaViolationError):
        parser.parse_message(bad_env, "sess")


def test_decode_and_validate_compat():
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    raw = json.dumps({
        "v": 2,
        "type": "move",
        "seq": 1,
        "ts": now_ms,
        "payload": {"dx": 5.0, "dy": -5.0}
    })
    msg_type, pload_dict, req_id, seq = parser.decode_and_validate(raw, "s1")
    assert msg_type == "move"
    assert isinstance(pload_dict, dict)
    assert pload_dict["dx"] == 5.0
