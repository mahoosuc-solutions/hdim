const { createDefinition } = require('../../../../lib/strategies/composite/tools/cql-libraries');

describe('cql_libraries tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('cql_libraries');
  });

  it('requires tenantId only', () => {
    expect(definition.inputSchema.required).toEqual(['tenantId']);
  });

  it('has optional status property', () => {
    expect(definition.inputSchema.properties.status).toBeDefined();
    expect(definition.inputSchema.properties.status.type).toBe('string');
  });

  it('calls /cql/api/v1/cql/libraries without status param', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/libraries',
      { tenantId: 'acme-health' }
    );
  });

  it('appends status query param when provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ tenantId: 't1', status: 'active' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/libraries?status=active',
      { tenantId: 't1' }
    );
  });

  it('encodes status in query string', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ tenantId: 't1', status: 'in review' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/libraries?status=in%20review',
      { tenantId: 't1' }
    );
  });

  it('does not append status when undefined', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ tenantId: 't1', status: undefined });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/libraries',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    const result = await definition.handler({ tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[{"id":"lib1"}]' });

    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('[{"id":"lib1"}]');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
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
