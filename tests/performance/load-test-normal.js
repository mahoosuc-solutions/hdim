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
 * Normal Load Test - 100 Concurrent Users
 *
 * Simulates normal production load with realistic user behavior.
 * Mix: 60% reads, 30% writes, 10% complex operations
 *
 * SLA Targets:
 * - P95 response time < 500ms
 * - Error rate < 0.1%
 * - CPU usage < 60%
 * - Memory usage < 70%
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
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '1m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '1m', target: 50 },    // Ramp down to 50 users
    { duration: '1m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    // Response time thresholds
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],

    // Error rate threshold (< 0.1%)
    'errors': ['rate<0.001'],

    // Throughput threshold (> 50 RPS)
    'http_reqs': ['rate>50'],

    // Check success rate (> 95%)
    'checks': ['rate>0.95'],

    // Custom metric thresholds
    'read_duration': ['p(95)<300'],
    'write_duration': ['p(95)<800'],
    'complex_duration': ['p(95)<2000'],
  },
};

// Configuration
const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:18080';
const TENANT_ID = 'test-tenant-001';

// Test data pools
const PATIENT_IDS = Array.from({ length: 100 }, (_, i) => `patient-${i + 1}`);
const MEASURE_IDS = [
  'COL',    // Colorectal Cancer Screening
  'CBP',    // Controlling High Blood Pressure
  'CDC-HBA1C', // Diabetes Care
  'BCS',    // Breast Cancer Screening
  'CCS',    // Cervical Cancer Screening
];

/**
 * Setup function
 */
export function setup() {
  console.log('🚀 Starting Normal Load Test (100 Users)');
  console.log(`Gateway URL: ${GATEWAY_URL}`);
  console.log(`Duration: 9 minutes (with ramp up/down)`);
  console.log(`Target: 100 concurrent users`);
  console.log(`Mix: 60% reads, 30% writes, 10% complex`);

  // Verify gateway health
  const healthRes = http.get(`${GATEWAY_URL}/actuator/health`);
  if (healthRes.status !== 200) {
    throw new Error(`Gateway not healthy: ${healthRes.status}`);
  }

  console.log('✅ System ready for load test');

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

  // Weighted operation selection (60/30/10)
  const rand = Math.random();

  if (rand < 0.6) {
    // 60% - Read operations
    performReadOperations(data, headers);
  } else if (rand < 0.9) {
    // 30% - Write operations
    performWriteOperations(data, headers);
  } else {
    // 10% - Complex operations
    performComplexOperations(data, headers);
  }

  // Realistic user think time (1-5 seconds)
  sleep(randomIntBetween(1, 5));
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
    () => createAlert(data, headers),
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
    () => generateReport(data, headers),
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
    closureReason: 'Test closure',
    closureDate: new Date().toISOString(),
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/care-gaps/close`, payload, { headers });

  check(res, {
    'close care-gap status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

function createAlert(data, headers) {
  const payload = JSON.stringify({
    serviceId: 'patient-service',
    alertType: 'CPU_HIGH',
    threshold: 80,
    notificationChannel: 'EMAIL',
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/alerts`, payload, { headers });

  check(res, {
    'create alert status ok': (r) => r.status === 201 || r.status === 409,
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

  const res = http.post(`${GATEWAY_URL}/api/v1/quality-measures/evaluate`, payload, { headers, timeout: '10s' });

  check(res, {
    'evaluate measure status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

function generateReport(data, headers) {
  const payload = JSON.stringify({
    reportType: 'CARE_GAP_SUMMARY',
    dateRange: {
      start: '2025-01-01',
      end: '2025-12-31',
    },
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/reports/generate`, payload, { headers, timeout: '15s' });

  check(res, {
    'generate report status ok': (r) => r.status === 200 || r.status === 202 || r.status === 404,
  }) || errorRate.add(1);
}

function bulkCareGapAction(data, headers) {
  const gapIds = Array.from({ length: 10 }, (_, i) => `gap-${randomIntBetween(1, 100)}`);

  const payload = JSON.stringify({
    action: 'ASSIGN',
    gapIds,
    assigneeId: `user-${randomIntBetween(1, 10)}`,
  });

  const res = http.post(`${GATEWAY_URL}/api/v1/care-gaps/bulk-action`, payload, { headers, timeout: '10s' });

  check(res, {
    'bulk action status ok': (r) => r.status === 200 || r.status === 404 || r.status === 400,
  }) || errorRate.add(1);
}

/**
 * Teardown function
 */
export function teardown(data) {
  console.log('🏁 Normal Load Test Complete');
}

/**
 * Custom summary
 */
export function handleSummary(data) {
  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'Normal Load Test (100 Users)',
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

  console.log('\n📊 Load Test Summary:');
  console.log(`   Peak VUs: ${summary.max_virtual_users}`);
  console.log(`   Total Requests: ${summary.total_requests}`);
  console.log(`   RPS: ${summary.requests_per_second.toFixed(2)}`);
  console.log(`   P95 Response: ${summary.response_times.p95_ms.toFixed(2)}ms`);
  console.log(`   Read P95: ${summary.operation_breakdown.read_p95_ms.toFixed(2)}ms`);
  console.log(`   Write P95: ${summary.operation_breakdown.write_p95_ms.toFixed(2)}ms`);
  console.log(`   Complex P95: ${summary.operation_breakdown.complex_p95_ms.toFixed(2)}ms`);
  console.log(`   Error Rate: ${(summary.error_rate * 100).toFixed(2)}%`);

  const slaCompliant =
    summary.response_times.p95_ms < 500 &&
    summary.error_rate < 0.001;

  console.log(`\n${slaCompliant ? '✅' : '❌'} SLA Compliance: ${slaCompliant ? 'PASS' : 'FAIL'}`);

  return {
    'stdout': JSON.stringify(summary, null, 2),
    'reports/load-test-normal-summary.json': JSON.stringify(summary, null, 2),
  };
}
