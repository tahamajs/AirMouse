package protocol

import (
	"bytes"
	"encoding/json"
	"fmt"
	"math"
	"math/rand"
	"strings"
	"sync"
	"testing"
	"time"
)

// ============================================================================
// 1. FUZZ ENVELOPE DECODING
// ============================================================================

// TestFuzzCorruptedByteBuffers feeds pseudo-random byte buffers of varied lengths
// to both DecodeWireMessageStrictV2 and DecodeWireMessage. Decoders must never panic
// and must cleanly return errors on invalid input.
func TestFuzzCorruptedByteBuffers(t *testing.T) {
	lengths := []int{0, 1, 2, 3, 7, 15, 31, 64, 128, 256, 512, 1024, 2048, 4096}
	r := rand.New(rand.NewSource(1337))

	for _, length := range lengths {
		for iter := 0; iter < 200; iter++ {
			buf := make([]byte, length)
			r.Read(buf)

			func() {
				defer func() {
					if rec := recover(); rec != nil {
						t.Fatalf("PANIC on random byte buffer (len=%d, iter=%d): %v", length, iter, rec)
					}
				}()

				// 1. Strict V2 decoding
				env, err := DecodeWireMessageStrictV2(buf)
				if err == nil && env != nil {
					// If random bytes somehow formed valid JSON, envelope invariants must hold
					if env.V != ProtocolVersion {
						t.Errorf("Unexpected version accepted from random bytes: %d", env.V)
					}
					if env.Type == "" || !IsValidType(env.Type) {
						t.Errorf("Invalid type accepted from random bytes: %s", env.Type)
					}
				}

				// 2. Legacy decoding
				_, _, _, _ = DecodeWireMessage(buf)
			}()
		}
	}
}

// TestFuzzTruncatedJSON takes valid protocol messages and slices them at every byte offset,
// verifying that partial/truncated JSON streams return clean parse errors without panic.
func TestFuzzTruncatedJSON(t *testing.T) {
	validPackets := []string{
		`{"v":2,"type":"move","seq":101,"ts":1740000000000,"id":"act-1","payload":{"dx":12.5,"dy":-4.0}}`,
		`{"v":2,"type":"click","seq":102,"ts":1740000000001,"payload":{"button":"left"}}`,
		`{"v":2,"type":"scroll","seq":103,"ts":1740000000002,"payload":{"delta":3.0}}`,
		`{"v":2,"type":"hello","seq":104,"ts":1740000000003,"payload":{"name":"Phone","version":"2.0.0","token":"tok_123"}}`,
		`{"v":2,"type":"ping","seq":105,"ts":1740000000004,"payload":{}}`,
		`{"v":2,"type":"welcome","seq":106,"ts":1740000000005,"payload":{"server":"test","version":"2.0","approved":true}}`,
		`{"v":2,"type":"ack","seq":107,"ts":1740000000006,"payload":{"ack_id":"req-99","status":"ok"}}`,
	}

	for _, packet := range validPackets {
		b := []byte(packet)
		for cut := 0; cut < len(b); cut++ {
			truncated := b[:cut]

			func() {
				defer func() {
					if rec := recover(); rec != nil {
						t.Fatalf("PANIC on truncated JSON at cut %d/%d (data=%q): %v", cut, len(b), string(truncated), rec)
					}
				}()

				_, errStrict := DecodeWireMessageStrictV2(truncated)
				if errStrict == nil {
					t.Fatalf("Expected error on truncated JSON prefix at cut %d/%d, got nil", cut, len(b))
				}

				_, _, _, _ = DecodeWireMessage(truncated)
			}()
		}
	}
}

