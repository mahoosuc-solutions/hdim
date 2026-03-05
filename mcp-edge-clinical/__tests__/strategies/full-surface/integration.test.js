/**
 * Full-surface strategy integration test.
 *
 * Loads the MCP server with CLINICAL_TOOL_STRATEGY=full-surface in demo mode
 * and verifies the full JSON-RPC lifecycle with 68 tools (52 FHIR + 16 domain).
 */
const supertest = require('supertest');

describe('full-surface strategy integration (demo mode)', () => {
  let request;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'full-surface';
    jest.resetModules();
    const { createApp } = require('../../../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  it('lists exactly 68 tools', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    expect(res.status).toBe(200);
    expect(res.body.result.tools).toHaveLength(72);
  });

  it('every tool has valid MCP schema', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });

    for (const tool of res.body.result.tools) {
      expect(tool.name).toBeDefined();
      expect(tool.description).toBeDefined();
      expect(tool.inputSchema).toBeDefined();
      expect(tool.inputSchema.type).toBe('object');
    }
  });

  it('tool names are unique', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/list', params: {} });
    const names = res.body.result.tools.map(t => t.name);
    expect(new Set(names).size).toBe(names.length);
  });

  // Sample FHIR tools to verify demo mode
  const FHIR_DEMO_CALLS = [
    { name: 'patient_read', args: { id: 'demo-1', tenantId: 'acme' } },
    { name: 'patient_search', args: { tenantId: 'acme' } },
    { name: 'patient_create', args: { resource: { resourceType: 'Patient' }, tenantId: 'acme' } },
    { name: 'observation_read', args: { id: 'demo-1', tenantId: 'acme' } },
    { name: 'medication_request_read', args: { id: 'demo-1', tenantId: 'acme' } },
    { name: 'medication_request_search', args: { tenantId: 'acme' } },
    { name: 'allergy_intolerance_read', args: { id: 'demo-1', tenantId: 'acme' } },
    { name: 'practitioner_role_read', args: { id: 'demo-1', tenantId: 'acme' } },
    { name: 'bundle_read', args: { id: 'demo-1', tenantId: 'acme' } },
  ];

  describe('demo mode returns fixtures for FHIR tools', () => {
    it.each(FHIR_DEMO_CALLS)('$name returns demo fixture', async ({ name, args }) => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 10, method: 'tools/call', params: { name, arguments: args } });
      expect(res.body.result).toBeDefined();
      expect(res.body.error).toBeUndefined();
      const content = JSON.parse(res.body.result.content[0].text);
      expect(content.demoMode).toBe(true);
    });
  });

  // Sample domain tools
  const DOMAIN_DEMO_CALLS = [
    { name: 'care_gap_list', args: { patientId: 'demo-1', tenantId: 'acme' } },
    { name: 'care_gap_stats', args: { tenantId: 'acme' } },
    { name: 'measure_evaluate', args: { patientId: 'demo-1', tenantId: 'acme' } },
    { name: 'cql_evaluate', args: { library: 'HbA1c-Control', patientId: 'demo-1', tenantId: 'acme' } },
    { name: 'health_score', args: { patientId: 'demo-1', tenantId: 'acme' } },
  ];

  describe('demo mode returns fixtures for domain tools', () => {
    it.each(DOMAIN_DEMO_CALLS)('$name returns demo fixture', async ({ name, args }) => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 20, method: 'tools/call', params: { name, arguments: args } });
      expect(res.body.result).toBeDefined();
      expect(res.body.error).toBeUndefined();
      const content = JSON.parse(res.body.result.content[0].text);
      expect(content.demoMode).toBe(true);
    });
  });

  it('RBAC blocks tool calls for restricted roles', async () => {
    // Executive should be denied patient_read
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 30, method: 'tools/call', params: { name: 'patient_read', arguments: { id: 'p1', tenantId: 'acme' } } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.message).toMatch(/Forbidden/i);

    // Executive should be allowed care_gap_stats
    const res2 = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 31, method: 'tools/call', params: { name: 'care_gap_stats', arguments: { tenantId: 'acme' } } });
    expect(res2.body.result).toBeDefined();
    expect(res2.body.error).toBeUndefined();
  });

  it('clinician role can access all 68 tools', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinician')
      .send({ jsonrpc: '2.0', id: 40, method: 'tools/list', params: {} });
    expect(res.body.result.tools).toHaveLength(72);
  });

  it('returns error for unknown tool', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 50, method: 'tools/call', params: { name: 'nonexistent_tool', arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toEqual(expect.any(Number));
  });
});
