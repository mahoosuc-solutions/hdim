/**
 * Contract smoke test — sales-automation-service (port 8106, context /sales-automation)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service sales-automation-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_SALES_AUTO || 'http://localhost:8106';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/sales-automation/actuator/health`);
  check(r, {
    '[sales-auto] health status 200':  (r) => r.status === 200,
    '[sales-auto] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[sales-auto] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