// TestFuzzDeeplyNestedJSON tests deeply nested objects and arrays up to 500 levels deep.
// Verifies that deep structures do not exhaust stack frames or trigger runtime panics.
func TestFuzzDeeplyNestedJSON(t *testing.T) {
	depths := []int{10, 50, 100, 200, 500}

	for _, depth := range depths {
		// Nested objects: {"a":{"a":...{}}}}
		var objBuf bytes.Buffer
		for i := 0; i < depth; i++ {
			objBuf.WriteString(`{"a":`)
		}
		objBuf.WriteString(`"bottom"`)
		for i := 0; i < depth; i++ {
			objBuf.WriteString(`}`)
		}

		func() {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on deeply nested object JSON (depth=%d): %v", depth, rec)
				}
			}()
			_, _ = DecodeWireMessageStrictV2(objBuf.Bytes())
			_, _, _, _ = DecodeWireMessage(objBuf.Bytes())
		}()

		// Nested arrays: [[[[...]]]]
		var arrBuf bytes.Buffer
		for i := 0; i < depth; i++ {
			arrBuf.WriteString(`[`)
		}
		arrBuf.WriteString(`1`)
		for i := 0; i < depth; i++ {
			arrBuf.WriteString(`]`)
		}

		func() {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on deeply nested array JSON (depth=%d): %v", depth, rec)
				}
			}()
			_, _ = DecodeWireMessageStrictV2(arrBuf.Bytes())
			_, _, _, _ = DecodeWireMessage(arrBuf.Bytes())
		}()

		// Deep payload nested in valid envelope:
		// {"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"deep":{...}}}
		var envBuf bytes.Buffer
		envBuf.WriteString(`{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":`)
		for i := 0; i < depth; i++ {
			envBuf.WriteString(`{"level":`)
		}
		envBuf.WriteString(`{"dx":10,"dy":10}`)
		for i := 0; i < depth; i++ {
			envBuf.WriteString(`}`)
		}
		envBuf.WriteString(`}`)

		func() {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on deeply nested envelope payload (depth=%d): %v", depth, rec)
				}
			}()
			env, err := DecodeWireMessageStrictV2(envBuf.Bytes())
			if err == nil && env != nil {
				// Move payload should fail because MovePayloadV2 disallows unknown nested fields
				_, errMove := DecodeMovePayloadV2(env.Payload)
				if errMove == nil {
					t.Errorf("Expected MovePayloadV2 to reject deeply nested arbitrary fields at depth=%d", depth)
				}
			}
		}()
	}
}

// TestFuzzExtremelyLargeIntegersAndFloats tests numbers that exceed standard bounds,
// including 100-digit integers, negative sequence numbers, NaN/Inf exponents, and subnormal floats.
func TestFuzzExtremelyLargeIntegersAndFloats(t *testing.T) {
	testCases := []struct {
		name string
		json string
	}{
		{
			name: "100-digit sequence number",
			json: `{"v":2,"type":"move","seq":9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999,"ts":1740000000000,"payload":{"dx":1,"dy":1}}`,
		},
		{
			name: "Negative sequence number",
			json: `{"v":2,"type":"move","seq":-1,"ts":1740000000000,"payload":{"dx":1,"dy":1}}`,
		},
		{
			name: "100-digit timestamp",
			json: `{"v":2,"type":"move","seq":1,"ts":9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999,"payload":{"dx":1,"dy":1}}`,
		},
		{
			name: "Negative timestamp",
			json: `{"v":2,"type":"move","seq":1,"ts":-1740000000000,"payload":{"dx":1,"dy":1}}`,
		},
		{
			name: "Huge version number",
			json: `{"v":999999999999999999999999999999999999,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1,"dy":1}}`,
		},
		{
			name: "Huge float exponent 1e309 in payload dx",
			json: `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1e309,"dy":1}}`,
		},
		{
			name: "Huge float exponent -1e309 in payload dy",
			json: `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1,"dy":-1e309}}`,
		},
		{
			name: "Subnormal float 1e-324 in payload dx",
			json: `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1e-324,"dy":0.0}}`,
		},
		{
			name: "Huge float exponent 1e999 in payload",
			json: `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1e999,"dy":1}}`,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on case %q: %v", tc.name, rec)
				}
			}()

			env, errStrict := DecodeWireMessageStrictV2([]byte(tc.json))
			if errStrict == nil && env != nil {
				// If envelope decoded, validating payload or replay must reject extreme values
				_, errPayload := DecodeMovePayloadV2(env.Payload)
				errReplay := ValidateTimestamp(env.Ts, time.Now().UnixMilli())
				if errPayload == nil && errReplay == nil {
					t.Errorf("Expected either payload or replay rejection for %q, but both passed", tc.name)
				}
			}

			_, _, _, _ = DecodeWireMessage([]byte(tc.json))
		})
	}
}

