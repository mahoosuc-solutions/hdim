/**
 * k6 Load Test — Full Clinical Pipeline (End-to-End)
 *
 * Simulates a realistic clinical workflow across all 4 core pilot services:
 *
 *   Step 1  GET patient health record       (patient-service :8084)
 *   Step 2  GET care gaps for patient       (care-gap-service :8086)
 *   Step 3  GET patient risk assessment     (patient-service :8084)
 *   Step 4  GET quality scores              (quality-measure-service :8087)
 *
 * Think-time of 1 second between steps approximates a clinician reviewing
 * results on screen before navigating to the next panel.
 *
 * SLO for this scenario: pipeline P95 < 500ms per request (more lenient than
 * single-service tests because each step involves cross-service data).
 *
 * Run:
 *   k6 run load-tests/scenarios/full-pipeline.js
 *   k6 run -e TEST_TYPE=smoke load-tests/scenarios/full-pipeline.js
 *   k6 run -e AUTH_TOKEN=<real-token> load-tests/scenarios/full-pipeline.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { defaultOptions } from '../config/options.js';
import { getAuthHeaders } from '../config/auth.js';

// ── Service base URLs ────────────────────────────────────────────────────────
const PATIENT_URL       = __ENV.BASE_URL_PATIENT        || 'http://localhost:8084';
const CARE_GAP_URL      = __ENV.BASE_URL_CARE_GAP       || 'http://localhost:8086';
const QUALITY_URL       = __ENV.BASE_URL_QUALITY_MEASURE || 'http://localhost:8087';

const PATIENT_ID = __ENV.PATIENT_ID || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

// ── k6 options — use the standard 100-VU profile ─────────────────────────────
export const options = {
  ...defaultOptions,
  thresholds: {
    // Individual request SLO: P95 < 200ms
    'http_req_duration{p(95)}': ['p(95)<200'],
    // Pipeline SLO: individual calls in p95 under 500ms
    'http_req_duration{group:::pipeline}': ['p(95)<500'],
    // Error rate
    http_req_failed: ['rate<0.01'],
    // Pipeline-level custom metrics
    'pipeline_total_duration': ['p(95)<1500'],
    'pipeline_errors':         ['rate<0.01'],
  },
};

// ── Custom metrics ───────────────────────────────────────────────────────────
const pipelineTotalDuration  = new Trend('pipeline_total_duration', true);
const step1Duration          = new Trend('pipeline_step1_health_record', true);
const step2Duration          = new Trend('pipeline_step2_care_gaps', true);
const step3Duration          = new Trend('pipeline_step3_risk_assessment', true);
const step4Duration          = new Trend('pipeline_step4_quality_score', true);
const pipelineErrors         = new Rate('pipeline_errors');
const pipelineCompletions    = new Counter('pipeline_completions');

// ── Main VU function ─────────────────────────────────────────────────────────
export default function () {
  const headers = getAuthHeaders();
  const pipelineStart = Date.now();
  let allOk = true;

  group('pipeline', function () {

    // -- Step 1: Retrieve patient health record --
    group('step1-health-record', function () {
      const res = http.get(
        `${PATIENT_URL}/patient/api/v1/patients/${PATIENT_ID}/health-record`,
        { headers, tags: { step: '1', service: 'patient-service' } }
      );

      const ok = check(res, {
        'Step 1 - health record: status 200':       (r) => r.status === 200,
        'Step 1 - health record: response time OK': (r) => r.timings.duration < 500,
      });

      step1Duration.add(res.timings.duration);
      if (!ok) allOk = false;
    });

    sleep(1); // Clinician reviews health record summary

    // -- Step 2: Retrieve care gaps --
    group('step2-care-gaps', function () {
      const res = http.get(
        `${CARE_GAP_URL}/care-gap/api/v1/care-gaps?patientId=${PATIENT_ID}`,
        { headers, tags: { step: '2', service: 'care-gap-service' } }
      );

      const ok = check(res, {
        'Step 2 - care gaps: status 200':       (r) => r.status === 200,
        'Step 2 - care gaps: response time OK': (r) => r.timings.duration < 500,
      });

      step2Duration.add(res.timings.duration);
      if (!ok) allOk = false;
    });

    sleep(1); // Clinician reviews open care gaps

    // -- Step 3: Retrieve risk assessment --
    group('step3-risk-assessment', function () {
      const res = http.get(
        `${PATIENT_URL}/patient/api/v1/patients/${PATIENT_ID}/risk-assessment`,
        { headers, tags: { step: '3', service: 'patient-service' } }
      );

      const ok = check(res, {
        'Step 3 - risk assessment: status 200':       (r) => r.status === 200,
        'Step 3 - risk assessment: response time OK': (r) => r.timings.duration < 500,
      });

      step3Duration.add(res.timings.duration);
      if (!ok) allOk = false;
    });

    sleep(1); // Clinician reviews risk stratification

    // -- Step 4: Retrieve quality score --
    group('step4-quality-score', function () {
      const res = http.get(
        `${QUALITY_URL}/quality-measure/api/v1/measures/score?patientId=${PATIENT_ID}`,
        { headers, tags: { step: '4', service: 'quality-measure-service' } }
      );

      const ok = check(res, {
        'Step 4 - quality score: status 200':       (r) => r.status === 200,
        'Step 4 - quality score: response time OK': (r) => r.timings.duration < 500,
      });

      step4Duration.add(res.timings.duration);
      if (!ok) allOk = false;
    });

  }); // end group('pipeline')

  // Record total pipeline duration (excludes think-time sleeps via wall clock)
  const pipelineElapsed = Date.now() - pipelineStart;
  pipelineTotalDuration.add(pipelineElapsed);
  pipelineErrors.add(!allOk);

  if (allOk) {
    pipelineCompletions.add(1);
  }

  // Final think-time before the next iteration
  sleep(1);
}

// ── Setup: verify all services are reachable ─────────────────────────────────
export function setup() {
  const services = [
    { name: 'patient-service',        url: `${PATIENT_URL}/patient/actuator/health` },
    { name: 'care-gap-service',       url: `${CARE_GAP_URL}/care-gap/actuator/health` },
    { name: 'quality-measure-service',url: `${QUALITY_URL}/quality-measure/actuator/health` },
  ];

  services.forEach(({ name, url }) => {
    const res = http.get(url, { headers: { Accept: 'application/json' } });
    if (res.status !== 200) {
      console.warn(`${name} health check returned ${res.status} at ${url}`);
    } else {
      console.log(`${name} is healthy.`);
    }
  });

  return {
    patientUrl:  PATIENT_URL,
    careGapUrl:  CARE_GAP_URL,
    qualityUrl:  QUALITY_URL,
    patientId:   PATIENT_ID,
  };
}

// ── Teardown ─────────────────────────────────────────────────────────────────
export function teardown(data) {
  console.log('Full pipeline load test complete.');
  console.log(`  patient-service:        ${data.patientUrl}`);
  console.log(`  care-gap-service:       ${data.careGapUrl}`);
  console.log(`  quality-measure-service:${data.qualityUrl}`);
  console.log(`  Patient ID tested:      ${data.patientId}`);
}
