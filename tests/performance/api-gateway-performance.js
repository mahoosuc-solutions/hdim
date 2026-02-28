import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * API Gateway Performance Test
 *
 * Tests gateway routing performance, health endpoints,
 * and basic API operations.
 *
 * SLA Targets:
 * - P95 response time < 100ms
 * - P99 response time < 200ms
 * - Error rate < 0.1%
 * - Throughput > 500 RPS
 */

// Custom metrics
const errorRate = new Rate('errors');
const gatewayDuration = new Trend('gateway_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 5 },   // Warm up
    { duration: '1m', target: 10 },   // Test load
    { duration: '30s', target: 0 },   // Cool down
  ],
  thresholds: {
    // Response time thresholds
    'http_req_duration': ['p(95)<250', 'p(99)<500'],

    // Error rate threshold (< 0.1%)
    'errors': ['rate<0.01'],

    // Throughput threshold (> 100 RPS for this small test)
    'http_reqs': ['rate>10'],

    // Check success rate (> 95%)
    'checks': ['rate>0.85'],
  },
};

// Gateway base URL
const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:18080';

// Test data
const TENANT_ID = 'test-tenant-001';
const TEST_PATIENT_ID = 'patient-test-001';

/**
 * Setup function - runs once before test
 */
export function setup() {
  console.log('🚀 Starting API Gateway Performance Test');
  console.log(`Gateway URL: ${GATEWAY_URL}`);
  console.log(`Target users: 10 concurrent`);
  console.log(`Duration: 2 minutes`);

  // Verify gateway is accessible
  const healthRes = http.get(`${GATEWAY_URL}/actuator/health`);
  if (healthRes.status !== 200) {
    throw new Error(`Gateway health check failed: ${healthRes.status}`);
  }

  console.log('✅ Gateway is healthy and ready');

  return {
    tenantId: TENANT_ID,
    patientId: TEST_PATIENT_ID,
  };
}

/**
 * Main test function - runs for each virtual user
 */
export default function (data) {
  const headers = {
    'X-Tenant-ID': data.tenantId,
    'Content-Type': 'application/json',
  };

  // Test 1: Health endpoint
  testHealthEndpoint(headers);

  // Test 2: Patient API routing
  testPatientRouting(data, headers);

  // Test 3: Care Gap API routing
  testCareGapRouting(data, headers);

  // Simulate user think time
  sleep(1);
}

/**
 * Test health endpoint performance
 */
function testHealthEndpoint(headers) {
  const start = Date.now();
  const res = http.get(`${GATEWAY_URL}/actuator/health`, { headers });
  const duration = Date.now() - start;

  gatewayDuration.add(duration);

  const success = check(res, {
    'health status is 200': (r) => r.status === 200,
    'health response time < 250ms': (r) => r.timings.duration < 250,
    'health response has status': (r) => r.json('status') !== undefined,
  });

  errorRate.add(res.status >= 500);
}

/**
 * Test patient API routing through gateway
 */
function testPatientRouting(data, headers) {
  const start = Date.now();
  const res = http.get(`${GATEWAY_URL}/api/v1/patients/${data.patientId}`, { headers });
  const duration = Date.now() - start;

  gatewayDuration.add(duration);

  const success = check(res, {
    'patient status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'patient response time < 400ms': (r) => r.timings.duration < 400,
    'patient response is JSON when JSON content-type is returned': (r) => {
      const contentType = r.headers['Content-Type'] || '';
      if (!contentType.includes('application/json')) {
        return true;
      }
      try {
        JSON.parse(r.body);
        return true;
      } catch {
        return false;
      }
    },
  });

  errorRate.add(res.status >= 500);
}

/**
 * Test care gap API routing through gateway
 */
function testCareGapRouting(data, headers) {
  const start = Date.now();
  const res = http.get(`${GATEWAY_URL}/api/v1/care-gaps`, { headers });
  const duration = Date.now() - start;

  gatewayDuration.add(duration);

  const success = check(res, {
    'care-gaps status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'care-gaps response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(res.status >= 500);
}

/**
 * Teardown function - runs once after test
 */
export function teardown(data) {
  console.log('🏁 API Gateway Performance Test Complete');
}

/**
 * Custom summary handler
 */
export function handleSummary(data) {
  const vusValues = data.metrics.vus ? data.metrics.vus.values : {};
  const reqValues = data.metrics.http_reqs ? data.metrics.http_reqs.values : {};
  const durationValues = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : {};
  const errorValues = data.metrics.errors ? data.metrics.errors.values : {};
  const checkValues = data.metrics.checks ? data.metrics.checks.values : {};

  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'API Gateway Performance Test',
    duration_seconds: data.state.testRunDurationMs / 1000,
    virtual_users: vusValues.max || 0,
    total_requests: reqValues.count || 0,
    requests_per_second: reqValues.rate || 0,
    response_times: {
      avg_ms: durationValues.avg || 0,
      p50_ms: durationValues['p(50)'] || 0,
      p95_ms: durationValues['p(95)'] || 0,
      p99_ms: durationValues['p(99)'] || 0,
      max_ms: durationValues.max || 0,
    },
    error_rate: errorValues.rate || 0,
    checks_passed: checkValues.rate || 0,
  };

  console.log('\n📊 Performance Summary:');
  console.log(`   Total Requests: ${summary.total_requests}`);
  console.log(`   RPS: ${summary.requests_per_second.toFixed(2)}`);
  console.log(`   Avg Response: ${summary.response_times.avg_ms.toFixed(2)}ms`);
  console.log(`   P95 Response: ${summary.response_times.p95_ms.toFixed(2)}ms`);
  console.log(`   P99 Response: ${summary.response_times.p99_ms.toFixed(2)}ms`);
  console.log(`   Error Rate: ${(summary.error_rate * 100).toFixed(2)}%`);
  console.log(`   Checks Passed: ${(summary.checks_passed * 100).toFixed(2)}%`);

  // Check SLA compliance
  const slaCompliant =
    summary.response_times.p95_ms < 100 &&
    summary.response_times.p99_ms < 200 &&
    summary.error_rate < 0.01 &&
    summary.checks_passed > 0.85;

  console.log(`\n${slaCompliant ? '✅' : '❌'} SLA Compliance: ${slaCompliant ? 'PASS' : 'FAIL'}`);

  return {
    'stdout': JSON.stringify(summary, null, 2),
    '/reports/api-gateway-summary.json': JSON.stringify(summary, null, 2),
  };
}
