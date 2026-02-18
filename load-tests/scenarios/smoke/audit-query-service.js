/**
 * Contract smoke test — audit-query-service (port 8088, context-path: / root)
 *
 * NOTE: audit-query-service uses context-path: / (root), unlike other services.
 * Endpoints are directly at /api/v1/audit/...
 *
 * Unique requirements validated:
 *   - HTTP 200 on audit statistics endpoint (aggregate counts, no PHI)
 *   - Response time within SLO
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service audit-query-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_AUDIT || 'https://localhost:8088';
const TENANT   = __ENV.TENANT_ID      || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // Statistics endpoint returns aggregate counts — no raw PHI in response
  const r = http.get(`${BASE_URL}/api/v1/audit/logs/statistics`, { headers });
  check(r, {
    '[audit-query] status 200':         (r) => r.status === 200,
    '[audit-query] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
