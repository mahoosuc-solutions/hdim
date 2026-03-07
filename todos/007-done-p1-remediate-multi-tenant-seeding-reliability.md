---
status: done
priority: p1
issue_id: "007"
tags: [seeding, multi-tenant, reliability, release-validation]
dependencies: ["001"]
---

# Remediate Multi-Tenant Seeding Completion Reliability

## Problem Statement
Multi-tenant scenario seeding could appear stalled with ambiguous status (`INITIALIZING`) and no deterministic failure signal, while tenant-level counts remained below expected release targets.

## Acceptance Criteria
- [x] Multi-tenant seeding path is session-aware and reports progress using active session context.
- [x] Session lifecycle states transition deterministically for in-progress and failure outcomes.
- [x] Multi-tenant seeding fails fast when per-tenant output is incomplete.
- [x] Regression tests cover multi-tenant failure and status propagation paths.
- [x] Release validation includes tenant-scoped seeded-count gate using `verify-seeding-counts.sh`.

## Work Log

### 2026-03-06 - Implemented

**By:** Codex

**Actions:**
- Updated multi-tenant seeding to pass `sessionId` to downstream cohort generation and aggregate tenant-level progress/counts.
- Added incomplete-output guard (`expected patients vs persisted patients`) in `MultiTenantStrategy` to fail deterministically.
- Extended `DemoSession` lifecycle with `RUNNING`, `FAILED`, and `CANCELLED` states.
- Updated `ScenarioLoaderService` to set running/failed session states and to call seeding with explicit completion-stage handling.
- Updated `DemoSeedingService` status reporting to prefer live progress stage and exposed progress update helpers for orchestrators.
- Added retry/backoff boundaries for downstream FHIR and care-gap POST operations.
- Added release validation script `scripts/release-validation/validate-demo-seeding-counts.sh` and wired count gating into local CI/demo validation flows.
- Added/updated unit tests:
  - `MultiTenantStrategyTest`
  - `ScenarioLoaderServiceTest`
  - `DemoSeedingServiceTest`

**Validation:**
- `./backend/gradlew -p backend :modules:services:demo-seeding-service:test --tests '*ScenarioLoaderServiceTest' --tests '*MultiTenantStrategyTest' --tests '*DemoSeedingServiceTest'`
- Result: 22 passed, 0 failed.
- `VERSION=v0.0.0-test TENANTS=summit-care-2026,valley-health-2026 EXPECTED_PATIENTS_BY_TENANT=summit-care-2026=1200,valley-health-2026=1200 ./scripts/release-validation/validate-demo-seeding-counts.sh`
- Result: expected non-zero gate while seed baseline remains below target; report generated at `docs/releases/v0.0.0-test/validation/demo-seeding-count-validation-report.md`.

**Learnings:**
- Primary stall perception came from session/progress orchestration gaps (missing session-aware seeding path), not only from raw data generation speed.
