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

describe('CQL tools — live gateway', () => {
  liveIt('cql_evaluate evaluates a CQL library against a patient', async () => {
    const res = await callTool(request, 'cql_evaluate', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryId: 'HbA1c-Control'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('cql_batch evaluates multiple CQL libraries', async () => {
    const res = await callTool(request, 'cql_batch', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryIds: ['HbA1c-Control']
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('cql_result retrieves stored CQL results', async () => {
    const res = await callTool(request, 'cql_result', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryId: 'HbA1c-Control'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('cql_libraries lists available CQL libraries', async () => {
    const res = await callTool(request, 'cql_libraries', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
