const { createDefinition } = require('../../lib/tools/demo-status');

describe('demo_status tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { get: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('demo_status');
  });

  it('has an inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('returns demo status on success', async () => {
    mockClient.get.mockResolvedValue({
      status: 200,
      ok: true,
      body: { seeded: true, scenarios: ['hedis-evaluation'] }
    });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(mockClient.get).toHaveBeenCalledWith('/api/v1/demo/status');
    expect(payload.status).toBe(200);
    expect(payload.ok).toBe(true);
    expect(payload.data).toEqual({ seeded: true, scenarios: ['hedis-evaluation'] });
  });

  it('returns error message on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('Service unavailable'));

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Service unavailable');
  });

  it('handles non-Error throws', async () => {
    mockClient.get.mockRejectedValue('timeout');

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('timeout');
  });
});
