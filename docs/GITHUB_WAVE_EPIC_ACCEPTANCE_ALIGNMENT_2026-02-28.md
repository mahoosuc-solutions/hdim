# GitHub Wave Epic Acceptance Alignment

**Date:** 2026-02-28  
**Scope:** Open epics and roadmap issues from Waves 0-3 (`#276-#285`, `#36`)  
**Reference Playbook:** `docs/implementation-guides/CUSTOMER_ONBOARDING_ADOPTION_PLAYBOOK.md`

---

## 1. Purpose

Map each open wave epic to concrete requirements, acceptance criteria, validation tests, and phase-gate evidence so execution and closure are objective.

---

## 2. Wave 0 Planning Baseline

## #285 Hospital COO/CIO Feature Roadmap

### Requirements
- Define roadmap outcomes by executive role (COO/CIO/CMO/CTO).
- Link each roadmap item to measurable KPI impact.
- Publish dependencies and required enabling architecture decisions.

### Acceptance Criteria
- Roadmap document approved by product + architecture leads.
- Every item includes owner, timeline, dependency, and KPI linkage.
- Investor-facing and customer-facing roadmaps use consistent claims.

### Validation Tests
- Documentation review checklist completion.
- Traceability test: roadmap item -> issue -> acceptance criteria -> test evidence.

### Phase-Gate Evidence
- Phase 0 outcomes charter alignment and governance cadence documented.

---

## #36 Record Clinical Portal Demo Video

### Requirements
- Demonstrate end-to-end customer value flow aligned to current implementation.
- Include CMO and CTO viewpoints in the walkthrough.

### Acceptance Criteria
- Video script references only implemented functionality.
- Demo covers at least one core workflow from data ingestion to outcome.
- Recording and script stored with version and date metadata.

### Validation Tests
- Dry-run checklist against live environment.
- Peer review confirms no roadmap-only claims presented as completed.

### Phase-Gate Evidence
- Wave 0 sales/enablement collateral evidence package.

---

## 3. Wave 1 Critical Integration

## #277 TEFCA/HIE Connectivity

### Requirements
- Implement secure interoperability connector path with tenant-aware controls.
- Support event ingestion/normalization to internal clinical data models.
- Provide observability and retry handling for failed exchanges.

### Acceptance Criteria
- Contract tests pass for inbound/outbound exchange payloads.
- Failed exchanges are retriable, logged, and operationally visible.
- PHI handling and access control requirements are validated.

### Validation Tests
- Integration tests with representative TEFCA/HIE payloads.
- Negative tests for malformed/unauthorized exchange attempts.
- Performance tests for sustained exchange volumes.

### Phase-Gate Evidence
- Phase 1 data trust and security gate artifacts.

---

## #276 Revenue Cycle Management Integration

### Requirements
- Integrate revenue workflow events with reconciliation and status visibility.
- Provide operational dashboard for transaction lifecycle and exception queues.

### Acceptance Criteria
- Revenue transaction states are traceable end-to-end.
- Reconciliation reports align with source system totals within defined tolerance.
- Exception workflows are actionable by operations users.

### Validation Tests
- Integration tests for claim/transaction status transitions.
- Reconciliation delta tests and failure-path tests.
- Role-based UI tests for exception resolution workflows.

### Phase-Gate Evidence
- Phase 1 reliability gate + Phase 2 workflow validation evidence.

---

## 4. Wave 2 Regulatory And Clinical Value

## #282 CMS Quality Program Compliance Dashboard

### Requirements
- Provide compliance dashboard with measure status, submission readiness, and exceptions.
- Include role-specific views for quality and executive users.

### Acceptance Criteria
- Dashboard metrics match validated backend source-of-truth outputs.
- Compliance exceptions include owner, due date, and resolution workflow.
- Export/report outputs are consistent and auditable.

### Validation Tests
- Data parity tests between dashboard and backend aggregates.
- UI tests for status filtering and exception drill-down.
- Audit tests for report generation access and activity logging.

### Phase-Gate Evidence
- Phase 2 outcome and compliance gate results.

---

## #278 Price Transparency Compliance

