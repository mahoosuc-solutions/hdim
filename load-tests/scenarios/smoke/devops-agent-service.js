/**
 * Contract smoke test — devops-agent-service (port 8090, context /)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service devops-agent-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_DEVOPS_AGENT || 'http://localhost:8090';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[devops-agent] health status 200':  (r) => r.status === 200,
    '[devops-agent] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[devops-agent] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
