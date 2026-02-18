# Service Validation Framework — Design Document

**Date:** 2026-02-18
**Scope:** All 58 HDIM backend services
**Goal:** Validate every service against its defined and unique requirements before GA

---

## Problem Statement

Load testing has proven the 4 core demo-stack services are stable under load (99,925 requests, 0% HTTP errors, quality-measure P95=92ms SLO passing). However, 54 other services have no end-to-end validation beyond their unit/integration tests. Before GA, every service must be validated against its specific requirements: correct API contracts, HIPAA compliance, business logic correctness, and SLO targets.

---

## Approach: Option C — Hybrid k6 Contracts + Gradle Functional Tests

Three layers working together:

1. **Service Validation Registry** — single source of truth for all 58 services
2. **k6 Contract Smoke Suite** — fast HTTP contract validation for all services
3. **Gradle Functional Tests** — deep business logic validation for clinical-core services

---

## Layer 1: Service Validation Registry

**File:** `validation/services.yml`

A YAML catalog defining every service's identity and requirements:

```yaml
services:
  - name: patient-service
    port: 8084
    base_path: /patient/api/v1
    category: clinical-core
    slo:
      p95_ms: 200
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - Multi-tenant isolation (tenant A cannot see tenant B data)
      - HIPAA Cache-Control: no-store on all PHI responses
      - Pagination correct (page/size params respected)

  - name: fhir-service
    port: 8085
    base_path: /fhir/api/v1
    category: clinical-core
    slo:
      p95_ms: 500
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - FHIR R4 Bundle structure valid (resourceType, entry[])
      - Patient/$everything returns all 14 resource types
      - Content-Type application/fhir+json

  - name: cql-engine-service
    port: 8081
    base_path: /cql/api/v1
    category: clinical-core
    slo:
      p95_ms: 2000
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - CQL measure evaluates to known result for test patient
      - HEDIS measure library loads without error
      - Evaluation idempotent (same input → same output)

  - name: hcc-service
    port: 8105
    base_path: /hcc/api/v1
    category: clinical-core
    slo:
      p95_ms: 500
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - HCC risk score in valid range (0.0–10.0)
      - RAF score computed for test cohort
      - Hierarchical condition categories applied correctly

  - name: care-gap-service
    port: 8086
    base_path: /care-gap/api/v1
    category: clinical-core
    slo:
      p95_ms: 200
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - OPEN/CLOSED gap status transitions correctly
      - Care gap closure audited
      - Gaps filtered by measure and status

  - name: quality-measure-service
    port: 8087
    base_path: /quality-measure/api/v1
    category: clinical-core
    slo:
      p95_ms: 200
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - HEDIS numerator/denominator counts correct for test cohort
      - Measure score (0–100) within expected range
      - Multi-tenant measure isolation

  - name: audit-query-service
    port: 8088
    base_path: /audit/api/v1
    category: clinical-core
    slo:
      p95_ms: 500
      error_rate_pct: 1
    tier: smoke+functional
    unique_requirements:
      - PHI access events queryable within 1s of write
      - Audit log immutable (no delete/update endpoints)
      - Events queryable by user, resource, time range

  - name: cql-engine-service
    port: 8081
    base_path: /cql/api/v1
    category: clinical-core
    tier: smoke+functional

  # ... (remaining 50 services cataloged as smoke-only initially)
```

The registry drives both test layers — no service requirements defined outside this file.

---

## Layer 2: k6 Contract Smoke Suite

**Location:** `load-tests/scenarios/smoke/<service-name>.js`
**Runner:** `load-tests/run-smoke-all.sh`
**Profile:** 1 VU × 3 iterations (~10s per service, ~10 min total for 58 services)

### What Each Smoke Test Checks

Every service smoke test validates:

| Check | Description |
|-------|-------------|
| HTTP status | Primary endpoints return 200 (or 201/204 as appropriate) |
| Response body | Non-empty, parseable JSON |
| Cache-Control | `no-store` or `no-cache` on PHI endpoints |
| Tenant isolation | `X-Tenant-ID` header respected (wrong tenant returns 403/empty) |
| Response time | P95 < service SLO from registry |

### Auth Pattern (already proven)
- Gateway-trust headers: `X-Auth-Validated`, `X-Auth-User-Id`, `X-Auth-Roles`, `X-Tenant-ID`
- mTLS: client cert extracted from running container, cached at `/tmp/hdim-certs/`
- Same pattern as existing `load-tests/config/auth.js`

### Example: fhir-service smoke test

```javascript
// load-tests/scenarios/smoke/fhir-service.js
import { check } from 'k6';
import { getAuthHeaders } from '../../config/auth.js';
import { BASE_URL_FHIR } from '../../config/endpoints.js';

export const options = {
  vus: 1,
  iterations: 3,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const headers = getAuthHeaders();
  const r = http.get(`${BASE_URL_FHIR}/fhir/api/v1/Patient`, { headers });

  check(r, {
    'status 200': (r) => r.status === 200,
    'FHIR Bundle resourceType': (r) => JSON.parse(r.body).resourceType === 'Bundle',
    'Cache-Control no-store': (r) => r.headers['Cache-Control']?.includes('no-store'),
    'response time OK': (r) => r.timings.duration < 500,
  });
}
```

