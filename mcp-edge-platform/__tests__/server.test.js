// mcp-edge-platform/__tests__/server.test.js
const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge server', () => {
  let app;
  let request;

  beforeAll(() => {
    app = createApp();
    request = supertest(app);
  });

  it('responds to GET /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-platform-edge');
    expect(res.body.status).toBe('healthy');
  });

  it('responds to POST /mcp with initialize', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-platform-edge');
  });

  it('lists all 8 platform tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 10, method: 'tools/list', params: {}
    });
    expect(res.body.result.tools).toHaveLength(8);
    const names = res.body.result.tools.map((t) => t.name).sort();
    expect(names).toEqual([
      'dashboard_stats', 'demo_seed', 'demo_status',
      'edge_health', 'fhir_metadata', 'platform_health',
      'platform_info', 'service_catalog'
    ]);
  });
});
