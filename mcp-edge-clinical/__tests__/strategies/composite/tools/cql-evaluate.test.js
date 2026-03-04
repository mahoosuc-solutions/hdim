const { createDefinition } = require('../../../../lib/strategies/composite/tools/cql-evaluate');

describe('cql_evaluate tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('cql_evaluate');
  });

  it('requires library, patientId, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['library', 'patientId', 'tenantId']);
  });

  it('calls correct gateway path with library and patientId in query string', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ library: 'HbA1c-Control', patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/cql/evaluate?library=HbA1c-Control&patient=p1',
      {},
      { tenantId: 'acme-health' }
    );
  });

  it('encodes library and patientId in query string', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ library: 'lib with spaces', patientId: 'id/special', tenantId: 't1' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/cql/evaluate?library=lib%20with%20spaces&patient=id%2Fspecial',
      {},
      { tenantId: 't1' }
    );
  });

  it('posts empty body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ library: 'HbA1c-Control', patientId: 'p1', tenantId: 't1' });
    expect(mockClient.post.mock.calls[0][1]).toEqual({});
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"result":"pass"}' });
    const result = await definition.handler({ library: 'HbA1c-Control', patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"numerator":true}' });

    const result = await definition.handler({ library: 'HbA1c-Control', patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"numerator":true}');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.post.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ library: 'HbA1c-Control', patientId: 'secret-patient-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue('string error');

    const result = await definition.handler({ library: 'lib', patientId: 'x', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
