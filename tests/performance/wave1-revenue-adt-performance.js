import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * Wave-1 Revenue/ADT Contract Performance Test
 *
 * Focuses on the newly introduced Wave-1 contract endpoints and validates
 * that they avoid 5xx failures and stay within practical latency targets.
 */

const errorRate = new Rate('errors');
const wave1Duration = new Trend('wave1_duration');
const endpointAvailable = new Rate('endpoint_available');

export const options = {
  stages: [
    { duration: '20s', target: 3 },
    { duration: '40s', target: 6 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<400', 'p(99)<800'],
    errors: ['rate<0.01'],
    checks: ['rate>0.9'],
    http_reqs: ['rate>5'],
  },
};

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:18080';

function jsonHeaders(tenantId) {
  return {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
  };
}

export function setup() {
  const healthRes = http.get(`${GATEWAY_URL}/actuator/health`);
  if (healthRes.status !== 200) {
    throw new Error(`Gateway unavailable: ${healthRes.status}`);
  }
  return { tenantId: 'test-tenant-001' };
}

export default function (data) {
  testRevenueSubmission(data.tenantId);
  testAdtIngest(data.tenantId);
  sleep(1);
}

function testRevenueSubmission(tenantId) {
  const payload = JSON.stringify({
    tenantId,
    claimId: `k6-claim-${__VU}-${__ITER}`,
    patientId: `k6-patient-${__VU}`,
    payerId: 'k6-payer-a',
    totalAmount: 100.0,
    idempotencyKey: `k6-idem-${__VU}-${__ITER}`,
    correlationId: `k6-corr-rev-${__VU}-${__ITER}`,
    actor: 'k6',
  });

  const res = http.post(
    `${GATEWAY_URL}/api/v1/revenue/claims/submissions`,
    payload,
    { headers: jsonHeaders(tenantId) }
  );

  wave1Duration.add(res.timings.duration);
  endpointAvailable.add(res.status !== 404);
  errorRate.add(res.status >= 500);

  check(res, {
    'revenue endpoint non-5xx': (r) => r.status < 500,
    'revenue latency < 800ms': (r) => r.timings.duration < 800,
  });
}

function testAdtIngest(tenantId) {
  const payload = JSON.stringify({
    tenantId,
    sourceSystem: 'hie-main',
    sourceMessageId: `k6-msg-${__VU}-${__ITER}`,
    eventType: 'A01',
    patientExternalId: `k6-ext-patient-${__VU}`,
    encounterExternalId: `k6-encounter-${__VU}-${__ITER}`,
    payloadHash: `k6-hash-${__VU}-${__ITER}`,
    correlationId: `k6-corr-adt-${__VU}-${__ITER}`,
  });

  const res = http.post(
    `${GATEWAY_URL}/api/v1/interoperability/adt/messages`,
    payload,
    { headers: jsonHeaders(tenantId) }
  );

  wave1Duration.add(res.timings.duration);
  endpointAvailable.add(res.status !== 404);
  errorRate.add(res.status >= 500);

  check(res, {
    'adt endpoint non-5xx': (r) => r.status < 500,
    'adt latency < 800ms': (r) => r.timings.duration < 800,
  });
}

export function handleSummary(data) {
  const reqValues = data.metrics.http_reqs ? data.metrics.http_reqs.values : {};
  const durationValues = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : {};
  const errorValues = data.metrics.errors ? data.metrics.errors.values : {};
  const checkValues = data.metrics.checks ? data.metrics.checks.values : {};
  const endpointValues = data.metrics.endpoint_available ? data.metrics.endpoint_available.values : {};

  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'Wave-1 Revenue/ADT Performance Test',
    total_requests: reqValues.count || 0,
    requests_per_second: reqValues.rate || 0,
    response_times: {
      avg_ms: durationValues.avg || 0,
      p95_ms: durationValues['p(95)'] || 0,
      p99_ms: durationValues['p(99)'] || 0,
      max_ms: durationValues.max || 0,
    },
    error_rate: errorValues.rate || 0,
    checks_passed: checkValues.rate || 0,
    endpoint_available_rate: endpointValues.rate || 0,
  };

  return {
    stdout: JSON.stringify(summary, null, 2),
    '/reports/wave1-revenue-adt-summary.json': JSON.stringify(summary, null, 2),
  };
}
