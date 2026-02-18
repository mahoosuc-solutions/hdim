/**
 * Contract smoke test — fhir-service (port 8085, context /fhir)
 *
 * Unique requirements validated:
 *   - FHIR R4 Bundle: resourceType = "Bundle"
 *   - Content-Type: application/fhir+json
 *   - Cache-Control: no-store on PHI responses (HIPAA)
 *   - Patient/$everything returns entry[] array
 *
 * Run standalone:
 *   ./load-tests/run-smoke-all.sh --service fhir-service
 */

import http from 'k6/http';
import { check } from 'k6';
import { smokeContractOptions } from '../../config/smoke.js';
import { getDemoAuthHeaders } from '../../config/tls.js';
import { getTlsOptions } from '../../config/options.js';

export const options = { ...smokeContractOptions, ...getTlsOptions() };

const BASE_URL   = __ENV.BASE_URL_FHIR || 'https://localhost:8085';
const TENANT     = __ENV.TENANT_ID     || 'acme-health';
const PATIENT_ID = __ENV.PATIENT_ID    || 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

export default function () {
  const headers = {
    ...getDemoAuthHeaders(TENANT),
    Accept: 'application/fhir+json',
  };

  // Check 1: Patient list is a valid FHIR R4 Bundle
  const r1 = http.get(`${BASE_URL}/fhir/Patient`, { headers });
  check(r1, {
    '[fhir] Patient list status 200':   (r) => r.status === 200,
    '[fhir] resourceType = Bundle':     (r) => { try { return JSON.parse(r.body).resourceType === 'Bundle'; } catch (_) { return false; } },
    '[fhir] Cache-Control no-store':    (r) => (r.headers['Cache-Control'] || '').includes('no-store'),
    '[fhir] Content-Type fhir+json':    (r) => (r.headers['Content-Type'] || '').includes('fhir+json'),
    '[fhir] response time < 5s':        (r) => r.timings.duration < 5000,
  });

  // Check 2: Patient/$everything returns entry array
  const r2 = http.get(`${BASE_URL}/fhir/Patient/${PATIENT_ID}/$everything`, { headers });
  check(r2, {
    '[$everything] status 200':          (r) => r.status === 200,
    '[$everything] resourceType Bundle': (r) => { try { return JSON.parse(r.body).resourceType === 'Bundle'; } catch (_) { return false; } },
    '[$everything] entry array present': (r) => { try { return Array.isArray(JSON.parse(r.body).entry); } catch (_) { return false; } },
  });
}
