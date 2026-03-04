const startTime = Date.now();

const definition = {
  name: 'edge_health',
  description: 'Local MCP edge health/status for devops sidecar',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const payload = {
      status: 'healthy',
      service: 'hdim-devops-edge',
      version: '0.1.0',
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString()
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
