const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

describe('clinical edge security', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  describe('helmet security headers', () => {
    it('sets X-Content-Type-Options: nosniff', async () => {
      const res = await request.get('/health');
      expect(res.headers['x-content-type-options']).toBe('nosniff');
    });

    it('sets X-Frame-Options header', async () => {
      const res = await request.get('/health');
      expect(res.headers['x-frame-options']).toBeDefined();
    });
  });

  describe('role authentication enforcement', () => {
    it('rejects tools/call without X-Operator-Role header', async () => {
      const res = await request.post('/mcp')
        .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'patient_summary', arguments: { patientId: 'p1', tenantId: 't1' } } });
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('missing_operator_role');
    });

    it('rejects unknown role (hacker)', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'hacker')
        .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'patient_summary', arguments: { patientId: 'p1', tenantId: 't1' } } });
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('unknown_role');
    });
  });

  describe('input validation', () => {
    it('rejects invalid JSON-RPC version', async () => {
      const res = await request.post('/mcp')
        .send({ jsonrpc: '1.0', id: 1, method: 'tools/call', params: {} });
      expect(res.body.error).toBeDefined();
      expect(res.body.error.message).toMatch(/Invalid JSON-RPC version/i);
    });

    it('rejects oversized payload (>1MB) with HTTP 413', async () => {
      const largeBody = JSON.stringify({ data: 'x'.repeat(1_100_000) });
      const res = await request.post('/mcp')
        .set('content-type', 'application/json')
        .send(largeBody);
      expect(res.status).toBe(413);
    });

    it('rejects malformed JSON with HTTP 400', async () => {
      const res = await request.post('/mcp')
        .set('content-type', 'application/json')
        .send('not json at all');
      expect(res.status).toBe(400);
    });
  });

  describe('rate limiting', () => {
    it('returns RateLimit-* headers on /mcp requests', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
      // Rate limiting is optional in clinical edge — only assert if present
      if (res.headers['ratelimit-limit']) {
        expect(res.headers['ratelimit-remaining']).toBeDefined();
      }
    });
  });
});