### Requirements
- Publish and maintain required price transparency data surfaces.
- Add validation and change controls for pricing dataset publication.

### Acceptance Criteria
- Required data fields and refresh cadence meet policy expectations.
- Publication process includes approval, versioning, and rollback path.
- Compliance audit trail is complete for each publication cycle.

### Validation Tests
- Schema and completeness tests for price datasets.
- Workflow tests for publish/approve/rollback scenarios.
- Access tests for public/internal visibility boundaries.

### Phase-Gate Evidence
- Phase 2 compliance gate with publication evidence.

---

## #281 Patient Attribution & Panel Management

### Requirements
- Attribute patients to panels using transparent and auditable rules.
- Provide actionable panel management workflows for clinical operations.

### Acceptance Criteria
- Attribution logic is deterministic and explainable.
- Panel assignment changes are versioned and auditable.
- Clinical users can act on attributed cohorts without manual data stitching.

### Validation Tests
- Rule engine unit tests for attribution logic.
- Integration tests for attribution refresh and panel updates.
- Workflow tests for panel-based outreach and task completion.

### Phase-Gate Evidence
- Phase 2 adoption and workflow reliability gate.

---

## #279 Utilization Management & Case Management

### Requirements
- Implement case workflows for utilization review and escalation.
- Track utilization risk signals and intervention outcomes.

### Acceptance Criteria
- Case lifecycle states and transitions are enforced and auditable.
- High-risk cases are prioritized with SLA-aware queues.
- Outcome reporting captures intervention status and cycle time.

### Validation Tests
- State-transition tests for case workflow.
- SLA breach and escalation path tests.
- UI tests for queue management and case updates.

### Phase-Gate Evidence
- Phase 2 adoption gate + Phase 3 optimization baseline.

---

## 5. Wave 3 Analytics And Workforce

## #283 Operational Analytics for Hospital Leadership

### Requirements
- Deliver executive operational analytics across quality, throughput, and risk.
- Support drill-down from enterprise overview to service-line detail.

### Acceptance Criteria
- Executive dashboard loads agreed KPI set with role-based access.
- Data latency and freshness meet operational decision-making needs.
- Drill-down flows support root-cause analysis without external tooling.

### Validation Tests
- KPI calculation tests and data lineage checks.
- Performance tests for dashboard load and filtering operations.
- Role-based access and export/audit tests.

### Phase-Gate Evidence
- Phase 3 outcome trend evidence and QBR package.

---

## #284 Benchmarking & Comparative Analytics

### Requirements
- Provide comparative benchmarking views across cohorts, timeframes, and sites.
- Support normalized metrics and confidence-aware interpretation guidance.

### Acceptance Criteria
- Benchmark methodology is documented and reproducible.
- Comparative views are statistically and operationally interpretable.
- Users can filter and export benchmark views with audit traceability.

### Validation Tests
- Benchmark computation tests with known fixture datasets.
- UI tests for filters, cohort comparisons, and exports.
- Documentation completeness review for methodology transparency.

### Phase-Gate Evidence
- Phase 3 optimization/leadership analytics evidence.

---

## #280 Provider Credentialing & Enrollment

### Requirements
- Track provider credentialing and enrollment status with due-date management.
- Support alerting/escalation for expiring or missing credential artifacts.

### Acceptance Criteria
- Credential lifecycle states are accurate and operationally visible.
- Renewal and exception workflows are time-bound with ownership.
- Compliance/audit logs capture changes and approvals.

### Validation Tests
- Workflow tests for credential lifecycle events.
- Alert/escalation tests for impending expirations.
- Access and auditability tests for credential updates.

### Phase-Gate Evidence
- Phase 3 operations and compliance gate artifacts.

---

## 6. Cross-Epic Definition Of Done

An epic may close only when all conditions are met:

1. Requirements and acceptance criteria are linked in issue comments.
2. Unit/integration/e2e tests for scope pass in CI.
3. Phase-gate evidence is attached (data, workflow, adoption, compliance).
4. No unresolved high/critical security findings in touched components.
5. Runbook/documentation is updated for operations and customer success.

