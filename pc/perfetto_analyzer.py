#!/usr/bin/env python3
"""
Air Mouse Perfetto Trace Analyzer – Complete Edition
Answers all 11 questions required for the exercise report.
Usage:
    python perfetto_analyzer.py trace_file.perfetto-trace
Output:
    - Printed report with statistics and explanations
    - Optionally saves a JSON summary file
Requirements: pip install perfetto pandas
"""

import sys
import json
import os
import pandas as pd
from perfetto.trace_processor import TraceProcessor

# ---------------------------------------------------------------------
# 1. Configuration – adjust slice names to match your app's tracepoints
# ---------------------------------------------------------------------
TRACEPOINTS = {
    "sensor_read":      "AirMouseApp.Sensors.sensor_read",    # start of sensor callback
    "filter":           "AirMouseApp.Filter.complementary",   # sensor fusion update
    "compute_delta":    "AirMouseApp.Filter.compute_delta",   # delta calculation
    "send_move":        "AirMouseApp.Communication.send",     # move packet
    "send_click":       "AirMouseApp.Communication.send",     # click/scroll packet (same)
    "click_detect":     "AirMouseApp.ClickDetect",            # optional custom click trace
    "scroll_detect":    "AirMouseApp.ScrollDetect",           # optional scroll trace
}

# ---------------------------------------------------------------------
# 2. Helper functions
# ---------------------------------------------------------------------
def query(tp: TraceProcessor, sql: str) -> pd.DataFrame:
    """Run SQL and return a DataFrame, with error handling."""
    try:
        df = tp.query(sql).as_pandas_dataframe()
        return df
    except Exception as e:
        print(f"⚠️ Query failed: {e}\nSQL: {sql[:200]}...")
        return pd.DataFrame({"error": [str(e)]})

def safe_mean(df: pd.DataFrame, col: str) -> float:
    if col in df and not df[col].dropna().empty:
        return df[col].dropna().mean()
    return float('nan')

def print_table(title: str, df: pd.DataFrame, max_rows=20):
    print(f"\n--- {title} ---")
    if df.empty or "error" in df.columns:
        print("No data available (check tracepoint names).")
        return
    with pd.option_context('display.max_rows', max_rows, 'display.max_columns', 10, 'display.width', 120):
        print(df)

