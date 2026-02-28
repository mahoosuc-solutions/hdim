# Roadmap Acceptance Matrices (2026-02-28)

## Global Definition of Done (All Healthcare SaaS Slices)
- Requirements are captured with clear API contracts, data ownership, and tenant boundary rules.
- Unit + integration + workflow smoke tests are automated and reproducible locally.
- Security controls are validated for authn/authz, audit logging, secrets handling, and least privilege.
- HIPAA/SOC 2 evidence is produced: access controls, logging, change traceability, and vulnerability posture.
- Performance budgets are defined and measured with pass/fail thresholds.

## #506 Revenue Cycle Transaction Backbone
Requirements:
- Support claim submission, claim status, remittance ingestion with idempotency.
- Persist end-to-end correlation IDs and state transitions.
Success criteria:
- 100% pass on happy-path + idempotent replay + invalid tenant/auth rejection.
Test cases:
- API contract tests, repository integrity tests, gateway routing smoke, load test at target TPS.
CI gates:
- `wave1-edge-gateway-validation.yml`, backend unit/integration tests, security scan.

## #507 HIE/ADT Exchange Backbone
Requirements:
- ADT ingest, acknowledgment, retrieval, and replay-safe processing.
- Tenant-isolated event visibility and immutable audit trail.
Success criteria:
- Event lifecycle validated (ingest -> ack -> retrieve) with strict tenant isolation.
Test cases:
- ADT contract tests, duplicate-message replay tests, event consistency checks.
CI gates:
- `wave1-edge-gateway-validation.yml`, backend tests, audit validation scripts.

## #508 Price Transparency Publication and Estimate Core
Requirements:
- Publish machine-readable rates and expose estimate calculation API.
- Versioned rate sources with provenance metadata.
Success criteria:
- Deterministic estimates for fixed input scenarios and stale-data protection.
Test cases:
- Calculation golden tests, schema validation for published files, authz tests.
CI gates:
- Contract tests + perf p95 budget + security static checks.

## #509 Utilization Review and LOS Core Workflow
Requirements:
- Admission review states, LOS tracking, escalation paths, and case notes.
- Full auditability of status transitions.
Success criteria:
- Deterministic workflow transitions with policy guardrails.
Test cases:
- State machine tests, concurrency tests, audit log completeness tests.
CI gates:
- Workflow integration tests + data integrity checks + audit evidence export.

## #510 Credential Lifecycle and Enrollment Core
Requirements:
- Provider credential status lifecycle, expiry/renewal, enrollment tracking.
- Role-based access for operations and compliance users.
Success criteria:
- No invalid transitions; alerts fire before expiration windows.
Test cases:
- Lifecycle transition tests, RBAC tests, notification trigger tests.
CI gates:
- Access-control validation + functional integration tests.

## #511 Attribution Ingestion and Panel Baseline
Requirements:
- Import attribution rosters with reconciliation and versioning.
- Panel assignment history with reversible corrections.
Success criteria:
- Reconciliation mismatches flagged with actionable diagnostics.
Test cases:
- Import schema tests, diff/reconcile tests, panel integrity tests.
CI gates:
- Data integrity suite + load test for roster ingest.

## #512 CMS Quality Scoring Pipeline Baseline
Requirements:
- Ingest measure data, calculate scores, publish benchmarkable outputs.
- Trace score lineage from source records.
Success criteria:
- Reproducible score outputs for fixed fixtures.
Test cases:
- Measure calculation regression tests, lineage/audit tests, export tests.
CI gates:
- Measure-focused CI + scoring regression suite.

## #513 Operational KPI Pipeline Baseline
Requirements:
- Generate throughput, quality, and cost KPIs with tenant segmentation.
- Backfill and near-real-time refresh modes.
Success criteria:
- KPI drift alerts and data freshness SLO met.
Test cases:
- KPI correctness tests, freshness tests, anomaly detection sanity checks.
CI gates:
- Pipeline integration tests + performance SLO gate.

## #514 Benchmark Ingest and Cohort Baseline
Requirements:
- Ingest external benchmark datasets and map to internal cohorts.
- Support cohort definitions with transparent inclusion rules.
Success criteria:
- Stable cohort outputs across reruns; benchmark mapping confidence tracked.
Test cases:
- Cohort definition tests, mapping precision tests, backfill consistency tests.
CI gates:
- Benchmark ingest integration tests + data quality thresholds.

## #515 Infrastructure Unblock for Assurance Gates
Requirements:
- Restore GitHub Actions billing/capacity.
- Confirm required assurance workflows execute end-to-end.
Success criteria:
- `Wave-1 Edge Gateway Validation` run completes on `master` with artifacts.
Test cases:
- Manual workflow dispatch and verification of artifact upload + pass/fail behavior.
CI gates:
- Account-level readiness check, then normal workflow enforcement.
