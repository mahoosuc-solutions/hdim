# Service Validation Framework — Phase 1 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a service validation registry + k6 contract smoke suite for the 10 services currently running in the demo stack, with terminal pass/fail output and persistent results files.

**Architecture:** Three artifacts — (1) `validation/services.yml` as the single source of truth for all service requirements, (2) one k6 smoke scenario per service in `load-tests/scenarios/smoke/`, (3) `load-tests/run-smoke-all.sh` that runs all scenarios and writes a structured markdown results file. Reuses existing mTLS cert extraction and auth header patterns from `run-demo-load-tests.sh`.

**Tech Stack:** k6 v0.54.0 (at `/tmp/k6-v0.54.0-linux-amd64/k6` or on PATH), bash, YAML, existing `load-tests/config/tls.js` + `options.js` helpers.

---

## Demo Stack Service Map

| Container | Port | Context Path | Primary Endpoint |
|-----------|------|-------------|-----------------|
| hdim-demo-patient | 8084 | `/patient` | `GET /patient/api/v1/patients` |
| hdim-demo-fhir | 8085 | `/fhir` | `GET /fhir/Patient` |
| hdim-demo-care-gap | 8086 | `/care-gap` | `GET /care-gap/api/v1/care-gaps` |
| hdim-demo-quality-measure | 8087 | `/quality-measure` | `GET /quality-measure/api/v1/measures/results` |
| hdim-demo-audit-query | 8088 | `/` (root) | `GET /api/v1/audit/logs/statistics` |
| hdim-demo-hcc | 8105 | `/hcc` | `GET /hcc/api/v1/hcc/crosswalk` |
| hdim-demo-cql-engine | 8081 | `/cql-engine` | `GET /cql-engine/api/v1/cql/evaluations` |
| hdim-demo-events | 8083 | `/events` | `GET /events/actuator/health` |
| hdim-demo-seeding | 8098 | `/` | `GET /actuator/health` |
| hdim-demo-postgres | 5435 | — | DB only (no smoke test) |

---

## Task 1: Create the Service Validation Registry

**Files:**
- Create: `validation/services.yml`
- Create: `validation/README.md`

**Step 1: Create `validation/services.yml`**

```yaml
# HDIM Service Validation Registry
# Source of truth for all service requirements, SLOs, and test tiers.
#
# tier:
#   smoke-only       — HTTP contract checks only (1 VU × 3 iterations)
#   smoke+functional — Contract checks + Gradle integration tests

services:
  # ── Clinical Core ────────────────────────────────────────────────────────────

  - name: patient-service
    port: 8084
    context_path: /patient
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 200
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /patient/api/v1/patients?page=0&size=20
        expect_status: 200
        expect_body_field: content
        check_hipaa_headers: true
    unique_requirements:
      - Multi-tenant isolation — tenant A cannot see tenant B data
      - HIPAA Cache-Control no-store on all PHI responses
      - Pagination correct (page/size params respected)

  - name: fhir-service
    port: 8085
    context_path: /fhir
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 500
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /fhir/Patient
        expect_status: 200
        expect_body_field: resourceType
        expect_body_value: Bundle
        accept_header: application/fhir+json
        check_hipaa_headers: true
    unique_requirements:
      - FHIR R4 Bundle resourceType = "Bundle" in all list responses
      - Patient/$everything returns all 14 resource types
      - Content-Type application/fhir+json on FHIR endpoints

  - name: care-gap-service
    port: 8086
    context_path: /care-gap
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 200
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /care-gap/api/v1/care-gaps?page=0&size=20
        expect_status: 200
        expect_body_field: content
        check_hipaa_headers: true
    unique_requirements:
      - OPEN/CLOSED gap status transitions correctly
      - Care gap closure creates an audit event
      - Gaps filterable by measure and status

  - name: quality-measure-service
    port: 8087
    context_path: /quality-measure
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 200
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /quality-measure/api/v1/measures/results?page=0&size=20
        expect_status: 200
        expect_body_field: content
        check_hipaa_headers: true
    unique_requirements:
      - HEDIS numerator/denominator counts correct for test cohort
      - Measure score (0–100) within expected range
      - Multi-tenant measure isolation

  - name: audit-query-service
    port: 8088
    context_path: /
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 500
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /api/v1/audit/logs/statistics
        expect_status: 200
        check_hipaa_headers: true
    unique_requirements:
      - PHI access events queryable within 1s of write
      - Audit log immutable — no delete or update endpoints exposed
      - Events queryable by user, resource type, and time range

  - name: hcc-service
    port: 8105
    context_path: /hcc
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 500
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /hcc/api/v1/hcc/crosswalk
        expect_status: 200
        check_hipaa_headers: false
    unique_requirements:
      - HCC risk score in valid range (0.0–10.0)
      - RAF score computed for test cohort
      - Hierarchical condition categories applied correctly

  - name: cql-engine-service
    port: 8081
    context_path: /cql-engine
    category: clinical-core
    tier: smoke+functional
    slo:
      p95_ms: 2000
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /cql-engine/api/v1/cql/evaluations
        expect_status: 200
        check_hipaa_headers: false
    unique_requirements:
      - CQL measure evaluates to known result for test patient
      - HEDIS measure library loads without error
      - Evaluation idempotent (same input → same output)

  # ── Supporting / Infrastructure ──────────────────────────────────────────────

  - name: events-service
    port: 8083
    context_path: /events
    category: supporting
    tier: smoke-only
    slo:
      p95_ms: 1000
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /events/actuator/health
        expect_status: 200
        check_hipaa_headers: false
    unique_requirements:
      - Health endpoint returns UP status
      - Kafka consumer group registered and lag = 0 at rest

  - name: demo-seeding-service
    port: 8098
    context_path: /
    category: supporting
    tier: smoke-only
    slo:
      p95_ms: 2000
      error_rate_pct: 1
    smoke_endpoints:
      - method: GET
        path: /actuator/health
        expect_status: 200
        check_hipaa_headers: false
    unique_requirements:
      - Health endpoint returns UP status
      - Demo session status = READY after seeding
```

