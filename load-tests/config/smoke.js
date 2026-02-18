/**
 * Shared k6 options for HDIM service contract smoke tests.
 *
 * Profile: 1 VU × 3 iterations (~10-15s per service).
 * Used by all scenarios in load-tests/scenarios/smoke/.
 *
 * Smoke tests are correctness checks, not performance checks.
 * Each service's SLO is defined in validation/services.yml.
 * Performance SLOs are enforced in load-tests/scenarios/*.js.
 */

export const smokeContractOptions = {
  vus: 1,
  iterations: 3,
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed:   ['rate<0.01'],
  },
  tags: {
    environment: __ENV.ENVIRONMENT || 'demo',
    team:        'hdim-platform',
    test_type:   'smoke-contract',
  },
};
