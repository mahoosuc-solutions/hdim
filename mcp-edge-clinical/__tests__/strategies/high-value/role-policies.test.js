const { clinicalRolePolicies } = require('../../../lib/strategies/high-value/role-policies');

describe('high-value clinicalRolePolicies', () => {
  const policies = clinicalRolePolicies();

  const EXPECTED_ROLES = [
    'clinical_admin',
    'platform_admin',
    'developer',
    'clinician',
    'care_coordinator',
    'quality_officer',
    'executive'
  ];

  const ALL_15_TOOLS = [
    'patient_read', 'patient_search',
    'observation_read', 'observation_search',
    'condition_read', 'condition_search',
    'medication_read', 'medication_search',
    'encounter_read', 'encounter_search',
    'care_gap_list', 'care_gap_close', 'care_gap_stats',
    'measure_evaluate', 'measure_results'
  ];

  it('returns an object with exactly 7 roles', () => {
    expect(Object.keys(policies)).toHaveLength(7);
  });

  it.each(EXPECTED_ROLES)('includes role "%s" with a non-empty regex array', (role) => {
    expect(policies[role]).toBeDefined();
    expect(Array.isArray(policies[role])).toBe(true);
    expect(policies[role].length).toBeGreaterThan(0);
    policies[role].forEach((m) => expect(m).toBeInstanceOf(RegExp));
  });

  it('admin roles match all 15 tools', () => {
    for (const role of ['clinical_admin', 'platform_admin', 'developer']) {
      for (const tool of ALL_15_TOOLS) {
        expect(policies[role].some((m) => m.test(tool))).toBe(true);
      }
    }
  });

  it('clinician can access 14 tools (all except care_gap_stats)', () => {
    const clinician = policies.clinician;
    for (const tool of ALL_15_TOOLS) {
      if (tool === 'care_gap_stats') {
        expect(clinician.some((m) => m.test(tool))).toBe(false);
      } else {
        expect(clinician.some((m) => m.test(tool))).toBe(true);
      }
    }
  });

  it('care_coordinator can access patient_read, patient_search, care_gap_list, care_gap_close, care_gap_stats', () => {
    const cc = policies.care_coordinator;
    const allowed = ['patient_read', 'patient_search', 'care_gap_list', 'care_gap_close', 'care_gap_stats'];
    const denied = ALL_15_TOOLS.filter(t => !allowed.includes(t));

    for (const tool of allowed) {
      expect(cc.some((m) => m.test(tool))).toBe(true);
    }
    for (const tool of denied) {
      expect(cc.some((m) => m.test(tool))).toBe(false);
    }
  });

  it('quality_officer can access measure_evaluate, measure_results, care_gap_stats', () => {
    const qo = policies.quality_officer;
    const allowed = ['measure_evaluate', 'measure_results', 'care_gap_stats'];
    const denied = ALL_15_TOOLS.filter(t => !allowed.includes(t));

    for (const tool of allowed) {
      expect(qo.some((m) => m.test(tool))).toBe(true);
    }
    for (const tool of denied) {
      expect(qo.some((m) => m.test(tool))).toBe(false);
    }
  });

  it('executive can only access care_gap_stats', () => {
    const exec = policies.executive;
    expect(exec.some((m) => m.test('care_gap_stats'))).toBe(true);
    for (const tool of ALL_15_TOOLS.filter(t => t !== 'care_gap_stats')) {
      expect(exec.some((m) => m.test(tool))).toBe(false);
    }
  });

  it('returns a fresh object on each call (no shared mutation)', () => {
    const a = clinicalRolePolicies();
    const b = clinicalRolePolicies();
    expect(a).not.toBe(b);
    expect(a.executive).not.toBe(b.executive);
  });
});
