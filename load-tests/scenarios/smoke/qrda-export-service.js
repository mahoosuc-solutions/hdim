/**
 * Contract smoke test — qrda-export-service (port 8104, context /qrda-export)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service qrda-export-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_QRDA || 'http://localhost:8104';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/qrda-export/actuator/health`);
  check(r, {
    '[qrda] health status 200':  (r) => r.status === 200,
    '[qrda] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[qrda] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
