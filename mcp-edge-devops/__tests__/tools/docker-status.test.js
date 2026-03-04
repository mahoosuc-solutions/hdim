const { createDefinition } = require('../../lib/tools/docker-status');

describe('docker_status tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = {
      composeFile: 'docker-compose.demo.yml',
      ps: jest.fn()
    };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('docker_status');
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

  it('calls dockerClient.ps() and returns services array', async () => {
    const services = [
      { Name: 'postgres', State: 'running' },
      { Name: 'fhir-service', State: 'running' }
    ];
    mockClient.ps.mockResolvedValue({ ok: true, services });

    const result = await definition.handler();

    expect(mockClient.ps).toHaveBeenCalledTimes(1);
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(true);
    expect(payload.services).toEqual(services);
    expect(payload.composeFile).toBe('docker-compose.demo.yml');
  });

  it('returns error payload when dockerClient.ps() throws', async () => {
    mockClient.ps.mockRejectedValue(new Error('Docker not running'));

    const result = await definition.handler();

    expect(result.content).toHaveLength(1);
    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Docker not running');
  });
});
