const { clinicalRolePolicies } = require('../../../lib/strategies/full-surface/role-policies');

describe('full-surface role-policies', () => {
  const policies = clinicalRolePolicies();

  it('defines 7 roles', () => {
    expect(Object.keys(policies)).toHaveLength(7);
  });

  it('includes expected roles', () => {
    const roles = Object.keys(policies);
    expect(roles).toEqual(expect.arrayContaining([
      'clinical_admin', 'platform_admin', 'developer',
      'clinician', 'care_coordinator', 'quality_officer', 'executive'
    ]));
  });

  it('admin roles match all tools', () => {
    for (const role of ['clinical_admin', 'platform_admin', 'developer', 'clinician']) {
      const patterns = policies[role];
      expect(patterns.some(p => p.test('any_tool_name'))).toBe(true);
    }
  });

  it('executive role matches only summary tools', () => {
    const patterns = policies.executive;
    const match = (name) => patterns.some(p => p.test(name));
    expect(match('care_gap_stats')).toBe(true);
    expect(match('health_score')).toBe(true);
    expect(match('patient_read')).toBe(false);
    expect(match('cql_evaluate')).toBe(false);
  });

  it('care_coordinator matches care gap and patient tools', () => {
    const patterns = policies.care_coordinator;
    const match = (name) => patterns.some(p => p.test(name));
    expect(match('patient_read')).toBe(true);
    expect(match('care_gap_list')).toBe(true);
    expect(match('care_gap_stats')).toBe(true);
    expect(match('measure_results')).toBe(true);
    expect(match('cql_evaluate')).toBe(false);
  });

  it('quality_officer matches measure and cql tools', () => {
    const patterns = policies.quality_officer;
    const match = (name) => patterns.some(p => p.test(name));
    expect(match('measure_evaluate')).toBe(true);
    expect(match('cql_evaluate')).toBe(true);
    expect(match('care_gap_stats')).toBe(true);
    expect(match('patient_read')).toBe(false);
  });
});
