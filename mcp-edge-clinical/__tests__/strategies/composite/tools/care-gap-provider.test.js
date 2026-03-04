const { createDefinition } = require('../../../../lib/strategies/composite/tools/care-gap-provider');

describe('care_gap_provider tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_provider');
  });

  it('requires providerId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['providerId', 'tenantId']);
  });

  it('calls clinicalClient.get with correct path and tenantId', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"gaps":[]}' });

    const result = await definition.handler({ providerId: 'prov-001', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/providers/prov-001/prioritized',
      { tenantId: 'acme-health' }
    );

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
  });

  it('encodes providerId in URL', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ providerId: 'id with spaces', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/providers/id%20with%20spaces/prioritized',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ providerId: 'prov-001', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Not Found'));

    const result = await definition.handler({ providerId: 'secret-provider', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Not Found');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-provider');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ providerId: 'x', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
