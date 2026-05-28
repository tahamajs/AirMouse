#!/usr/bin/env python3
"""
Air Mouse Perfetto Trace Analyzer
Answers the 11 required questions using a Perfetto trace file.
Requires: pip install perfetto pandas
"""

import sys
import pandas as pd
from perfetto.trace_processor import TraceProcessor

def run_query(tp: TraceProcessor, query: str) -> pd.DataFrame:
    try:
        return tp.query(query).as_pandas()
    except Exception as exc:
        return pd.DataFrame({"error": [str(exc)]})

def analyze_trace(trace_file: str) -> dict:
    print(f"Loading trace: {trace_file}")
    tp = TraceProcessor(trace=trace_file)

    results = {}

    # Q1: Sensor callback processing samples emitted by android.os.Trace.
    q1_query = """
        SELECT
            slice.name,
            ROUND(slice.ts / 1e6, 3) AS start_ms,
            ROUND(slice.dur / 1e6, 6) AS dur_ms,
            thread.name AS thread_name
        FROM slice
        JOIN thread_track ON slice.track_id = thread_track.id
        JOIN thread USING(utid)
        WHERE slice.name GLOB 'AirMouseSensor*'
        ORDER BY slice.ts
        LIMIT 30
    """
    results['q1_sensor_delivery_samples'] = run_query(tp, q1_query)

    # Q2: Explanation provided as text – not in trace
    results['q2_answer'] = "Raw sensors have bias, drift, noise. Fusion (Madgwick) combines gyro (fast), accel (gravity reference), mag (yaw reference) to produce drift‑free orientation."

    # Q3: Actual sampling period vs configured
    q3_query = """
        WITH events AS (
            SELECT ts
            FROM slice
            WHERE name = 'AirMouseSensorGyroscope'
            ORDER BY ts
        )
        SELECT
            ROUND((ts - LAG(ts) OVER (ORDER BY ts)) / 1e6, 3) AS period_ms
        FROM events
        LIMIT 50
    """
    results['q3_sampling_periods'] = run_query(tp, q3_query)

    # Q4: Thread contention – use sched_switch events
    q4_query = """
        SELECT
            thread.name AS thread_name,
            ROUND(SUM(thread_state.dur) / 1e6, 3) AS blocked_or_waiting_ms
        FROM thread_state
        JOIN thread USING(utid)
        WHERE thread.name IN ('main', 'AirMouseSensorThread')
          AND thread_state.state != 'Running'
        GROUP BY thread.name
        ORDER BY blocked_or_waiting_ms DESC
    """
    results['q4_thread_waiting'] = run_query(tp, q4_query)

    # Q5: Wake‑up vs non‑wake‑up – text explanation
    results['q5_answer'] = "Wake‑up sensors (e.g., significant motion) wake CPU from suspend; non‑wake‑up deliver events only when CPU already awake. Air Mouse uses non‑wake‑up (screen on)."

    # Q6: CPU time of Madgwick filter
    q6_query = """
        SELECT
            name,
            COUNT(*) AS calls,
            ROUND(AVG(dur) / 1000.0, 3) AS avg_us,
            ROUND(MAX(dur) / 1000.0, 3) AS max_us,
            ROUND(SUM(dur) / 1e6, 3) AS total_ms
        FROM slice
        WHERE name GLOB 'Madgwick*Update' AND dur > 0
        GROUP BY name
        ORDER BY total_ms DESC
    """
    results['q6_filter_cpu_time'] = run_query(tp, q6_query)

    # Q7: Most processing‑intensive sensor (estimated from slice counts)
    q7_query = """
        SELECT
            name,
            COUNT(*) AS count,
            ROUND(AVG(dur) / 1000.0, 3) AS avg_us,
            ROUND(SUM(dur) / 1e6, 3) AS total_ms
        FROM slice
        WHERE name GLOB 'AirMouseSensor*'
        GROUP BY name
        ORDER BY total_ms DESC
    """
    results['q7_sensor_processing_cost'] = run_query(tp, q7_query)

    # Q8: Sampling rate effect – text
    results['q8_answer'] = "Higher sampling rate lowers input latency and makes cursor movement smoother, but increases CPU scheduling, sensor processing, network packets, and battery use. The app normally uses SENSOR_DELAY_GAME and drops to SENSOR_DELAY_NORMAL in low-power/stationary mode."

    # Q9: Latency from sensor to cursor – estimate using ftrace events
    q9_query = """
        SELECT
            name,
            COUNT(*) AS calls,
            ROUND(AVG(dur) / 1000.0, 3) AS avg_us,
            ROUND(MAX(dur) / 1000.0, 3) AS max_us
        FROM slice
        WHERE name IN ('AirMouseOrientation', 'AirMouseNetworkSendMove', 'AirMouseNetworkSendAckCommand')
        GROUP BY name
        ORDER BY name
    """
    results['q9_motion_to_network_slices'] = run_query(tp, q9_query)

    q10_query = """
        SELECT
            thread.name AS thread_name,
            slice.name AS slice_name,
            COUNT(*) AS calls,
            ROUND(SUM(slice.dur) / 1e6, 3) AS total_ms
        FROM slice
        JOIN thread_track ON slice.track_id = thread_track.id
        JOIN thread USING(utid)
        WHERE slice.name GLOB 'AirMouse*' OR slice.name GLOB 'Madgwick*'
        GROUP BY thread.name, slice.name
        ORDER BY thread.name, total_ms DESC
    """
    results['q10_thread_breakdown'] = run_query(tp, q10_query)

    # Q10: Threads and UI separation – text
    results['q10_answer'] = "Sensor events run on the dedicated AirMouseSensorThread HandlerThread. Network I/O runs on Kotlin Dispatchers.IO. View updates are posted back to the main thread with runOnUiThread, so sensor fusion and socket writes do not block UI rendering."

    # Q11: Slow vs sudden movement – text
    results['q11_answer'] = "Sudden movements trigger gesture detection (click/scroll) which adds a network packet; slow movements only send move deltas. No significant difference in CPU load."

    return results

def main():
    if len(sys.argv) < 2:
        print("Usage: python perfetto_analyzer.py <trace.perfetto-trace>")
        sys.exit(1)

    results = analyze_trace(sys.argv[1])

    print("\n" + "="*60)
    print("AIR MOUSE PERFETTO ANALYSIS RESULTS")
    print("="*60)

    for k, v in results.items():
        print(f"\n--- {k} ---")
        if isinstance(v, pd.DataFrame):
            print(v.to_string())
        else:
            print(v)

if __name__ == "__main__":
    main()
