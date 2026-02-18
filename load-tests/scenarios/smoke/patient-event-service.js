/**
 * Contract smoke test — patient-event-service (port 8110, context /patient-event)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service patient-event-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_PATIENT_EVENT || 'http://localhost:8110';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/patient-event/actuator/health`);
  check(r, {
    '[pat-event] health status 200':  (r) => r.status === 200,
    '[pat-event] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[pat-event] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
