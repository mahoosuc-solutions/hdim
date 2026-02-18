/**
 * Shared k6 options for HDIM load tests.
 *
 * SLO Targets:
 *   - P95 response time < 200ms
 *   - Error rate < 1%
 *
 * Load Profile:
 *   - Ramp 0 → 10 VUs over 30s   (warm-up)
 *   - Hold 10 VUs for 60s         (baseline)
 *   - Ramp 10 → 100 VUs over 60s  (ramp-up)
 *   - Hold 100 VUs for 180s       (peak load)
 *   - Ramp 100 → 0 VUs over 30s   (cool-down)
 *
 * Total duration: ~6 minutes
 */

export const defaultOptions = {
  stages: [
    { duration: '30s', target: 10 },   // Warm-up: ramp to 10 users
    { duration: '1m',  target: 10 },   // Baseline: hold 10 users
    { duration: '1m',  target: 100 },  // Ramp-up: scale to 100 users
    { duration: '3m',  target: 100 },  // Peak: hold 100 users
    { duration: '30s', target: 0 },    // Cool-down: ramp to 0
  ],

  thresholds: {
    // SLO: 95th percentile must be under 200ms
    http_req_duration: ['p(95)<200'],
    // SLO: error rate must be under 1%
    http_req_failed: ['rate<0.01'],
    // Per-group thresholds (applied when groups are used)
    'http_req_duration{group:::patient}':        ['p(95)<200'],
    'http_req_duration{group:::health-record}':  ['p(95)<200'],
    'http_req_duration{group:::risk-assessment}':['p(95)<200'],
    'http_req_duration{group:::care-gaps}':      ['p(95)<200'],
    'http_req_duration{group:::care-gap-count}': ['p(95)<200'],
    'http_req_duration{group:::measure-results}':['p(95)<200'],
    'http_req_duration{group:::measure-score}':  ['p(95)<200'],
    'http_req_duration{group:::pipeline}':       ['p(95)<500'],
  },

  tags: {
    environment: __ENV.ENVIRONMENT || 'local',
    team: 'hdim-platform',
    slo_target: 'p95_200ms',
  },

  // Batch concurrent connections limit per VU
  batch: 10,
  batchPerHost: 4,

  // Summary output
  summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

/**
 * Smoke test options — 1 VU, 1 iteration per scenario.
 * Used for quick sanity checks before a full load run.
 */
export const smokeOptions = {
  vus: 1,
  iterations: 1,

  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },

  tags: {
    environment: __ENV.ENVIRONMENT || 'local',
    team: 'hdim-platform',
    test_type: 'smoke',
  },
};

/**
 * Returns options based on the TEST_TYPE environment variable.
 *   TEST_TYPE=smoke  → smokeOptions
 *   TEST_TYPE=load   → defaultOptions (default)
 */
export function getOptions() {
  const testType = __ENV.TEST_TYPE || 'load';
  if (testType === 'smoke') {
    return smokeOptions;
  }
  return defaultOptions;
}

/**
 * Returns k6 TLS options for mTLS connections when TLS_CA_CERT env var is set.
 * Merges with scenario options to enable certificate-authenticated connections
 * to the HDIM demo services.
 *
 * @returns {Object} k6-compatible tlsAuth block, or {} if certs not configured
 */
export function getTlsOptions() {
  const clientCert = __ENV.TLS_CLIENT_CERT || '';
  const clientKey  = __ENV.TLS_CLIENT_KEY  || '';

  if (!clientCert || !clientKey) {
    return {};
  }

  return {
    // Provide client certificate for mTLS
    tlsAuth: [
      {
        cert: clientCert,
        key:  clientKey,
        domains: ['localhost', '127.0.0.1'],
      },
    ],
    // The demo services use a self-signed CA (hdim-local-ca) that k6 does not
    // have in its trust store. Skip server cert verification while still
    // presenting our client certificate for mTLS.
    insecureSkipTLSVerify: true,
  };
}
