/**
 * Contract smoke test — care-gap-event-service (port 8111, context /care-gap-event)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service care-gap-event-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_CARE_GAP_EVENT || 'http://localhost:8111';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/care-gap-event/actuator/health`);
  check(r, {
    '[cg-event] health status 200':  (r) => r.status === 200,
    '[cg-event] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[cg-event] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
