const { definition } = require('../../lib/tools/edge-health');

describe('edge_health tool', () => {
  it('has the correct name', () => {
    expect(definition.name).toBe('edge_health');
  });

  it('has an inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('handler returns healthy status with expected fields', async () => {
    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.status).toBe('healthy');
    expect(payload.service).toBe('hdim-platform-edge');
    expect(payload.version).toBe('0.1.0');
    expect(typeof payload.uptime).toBe('number');
    expect(payload.uptime).toBeGreaterThanOrEqual(0);
    expect(typeof payload.timestamp).toBe('string');
    expect(typeof payload.demoMode).toBe('boolean');
  });

  it('returns content in MCP format', async () => {
    const result = await definition.handler();
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
  });
});
