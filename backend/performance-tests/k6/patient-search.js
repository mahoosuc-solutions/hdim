import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 30,
  duration: '3m',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<750'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const TENANT_ID = __ENV.TENANT_ID || 'TENANT001';

export default function () {
  const query = ['john', 'smith', 'doe', 'mrn-001'][Math.floor(Math.random() * 4)];
  const res = http.get(`${BASE_URL}/api/v1/patients/search?q=${query}`, {
    headers: { 'X-Tenant-ID': TENANT_ID },
  });

  check(res, {
    'patient search status 200': (r) => r.status === 200,
    'patient search response is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });

  sleep(0.5);
}
