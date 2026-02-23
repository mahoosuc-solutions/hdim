# IPA Network Runbook

## 1) Purpose and Archetype
- Segment: IPA with many semi-autonomous practices
- Typical profile: 50+ practices with variable IT maturity

## 2) Primary Outcomes and KPI Targets
1. Drive standardized transitions-of-care workflows across pilot practices
2. Improve patient engagement/comprehension on pilot cohort
3. Establish repeatable rollout pattern per practice cluster
4. Prove runway for network-wide scale

## 3) Deployment Model Decision
- Options: HDIM-hosted / Customer-cloud / On-prem / Air-gapped
- Due: End of Week 1
- Owner: IPA IT + HDIM SA

## 4) Role-Based Execution
### Sales
- [ ] Align pilot scope to a specific practice cluster
### Solution Architecture
- [ ] Define multi-tenant/network segmentation approach
### Engineering
- [ ] Validate integration variance across pilot cluster
### Customer Success
- [ ] Establish hub-and-spoke governance and training

## 5) Hard Gates and Required Evidence
| Gate ID | Gate Name | Owner | Required Evidence | Pass Criteria | Failure Handling | Due Week |
|---|---|---|---|---|---|---|
| G1 | Security + Segmentation | SA Lead | segmentation and control evidence | approved by IPA security | stop launch | 1 |
| G2 | Practice Cluster Readiness | Eng Lead | pilot cluster integration evidence | target practices validated | reduce cluster scope | 2 |
| G3 | Governance Readiness | CS Lead | steering + escalation charter | accountability confirmed | defer launch | 2 |

## 6) Risk Register
| Risk ID | Category | Likelihood | Impact | Mitigation | Trigger | Owner |
|---|---|---|---|---|---|---|
| IPA-R1 | operational | High | High | cluster-first rollout | uneven practice participation | CS Lead |
| IPA-R2 | integration | Med | High | standard mapping kit | format drift between practices | Eng Lead |
| IPA-R3 | security | Med | High | early security workshop | control approval delays | SA Lead |

## 7) Week-by-Week Timeline
- Week 1: cluster definition, deployment decision, G1
- Week 2: cluster readiness validation, G2/G3
- Weeks 3-4: pilot launch for selected cluster
- Weeks 5-8: stabilize and optimize
- Weeks 9-13: measure outcomes and scale blueprint

## 8) Artifacts and Sign-Offs
- [ ] segmentation and security sign-off
- [ ] integration readiness sign-off
- [ ] governance sign-off

## 9) Escalation Paths
- Primary: Practice lead -> IPA ops -> CS
- Secondary: CS -> SA/Eng
- Executive: IPA executive sponsor + HDIM sponsor
