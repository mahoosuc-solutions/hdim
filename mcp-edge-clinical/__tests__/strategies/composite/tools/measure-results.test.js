const { createDefinition } = require('../../../../lib/strategies/composite/tools/measure-results');

describe('measure_results tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('measure_results');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls GET /quality-measure/results with patient query param', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });

    await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/quality-measure/results?patient=p1',
      { tenantId: 'acme-health' }
    );
  });

  it('encodes patientId in URL', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    await definition.handler({ patientId: 'id with spaces', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/quality-measure/results?patient=id%20with%20spaces',
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
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[{"measureId":"HbA1c"}]' });

    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('[{"measureId":"HbA1c"}]');
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
});
