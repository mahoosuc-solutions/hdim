const { createDefinition, SERVICE_CATALOG } = require('../../lib/tools/service-catalog');

describe('service_catalog tool', () => {
  let mockClient;
  let definition;

  beforeEach(() => {
    mockClient = { get: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has the correct name', () => {
    expect(definition.name).toBe('service_catalog');
  });

  it('has an inputSchema with optional category', () => {
    expect(definition.inputSchema.properties.category).toBeDefined();
    expect(definition.inputSchema.properties.category.type).toBe('string');
  });

  it('returns all 11 services when no category filter', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: { status: 'UP' } });

    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.services).toHaveLength(11);
    expect(payload.checkedAt).toBeDefined();
  });

  it('filters services by category', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: { status: 'UP' } });

    const result = await definition.handler({ category: 'quality' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.services).toHaveLength(3);
    payload.services.forEach((svc) => {
      expect(svc.category).toBe('quality');
    });
  });

  it('returns unknown status for services without healthPath', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: {} });

    const result = await definition.handler({ category: 'data' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.services).toHaveLength(2);
    payload.services.forEach((svc) => {
      expect(svc.status).toBe('unknown');
      expect(svc.detail).toBe('no health endpoint');
    });
    // get should not be called for data services
    expect(mockClient.get).not.toHaveBeenCalled();
  });

  it('marks healthy services with status healthy', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: { status: 'UP' } });

    const result = await definition.handler({ category: 'gateway' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.services[0].status).toBe('healthy');
    expect(payload.services[0].httpStatus).toBe(200);
  });

  it('marks unreachable services on error', async () => {
    mockClient.get.mockRejectedValue(new Error('ECONNREFUSED'));

    const result = await definition.handler({ category: 'gateway' });
    const payload = JSON.parse(result.content[0].text);

    expect(payload.services[0].status).toBe('unreachable');
    expect(payload.services[0].error).toBe('ECONNREFUSED');
  });

  it('exports SERVICE_CATALOG with 11 entries', () => {
    expect(SERVICE_CATALOG).toHaveLength(11);
  });
});
