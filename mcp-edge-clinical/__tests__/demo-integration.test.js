/**
 * Demo Integration Test
 *
 * Enables HDIM_DEMO_MODE and calls every single composite tool via JSON-RPC,
 * verifying each returns valid fixture data with demoMode flag set.
 */
const supertest = require('supertest');

const DEMO_TOOLS = [
  { name: 'fhir_read', args: { resourceType: 'Patient', id: 'demo-1', tenantId: 'acme' } },
  { name: 'fhir_search', args: { resourceType: 'Observation', tenantId: 'acme' } },
  { name: 'fhir_create', args: { resourceType: 'Observation', resource: { resourceType: 'Observation' }, tenantId: 'acme' } },
  { name: 'fhir_bundle', args: { type: 'transaction', entries: [{}], tenantId: 'acme' } },
  { name: 'patient_summary', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'patient_timeline', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'patient_risk', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'patient_list', args: { tenantId: 'acme' } },
  { name: 'pre_visit_plan', args: { patientId: 'demo-1', providerId: 'prov-1', tenantId: 'acme' } },
  { name: 'care_gap_list', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'care_gap_identify', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'care_gap_close', args: { gapId: 'gap-1', tenantId: 'acme', closedBy: 'dr-1', reason: 'test' } },
  { name: 'care_gap_stats', args: { tenantId: 'acme' } },
  { name: 'care_gap_population', args: { tenantId: 'acme' } },
  { name: 'care_gap_provider', args: { providerId: 'prov-1', tenantId: 'acme' } },
  { name: 'measure_evaluate', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'measure_results', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'measure_score', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'measure_population', args: { tenantId: 'acme' } },
  { name: 'cds_patient_view', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'health_score', args: { patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'cql_evaluate', args: { library: 'test-lib', patientId: 'demo-1', tenantId: 'acme' } },
  { name: 'cql_batch', args: { library: 'test-lib', patientIds: ['demo-1'], tenantId: 'acme' } },
  { name: 'cql_libraries', args: { tenantId: 'acme' } },
  { name: 'cql_result', args: { patientId: 'demo-1', library: 'test-lib', tenantId: 'acme' } },
  { name: 'edge_health', args: {} },
];

describe('clinical edge demo integration — all 26 tools', () => {
  let request;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    jest.resetModules();
    const { createApp } = require('../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  it('has exactly 26 tool configs in the test matrix', () => {
    expect(DEMO_TOOLS.length).toBe(26);
    const names = DEMO_TOOLS.map(t => t.name);
    expect(new Set(names).size).toBe(26); // all unique
  });

  it.each(DEMO_TOOLS)(
    '$name — returns valid demo fixture',
    async ({ name, args }) => {
      const res = await request
        .post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({
          jsonrpc: '2.0',
          id: 1,
          method: 'tools/call',
          params: { name, arguments: args },
        });

      expect(res.status).toBe(200);

      // Must have result, no error
      expect(res.body.result).toBeDefined();
      expect(res.body.error).toBeUndefined();

      // Result must contain content array with text
      const content = res.body.result.content;
      expect(Array.isArray(content)).toBe(true);
      expect(content.length).toBeGreaterThanOrEqual(1);
      expect(content[0].type).toBe('text');

      // Content text must be valid JSON
      const text = content[0].text;
      let parsed;
      expect(() => { parsed = JSON.parse(text); }).not.toThrow();

      // Fixture must include demoMode flag
      expect(parsed.demoMode).toBe(true);

      // Fixture must report success (201 is valid for create operations)
      expect(parsed.ok).toBe(true);
      expect([200, 201]).toContain(parsed.status);
    },
  );
});
