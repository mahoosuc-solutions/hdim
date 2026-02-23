# Source-System Interface Inventory Template

Prepared: 2026-02-23
Action: A-004 | Owner: Engineering Lead | Due: 2026-02-26
Purpose: Capture all source systems, integration patterns, and field mappings required for Phase 1 pilot.
Instructions: Engineering Lead to complete this template with customer IT team in Week 1. Column "Required for Phase 1" drives integration scope.

---

## 1) Source System Inventory

| System ID | System Name | System Type | Vendor / Platform | Integration Owner (Customer) | Integration Owner (HDIM) | Environment | Status |
|---|---|---|---|---|---|---|---|
| SS-001 | | ADT / Discharge Event Feed | | | | Prod / Staging | [ ] Confirmed |
| SS-002 | | EHR (Clinical Context) | | | | Prod / Staging | [ ] Confirmed |
| SS-003 | | Care Management / Care Coordination | | | | Prod / Staging | [ ] Confirmed |
| SS-004 | | Patient Communication Preference Store | | | | Prod / Staging | [ ] Confirmed |
| SS-005 | | Identity / MPI (Master Patient Index) | | | | Prod / Staging | [ ] Confirmed |
| SS-006 | | Claims / Payer Data | | | | Prod / Staging | [ ] Confirmed |
| SS-007 | | (Add rows as needed) | | | | | |

---

## 2) Minimum Required Dataset — Phase 1

The following fields are required for 7-day post-discharge engagement workflows.

| Field Name | FHIR R4 Resource | Source System (ID) | Required for Phase 1 | Available Today? | Notes / Gaps |
|---|---|---|---|---|---|
| Patient ID / MRN | Patient.identifier | | Yes | [ ] Yes [ ] No | |
| Discharge date and time | Encounter.period.end | | Yes | [ ] Yes [ ] No | |
| Discharge disposition | Encounter.hospitalization.dischargeDisposition | | Yes | [ ] Yes [ ] No | |
| Primary diagnosis | Condition.code | | Yes | [ ] Yes [ ] No | |
| Attending/Discharging provider | Encounter.participant.individual | | Yes | [ ] Yes [ ] No | |
| Discharge instructions text | DocumentReference or Composition | | Yes | [ ] Yes [ ] No | |
| Preferred language | Patient.communication | | Yes | [ ] Yes [ ] No | |
| Contact phone / SMS | Patient.telecom | | Yes | [ ] Yes [ ] No | |
| Care manager assignment | CareTeam.participant | | Preferred | [ ] Yes [ ] No | |
| Active payer program flags | Coverage.class | | Preferred | [ ] Yes [ ] No | |
| Open care gaps (HEDIS) | Observation (quality) | | Phase 2 | [ ] Yes [ ] No | |

---

## 3) Integration Pattern by Source

| System ID | Integration Mechanism | Protocol | Auth Method | Frequency | Latency SLA | PHI In Transit? | Notes |
|---|---|---|---|---|---|---|---|
| SS-001 | | HL7 v2 ADT / FHIR R4 Bundle | | Real-time / Batch | | Yes — encrypt in transit | |
| SS-002 | | FHIR R4 API / CDA | | On-demand | | Yes | |
| SS-003 | | REST API / FHIR R4 | | On-demand | | Yes | |
| SS-004 | | REST API | | On-demand | | Yes | |
| SS-005 | | FHIR Patient / REST | | On-demand | | Yes | |
| SS-006 | | Batch / EDI 837 / FHIR | | Daily batch | | Yes | |

---

## 4) Data Gap Tracking

| Gap ID | Field / System | Gap Description | Severity | Mitigation | Owner | Target Date | Status |
|---|---|---|---|---|---|---|---|
| DG-001 | | | High / Med / Low | | | | Open |
| DG-002 | | | | | | | |

---

## 5) Sign-Off (Completion Criteria for A-004)

| Role | Name | Sign-Off | Date |
|---|---|---|---|
| Engineering Lead (HDIM) | | [ ] Approved | |
| Integration Lead (Customer IT) | | [ ] Approved | |
| Solution Architect (HDIM) | | [ ] Reviewed | |

When all three sign-off boxes are checked, update `04-action-log.md` A-004 status to Closed and link this file.