**Step 2: Create `validation/README.md`**

```markdown
# HDIM Service Validation Registry

`services.yml` is the single source of truth for all 58 HDIM service requirements,
SLO targets, and test coverage tiers.

## Usage

Run smoke validation against the demo stack:
```bash
./load-tests/run-smoke-all.sh
```

## Tiers

| Tier | What runs |
|------|-----------|
| `smoke-only` | k6 HTTP contract checks (1 VU × 3 iterations) |
| `smoke+functional` | Contract checks + `./gradlew testIntegration` (Phase 2) |

## Adding a New Service

Add an entry to `services.yml` following the existing pattern.
The smoke runner picks it up automatically on next run.
```

**Step 3: Commit**

```bash
git add validation/services.yml validation/README.md
git commit -m "feat(validation): add service validation registry — 9 demo-stack services cataloged"
```

---

## Task 2: Add Shared Smoke Config to k6

**Files:**
- Create: `load-tests/config/smoke.js`

**Step 1: Write the shared smoke options module**

```javascript
// load-tests/config/smoke.js
//
// Shared k6 options for service contract smoke tests.
// Profile: 1 VU × 3 iterations (~10-15s per service).
// Used by all scenarios in load-tests/scenarios/smoke/.

/**
 * k6 options for smoke contract tests.
 * Fast — 1 VU, 3 iterations, lenient thresholds.
 */
export const smokeContractOptions = {
  vus: 1,
  iterations: 3,
  thresholds: {
    // Smoke tests use a generous P95 — they're correctness checks, not perf checks.
    // Each service registry entry carries its own SLO; perf is validated in load tests.
    http_req_duration: ['p(95)<5000'],
    http_req_failed:   ['rate<0.01'],
  },
  tags: {
    environment: __ENV.ENVIRONMENT || 'demo',
    team:        'hdim-platform',
    test_type:   'smoke-contract',
  },
};
```

**Step 2: Commit**

```bash
git add load-tests/config/smoke.js
git commit -m "feat(validation): add shared smoke contract k6 options"
```

---

## Task 3: Write Smoke Tests — patient-service (already tested, new smoke format)

**Files:**
- Create: `load-tests/scenarios/smoke/patient-service.js`

**Step 1: Write the smoke scenario**

