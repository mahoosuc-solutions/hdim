# Wave-1 Adapter Perf/Security Gate Report (2026-02-28)

## Scope

- Revenue clearinghouse adapter hardening path (`#506`)
- Wave-1 evidence tracking (`#502`)

## Security Gate

- Command:
  - `bash scripts/validation/validate-compliance-evidence-gate.sh`
- Result: `PASS`
- Artifact:
  - `validation-reports/compliance-evidence-gate-summary.md`
  - `validation-reports/compliance-evidence-gate-summary.json`

## Containerized Adapter Validation

- Command:
  - `./gradlew :modules:services:payer-workflows-service:test --tests com.healthdata.payer.service.RestClearinghouseSubmissionAdapterContainerTest`
- Result: `2 passed, 0 failed`
- Notes:
  - Uses Testcontainers with deployed `wiremock/wiremock:3.13.1` container.
  - Validates real HTTP contract behavior (200 success mapping, 5xx retryable mapping).

## API Performance Gate

- Command:
  - `tests/performance/run-tests.sh api`
- Environment:
  - Demo stack up via `docker compose -f docker-compose.demo.yml up -d`
  - Gateway health check passing before test execution
- Result: `FAIL` (thresholds crossed)
- Observed metrics from run:
  - `Total Requests: 1966`
  - `RPS: 15.90`
  - `Avg Response: 4.44ms`
  - `P95 Response: 8.02ms`
  - `Error Rate: 33.49%`
  - `Checks Passed: 87.44%`
- Performance artifacts:
  - `reports/api-gateway-performance_20260227_195611.json`
  - `reports/api-gateway-summary.json`

## Harness Adjustments Applied

- Fixed k6 summary robustness and output path in:
  - `tests/performance/api-gateway-performance.js`
- Why:
  - Prevents summary crashes when some metrics are absent.
  - Ensures summary output writes to mounted report path in Docker execution.

## Gate Decision

- Security evidence gate: **PASS**
- Containerized adapter contract gate: **PASS**
- Performance gate: **FAIL** (functional/load behavior not yet within current k6 thresholds)
- Overall: **Conditional GO for continued hardening; NO-GO for production performance claims until k6 threshold failures are resolved**
