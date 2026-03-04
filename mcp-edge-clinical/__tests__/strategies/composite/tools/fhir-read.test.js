const { createDefinition, VALID_RESOURCE_TYPES } = require('../../../../lib/strategies/composite/tools/fhir-read');

describe('fhir_read tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('fhir_read');
  });

  it('requires resourceType, id, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['resourceType', 'id', 'tenantId']);
  });

  it('exports 20 valid FHIR resource types', () => {
    expect(VALID_RESOURCE_TYPES).toHaveLength(20);
    expect(VALID_RESOURCE_TYPES).toContain('Patient');
    expect(VALID_RESOURCE_TYPES).toContain('Observation');
    expect(VALID_RESOURCE_TYPES).toContain('Bundle');
  });

  it('enumerates resource types in schema', () => {
    expect(definition.inputSchema.properties.resourceType.enum).toEqual(VALID_RESOURCE_TYPES);
  });

  it('calls clinicalClient.get with correct path and tenantId', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Patient","id":"p1"}' });

    const result = await definition.handler({ resourceType: 'Patient', id: 'p1', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith('/fhir/Patient/p1', { tenantId: 'acme-health' });

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.resourceType).toBe('Patient');
    expect(parsed.data).toBe('{"resourceType":"Patient","id":"p1"}');
  });

  it('returns valid JSON on success', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ resourceType: 'Observation', id: 'o1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without patient data on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ resourceType: 'Patient', id: 'secret-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
    expect(parsed.resourceType).toBe('Patient');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ resourceType: 'Patient', id: 'x', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
