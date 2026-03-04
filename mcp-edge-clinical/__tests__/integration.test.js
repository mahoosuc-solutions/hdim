/**
 * Full JSON-RPC Integration Test
 *
 * Exercises the complete MCP JSON-RPC 2.0 lifecycle in demo mode:
 *   initialize -> tools/list -> tools/call round trip
 *   MCP 2025-11-25 contract conformance
 *   Cross-domain tool calls across all 5 clinical domains
 *   Error handling and notification acknowledgement
 */
const supertest = require('supertest');

describe('clinical edge integration (demo mode)', () => {
  let request;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    jest.resetModules();
    const { createApp } = require('../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  // ---- MCP protocol lifecycle ----

  describe('MCP protocol lifecycle', () => {
    it('initialize -> tools/list -> tools/call round trip', async () => {
      // 1. Initialize
      const init = await request.post('/mcp')
        .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
      expect(init.status).toBe(200);
      expect(init.body.result.protocolVersion).toBe('2025-11-25');
      expect(init.body.result.serverInfo.name).toBe('hdim-clinical-edge');
      expect(init.body.result.capabilities.tools).toBeDefined();

      // 2. List tools
      const list = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
      expect(list.status).toBe(200);
      expect(list.body.result.tools.length).toBe(26);

      // 3. Verify each tool has valid schema
      for (const tool of list.body.result.tools) {
        expect(tool.name).toBeDefined();
        expect(tool.description).toBeDefined();
        expect(tool.inputSchema).toBeDefined();
        expect(tool.inputSchema.type).toBe('object');
      }
    });

    it('notifications/initialized returns 204', async () => {
      const res = await request.post('/mcp')
        .send({ jsonrpc: '2.0', method: 'notifications/initialized' });
      expect(res.status).toBe(204);
    });

    it('notifications/cancelled returns 204', async () => {
      const res = await request.post('/mcp')
        .send({ jsonrpc: '2.0', method: 'notifications/cancelled' });
      expect(res.status).toBe(204);
    });
  });

  // ---- MCP contract conformance ----

  describe('MCP contract conformance', () => {
    it('initialize response follows MCP 2025-11-25 spec', async () => {
      const res = await request.post('/mcp')
        .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
      const r = res.body.result;
      expect(r.protocolVersion).toBe('2025-11-25');
      expect(r.serverInfo).toBeDefined();
      expect(r.serverInfo.name).toEqual(expect.any(String));
      expect(r.serverInfo.version).toEqual(expect.any(String));
      expect(r.capabilities).toBeDefined();
    });

    it('tools/list response follows MCP spec', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
      for (const tool of res.body.result.tools) {
        expect(typeof tool.name).toBe('string');
        expect(typeof tool.description).toBe('string');
        expect(tool.inputSchema).toMatchObject({ type: 'object' });
      }
    });

    it('tools/call response follows MCP spec', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'care_gap_stats', arguments: { tenantId: 'acme' } } });
      expect(res.body.result.content).toBeDefined();
      expect(Array.isArray(res.body.result.content)).toBe(true);
      expect(res.body.result.content[0].type).toBe('text');
      expect(typeof res.body.result.content[0].text).toBe('string');
      const parsed = JSON.parse(res.body.result.content[0].text);
      expect(parsed).toBeDefined();
    });

    it('error response follows MCP spec', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 4, method: 'tools/call', params: { name: 'nonexistent_tool', arguments: {} } });
      expect(res.body.error).toBeDefined();
      expect(res.body.error.code).toEqual(expect.any(Number));
      expect(res.body.error.message).toEqual(expect.any(String));
    });
  });

  // ---- Cross-domain tool calls ----

  describe('cross-domain tool calls in demo mode', () => {
    const CROSS_DOMAIN_SAMPLES = [
      { name: 'fhir_read', args: { resourceType: 'Patient', id: 'demo-1', tenantId: 'acme' }, domain: 'FHIR' },
      { name: 'patient_summary', args: { patientId: 'demo-1', tenantId: 'acme' }, domain: 'Patient' },
      { name: 'care_gap_stats', args: { tenantId: 'acme' }, domain: 'Care Gap' },
      { name: 'measure_evaluate', args: { patientId: 'demo-1', tenantId: 'acme' }, domain: 'Quality Measure' },
      { name: 'cql_libraries', args: { tenantId: 'acme' }, domain: 'CQL' },
    ];

    it.each(CROSS_DOMAIN_SAMPLES)('$domain: $name returns demo fixture', async ({ name, args }) => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 5, method: 'tools/call', params: { name, arguments: args } });
      expect(res.body.result).toBeDefined();
      expect(res.body.error).toBeUndefined();
      const content = JSON.parse(res.body.result.content[0].text);
      expect(content.demoMode).toBe(true);
    });
  });
});
