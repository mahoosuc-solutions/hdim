const DEFAULT_GATEWAY_URL = 'http://localhost:18080';

const definition = {
  name: 'platform_info',
  description: 'HDIM platform MCP edge info: base URLs, version, configuration',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const payload = {
      name: 'hdim-platform-edge',
      version: '0.1.0',
      defaultGatewayUrl: DEFAULT_GATEWAY_URL,
      envGatewayUrl: process.env.HDIM_BASE_URL || null,
      demoMode: process.env.HDIM_DEMO_MODE === 'true',
      protocol: '2025-11-25'
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
