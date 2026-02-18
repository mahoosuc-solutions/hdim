/**
 * Contract smoke test — migration-workflow-service (port 8103, context /)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service migration-workflow-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_MIGRATION || 'http://localhost:8103';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[migration] health status 200':  (r) => r.status === 200,
    '[migration] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[migration] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
