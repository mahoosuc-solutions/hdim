/**
 * Authentication helpers for HDIM k6 load tests.
 *
 * In CI/production load runs, override AUTH_TOKEN and TENANT_ID via environment:
 *   k6 run -e AUTH_TOKEN=eyJ... -e TENANT_ID=real-tenant-id scenario.js
 *
 * The placeholder token below is intentionally invalid — it will cause 401s
 * unless replaced with a real JWT issued by the HDIM gateway.
 */

/** Performance test tenant — must exist in the target environment. */
export const PERF_TENANT_ID = __ENV.TENANT_ID || 'test-tenant-perf';

/**
 * Placeholder JWT for local/CI load tests.
 *
 * Replace with a real token via -e AUTH_TOKEN=<token> for actual runs.
 * The token below is a structurally valid but unsigned JWT header.payload
 * so k6 will not blow up on encoding checks.
 */
export const AUTH_TOKEN =
  __ENV.AUTH_TOKEN ||
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
  'eyJzdWIiOiJsb2FkLXRlc3QtdXNlciIsInRlbmFudElkIjoidGVzdC10ZW5hbnQtcGVyZiIsInJvbGVzIjpbIkVWQUxVQVRPUiJdLCJpYXQiOjE3MDAwMDAwMDB9.' +
  'PLACEHOLDER_SIGNATURE_REPLACE_FOR_REAL_RUNS';

/**
 * Returns standard HTTP headers for authenticated HDIM API requests.
 *
 * @param {string} [tenantId] - Optional tenant override; defaults to PERF_TENANT_ID.
 * @returns {{ Authorization: string, 'X-Tenant-ID': string, 'Content-Type': string }}
 */
export function getAuthHeaders(tenantId) {
  return {
    Authorization: `Bearer ${AUTH_TOKEN}`,
    'X-Tenant-ID': tenantId || PERF_TENANT_ID,
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
}

/**
 * Returns headers without the Authorization header — useful for public
 * health-check endpoints that do not require authentication.
 */
export function getPublicHeaders() {
  return {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
}
