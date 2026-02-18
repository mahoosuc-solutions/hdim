/**
 * Contract smoke test — analytics-service (port 8092, context /analytics)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service analytics-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_ANALYTICS || 'http://localhost:8092';
const TENANT   = __ENV.TENANT_ID          || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/analytics/actuator/health`);
  check(r, {
    '[analytics] health status 200':  (r) => r.status === 200,
    '[analytics] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[analytics] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
