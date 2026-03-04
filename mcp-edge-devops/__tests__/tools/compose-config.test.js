const { createDefinition } = require('../../lib/tools/compose-config');

describe('compose_config tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = {
      composeFile: 'docker-compose.demo.yml',
      config: jest.fn()
    };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('compose_config');
  });

  it('has a description', () => {
    expect(typeof definition.description).toBe('string');
    expect(definition.description.length).toBeGreaterThan(0);
  });

  it('has a valid inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('calls dockerClient.config() and returns config on success', async () => {
    const stdout = 'services:\n  postgres:\n    image: postgres:15';
    mockClient.config.mockResolvedValue({ ok: true, stdout, stderr: '' });

    const result = await definition.handler();

    expect(mockClient.config).toHaveBeenCalledTimes(1);
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(true);
    expect(payload.composeFile).toBe('docker-compose.demo.yml');
    expect(payload.config).toBe(stdout);
    expect(payload.stderr).toBeNull();
  });

  it('returns null config when result is not ok', async () => {
    mockClient.config.mockResolvedValue({ ok: false, stdout: '', stderr: 'invalid compose file' });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.config).toBeNull();
    expect(payload.stderr).toBe('invalid compose file');
  });

  it('returns error payload when dockerClient.config() throws', async () => {
    mockClient.config.mockRejectedValue(new Error('Docker not running'));

    const result = await definition.handler();

    expect(result.content).toHaveLength(1);
    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Docker not running');
  });
});
