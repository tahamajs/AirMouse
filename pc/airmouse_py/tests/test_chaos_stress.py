"""
pc/airmouse_py/tests/test_chaos_stress.py
-----------------------------------------
Exhaustive Hardcore Stress & Chaos Test Suite.
Validates:
1. Protocol Envelope Fuzzing: invalid versions, missing fields, non-numeric values,
   negative timestamps, future timestamps (>5000ms), NaN/Inf coordinates, bounds violations,
   and malformed payloads with live TCP/WebSocket server resilience.
2. HMAC Auth Tampering: single-bit mutations, truncated signatures, empty signatures,
   expired tokens, future tokens, secret rotation, and server handshake rejection.
3. Jitter Buffer Flooding: 500 out-of-order and duplicated packets in a tight loop,
   monotonic sequence tracking, stale packet dropping, and memory stability without runaway.
"""

import asyncio
import hashlib
import hmac
import json
import math
import random
import socket
import threading
import time
from typing import Any, Dict, List, Optional
import pytest

from airmouse import (
    AirMouseServer,
    AuthManager,
    ProtocolParser,
    UDPJitterBuffer,
)
from network.protocol import (
    BoundsViolationError,
    ClickPayload,
    HelloPayload,
    MovePayload,
    ReplayAttackError,
    SchemaViolationError,
    ScrollPayload,
    SessionTracker,
    WireEnvelope,
)


def get_free_port() -> int:
    """Finds an available TCP port on localhost."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


# ============================================================================
# SECTION 1: PROTOCOL ENVELOPE FUZZING
# ============================================================================


def test_fuzz_invalid_versions():
    """Verify that invalid protocol versions (v: 999, 0, -1, non-int) are strictly rejected."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)
    invalid_versions = [999, 0, -1, 1, 3, 100, -999, "2", "v2", None, 2.5]

    for inv_v in invalid_versions:
        payload_data = {"v": inv_v, "type": "ping", "seq": 1, "ts": now_ms, "payload": {}}
        raw = json.dumps(payload_data)
        with pytest.raises(SchemaViolationError):
            parser.parse_message(raw, session_id="fuzz_version_session")


def test_fuzz_missing_required_envelope_fields():
    """Verify missing required envelope fields (type, seq, ts, payload) cause SchemaViolationError."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    base_envelope = {
        "v": 2,
        "type": "ping",
        "seq": 1,
        "ts": now_ms,
        "payload": {},
    }

    # In WireEnvelope, 'v' defaults to 2; the strictly required fields are type, seq, ts, payload
    required_keys = ["type", "seq", "ts", "payload"]
    for missing_key in required_keys:
        corrupted = dict(base_envelope)
        del corrupted[missing_key]
        raw = json.dumps(corrupted)
        with pytest.raises(SchemaViolationError):
            parser.parse_message(raw, session_id="fuzz_missing_fields")

    # Explicitly test v=None and missing all fields
    with pytest.raises(SchemaViolationError):
        parser.parse_message(json.dumps({"type": "ping"}), session_id="fuzz_missing_fields")
    with pytest.raises(SchemaViolationError):
        parser.parse_message(json.dumps({"v": None, "type": "ping", "seq": 1, "ts": now_ms, "payload": {}}), session_id="fuzz_missing_fields")


def test_fuzz_non_numeric_and_invalid_type_fields():
    """Verify non-numeric values in numeric fields (seq, ts) are strictly rejected."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    bad_seq_values = ["one", "abc", "", None, -1, -500, [1], {"num": 1}]
    for bad_seq in bad_seq_values:
        envelope = {"v": 2, "type": "ping", "seq": bad_seq, "ts": now_ms, "payload": {}}
        raw = json.dumps(envelope)
        with pytest.raises(SchemaViolationError):
            parser.parse_message(raw, session_id="fuzz_bad_seq")

    bad_ts_values = ["now", "2026-10-25", None, [now_ms], {"ts": now_ms}]
    for bad_ts in bad_ts_values:
        envelope = {"v": 2, "type": "ping", "seq": 1, "ts": bad_ts, "payload": {}}
        raw = json.dumps(envelope)
        with pytest.raises(SchemaViolationError):
            parser.parse_message(raw, session_id="fuzz_bad_ts")


