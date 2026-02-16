# Correlation Engine Design

## Objective

Correlate anomalies across service boundaries to improve root-cause identification.

## Inputs

- Prometheus metric anomalies
- Service dependency metadata
- Distributed trace context (when available)

## Processing

- Build temporal and dependency-based correlation graph
- Score candidate root causes by proximity and propagation
- Attach remediation hints and related alerts

## Outputs

- Ranked root-cause list
- Affected service set
- Suggested investigation path
