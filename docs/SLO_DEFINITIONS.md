# SLO Definitions

This document defines service-level objectives (SLOs) by service tier and maps them to Grafana dashboards.

## Tiering Model

- Tier 0: Gateway, Auth, Patient Search
- Tier 1: Clinical workflows (care gaps, quality evaluation)
- Tier 2: Supporting analytics and batch services

## Availability Objectives

- Tier 0: 99.95% monthly availability
- Tier 1: 99.90% monthly availability
- Tier 2: 99.50% monthly availability

## Latency Objectives

| Endpoint/Capability | Tier | P95 SLO | P99 SLO |
|---|---|---:|---:|
| Authentication (`/api/auth/login`) | Tier 0 | <= 500 ms | <= 1000 ms |
| Patient Search (`/api/v1/patients/search`) | Tier 0 | <= 750 ms | <= 1200 ms |
| Care Gap API (`/api/v1/care-gaps`) | Tier 1 | <= 1000 ms | <= 1600 ms |
| Quality Evaluation (`/api/v1/quality/evaluate`) | Tier 1 | <= 1200 ms | <= 2000 ms |
| FHIR Reads (`/fhir/*`) | Tier 1 | <= 900 ms | <= 1500 ms |

## Error Budget Policy

- Monthly error budget = `1 - SLO`.
- Burn rate > 2x for 1h triggers warning.
- Burn rate > 6x for 15m triggers critical alert and release freeze.

## Grafana Dashboards

The required three dashboards are available in `docker/grafana/dashboards/`:

1. SLO Dashboard: `platform-overview.json`
2. Business Metrics Dashboard: `business-metrics.json`
3. Security Dashboard: `hipaa-compliance.json`

These dashboards are auto-provisioned via `docker/grafana/dashboards/default.yml`.