```javascript
// load-tests/scenarios/smoke/patient-service.js
//
// Contract smoke test for patient-service.
// Unique requirements:
//   - HTTP 200 on patient list endpoint
//   - Response body contains "content" field (paged response)
//   - Cache-Control: no-store on PHI response
//   - Tenant isolation: wrong tenant gets 403 or empty result
//
// Run:
//   ./load-tests/run-smoke-all.sh --service patient-service

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_PATIENT || 'https://localhost:8084';
const TENANT   = __ENV.TENANT_ID        || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // ── Check 1: Patient list returns 200 + paged body ──────────────────────────
  const r1 = http.get(`${BASE_URL}/patient/api/v1/patients?page=0&size=20`, { headers });
  check(r1, {
    '[patient] status 200':              (r) => r.status === 200,
    '[patient] body has content field':  (r) => JSON.parse(r.body).content !== undefined,
    '[patient] Cache-Control no-store':  (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[patient] response time < 5s':      (r) => r.timings.duration < 5000,
  });

  // ── Check 2: Tenant isolation — wrong tenant gets 403 or empty ──────────────
  const wrongTenantHeaders = getDemoAuthHeaders('wrong-tenant-that-does-not-exist');
  const r2 = http.get(`${BASE_URL}/patient/api/v1/patients?page=0&size=20`, { headers: wrongTenantHeaders });
  check(r2, {
    '[patient] wrong tenant: 403 or empty content': (r) => {
      if (r.status === 403) return true;
      try {
        const body = JSON.parse(r.body);
        return body.content && body.content.length === 0;
      } catch (_) { return false; }
    },
  });
}
```

**Step 2: Run it manually to verify**

```bash
TLS_CLIENT_CERT=$(docker exec hdim-demo-patient cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem)
TLS_CLIENT_KEY=$(docker exec hdim-demo-patient cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem)

/tmp/k6-v0.54.0-linux-amd64/k6 run \
  -e TLS_CLIENT_CERT="$TLS_CLIENT_CERT" \
  -e TLS_CLIENT_KEY="$TLS_CLIENT_KEY" \
  -e TENANT_ID="acme-health" \
  load-tests/scenarios/smoke/patient-service.js
```

Expected: all checks ✓, `http_req_failed: 0.00%`

**Step 3: Commit**

```bash
git add load-tests/scenarios/smoke/patient-service.js
git commit -m "feat(validation): smoke contract test for patient-service"
```

---

## Task 4: Write Smoke Tests — fhir-service

**Files:**
- Create: `load-tests/scenarios/smoke/fhir-service.js`

**Step 1: Write the smoke scenario**

```javascript
// load-tests/scenarios/smoke/fhir-service.js
//
// Contract smoke test for fhir-service.
// Unique requirements:
//   - FHIR R4 Bundle: resourceType = "Bundle"
//   - Content-Type: application/fhir+json
//   - Cache-Control: no-store on PHI responses
//   - Patient/$everything returns non-empty entry[]

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL   = __ENV.BASE_URL_FHIR || 'https://localhost:8085';
const TENANT     = __ENV.TENANT_ID     || 'acme-health';
const PATIENT_ID = __ENV.PATIENT_ID    || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

export default function () {
  const headers = {
    ...getDemoAuthHeaders(TENANT),
    Accept: 'application/fhir+json',
  };

  // ── Check 1: Patient list is a valid FHIR R4 Bundle ─────────────────────────
  const r1 = http.get(`${BASE_URL}/fhir/Patient`, { headers });
  check(r1, {
    '[fhir] Patient list status 200':         (r) => r.status === 200,
    '[fhir] resourceType = Bundle':            (r) => { try { return JSON.parse(r.body).resourceType === 'Bundle'; } catch(_) { return false; } },
    '[fhir] Cache-Control no-store':           (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[fhir] Content-Type fhir+json':           (r) => (r.headers['Content-Type'] || '').includes('fhir+json'),
    '[fhir] response time < 5s':               (r) => r.timings.duration < 5000,
  });

  // ── Check 2: Patient/$everything returns entry array ────────────────────────
  const r2 = http.get(`${BASE_URL}/fhir/Patient/${PATIENT_ID}/$everything`, { headers });
  check(r2, {
    '[$everything] status 200':               (r) => r.status === 200,
    '[$everything] resourceType = Bundle':    (r) => { try { return JSON.parse(r.body).resourceType === 'Bundle'; } catch(_) { return false; } },
    '[$everything] entry array present':      (r) => { try { return Array.isArray(JSON.parse(r.body).entry); } catch(_) { return false; } },
  });
}
```

