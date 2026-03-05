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

describe('CDS tools — live gateway', () => {
  liveIt('cds_patient_view returns CDS cards for a patient', async () => {
    const res = await callTool(request, 'cds_patient_view', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('health_score returns patient health score', async () => {
    const res = await callTool(request, 'health_score', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('pre_visit_plan generates a pre-visit summary', async () => {
    const res = await callTool(request, 'pre_visit_plan', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
