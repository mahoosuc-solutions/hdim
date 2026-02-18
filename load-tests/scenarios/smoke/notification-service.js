/**
 * Contract smoke test — notification-service (port 8107, context /notification)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service notification-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_NOTIFICATION || 'http://localhost:8107';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/notification/actuator/health`);
  check(r, {
    '[notification] health status 200':  (r) => r.status === 200,
    '[notification] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[notification] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
