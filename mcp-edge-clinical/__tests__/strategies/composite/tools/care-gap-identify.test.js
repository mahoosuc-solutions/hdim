const { createDefinition } = require('../../../../lib/strategies/composite/tools/care-gap-identify');

describe('care_gap_identify tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_identify');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls clinicalClient.post with correct path and body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"gaps":[]}' });

    const result = await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/care-gap/identify',
      { patientId: 'p1', tenantId: 'acme-health' },
      { tenantId: 'acme-health' }
    );

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
  });

  it('includes library in body when provided', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', library: 'HbA1c-Control' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/care-gap/identify',
      { patientId: 'p1', tenantId: 't1', library: 'HbA1c-Control' },
      { tenantId: 't1' }
    );
  });

  it('does not include library in body when not provided', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const callBody = mockClient.post.mock.calls[0][1];
    expect(callBody).not.toHaveProperty('library');
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.post.mockRejectedValue(new Error('Service unavailable'));

    const result = await definition.handler({ patientId: 'secret-patient-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Service unavailable');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue('string error');

    const result = await definition.handler({ patientId: 'x', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
