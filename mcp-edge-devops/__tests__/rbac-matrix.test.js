const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

const TOOL_ARGS = {
  docker_logs: { service: 'fhir-service' },
  docker_restart: { service: 'fhir-service' }
};

const ROLE_TOOL_MATRIX = {
  platform_admin:   { edge_health: true, docker_status: true, docker_logs: true, docker_restart: true, service_dependencies: true, compose_config: true, build_status: true },
  developer:        { edge_health: true, docker_status: true, docker_logs: true, docker_restart: true, service_dependencies: true, compose_config: true, build_status: true },
  clinical_admin:   { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  quality_officer:  { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  executive:        { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  clinician:        { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  care_coordinator: { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
};

jest.setTimeout(30_000);

describe('PROOF: RBAC Exhaustive Matrix — devops edge (7 roles × 7 tools = 49 cases)', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const cases = [];
  for (const [role, tools] of Object.entries(ROLE_TOOL_MATRIX)) {
    for (const [tool, allowed] of Object.entries(tools)) {
      cases.push([role, tool, allowed]);
    }
  }

  it.each(cases)('%s → %s → allowed=%s', async (role, tool, allowed) => {
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
