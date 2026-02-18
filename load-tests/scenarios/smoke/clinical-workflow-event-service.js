/**
 * Contract smoke test — clinical-workflow-event-service (port 8113, context /clinical-workflow-event)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service clinical-workflow-event-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_CW_EVENT || 'http://localhost:8113';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/clinical-workflow-event/actuator/health`);
  check(r, {
    '[cw-event] health status 200':  (r) => r.status === 200,
    '[cw-event] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[cw-event] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
