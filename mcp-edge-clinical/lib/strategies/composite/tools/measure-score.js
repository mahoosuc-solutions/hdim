function createDefinition(clinicalClient) {
  return {
    name: 'measure_score',
    description: 'Get aggregated quality score for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'measure_score' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
