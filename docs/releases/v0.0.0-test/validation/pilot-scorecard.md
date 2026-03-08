# Pilot Scorecard (Initial Baseline + Target)

**Release Lane:** `v0.0.0-test`  
**Date:** 2026-03-08  
**Status:** Initial modeled baseline and target set; current values to refresh weekly from validated telemetry.

| KPI | Definition | Numerator | Denominator | Baseline | Target | Current | Owner | Cadence | Data Source | Confidence |
|---|---|---|---|---|---|---|---|---|---|---|
| Care-gap closure lift | Relative increase in closed actionable care gaps | Closed care gaps in period | Eligible care gaps in period | 22% closure rate | 30% closure rate | 22% (baseline week) | Clinical Ops | Weekly | `quality-measure-service` + worklist logs | Medium |
| Time-to-intervention | Median time from gap detection to first action | Sum of intervention times | Number of interventions | 96 hours median | <=48 hours median | 96 hours (baseline week) | Care Management | Weekly | event-processing + worklist events | Medium |
| Denied-claim reduction | Relative reduction in avoidable denials | Baseline denial rate - current denial rate | Baseline denial rate | 12.0% avoidable denial rate | <=9.0% | 12.0% (baseline week) | Revenue Cycle | Weekly | claims + remittance data | Medium |
| Manual-work reduction | Reduction in manual touches per case | Baseline touches - current touches | Baseline touches | 14 touches/case | <=9 touches/case | 14 touches/case (baseline week) | Ops Lead | Weekly | workflow telemetry | Medium |
| Platform availability | Uptime over period | Available minutes | Total minutes | 99.5% | >=99.9% | 99.5% (baseline week) | SRE | Daily/Weekly | monitoring + gateway checks | High |
| p95 API latency | 95th percentile latency for critical APIs | p95 latency measurement | N/A | 1.8s | <=2.0s | 1.8s (baseline week) | Platform Lead | Daily/Weekly | APM metrics | High |
| Incident rate | Production incidents in period | Incident count | N/A | 3 incidents/week | <=1 incident/week | 3 incidents/week (baseline week) | Operations | Weekly | incident tracker | Medium |

## Weekly Operating Rule
- Update every KPI weekly.
- Any regression >10% against baseline triggers investigation.
- Any SLO miss (availability, p95 latency) triggers immediate operations review.

## Sign-Off
- Clinical Executive: Pending
- Operations Executive: Pending
- Security/Compliance Lead: Pending
- Customer Success Lead: Pending
