function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_stats',
    description: 'Get aggregate care gap statistics',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_stats' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
