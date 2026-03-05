const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

describe('clinical edge isolation boundary', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const CLINICAL_TOOLS = [
    'patient_summary', 'patient_timeline', 'patient_risk', 'patient_list', 'pre_visit_plan',
    'care_gap_list', 'care_gap_identify', 'care_gap_close', 'care_gap_stats', 'care_gap_population', 'care_gap_provider',
    'fhir_read', 'fhir_search', 'fhir_create', 'fhir_bundle',
    'cds_patient_view', 'health_score',
    'measure_evaluate', 'measure_results', 'measure_score', 'measure_population',
    'cql_evaluate', 'cql_batch', 'cql_libraries', 'cql_result',
    'edge_health',
    'admin_preview_strategy', 'admin_set_strategy', 'admin_rollback_strategy'
  ].sort();

  const DEVOPS_ONLY_TOOLS = [
    'docker_ps', 'docker_restart', 'docker_logs', 'docker_status',
    'service_dependencies', 'compose_config', 'build_status'
  ];

  const PLATFORM_ONLY_TOOLS = [
    'dashboard_stats', 'system_health', 'platform_info',
    'demo_status', 'demo_seed'
  ];

  it('exposes exactly 29 tools from composite strategy (25 strategy + 4 infra/admin)', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(CLINICAL_TOOLS);
    expect(names).toHaveLength(29);
  });

  it('every expected clinical tool is present', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    const names = new Set(res.body.result.tools.map(t => t.name));
    for (const tool of CLINICAL_TOOLS) {
      expect(names.has(tool)).toBe(true);
    }
  });

  it.each(DEVOPS_ONLY_TOOLS)('does NOT expose devops tool: %s', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('unknown_tool');
  });

  it.each(PLATFORM_ONLY_TOOLS)('does NOT expose platform tool: %s', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('unknown_tool');
  });

  it('tools/list response includes inputSchema for all tools', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    for (const tool of res.body.result.tools) {
      expect(tool.inputSchema).toBeDefined();
      expect(tool.inputSchema.type).toBe('object');
    }
  });
});
