function createDefinition(clinicalClient) {
  return {
    name: 'measure_population',
    description: 'Get population quality measure report',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'measure_population' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
