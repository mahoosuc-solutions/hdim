const { createApp } = require('../server');
const supertest = require('supertest');

describe('devops edge isolation boundary', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const DEVOPS_TOOLS = [
    'edge_health', 'docker_status', 'docker_logs',
    'docker_restart', 'service_dependencies',
    'compose_config', 'build_status'
  ].sort();

  const PLATFORM_ONLY_TOOLS = [
    'platform_health', 'platform_info', 'fhir_metadata',
    'service_catalog', 'dashboard_stats', 'demo_status', 'demo_seed'
  ];

  it('exposes exactly the 7 designated devops tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(DEVOPS_TOOLS);
    expect(names).toHaveLength(7);
  });

  it.each(PLATFORM_ONLY_TOOLS)('does NOT expose platform tool: %s', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
  });

  it('devops sidecar has no gateway/PHI access paths', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    const toolNames = res.body.result.tools.map(t => t.name);
    expect(toolNames).not.toContain('fhir_metadata');
    expect(toolNames).not.toContain('dashboard_stats');
    expect(toolNames).not.toContain('demo_seed');
  });
});
