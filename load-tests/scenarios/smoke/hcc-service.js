/**
 * Contract smoke test — hcc-service (port 8105, context /hcc)
 *
 * Unique requirements validated:
 *   - HTTP 200 on HCC crosswalk endpoint (reference data, no PHI)
 *   - Response body is non-empty
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service hcc-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_HCC || 'https://localhost:8105';
const TENANT   = __ENV.TENANT_ID    || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  // Crosswalk is reference data (ICD-10 → HCC mapping) — no PHI
  // icd10Codes is a required @RequestParam — endpoint returns 400 without it
  const r = http.get(`${BASE_URL}/hcc/api/v1/hcc/crosswalk?icd10Codes=E11.9&icd10Codes=I10`, { headers });
  check(r, {
    '[hcc] crosswalk status 200':  (r) => r.status === 200,
    '[hcc] body is non-empty':     (r) => r.body && r.body.length > 2,
    '[hcc] response time < 5s':    (r) => r.timings.duration < 5000,
  });
}