// TestFuzzNonUTF8Bytes tests non-UTF-8 sequences, raw binary characters, overlong encodings,
// and lone surrogate bytes embedded at various locations in JSON envelopes.
func TestFuzzNonUTF8Bytes(t *testing.T) {
	badByteSequences := [][]byte{
		{0xFF},
		{0xFE},
		{0xC0, 0xAF},             // Overlong UTF-8
		{0xE0, 0x80, 0xAF},       // Overlong 3-byte
		{0xF4, 0x90, 0x80, 0x80}, // Out of Unicode range (> 0x10FFFF)
		{0xED, 0xA0, 0x80},       // UTF-16 surrogate half
		{0x80, 0x81, 0x82},       // Stray continuation bytes
		{0x00},                   // Null byte
	}

	template := `{"v":2,"type":"move","seq":10,"ts":1740000000000,"id":"id_test","payload":{"dx":10.0,"dy":5.0}}`
	baseBytes := []byte(template)

	for idx, badSeq := range badByteSequences {
		// 1. Raw buffer prefixed/postfixed with bad bytes
		mutated := append(append([]byte{}, badSeq...), baseBytes...)
		func() {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on bad prefix seq #%d: %v", idx, rec)
				}
			}()
			_, _ = DecodeWireMessageStrictV2(mutated)
			_, _, _, _ = DecodeWireMessage(mutated)
		}()

		// 2. Injected into string values (id, type)
		injectedJSON := fmt.Sprintf(`{"v":2,"type":"move","seq":10,"ts":1740000000000,"id":"%s","payload":{"dx":10,"dy":10}}`, string(badSeq))
		func() {
			defer func() {
				if rec := recover(); rec != nil {
					t.Fatalf("PANIC on injected string value seq #%d: %v", idx, rec)
				}
			}()
			_, _ = DecodeWireMessageStrictV2([]byte(injectedJSON))
			_, _, _, _ = DecodeWireMessage([]byte(injectedJSON))
		}()

		// 3. Byte substitution at random offsets
		r := rand.New(rand.NewSource(int64(idx * 7919)))
		for rep := 0; rep < 50; rep++ {
			corrupted := make([]byte, len(baseBytes))
			copy(corrupted, baseBytes)
			pos := r.Intn(len(corrupted))
			corrupted[pos] = badSeq[0]

			func() {
				defer func() {
					if rec := recover(); rec != nil {
						t.Fatalf("PANIC on byte substitution pos=%d, rep=%d: %v", pos, rep, rec)
					}
				}()
				_, _ = DecodeWireMessageStrictV2(corrupted)
				_, _, _, _ = DecodeWireMessage(corrupted)
			}()
		}
	}
}

// ============================================================================
// 2. BOUNDS & SECURITY ENFORCEMENT
// ============================================================================

// TestBoundsAndSecurityEnforcement_MoveCoordinates verifies that movements with
// |dx| > 500.0 or |dy| > 500.0 are strictly rejected, while boundary values
// (|dx| <= 500.0, |dy| <= 500.0) are correctly accepted.
func TestBoundsAndSecurityEnforcement_MoveCoordinates(t *testing.T) {
	validBoundaries := []struct {
		dx float64
		dy float64
	}{
		{0.0, 0.0},
		{500.0, 500.0},
		{-500.0, -500.0},
		{500.0, -500.0},
		{-500.0, 500.0},
		{499.9999, -499.9999},
		{-0.0001, 0.0001},
	}

	for _, tc := range validBoundaries {
		raw := json.RawMessage(fmt.Sprintf(`{"dx":%f,"dy":%f}`, tc.dx, tc.dy))
		p, err := DecodeMovePayloadV2(raw)
		if err != nil {
			t.Errorf("Expected valid boundary (%.4f, %.4f) to PASS, got error: %v", tc.dx, tc.dy, err)
		} else if p.DX != tc.dx || p.DY != tc.dy {
			t.Errorf("Boundary value mismatch: expected (%.4f, %.4f), got (%.4f, %.4f)", tc.dx, tc.dy, p.DX, p.DY)
		}

		// Also check legacy decoder
		legacyMap := map[string]any{"dx": tc.dx, "dy": tc.dy}
		legDx, legDy, legErr := DecodeMovePayload(legacyMap)
		if legErr != nil {
			t.Errorf("Legacy DecodeMovePayload rejected valid boundary (%.4f, %.4f): %v", tc.dx, tc.dy, legErr)
		} else if legDx != tc.dx || legDy != tc.dy {
			t.Errorf("Legacy value mismatch: expected (%.4f, %.4f), got (%.4f, %.4f)", tc.dx, tc.dy, legDx, legDy)
		}
	}

	invalidOutOfBounds := []struct {
		name string
		dx   float64
		dy   float64
	}{
		{"dx just above 500", 500.0001, 0.0},
		{"dx just below -500", -500.0001, 0.0},
		{"dy just above 500", 0.0, 500.0001},
		{"dy just below -500", 0.0, -500.0001},
		{"dx large positive", 501.0, 0.0},
		{"dy large positive", 0.0, 501.0},
		{"dx large negative", -1000.0, 0.0},
		{"dy large negative", 0.0, -1000.0},
		{"extreme positive", 1e6, 1e6},
		{"extreme negative", -1e9, -1e9},
		{"near float max", math.MaxFloat64 / 2.0, 0.0},
	}

	for _, tc := range invalidOutOfBounds {
		t.Run(tc.name, func(t *testing.T) {
			raw := json.RawMessage(fmt.Sprintf(`{"dx":%f,"dy":%f}`, tc.dx, tc.dy))
			_, err := DecodeMovePayloadV2(raw)
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: DecodeMovePayloadV2 accepted out-of-bounds (%f, %f)", tc.dx, tc.dy)
			}
			if !strings.Contains(err.Error(), "out of bounds") {
				t.Errorf("Expected 'out of bounds' in error, got: %v", err)
			}

			// Also verify legacy DecodeMovePayload
			legacyMap := map[string]any{"dx": tc.dx, "dy": tc.dy}
			_, _, legErr := DecodeMovePayload(legacyMap)
			if legErr == nil {
				t.Fatalf("SECURITY VIOLATION: DecodeMovePayload accepted out-of-bounds (%f, %f)", tc.dx, tc.dy)
			}
		})
	}
}

