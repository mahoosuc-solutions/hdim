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

const BASE_URL = __ENV.BASE_URL_FHIR || 'https://localhost:8085';
const TENANT   = __ENV.TENANT_ID     || 'acme-health';

// setup() runs once before VUs start — discovers a real patient ID from the
// FHIR Patient list so $everything uses a known-existing ID.
// If no patients are seeded, patientId is null and $everything check is skipped.
export function setup() {
  const headers = {
    ...getDemoAuthHeaders(TENANT),
    Accept: 'application/fhir+json',
  };
  const r = http.get(`${BASE_URL}/fhir/Patient?_count=1`, { headers, ...getTlsOptions() });
  try {
    const body = JSON.parse(r.body);
    const entry = body.entry;
    if (Array.isArray(entry) && entry.length > 0) {
      return { patientId: entry[0].resource.id };
    }
  } catch (_) {}
  return { patientId: null };
}

export default function (data) {
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

  // Check 2: Patient/$everything — only run if a real patient ID was found in setup()
  if (data.patientId) {
    const r2 = http.get(`${BASE_URL}/fhir/Patient/${data.patientId}/$everything`, { headers });
    check(r2, {
      '[$everything] status 200':          (r) => r.status === 200,
      '[$everything] resourceType Bundle': (r) => { try { return JSON.parse(r.body).resourceType === 'Bundle'; } catch (_) { return false; } },
      '[$everything] entry array present': (r) => { try { return Array.isArray(JSON.parse(r.body).entry); } catch (_) { return false; } },
    });
  }
}
