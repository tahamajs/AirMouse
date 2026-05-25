#!/usr/bin/env python3
# Automatically answer the 11 Air Mouse questions from a Perfetto trace
import sys
from perfetto.trace_processor import TraceProcessor

def analyze_trace(trace_file):
    tp = TraceProcessor(trace=trace_file)
    # Q1: sensor events from request to delivery
    q1 = tp.query("SELECT ts, dur, name FROM slice WHERE name GLOB '*sensor*' LIMIT 5").as_pandas()
    # Q3: sampling periods
    q3 = tp.query("SELECT ts, value FROM counter WHERE name = 'sensor_sampling_period_ns' LIMIT 10").as_pandas()
    # Q6: CPU time of filter function (assuming we instrumented)
    q6 = tp.query("SELECT SUM(dur) FROM slice WHERE name GLOB '*Madgwick*'").as_pandas()
    # ... more queries
    print("Q1 sample:", q1)
    print("Q3 sample:", q3)
    print("Q6 total filter CPU time (ns):", q6.iloc[0,0])
    # Return answers as dict
    return {"q1": q1.to_dict(), "q3": q3.to_dict(), "q6": q6.iloc[0,0]}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python perfetto_analyzer.py <trace.perfetto-trace>")
        sys.exit(1)
    results = analyze_trace(sys.argv[1])
    print("\n=== Results ===")
    for k,v in results.items():
        print(f"{k}: {v}")