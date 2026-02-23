# Today's Priority To-Do (Execution Control)

Date: 2026-02-23
Purpose: close all currently open Week 1 blockers in priority order.

## Priority 1 (Do First - Hard Dependencies)

1. Confirm customer security approver and backup.
- Owner: Sales Lead
- Due: 2026-02-25
- Source: `04-action-log.md` (A-001)
- Completion criteria: names and emails entered in `01-owner-roster.md` and `11-decision-log.md`.

2. Schedule Week-1 architecture workshop (90 min).
- Owner: Customer Success Lead
- Due: 2026-02-25
- Source: `04-action-log.md` (A-002)
- Completion criteria: invite sent using `14-workshop-invite-email-ready.md` with confirmed date/time.

3. Prepare and send workshop pre-read packet.
- Owner: Solution Architect
- Due: 2026-02-25
- Source: `10-workshop-pre-read-email.md`
- Completion criteria: pre-read email sent and attachments referenced.

## Priority 2 (Needed for G1 Pass)

4. Finalize deployment comparison one-pager for workshop.
- Owner: Solution Architect
- Due: 2026-02-25
- Source: `04-action-log.md` (A-003)
- Completion criteria: decision-ready comparison attached to workshop pre-read.

5. Capture workshop decisions in decision log.
- Owner: Solution Architect
- Due: Day of workshop
- Source: `11-decision-log.md`
- Completion criteria: D-001, D-002, D-003 updated from Pending to final decisions.

6. Complete G1 evidence package.
- Owner: Solution Architect
- Due: 2026-02-27
- Source: `07-gate-evidence-g1-architecture-security.md`
- Completion criteria: all checklist items checked and status moved to PASS.

## Priority 3 (Kickoff Readiness)

7. Draft source-system interface inventory template.
- Owner: Engineering Lead
- Due: 2026-02-26
- Source: `04-action-log.md` (A-004)
- Completion criteria: inventory template shared and linked from integration checklist.

8. Publish baseline KPI definition sheet and assign data owners.
- Owner: Customer Success Lead
- Due: 2026-02-26
- Source: `04-action-log.md` (A-005)
- Completion criteria: baseline owners listed and placeholders removed in `06-weekly-steering-update-week1.md`.

9. Establish weekly steering cadence and attendee list.
- Owner: Customer Success Lead
- Due: 2026-02-26
- Source: `04-action-log.md` (A-006)
- Completion criteria: recurring invite sent and reflected in `06-weekly-steering-update-week1.md`.

## Priority 4 (Closure and Communication)

10. Send post-workshop decision summary.
- Owner: Sales Lead or Customer Success Lead
- Due: within 24 hours post-workshop
- Source: `15-post-workshop-followup-email.md`
- Completion criteria: sent email with updated decision log, gate snapshot, risk register, and actions.

11. Issue readiness memo with sign-offs.
- Owner: Customer Success Lead
- Due: end of Week 1
- Source: `08-runbook-readiness-memo.md`
- Completion criteria: sign-off lines populated and recommendation set to Proceed/Hold.

## End-of-Day Control Check

- [ ] `02-gate-status-snapshot.md` updated
- [x] `04-action-log.md` statuses updated — A-003 → Ready for Review, A-004 → Ready for Review, A-005 → In Progress (2026-02-23)
- [ ] `05-risk-register.md` updated with net new risks
- [ ] `16-week1-command-center-checklist.md` reviewed

## Agent-Completed (2026-02-23)

- [x] A-003: `32-deployment-model-decision-onepager.md` — decision table, shared responsibility summary, workshop capture section
- [x] A-004: `33-source-system-interface-inventory-template.md` — source system inventory, minimum required FHIR R4 fields, integration pattern table, gap tracker, sign-off
- [x] A-005 (partial): `37-kpi-baseline-definition-sheet.md` — all 4 KPIs defined with numerators, denominators, targets, and reporting cadence; data owners require human assignment
- [x] `04-action-log.md` updated to reflect new artifact status

## Remaining — Blocked on Human Input

- A-001: Security approver names/emails (Sales Lead action)
- A-002: Workshop invite with confirmed date/time (Customer Success Lead action)
- A-005 data owners: Customer program team must fill in `37-kpi-baseline-definition-sheet.md` owner column
- A-006: Steering cadence once customer contacts are known
- All 40+ execution pack placeholders in `36-account-variables-fill-now.md`
