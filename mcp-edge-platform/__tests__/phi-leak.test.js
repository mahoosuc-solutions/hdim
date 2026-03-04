const { createApp } = require('../server');
const supertest = require('supertest');

const SENSITIVE_PATTERNS = [
  /patient_id/i,
  /\bssn\b|social.security/i,
  /\bmrn\b|medical.record/i,
  /date.of.birth/i,
  /Bearer\s+\S{10,}/,
  /at\s+\w+\s+\(.*\.js:\d+:\d+\)/,
  /\bpassword\b/i,
  /\bsecret\b/i,
];

describe('error response PHI leak detection', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('unknown tool error contains no sensitive data', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'unknown', arguments: {} } });
    const body = JSON.stringify(res.body);
    for (const pattern of SENSITIVE_PATTERNS) {
      expect(body).not.toMatch(pattern);
    }
  });

  it('forbidden error contains no API key', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'care_coordinator')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'fhir_metadata', arguments: {} } });
    const body = JSON.stringify(res.body);
    expect(body).not.toContain('hdim_secret_key');
    expect(body).not.toContain('12345');
  });

  it('tool not found error contains no stack trace', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'nonexistent', arguments: {} } });
    const body = JSON.stringify(res.body);
    expect(body).not.toMatch(/at\s+\w+\s+\(.*\.js:\d+:\d+\)/);
  });

  it('invalid JSON-RPC version error is clean', async () => {
    const res = await request.post('/mcp').send({ jsonrpc: '1.0', id: 1, method: 'initialize' });
    const body = JSON.stringify(res.body);
    for (const pattern of SENSITIVE_PATTERNS) {
      expect(body).not.toMatch(pattern);
    }
  });

  it('method not found error is clean', async () => {
    const res = await request.post('/mcp').send({ jsonrpc: '2.0', id: 1, method: 'bad/method' });
    const body = JSON.stringify(res.body);
    for (const pattern of SENSITIVE_PATTERNS) {
      expect(body).not.toMatch(pattern);
    }
  });
});
