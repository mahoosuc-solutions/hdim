# GitHub Wave Burndown Dashboard

**Baseline Date:** 2026-02-27  
**Scope:** Open issues assigned to wave milestones #13-#16  
**Source of truth:** GitHub milestones and issues in `webemo-aaron/hdim`

---

## 1. Baseline Snapshot (2026-02-27)

| Milestone | Due Date | Open | Closed | Total | Completion |
|---|---:|---:|---:|---:|---:|
| [2026-Wave-0 Planning Baseline](https://github.com/webemo-aaron/hdim/milestone/13) | 2026-03-05 | 2 | 0 | 2 | 0% |
| [2026-Wave-1 Critical Integration](https://github.com/webemo-aaron/hdim/milestone/14) | 2026-03-19 | 2 | 0 | 2 | 0% |
| [2026-Wave-2 Regulatory and Clinical Value](https://github.com/webemo-aaron/hdim/milestone/15) | 2026-04-09 | 4 | 0 | 4 | 0% |
| [2026-Wave-3 Analytics and Workforce](https://github.com/webemo-aaron/hdim/milestone/16) | 2026-04-23 | 3 | 0 | 3 | 0% |
| **Total** |  | **11** | **0** | **11** | **0%** |

---

## 2. Weekly Burndown Targets (Fridays)

| Checkpoint Date | Target Remaining Open | Notes |
|---|---:|---|
| 2026-02-27 | 11 | Baseline established |
| 2026-03-06 | 9 | Wave 0 complete by 2026-03-05 |
| 2026-03-13 | 8 | Advance Wave 1 work before due date |
| 2026-03-20 | 7 | Wave 1 complete by 2026-03-19 |
| 2026-03-27 | 5 | Burn-down into Wave 2 |
| 2026-04-03 | 4 | Wave 2 near completion |
| 2026-04-10 | 3 | Wave 2 complete by 2026-04-09 |
| 2026-04-17 | 1 | Final Wave 3 closures in progress |
| 2026-04-24 | 0 | Wave 3 complete by 2026-04-23 |

---

## 3. Current Open Issues by Wave

## Wave 0
- [#285](https://github.com/webemo-aaron/hdim/issues/285) - Hospital COO/CIO Feature Roadmap
- [#36](https://github.com/webemo-aaron/hdim/issues/36) - Record Clinical Portal Demo Video

## Wave 1
- [#277](https://github.com/webemo-aaron/hdim/issues/277) - TEFCA/HIE Connectivity
- [#276](https://github.com/webemo-aaron/hdim/issues/276) - Revenue Cycle Management Integration

### Wave 1 Execution Slices (Child Tracking)
- [#507](https://github.com/webemo-aaron/hdim/issues/507) - Execution Slice 1: HIE/ADT exchange backbone (child of #277)
- [#506](https://github.com/webemo-aaron/hdim/issues/506) - Execution Slice 1: Revenue cycle transaction backbone (child of #276)

## Wave 2
- [#282](https://github.com/webemo-aaron/hdim/issues/282) - CMS Quality Program Compliance Dashboard
- [#278](https://github.com/webemo-aaron/hdim/issues/278) - Price Transparency Compliance
- [#281](https://github.com/webemo-aaron/hdim/issues/281) - Patient Attribution & Panel Management
- [#279](https://github.com/webemo-aaron/hdim/issues/279) - Utilization Management & Case Management

## Wave 3
- [#283](https://github.com/webemo-aaron/hdim/issues/283) - Operational Analytics for Hospital Leadership
- [#284](https://github.com/webemo-aaron/hdim/issues/284) - Benchmarking & Comparative Analytics
- [#280](https://github.com/webemo-aaron/hdim/issues/280) - Provider Credentialing & Enrollment

---

## 4. Weekly Update Procedure

1. Refresh milestone stats:
   - `gh api 'repos/webemo-aaron/hdim/milestones?state=open&per_page=100'`
2. Refresh wave issue inventory:
   - `gh issue list --state open --limit 200 --json number,title,milestone,updatedAt,url`
   - Include execution-slice children for Wave 1 parent epics:
     - `gh issue view 276 --json timelineItems`
     - `gh issue view 277 --json timelineItems`
3. Update this dashboard:
   - actual remaining open count
   - variance vs target (`actual - target`)
   - corrective actions if variance is positive
4. Post a milestone status note in each wave’s highest-priority issue.

---

## 5. Governance Thresholds

- `On track`: actual remaining open <= target remaining open.
- `Watch`: actual remaining open = target + 1.
- `At risk`: actual remaining open >= target + 2.

When status is `At risk`, require:
1. Scope split or descoping decision within 24 hours.
2. Additional SWE+QA pairing allocation for the affected wave.
3. Updated compliance evidence timing note in release readiness docs.
