const { createDefinition } = require('../../lib/tools/docker-restart');

describe('docker_restart tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = {
      restart: jest.fn()
    };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('docker_restart');
  });

  it('has a description', () => {
    expect(typeof definition.description).toBe('string');
    expect(definition.description.length).toBeGreaterThan(0);
  });

  it('has a valid inputSchema requiring service', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {
        service: { type: 'string', description: expect.any(String) }
      },
      required: ['service'],
      additionalProperties: false
    });
  });

  it('calls dockerClient.restart() and returns success payload', async () => {
    mockClient.restart.mockResolvedValue({ ok: true, stdout: 'Restarting fhir-service ... done', stderr: '' });

    const result = await definition.handler({ service: 'fhir-service' });

    expect(mockClient.restart).toHaveBeenCalledWith('fhir-service');
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(true);
    expect(payload.service).toBe('fhir-service');
    expect(payload.stdout).toBe('Restarting fhir-service ... done');
    expect(payload.stderr).toBeNull();
  });

  it('returns error payload when service is missing/empty', async () => {
    const result = await definition.handler({ service: '' });

    expect(mockClient.restart).not.toHaveBeenCalled();
    const payload = JSON.parse(result.content[0].text);
    expect(payload.error).toBe('service is required');
  });

  it('returns error payload when dockerClient.restart() throws', async () => {
    mockClient.restart.mockRejectedValue(new Error('Container not found'));

    const result = await definition.handler({ service: 'bogus' });

    expect(result.content).toHaveLength(1);
    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Container not found');
  });

  describe('service name validation', () => {
    it('rejects service names with shell metacharacters', async () => {
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'postgres; rm -rf /' });
      const body = JSON.parse(result.content[0].text);
      expect(body.error).toMatch(/invalid service name/i);
    });

    it('rejects service names with spaces', async () => {
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'my service' });
      const body = JSON.parse(result.content[0].text);
      expect(body.error).toMatch(/invalid service name/i);
    });

    it('rejects empty string after trim', async () => {
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: '   ' });
      const body = JSON.parse(result.content[0].text);
      expect(body.error).toMatch(/required/i);
    });

    it('accepts valid service names with hyphens and underscores', async () => {
      mockClient.restart.mockResolvedValue({ ok: true, stdout: '', stderr: '' });
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'fhir-service_v2' });
      const body = JSON.parse(result.content[0].text);
      expect(body.ok).toBe(true);
    });

    it('accepts service names with dots', async () => {
      mockClient.restart.mockResolvedValue({ ok: true, stdout: '', stderr: '' });
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'service.v2' });
      const body = JSON.parse(result.content[0].text);
      expect(body.ok).toBe(true);
    });
  });
});
