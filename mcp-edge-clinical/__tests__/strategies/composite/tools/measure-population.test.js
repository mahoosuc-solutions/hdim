const { createDefinition } = require('../../../../lib/strategies/composite/tools/measure-population');

describe('measure_population tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('measure_population');
  });

  it('requires only tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['tenantId']);
  });

  it('does not accept patientId (non-PHI tool)', () => {
    expect(definition.inputSchema.properties).not.toHaveProperty('patientId');
  });

  it('calls GET /quality-measure/report/population with tenantId', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"totalPatients":1200}' });

    const result = await definition.handler({ tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/quality-measure/report/population',
      { tenantId: 'acme-health' }
    );

    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"totalPatients":1200}');
  });

  it('does not include patientId in request', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ tenantId: 't1' });
    const callPath = mockClient.get.mock.calls[0][0];
    expect(callPath).not.toContain('patient');
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Timeout'));

    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Timeout');
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
