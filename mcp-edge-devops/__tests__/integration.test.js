const { createApp } = require('../server');
const supertest = require('supertest');

describe('devops edge integration — tool execution', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('tools/call edge_health returns healthy response', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });
    expect(res.status).toBe(200);
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.status).toBe('healthy');
    expect(content.service).toBe('hdim-devops-edge');
  });

  it('tools/call service_dependencies returns full graph', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call', params: { name: 'service_dependencies', arguments: {} } });
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.dependencies).toBeDefined();
    expect(content.dependencies['fhir-service']).toContain('postgres');
  });

  it('tools/call service_dependencies with specific service', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'developer')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'service_dependencies', arguments: { service: 'fhir-service' } } });
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.service).toBe('fhir-service');
    expect(content.dependencies).toContain('postgres');
  });

  it('unknown tool returns -32602', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 4, method: 'tools/call', params: { name: 'nonexistent', arguments: {} } });
    expect(res.body.error.code).toBe(-32602);
  });

  it('missing operator role returns -32603', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 5, method: 'tools/call', params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('missing_operator_role');
  });

  it('executive role cannot use docker_restart', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 6, method: 'tools/call', params: { name: 'docker_restart', arguments: { service: 'test' } } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.data.reason).toBe('forbidden_for_role');
  });
});