// TestBoundsAndSecurityEnforcement_SpecialFloats injects NaN, +Inf, -Inf, and invalid dt values.
// All decoders must safely reject these values without crashing.
func TestBoundsAndSecurityEnforcement_SpecialFloats(t *testing.T) {
	specialFloats := []struct {
		name string
		val  float64
	}{
		{"NaN", math.NaN()},
		{"+Inf", math.Inf(1)},
		{"-Inf", math.Inf(-1)},
	}

	for _, sf := range specialFloats {
		t.Run("Move_DX_"+sf.name, func(t *testing.T) {
			m := map[string]any{"dx": sf.val, "dy": 10.0}
			_, _, err := DecodeMovePayload(m)
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: DecodeMovePayload accepted %s for dx", sf.name)
			}
		})

		t.Run("Move_DY_"+sf.name, func(t *testing.T) {
			m := map[string]any{"dx": 10.0, "dy": sf.val}
			_, _, err := DecodeMovePayload(m)
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: DecodeMovePayload accepted %s for dy", sf.name)
			}
		})

		t.Run("Scroll_"+sf.name, func(t *testing.T) {
			m := map[string]any{"delta": sf.val}
			_, err := DecodeScrollPayload(m)
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: DecodeScrollPayload accepted %s for delta", sf.name)
			}
		})
	}

	// MovePayloadV2 dt validation
	invalidDTs := []struct {
		name string
		json string
	}{
		{"negative dt", `{"dx":10.0,"dy":10.0,"dt":-0.016}`},
		{"negative large dt", `{"dx":10.0,"dy":10.0,"dt":-500.0}`},
	}

	for _, idt := range invalidDTs {
		t.Run(idt.name, func(t *testing.T) {
			_, err := DecodeMovePayloadV2(json.RawMessage(idt.json))
			if err == nil {
				t.Fatalf("Expected negative dt to be rejected: %s", idt.json)
			}
		})
	}
}

// TestBoundsAndSecurityEnforcement_ScrollLimits verifies that scroll deltas
// exceeding |delta| > 10.0 are rejected while |delta| <= 10.0 are accepted.
func TestBoundsAndSecurityEnforcement_ScrollLimits(t *testing.T) {
	validDeltas := []float64{0.0, 1.0, -1.0, 5.5, -5.5, 10.0, -10.0}
	for _, d := range validDeltas {
		raw := json.RawMessage(fmt.Sprintf(`{"delta":%f}`, d))
		p, err := DecodeScrollPayloadV2(raw)
		if err != nil {
			t.Errorf("Expected valid scroll delta %.2f to PASS, got: %v", d, err)
		} else if p.Delta != d {
			t.Errorf("Expected delta %.2f, got %.2f", d, p.Delta)
		}

		// Legacy map decoder
		leg, legErr := DecodeScrollPayload(map[string]any{"delta": d})
		if legErr != nil {
			t.Errorf("Legacy DecodeScrollPayload rejected valid delta %.2f: %v", d, legErr)
		} else if leg != int(d) {
			t.Errorf("Legacy delta expected %d, got %d", int(d), leg)
		}
	}

	invalidDeltas := []float64{10.0001, -10.0001, 10.5, -10.5, 11.0, -15.0, 100.0, -500.0}
	for _, d := range invalidDeltas {
		raw := json.RawMessage(fmt.Sprintf(`{"delta":%f}`, d))
		_, err := DecodeScrollPayloadV2(raw)
		if err == nil {
			t.Fatalf("SECURITY VIOLATION: DecodeScrollPayloadV2 accepted out-of-bounds scroll delta: %f", d)
		}

		_, legErr := DecodeScrollPayload(map[string]any{"delta": d})
		if legErr == nil {
			t.Fatalf("SECURITY VIOLATION: DecodeScrollPayload accepted out-of-bounds scroll delta: %f", d)
		}
	}
}

