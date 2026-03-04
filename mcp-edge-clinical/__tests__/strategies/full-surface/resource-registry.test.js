const { RESOURCE_REGISTRY } = require('../../../lib/strategies/full-surface/resource-registry');

describe('full-surface resource-registry', () => {
  it('has 20 entries', () => {
    expect(RESOURCE_REGISTRY).toHaveLength(20);
  });

  it('all entries have type, searchable, creatable properties', () => {
    for (const entry of RESOURCE_REGISTRY) {
      expect(typeof entry.type).toBe('string');
      expect(entry.type.length).toBeGreaterThan(0);
      expect(typeof entry.searchable).toBe('boolean');
      expect(typeof entry.creatable).toBe('boolean');
    }
  });

  it('types are unique', () => {
    const types = RESOURCE_REGISTRY.map(r => r.type);
    expect(new Set(types).size).toBe(types.length);
  });

  it('includes expected resource types', () => {
    const types = RESOURCE_REGISTRY.map(r => r.type);
    expect(types).toContain('Patient');
    expect(types).toContain('Observation');
    expect(types).toContain('MedicationRequest');
    expect(types).toContain('Bundle');
    expect(types).toContain('PractitionerRole');
  });

  it('Bundle is not searchable or creatable', () => {
    const bundle = RESOURCE_REGISTRY.find(r => r.type === 'Bundle');
    expect(bundle.searchable).toBe(false);
    expect(bundle.creatable).toBe(false);
  });
});