**Step 2: Run and verify**

```bash
TLS_CLIENT_CERT=$(docker exec hdim-demo-patient cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem)
TLS_CLIENT_KEY=$(docker exec hdim-demo-patient cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem)

/tmp/k6-v0.54.0-linux-amd64/k6 run \
  -e TLS_CLIENT_CERT="$TLS_CLIENT_CERT" \
  -e TLS_CLIENT_KEY="$TLS_CLIENT_KEY" \
  -e TENANT_ID="acme-health" \
  -e BASE_URL_FHIR="https://localhost:8085" \
  load-tests/scenarios/smoke/fhir-service.js
```

Expected: all checks ✓

**Step 3: Commit**

```bash
git add load-tests/scenarios/smoke/fhir-service.js
git commit -m "feat(validation): smoke contract test for fhir-service (FHIR R4 Bundle + $everything)"
```

---

## Task 5: Write Smoke Tests — care-gap, quality-measure, audit-query

**Files:**
- Create: `load-tests/scenarios/smoke/care-gap-service.js`
- Create: `load-tests/scenarios/smoke/quality-measure-service.js`
- Create: `load-tests/scenarios/smoke/audit-query-service.js`

**Step 1: care-gap-service.js**

```javascript
// load-tests/scenarios/smoke/care-gap-service.js
import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_CARE_GAP || 'https://localhost:8086';
const TENANT   = __ENV.TENANT_ID         || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/care-gap/api/v1/care-gaps?page=0&size=20`, { headers });
  check(r, {
    '[care-gap] status 200':              (r) => r.status === 200,
    '[care-gap] body has content field':  (r) => { try { return JSON.parse(r.body).content !== undefined; } catch(_) { return false; } },
    '[care-gap] Cache-Control no-store':  (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[care-gap] response time < 5s':      (r) => r.timings.duration < 5000,
  });
}
```

**Step 2: quality-measure-service.js**

```javascript
// load-tests/scenarios/smoke/quality-measure-service.js
import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_QUALITY || 'https://localhost:8087';
const TENANT   = __ENV.TENANT_ID        || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/quality-measure/api/v1/measures/results?page=0&size=20`, { headers });
  check(r, {
    '[quality-measure] status 200':             (r) => r.status === 200,
    '[quality-measure] body has content field': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch(_) { return false; } },
    '[quality-measure] Cache-Control no-store': (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[quality-measure] response time < 5s':     (r) => r.timings.duration < 5000,
  });
}
```

**Step 3: audit-query-service.js**

```javascript
// load-tests/scenarios/smoke/audit-query-service.js
//
// Note: audit-query-service has context-path: / (root), unlike other services.
// Endpoints are at /api/v1/audit/...

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_AUDIT || 'https://localhost:8088';
const TENANT   = __ENV.TENANT_ID      || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // Statistics endpoint — no PHI, returns aggregate counts
  const r = http.get(`${BASE_URL}/api/v1/audit/logs/statistics`, { headers });
  check(r, {
    '[audit-query] status 200':         (r) => r.status === 200,
    '[audit-query] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
```

**Step 4: Commit**

```bash
git add load-tests/scenarios/smoke/care-gap-service.js \
        load-tests/scenarios/smoke/quality-measure-service.js \
        load-tests/scenarios/smoke/audit-query-service.js
git commit -m "feat(validation): smoke contract tests for care-gap, quality-measure, audit-query"
```

---

## Task 6: Write Smoke Tests — hcc, cql-engine, events, seeding

**Files:**
- Create: `load-tests/scenarios/smoke/hcc-service.js`
- Create: `load-tests/scenarios/smoke/cql-engine-service.js`
- Create: `load-tests/scenarios/smoke/events-service.js`
- Create: `load-tests/scenarios/smoke/demo-seeding-service.js`

**Step 1: hcc-service.js**

