/**
 * k6 Load Test — care-gap-service
 *
 * Targets: http://localhost:8086 (or BASE_URL_CARE_GAP env var)
 * Context path: /care-gap
 *
 * Endpoints exercised:
 *   GET /care-gap/api/v1/care-gaps?patientId={patientId}
 *   GET /care-gap/api/v1/care-gaps/count?patientId={patientId}
 *
 * Run:
 *   k6 run load-tests/scenarios/care-gap-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/care-gap-service.js
 *   k6 run -e AUTH_TOKEN=<real-token> load-tests/scenarios/care-gap-service.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions } from '../config/options.js';
import { getAuthHeaders } from '../config/auth.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL_CARE_GAP || 'http://localhost:8086';
const PATIENT_ID = __ENV.PATIENT_ID || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

// ── k6 options ───────────────────────────────────────────────────────────────
export const options = getOptions();

// ── Custom metrics ───────────────────────────────────────────────────────────
const careGapListDuration  = new Trend('care_gap_list_duration', true);
const careGapCountDuration = new Trend('care_gap_count_duration', true);
const errorRate            = new Rate('care_gap_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getAuthHeaders();
  const params = { headers, tags: { service: 'care-gap-service' } };

  // --- Group 1: List care gaps for patient ---
  group('care-gaps', function () {
    const res = http.get(
      `${BASE_URL}/care-gap/api/v1/care-gaps?patientId=${PATIENT_ID}`,
      { ...params, tags: { endpoint: 'list_care_gaps' } }
    );

    const ok = check(res, {
      'GET care-gaps: status 200':       (r) => r.status === 200,
      'GET care-gaps: response time OK': (r) => r.timings.duration < 500,
      'GET care-gaps: has body':         (r) => r.body && r.body.length > 0,
    });

    careGapListDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 2: Count care gaps for patient ---
  group('care-gap-count', function () {
    const res = http.get(
      `${BASE_URL}/care-gap/api/v1/care-gaps/count?patientId=${PATIENT_ID}`,
      { ...params, tags: { endpoint: 'count_care_gaps' } }
    );

    const ok = check(res, {
      'GET care-gap count: status 200':       (r) => r.status === 200,
      'GET care-gap count: response time OK': (r) => r.timings.duration < 500,
      'GET care-gap count: has body':         (r) => r.body && r.body.length > 0,
    });

    careGapCountDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(1);
}

// ── Setup ────────────────────────────────────────────────────────────────────
export function setup() {
  const res = http.get(`${BASE_URL}/care-gap/actuator/health`, {
    headers: { Accept: 'application/json' },
  });
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
