function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_population',
    description: 'Get population-level care gap report',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_population' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
