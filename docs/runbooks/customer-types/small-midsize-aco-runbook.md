# Small/Midsize ACO Runbook

## 1) Purpose and Archetype
- Segment: ACO with multiple practices and shared quality incentives
- Typical profile: 8,000-100,000 attributed lives

## 2) Primary Outcomes and KPI Targets
1. Improve care-transition follow-up performance
2. Improve member comprehension and engagement
3. Reduce workflow latency for escalations
4. Demonstrate measurable pilot ROI

## 3) Deployment Model Decision
- Options: HDIM-hosted / Customer-cloud / On-prem / Air-gapped
- Due: End of Week 1
- Owner: ACO IT + HDIM SA

## 4) Role-Based Execution
### Sales
- [ ] Align contract, pilot economics, and expansion criteria
### Solution Architecture
- [ ] Confirm multi-practice data and identity boundaries
### Engineering
- [ ] Validate source heterogeneity and mapping coverage
### Customer Success
- [ ] Set governance cadence with ACO ops leads

## 5) Hard Gates and Required Evidence
| Gate ID | Gate Name | Owner | Required Evidence | Pass Criteria | Failure Handling | Due Week |
|---|---|---|---|---|---|---|
| G1 | Topology + Security | SA Lead | deployment decision + controls evidence | approved by IT/security | block pilot start | 1 |
| G2 | Multi-source Integration | Eng Lead | mapping and sync evidence | all priority feeds validated | reduce scope or delay | 2 |
| G3 | Ops Readiness | CS Lead | steering plan + action owners | owners and cadence confirmed | defer launch | 2 |

## 6) Risk Register
| Risk ID | Category | Likelihood | Impact | Mitigation | Trigger | Owner |
|---|---|---|---|---|---|---|
| ACO-R1 | integration | High | High | phased source onboarding | feed mismatch across practices | Eng Lead |
| ACO-R2 | operational | Med | High | clear RACI by practice | missing practice-level ownership | CS Lead |
| ACO-R3 | commercial | Med | Med | KPI baseline lock | unclear success definition | Sales Lead |

## 7) Week-by-Week Timeline
- Week 1: discovery, RACI, deployment decision, G1
- Week 2: data mapping and UAT, G2/G3
- Weeks 3-4: phased rollout across pilot practices
- Weeks 5-8: run pilot with weekly steering
- Weeks 9-13: quantify outcomes and scale path

## 8) Artifacts and Sign-Offs
- [ ] gate evidence docs
- [ ] weekly steering updates
- [ ] pilot closeout package

## 9) Escalation Paths
- Primary: Practice ops -> CS
- Secondary: CS -> SA/Eng
- Executive: ACO sponsor + HDIM sponsor
