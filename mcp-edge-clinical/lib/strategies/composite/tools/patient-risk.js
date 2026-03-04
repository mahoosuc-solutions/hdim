function createDefinition(clinicalClient) {
  return {
    name: 'patient_risk',
    description: 'Get patient risk assessment scores',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'patient_risk' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