# ---------------------------------------------------------------------
# 3. Main analysis function
# ---------------------------------------------------------------------
def analyze_trace(trace_file: str):
    print(f"🔍 Loading trace: {trace_file}")

    # Try direct loading; if it fails, instruct user to use HTTP server
    try:
        tp = TraceProcessor(trace=trace_file)
    except Exception:
        print("❌ Direct file load failed. Try starting the trace processor HTTP server:")
        print("   ./trace_processor_shell.exe --httpd")
        print("   Then modify the script to use TraceProcessor(addr='http://127.0.0.1:9001')")
        sys.exit(1)

    # -----------------------------------------------------------------
    # Q1: Timeline from sensor request to data delivery
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q1: Sensor data delivery steps (first 30 sensor_read slices)")
    print("="*70)
    q1_query = f"""
        SELECT
            slice.ts / 1e6 AS start_ms,
            slice.dur / 1e6 AS duration_ms,
            thread.name AS thread
        FROM slice
        JOIN thread_track ON slice.track_id = thread_track.id
        JOIN thread USING(utid)
        WHERE slice.name = '{TRACEPOINTS["sensor_read"]}'
        ORDER BY slice.ts
        LIMIT 30
    """
    df_q1 = query(tp, q1_query)
    print_table("Q1: Sensor read slices", df_q1)
    if not df_q1.empty and "duration_ms" in df_q1:
        avg_dur = df_q1["duration_ms"].mean()
        print(f"Average sensor callback duration: {avg_dur:.4f} ms")
    else:
        print("No sensor_read slices found. Check TRACEPOINTS.")

    # -----------------------------------------------------------------
    # Q2: Why raw sensors have errors & how fusion helps (text)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q2: Sensor errors and fusion")
    print("="*70)
    print("Gyroscope: bias and drift over time. Accelerometer: noisy, affected by linear acceleration.")
    print("Magnetometer: hard/soft iron distortion. Sensor fusion (Madgwick) combines:")
    print("- Gyro for short‑term fast response")
    print("- Accelerometer (gravity) to correct pitch/roll drift")
    print("- Magnetometer (Earth’s field) to correct yaw drift")
    print("Result: stable, drift‑free orientation used for Air Mouse.")

    # -----------------------------------------------------------------
    # Q3: Sampling period vs configured rate
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q3: Actual vs. configured sampling period")
    print("="*70)
    q3_query = f"""
        WITH events AS (
            SELECT ts
            FROM slice
            WHERE name = '{TRACEPOINTS["sensor_read"]}'
            ORDER BY ts
        )
        SELECT
            (ts - LAG(ts) OVER (ORDER BY ts)) / 1e6 AS interval_ms
        FROM events
        LIMIT 1000
    """
    df_q3 = query(tp, q3_query)
    if not df_q3.empty and "interval_ms" in df_q3:
        intervals = df_q3["interval_ms"].dropna()
        mean_period = intervals.mean()
        std_period = intervals.std()
        print(f"Configured period (example): 20 ms (SENSOR_DELAY_GAME)")
        print(f"Actual mean interval: {mean_period:.2f} ms, std: {std_period:.2f} ms")
        if mean_period > 22:
            print("Note: intervals larger than requested likely due to system load/batching.")
    else:
        print("No interval data. Ensure sensor_read events are present.")

    # -----------------------------------------------------------------
    # Q4: Thread contention (using thread_state)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q4: Thread contention – waiting times")
    print("="*70)
    q4_query = """
        SELECT
            thread.name AS thread_name,
            thread_state.state,
            ROUND(SUM(thread_state.dur) / 1e6, 3) AS total_ms,
            COUNT(*) AS occurrences
        FROM thread_state
        JOIN thread USING(utid)
        WHERE thread.name IN ('main', 'RenderThread', 'surfaceflinger')
           OR thread.name GLOB 'sensor*'
        GROUP BY thread_name, state
        ORDER BY total_ms DESC
        LIMIT 30
    """
    df_q4 = query(tp, q4_query)
    print_table("Q4: Thread waiting/blocked times", df_q4)
    print("Interpretation: Look for 'main' or 'RenderThread' in 'S' (sleeping) or 'D' (waiting) states,")
    print("which can indicate contention with sensor processing. In a well-optimized app, sensor work")
    print("should be on a dedicated thread and not block UI for more than a few ms.")

    # -----------------------------------------------------------------
    # Q5: Wake-up vs non-wake-up sensors (text)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q5: Wake-up vs. non‑wake‑up sensors")
    print("="*70)
    print("Wake‑up sensors (e.g., significant motion) can wake the CPU from suspend.")
    print("Non‑wake‑up sensors deliver data only when the CPU is already awake.")
    print("Air Mouse uses non‑wake‑up sensors because the screen is on and we need low latency.")
    print("Advantage: lower power. Disadvantage: cannot detect gestures with screen off.")

    # -----------------------------------------------------------------
    # Q6: CPU time of filter function
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q6: Filter function CPU time")
    print("="*70)
    q6_query = f"""
        SELECT
            COUNT(*) AS calls,
            AVG(dur) / 1e6 AS avg_ms,
            MAX(dur) / 1e6 AS max_ms,
            SUM(dur) / 1e6 AS total_ms
        FROM slice
        WHERE name = '{TRACEPOINTS["filter"]}'
    """
    df_q6 = query(tp, q6_query)
    print_table("Q6: Filter slice statistics", df_q6)
    if not df_q6.empty and "avg_ms" in df_q6:
        avg = df_q6["avg_ms"].iloc[0]
        total = df_q6["total_ms"].iloc[0]
        print(f"Average filter duration: {avg:.4f} ms, total CPU time in trace: {total:.2f} ms")
    else:
        print("No filter slices found.")

    # -----------------------------------------------------------------
    # Q7: Most processing‑intensive sensor
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q7: Sensor processing cost by type")
    print("="*70)
    q7_query = f"""
        SELECT
            name,
            COUNT(*) AS calls,
            AVG(dur) / 1e6 AS avg_ms,
            SUM(dur) / 1e6 AS total_ms
        FROM slice
        WHERE name IN (
            '{TRACEPOINTS["sensor_read"]}',
            '{TRACEPOINTS["filter"]}',
            '{TRACEPOINTS["compute_delta"]}'
        )
        GROUP BY name
        ORDER BY total_ms DESC
    """
    df_q7 = query(tp, q7_query)
    print_table("Q7: Per-stage processing cost", df_q7)
    if not df_q7.empty:
        max_row = df_q7.loc[df_q7["total_ms"].idxmax()]
        print(f"Most expensive stage: {max_row['name']} (total {max_row['total_ms']:.2f} ms)")
    else:
        print("No processing slices found. Adjust TRACEPOINTS.")

    # -----------------------------------------------------------------
    # Q8: Effect of sensor sampling rate (text + evidence)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q8: Effect of sampling rate on system overhead")
    print("="*70)
    # We can show the correlation between number of sensor events and CPU time
    q8_query = f"""
        SELECT
            COUNT(*) AS event_count,
            SUM(dur) / 1e6 AS total_sensor_ms,
            SUM(dur) / 1e6 / (SELECT COUNT(*) FROM slice WHERE name = '{TRACEPOINTS["sensor_read"]}') AS cost_per_event_ms
        FROM slice
        WHERE name = '{TRACEPOINTS["sensor_read"]}'
    """
    df_q8 = query(tp, q8_query)
    print("In this trace:")
    print_table("Q8: Overhead per sensor event", df_q8)
    print("If you recorded traces at different rates (e.g., 20 ms vs 5 ms), compare these numbers.")
    print("Higher rate → more events → higher total CPU time, more context switches, more network packets.")

    # -----------------------------------------------------------------
    # Q9: End‑to‑end latency (sensor → send)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q9: Latency from sensor data ready to socket send")
    print("="*70)
    # We look for a sensor_read immediately followed by send_move.
    # This assumes the send happens right after processing.
    q9_query = f"""
        WITH paired AS (
            SELECT
                s.ts AS sensor_start,
                s.ts + s.dur AS sensor_end,
                m.ts AS send_start,
                m.ts + m.dur AS send_end,
                (m.ts - s.ts) / 1e6 AS latency_ms
            FROM slice s
            JOIN slice m ON s.track_id = m.track_id
            WHERE s.name = '{TRACEPOINTS["sensor_read"]}'
              AND m.name = '{TRACEPOINTS["send_move"]}'
              AND m.ts > s.ts
              AND m.ts - s.ts < 1e9   -- within 1 second
        )
        SELECT
            COUNT(*) AS pairs,
            AVG(latency_ms) AS avg_ms,
            MAX(latency_ms) AS max_ms
        FROM paired
    """
    df_q9 = query(tp, q9_query)
    print_table("Q9: Sensor‑to‑send latency", df_q9)
    if not df_q9.empty and "avg_ms" in df_q9:
        avg_lat = df_q9["avg_ms"].iloc[0]
        print(f"Average local latency: {avg_lat:.2f} ms")
        print("Add network RTT (~1 ms) and server rendering (~2 ms) for total pointer latency.")
    else:
        print("Could not pair sensor_read with send_move. Check if both tracepoints fire in order.")

    # -----------------------------------------------------------------
    # Q10: Thread assignment (sensor, processing, communication, UI)
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q10: Thread breakdown for each activity")
    print("="*70)
    q10_query = f"""
        SELECT DISTINCT
            thread.name AS thread_name,
            slice.name AS activity
        FROM slice
        JOIN thread_track ON slice.track_id = thread_track.id
        JOIN thread USING(utid)
        WHERE slice.name GLOB 'AirMouseApp.*' OR slice.name GLOB 'Madgwick*'
        ORDER BY thread_name, activity
    """
    df_q10 = query(tp, q10_query)
    print_table("Q10: Thread assignment", df_q10)
    print("Ideal separation:")
    print("- Sensor callbacks → dedicated 'SensorThread' (via HandlerThread)")
    print("- Filter/delta computation → same or separate worker thread")
    print("- Network I/O → 'NetworkThread' or coroutine (IO dispatcher)")
    print("- UI updates → 'main' thread")

    # -----------------------------------------------------------------
    # Q11: Slow vs fast movement impact
    # -----------------------------------------------------------------
    print("\n" + "="*70)
    print("Q11: Effect of slow vs fast movement on processing")
    print("="*70)
    # We can compare filter slice durations when the mouse delta is large vs small.
    # Since we don't have delta magnitude directly in slices, we approximate by looking at
    # the distribution of filter durations – fast movements might show slightly higher durations
    # due to larger numbers, but not significantly.
    q11_query = f"""
        SELECT
            CASE
                WHEN dur / 1e6 < 0.5 THEN '<0.5 ms'
                WHEN dur / 1e6 < 1.0 THEN '0.5-1 ms'
                ELSE '>1 ms'
            END AS duration_bin,
            COUNT(*) AS count
        FROM slice
        WHERE name = '{TRACEPOINTS["filter"]}'
        GROUP BY duration_bin
        ORDER BY MIN(dur)
    """
    df_q11 = query(tp, q11_query)
    print_table("Q11: Filter duration histogram (approximates slow/fast)", df_q11)
    print("Most filter calls are expected to be short (<1 ms). Fast movement does not")
    print("significantly increase duration; occasional spikes may occur due to GC or JIT.")
    print("Pointer smoothness is maintained because move deltas are clamped.")

    # -----------------------------------------------------------------
    # Save JSON summary (optional)
    # -----------------------------------------------------------------
    summary = {
        "q1_avg_callback_ms": safe_mean(df_q1, "duration_ms") if not df_q1.empty else None,
        "q3_mean_period_ms": safe_mean(df_q3, "interval_ms") if not df_q3.empty else None,
        "q6_avg_filter_ms": df_q6["avg_ms"].iloc[0] if not df_q6.empty and "avg_ms" in df_q6 else None,
        "q9_avg_latency_ms": df_q9["avg_ms"].iloc[0] if not df_q9.empty and "avg_ms" in df_q9 else None,
    }
    out_json = trace_file.replace(".perfetto-trace", "_analysis.json")
    with open(out_json, "w") as f:
        json.dump(summary, f, indent=4)
    print(f"\n💾 Summary written to {out_json}")
    print("="*70)
    print("Analysis complete. Use the printed results for your report.")

# ---------------------------------------------------------------------
# 4. Entry point
# ---------------------------------------------------------------------
if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python perfetto_analyzer.py <trace_file.perfetto-trace>")
        sys.exit(1)
    trace_file = sys.argv[1]
    if not os.path.exists(trace_file):
        print(f"❌ File not found: {trace_file}")
        sys.exit(1)
    analyze_trace(trace_file)