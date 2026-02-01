import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Helper functions (replaces k6/experimental/utils)
function randomIntBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

/**
 * Stress Load Test - Find Breaking Point
 *
 * Ramps from 100 to 1500 concurrent users to identify system limits.
 * Mix: 70% reads, 20% writes, 10% complex operations
 *
 * Goals:
 * - Identify maximum concurrent users
 * - Find bottleneck resources
 * - Verify graceful degradation (no crashes)
 * - Error rate < 5% until breaking point
 */

// Custom metrics
const errorRate = new Rate('errors');
const readDuration = new Trend('read_duration');
const writeDuration = new Trend('write_duration');
const complexDuration = new Trend('complex_duration');
const operationCounter = new Counter('operations_by_type');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 100 },    // Baseline
    { duration: '3m', target: 300 },    // Ramp to moderate load
    { duration: '3m', target: 600 },    // Ramp to high load
    { duration: '3m', target: 1000 },   // Ramp to stress load
    { duration: '2m', target: 1500 },   // Push to breaking point
    { duration: '2m', target: 0 },      // Ramp down
  ],
  thresholds: {
    // Relaxed thresholds - expect degradation
    'http_req_duration': ['p(95)<2000', 'p(99)<5000'],

    // Error rate < 5% until breaking point
    'errors': ['rate<0.05'],

    // Graceful degradation - no total failures
    'http_req_failed': ['rate<0.10'],
  },
};

// Configuration
const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:18080';
const TENANT_ID = 'test-tenant-001';

// Test data pools
const PATIENT_IDS = Array.from({ length: 500 }, (_, i) => `patient-${i + 1}`);
const MEASURE_IDS = ['COL', 'CBP', 'CDC-HBA1C', 'BCS', 'CCS'];

/**
 * Setup function
 */
export function setup() {
  console.log('🚀 Starting Stress Load Test (100 → 1500 Users)');
  console.log(`Gateway URL: ${GATEWAY_URL}`);
  console.log(`Duration: 15 minutes`);
  console.log(`Target: 1500 concurrent users (peak)`);
  console.log(`Goal: Find breaking point`);

  // Verify gateway health
  const healthRes = http.get(`${GATEWAY_URL}/actuator/health`);
  if (healthRes.status !== 200) {
    throw new Error(`Gateway not healthy: ${healthRes.status}`);
  }

  console.log('✅ System ready for stress test');

  return {
    tenantId: TENANT_ID,
    patientIds: PATIENT_IDS,
    measureIds: MEASURE_IDS,
  };
}

/**
 * Main test function
 */
export default function (data) {
  const headers = {
    'X-Tenant-ID': data.tenantId,
    'Content-Type': 'application/json',
  };

  // Weighted operation selection (70/20/10)
  const rand = Math.random();

  if (rand < 0.7) {
    // 70% - Read operations
    performReadOperations(data, headers);
  } else if (rand < 0.9) {
    // 20% - Write operations
    performWriteOperations(data, headers);
  } else {
    // 10% - Complex operations
    performComplexOperations(data, headers);
  }

  // Variable think time based on load
  const vus = __VU;
  const thinkTime = vus < 500 ? randomIntBetween(1, 3) : randomIntBetween(1, 2);
  sleep(thinkTime);
}

/**
 * Read operations (fast, frequent)
 */
function performReadOperations(data, headers) {
  operationCounter.add(1, { type: 'read' });

  const operations = [
    () => getPatient(data, headers),
    () => searchPatients(data, headers),
    () => getCareGaps(data, headers),
    () => getMetrics(headers),
  ];

  const operation = randomItem(operations);
  const start = Date.now();
  operation();
  readDuration.add(Date.now() - start);
}

/**
 * Write operations (moderate speed)
 */
function performWriteOperations(data, headers) {
  operationCounter.add(1, { type: 'write' });

  const operations = [
    () => updatePatient(data, headers),
    () => closeCareGap(data, headers),
  ];

  const operation = randomItem(operations);
  const start = Date.now();
  operation();
  writeDuration.add(Date.now() - start);
}

/**
 * Complex operations (slow, infrequent)
 */
function performComplexOperations(data, headers) {
  operationCounter.add(1, { type: 'complex' });

  const operations = [
    () => evaluateQualityMeasure(data, headers),
    () => bulkCareGapAction(data, headers),
  ];

  const operation = randomItem(operations);
  const start = Date.now();
  operation();
  complexDuration.add(Date.now() - start);
}

// ============ Read Operations ============

function getPatient(data, headers) {
  const patientId = randomItem(data.patientIds);
  const res = http.get(`${GATEWAY_URL}/api/v1/patients/${patientId}`, { headers });

  check(res, {
    'get patient status ok': (r) => r.status === 200 || r.status === 404,
  }) || errorRate.add(1);
}

function searchPatients(data, headers) {
  const res = http.get(`${GATEWAY_URL}/api/v1/patients?page=0&size=20`, { headers });

  check(res, {
    'search patients status ok': (r) => r.status === 200,
  }) || errorRate.add(1);
}

