function createDefinition(platformClient) {
  return {
    name: 'dashboard_stats',
    description: 'Executive KPIs: aggregate dashboard stats (no individual PHI)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const res = await platformClient.get('/analytics/dashboards');
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