// TestBoundsAndSecurityEnforcement_TimestampDriftAndReplay verifies sliding timestamp window
// (+/- 5000ms). Packets outside this window (> 5000ms drift) must be flagged as replay.
func TestBoundsAndSecurityEnforcement_TimestampDriftAndReplay(t *testing.T) {
	nowMs := int64(1740000000000)

	validCases := []struct {
		name string
		ts   int64
	}{
		{"exact now", nowMs},
		{"past drift 1000ms", nowMs - 1000},
		{"future drift 1000ms", nowMs + 1000},
		{"past exact boundary 5000ms", nowMs - ReplayWindowMs},
		{"future exact boundary 5000ms", nowMs + ReplayWindowMs},
	}

	for _, vc := range validCases {
		t.Run("Valid_"+vc.name, func(t *testing.T) {
			if err := ValidateTimestamp(vc.ts, nowMs); err != nil {
				t.Errorf("Expected valid timestamp (ts=%d, now=%d) to pass: %v", vc.ts, nowMs, err)
			}
		})
	}

	invalidCases := []struct {
		name string
		ts   int64
	}{
		{"past drift 5001ms (1ms expired)", nowMs - ReplayWindowMs - 1},
		{"future drift 5001ms (1ms future skew)", nowMs + ReplayWindowMs + 1},
		{"past drift 10000ms", nowMs - 10000},
		{"future drift 10000ms", nowMs + 10000},
		{"timestamp 0", 0},
		{"negative timestamp", -1000},
		{"large negative timestamp", -1740000000000},
		{"math.MaxInt64", math.MaxInt64},
		{"math.MinInt64 (overflow check)", math.MinInt64},
	}

	for _, ic := range invalidCases {
		t.Run("Invalid_"+ic.name, func(t *testing.T) {
			err := ValidateTimestamp(ic.ts, nowMs)
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: ValidateTimestamp accepted out-of-window timestamp (ts=%d, now=%d)", ic.ts, nowMs)
			}
			if !strings.Contains(err.Error(), "replay_rejected") {
				t.Errorf("Expected 'replay_rejected' in error, got: %v", err)
			}
		})
	}
}

// TestReplayDetectorMonotonicSequenceChaos verifies strictly monotonic sequence numbers,
// replay detection, duplicate sequence rejection, and reset behavior.
func TestReplayDetectorMonotonicSequenceChaos(t *testing.T) {
	rd := NewReplayDetector()

	// Initial baseline sequence
	if err := rd.CheckAndAdvanceSeq(500); err != nil {
		t.Fatalf("Failed initial sequence 500: %v", err)
	}

	// 1. Duplicate sequence must be rejected
	if err := rd.CheckAndAdvanceSeq(500); err == nil {
		t.Fatalf("Expected duplicate sequence 500 to be rejected as replay")
	}

	// 2. Decreasing sequence must be rejected
	if err := rd.CheckAndAdvanceSeq(499); err == nil {
		t.Fatalf("Expected decreasing sequence 499 to be rejected as replay")
	}
	if err := rd.CheckAndAdvanceSeq(0); err == nil {
		t.Fatalf("Expected sequence 0 to be rejected as replay")
	}

	// 3. Strictly increasing sequences must succeed
	increasing := []uint64{501, 502, 550, 1000, 10000, 99999999}
	for _, seq := range increasing {
		if err := rd.CheckAndAdvanceSeq(seq); err != nil {
			t.Errorf("Expected sequence %d to be accepted: %v", seq, err)
		}
	}

	if rd.LastSeq() != 99999999 {
		t.Errorf("Expected LastSeq 99999999, got %d", rd.LastSeq())
	}

	// 4. Reset resets baseline
	rd.Reset()
	if err := rd.CheckAndAdvanceSeq(10); err != nil {
		t.Fatalf("Failed to establish new baseline after reset: %v", err)
	}
	if rd.LastSeq() != 10 {
		t.Errorf("Expected LastSeq 10 after reset, got %d", rd.LastSeq())
	}
}