---

## Layer 3: Gradle Functional Tests

**Command:** `./gradlew testIntegration` (existing — extended, not replaced)

For 15 clinical-core services, existing Spring integration tests are extended with requirement-specific assertions from the registry:

| Service | Unique Functional Requirement Tested |
|---------|--------------------------------------|
| `fhir-service` | FHIR R4 Bundle structure; Patient/$everything returns all 14 resource types |
| `cql-engine-service` | CQL evaluates to known result for test patient; idempotent evaluation |
| `hcc-service` | HCC score in range 0.0–10.0; RAF calculation correct |
| `care-gap-service` | OPEN→CLOSED transition; closure audited; filter by measure/status |
| `quality-measure-service` | HEDIS numerator/denominator counts; score range 0–100 |
| `audit-query-service` | PHI event queryable within 1s; immutability (no delete) |
| `consent-service` | Non-consented tenant blocked; consent revocation effective immediately |
| `patient-service` | Tenant A cannot access tenant B data (403/empty) |
| `cqrs-query-service` | Read model reflects write within eventual consistency window |
| `event-store-service` | Events immutable; replay produces same projected state |
| `data-ingestion-service` | Ingested record appears in patient-service within 5s |
| `notification-service` | DLT receives message after 2 retry failures |
| `cms-connector-service` | CMS data mapped to FHIR R4 format correctly |
| `clinical-workflow-service` | Workflow state machine transitions valid |
| `analytics-service` | Aggregate query returns consistent results |

---

## Error Surfacing (Three Layers)

### 1. Terminal — Immediate Feedback
Colored pass/fail table printed after each service completes:

```
Service Validation Results — 2026-02-18T14:23:01Z
══════════════════════════════════════════════════
✅ patient-service        smoke  3/3 checks   P95=81ms
✅ fhir-service           smoke  3/3 checks   P95=145ms
❌ cql-engine-service     smoke  FAIL: FHIR Bundle resourceType missing
                                 Response: {"error":"not found"} (HTTP 404)
✅ care-gap-service       smoke  3/3 checks   P95=139ms
...
══════════════════════════════════════════════════
PASSED: 9/10  FAILED: 1/10
```

Failures show: service name, check that failed, HTTP status, response body snippet (first 200 chars, PHI-safe).

### 2. Results File — Persistent Record
Every run writes `validation/results/YYYY-MM-DD-HHMMSS-smoke.md` — same pattern as load test results. Contains:
- Full pass/fail table
- Failing check details with response excerpts
- DB counts snapshot
- Git commit hash

Committed to repo after review (same workflow as load test results).

### 3. GitHub Actions Annotations — CI Integration (Phase 4)
When wired into CI, failed smoke checks become inline PR annotations:
- Annotation points to the specific service file
- Shows the failing check name and response
- Blocks merge if any `clinical-core` service fails
- `supporting` services produce warnings, not blockers

---

## Phased Execution Plan

### Phase 1 — Demo Stack (this sprint)
- Build `validation/services.yml` with all 58 services cataloged
- Write smoke tests for 10 demo-stack services (patient, fhir, cql-engine, care-gap, quality-measure, audit-query, hcc, events, seeding)
- Build `run-smoke-all.sh` with terminal + results file output
- Run, document, commit results

### Phase 2 — Functional Depth (next sprint)
- Extend Gradle integration tests for 15 clinical-core services
- Add functional requirement assertions from registry
- Run `testIntegration`, update results

### Phase 3 — All 58 Services
- Write smoke test files for remaining 48 services
- Run as containers become available locally
- Registry marks untestable services as `pending-container`

### Phase 4 — CI Integration
- Wire `run-smoke-all.sh` into GitHub Actions nightly job
- Add PR annotation output (k6 JUnit XML → GitHub annotations)
- `clinical-core` failures block merge; `supporting` produce warnings

---

## File Structure

```
validation/
  services.yml                          # Service registry (source of truth)
  results/
    2026-02-18-140000-smoke.md          # Per-run results
    ...

load-tests/
  scenarios/
    smoke/                              # NEW: one file per service
      patient-service.js
      fhir-service.js
      cql-engine-service.js
      ... (58 total)
    patient-service.js                  # Existing load tests (unchanged)
    care-gap-service.js
    ...
  run-smoke-all.sh                      # NEW: runs all smoke tests, outputs table

backend/modules/services/
  fhir-service/.../test/               # Existing — extended with functional reqs
  cql-engine-service/.../test/
  ...

.github/workflows/
  service-validation.yml               # Phase 4: nightly CI job
```

---

## Success Criteria

| Milestone | Criteria |
|-----------|----------|
| Phase 1 complete | 10/10 demo-stack services pass smoke; results file committed |
| Phase 2 complete | 15/15 clinical-core services pass functional tests |
| Phase 3 complete | All 58 services cataloged; smoke tests written for all |
| Phase 4 complete | CI nightly job green; PR annotations wired |
| GA ready | Zero failures on `clinical-core` services; all SLOs documented |
