function createDefinition(platformClient) {
  return {
    name: 'demo_seed',
    description: 'Trigger demo scenario loading (creates synthetic patient data)',
    inputSchema: {
      type: 'object',
      properties: {
        scenarioName: { type: 'string', description: 'Scenario to load (e.g. hedis-evaluation, risk-stratification)' }
      },
      required: ['scenarioName'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { scenarioName } = args;
      if (!scenarioName) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'scenarioName is required' }) }] };
      }
      try {
        const res = await platformClient.post(`/api/v1/demo/scenarios/${encodeURIComponent(scenarioName)}`, {});
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
