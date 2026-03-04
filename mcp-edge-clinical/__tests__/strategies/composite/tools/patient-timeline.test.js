const { createDefinition } = require('../../../../lib/strategies/composite/tools/patient-timeline');

describe('patient_timeline tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('patient_timeline');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls clinicalClient.get with correct base path', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"events":[]}' });

    await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/patient/timeline/by-date?patient=p1',
      { tenantId: 'acme-health' }
    );
  });

  it('appends startDate when provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', startDate: '2025-01-01' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/patient/timeline/by-date?patient=p1&startDate=2025-01-01',
      { tenantId: 't1' }
    );
  });

  it('appends endDate when provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', endDate: '2025-12-31' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/patient/timeline/by-date?patient=p1&endDate=2025-12-31',
      { tenantId: 't1' }
    );
  });

  it('appends both startDate and endDate when provided', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1', startDate: '2025-01-01', endDate: '2025-12-31' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/patient/timeline/by-date?patient=p1&startDate=2025-01-01&endDate=2025-12-31',
      { tenantId: 't1' }
    );
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('Timeout'));

    const result = await definition.handler({ patientId: 'secret-patient-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);

    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('Timeout');
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
