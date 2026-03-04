const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge isolation boundary', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const PLATFORM_TOOLS = [
    'edge_health', 'platform_health', 'platform_info',
    'fhir_metadata', 'service_catalog', 'dashboard_stats',
    'demo_status', 'demo_seed'
  ].sort();

  const DEVOPS_ONLY_TOOLS = [
    'docker_status', 'docker_logs', 'docker_restart',
    'service_dependencies', 'compose_config', 'build_status'
  ];

  it('exposes exactly the 8 designated platform tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(PLATFORM_TOOLS);
    expect(names).toHaveLength(8);
  });

  it.each(DEVOPS_ONLY_TOOLS)('does NOT expose devops tool: %s', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
  });

  it('no PHI-level data accessible through platform tools', async () => {
    // edge_health and platform_info are static tools that return aggregate data
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });
    const content = res.body.result.content[0].text;
    expect(content).not.toMatch(/patient_id|mrn|ssn/i);
  });
});
