const { createDefinition } = require('../../lib/tools/platform-health');

describe('platform_health tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { get: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('platform_health');
  });

  it('has an inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('returns gateway status on success', async () => {
    mockClient.get.mockResolvedValue({
      status: 200,
      ok: true,
      body: { status: 'UP' }
    });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(mockClient.get).toHaveBeenCalledWith('/actuator/health');
    expect(payload.gateway.status).toBe(200);
    expect(payload.gateway.ok).toBe(true);
    expect(payload.gateway.body).toEqual({ status: 'UP' });
  });

  it('returns error message on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('Connection refused'));

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.gateway.ok).toBe(false);
    expect(payload.gateway.error).toBe('Connection refused');
  });

  it('handles non-Error throws', async () => {
    mockClient.get.mockRejectedValue('unexpected string error');

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.gateway.ok).toBe(false);
    expect(payload.gateway.error).toBe('unexpected string error');
  });
});