```javascript
// load-tests/scenarios/smoke/hcc-service.js
import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_HCC || 'https://localhost:8105';
const TENANT   = __ENV.TENANT_ID    || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // Crosswalk is a reference data endpoint — no PHI, safe to smoke
  const r = http.get(`${BASE_URL}/hcc/api/v1/hcc/crosswalk`, { headers });
  check(r, {
    '[hcc] crosswalk status 200':      (r) => r.status === 200,
    '[hcc] body is non-empty':         (r) => r.body && r.body.length > 2,
    '[hcc] response time < 5s':        (r) => r.timings.duration < 5000,
  });
}
```

**Step 2: cql-engine-service.js**

```javascript
// load-tests/scenarios/smoke/cql-engine-service.js
import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_CQL || 'https://localhost:8081';
const TENANT   = __ENV.TENANT_ID    || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // List evaluations — GET, returns paged results or empty array
  const r = http.get(`${BASE_URL}/cql-engine/api/v1/cql/evaluations`, { headers });
  check(r, {
    '[cql-engine] status 200':         (r) => r.status === 200,
    '[cql-engine] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
```

**Step 3: events-service.js**

```javascript
// load-tests/scenarios/smoke/events-service.js
//
// Events service is Kafka-backed — only the actuator health endpoint
// is accessible via HTTP. Kafka consumer health is validated separately.

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_EVENTS || 'https://localhost:8083';
const TENANT   = __ENV.TENANT_ID       || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/events/actuator/health`, { headers });
  check(r, {
    '[events] health status 200':      (r) => r.status === 200,
    '[events] status UP':              (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch(_) { return false; } },
    '[events] response time < 5s':     (r) => r.timings.duration < 5000,
  });
}
```

**Step 4: demo-seeding-service.js**

```javascript
// load-tests/scenarios/smoke/demo-seeding-service.js
//
// Demo seeding service runs on HTTP (port 8098), not mTLS.
// Uses plain HTTP GET — no TLS options needed.

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions; // No TLS

const BASE_URL = __ENV.BASE_URL_SEEDING || 'http://localhost:8098';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[seeding] health status 200':  (r) => r.status === 200,
    '[seeding] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch(_) { return false; } },
    '[seeding] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
```

**Step 5: Commit**

```bash
git add load-tests/scenarios/smoke/hcc-service.js \
        load-tests/scenarios/smoke/cql-engine-service.js \
        load-tests/scenarios/smoke/events-service.js \
        load-tests/scenarios/smoke/demo-seeding-service.js
git commit -m "feat(validation): smoke contract tests for hcc, cql-engine, events, demo-seeding"
```

---

## Task 7: Build the Smoke Runner (`run-smoke-all.sh`)

**Files:**
- Create: `load-tests/run-smoke-all.sh`
- Create: `validation/results/.gitkeep`

**Step 1: Create the runner**

```bash
#!/usr/bin/env bash
# =============================================================================
# run-smoke-all.sh — HDIM Service Validation Smoke Runner
# =============================================================================
#
# Runs k6 contract smoke tests against all demo-stack services.
# Outputs: colored terminal table + validation/results/YYYY-MM-DD-HHmmss-smoke.md
#
# Usage:
#   ./load-tests/run-smoke-all.sh                        # All services
#   ./load-tests/run-smoke-all.sh --service fhir-service # One service
#
# Prerequisites:
#   - Demo containers running (hdim-demo-patient, etc.)
#   - k6 at /tmp/k6-v0.54.0-linux-amd64/k6 or on PATH
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${REPO_ROOT}/validation/results"
TIMESTAMP="$(date +%Y-%m-%d-%H%M%S)"
RESULTS_FILE="${RESULTS_DIR}/${TIMESTAMP}-smoke.md"
SERVICE_FILTER=""
EXIT_CODE=0

# ── Parse args ────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) SERVICE_FILTER="${2:-}"; shift 2 ;;
    --help|-h) sed -n '2,20p' "$0" | sed 's/^# //'; exit 0 ;;
    *) echo "Unknown: $1. Use --help." >&2; exit 1 ;;
  esac
done

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; RESET='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${RESET}  $*"; }
log_ok()      { echo -e "${GREEN}[PASS]${RESET}  $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${RESET}  $*"; }
log_section() { echo -e "\n${BOLD}${BLUE}━━━ $* ━━━${RESET}\n"; }

