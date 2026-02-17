import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '2m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';

export default function () {
  const payload = JSON.stringify({
    username: __ENV.TEST_USERNAME || 'demo',
    password: __ENV.TEST_PASSWORD || 'demo123',
  });

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'login status 200/201': (r) => r.status === 200 || r.status === 201,
    'login response has token or session': (r) =>
      (r.body || '').includes('token') || (r.body || '').includes('session'),
  });

  sleep(1);
}