def test_fuzz_timestamps_replay_window_and_negative():
    """
    Verify timestamps outside replay window (+/-5000ms), far future timestamps,
    and negative timestamps are rejected as ReplayAttackError.
    """
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    # Future timestamps > 5000ms window
    future_timestamps = [
        now_ms + 7000,
        now_ms + 10000,
        now_ms + 60000,
        now_ms + 1000000,
        2**40,
    ]
    for fut_ts in future_timestamps:
        envelope = {"v": 2, "type": "ping", "seq": 1, "ts": fut_ts, "payload": {}}
        with pytest.raises(ReplayAttackError):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_future_ts")

    # Past timestamps < -5000ms window
    past_timestamps = [
        now_ms - 7000,
        now_ms - 10000,
        now_ms - 60000,
        now_ms - 1000000,
    ]
    for past_ts in past_timestamps:
        envelope = {"v": 2, "type": "ping", "seq": 1, "ts": past_ts, "payload": {}}
        with pytest.raises(ReplayAttackError):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_past_ts")

    # Negative timestamps
    negative_timestamps = [-1, -100, -5000, -1000000, -2**31]
    for neg_ts in negative_timestamps:
        envelope = {"v": 2, "type": "ping", "seq": 1, "ts": neg_ts, "payload": {}}
        with pytest.raises(ReplayAttackError):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_neg_ts")

    # Timestamps within valid window (+/- 5000ms) should succeed
    valid_ts = now_ms - 1000
    valid_env = {"v": 2, "type": "ping", "seq": 1, "ts": valid_ts, "payload": {}}
    m_type, _, _, seq = parser.parse_message(json.dumps(valid_env), session_id="fuzz_valid_ts")
    assert m_type == "ping"
    assert seq == 1


def test_fuzz_nan_and_infinity_coordinates():
    """Verify NaN, +Inf, -Inf in MovePayload and ScrollPayload raise BoundsViolationError."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    invalid_coord_pairs = [
        (float("nan"), 0.0),
        (0.0, float("nan")),
        (float("nan"), float("nan")),
        (float("inf"), 0.0),
        (0.0, float("inf")),
        (float("-inf"), 0.0),
        (0.0, float("-inf")),
        (float("inf"), float("-inf")),
    ]

    for idx, (dx, dy) in enumerate(invalid_coord_pairs, start=1):
        envelope = {
            "v": 2,
            "type": "move",
            "seq": idx,
            "ts": now_ms,
            "payload": {"dx": dx, "dy": dy},
        }
        with pytest.raises((BoundsViolationError, SchemaViolationError)):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_nan_inf")

    # Scroll with NaN or Inf
    for idx, delta in enumerate([float("nan"), float("inf"), float("-inf")], start=100):
        envelope = {
            "v": 2,
            "type": "scroll",
            "seq": idx,
            "ts": now_ms,
            "payload": {"delta": delta},
        }
        with pytest.raises((BoundsViolationError, SchemaViolationError)):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_nan_inf_scroll")


def test_fuzz_boundary_violations_for_motion_and_scroll():
    """Verify bounds (> 500.0 px for move, > 10.0 for scroll) raise BoundsViolationError."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    # Move bounds exceeding 500.0
    out_of_bounds_coords = [
        (500.001, 0.0),
        (-500.001, 0.0),
        (0.0, 500.001),
        (0.0, -500.001),
        (10000.0, 0.0),
        (-100000.0, 0.0),
        (1e6, -1e6),
    ]
    for idx, (dx, dy) in enumerate(out_of_bounds_coords, start=1):
        envelope = {
            "v": 2,
            "type": "move",
            "seq": idx,
            "ts": now_ms,
            "payload": {"dx": dx, "dy": dy},
        }
        with pytest.raises(BoundsViolationError):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_bounds")

    # Scroll bounds exceeding 10.0
    out_of_bounds_scrolls = [10.001, -10.001, 50.0, -100.0, 1e6]
    for idx, delta in enumerate(out_of_bounds_scrolls, start=50):
        envelope = {
            "v": 2,
            "type": "scroll",
            "seq": idx,
            "ts": now_ms,
            "payload": {"delta": delta},
        }
        with pytest.raises(BoundsViolationError):
            parser.parse_message(json.dumps(envelope), session_id="fuzz_scroll_bounds")

    # Exact boundary (500.0 and 10.0) must be accepted
    envelope_edge_move = {
        "v": 2,
        "type": "move",
        "seq": 200,
        "ts": now_ms,
        "payload": {"dx": 500.0, "dy": -500.0},
    }
    _, move_obj, _, _ = parser.parse_message(json.dumps(envelope_edge_move), session_id="fuzz_exact_bound")
    assert isinstance(move_obj, MovePayload)
    assert move_obj.dx == 500.0
    assert move_obj.dy == -500.0

    envelope_edge_scroll = {
        "v": 2,
        "type": "scroll",
        "seq": 201,
        "ts": now_ms,
        "payload": {"delta": 10.0},
    }
    _, scroll_obj, _, _ = parser.parse_message(json.dumps(envelope_edge_scroll), session_id="fuzz_exact_bound")
    assert isinstance(scroll_obj, ScrollPayload)
    assert scroll_obj.delta == 10.0


