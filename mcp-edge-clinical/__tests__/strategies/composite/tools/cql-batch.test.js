const { createDefinition } = require('../../../../lib/strategies/composite/tools/cql-batch');

describe('cql_batch tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('cql_batch');
  });

  it('requires library, patientIds, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['library', 'patientIds', 'tenantId']);
  });

  it('calls correct gateway path with tenantId', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ library: 'HbA1c-Control', patientIds: ['p1', 'p2'], tenantId: 'acme-health' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/cql/api/v1/cql/evaluations/batch',
      { libraryId: 'HbA1c-Control', patientIds: ['p1', 'p2'] },
      { tenantId: 'acme-health' }
    );
  });

  it('maps library to libraryId in request body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ library: 'BCS', patientIds: ['p1'], tenantId: 't1' });
    const body = mockClient.post.mock.calls[0][1];
    expect(body.libraryId).toBe('BCS');
    expect(body.library).toBeUndefined();
  });

  it('passes patientIds array in body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    const ids = ['p1', 'p2', 'p3'];
    await definition.handler({ library: 'HbA1c-Control', patientIds: ids, tenantId: 't1' });
    const body = mockClient.post.mock.calls[0][1];
    expect(body.patientIds).toEqual(ids);
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    const result = await definition.handler({ library: 'HbA1c-Control', patientIds: ['p1'], tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '[{"patientId":"p1","result":"pass"}]' });

    const result = await definition.handler({ library: 'HbA1c-Control', patientIds: ['p1'], tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('[{"patientId":"p1","result":"pass"}]');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.post.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ library: 'lib', patientIds: ['secret-id'], tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue('string error');

    const result = await definition.handler({ library: 'lib', patientIds: ['x'], tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
