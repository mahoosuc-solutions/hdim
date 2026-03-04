function createDefinition(platformClient) {
  return {
    name: 'demo_status',
    description: 'Demo seeding system status and available scenarios',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const res = await platformClient.get('/api/v1/demo/status');
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
