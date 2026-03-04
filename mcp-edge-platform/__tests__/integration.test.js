const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge integration — tool execution', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('tools/call edge_health returns healthy response', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });
    expect(res.status).toBe(200);
    expect(res.body.jsonrpc).toBe('2.0');
    expect(res.body.id).toBe(1);
    const content = JSON.parse(res.body.result.content[0].text);
    expect(['healthy', 'degraded']).toContain(content.status);
    expect(content.service).toBe('hdim-platform-edge');
  });

  it('tools/call platform_info returns config data', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call', params: { name: 'platform_info', arguments: {} } });
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.name).toBe('hdim-platform-edge');
    expect(content.version).toBe('0.1.0');
  });

  it('unknown tool returns -32602', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'nonexistent', arguments: {} } });
    expect(res.body.error.code).toBe(-32602);
  });

  it('missing operator role returns -32603', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'tools/call', params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('missing_operator_role');
  });

  it('executive role can access edge_health but not fhir_metadata', async () => {
    const allowed = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 5, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });
    expect(allowed.body.result).toBeDefined();

    const denied = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 6, method: 'tools/call', params: { name: 'fhir_metadata', arguments: {} } });
    expect(denied.body.error).toBeDefined();
    expect(denied.body.error.data.reason).toBe('forbidden_for_role');
  });
});
