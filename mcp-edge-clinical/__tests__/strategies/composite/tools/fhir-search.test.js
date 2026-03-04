const { createDefinition } = require('../../../../lib/strategies/composite/tools/fhir-search');

describe('fhir_search tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('fhir_search');
  });

  it('requires resourceType and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['resourceType', 'tenantId']);
  });

  it('does not require patient or params', () => {
    expect(definition.inputSchema.required).not.toContain('patient');
    expect(definition.inputSchema.required).not.toContain('params');
  });

  it('calls GET with no query string when only resourceType and tenantId provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Bundle","entry":[]}' });

    await definition.handler({ resourceType: 'Patient', tenantId: 'acme' });
    expect(mockClient.get).toHaveBeenCalledWith('/fhir/Patient', { tenantId: 'acme' });
  });

  it('includes patient param in query string', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ resourceType: 'Observation', tenantId: 't1', patient: 'p123' });
    const calledPath = mockClient.get.mock.calls[0][0];
    expect(calledPath).toContain('/fhir/Observation');
    expect(calledPath).toContain('patient=p123');
  });

  it('includes additional params in query string', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({
      resourceType: 'Condition',
      tenantId: 't1',
      params: { _count: '10', category: 'encounter-diagnosis' }
    });
    const calledPath = mockClient.get.mock.calls[0][0];
    expect(calledPath).toContain('_count=10');
    expect(calledPath).toContain('category=encounter-diagnosis');
  });

  it('combines patient and params in query string', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({
      resourceType: 'Observation',
      tenantId: 't1',
      patient: 'p1',
      params: { _count: '5' }
    });
    const calledPath = mockClient.get.mock.calls[0][0];
    expect(calledPath).toContain('patient=p1');
    expect(calledPath).toContain('_count=5');
  });

  it('returns valid JSON with resourceType on success', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"total":3}' });

    const result = await definition.handler({ resourceType: 'Encounter', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.resourceType).toBe('Encounter');
  });

  it('returns error without patient data on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('Timeout'));

    const result = await definition.handler({ resourceType: 'Patient', tenantId: 't1', patient: 'secret-patient-id' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Timeout');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient-id');
  });

  it('handles non-Error throw values in catch block', async () => {
    mockClient.get.mockRejectedValue('raw string error');

    const result = await definition.handler({ resourceType: 'Patient', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('raw string error');
    expect(parsed.resourceType).toBe('Patient');
  });
});
