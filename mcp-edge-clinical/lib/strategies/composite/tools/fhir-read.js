function createDefinition(clinicalClient) {
  return {
    name: 'fhir_read',
    description: 'Read a FHIR R4 resource by type and ID',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'fhir_read' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
