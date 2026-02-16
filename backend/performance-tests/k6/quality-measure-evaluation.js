import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 15,
  duration: '3m',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1200'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const TENANT_ID = __ENV.TENANT_ID || 'TENANT001';

export default function () {
  const payload = JSON.stringify({
    patientId: __ENV.PATIENT_ID || 'patient-001',
    measureIds: ['HEDIS_CIS', 'HEDIS_BCS'],
    asOfDate: new Date().toISOString().slice(0, 10),
  });

  const res = http.post(`${BASE_URL}/api/v1/quality/evaluate`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': TENANT_ID,
    },
  });

  check(res, {
    'quality eval status 200/202': (r) => r.status === 200 || r.status === 202,
  });

  sleep(1);
}
