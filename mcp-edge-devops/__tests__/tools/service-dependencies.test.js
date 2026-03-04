const { definition, SERVICE_DEPENDENCIES } = require('../../lib/tools/service-dependencies');

describe('service_dependencies tool', () => {
  it('has the correct name', () => {
    expect(definition.name).toBe('service_dependencies');
  });

  it('has a description', () => {
    expect(typeof definition.description).toBe('string');
    expect(definition.description.length).toBeGreaterThan(0);
  });

  it('has a valid inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {
        service: { type: 'string', description: expect.any(String) }
      },
      additionalProperties: false
    });
  });

  it('returns full dependency graph when no service specified', async () => {
    const result = await definition.handler({});

    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.dependencies).toEqual(SERVICE_DEPENDENCIES);
    expect(Object.keys(payload.dependencies)).toContain('gateway-edge');
    expect(Object.keys(payload.dependencies)).toContain('fhir-service');
  });

  it('returns full dependency graph when args is undefined', async () => {
    const result = await definition.handler();

    const payload = JSON.parse(result.content[0].text);
    expect(payload.dependencies).toEqual(SERVICE_DEPENDENCIES);
  });

  it('returns dependencies for a specific known service', async () => {
    const result = await definition.handler({ service: 'gateway-edge' });

    const payload = JSON.parse(result.content[0].text);
    expect(payload.service).toBe('gateway-edge');
    expect(payload.dependencies).toEqual([
      'gateway-admin-service',
      'gateway-clinical-service',
      'gateway-fhir-service'
    ]);
  });

  it('returns error with known services list for unknown service', async () => {
    const result = await definition.handler({ service: 'nonexistent-service' });

    const payload = JSON.parse(result.content[0].text);
    expect(payload.error).toContain('Unknown service: nonexistent-service');
    expect(payload.knownServices).toEqual(Object.keys(SERVICE_DEPENDENCIES));
    expect(payload.knownServices.length).toBeGreaterThan(0);
  });
});
