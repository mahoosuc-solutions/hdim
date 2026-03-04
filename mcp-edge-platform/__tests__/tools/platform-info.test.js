const { definition } = require('../../lib/tools/platform-info');

describe('platform_info tool', () => {
  it('has the correct name', () => {
    expect(definition.name).toBe('platform_info');
  });

  it('has an inputSchema', () => {
    expect(definition.inputSchema).toEqual({
      type: 'object',
      properties: {},
      additionalProperties: false
    });
  });

  it('handler returns expected fields', async () => {
    const result = await definition.handler();
    const payload = JSON.parse(result.content[0].text);

    expect(payload.name).toBe('hdim-platform-edge');
    expect(payload.version).toBe('0.1.0');
    expect(payload.defaultGatewayUrl).toBe('http://localhost:18080');
    expect(payload).toHaveProperty('envGatewayUrl');
    expect(typeof payload.demoMode).toBe('boolean');
    expect(payload.protocol).toBe('2025-11-25');
  });

  it('returns content in MCP format', async () => {
    const result = await definition.handler();
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
  });
});
