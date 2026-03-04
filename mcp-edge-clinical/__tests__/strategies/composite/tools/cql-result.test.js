const { createDefinition } = require('../../../../lib/strategies/composite/tools/cql-result');

describe('cql_result tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('cql_result');
  });

  it('requires patientId, library, and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'library', 'tenantId']);
  });

  it('calls correct gateway path with patientId and library', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', library: 'HbA1c-Control', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/evaluations/patient/p1/library/HbA1c-Control/latest',
      { tenantId: 'acme-health' }
    );
  });

  it('encodes patientId and library in path', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'id/special', library: 'lib with spaces', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/cql/api/v1/cql/evaluations/patient/id%2Fspecial/library/lib%20with%20spaces/latest',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ patientId: 'p1', library: 'HbA1c-Control', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"evaluationId":"e1"}' });

    const result = await definition.handler({ patientId: 'p1', library: 'HbA1c-Control', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"evaluationId":"e1"}');
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler({ patientId: 'secret-patient-id', library: 'lib', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Connection refused');
    expect(parsed.data).toBeUndefined();
    expect(result.content[0].text).not.toContain('secret-patient-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const result = await definition.handler({ patientId: 'x', library: 'lib', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});
