/**
 * Contract smoke test — ehr-connector-service (port 8100, context /)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service ehr-connector-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_EHR || 'http://localhost:8100';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[ehr] health status 200':  (r) => r.status === 200,
    '[ehr] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[ehr] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
