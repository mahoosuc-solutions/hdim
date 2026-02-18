/**
 * Contract smoke test — event-processing-service (port 8083, context /events)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service event-processing-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_EVENT_PROCESSING || 'http://localhost:8083';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/events/actuator/health`);
  check(r, {
    '[evt-proc] health status 200':  (r) => r.status === 200,
    '[evt-proc] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[evt-proc] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
