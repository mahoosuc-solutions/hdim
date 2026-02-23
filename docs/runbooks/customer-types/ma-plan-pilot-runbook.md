# MA Plan Pilot Runbook

## 1) Purpose and Archetype
- Segment: Medicare Advantage plan (transitions-of-care pilot)
- Typical profile: ~30,000 members, high focus on post-discharge outcomes

## 2) Primary Outcomes and KPI Targets
1. Improve 7-day post-discharge engagement by >=20% relative
2. Improve comprehension confirmation by >=15% relative
3. Reduce unresolved patient questions at 48-72h
4. Maintain >=99.5% service availability during pilot hours

## 3) Deployment Model Decision
- Options: HDIM-hosted / Customer-cloud / On-prem / Air-gapped
- Due: End of Week 1
- Owner: Joint (Customer IT/security + HDIM solution architect)

## 4) Role-Based Execution
### Sales
- [ ] Align sponsor and procurement stakeholders
- [ ] Finalize pilot scope and budget model
### Solution Architecture
- [ ] Confirm topology and data trust boundaries
- [ ] Complete shared responsibility mapping
### Engineering
- [ ] Validate integration flows and non-prod connectivity
- [ ] Validate KPI instrumentation and escalation routing
### Customer Success
- [ ] Stand up weekly steering and comms cadence
- [ ] Track adoption and issue triage

## 5) Hard Gates and Required Evidence
| Gate ID | Gate Name | Owner | Required Evidence | Pass Criteria | Failure Handling | Due Week |
|---|---|---|---|---|---|---|
| G1 | Architecture + Security | SA Lead | signed security checklist + deployment decision | controls accepted by security sponsor | block kickoff and escalate | 1 |
| G2 | Integration + UAT | Eng Lead | integration validation + UAT signoff | core workflows pass | delay go-live and remediate | 2 |
| G3 | Pilot Readiness | CS Lead | KPI baseline + governance schedule | steering cadence and ownership confirmed | convert to pre-pilot state | 2 |

## 6) Risk Register
| Risk ID | Category | Likelihood | Impact | Mitigation | Trigger | Owner |
|---|---|---|---|---|---|---|
| MA-R1 | integration | Med | High | early mapping workshop | missing/late source fields | Eng Lead |
| MA-R2 | security | Med | High | week-1 control review | security sign-off delayed | SA Lead |
| MA-R3 | operational | Med | Med | weekly steering discipline | unresolved blockers >7 days | CS Lead |

## 7) Week-by-Week Timeline
- Week 1: discovery finalization + deployment decision + G1
- Week 2: integration validation + UAT + G2/G3
- Weeks 3-4: controlled launch and workflow tuning
- Weeks 5-8: pilot execution with weekly KPI governance
- Weeks 9-13: optimization, outcome analysis, scale recommendation

## 8) Artifacts and Sign-Offs
- [ ] `templates/GATE_EVIDENCE_TEMPLATE.md` (G1/G2/G3)
- [ ] `templates/WEEKLY_STEERING_UPDATE_TEMPLATE.md`
- [ ] `templates/PILOT_CLOSEOUT_TEMPLATE.md`

## 9) Escalation Paths
- Primary: CS Lead -> Eng Lead
- Secondary: Solution Architect -> Product/Engineering leadership
- Executive: Sponsor-to-sponsor escalation in weekly steering
