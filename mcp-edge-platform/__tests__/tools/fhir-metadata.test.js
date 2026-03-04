const { createDefinition } = require('../../lib/tools/fhir-metadata');

describe('fhir_metadata tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { get: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('fhir_metadata');
  });

  it('has an inputSchema with no properties', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('returns status, ok, and metadata on success', async () => {
    mockClient.get.mockResolvedValue({
      status: 200,
      ok: true,
      body: { resourceType: 'CapabilityStatement', fhirVersion: '4.0.1' }
    });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(mockClient.get).toHaveBeenCalledWith('/fhir/metadata');
    expect(payload.status).toBe(200);
    expect(payload.ok).toBe(true);
    expect(payload.metadata).toEqual({
      resourceType: 'CapabilityStatement',
      fhirVersion: '4.0.1'
    });
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
