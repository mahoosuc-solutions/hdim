const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

const ROLE_TOOL_MATRIX = {
  platform_admin:   { edge_health: true, platform_health: true, platform_info: true, fhir_metadata: true, service_catalog: true, dashboard_stats: true, demo_status: true, demo_seed: true },
  developer:        { edge_health: true, platform_health: true, platform_info: true, fhir_metadata: true, service_catalog: true, dashboard_stats: true, demo_status: true, demo_seed: true },
  clinical_admin:   { edge_health: true, platform_health: true, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  quality_officer:  { edge_health: true, platform_health: false, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  executive:        { edge_health: true, platform_health: false, platform_info: true, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  clinician:        { edge_health: true, platform_health: true, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: false, demo_status: false, demo_seed: false },
  care_coordinator: { edge_health: true, platform_health: false, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: false, demo_status: false, demo_seed: false },
};

jest.setTimeout(30_000);

const TOOL_ARGS = {
  demo_seed: { scenarioName: 'hedis-evaluation' }
};

describe('RBAC exhaustive matrix — platform edge (7 roles × 8 tools)', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const cases = [];
  for (const [role, tools] of Object.entries(ROLE_TOOL_MATRIX)) {
    for (const [tool, allowed] of Object.entries(tools)) {
      cases.push([role, tool, allowed]);
    }
  }

  it.each(cases)('%s %s → %s', async (role, tool, allowed) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', role)
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: TOOL_ARGS[tool] || {} } });
    if (allowed) {
      expect(res.body.error).toBeUndefined();
      expect(res.body.result).toBeDefined();
    } else {
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    }
  });
});
