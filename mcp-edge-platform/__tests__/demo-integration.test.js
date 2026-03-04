const { createApp } = require('../server');
const supertest = require('supertest');

const PLATFORM_TOOLS = [
  'edge_health',
  'platform_health',
  'platform_info',
  'fhir_metadata',
  'service_catalog',
  'dashboard_stats',
  'demo_status',
  'demo_seed'
];

function callTool(request, name, id = 1) {
  return request.post('/mcp')
    .set('x-operator-role', 'platform_admin')
    .send({ jsonrpc: '2.0', id, method: 'tools/call', params: { name, arguments: {} } });
}

describe('platform edge — demo mode integration', () => {
  let request;
  const originalDemoMode = process.env.HDIM_DEMO_MODE;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    request = supertest(createApp());
  });

  afterAll(() => {
    if (originalDemoMode === undefined) {
      delete process.env.HDIM_DEMO_MODE;
    } else {
      process.env.HDIM_DEMO_MODE = originalDemoMode;
    }
  });

  describe.each(PLATFORM_TOOLS)('tool "%s" in demo mode', (toolName) => {
    let res;

    beforeAll(async () => {
      res = await callTool(request, toolName);
    });

    it('returns HTTP 200', () => {
      expect(res.status).toBe(200);
    });

    it('conforms to MCP JSON-RPC response spec', () => {
      expect(res.body.jsonrpc).toBe('2.0');
      expect(res.body.id).toBeDefined();
      expect(res.body.result).toBeDefined();
      expect(res.body.result.content).toBeInstanceOf(Array);
      expect(res.body.result.content.length).toBeGreaterThanOrEqual(1);
      expect(res.body.result.content[0].type).toBe('text');
      expect(typeof res.body.result.content[0].text).toBe('string');
    });

    it('returns parseable JSON fixture data', () => {
      const parsed = JSON.parse(res.body.result.content[0].text);
      expect(parsed).toBeDefined();
      expect(typeof parsed).toBe('object');
    });

  });
});

describe('platform edge — demo mode disabled', () => {
  let request;
  const originalDemoMode = process.env.HDIM_DEMO_MODE;

  beforeAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    request = supertest(createApp());
  });

  afterAll(() => {
    if (originalDemoMode === undefined) {
      delete process.env.HDIM_DEMO_MODE;
    } else {
      process.env.HDIM_DEMO_MODE = originalDemoMode;
    }
  });

  it('edge_health still works as a local tool', async () => {
    const res = await callTool(request, 'edge_health');
    expect(res.status).toBe(200);
    expect(res.body.jsonrpc).toBe('2.0');
    expect(res.body.result).toBeDefined();
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.status).toBe('healthy');
    expect(content.service).toBe('hdim-platform-edge');
  });
});
