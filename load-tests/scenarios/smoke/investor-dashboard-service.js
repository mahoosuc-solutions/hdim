/**
 * Contract smoke test — investor-dashboard-service (port 8120, context /investor)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service investor-dashboard-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_INVESTOR || 'http://localhost:8120';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/investor/actuator/health`);
  check(r, {
    '[investor] health status 200':  (r) => r.status === 200,
    '[investor] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[investor] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
