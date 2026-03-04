jest.mock('../../lib/docker-client', () => ({
  ...jest.requireActual('../../lib/docker-client'),
  runCommand: jest.fn()
}));

const { runCommand } = require('../../lib/docker-client');
const { createDefinition } = require('../../lib/tools/build-status');

describe('build_status tool', () => {
  let definition;

  beforeEach(() => {
    jest.clearAllMocks();
    definition = createDefinition();
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('build_status');
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

  it('returns affected projects on success', async () => {
    runCommand.mockResolvedValue({
      ok: true,
      stdout: 'api-gateway\nfhir-service\npatient-ui\n',
      stderr: ''
    });

    const result = await definition.handler();

    expect(runCommand).toHaveBeenCalledWith(
      'npx',
      ['nx', 'show', 'projects', '--affected'],
      expect.objectContaining({ timeoutMs: 30_000 })
    );

    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(true);
    expect(payload.affectedProjects).toEqual(['api-gateway', 'fhir-service', 'patient-ui']);
    expect(payload.count).toBe(3);
    expect(payload.stderr).toBeNull();
  });

  it('returns empty projects array when result is not ok', async () => {
    runCommand.mockResolvedValue({
      ok: false,
      stdout: '',
      stderr: 'nx not found'
    });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.ok).toBe(false);
    expect(payload.affectedProjects).toEqual([]);
    expect(payload.count).toBe(0);
    expect(payload.stderr).toBe('nx not found');
  });

  it('returns error payload when runCommand throws', async () => {
    runCommand.mockRejectedValue(new Error('Command failed'));

    const result = await definition.handler();

    expect(result.content).toHaveLength(1);
    const payload = JSON.parse(result.content[0].text);
    expect(payload.ok).toBe(false);
    expect(payload.error).toBe('Command failed');
  });
});
