const { createApp } = require('../server');
const supertest = require('supertest');

describe('clinical edge_health deep probe', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('returns downstream field with fhir in edge_health response', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });

    const payload = JSON.parse(res.body.result.content[0].text);
    expect(payload.downstream).toBeDefined();
    expect(payload.downstream.fhir).toBeDefined();
    expect(typeof payload.downstream.fhir.reachable).toBe('boolean');
  });

  it('includes all required fields', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });

    const payload = JSON.parse(res.body.result.content[0].text);
    expect(payload.status).toBeDefined();
    expect(payload.service).toBe('hdim-clinical-edge');
    expect(payload.version).toBe('0.1.0');
    expect(typeof payload.uptime).toBe('number');
    expect(payload.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
  });

  it('reports degraded when FHIR is unreachable', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });

    const payload = JSON.parse(res.body.result.content[0].text);
    expect(payload.downstream.fhir.reachable).toBe(false);
    expect(payload.status).toBe('degraded');
  });

  it('reports healthy when FHIR is reachable', async () => {
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({ ok: true });

    try {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'platform_admin')
        .send({ jsonrpc: '2.0', id: 10, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });

      const payload = JSON.parse(res.body.result.content[0].text);
      expect(payload.status).toBe('healthy');
      expect(payload.downstream.fhir.reachable).toBe(true);
    } finally {
      global.fetch = originalFetch;
    }
  });
});
