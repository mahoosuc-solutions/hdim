# Deployment Model Decision One-Pager (Workshop Use)

Prepared: 2026-02-23
Action: A-003 | Owner: Solution Architect | Due: 2026-02-25
Purpose: Decision-ready comparison for the Week-1 architecture workshop.
Decision needed: Select one model by end of Workshop.

---

## The Three Options at a Glance

| Criterion | Option 1: HDIM-Hosted | Option 2: Customer-Hosted Cloud | Option 3: On-Prem / Air-Gapped |
|---|---|---|---|
| **PHI location** | HDIM-managed tenant | Customer cloud account | Customer data center |
| **Who operates infra** | HDIM | Customer (cloud), HDIM (app) | Customer (infra), HDIM (app) |
| **Speed to pilot start** | Fastest (30-45 days) | Moderate (45-75 days) | Longest (60-120 days) |
| **Customer IT burden** | Lowest | Medium | Highest |
| **Network isolation** | Shared (dedicated tenant) | VPC-level isolation | Full physical isolation option |
| **Key management** | HDIM-managed | Customer-managed (preferred) | Customer-managed |
| **Audit export access** | HDIM portal + API | HDIM + customer direct access | HDIM + customer direct access |
| **Incident response lead** | HDIM-led | Joint | Joint |
| **Best fit** | Fastest value, lowest IT lift | Cloud control requirement | Strict data locality / compliance mandate |

---

## PHI and Zero-Retention Commitment (All Models)

- Source PHI remains in payer/provider systems and approved operational stores.
- AI requests are request-scoped — no customer data used for model training.
- Redaction applied at API layer before any AI processing.
- Audit trail retained per agreed contractual retention policy.

---

## Decision Factors to Confirm in Workshop

| Factor | Question to Resolve |
|---|---|
| Data residency policy | Does your security policy require PHI to remain within customer-owned infrastructure? |
| Cloud account ownership | Do you have an existing cloud subscription (AWS/Azure/GCP) to host the platform? |
| Network isolation requirement | Is VPC-level isolation sufficient, or is physical isolation required? |
| Timeline priority | Is 30-45 day pilot start feasible, or do procurement/security cycles extend the timeline? |
| Key management policy | Is HDIM-managed key management acceptable, or is customer-managed required by policy? |
| Operational staffing | Is customer IT staffing available for joint operations (on-prem model)? |

---

## HDIM Recommendation for Phase 1 Pilot

**Option 1 (HDIM-Hosted)** is recommended for Phase 1 pilot speed and lowest risk.

Rationale:
- Shortest path to 7-day post-discharge engagement workflows live and generating evidence.
- Removes infrastructure provisioning from the pilot's critical path.
- Pilot results become the evidence base for a customer-cloud or on-prem production decision.
- If security policy blocks Option 1, Option 2 is the next-fastest path.

---

## Workshop Decision Capture

| Item | Decision | Owner | Date |
|---|---|---|---|
| Selected deployment model | `[ ] Hosted  [ ] Customer-cloud  [ ] On-prem` | | |
| Security approver confirmed | `[ ] Yes  [ ] No` | | |
| Key management approach agreed | | | |
| Commercial model (adders) acknowledged | `[ ] Yes  [ ] No` | | |

Complete this section during workshop. Transfer results to `11-decision-log.md` (D-001, D-002) and `02-gate-status-snapshot.md`.

---

*Source: `07-deployment-models-hosted-vs-onprem.md`, `03-technical-architecture-and-security-baseline.md`*
