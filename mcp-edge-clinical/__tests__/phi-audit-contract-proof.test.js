/**
 * PHI Audit Annotation Contract Proof
 *
 * Structural proof that every clinical tool has a valid audit annotation.
 * Verifies PHI classification completeness, write operation correctness,
 * and patientIdArg validity across all 3 strategies.
 *
 * HIPAA 164.312(b) — audit controls must cover all access points.
 */

describe('PHI audit annotation contract', () => {

  function loadToolsForStrategy(strategy) {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = strategy;
    jest.resetModules();
    const { createClinicalClient } = require('../lib/clinical-client');
    const client = createClinicalClient();
    const strat = require(`../lib/strategies/${strategy}`);
    const tools = strat.loadTools(client);
    // Add edge_health (always loaded)
    tools.push(require('../lib/tools/edge-health').definition);
    delete process.env.CLINICAL_TOOL_STRATEGY;
    return tools;
  }

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  describe.each(['composite', 'high-value', 'full-surface'])('%s strategy', (strategy) => {

    let tools;
    beforeAll(() => {
      tools = loadToolsForStrategy(strategy);
    });

    it('every tool has an audit field or is edge_health', () => {
      const missing = tools
        .filter(t => t.name !== 'edge_health' && !t.audit)
        .map(t => t.name);
      expect(missing).toEqual([]);
    });

    it('every audit field has phi (boolean), write (boolean)', () => {
      const invalid = tools
        .filter(t => t.audit)
        .filter(t => typeof t.audit.phi !== 'boolean' || typeof t.audit.write !== 'boolean')
        .map(t => t.name);
      expect(invalid).toEqual([]);
    });

    it('every phi:true tool with write:true is a known write tool', () => {
      const KNOWN_WRITE_TOOLS = [
        'fhir_create', 'fhir_bundle', 'care_gap_close',
        // Factory-generated *_create tools
        'patient_create', 'observation_create', 'encounter_create',
        'condition_create', 'medication_request_create', 'immunization_create',
        'allergy_intolerance_create', 'procedure_create',
        'document_reference_create', 'care_plan_create', 'goal_create',
        'appointment_create', 'task_create'
      ];
      const writeTools = tools
        .filter(t => t.audit?.phi && t.audit?.write)
        .map(t => t.name);
      for (const wt of writeTools) {
        expect(KNOWN_WRITE_TOOLS).toContain(wt);
      }
    });

    it('every tool with patientIdArg references a valid input property', () => {
      const invalid = tools
        .filter(t => t.audit?.patientIdArg)
        .filter(t => {
          const props = t.inputSchema?.properties || {};
          return !props[t.audit.patientIdArg];
        })
        .map(t => `${t.name} -> ${t.audit.patientIdArg}`);
      expect(invalid).toEqual([]);
    });

    it('no phi:false tool has write:true', () => {
      const bad = tools
        .filter(t => t.audit && !t.audit.phi && t.audit.write)
        .map(t => t.name);
      expect(bad).toEqual([]);
    });

    it('no phi:false tool has patientIdArg', () => {
      const bad = tools
        .filter(t => t.audit && !t.audit.phi && t.audit.patientIdArg)
        .map(t => t.name);
      expect(bad).toEqual([]);
    });
  });

  describe('PHI completeness proof', () => {
    it('all FHIR patient-data tools are classified as phi:true', () => {
      const tools = loadToolsForStrategy('composite');
      const KNOWN_PHI_TOOLS = [
        'fhir_read', 'fhir_search', 'fhir_create', 'fhir_bundle',
        'patient_summary', 'patient_timeline', 'patient_risk', 'patient_list',
        'pre_visit_plan', 'care_gap_list', 'care_gap_identify', 'care_gap_close',
        'care_gap_provider', 'measure_evaluate', 'measure_results', 'measure_score',
        'cds_patient_view', 'health_score',
        'cql_evaluate', 'cql_batch', 'cql_result'
      ];
      for (const name of KNOWN_PHI_TOOLS) {
        const tool = tools.find(t => t.name === name);
        expect(tool).toBeDefined();
        expect(tool.audit?.phi).toBe(true);
      }
    });

    it('all aggregate/reference tools are classified as phi:false', () => {
      const tools = loadToolsForStrategy('composite');
      const KNOWN_NON_PHI = [
        'care_gap_stats', 'care_gap_population', 'measure_population', 'cql_libraries'
      ];
      for (const name of KNOWN_NON_PHI) {
        const tool = tools.find(t => t.name === name);
        expect(tool).toBeDefined();
        expect(tool.audit?.phi).toBe(false);
      }
    });
  });

  describe('full-surface factory PHI derivation', () => {
    it('Organization/Practitioner/PractitionerRole tools are phi:false', () => {
      const tools = loadToolsForStrategy('full-surface');
      const nonPhiPrefixes = ['organization', 'practitioner', 'practitioner_role'];
      const nonPhiTools = tools.filter(t =>
        nonPhiPrefixes.some(p => t.name.startsWith(p))
      );
      expect(nonPhiTools.length).toBeGreaterThan(0);
      for (const t of nonPhiTools) {
        expect(t.audit?.phi).toBe(false);
      }
    });

    it('Patient/Observation/Condition factory tools are phi:true', () => {
      const tools = loadToolsForStrategy('full-surface');
      const phiPrefixes = ['patient', 'observation', 'condition'];
      const phiTools = tools.filter(t =>
        phiPrefixes.some(p => t.name.startsWith(p))
      );
      expect(phiTools.length).toBeGreaterThan(0);
      for (const t of phiTools) {
        expect(t.audit?.phi).toBe(true);
      }
    });
  });
});
