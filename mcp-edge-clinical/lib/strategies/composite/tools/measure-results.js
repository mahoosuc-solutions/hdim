function createDefinition(clinicalClient) {
  return {
    name: 'measure_results',
    description: 'Get quality measure results for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'measure_results' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
