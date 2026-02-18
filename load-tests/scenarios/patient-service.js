/**
 * k6 Load Test — patient-service
 *
 * Targets: https://localhost:8084 (or BASE_URL_PATIENT env var)
 * Context path: /patient
 *
 * Endpoints exercised:
 *   GET /patient/api/v1/patients               (patient list, paged)
 *   GET /patient/api/v1/patients               (second page — pagination test)
 *   GET /patient/api/v1/patients               (with size=5 — page size test)
 *
 * mTLS: pass TLS_CA_CERT, TLS_CLIENT_CERT, TLS_CLIENT_KEY env vars.
 * Auth: gateway-trust headers (GATEWAY_AUTH_DEV_MODE=true in demo containers).
 *
 * Run:
 *   k6 run load-tests/scenarios/patient-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/patient-service.js
 *   ./load-tests/run-demo-load-tests.sh --smoke --scenario patient
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions, getTlsOptions } from '../config/options.js';
import { getDemoAuthHeaders } from '../config/tls.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL_PATIENT || 'https://localhost:8084';
const PATIENT_ID = __ENV.PATIENT_ID || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
const TENANT_ID  = __ENV.TENANT_ID   || 'acme-health';

// ── k6 options (merged with mTLS config) ─────────────────────────────────────
export const options = {
  ...getOptions(),
  ...getTlsOptions(),
};

// ── Custom metrics ───────────────────────────────────────────────────────────
const patientListDuration   = new Trend('patient_list_duration', true);
const patientPage2Duration  = new Trend('patient_list_page2_duration', true);
const patientSmallDuration  = new Trend('patient_list_small_duration', true);
const errorRate             = new Rate('patient_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getDemoAuthHeaders(TENANT_ID);

  // --- Group 1: List patients (first page) ---
  group('patient', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients?page=0&size=20`,
      { headers, tags: { endpoint: 'list_patients' } }
    );

    const ok = check(res, {
      'GET patients list: status 200':       (r) => r.status === 200,
      'GET patients list: response time OK': (r) => r.timings.duration < 500,
      'GET patients list: has body':         (r) => r.body && r.body.length > 0,
    });

    patientListDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 2: List patients (second page — pagination test) ---
  group('health-record', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients?page=1&size=20`,
      { headers, tags: { endpoint: 'list_patients_p2' } }
    );

    const ok = check(res, {
      'GET patients page 2: status 200':       (r) => r.status === 200,
      'GET patients page 2: response time OK': (r) => r.timings.duration < 500,
      'GET patients page 2: has body':         (r) => r.body && r.body.length > 0,
    });

    patientPage2Duration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 3: List patients with small page size ---
  group('risk-assessment', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients?page=0&size=5`,
      { headers, tags: { endpoint: 'list_patients_small' } }
    );

    const ok = check(res, {
      'GET patients small page: status 200':       (r) => r.status === 200,
      'GET patients small page: response time OK': (r) => r.timings.duration < 500,
      'GET patients small page: has body':         (r) => r.body && r.body.length > 0,
    });

    patientSmallDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(1);
}

// ── Setup: verify service is reachable before starting load ──────────────────
export function setup() {
  const headers = getDemoAuthHeaders(TENANT_ID);
  const res = http.get(`${BASE_URL}/patient/actuator/health`, { headers });
  if (res.status !== 200) {
    console.warn(
      `patient-service health check returned ${res.status}. ` +
      `Ensure the service is running at ${BASE_URL}.`
    );
  }
  return { baseUrl: BASE_URL, patientId: PATIENT_ID };
}

// ── Teardown: emit summary ───────────────────────────────────────────────────
export function teardown(data) {
  console.log(`patient-service load test complete.`);
  console.log(`  Target: ${data.baseUrl}`);
  console.log(`  Patient ID: ${data.patientId}`);
}
