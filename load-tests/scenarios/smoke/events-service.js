/**
 * Contract smoke test — events-service (port 8083, context /events)
 *
 * Events service is Kafka-backed. Only the Spring Boot actuator health
 * endpoint is accessible via HTTP — Kafka consumer health is validated
 * indirectly via the health composite check.
 *
 * Unique requirements validated:
 *   - Health endpoint returns HTTP 200
 *   - Actuator status = "UP"
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service events-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL = __ENV.BASE_URL_EVENTS || 'https://localhost:8083';
const TENANT   = __ENV.TENANT_ID       || 'acme-health';

export default function () {
  const headers = getDemoAuthHeaders(TENANT);

  const r = http.get(`${BASE_URL}/events/actuator/health`, { headers });
  check(r, {
    '[events] health status 200': (r) => r.status === 200,
    '[events] status UP':         (r) => { try { return JSON.parse(r.body).status === 'UP'; } catch (_) { return false; } },
    '[events] response time < 5s':(r) => r.timings.duration < 5000,
  });
}
