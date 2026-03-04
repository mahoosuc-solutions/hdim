const { createDefinition } = require('../../../../lib/strategies/composite/tools/pre-visit-plan');

describe('pre_visit_plan tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('pre_visit_plan');
  });

  it('requires patientId, providerId, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'providerId', 'tenantId']);
  });

  it('calls clinicalClient.get with both providerId and patientId in path', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"summary":"pre-visit"}' });

    const result = await definition.handler({ patientId: 'pat-1', providerId: 'prov-1', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/api/v1/providers/prov-1/patients/pat-1/pre-visit-summary',
      { tenantId: 'acme-health' }
    );

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
  });

  it('encodes providerId and patientId in URL', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'id with spaces', providerId: 'prov/special', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/api/v1/providers/prov%2Fspecial/patients/id%20with%20spaces/pre-visit-summary',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ patientId: 'p1', providerId: 'pr1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Not found'));

    const result = await definition.handler({ patientId: 'secret-patient', providerId: 'secret-provider', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Not found');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient');
    expect(result.content[0].text).not.toContain('secret-provider');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ patientId: 'x', providerId: 'y', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
