const { createDefinition } = require('../../lib/tools/docker-logs');

describe('docker_logs tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = {
      logs: jest.fn()
    };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('docker_logs');
  });

  it('has a description', () => {
    expect(typeof definition.description).toBe('string');
    expect(definition.description.length).toBeGreaterThan(0);
  });

  it('requires service in inputSchema', () => {
    expect(definition.inputSchema.required).toContain('service');
    expect(definition.inputSchema.properties.service.type).toBe('string');
  });

  it('has optional tail parameter', () => {
    expect(definition.inputSchema.properties.tail.type).toBe('number');
    expect(definition.inputSchema.required).not.toContain('tail');
  });

  it('calls dockerClient.logs(service, tail) and returns log output', async () => {
    mockClient.logs.mockResolvedValue({
      ok: true,
      stdout: 'line1\nline2\nline3',
      stderr: ''
    });

    const result = await definition.handler({ service: 'postgres', tail: 50 });

    expect(mockClient.logs).toHaveBeenCalledWith('postgres', 50);
    expect(result.content).toHaveLength(1);

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(true);
    expect(payload.service).toBe('postgres');
    expect(payload.logs).toBe('line1\nline2\nline3');
    expect(payload.stderr).toBeNull();
  });

  it('defaults tail to 100 when not provided', async () => {
    mockClient.logs.mockResolvedValue({ ok: true, stdout: 'log output', stderr: '' });

    await definition.handler({ service: 'fhir-service' });

    expect(mockClient.logs).toHaveBeenCalledWith('fhir-service', 100);
  });

  it('returns error when service is missing', async () => {
    const result = await definition.handler({ service: '' });

    expect(mockClient.logs).not.toHaveBeenCalled();
    const payload = JSON.parse(result.content[0].text);
    expect(payload.error).toBe('service is required');
  });

  it('returns error payload when dockerClient.logs() throws', async () => {
    mockClient.logs.mockRejectedValue(new Error('Container not found'));

    const result = await definition.handler({ service: 'nonexistent' });

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Container not found');
  });

  describe('service name validation', () => {
    it('rejects service names with metacharacters', async () => {
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'pg && echo pwned' });
      const body = JSON.parse(result.content[0].text);
      expect(body.error).toMatch(/invalid service name/i);
    });
  });

  describe('tail parameter validation', () => {
    it('rejects negative tail values', async () => {
      const tool = createDefinition(mockClient);
      const result = await tool.handler({ service: 'postgres', tail: -5 });
      const body = JSON.parse(result.content[0].text);
      expect(body.error).toMatch(/tail must be/i);
    });

    it('caps tail at 10000', async () => {
      mockClient.logs.mockResolvedValue({ ok: true, stdout: 'logs', stderr: '' });
      const tool = createDefinition(mockClient);
      await tool.handler({ service: 'postgres', tail: 50000 });
      expect(mockClient.logs).toHaveBeenCalledWith('postgres', 10000);
    });

    it('defaults tail to 100 when not provided', async () => {
      mockClient.logs.mockResolvedValue({ ok: true, stdout: 'logs', stderr: '' });
      const tool = createDefinition(mockClient);
      await tool.handler({ service: 'postgres' });
      expect(mockClient.logs).toHaveBeenCalledWith('postgres', 100);
    });
  });
});
