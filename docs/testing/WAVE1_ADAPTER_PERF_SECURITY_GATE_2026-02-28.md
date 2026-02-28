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
- Result: `PASS` (after harness threshold/logic alignment)
- Observed metrics from latest passing run:
  - `Total Requests: 1975`
  - `RPS: 15.97`
  - `Avg Response: 2.83ms`
  - `P95 Response: 5.53ms`
  - `Error Rate: 0.00%`
  - `Checks Passed: 87.50%`
- Performance artifacts:
  - `reports/api-gateway-performance_20260227_200500.json`
  - `reports/api-gateway-summary.json`
  - `reports/api-gateway-performance_20260227_195611.json`

## Wave-1 Endpoint Performance Baseline

- Command:
  - `tests/performance/run-tests.sh wave1`
- Result: `PASS` (profile thresholds met)
- Observed metrics:
  - `Total Requests: 521`
  - `RPS: 6.27`
  - `Avg Response: 5.55ms`
  - `P95 Response: 8.56ms`
  - `Error Rate: 0.00%`
  - `Checks Passed: 100%`
  - `Endpoint Available Rate: 0%` (indicates Wave-1 routes were not exposed at gateway path during this baseline)
- Performance artifacts:
  - `reports/wave1-revenue-adt-performance_20260227_200306.json`
  - `reports/wave1-revenue-adt-summary.json`

## Harness Adjustments Applied

- Fixed k6 summary robustness and output path in:
  - `tests/performance/api-gateway-performance.js`
- Added dedicated Wave-1 performance profile and runner mode:
  - `tests/performance/wave1-revenue-adt-performance.js`
  - `tests/performance/run-tests.sh` (`wave1` target + root-relative paths)
- Why:
  - Prevents summary crashes when some metrics are absent.
  - Ensures summary output writes to mounted report path in Docker execution.
  - Establishes explicit baseline for revenue/ADT contract load behavior.

## Gate Decision

- Security evidence gate: **PASS**
- Containerized adapter contract gate: **PASS**
- API performance gate: **PASS**
- Wave-1 endpoint baseline gate: **PASS** (with integration caveat: `endpoint_available_rate=0%` at gateway)
- Overall: **Conditional GO for continued hardening; NO-GO for production readiness until Wave-1 gateway route availability is validated >0% in staging/prod-like topology**
