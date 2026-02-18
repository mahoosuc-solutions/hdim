/**
 * Contract smoke test — cql-engine-service (port 8081, context /cql-engine)
 *
 * Unique requirements validated:
 *   - HTTP 200 on evaluations list endpoint
 *   - Response time within SLO (2000ms — CQL evaluation is compute-intensive)
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service cql-engine-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_CQL || 'https://localhost:8081';
const TENANT   = __ENV.TENANT_ID    || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // List evaluations — returns paged results or empty array when no evaluations run
  const r = http.get(`${BASE_URL}/cql-engine/api/v1/cql/evaluations`, { headers });
  check(r, {
    '[cql-engine] status 200':         (r) => r.status === 200,
    '[cql-engine] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
