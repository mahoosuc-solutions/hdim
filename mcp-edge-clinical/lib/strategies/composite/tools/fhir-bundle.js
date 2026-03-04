function createDefinition(clinicalClient) {
  return {
    name: 'fhir_bundle',
    description: 'Submit a FHIR Bundle (transaction or batch)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'fhir_bundle' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
