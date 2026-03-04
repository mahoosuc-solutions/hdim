const { createDefinition } = require('../../../../lib/strategies/composite/tools/fhir-bundle');

describe('fhir_bundle tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('fhir_bundle');
  });

  it('requires type, entries, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['type', 'entries', 'tenantId']);
  });

  it('only allows transaction or batch type', () => {
    expect(definition.inputSchema.properties.type.enum).toEqual(['transaction', 'batch']);
  });

  it('posts Bundle with transaction type to /fhir/Bundle', async () => {
    const entries = [
      { resource: { resourceType: 'Patient' }, request: { method: 'POST', url: 'Patient' } }
    ];
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Bundle","type":"transaction-response"}' });

    const result = await definition.handler({ type: 'transaction', entries, tenantId: 'acme' });

    expect(mockClient.post).toHaveBeenCalledWith(
      '/fhir/Bundle',
      { resourceType: 'Bundle', type: 'transaction', entry: entries },
      { tenantId: 'acme' }
    );
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
  });

  it('posts Bundle with batch type', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ type: 'batch', entries: [], tenantId: 't1' });
    const postedBody = mockClient.post.mock.calls[0][1];
    expect(postedBody.type).toBe('batch');
    expect(postedBody.resourceType).toBe('Bundle');
  });

  it('returns valid JSON on success', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ type: 'transaction', entries: [], tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without patient data on failure', async () => {
    mockClient.post.mockRejectedValue(new Error('Server error'));

    const result = await definition.handler({ type: 'transaction', entries: [{ resource: { id: 'secret' } }], tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Server error');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue(null);

    const result = await definition.handler({ type: 'batch', entries: [], tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
  });
});
