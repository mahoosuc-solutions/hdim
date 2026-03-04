const { createDefinition } = require('../../../../lib/strategies/composite/tools/cds-patient-view');

describe('cds_patient_view tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('cds_patient_view');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls POST /quality-measure/cds-services/patient-view with CDS Hooks body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"cards":[]}' });

    await definition.handler({ patientId: 'p1', tenantId: 'acme-health' });

    expect(mockClient.post).toHaveBeenCalledTimes(1);
    const [path, body, opts] = mockClient.post.mock.calls[0];
    expect(path).toBe('/quality-measure/cds-services/patient-view');
    expect(opts).toEqual({ tenantId: 'acme-health' });

    // Verify CDS Hooks structure
    expect(body.hook).toBe('patient-view');
    expect(body.context.patientId).toBe('p1');
    expect(body.context.userId).toBe('mcp-edge-user');
  });

  it('generates hookInstance with mcp-edge- prefix', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });

    await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const body = mockClient.post.mock.calls[0][1];
    expect(body.hookInstance).toMatch(/^mcp-edge-\d+$/);
  });

  it('returns valid JSON in MCP content format', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"cards":[]}' });
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    expect(() => JSON.parse(result.content[0].text)).not.toThrow();
  });

  it('returns response data correctly', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{"cards":[{"summary":"Due for screening"}]}' });

    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('{"cards":[{"summary":"Due for screening"}]}');
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
