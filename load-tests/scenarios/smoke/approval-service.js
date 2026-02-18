/**
 * Contract smoke test — approval-service (port 8097, context /)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service approval-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_APPROVAL || 'http://localhost:8097';
const TENANT   = __ENV.TENANT_ID         || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[approval] health status 200':  (r) => r.status === 200,
    '[approval] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[approval] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