// TestReplayDetectorConcurrentRace verifies thread-safety of ReplayDetector
// under concurrent execution with 50 goroutines.
func TestReplayDetectorConcurrentRace(t *testing.T) {
	rd := NewReplayDetector()
	nowMs := time.Now().UnixMilli()

	var wg sync.WaitGroup
	workers := 50
	perWorker := 200

	for w := 0; w < workers; w++ {
		wg.Add(1)
		go func(workerID int) {
			defer wg.Done()
			for i := 0; i < perWorker; i++ {
				seq := uint64((workerID * perWorker) + i + 1)
				env := &EnvelopeV2{
					V:       ProtocolVersion,
					Type:    "move",
					Seq:     seq,
					Ts:      nowMs,
					Payload: json.RawMessage(`{"dx":1,"dy":1}`),
				}
				// Call ValidateMessage concurrently
				_ = rd.ValidateMessage(env, nowMs)
				_ = rd.LastSeq()
			}
		}(w)
	}

	wg.Wait()
}

// TestStrictV2DisallowUnknownFieldsAllPayloads verifies that unknown fields injected
// into any payload struct (Move, Click, Scroll, Hello, Key) are rejected by DisallowUnknownFields.
func TestStrictV2DisallowUnknownFieldsAllPayloads(t *testing.T) {
	testCases := []struct {
		name string
		test func() error
	}{
		{
			name: "MovePayload unknown field",
			test: func() error {
				_, err := DecodeMovePayloadV2(json.RawMessage(`{"dx":10,"dy":10,"malicious":"exploit"}`))
				return err
			},
		},
		{
			name: "ClickPayload unknown field",
			test: func() error {
				_, err := DecodeClickPayloadV2(json.RawMessage(`{"button":"left","unauthorized":true}`))
				return err
			},
		},
		{
			name: "ScrollPayload unknown field",
			test: func() error {
				_, err := DecodeScrollPayloadV2(json.RawMessage(`{"delta":2.0,"extra_arg":123}`))
				return err
			},
		},
		{
			name: "HelloPayload unknown field",
			test: func() error {
				_, err := DecodeHelloPayloadV2(json.RawMessage(`{"name":"Phone","injected_field":"bad"}`))
				return err
			},
		},
		{
			name: "KeyPayload unknown field",
			test: func() error {
				_, err := DecodeKeyPayloadV2(json.RawMessage(`{"key":"Enter","root_access":true}`))
				return err
			},
		},
		{
			name: "EnvelopeV2 unknown field",
			test: func() error {
				raw := `{"v":2,"type":"move","seq":1,"ts":1740000000000,"unauthorized":true,"payload":{"dx":1,"dy":1}}`
				_, err := DecodeWireMessageStrictV2([]byte(raw))
				return err
			},
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			err := tc.test()
			if err == nil {
				t.Fatalf("SECURITY VIOLATION: %s accepted unknown field", tc.name)
			}
			if !strings.Contains(err.Error(), "unknown field") {
				t.Errorf("Expected error to mention 'unknown field', got: %v", err)
			}
		})
	}
}

// TestTrailingGarbageRejection verifies that trailing data or multiple concatenated
// JSON messages without proper framing are rejected by DecodeWireMessageStrictV2.
func TestTrailingGarbageRejection(t *testing.T) {
	cases := []struct {
		name string
		raw  string
	}{
		{
			name: "concatenated JSON messages",
			raw:  `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1,"dy":1}}{"v":2,"type":"move","seq":2,"ts":1740000000000,"payload":{"dx":2,"dy":2}}`,
		},
		{
			name: "trailing text garbage",
			raw:  `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1,"dy":1}} EXTRA_TRAILING_DATA`,
		},
		{
			name: "trailing non-whitespace tokens",
			raw:  `{"v":2,"type":"move","seq":1,"ts":1740000000000,"payload":{"dx":1,"dy":1}} 12345`,
		},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := DecodeWireMessageStrictV2([]byte(tc.raw))
			if err == nil {
				t.Fatalf("Expected trailing data rejection for %s, but got nil", tc.name)
			}
			if !strings.Contains(err.Error(), "trailing data") {
				t.Errorf("Expected 'trailing data' in error, got: %v", err)
			}
		})
	}
}
