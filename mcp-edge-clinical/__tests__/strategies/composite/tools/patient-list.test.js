const { createDefinition } = require('../../../../lib/strategies/composite/tools/patient-list');

describe('patient_list tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('patient_list');
  });

  it('requires only tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['tenantId']);
  });

  it('does not require patientId', () => {
    expect(definition.inputSchema.required).not.toContain('patientId');
  });

  it('calls clinicalClient.get with default page=0 and size=20', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"content":[]}' });

    await definition.handler({ tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/api/v1/patients?page=0&size=20',
      { tenantId: 'acme-health' }
    );
  });

  it('uses provided page and size', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"content":[]}' });

    await definition.handler({ tenantId: 't1', page: 3, size: 50 });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/api/v1/patients?page=3&size=50',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking data', async () => {
    mockClient.get.mockRejectedValue(new Error('Forbidden'));

    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Forbidden');
    expect(parsed.data).toBeUndefined();
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
