const platformApp = require('../../mcp-edge-platform/server').createApp;
const devopsApp = require('../../mcp-edge-devops/server').createApp;
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

const PLATFORM_TOOLS = ['edge_health', 'platform_health', 'platform_info', 'fhir_metadata', 'service_catalog', 'dashboard_stats', 'demo_status', 'demo_seed'];
const DEVOPS_TOOLS = ['edge_health', 'docker_status', 'docker_logs', 'docker_restart', 'service_dependencies', 'compose_config', 'build_status'];
const PLATFORM_ONLY = PLATFORM_TOOLS.filter(t => t !== 'edge_health' && !DEVOPS_TOOLS.includes(t));
const DEVOPS_ONLY = DEVOPS_TOOLS.filter(t => t !== 'edge_health' && !PLATFORM_TOOLS.includes(t));

describe('PROOF: Cross-Sidecar Isolation — NIST SC-7, SOC2 CC6.1, HIPAA 164.312(a)(1)', () => {
  let platformReq, devopsReq;
  beforeAll(() => {
    platformReq = supertest(platformApp());
    devopsReq = supertest(devopsApp());
  });

  it('platform edge exposes exactly 8 tools', async () => {
    const res = await platformReq.post('/mcp').send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(PLATFORM_TOOLS.sort());
  });

  it('devops edge exposes exactly 7 tools', async () => {
    const res = await devopsReq.post('/mcp').send({ jsonrpc: '2.0', id: 1, method: 'tools/list', params: {} });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(DEVOPS_TOOLS.sort());
  });

  it.each(DEVOPS_ONLY)('platform edge rejects devops tool: %s', async (tool) => {
    const res = await platformReq.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('unknown_tool');
  });

  it.each(PLATFORM_ONLY)('devops edge rejects platform tool: %s', async (tool) => {
    const res = await devopsReq.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('unknown_tool');
  });
});
