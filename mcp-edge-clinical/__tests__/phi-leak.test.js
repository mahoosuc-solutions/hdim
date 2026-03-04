const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

const SENSITIVE_PATTERNS = [
  /patient[_-]?id/i,
  /\b\d{3}-\d{2}-\d{4}\b/,          // SSN
  /\bMRN[:\s]*\w+/i,                 // MRN
  /\bdate[_-]?of[_-]?birth/i,        // DOB
  /Bearer\s+\S{10,}/,                // Bearer token
  /at\s+\S+\.js:\d+/,                // Stack trace
  /password/i,
  /secret/i,
];

function assertClean(body) {
  const text = typeof body === 'string' ? body : JSON.stringify(body);
  for (const pattern of SENSITIVE_PATTERNS) {
    expect(text).not.toMatch(pattern);
  }
}

describe('clinical edge PHI leak detection', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('unknown tool error contains no sensitive data', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'unknown_tool', arguments: {} } });
    expect(res.body.error).toBeDefined();
    assertClean(res.body);
    expect(JSON.stringify(res.body)).not.toContain('hdim_secret_key');
  });

  it('forbidden role error contains no API keys or Bearer tokens', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'patient_summary', arguments: { patientId: 'p1', tenantId: 't1' } } });
    expect(res.body.error).toBeDefined();
    const body = JSON.stringify(res.body);
    assertClean(body);
    expect(body).not.toContain('hdim_secret_key');
    expect(body).not.toContain('12345');
  });

  it('invalid JSON-RPC version error is clean', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '1.0', id: 1, method: 'initialize' });
    assertClean(res.body);
  });

  it('method not found error is clean', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'bad/method' });
    assertClean(res.body);
  });

  it('missing required params error is clean', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'patient_summary', arguments: {} } });
    expect(res.body.error).toBeDefined();
    const body = JSON.stringify(res.body);
    // Schema validation may reference property names like "patientId" — that is
    // expected and not a PHI leak. Assert no actual PHI values, tokens, or traces.
    expect(body).not.toMatch(/\b\d{3}-\d{2}-\d{4}\b/);        // SSN
    expect(body).not.toMatch(/\bMRN[:\s]*\w+/i);               // MRN
    expect(body).not.toMatch(/Bearer\s+\S{10,}/);              // Bearer token
    expect(body).not.toMatch(/at\s+\S+\.js:\d+/);              // Stack trace
    expect(body).not.toMatch(/password/i);
    expect(body).not.toMatch(/secret/i);
  });

  describe('clinical-specific PHI patterns', () => {
    const CLINICAL_PATTERNS = [
      /\bJohn\b.*\bDoe\b/i,
      /\bJane\b.*\bSmith\b/i,
      /\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}.*birthDate/i,
      /resourceType.*Patient.*name/i,
    ];

    it.each([
      ['unknown tool', { name: 'nonexistent', arguments: {} }],
      ['forbidden role tool', { name: 'cql_evaluate', arguments: { library: 'lib', patientId: 'p1', tenantId: 't1' } }],
    ])('error from %s does not leak FHIR resource fragments', async (_label, params) => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'executive')
        .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params });
      const body = JSON.stringify(res.body);
      for (const pattern of CLINICAL_PATTERNS) {
        expect(body).not.toMatch(pattern);
      }
      assertClean(body);
    });
  });
});
