import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    patient_search_steady_load: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 80,
      maxVUs: 250,
      stages: [
        { target: 100, duration: '2m' },
        { target: 100, duration: '8m' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<200'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const TENANT_ID = __ENV.TENANT_ID || 'TENANT001';
const SEARCH_TERMS = ['john', 'smith', 'williams', 'mrn-001', 'mrn-002'];

export default function () {
  const q = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];

  const res = http.get(`${BASE_URL}/api/v1/patients/search?q=${encodeURIComponent(q)}`, {
    headers: {
      'X-Tenant-ID': TENANT_ID,
      Accept: 'application/json',
    },
    tags: { endpoint: 'patient-search' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });
}