function getCareGaps(data, headers) {
  const patientId = randomItem(data.patientIds);
  const res = http.get(`${GATEWAY_URL}/api/v1/care-gaps?patientId=${patientId}`, { headers });

  check(res, {
    'get care-gaps status ok': (r) => r.status === 200 || r.status === 404,
  }) || errorRate.add(1);
}

function getMetrics(headers) {
  const res = http.get(`${GATEWAY_URL}/api/v1/metrics/real-time`, { headers });

  check(res, {
    'get metrics status ok': (r) => r.status === 200 || r.status === 404,
  }) || errorRate.add(1);
}

// ============ Write Operations ============

function updatePatient(data, headers) {
  const patientId = randomItem(data.patientIds);
  const payload = JSON.stringify({
    phone: `555-${randomIntBetween(1000, 9999)}`,
    email: `patient${randomIntBetween(1, 999)}@test.com`,
  });

  const res = http.patch(`${GATEWAY_URL}/api/v1/patients/${patientId}`, payload, { headers });

  check(res, {
    'update patient status ok': (r) => r.status === 200 || r.status === 404,
  }) || errorRate.add(1);
}

function closeCareGap(data, headers) {
  const payload = JSON.stringify({
    gapId: `gap-${randomIntBetween(1, 100)}`,
    closureReason: 'Stress test closure',
    closureDate: new Date().toISOString(),
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/care-gaps/close`, payload, { headers });

  check(res, {
    'close care-gap status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

// ============ Complex Operations ============

function evaluateQualityMeasure(data, headers) {
  const measureId = randomItem(data.measureIds);
  const patientId = randomItem(data.patientIds);

  const payload = JSON.stringify({
    measureId,
    patientId,
    evaluationDate: new Date().toISOString(),
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/quality-measures/evaluate`, payload, { headers, timeout: '15s' });

  check(res, {
    'evaluate measure status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

function bulkCareGapAction(data, headers) {
  const gapIds = Array.from({ length: 20 }, (_, i) => `gap-${randomIntBetween(1, 100)}`);

  const payload = JSON.stringify({
    action: 'ASSIGN',
    gapIds,
    assigneeId: `user-${randomIntBetween(1, 10)}`,
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/care-gaps/bulk-action`, payload, { headers, timeout: '15s' });

  check(res, {
    'bulk action status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

/**
 * Teardown function
 */
export function teardown(data) {
  console.log('🏁 Stress Load Test Complete');
}

/**
 * Custom summary
 */
export function handleSummary(data) {
  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'Stress Load Test (1500 Users)',
    duration_seconds: data.state.testRunDurationMs / 1000,
    max_virtual_users: data.metrics.vus.values.max,
    total_requests: data.metrics.http_reqs.values.count,
    requests_per_second: data.metrics.http_reqs.values.rate,
    response_times: {
      avg_ms: data.metrics.http_req_duration.values.avg,
      p50_ms: data.metrics.http_req_duration.values['p(50)'],
      p95_ms: data.metrics.http_req_duration.values['p(95)'],
      p99_ms: data.metrics.http_req_duration.values['p(99)'],
      max_ms: data.metrics.http_req_duration.values.max,
    },
    operation_breakdown: {
      read_p95_ms: data.metrics.read_duration ? data.metrics.read_duration.values['p(95)'] : 0,
      write_p95_ms: data.metrics.write_duration ? data.metrics.write_duration.values['p(95)'] : 0,
      complex_p95_ms: data.metrics.complex_duration ? data.metrics.complex_duration.values['p(95)'] : 0,
    },
    error_rate: data.metrics.errors ? data.metrics.errors.values.rate : 0,
    checks_passed: data.metrics.checks ? data.metrics.checks.values.rate : 0,
  };

  console.log('\n📊 Stress Test Summary:');
  console.log(`   Peak VUs: ${summary.max_virtual_users}`);
  console.log(`   Total Requests: ${summary.total_requests}`);
  console.log(`   RPS: ${summary.requests_per_second.toFixed(2)}`);
  console.log(`   P95 Response: ${summary.response_times.p95_ms.toFixed(2)}ms`);
  console.log(`   P99 Response: ${summary.response_times.p99_ms.toFixed(2)}ms`);
  console.log(`   Max Response: ${summary.response_times.max_ms.toFixed(2)}ms`);
  console.log(`   Error Rate: ${(summary.error_rate * 100).toFixed(2)}%`);

  const breakingPoint = summary.error_rate > 0.05 ? 'REACHED' : 'NOT REACHED';
  const gracefulDegradation = summary.error_rate < 0.10;

  console.log(`\n🔥 Breaking Point: ${breakingPoint}`);
  console.log(`${gracefulDegradation ? '✅' : '❌'} Graceful Degradation: ${gracefulDegradation ? 'YES' : 'NO'}`);

  return {
    'stdout': JSON.stringify(summary, null, 2),
    'reports/load-test-stress-summary.json': JSON.stringify(summary, null, 2),
  };
}
