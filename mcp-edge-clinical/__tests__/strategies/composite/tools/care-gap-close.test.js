const { createDefinition } = require('../../../../lib/strategies/composite/tools/care-gap-close');

describe('care_gap_close tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_close');
  });

  it('requires gapId, tenantId, closedBy, and reason', () => {
    expect(definition.inputSchema.required).toEqual(['gapId', 'tenantId', 'closedBy', 'reason']);
  });

  it('calls clinicalClient.post with correctly mapped body fields', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"closed":true}' });

    const result = await definition.handler({
      gapId: 'gap-123',
      tenantId: 'acme-health',
      closedBy: 'dr-smith',
      reason: 'HbA1c result within target range'
    });

    expect(mockClient.post).toHaveBeenCalledWith(
      '/care-gap/close',
      {
        careGapId: 'gap-123',
        closedBy: 'dr-smith',
        closureReason: 'HbA1c result within target range',
        tenantId: 'acme-health'
      },
      { tenantId: 'acme-health' }
    );

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"closed":true}');
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ gapId: 'g1', tenantId: 't1', closedBy: 'u1', reason: 'r1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.post.mockRejectedValue(new Error('Forbidden'));

    const result = await definition.handler({
      gapId: 'secret-gap-id',
      tenantId: 't1',
      closedBy: 'secret-user',
      reason: 'secret reason'
    });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Forbidden');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-gap-id');
    expect(result.content[0].text).not.toContain('secret-user');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue('string error');

    const result = await definition.handler({ gapId: 'g1', tenantId: 't1', closedBy: 'u1', reason: 'r1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
