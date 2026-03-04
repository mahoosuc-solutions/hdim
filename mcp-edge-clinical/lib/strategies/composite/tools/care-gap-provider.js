function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_provider',
    description: 'Get prioritized care gaps for a provider panel',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_provider' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
