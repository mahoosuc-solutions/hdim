/**
 * Contract smoke test — agent-validation-service (port 8114, context /agent-validation)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service agent-validation-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_AGENT_VALIDATION || 'http://localhost:8114';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/agent-validation/actuator/health`);
  check(r, {
    '[agent-val] health status 200':  (r) => r.status === 200,
    '[agent-val] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[agent-val] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
