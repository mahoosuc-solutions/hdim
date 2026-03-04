function createDefinition(clinicalClient) {
  return {
    name: 'health_score',
    description: 'Get composite health score for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'health_score' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
