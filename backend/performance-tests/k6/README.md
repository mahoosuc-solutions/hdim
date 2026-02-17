# k6 Performance Scenarios

This directory contains the five baseline k6 scenarios required for Phase 4 performance validation:

1. `auth-flow.js`
2. `patient-search.js`
3. `quality-measure-evaluation.js`
4. `fhir-resources.js`
5. `care-gap-detection.js`

## Usage

```bash
k6 run backend/performance-tests/k6/auth-flow.js
k6 run backend/performance-tests/k6/patient-search.js
k6 run backend/performance-tests/k6/quality-measure-evaluation.js
k6 run backend/performance-tests/k6/fhir-resources.js
k6 run backend/performance-tests/k6/care-gap-detection.js
```

Optional environment variables:

- `BASE_URL` (default: `http://localhost:18080`)
- `TENANT_ID` (default: `TENANT001`)
- `TEST_USERNAME` / `TEST_PASSWORD` (auth scenario)
- `PATIENT_ID` (quality scenario)

## Dedicated Patient Search Load Test (Issue #273)

This scenario matches the explicit target profile from issue #273:

- Target load: `100 requests/second`
- Duration: `10 minutes` total
- Ramp-up: `2 minutes`
- Thresholds:
  - `p95 < 200ms`
  - `0% error rate`

Run command:

```bash
k6 run backend/performance-tests/k6/patient-search-load-test.js
```
