const { createDefinition } = require('../../lib/tools/dashboard-stats');

describe('dashboard_stats tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { get: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('dashboard_stats');
  });

  it('has an inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('returns dashboard data on success', async () => {
    mockClient.get.mockResolvedValue({
      status: 200,
      ok: true,
      body: { totalPatients: 42, activeMeasures: 5 }
    });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(mockClient.get).toHaveBeenCalledWith('/analytics/dashboards');
    expect(payload.status).toBe(200);
    expect(payload.ok).toBe(true);
    expect(payload.data).toEqual({ totalPatients: 42, activeMeasures: 5 });
  });

  it('returns error message on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Connection refused');
  });

  it('handles non-Error throws', async () => {
    mockClient.get.mockRejectedValue('unexpected string error');

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('unexpected string error');
  });
});
