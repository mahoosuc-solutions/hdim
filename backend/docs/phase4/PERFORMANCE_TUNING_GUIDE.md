# Phase 4 Performance Tuning Guide

## Tuning Priorities

1. Reduce alert noise while preserving critical signal.
2. Keep P95/P99 latency within SLO budgets.
3. Optimize dashboard query latency with summary tables.

## Recommended Actions

- Tighten scrape and retention settings to match workload.
- Use pre-aggregated metrics for frequently queried panels.
- Profile slow DB queries used by analytics endpoints.
- Tune JVM heap and GC settings for high-throughput services.

## Validation

- Re-run baseline and load tests after each tuning cycle.
- Track regression against `docs/PERFORMANCE_BASELINES.md`.
