# Pilot Scorecard Template

Use this as the single source for pilot success metrics.

| KPI | Definition | Numerator | Denominator | Baseline | Target | Current | Owner | Cadence | Data Source | Confidence |
|---|---|---|---|---|---|---|---|---|---|---|
| Care-gap closure lift | Relative increase in closed actionable care gaps | Closed care gaps in period | Eligible care gaps in period |  |  |  | Clinical Ops | Weekly | `quality-measure-service` + worklist logs | High/Med/Low |
| Time-to-intervention | Median time from gap detection to first action | Sum of intervention times | Number of interventions |  |  |  | Care Management | Weekly | event-processing + worklist events | High/Med/Low |
| Denied-claim reduction | Relative reduction in avoidable denials | Baseline denial rate - current denial rate | Baseline denial rate |  |  |  | Revenue Cycle | Weekly | claims + remittance data | High/Med/Low |
| Manual-work reduction | Reduction in manual touches per case | Baseline touches - current touches | Baseline touches |  |  |  | Ops Lead | Weekly | workflow telemetry | High/Med/Low |
| Platform availability | Uptime over period | Available minutes | Total minutes |  | >=99.9% |  | SRE | Daily/Weekly | monitoring + gateway checks | High/Med/Low |
| p95 API latency | 95th percentile latency for critical APIs | p95 latency measurement | N/A |  | <=2s |  | Platform Lead | Daily/Weekly | APM metrics | High/Med/Low |
| Incident rate | Production incidents in period | Incident count | N/A |  | <= target |  | Operations | Weekly | incident tracker | High/Med/Low |

## Sign-Off
- Clinical Executive:
- Operations Executive:
- Security/Compliance Lead:
- Customer Success Lead:
