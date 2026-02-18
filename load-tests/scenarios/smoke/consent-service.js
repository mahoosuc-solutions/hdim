/**
 * Contract smoke test — consent-service (port 8082, context /consent)
 *
 * Unique requirements validated:
 *   - Health endpoint returns UP
 *   - Response time < 5s
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service consent-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_CONSENT || 'http://localhost:8082';
const TENANT   = __ENV.TENANT_ID || 'acme-health';

export default function () {
  const r = http.get(`${BASE_URL}/consent/actuator/health`);
  check(r, {
    '[consent] health status 200':  (r) => r.status === 200,
    '[consent] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[consent] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
