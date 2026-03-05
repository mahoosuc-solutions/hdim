const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request;
let gatewayReachable = false;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  if (!gatewayReachable) {
    console.warn('Gateway unreachable — skipping platform live tests');
    return;
  }

  process.env.HDIM_DEMO_MODE = 'false';
  jest.resetModules();
  const { createApp } = require('../../server');
  const supertest = require('supertest');
  request = supertest(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
});

function liveIt(name, fn) {
  it(name, async () => {
    if (!gatewayReachable) return;
    await fn();
  });
}

describe('platform edge — live gateway', () => {
  liveIt('edge_health returns healthy status', async () => {
    const res = await callTool(request, 'edge_health');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(['healthy', 'degraded']).toContain(data.status);
  });

  liveIt('platform_health returns gateway health from actuator', async () => {
    const res = await callTool(request, 'platform_health');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(200);
  });

  liveIt('platform_info returns version and config', async () => {
    const res = await callTool(request, 'platform_info');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.name).toBe('hdim-platform-edge');
    expect(data.version).toBeDefined();
  });

  liveIt('fhir_metadata returns FHIR capability statement', async () => {
    const res = await callTool(request, 'fhir_metadata');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });

  liveIt('service_catalog returns available services', async () => {
    const res = await callTool(request, 'service_catalog');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('dashboard_stats returns operational statistics', async () => {
    const res = await callTool(request, 'dashboard_stats');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('demo_status returns demo mode state', async () => {
    const res = await callTool(request, 'demo_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  liveIt('demo_seed populates gateway with test data', async () => {
    const res = await callTool(request, 'demo_seed', { scenarioName: 'hedis-evaluation' });
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
