const { clinicalRolePolicies } = require('../../../lib/strategies/composite/role-policies');

describe('clinicalRolePolicies', () => {
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

  it('returns an object with exactly 7 roles', () => {
    expect(Object.keys(policies)).toHaveLength(7);
  });

  it.each(EXPECTED_ROLES)('includes role "%s" with a non-empty regex array', (role) => {
    expect(policies[role]).toBeDefined();
    expect(Array.isArray(policies[role])).toBe(true);
    expect(policies[role].length).toBeGreaterThan(0);
    policies[role].forEach((m) => expect(m).toBeInstanceOf(RegExp));
  });

  it('admin roles match any tool name', () => {
    for (const role of ['clinical_admin', 'platform_admin', 'developer']) {
      expect(policies[role].some((m) => m.test('anything'))).toBe(true);
    }
  });

  it('executive role only matches expected tools', () => {
    const exec = policies.executive;
    expect(exec.some((m) => m.test('care_gap_stats'))).toBe(true);
    expect(exec.some((m) => m.test('health_score'))).toBe(true);
    expect(exec.some((m) => m.test('patient_summary'))).toBe(false);
    expect(exec.some((m) => m.test('fhir_read'))).toBe(false);
  });

  it('returns a fresh object on each call (no shared mutation)', () => {
    const a = clinicalRolePolicies();
    const b = clinicalRolePolicies();
    expect(a).not.toBe(b);
    expect(a.executive).not.toBe(b.executive);
  });
});
