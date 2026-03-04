function createDefinition(clinicalClient) {
  return {
    name: 'patient_timeline',
    description: 'Get patient event timeline',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'patient_timeline' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
