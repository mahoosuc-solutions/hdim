function createDefinition(clinicalClient) {
  return {
    name: 'measure_evaluate',
    description: 'Evaluate a quality measure for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'measure_evaluate' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
