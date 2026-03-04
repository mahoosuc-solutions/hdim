const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => {
  process.env.HDIM_DEMO_MODE = 'true';
  process.env.MCP_EDGE_RATE_LIMIT_MAX = '500';
});
afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
  delete process.env.MCP_EDGE_RATE_LIMIT_MAX;
});

// All 26 composite tools (25 clinical + 1 infrastructure)
const ALL_TOOLS = [
  'patient_summary', 'patient_timeline', 'patient_risk', 'patient_list', 'pre_visit_plan',
  'care_gap_list', 'care_gap_identify', 'care_gap_close', 'care_gap_stats', 'care_gap_population', 'care_gap_provider',
  'fhir_read', 'fhir_search', 'fhir_create', 'fhir_bundle',
  'cds_patient_view', 'health_score',
  'measure_evaluate', 'measure_results', 'measure_score', 'measure_population',
  'cql_evaluate', 'cql_batch', 'cql_libraries', 'cql_result',
  'edge_health'
];

// Minimum valid arguments per tool (needed when auth passes and param validation runs)
const TOOL_ARGS = {
  patient_summary:    { patientId: 'p1', tenantId: 't1' },
  patient_timeline:   { patientId: 'p1', tenantId: 't1' },
  patient_risk:       { patientId: 'p1', tenantId: 't1' },
  patient_list:       { tenantId: 't1' },
  pre_visit_plan:     { patientId: 'p1', providerId: 'pr1', tenantId: 't1' },
  care_gap_list:      { patientId: 'p1', tenantId: 't1' },
  care_gap_identify:  { patientId: 'p1', tenantId: 't1' },
  care_gap_close:     { gapId: 'g1', tenantId: 't1', closedBy: 'nurse', reason: 'done' },
  care_gap_stats:     { tenantId: 't1' },
  care_gap_population:{ tenantId: 't1' },
  care_gap_provider:  { providerId: 'pr1', tenantId: 't1' },
  fhir_read:          { resourceType: 'Patient', id: 'id1', tenantId: 't1' },
  fhir_search:        { resourceType: 'Patient', tenantId: 't1' },
  fhir_create:        { resourceType: 'Patient', resource: { resourceType: 'Patient' }, tenantId: 't1' },
  fhir_bundle:        { type: 'transaction', entries: [{}], tenantId: 't1' },
  cds_patient_view:   { patientId: 'p1', tenantId: 't1' },
  health_score:       { patientId: 'p1', tenantId: 't1' },
  measure_evaluate:   { patientId: 'p1', tenantId: 't1' },
  measure_results:    { patientId: 'p1', tenantId: 't1' },
  measure_score:      { patientId: 'p1', tenantId: 't1' },
  measure_population: { tenantId: 't1' },
  cql_evaluate:       { library: 'lib1', patientId: 'p1', tenantId: 't1' },
  cql_batch:          { library: 'lib1', patientIds: ['p1'], tenantId: 't1' },
  cql_libraries:      { tenantId: 't1' },
  cql_result:         { patientId: 'p1', library: 'lib1', tenantId: 't1' },
  edge_health:        {}
};

// Per-role allowed tool sets
const CLINICIAN_TOOLS = new Set([
  'patient_summary', 'patient_timeline', 'patient_risk', 'patient_list', 'pre_visit_plan',
  'care_gap_list', 'care_gap_identify', 'care_gap_close', 'care_gap_provider',
  'fhir_read', 'fhir_search', 'fhir_create', 'fhir_bundle',
  'cds_patient_view', 'health_score',
  'measure_evaluate', 'measure_results', 'measure_score',
  'cql_evaluate', 'cql_result'
]);

const CARE_COORDINATOR_TOOLS = new Set([
  'patient_summary', 'patient_list', 'pre_visit_plan',
  'care_gap_list', 'care_gap_identify', 'care_gap_close',
  'care_gap_stats', 'care_gap_population', 'care_gap_provider'
]);

const QUALITY_OFFICER_TOOLS = new Set([
  'measure_evaluate', 'measure_results', 'measure_score', 'measure_population',
  'cql_evaluate', 'cql_batch', 'cql_libraries', 'cql_result',
  'care_gap_stats', 'care_gap_population'
]);

const EXECUTIVE_TOOLS = new Set([
  'care_gap_stats', 'care_gap_population', 'measure_population', 'health_score'
]);

// Build exhaustive 7x26 = 182 matrix
function allowed(role, tool) {
  if (['clinical_admin', 'platform_admin', 'developer'].includes(role)) return true;
  if (role === 'clinician') return CLINICIAN_TOOLS.has(tool);
  if (role === 'care_coordinator') return CARE_COORDINATOR_TOOLS.has(tool);
  if (role === 'quality_officer') return QUALITY_OFFICER_TOOLS.has(tool);
  if (role === 'executive') return EXECUTIVE_TOOLS.has(tool);
  return false;
}

const ROLE_TOOL_MATRIX = {};
const ROLES = ['clinical_admin', 'platform_admin', 'developer', 'clinician', 'care_coordinator', 'quality_officer', 'executive'];
for (const role of ROLES) {
  ROLE_TOOL_MATRIX[role] = {};
  for (const tool of ALL_TOOLS) {
    ROLE_TOOL_MATRIX[role][tool] = allowed(role, tool);
  }
}

jest.setTimeout(30_000);

describe('RBAC exhaustive matrix — clinical edge (7 roles x 26 tools = 182 cases)', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  // Flatten matrix into test cases
  const cases = [];
  for (const [role, tools] of Object.entries(ROLE_TOOL_MATRIX)) {
    for (const [tool, isAllowed] of Object.entries(tools)) {
      cases.push([role, tool, isAllowed]);
    }
  }

  // Verify we have exactly 182 cases
  it('matrix covers 182 role-tool pairs', () => {
    expect(cases).toHaveLength(182);
  });

  it.each(cases)('%s %s → %s', async (role, tool, isAllowed) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', role)
      .send({
        jsonrpc: '2.0',
        id: 1,
        method: 'tools/call',
        params: { name: tool, arguments: TOOL_ARGS[tool] || {} }
      });

    if (isAllowed) {
      expect(res.body.error).toBeUndefined();
      expect(res.body.result).toBeDefined();
    } else {
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    }
  });
});
