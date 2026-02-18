/**
 * Contract smoke test — demo-seeding-service (port 8098, plain HTTP)
 *
 * NOTE: demo-seeding-service runs on plain HTTP (port 8098), NOT mTLS.
 * No TLS options are applied — uses smokeContractOptions only.
 *
 * Unique requirements validated:
 *   - Health endpoint returns HTTP 200
 *   - Actuator status = "UP"
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service demo-seeding-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';

// No getTlsOptions() — demo-seeding-service is plain HTTP
export const options = smokeContractOptions;

const BASE_URL = __ENV.BASE_URL_SEEDING || 'http://localhost:8098';

export default function () {
  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, {
    '[seeding] health status 200':  (r) => r.status === 200,
    '[seeding] status UP':          (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[seeding] response time < 5s': (r) => r.timings.duration < 5000,
  });
}
