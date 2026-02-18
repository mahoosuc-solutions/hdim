/**
 * Contract smoke test — care-gap-service (port 8086, context /care-gap)
 *
 * Unique requirements validated:
 *   - HTTP 200 on care-gaps list endpoint
 *   - Response body has "content" field (paged Spring response)
 *   - Cache-Control: no-store (HIPAA — care gaps are PHI)
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service care-gap-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_CARE_GAP || 'https://localhost:8086';
const TENANT   = __ENV.TENANT_ID         || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/care-gap/api/v1/care-gaps?page=0&size=20`, { headers });
  check(r, {
    '[care-gap] status 200':             (r) => r.status === 200,
    '[care-gap] body has content field': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch (_) { return false; } },
    '[care-gap] Cache-Control no-store': (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[care-gap] response time < 5s':     (r) => r.timings.duration < 5000,
  });
}
