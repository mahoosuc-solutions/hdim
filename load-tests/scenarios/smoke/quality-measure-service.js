/**
 * Contract smoke test — quality-measure-service (port 8087, context /quality-measure)
 *
 * Unique requirements validated:
 *   - HTTP 200 on measure results endpoint
 *   - Response body has "content" field (paged Spring response)
 *   - Cache-Control: no-store (HIPAA — measure results contain PHI)
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service quality-measure-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_QUALITY || 'https://localhost:8087';
const TENANT   = __ENV.TENANT_ID        || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/quality-measure/results?page=0&size=20`, { headers });
  check(r, {
    '[quality-measure] status 200':             (r) => r.status === 200,
    '[quality-measure] body has content field': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch (_) { return false; } },
    '[quality-measure] Cache-Control no-store': (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[quality-measure] response time < 5s':     (r) => r.timings.duration < 5000,
  });
}
