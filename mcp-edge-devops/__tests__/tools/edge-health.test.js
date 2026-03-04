const { definition } = require('../../lib/tools/edge-health');

describe('edge_health tool', () => {
  it('has the correct name', () => {
    expect(definition.name).toBe('edge_health');
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

  it('handler returns expected health payload fields', async () => {
    const result = await definition.handler();

    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');

    const payload = JSON.parse(result.content[0].text);
    expect(payload.status).toBe('healthy');
    expect(payload.service).toBe('hdim-devops-edge');
    expect(payload.version).toBe('0.1.0');
    expect(typeof payload.uptime).toBe('number');
    expect(payload.uptime).toBeGreaterThanOrEqual(0);
    expect(typeof payload.timestamp).toBe('string');
    // Verify timestamp is a valid ISO string
    expect(new Date(payload.timestamp).toISOString()).toBe(payload.timestamp);
  });
});
