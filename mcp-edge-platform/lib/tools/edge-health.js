const startTime = Date.now();

const definition = {
  name: 'edge_health',
  description: 'Local MCP edge health/status with downstream gateway probe',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const gatewayUrl = process.env.HDIM_BASE_URL || 'http://localhost:18080';
    let gatewayReachable = false;

    try {
      const res = await fetch(`${gatewayUrl}/actuator/health`, {
        signal: AbortSignal.timeout(3000)
      });
      gatewayReachable = res.ok;
    } catch {
      // gateway unreachable
    }

    const status = gatewayReachable ? 'healthy' : 'degraded';
    const payload = {
      status,
      service: 'hdim-platform-edge',
      version: '0.1.0',
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString(),
      demoMode: process.env.HDIM_DEMO_MODE === 'true',
      downstream: {
        gateway: { url: gatewayUrl, reachable: gatewayReachable }
      }
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
