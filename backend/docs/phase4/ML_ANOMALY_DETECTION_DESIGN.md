# ML Anomaly Detection Design

## Objective

Detect service behavior deviations using time-windowed metrics and anomaly thresholds.

## Signals

- Error rate
- Latency (P50/P95/P99)
- Throughput
- Resource utilization (CPU, memory)

## Decision Model

- Baseline derived from recent historical windows
- Alert only when sustained deviation crosses threshold
- Severity mapped by magnitude and duration

## Operational Notes

- Keep false positive rate low by requiring persistence
- Route high-confidence anomalies to critical channels
