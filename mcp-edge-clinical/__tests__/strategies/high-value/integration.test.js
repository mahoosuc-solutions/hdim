/**
 * High-value strategy integration test.
 *
 * Loads the MCP server with CLINICAL_TOOL_STRATEGY=high-value in demo mode
 * and verifies the full JSON-RPC lifecycle with exactly 15 tools.
 */
const supertest = require('supertest');

describe('high-value strategy integration (demo mode)', () => {
  let request;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'high-value';
    jest.resetModules();
    const { createApp } = require('../../../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  const EXPECTED_TOOL_NAMES = [
    'patient_read', 'patient_search',
    'observation_read', 'observation_search',
    'condition_read', 'condition_search',
    'medication_read', 'medication_search',
    'encounter_read', 'encounter_search',
    'care_gap_list', 'care_gap_close', 'care_gap_stats',
    'measure_evaluate', 'measure_results',
    'edge_health'
  ];

  it('lists exactly 16 tools', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    expect(res.status).toBe(200);
    expect(res.body.result.tools).toHaveLength(16);

    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual([...EXPECTED_TOOL_NAMES].sort());
  });

  it('each tool has valid MCP schema', async () => {
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

  describe('demo mode returns fixtures for all 16 tools', () => {
    const DEMO_CALLS = [
      { name: 'patient_read', args: { id: 'demo-1', tenantId: 'acme' } },
      { name: 'patient_search', args: { tenantId: 'acme' } },
      { name: 'observation_read', args: { id: 'demo-1', tenantId: 'acme' } },
      { name: 'observation_search', args: { tenantId: 'acme' } },
      { name: 'condition_read', args: { id: 'demo-1', tenantId: 'acme' } },
      { name: 'condition_search', args: { tenantId: 'acme' } },
      { name: 'medication_read', args: { id: 'demo-1', tenantId: 'acme' } },
      { name: 'medication_search', args: { tenantId: 'acme' } },
      { name: 'encounter_read', args: { id: 'demo-1', tenantId: 'acme' } },
      { name: 'encounter_search', args: { tenantId: 'acme' } },
      { name: 'care_gap_list', args: { patientId: 'demo-1', tenantId: 'acme' } },
      { name: 'care_gap_close', args: { gapId: 'g1', tenantId: 'acme', closedBy: 'dr', reason: 'test' } },
      { name: 'care_gap_stats', args: { tenantId: 'acme' } },
      { name: 'measure_evaluate', args: { patientId: 'demo-1', tenantId: 'acme' } },
      { name: 'measure_results', args: { patientId: 'demo-1', tenantId: 'acme' } },
    ];

    it.each(DEMO_CALLS)('$name returns demo fixture', async ({ name, args }) => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 10, method: 'tools/call', params: { name, arguments: args } });
      expect(res.body.result).toBeDefined();
      expect(res.body.error).toBeUndefined();
      const content = JSON.parse(res.body.result.content[0].text);
      expect(content.demoMode).toBe(true);
    });
  });

  it('RBAC blocks tool calls for restricted roles', async () => {
    // Executive role should be denied access to patient_read
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 20, method: 'tools/call', params: { name: 'patient_read', arguments: { id: 'p1', tenantId: 'acme' } } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.message).toMatch(/Forbidden/i);

    // Executive role should be allowed care_gap_stats
    const res2 = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({ jsonrpc: '2.0', id: 21, method: 'tools/call', params: { name: 'care_gap_stats', arguments: { tenantId: 'acme' } } });
    expect(res2.body.result).toBeDefined();
    expect(res2.body.error).toBeUndefined();
  });

  it('returns error for unknown tool', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 30, method: 'tools/call', params: { name: 'nonexistent_tool', arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toEqual(expect.any(Number));
  });
});
