function createDefinition(platformClient) {
  return {
    name: 'platform_health',
    description: 'Gateway actuator health check (/actuator/health)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      let gateway;
      try {
        const res = await platformClient.get('/actuator/health');
        gateway = { status: res.status, ok: res.ok, body: res.body };
      } catch (err) {
        gateway = { ok: false, error: err?.message || String(err) };
      }
      return { content: [{ type: 'text', text: JSON.stringify({ gateway }, null, 2) }] };
    }
  };
}

module.exports = { createDefinition };
