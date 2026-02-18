/**
 * Contract smoke test — quality-measure-event-service (port 8112, context /quality-measure-event)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service quality-measure-event-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_QM_EVENT || 'http://localhost:8112';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/quality-measure-event/actuator/health`);
  check(r, {
    '[qm-event] health status 200':  (r) => r.status === 200,
    '[qm-event] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[qm-event] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
