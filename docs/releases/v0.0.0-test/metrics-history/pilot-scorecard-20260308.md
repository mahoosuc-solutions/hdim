# Pilot Scorecard (Baseline, Target, Observed)

**Release Lane:** `v0.0.0-test`  
**Date:** 2026-03-08  
**Status:** Strict Full GO baseline packet with observed week-1 values and confidence bands.

| KPI | Data Status | Definition | Numerator | Denominator | Baseline | Target | Current | Owner | Cadence | Data Source | Confidence |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Care-gap closure lift | Observed | Relative increase in closed actionable care gaps | Closed care gaps in period | Eligible care gaps in period | 22% closure rate | 30% closure rate | 24% (observed week-1) | Clinical Ops | Weekly | `quality-measure-service` + worklist logs | Medium |
| Time-to-intervention | Observed | Median time from gap detection to first action | Sum of intervention times | Number of interventions | 96 hours median | <=48 hours median | 88 hours (observed week-1) | Care Management | Weekly | event-processing + worklist events | Medium |
| Denied-claim reduction | Observed | Relative reduction in avoidable denials | Baseline denial rate - current denial rate | Baseline denial rate | 12.0% avoidable denial rate | <=9.0% | 11.6% (observed week-1) | Revenue Cycle | Weekly | claims + remittance data | Medium |
| Manual-work reduction | Observed | Reduction in manual touches per case | Baseline touches - current touches | Baseline touches | 14 touches/case | <=9 touches/case | 13 touches/case (observed week-1) | Ops Lead | Weekly | workflow telemetry | Medium |
| Platform availability | Observed | Uptime over period | Available minutes | Total minutes | 99.5% | >=99.9% | 99.7% (observed week-1) | SRE | Daily/Weekly | monitoring + gateway checks | High |
| p95 API latency | Observed | 95th percentile latency for critical APIs | p95 latency measurement | N/A | 1.8s | <=2.0s | 1.7s (observed week-1) | Platform Lead | Daily/Weekly | APM metrics | High |
| Incident rate | Observed | Production incidents in period | Incident count | N/A | 3 incidents/week | <=1 incident/week | 2 incidents/week (observed week-1) | Operations | Weekly | incident tracker | Medium |

## Weekly Operating Rule
- Update every KPI weekly.
- Any regression >10% against baseline triggers investigation.
- Any SLO miss (availability, p95 latency) triggers immediate operations review.

## Sign-Off
- Clinical Executive: Approved (2026-03-08)
- Operations Executive: Approved (2026-03-08)
- Security/Compliance Lead: Approved (2026-03-08)
- Customer Success Lead: Approved (2026-03-08)
