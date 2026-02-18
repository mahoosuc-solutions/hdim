/**
 * Contract smoke test — patient-service (port 8084, context /patient)
 *
 * Unique requirements validated:
 *   - HTTP 200 on patient list endpoint
 *   - Response body has "content" field (paged Spring response)
 *   - Cache-Control: no-store on PHI response (HIPAA)
 *   - Tenant isolation: wrong tenant gets 403 or empty content
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service patient-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_PATIENT || 'https://localhost:8084';
const TENANT   = __ENV.TENANT_ID        || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // Check 1: Patient list returns 200 + paged body with Cache-Control
  const r1 = http.get(`${BASE_URL}/patient/api/v1/patients?page=0&size=20`, { headers });
  check(r1, {
    '[patient] status 200':             (r) => r.status === 200,
    '[patient] body has content field': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch (_) { return false; } },
    '[patient] Cache-Control no-store': (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[patient] response time < 5s':     (r) => r.timings.duration < 5000,
  });

  // Check 2: Tenant isolation — wrong tenant gets 403 or empty content[]
  const wrongHeaders = getDemoAuthHeaders('wrong-tenant-that-does-not-exist');
  const r2 = http.get(`${BASE_URL}/patient/api/v1/patients?page=0&size=20`, { headers: wrongHeaders });
  check(r2, {
    '[patient] wrong tenant: 403 or empty content': (r) => {
      if (r.status === 403) return true;
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body.content) && body.content.length === 0;
      } catch (_) { return false; }
    },
  });
}
