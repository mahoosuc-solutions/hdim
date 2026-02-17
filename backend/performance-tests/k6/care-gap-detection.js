import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '3m',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const TENANT_ID = __ENV.TENANT_ID || 'TENANT001';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/care-gaps?status=OPEN&page=0&size=25`, {
    headers: { 'X-Tenant-ID': TENANT_ID },
  });

  check(res, {
    'care gap status 200': (r) => r.status === 200,
    'care gap payload non-empty': (r) => (r.body || '').length > 2,
  });

  sleep(0.75);
}
