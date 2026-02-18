/**
 * k6 Load Test — care-gap-service
 *
 * Targets: https://localhost:8086 (or BASE_URL_CARE_GAP env var)
 * Context path: /care-gap
 *
 * Endpoints exercised:
 *   GET /care-gap/api/v1/care-gaps?patientId={patientId}   (patient care gaps)
 *   GET /care-gap/api/v1/care-gaps                          (all care gaps, paged)
 *
 * mTLS: pass TLS_CA_CERT, TLS_CLIENT_CERT, TLS_CLIENT_KEY env vars.
 * Auth: gateway-trust headers (GATEWAY_AUTH_DEV_MODE=true in demo containers).
 *
 * Run:
 *   k6 run load-tests/scenarios/care-gap-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/care-gap-service.js
 *   ./load-tests/run-demo-load-tests.sh --smoke --scenario care-gap
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions, getTlsOptions } from '../config/options.js';
import { getDemoAuthHeaders } from '../config/tls.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL_CARE_GAP || 'https://localhost:8086';
const PATIENT_ID = __ENV.PATIENT_ID        || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
const TENANT_ID  = __ENV.TENANT_ID         || 'acme-health';

// ── k6 options (merged with mTLS config) ─────────────────────────────────────
export const options = {
  ...getOptions(),
  ...getTlsOptions(),
};

// ── Custom metrics ───────────────────────────────────────────────────────────
const careGapPatientDuration = new Trend('care_gap_patient_duration', true);
const careGapAllDuration     = new Trend('care_gap_all_duration', true);
const errorRate              = new Rate('care_gap_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getDemoAuthHeaders(TENANT_ID);
  const params = { headers, tags: { service: 'care-gap-service' } };

  // --- Group 1: List care gaps for a specific patient ---
  group('care-gaps', function () {
    const res = http.get(
      `${BASE_URL}/care-gap/api/v1/care-gaps?patientId=${PATIENT_ID}`,
      { ...params, tags: { endpoint: 'list_care_gaps_patient' } }
    );

    const ok = check(res, {
      'GET care-gaps (patient): status 200':       (r) => r.status === 200,
      'GET care-gaps (patient): response time OK': (r) => r.timings.duration < 500,
      'GET care-gaps (patient): has body':         (r) => r.body && r.body.length > 0,
    });

    careGapPatientDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 2: List all care gaps (paged) ---
  group('care-gap-count', function () {
    const res = http.get(
      `${BASE_URL}/care-gap/api/v1/care-gaps?page=0&size=20`,
      { ...params, tags: { endpoint: 'list_care_gaps_all' } }
    );

    const ok = check(res, {
      'GET care-gaps (all): status 200':       (r) => r.status === 200,
      'GET care-gaps (all): response time OK': (r) => r.timings.duration < 500,
      'GET care-gaps (all): has body':         (r) => r.body && r.body.length > 0,
    });

    careGapAllDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(1);
}

// ── Setup ────────────────────────────────────────────────────────────────────
export function setup() {
  const headers = getDemoAuthHeaders(TENANT_ID);
  const res = http.get(`${BASE_URL}/care-gap/actuator/health`, { headers });
  if (res.status !== 200) {
    console.warn(
      `care-gap-service health check returned ${res.status}. ` +
      `Ensure the service is running at ${BASE_URL}.`
    );
  }
  return { baseUrl: BASE_URL, patientId: PATIENT_ID };
}

// ── Teardown ─────────────────────────────────────────────────────────────────
export function teardown(data) {
  console.log(`care-gap-service load test complete.`);
  console.log(`  Target: ${data.baseUrl}`);
  console.log(`  Patient ID: ${data.patientId}`);
}
