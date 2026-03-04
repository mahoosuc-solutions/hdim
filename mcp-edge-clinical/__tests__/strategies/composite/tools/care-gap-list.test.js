const { createDefinition } = require('../../../../lib/strategies/composite/tools/care-gap-list');

describe('care_gap_list tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_list');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls /care-gap/open by default when no status provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/open?patient=p1',
      { tenantId: 'acme-health' }
    );
  });

  it('maps status "open" to /care-gap/open', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'open' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/open?patient=p1',
      { tenantId: 't1' }
    );
  });

  it('maps status "high-priority" to /care-gap/high-priority', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'high-priority' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/high-priority?patient=p1',
      { tenantId: 't1' }
    );
  });

  it('maps status "overdue" to /care-gap/overdue', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'overdue' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/overdue?patient=p1',
      { tenantId: 't1' }
    );
  });

  it('maps status "upcoming" to /care-gap/upcoming', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'upcoming' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/upcoming?patient=p1',
      { tenantId: 't1' }
    );
  });

  it('defaults to /care-gap/open for unknown status', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'unknown' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/open?patient=p1',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[{"id":"gap1"}]' });

    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('[{"id":"gap1"}]');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ patientId: 'secret-patient-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ patientId: 'x', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });

  it('encodes patientId in URL', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    await definition.handler({ patientId: 'id with spaces', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/care-gap/open?patient=id%20with%20spaces',
      { tenantId: 't1' }
    );
  });
});
