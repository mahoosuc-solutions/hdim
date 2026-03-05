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

describe('care gap tools — live gateway', () => {
  liveIt('care_gap_list returns open care gaps for a patient', async () => {
    const res = await callTool(request, 'care_gap_list', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('care_gap_identify detects care gaps for a patient', async () => {
    const res = await callTool(request, 'care_gap_identify', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('care_gap_stats returns aggregate statistics', async () => {
    const res = await callTool(request, 'care_gap_stats', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('care_gap_population returns population-level gaps', async () => {
    const res = await callTool(request, 'care_gap_population', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  liveIt('care_gap_close closes a care gap (if one exists)', async () => {
    const listRes = await callTool(request, 'care_gap_list', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    const listData = JSON.parse(listRes.body.result.content[0].text);
    const gaps = listData.data?.gaps || listData.data?.entry || [];
    if (gaps.length === 0) {
      console.warn('No care gaps to close — skipping close test');
      return;
    }
    const gapId = gaps[0].id || gaps[0].gapId;
    const res = await callTool(request, 'care_gap_close', {
      gapId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      closedBy: 'live-test-runner',
      reason: 'Automated live test verification'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