# ── Find k6 ──────────────────────────────────────────────────────────────────
find_k6() {
  if command -v k6 &>/dev/null; then echo "k6"
  elif [[ -x /tmp/k6-v0.54.0-linux-amd64/k6 ]]; then echo "/tmp/k6-v0.54.0-linux-amd64/k6"
  else log_error "k6 not found."; exit 1; fi
}

K6="$(find_k6)"

# ── Extract mTLS certs (same pattern as run-demo-load-tests.sh) ───────────────
extract_certs() {
  local container="hdim-demo-patient"
  if ! docker ps --format "{{.Names}}" | grep -q "^${container}$"; then
    log_error "Container ${container} not running. Start demo stack first."
    exit 1
  fi
  TLS_CLIENT_CERT="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem)"
  TLS_CLIENT_KEY="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem)"
  export TLS_CLIENT_CERT TLS_CLIENT_KEY
  log_ok "mTLS certificates extracted"
}

# ── Run one smoke scenario ────────────────────────────────────────────────────
# Returns: 0=pass, 1=fail
# Appends result line to RESULTS array
declare -a RESULTS=()
declare -a FAILURES=()

run_smoke() {
  local service="$1"
  local script="$2"
  local base_url_var="$3"    # e.g. BASE_URL_PATIENT
  local base_url_val="$4"    # e.g. https://localhost:8084

  if [[ -n "${SERVICE_FILTER}" && "${service}" != "${SERVICE_FILTER}" ]]; then
    return 0
  fi

  if [[ ! -f "${SCRIPT_DIR}/scenarios/smoke/${service}.js" ]]; then
    log_warn "No smoke test found for ${service} — skipping"
    RESULTS+=("⏭  ${service} | SKIPPED | no smoke test file")
    return 0
  fi

  local tmp_out
  tmp_out="$(mktemp /tmp/hdim-smoke-XXXXXX.txt)"

  local k6_args=(
    "${K6}" run
    --quiet
    -e "TLS_CLIENT_CERT=${TLS_CLIENT_CERT}"
    -e "TLS_CLIENT_KEY=${TLS_CLIENT_KEY}"
    -e "TENANT_ID=${TENANT_ID:-acme-health}"
    -e "PATIENT_ID=${PATIENT_ID:-f47ac10b-58cc-4372-a567-0e02b2c3d479}"
    -e "${base_url_var}=${base_url_val}"
    "${SCRIPT_DIR}/scenarios/smoke/${service}.js"
  )

  local start_ms duration_ms
  start_ms="$(date +%s%3N)"

  if "${k6_args[@]}" > "${tmp_out}" 2>&1; then
    duration_ms=$(( $(date +%s%3N) - start_ms ))
    log_ok "${service} — ${duration_ms}ms"
    RESULTS+=("✅ ${service} | PASS | ${duration_ms}ms")
    rm -f "${tmp_out}"
    return 0
  else
    duration_ms=$(( $(date +%s%3N) - start_ms ))
    log_error "${service} — FAILED"
    # Extract failure detail: last 10 lines of k6 output
    local detail
    detail="$(tail -10 "${tmp_out}" | sed 's/^/    /')"
    RESULTS+=("❌ ${service} | FAIL | ${duration_ms}ms")
    FAILURES+=("### ${service}\n\`\`\`\n${detail}\n\`\`\`")
    rm -f "${tmp_out}"
    EXIT_CODE=1
    return 1
  fi
}

