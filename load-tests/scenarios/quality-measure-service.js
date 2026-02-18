/**
 * k6 Load Test — quality-measure-service
 *
 * Targets: https://localhost:8087 (or BASE_URL_QUALITY_MEASURE env var)
 * Context path: /quality-measure
 *
 * Endpoints exercised:
 *   GET /quality-measure/results?patient={patientId}   (patient measure results)
 *   GET /quality-measure/score?patient={patientId}     (patient quality score)
 *
 * Note: The QualityMeasureController is a @RestController without a
 * @RequestMapping prefix — its endpoints live directly under /quality-measure
 * (the servlet context path), not under /api/v1/measures.
 *
 * mTLS: pass TLS_CA_CERT, TLS_CLIENT_CERT, TLS_CLIENT_KEY env vars.
 * Auth: gateway-trust headers (GATEWAY_AUTH_DEV_MODE=true in demo containers).
 *
 * Run:
 *   k6 run load-tests/scenarios/quality-measure-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/quality-measure-service.js
 *   ./load-tests/run-demo-load-tests.sh --smoke --scenario quality
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions, getTlsOptions } from '../config/options.js';
import { getDemoAuthHeaders } from '../config/tls.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL_QUALITY_MEASURE || 'https://localhost:8087';
const PATIENT_ID = __ENV.PATIENT_ID               || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
const TENANT_ID  = __ENV.TENANT_ID                || 'acme-health';

// ── k6 options (merged with mTLS config) ─────────────────────────────────────
export const options = {
  ...getOptions(),
  ...getTlsOptions(),
};

// ── Custom metrics ───────────────────────────────────────────────────────────
const measureResultsDuration = new Trend('measure_results_duration', true);
const measureScoreDuration   = new Trend('measure_score_duration', true);
const errorRate              = new Rate('quality_measure_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getDemoAuthHeaders(TENANT_ID);
  const params = { headers, tags: { service: 'quality-measure-service' } };

  // --- Group 1: Get measure results for patient ---
  group('measure-results', function () {
    const res = http.get(
      `${BASE_URL}/quality-measure/results?patient=${PATIENT_ID}`,
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
      `${BASE_URL}/quality-measure/score?patient=${PATIENT_ID}`,
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
  const headers = getDemoAuthHeaders(TENANT_ID);
  const res = http.get(`${BASE_URL}/quality-measure/actuator/health`, { headers });
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
