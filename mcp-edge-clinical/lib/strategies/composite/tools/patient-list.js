function createDefinition(clinicalClient) {
  return {
    name: 'patient_list',
    description: 'List patients for a tenant',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'patient_list' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
