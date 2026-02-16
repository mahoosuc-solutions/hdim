import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 25,
  duration: '3m',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<900'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';

export default function () {
  const resourceType = ['Patient', 'Observation', 'Condition', 'Encounter'][Math.floor(Math.random() * 4)];
  const res = http.get(`${BASE_URL}/fhir/${resourceType}?_count=20`);

  check(res, {
    'fhir status 200': (r) => r.status === 200,
    'fhir body non-empty': (r) => (r.body || '').length > 0,
  });

  sleep(0.5);
}
