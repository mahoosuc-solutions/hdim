/**
 * k6 Load Test — quality-measure-service
 *
 * Targets: http://localhost:8087 (or BASE_URL_QUALITY_MEASURE env var)
 * Context path: /quality-measure
 *
 * Endpoints exercised:
 *   GET /quality-measure/api/v1/measures/results?patientId={patientId}
 *   GET /quality-measure/api/v1/measures/score?patientId={patientId}
 *
 * Run:
 *   k6 run load-tests/scenarios/quality-measure-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/quality-measure-service.js
 *   k6 run -e AUTH_TOKEN=<real-token> load-tests/scenarios/quality-measure-service.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions } from '../config/options.js';
import { getAuthHeaders } from '../config/auth.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL_QUALITY_MEASURE || 'http://localhost:8087';
const PATIENT_ID = __ENV.PATIENT_ID || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

// ── k6 options ───────────────────────────────────────────────────────────────
export const options = getOptions();

// ── Custom metrics ───────────────────────────────────────────────────────────
const measureResultsDuration = new Trend('measure_results_duration', true);
const measureScoreDuration   = new Trend('measure_score_duration', true);
const errorRate              = new Rate('quality_measure_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getAuthHeaders();
  const params = { headers, tags: { service: 'quality-measure-service' } };

  // --- Group 1: Get measure results for patient ---
  group('measure-results', function () {
    const res = http.get(
      `${BASE_URL}/quality-measure/api/v1/measures/results?patientId=${PATIENT_ID}`,
      { ...params, tags: { endpoint: 'get_measure_results' } }
    );

    const ok = check(res, {
      'GET measure results: status 200':       (r) => r.status === 200,
      'GET measure results: response time OK': (r) => r.timings.duration < 500,
      'GET measure results: has body':         (r) => r.body && r.body.length > 0,
    });

    measureResultsDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 2: Get quality score for patient ---
  group('measure-score', function () {
    const res = http.get(
      `${BASE_URL}/quality-measure/api/v1/measures/score?patientId=${PATIENT_ID}`,
      { ...params, tags: { endpoint: 'get_measure_score' } }
    );

    const ok = check(res, {
      'GET measure score: status 200':       (r) => r.status === 200,
      'GET measure score: response time OK': (r) => r.timings.duration < 500,
      'GET measure score: has body':         (r) => r.body && r.body.length > 0,
    });

    measureScoreDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(1);
}

// ── Setup ────────────────────────────────────────────────────────────────────
export function setup() {
  const res = http.get(`${BASE_URL}/quality-measure/actuator/health`, {
    headers: { Accept: 'application/json' },
  });
  if (res.status !== 200) {
    console.warn(
      `quality-measure-service health check returned ${res.status}. ` +
      `Ensure the service is running at ${BASE_URL}.`
    );
  }
  return { baseUrl: BASE_URL, patientId: PATIENT_ID };
}

// ── Teardown ─────────────────────────────────────────────────────────────────
export function teardown(data) {
  console.log(`quality-measure-service load test complete.`);
  console.log(`  Target: ${data.baseUrl}`);
  console.log(`  Patient ID: ${data.patientId}`);
}
