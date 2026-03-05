const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

function liveIt(name, fn) {
  it(name, async () => { if (!gatewayReachable) return; await fn(); });
}

describe('patient tools — live gateway', () => {
  liveIt('patient_summary returns health record bundle', async () => {
    const res = await callTool(request, 'patient_summary', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });

  liveIt('patient_timeline returns encounter timeline', async () => {
    const res = await callTool(request, 'patient_timeline', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('patient_risk returns risk assessment', async () => {
    const res = await callTool(request, 'patient_risk', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('patient_list returns patient roster', async () => {
    const res = await callTool(request, 'patient_list', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });
});
