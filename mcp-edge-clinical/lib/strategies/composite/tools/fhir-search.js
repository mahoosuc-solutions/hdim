function createDefinition(clinicalClient) {
  return {
    name: 'fhir_search',
    description: 'Search FHIR R4 resources by type with query parameters',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'fhir_search' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
