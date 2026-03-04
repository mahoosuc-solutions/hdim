const supertest = require('supertest');

describe('clinical edge server', () => {
  let app, request;

  beforeAll(() => {
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    process.env.HDIM_DEMO_MODE = 'true';
    const { createApp } = require('../server');
    app = createApp();
    request = supertest(app);
  });

  afterAll(() => {
    delete process.env.CLINICAL_TOOL_STRATEGY;
    delete process.env.HDIM_DEMO_MODE;
  });

  it('responds to /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-clinical-edge');
    expect(res.body.status).toBe('healthy');
  });

  it('responds to MCP initialize', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-clinical-edge');
    expect(res.body.result.protocolVersion).toBe('2025-11-25');
  });

  it('lists 25 tools from composite strategy', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
    expect(res.status).toBe(200);
    const toolNames = res.body.result.tools.map(t => t.name);
    expect(toolNames).toContain('patient_summary');
    expect(toolNames).toContain('fhir_read');
    expect(toolNames).toContain('care_gap_list');
    expect(toolNames).toContain('measure_evaluate');
    expect(toolNames).toContain('cql_evaluate');
    expect(toolNames.length).toBe(25);
  });

  it('can call a stub tool', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'patient_summary', arguments: {} } });
    expect(res.body.result).toBeDefined();
    expect(res.body.error).toBeUndefined();
  });
});