# ── Write results markdown file ───────────────────────────────────────────────
write_results() {
  mkdir -p "${RESULTS_DIR}"
  local git_sha
  git_sha="$(git -C "${REPO_ROOT}" rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
  local pass_count fail_count skip_count total_count
  pass_count=$(printf '%s\n' "${RESULTS[@]}" | grep -c '✅' || true)
  fail_count=$(printf '%s\n' "${RESULTS[@]}" | grep -c '❌' || true)
  skip_count=$(printf '%s\n' "${RESULTS[@]}" | grep -c '⏭' || true)
  total_count="${#RESULTS[@]}"

  {
    echo "# Service Validation Smoke Results"
    echo ""
    echo "**Date:** $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "**Git:**  ${git_sha}"
    echo "**Mode:** Contract smoke (1 VU × 3 iterations)"
    echo ""
    echo "## Summary"
    echo ""
    echo "| Result | Count |"
    echo "|--------|-------|"
    echo "| ✅ PASS   | ${pass_count} |"
    echo "| ❌ FAIL   | ${fail_count} |"
    echo "| ⏭ SKIP   | ${skip_count} |"
    echo "| Total  | ${total_count} |"
    echo ""
    echo "## Per-Service Results"
    echo ""
    echo "| Status | Service | Duration |"
    echo "|--------|---------|----------|"
    for r in "${RESULTS[@]}"; do
      IFS='|' read -r status svc dur <<< "$r"
      echo "| ${status} | ${svc} | ${dur} |"
    done
    echo ""
    if [[ "${fail_count}" -gt 0 ]]; then
      echo "## Failure Details"
      echo ""
      for f in "${FAILURES[@]}"; do
        echo -e "$f"
        echo ""
      done
    fi
  } > "${RESULTS_FILE}"

  log_info "Results written to: ${RESULTS_FILE}"
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  log_section "HDIM Service Validation — Contract Smoke Tests"
  log_info "Timestamp: ${TIMESTAMP}"

  extract_certs

  log_section "Running Smoke Tests"

  # service-name | BASE_URL_VAR | base_url_value
  run_smoke "patient-service"       "BASE_URL_PATIENT"  "https://localhost:8084" || true
  run_smoke "fhir-service"          "BASE_URL_FHIR"     "https://localhost:8085" || true
  run_smoke "care-gap-service"      "BASE_URL_CARE_GAP" "https://localhost:8086" || true
  run_smoke "quality-measure-service" "BASE_URL_QUALITY" "https://localhost:8087" || true
  run_smoke "audit-query-service"   "BASE_URL_AUDIT"    "https://localhost:8088" || true
  run_smoke "hcc-service"           "BASE_URL_HCC"      "https://localhost:8105" || true
  run_smoke "cql-engine-service"    "BASE_URL_CQL"      "https://localhost:8081" || true
  run_smoke "events-service"        "BASE_URL_EVENTS"   "https://localhost:8083" || true
  run_smoke "demo-seeding-service"  "BASE_URL_SEEDING"  "http://localhost:8098"  || true

  write_results

  log_section "Results"
  for r in "${RESULTS[@]}"; do
    echo "  $r"
  done
  echo ""

  if [[ "${EXIT_CODE}" -eq 0 ]]; then
    log_ok "All smoke tests passed."
  else
    log_error "One or more smoke tests failed. See ${RESULTS_FILE} for details."
  fi

  exit "${EXIT_CODE}"
}

main "$@"
```

**Step 2: Make executable and create results gitkeep**

```bash
chmod +x load-tests/run-smoke-all.sh
mkdir -p validation/results
touch validation/results/.gitkeep
```

**Step 3: Add results to .gitignore (keep raw output local)**

Add to `.gitignore`:
```
# Smoke test raw output (commit only .md summaries)
validation/results/*.json
```

**Step 4: Run the full smoke suite to verify**

```bash
./load-tests/run-smoke-all.sh
```

Expected: colored output, results file at `validation/results/YYYY-MM-DD-HHmmss-smoke.md`

**Step 5: Commit**

```bash
git add load-tests/run-smoke-all.sh validation/results/.gitkeep
git commit -m "feat(validation): smoke runner — terminal table + results .md file"
```

---

## Task 8: Run Phase 1 Smoke Suite and Commit Results

**Step 1: Run full smoke suite**

```bash
./load-tests/run-smoke-all.sh
```

**Step 2: Review results file**

```bash
cat validation/results/*-smoke.md
```

Check: all 9 services PASS or document known failures with explanation.

**Step 3: Commit results**

```bash
git add validation/results/*-smoke.md
git commit -m "docs(validation): Phase 1 smoke results — $(date +%Y-%m-%d)"
```

**Step 4: Push**

```bash
git push origin master
```

---

## Phase 1 Complete Criteria

- [ ] `validation/services.yml` committed — 9 demo-stack services cataloged
- [ ] `load-tests/scenarios/smoke/` — 9 smoke test files
- [ ] `load-tests/run-smoke-all.sh` — runner with terminal output + results file
- [ ] Smoke suite runs to completion without errors
- [ ] Results `.md` committed
- [ ] All `clinical-core` services pass (or failures documented with root cause)
