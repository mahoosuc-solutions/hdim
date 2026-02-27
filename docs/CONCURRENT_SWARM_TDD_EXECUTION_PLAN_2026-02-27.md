# Concurrent Swarm TDD Execution Plan

**Date:** 2026-02-27  
**Scope:** Open wave issues `#276-#285` and `#36`  
**Objective:** Execute issue closure via concurrent SWE + QA + Compliance agents using incremental TDD with evidence-backed validation.

---

## 1. Swarm Topology

1. **Orchestrator Agent**
- Owns sequencing, dependency management, and wave readiness sign-off.
- Enforces issue closure checklist and evidence attachment.

2. **Wave SWE Agents (parallel)**
- `SWE-W0`: `#285`, `#36`
- `SWE-W1`: `#277`, `#276`
- `SWE-W2`: `#282`, `#278`, `#281`, `#279`
- `SWE-W3`: `#283`, `#284`, `#280`

3. **QA Agent**
- Runs unit/integration/e2e matrix after each merge train.
- Blocks closure on failing gates.

4. **Compliance Agent**
- Runs HIPAA/SOC2 readiness checks.
- Maintains evidence docs and cutoff go/no-go artifacts.

---

## 2. Incremental TDD Workflow (per issue)

1. **Red**
- Add/extend failing test for the issue acceptance criterion.
- Prefer service-level tests + API contract tests first.

2. **Green**
- Implement minimal code to pass.
- Keep feature-flag + rollback path if cross-service.

3. **Refactor**
- Remove duplication, align naming, preserve passing tests.

4. **Evidence**
- Attach test logs + validation outputs to issue comment.
- Update compliance evidence docs for wave-level changes.

---

## 3. Issue-to-Code/Data Model Mapping (Execution Targets)

- `#277 TEFCA/HIE`: `backend/modules/services/ehr-connector-service/**` and connection DTO/config validation.
- `#276 Revenue Cycle`: `backend/modules/services/payer-workflows-service/**`, `apps/clinical-portal/src/app/pages/phase2-execution/**`.
- `#282 CMS Dashboard`: `backend/modules/services/analytics-service/**`, `backend/modules/services/quality-measure-service/**`.
- `#278 Price Transparency`: `backend/modules/services/cost-analysis-service/**`.
- `#281 Attribution` + `#280 Credentialing`: `backend/modules/services/patient-service/**`, `backend/modules/services/quality-measure-service/**`.
- `#279 Utilization/Case Mgmt`: `backend/modules/services/care-gap-service/**`, care-gap portal pages.
- `#283/#284 Analytics`: analytics service dashboards + metric snapshots.
- `#285/#36`: roadmap + demo/storyboard artifacts (`docs/product/**`, `apps/clinical-portal/public/demo/**`).

---

## 4. Required Validation Gates

## Unit
- `npm run test`
- `./backend/gradlew test --no-daemon`

## Integration/Security/Config
- `npm run test:mcp`
- `bash scripts/validation/validate-data-access-security.sh`
- `./validate-system.sh`
- `bash scripts/security/validate-phase4-hipaa-controls.sh`

## Data Model
- Direct DB inventory snapshot via `docker exec hdim-demo-postgres psql ...`
- `bash scripts/validate-database-schema.sh`  
  - strict mode available: `STRICT_SCHEMA=1 bash scripts/validate-database-schema.sh`

## Release/Compliance
- `node scripts/mcp/operator-go-no-go.mjs --mode strict --profile production`

---

## 5. Baseline Execution Results (2026-02-27)

## Passed
- `npm run test:mcp` (`test-results/tdd-baseline-test-mcp-2026-02-27.log`)
- `npm run test` (`test-results/tdd-baseline-npm-test-2026-02-27.log`)
- `bash scripts/security/validate-phase4-hipaa-controls.sh` (`test-results/tdd-baseline-hipaa-controls-2026-02-27.log`)
- `bash scripts/validation/validate-data-access-security.sh` (`test-results/tdd-baseline-data-access-security-2026-02-27.log`)
- `./validate-system.sh` (`test-results/tdd-baseline-validate-system-2026-02-27.log`)

## Failed
- `./backend/gradlew test --no-daemon` (`test-results/tdd-baseline-gradle-test-2026-02-27.log`)
  - `analytics-service` failures:
    - `EntityMigrationValidationTest.initializationError` (multiple `@SpringBootConfiguration`)
    - `DashboardControllerIntegrationTest` context-load failures (3 tests)
  - `audit-query-service` failure:
    - `EntityMigrationValidationTest.initializationError` (Testcontainers Docker environment detection)

## Data Model Snapshot
- Database table counts recorded in:
  - `test-results/tdd-baseline-db-table-counts-2026-02-27.log`
- Key domain table checks confirmed present:
  - FHIR core: `patients`, `observations`, `conditions`, `encounters`, `medication_requests`, `allergy_intolerances`, `immunizations`
  - Patient/quality/care-gap core tables validated via direct SQL queries during baseline run.

---

## 6. Systematic Issue Closure Checklist

Each issue may be closed only when all pass:

1. Acceptance criteria implemented.
2. Red/Green/Refactor evidence captured in PR.
3. Unit + integration gates pass.
4. No regression in `test:mcp`, data-access security, HIPAA controls.
5. Data model impact validated (table/index checks or migration evidence).
6. Wave lead issue comment updated with:
- test log paths
- compliance artifacts
- residual risk (if any)

---

## 7. Immediate Next Fix Queue

1. Fix `analytics-service` test bootstrapping conflict (single SpringBoot test app configuration).
2. Stabilize `DashboardControllerIntegrationTest` context setup.
3. Fix `audit-query-service` `EntityMigrationValidationTest` Testcontainers/Docker detection.
4. Re-run `./backend/gradlew test --no-daemon` and attach clean result log to wave lead issues.
