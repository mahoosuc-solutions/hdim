const { createDefinition } = require('../../../../lib/strategies/composite/tools/fhir-create');

describe('fhir_create tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('fhir_create');
  });

  it('requires resourceType, resource, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['resourceType', 'resource', 'tenantId']);
  });

  it('calls clinicalClient.post with correct path, body, and tenantId', async () => {
    const resource = { resourceType: 'Patient', name: [{ family: 'Doe', given: ['John'] }] };
    mockClient.post.mockResolvedValue({ status: 201, ok: true, body: '{"id":"new-uuid"}' });

    const result = await definition.handler({ resourceType: 'Patient', resource, tenantId: 'acme' });

    expect(mockClient.post).toHaveBeenCalledWith('/fhir/Patient', resource, { tenantId: 'acme' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(201);
    expect(parsed.ok).toBe(true);
    expect(parsed.resourceType).toBe('Patient');
    expect(parsed.data).toBe('{"id":"new-uuid"}');
  });

  it('returns valid JSON on success', async () => {
    mockClient.post.mockResolvedValue({ status: 201, ok: true, body: '{}' });
    const result = await definition.handler({ resourceType: 'Observation', resource: {}, tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without patient data on failure', async () => {
    const resource = { resourceType: 'Patient', id: 'secret-data' };
    mockClient.post.mockRejectedValue(new Error('Validation failed'));

    const result = await definition.handler({ resourceType: 'Patient', resource, tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Validation failed');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-data');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue(42);

    const result = await definition.handler({ resourceType: 'Patient', resource: {}, tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('42');
  });
});
