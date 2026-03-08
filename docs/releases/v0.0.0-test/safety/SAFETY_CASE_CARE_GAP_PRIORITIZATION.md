# Clinical Safety Case

## Workflow
- Name: Care Gap Prioritization and Routing
- Clinical intent: Prioritize actionable care gaps for timely intervention
- In-scope population: Adults with active quality-measure eligibility
- Out-of-scope use: Final diagnosis or treatment recommendation

## Safety Hazards
| Hazard ID | Hazard Description | Trigger | Potential Harm | Severity | Likelihood |
|---|---|---|---|---|---|
| H-001 | High-risk gap not prioritized | Incomplete input feed | Delayed intervention | High | Medium |
| H-002 | Incorrect tenant routing | Tenant context mismatch | Privacy/event handling error | Critical | Low |

## Mitigations
| Hazard ID | Preventive Control | Detective Control | Safe Degradation Behavior | Owner |
|---|---|---|---|---|
| H-001 | Data completeness checks before scoring | Alert on stale/missing ingestion | Route as "Needs Review" queue | Clinical Ops |
| H-002 | Tenant-scoped access enforcement | Tenant isolation regression tests | Block routing and raise incident | Security Lead |

## Human Accountability
- Final clinical decision maker: Care manager / clinician
- Override path: Manual reprioritization in worklist
- Escalation path: Clinical operations lead
- Escalation SLA: <=4 hours for high-risk queue anomalies

## Monitoring Signals
| Signal | Threshold | Alert Target | Runbook |
|---|---|---|---|
| Data freshness | >60 min stale | Platform + Clinical Ops | Incident response + data pipeline recovery |
| Recommendation error rate | >2% flagged mismatch | Product + Clinical Ops | Safety review workflow |
| Latency | p95 >2s | Platform | API performance mitigation |

## Validation Evidence
- Unit/integration tests: quality-worklist prioritization and tenant scoping tests
- Simulation or replay tests: historical event replay for prioritization stability
- Negative/failure mode tests: missing data and stale feed scenarios
- Production drill evidence: release preflight + regulatory readiness reports

## Approvals
- Clinical Safety Owner: Pending
- Medical Director: Pending
- Engineering Owner: Pending
- Compliance Owner: Pending
