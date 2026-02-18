/**
 * k6 Load Test — patient-service
 *
 * Targets: http://localhost:8084 (or BASE_URL_PATIENT env var)
 * Context path: /patient
 *
 * Endpoints exercised:
 *   GET /patient/api/v1/patients/{patientId}
 *   GET /patient/api/v1/patients/{patientId}/health-record
 *   GET /patient/api/v1/patients/{patientId}/risk-assessment
 *
 * Run:
 *   k6 run load-tests/scenarios/patient-service.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/patient-service.js
 *   k6 run -e AUTH_TOKEN=<real-token> load-tests/scenarios/patient-service.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getOptions } from '../config/options.js';
import { getAuthHeaders } from '../config/auth.js';

// ── Service configuration ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL_PATIENT || 'http://localhost:8084';
const PATIENT_ID = __ENV.PATIENT_ID || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

// ── k6 options ───────────────────────────────────────────────────────────────
export const options = getOptions();

// ── Custom metrics ───────────────────────────────────────────────────────────
const patientGetDuration      = new Trend('patient_get_duration', true);
const healthRecordDuration    = new Trend('health_record_get_duration', true);
const riskAssessmentDuration  = new Trend('risk_assessment_get_duration', true);
const errorRate               = new Rate('patient_service_errors');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getAuthHeaders();

  // --- Group 1: Get patient demographics ---
  group('patient', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients/${PATIENT_ID}`,
      { headers, tags: { endpoint: 'get_patient' } }
    );

    const ok = check(res, {
      'GET patient: status 200':       (r) => r.status === 200,
      'GET patient: response time OK': (r) => r.timings.duration < 500,
      'GET patient: has body':         (r) => r.body && r.body.length > 0,
    });

    patientGetDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 2: Get patient health record ---
  group('health-record', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients/${PATIENT_ID}/health-record`,
      { headers, tags: { endpoint: 'get_health_record' } }
    );

    const ok = check(res, {
      'GET health-record: status 200':       (r) => r.status === 200,
      'GET health-record: response time OK': (r) => r.timings.duration < 500,
      'GET health-record: has body':         (r) => r.body && r.body.length > 0,
    });

    healthRecordDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(0.5);

  // --- Group 3: Get patient risk assessment ---
  group('risk-assessment', function () {
    const res = http.get(
      `${BASE_URL}/patient/api/v1/patients/${PATIENT_ID}/risk-assessment`,
      { headers, tags: { endpoint: 'get_risk_assessment' } }
    );

    const ok = check(res, {
      'GET risk-assessment: status 200':       (r) => r.status === 200,
      'GET risk-assessment: response time OK': (r) => r.timings.duration < 500,
      'GET risk-assessment: has body':         (r) => r.body && r.body.length > 0,
    });

    riskAssessmentDuration.add(res.timings.duration);
    errorRate.add(!ok);
  });

  sleep(1);
}

// ── Setup: verify service is reachable before starting load ──────────────────
export function setup() {
  const res = http.get(`${BASE_URL}/patient/actuator/health`, {
    headers: { Accept: 'application/json' },
  });
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
