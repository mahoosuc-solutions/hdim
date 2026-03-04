const { createDefinition } = require('../../lib/tools/demo-seed');

describe('demo_seed tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('demo_seed');
  });

  it('has an inputSchema with required scenarioName', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {
        scenarioName: { type: 'string', description: expect.any(String) }
      },
      required: ['scenarioName'],
      additionalProperties: false
    });
  });

  it('returns error when scenarioName is missing', async () => {
    const result = await definition.handler({});
    const payload = JSON.parse(result.content[0].text);

    expect(payload.error).toBe('scenarioName is required');
    expect(mockClient.post).not.toHaveBeenCalled();
  });

  it('posts scenario and returns result on success', async () => {
    mockClient.post.mockResolvedValue({
      status: 201,
      ok: true,
      body: { scenario: 'hedis-evaluation', patientsCreated: 10 }
    });

    const result = await definition.handler({ scenarioName: 'hedis-evaluation' });
    const payload = JSON.parse(result.content[0].text);

    expect(mockClient.post).toHaveBeenCalledWith('/api/v1/demo/scenarios/hedis-evaluation', {});
    expect(payload.status).toBe(201);
    expect(payload.ok).toBe(true);
    expect(payload.data).toEqual({ scenario: 'hedis-evaluation', patientsCreated: 10 });
  });

  it('encodes scenarioName in URL', async () => {
    mockClient.post.mockResolvedValue({ status: 201, ok: true, body: {} });

    await definition.handler({ scenarioName: 'risk/stratification' });

    expect(mockClient.post).toHaveBeenCalledWith('/api/v1/demo/scenarios/risk%2Fstratification', {});
  });

  it('returns error message on failure', async () => {
    mockClient.post.mockRejectedValue(new Error('Internal server error'));

    const result = await definition.handler({ scenarioName: 'hedis-evaluation' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Internal server error');
  });

  it('handles non-Error throws', async () => {
    mockClient.post.mockRejectedValue('network failure');

    const result = await definition.handler({ scenarioName: 'hedis-evaluation' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('network failure');
  });
});
