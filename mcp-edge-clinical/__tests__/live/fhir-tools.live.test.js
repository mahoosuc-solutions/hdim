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
  if (!gatewayReachable || !ctx?.discoveredPatientId) {
    gatewayReachable = false;
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

describe('clinical FHIR tools — live gateway', () => {
  liveIt('fhir_read retrieves a Patient by ID', async () => {
    const res = await callTool(request, 'fhir_read', {
      resourceType: 'Patient',
      resourceId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(200);
    expect(data.data.resourceType).toBe('Patient');
    expect(data.data.id).toBe(ctx.discoveredPatientId);
  });

  liveIt('fhir_search finds patients', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Patient',
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data.resourceType).toBe('Bundle');
    expect(data.data.entry.length).toBeGreaterThan(0);
  });

  liveIt('fhir_search finds observations for a patient', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Observation',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('fhir_create creates an Observation resource', async () => {
    const res = await callTool(request, 'fhir_create', {
      resourceType: 'Observation',
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      resource: {
        resourceType: 'Observation',
        status: 'final',
        subject: { reference: `Patient/${ctx.discoveredPatientId}` },
        code: {
          coding: [{ system: 'http://loinc.org', code: '8310-5', display: 'Body temperature' }]
        },
        valueQuantity: { value: 37.0, unit: 'Cel', system: 'http://unitsofmeasure.org', code: 'Cel' }
      }
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(201);
  });
});