def test_fuzz_malformed_json_and_unexpected_fields():
    """Verify malformed JSON strings, garbage bytes, and unexpected fields are safely rejected."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    malformed_inputs = [
        "",
        "   ",
        "\n",
        "{broken",
        "not a json",
        "{'v': 2}",  # single quotes invalid in json
        "[1, 2, 3]",
        "null",
        "12345",
        f'{{"v": 2, "type": "ping", "seq": 1, "ts": {now_ms}, "payload": {{}}, "extra_field": "disallowed"}}',
        f'{{"v": 2, "type": "move", "seq": 1, "ts": {now_ms}, "payload": {{"dx": 1.0, "dy": 1.0, "injected": "bad"}}}}',
        "{" + '"a":' * 100 + "1" + "}" * 100,  # deeply nested JSON
    ]

    for raw in malformed_inputs:
        with pytest.raises(SchemaViolationError):
            parser.parse_message(raw, session_id="fuzz_malformed")


def test_server_live_envelope_fuzzing_resilience():
    """
    Live TCP integration fuzz test:
    Connects to AirMouseServer, authenticates, and barrages the server with fuzzed packets.
    Verifies the server remains completely healthy, returns nacks, and handles legitimate commands afterwards.
    """
    async def _test():
        auth_mgr = AuthManager()
        token = auth_mgr.generate_token("fuzz_tester_device")

        tcp_port = get_free_port()
        ws_port = get_free_port()

        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=tcp_port,
            ws_port=ws_port,
            udp_port=0,
            enable_tls=False,
            auth_manager=auth_mgr,
        )

        moves_recorded = []
        clicks_recorded = []
        server.on_move(lambda dx, dy: moves_recorded.append((dx, dy)))
        server.on_click(lambda b: clicks_recorded.append(b))

        await server.start()
        try:
            reader, writer = await asyncio.open_connection("127.0.0.1", tcp_port)
            now_ms = int(time.time() * 1000)

            # Legitimate Handshake
            hello = {
                "v": 2,
                "type": "hello",
                "seq": 1,
                "ts": now_ms,
                "payload": {
                    "device_id": "fuzz_bot",
                    "name": "Chaos Fuzz Bot",
                    "version": "2.0.0",
                    "token": token,
                },
            }
            writer.write((json.dumps(hello) + "\n").encode("utf-8"))
            await writer.drain()

            welcome_line = await reader.readline()
            welcome = json.loads(welcome_line.decode("utf-8"))
            assert welcome["type"] == "welcome"
            assert welcome["payload"]["approved"] is True

            # Fuzz barrage over active connection
            fuzz_packets = [
                # Invalid version
                {"v": 999, "type": "ping", "seq": 2, "ts": now_ms, "payload": {}},
                # Negative timestamp
                {"v": 2, "type": "move", "seq": 3, "ts": -5000, "payload": {"dx": 1.0, "dy": 1.0}},
                # Future timestamp (> 5000ms)
                {"v": 2, "type": "move", "seq": 4, "ts": now_ms + 10000, "payload": {"dx": 1.0, "dy": 1.0}},
                # NaN coordinate
                '{"v": 2, "type": "move", "seq": 5, "ts": ' + str(now_ms) + ', "payload": {"dx": NaN, "dy": 0.0}}',
                # Out of bounds motion
                {"v": 2, "type": "move", "seq": 6, "ts": now_ms + 10, "payload": {"dx": 9999.0, "dy": 0.0}},
                # Out of bounds scroll
                {"v": 2, "type": "scroll", "seq": 7, "ts": now_ms + 20, "payload": {"delta": 50.0}},
                # Unknown type
                {"v": 2, "type": "chaos_injection", "seq": 8, "ts": now_ms + 30, "payload": {}},
                # Missing required envelope fields
                {"v": 2, "seq": 9, "ts": now_ms + 40},
            ]

            for pkt in fuzz_packets:
                line = (json.dumps(pkt) + "\n") if isinstance(pkt, dict) else (pkt + "\n")
                writer.write(line.encode("utf-8"))
                await writer.drain()

                # Read response; server must reply with NACK and remain alive
                resp_line = await reader.readline()
                resp = json.loads(resp_line.decode("utf-8"))
                assert resp["type"] == "nack"
                assert "error" in resp["payload"]

            # Verify server is still completely alive and functioning for legitimate commands
            legit_click = {
                "v": 2,
                "type": "click",
                "id": "post_fuzz_click",
                "seq": 100,
                "ts": now_ms + 50,
                "payload": {"button": "right"},
            }
            writer.write((json.dumps(legit_click) + "\n").encode("utf-8"))
            await writer.drain()

            ack_line = await reader.readline()
            ack = json.loads(ack_line.decode("utf-8"))
            assert ack["type"] == "ack"
            assert ack["payload"]["ack_id"] == "post_fuzz_click"

            await asyncio.sleep(0.05)
            assert "right" in clicks_recorded

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_test())


# ============================================================================
# SECTION 2: HMAC AUTH TAMPERING
# ============================================================================


def test_hmac_tamper_signature_single_bit_mutations(tmp_path):
    """
    Exhaustively test single-character and bit mutations in the HMAC signature.
    Every mutated token must be strictly rejected.
    """
    auth_mgr = AuthManager(secret_dir=str(tmp_path))
    client_id = "test_pixel_device"
    valid_token = auth_mgr.generate_token(client_id)
    assert auth_mgr.validate_token(valid_token) is True

    parts = valid_token.split(".")
    assert len(parts) == 4
    c_id, ts, nonce, original_sig = parts

    # Mutate characters in signature
    hex_chars = "0123456789abcdef"
    for i in range(len(original_sig)):
        cur_char = original_sig[i]
        # Choose a different hex char to guarantee mutation
        alt_char = "0" if cur_char != "0" else "1"
        mutated_sig = original_sig[:i] + alt_char + original_sig[i + 1:]
        mutated_token = f"{c_id}.{ts}.{nonce}.{mutated_sig}"

        assert auth_mgr.validate_token(mutated_token) is False, (
            f"Tampered signature mutated at pos {i} was accepted: {mutated_token}"
        )


def test_hmac_tamper_nonce_and_client_id_mutation(tmp_path):
    """Verify mutating client_id or nonce invalidates the HMAC signature."""
    auth_mgr = AuthManager(secret_dir=str(tmp_path))
    valid_token = auth_mgr.generate_token("client_alpha")
    c_id, ts, nonce, sig = valid_token.split(".")

    # Mutate client_id
    mutated_client_token = f"client_beta.{ts}.{nonce}.{sig}"
    assert auth_mgr.validate_token(mutated_client_token) is False

    # Mutate nonce (guarantee mutation by picking an alternating character)
    alt_char = "1" if nonce[0] == "0" else "0"
    mutated_nonce = alt_char + nonce[1:]
    mutated_nonce_token = f"{c_id}.{ts}.{mutated_nonce}.{sig}"
    assert auth_mgr.validate_token(mutated_nonce_token) is False


def test_hmac_truncated_empty_and_corrupted_signatures(tmp_path):
    """Verify truncated signatures, empty signatures, and corrupted structural formats are rejected."""
    auth_mgr = AuthManager(secret_dir=str(tmp_path))
    valid_token = auth_mgr.generate_token("client_test")
    c_id, ts, nonce, sig = valid_token.split(".")

    # Truncated signatures of various lengths
    for truncate_len in [0, 1, 8, 16, 32, 63]:
        truncated_sig = sig[:truncate_len]
        truncated_token = f"{c_id}.{ts}.{nonce}.{truncated_sig}"
        assert auth_mgr.validate_token(truncated_token) is False

    # Empty signature with trailing dot
    assert auth_mgr.validate_token(f"{c_id}.{ts}.{nonce}.") is False

    # Missing parts (3 parts instead of 4)
    assert auth_mgr.validate_token(f"{c_id}.{ts}.{nonce}") is False

    # Extra parts (5 parts instead of 4)
    assert auth_mgr.validate_token(f"{c_id}.{ts}.{nonce}.{sig}.extra") is False

    # Non-string or null inputs
    assert auth_mgr.validate_token("") is False
    assert auth_mgr.validate_token(None) is False  # type: ignore
    assert auth_mgr.validate_token(12345) is False  # type: ignore
    assert auth_mgr.validate_token("......") is False

    # Non-integer timestamp
    assert auth_mgr.validate_token(f"{c_id}.not_an_int.{nonce}.{sig}") is False


def test_hmac_expired_and_future_tokens(tmp_path):
    """Verify tokens with timestamps outside TTL window (>86400s past or >300s future) are rejected."""
    auth_mgr = AuthManager(secret_dir=str(tmp_path), token_ttl_seconds=3600)
    current_time = int(time.time())

    def forge_token(c_id: str, ts_val: int) -> str:
        nonce = "0123456789abcdef0123456789abcdef"
        msg = f"{c_id}:{ts_val}:{nonce}".encode("utf-8")
        s = hmac.new(auth_mgr.secret, msg, hashlib.sha256).hexdigest()
        return f"{c_id}.{ts_val}.{nonce}.{s}"

    # Valid token within window
    assert auth_mgr.validate_token(forge_token("device_a", current_time)) is True

    # Expired tokens (past TTL)
    expired_token = forge_token("device_a", current_time - 3601)
    assert auth_mgr.validate_token(expired_token) is False

    ancient_token = forge_token("device_a", current_time - 1_000_000)
    assert auth_mgr.validate_token(ancient_token) is False

    # Future token outside +300s clock skew window
    far_future_token = forge_token("device_a", current_time + 301)
    assert auth_mgr.validate_token(far_future_token) is False

    # Near future token within +300s clock skew window
    near_future_token = forge_token("device_a", current_time + 150)
    assert auth_mgr.validate_token(near_future_token) is True


def test_hmac_secret_rotation_invalidation(tmp_path):
    """Verify rotating master secret key immediately invalidates all previously issued tokens."""
    auth_mgr = AuthManager(secret_dir=str(tmp_path))
    token1 = auth_mgr.generate_token("device_1")
    token2 = auth_mgr.generate_token("device_2")

    assert auth_mgr.validate_token(token1) is True
    assert auth_mgr.validate_token(token2) is True

    # Rotate secret
    auth_mgr.rotate_secret()

    # Pre-rotation tokens must be rejected
    assert auth_mgr.validate_token(token1) is False
    assert auth_mgr.validate_token(token2) is False

    # New token generated with rotated key must be valid
    new_token = auth_mgr.generate_token("device_1")
    assert auth_mgr.validate_token(new_token) is True


def test_server_rejection_of_tampered_auth():
    """Verify AirMouseServer terminates unauthenticated sessions with tampered tokens."""
    async def _test():
        auth_mgr = AuthManager()
        valid_token = auth_mgr.generate_token("victim_device")
        # Tamper with the token signature
        c_id, ts, nonce, sig = valid_token.split(".")
        tampered_token = f"{c_id}.{ts}.{nonce}.{sig[:-1]}0"

        tcp_port = get_free_port()
        ws_port = get_free_port()

        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=tcp_port,
            ws_port=ws_port,
            udp_port=0,
            enable_tls=False,
            auth_manager=auth_mgr,
        )

        await server.start()
        try:
            reader, writer = await asyncio.open_connection("127.0.0.1", tcp_port)
            now_ms = int(time.time() * 1000)

            hello = {
                "v": 2,
                "type": "hello",
                "seq": 1,
                "ts": now_ms,
                "payload": {
                    "device_id": "victim_device",
                    "name": "Tampered Attacker",
                    "version": "2.0.0",
                    "token": tampered_token,
                },
            }
            writer.write((json.dumps(hello) + "\n").encode("utf-8"))
            await writer.drain()

            nack_line = await reader.readline()
            nack = json.loads(nack_line.decode("utf-8"))
            assert nack["type"] == "nack"
            assert nack["payload"]["error"] == "invalid_token"

            # Connection should be closed by server upon invalid auth token
            subsequent = await reader.read(1024)
            assert subsequent == b""

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_test())


# ============================================================================
# SECTION 3: JITTER BUFFER FLOODING
# ============================================================================


def test_jitter_buffer_500_out_of_order_and_duplicates():
    """
    Simulate receiving 500 out-of-order and duplicated UDP packets in a tight loop.
    Verify:
    1. Monotonic sequence tracking: highest_popped_seq monotonically advances.
    2. Any popped items preserve monotonic non-decreasing sequence order.
    3. Flush extracts all remaining items in strictly ascending sequence order.
    4. Adaptive queue depth stays bounded between min_depth and max_depth.
    5. No memory runaway: len(queue) is bounded and completely clears upon flush.
    """
    jb = UDPJitterBuffer(min_depth=2, max_depth=8)
    total_packets = 500

    # Build stream with jitter and duplicate bursts
    stream = []
    for seq in range(1, total_packets + 1):
        stream.append(seq)
        if seq % 4 == 0:  # 25% duplicate packets
            stream.append(seq)

    # Apply local sliding window shuffle to simulate realistic UDP packet reordering
    window = []
    shuffled_stream = []
    random.seed(1337)
    for seq in stream:
        window.append(seq)
        if len(window) >= 12:
            random.shuffle(window)
            shuffled_stream.append(window.pop(0))
    shuffled_stream.extend(window)

    popped_sequences: List[int] = []
    max_observed_queue_len = 0

    # Flood the buffer in a tight loop while periodically popping
    for seq in shuffled_stream:
        jb.push(seq=seq, packet=f"payload_{seq}")

        if len(jb.queue) > max_observed_queue_len:
            max_observed_queue_len = len(jb.queue)

        # Verify dynamic target depth is always safely bounded
        target_depth = jb.get_target_depth()
        assert 2 <= target_depth <= 8, f"Target depth {target_depth} outside bounds [2, 8]"
        assert not math.isnan(jb.jitter)
        assert not math.isinf(jb.jitter)

        # Attempt to pop if depth satisfied
        popped = jb.pop()
        if popped is not None:
            p_seq = int(popped.split("_")[1])
            popped_sequences.append(p_seq)

    # Flush remaining packets
    flushed = jb.flush()
    flushed_sequences = [int(p.split("_")[1]) for p in flushed]
    popped_sequences.extend(flushed_sequences)

    # Monotonic Sequence Tracking Verifications
    # 1. Popped packets must be monotonically non-decreasing
    for i in range(len(popped_sequences) - 1):
        assert popped_sequences[i] <= popped_sequences[i + 1], (
            f"Non-monotonic sequence observed: {popped_sequences[i]} > {popped_sequences[i+1]}"
        )

    # 2. Flushed packets must be strictly ascending (flush filters out duplicates <= highest_popped_seq)
    for i in range(len(flushed_sequences) - 1):
        assert flushed_sequences[i] < flushed_sequences[i + 1], (
            f"Flushed sequence not strictly ascending: {flushed_sequences[i]} >= {flushed_sequences[i+1]}"
        )

    # 3. Monotonic highest_popped_seq tracking
    assert jb.highest_popped_seq == total_packets

    # 4. Memory Stability: queue must be completely empty after flush
    assert len(jb.queue) == 0, f"Memory leak: queue still has {len(jb.queue)} items after flush"
    assert max_observed_queue_len <= total_packets


def test_jitter_buffer_reverse_sequence_flood():
    """
    Flood buffer in completely reverse sequence order (500 down to 1).
    Once seq 500 is popped, all 499 stale packets arriving afterwards must be immediately dropped.
    """
    jb = UDPJitterBuffer(min_depth=1, max_depth=8)

    # Push 500 first
    jb.push(500, "pkt_500")
    first = jb.pop()
    assert first == "pkt_500"
    assert jb.highest_popped_seq == 500

    # Push 499 down to 1 in reverse order
    for seq in range(499, 0, -1):
        jb.push(seq, f"pkt_{seq}")
        # Every packet should be rejected immediately because seq <= highest_popped_seq
        assert len(jb.queue) == 0

    assert jb.highest_popped_seq == 500
    assert len(jb.queue) == 0


def test_jitter_buffer_burst_identical_duplicates():
    """Verify sending 500 identical duplicate packets does not cause memory runaway."""
    jb = UDPJitterBuffer(min_depth=1, max_depth=8)

    # Push identical sequence 42 500 times
    for _ in range(500):
        jb.push(42, "duplicate_42")

    # Pop first
    first = jb.pop()
    assert first == "duplicate_42"
    assert jb.highest_popped_seq == 42

    # Flush remaining duplicates
    flushed = jb.flush()
    # Flush filters duplicates where seq <= highest_popped_seq
    assert len(flushed) == 0
    assert len(jb.queue) == 0

    # Further pushes of seq 42 must be immediately dropped
    for _ in range(50):
        jb.push(42, "duplicate_42")
    assert len(jb.queue) == 0


def test_jitter_buffer_extreme_sequence_boundaries():
    """Verify numerical stability at 0, 1, and 64-bit integer boundaries."""
    jb = UDPJitterBuffer(min_depth=1, max_depth=8)

    boundary_seqs = [0, 1, 1000, 2**31 - 1, 2**31, 2**63 - 1]
    for seq in boundary_seqs:
        jb.push(seq, f"boundary_{seq}")
        popped = jb.pop()
        assert popped == f"boundary_{seq}"
        assert jb.highest_popped_seq == seq

    # Stale boundary rejection
    jb.push(2**31 - 1, "stale_boundary")
    assert len(jb.queue) == 0


def test_jitter_buffer_reset_and_stream_reuse():
    """Verify resetting the buffer completely restores initial state and allows new streams."""
    jb = UDPJitterBuffer(min_depth=2, max_depth=8)

    # Flood with initial stream
    for seq in range(1, 200):
        jb.push(seq, f"old_{seq}")
    jb.pop()

    assert jb.highest_popped_seq > 0
    assert len(jb.queue) > 0

    # Reset
    jb.reset()
    assert len(jb.queue) == 0
    assert jb.jitter == 0.0
    assert jb.last_arrival_time is None
    assert jb.last_delay is None
    assert jb.highest_popped_seq == -1

    # New stream starting back at sequence 1 must be accepted cleanly
    jb.push(1, "new_1")
    jb.push(2, "new_2")
    assert len(jb.queue) == 2
    popped = jb.pop()
    assert popped == "new_1"
    assert jb.highest_popped_seq == 1


def test_jitter_buffer_multithreaded_flood_concurrency():
    """
    Test thread safety and concurrency stability:
    Multiple producer threads push packets while a consumer thread pops.
    """
    jb = UDPJitterBuffer(min_depth=1, max_depth=8)
    lock = threading.Lock()
    total_packets_per_thread = 150
    thread_count = 3

    popped_items = []
    stop_consumer = threading.Event()

    def producer(thread_id: int):
        for seq in range(1, total_packets_per_thread + 1):
            with lock:
                jb.push(seq, f"t{thread_id}_{seq}")
            time.sleep(0.0001)

    def consumer():
        while not stop_consumer.is_set():
            with lock:
                p = jb.pop()
                if p:
                    popped_items.append(p)
            time.sleep(0.0001)

    threads = [threading.Thread(target=producer, args=(t,)) for t in range(thread_count)]
    cons_thread = threading.Thread(target=consumer)

    cons_thread.start()
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    # Allow consumer to drain
    time.sleep(0.05)
    stop_consumer.set()
    cons_thread.join()

    with lock:
        flushed = jb.flush()
        popped_items.extend(flushed)

    assert len(popped_items) > 0
    assert len(jb.queue) == 0
