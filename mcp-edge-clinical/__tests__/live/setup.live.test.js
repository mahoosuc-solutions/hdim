const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  writeTestContext,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request;
let gatewayReachable = false;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  if (!gatewayReachable) {
    console.warn('Gateway unreachable — skipping clinical live setup');
    return;
  }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
  delete process.env.CLINICAL_TOOL_STRATEGY;
});

function liveIt(name, fn) {
  it(name, async () => {
    if (!gatewayReachable) return;
    await fn();
  });
}

describe('clinical live setup — seed and discover', () => {
  liveIt('discovers patients via fhir_search', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Patient',
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    const patients = data.data.entry || [];
    expect(patients.length).toBeGreaterThan(0);
    const ctx = readTestContext() || { patients: {}, resources: {} };
    if (patients.length > 0) {
      ctx.discoveredPatientId = patients[0].resource.id;
    }
    writeTestContext(ctx);
    expect(ctx.discoveredPatientId).toBeDefined();
  });

  liveIt('discovers observations for a patient', async () => {
    const ctx = readTestContext();
    if (!ctx?.discoveredPatientId) return;
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Observation',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('discovers conditions for a patient', async () => {
    const ctx = readTestContext();
    if (!ctx?.discoveredPatientId) return;
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Condition',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
