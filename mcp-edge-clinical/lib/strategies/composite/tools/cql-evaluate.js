function createDefinition(clinicalClient) {
  return {
    name: 'cql_evaluate',
    description: 'Evaluate a CQL library for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'cql_evaluate' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
