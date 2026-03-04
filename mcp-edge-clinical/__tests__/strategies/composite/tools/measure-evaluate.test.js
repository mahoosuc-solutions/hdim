const { createDefinition } = require('../../../../lib/strategies/composite/tools/measure-evaluate');

describe('measure_evaluate tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('measure_evaluate');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('has optional measureId in schema', () => {
    expect(definition.inputSchema.properties).toHaveProperty('measureId');
    expect(definition.inputSchema.required).not.toContain('measureId');
  });

  it('calls POST /quality-measure/calculate without measureId', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"result":"ok"}' });

    await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/quality-measure/calculate',
      { patientId: 'p1' },
      { tenantId: 'acme-health' }
    );
  });

  it('includes measureId in body when provided', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', measureId: 'HbA1c-Control' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/quality-measure/calculate',
      { patientId: 'p1', measureId: 'HbA1c-Control' },
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"evaluated":true}' });

    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"evaluated":true}');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.post.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ patientId: 'secret-patient-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
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
