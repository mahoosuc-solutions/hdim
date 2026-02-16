# Performance Baselines and Latency Targets

This document captures baseline outcomes and latency targets for critical platform endpoints.

## Baseline Environment

- Gateway URL: `http://localhost:18080`
- Profile: demo/local compose
- Test harnesses:
  - `backend/testing/load-testing/*`
  - `backend/performance-tests/k6/*`

## Endpoint Latency Targets

| Endpoint | Use Case | P95 Target | P99 Target | Error Rate Target |
|---|---|---:|---:|---:|
| `/api/auth/login` | User authentication | <= 500 ms | <= 1000 ms | < 1% |
| `/api/v1/patients/search` | Patient lookup | <= 750 ms | <= 1200 ms | < 2% |
| `/api/v1/quality/evaluate` | Measure evaluation | <= 1200 ms | <= 2000 ms | < 2% |
| `/fhir/Patient` and core FHIR reads | Interoperability | <= 900 ms | <= 1500 ms | < 2% |
| `/api/v1/care-gaps` | Care gap workflows | <= 1000 ms | <= 1600 ms | < 2% |

## Baseline Summary

- Existing baseline executions are documented under:
  - `backend/testing/load-testing/BASELINE_RESULTS.md`
  - `docs/PERFORMANCE_BASELINE_RESULTS.md`
  - `docs/Q1_2026_TESTING_BASELINE_EXECUTION_SUMMARY.md`
- Current baseline trend indicates gateway health and authentication paths are within target under normal local load.

## Re-Execution Procedure

1. Start the stack with `docker compose -f docker-compose.demo.yml up -d`.
2. Run shell-based baselines via `backend/testing/load-testing/run-load-tests.sh`.
3. Run k6 scenarios:
   - `k6 run backend/performance-tests/k6/auth-flow.js`
   - `k6 run backend/performance-tests/k6/patient-search.js`
   - `k6 run backend/performance-tests/k6/quality-measure-evaluation.js`
   - `k6 run backend/performance-tests/k6/fhir-resources.js`
   - `k6 run backend/performance-tests/k6/care-gap-detection.js`
4. Save output artifacts to `backend/testing/load-testing/results/<timestamp>/`.

## Exit Criteria

- All five target endpoints meet P95 and P99 targets.
- Error rate remains below threshold for each scenario.
- Any regression >= 20% from prior baseline requires triage before release.
