function createDefinition(clinicalClient) {
  return {
    name: 'fhir_create',
    description: 'Create a FHIR R4 resource',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'fhir_create' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
