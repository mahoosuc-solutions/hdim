function createDefinition(clinicalClient) {
  return {
    name: 'patient_summary',
    description: 'Get comprehensive patient health record summary',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'patient_summary' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
