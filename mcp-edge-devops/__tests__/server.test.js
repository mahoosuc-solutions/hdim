// mcp-edge-devops/__tests__/server.test.js
const { createApp } = require('../server');
const supertest = require('supertest');

describe('devops edge server', () => {
  let app;
  let request;

  beforeAll(() => {
    app = createApp();
    request = supertest(app);
  });

  it('responds to GET /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-devops-edge');
    expect(res.body.status).toBe('healthy');
  });

  it('lists all 7 devops tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 10, method: 'tools/list', params: {}
    });
    expect(res.body.result.tools).toHaveLength(7);
    const names = res.body.result.tools.map((t) => t.name).sort();
    expect(names).toEqual([
      'build_status', 'compose_config', 'docker_logs',
      'docker_restart', 'docker_status', 'edge_health',
      'service_dependencies'
    ]);
  });

  it('responds to POST /mcp with initialize', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-devops-edge');
  });
});
