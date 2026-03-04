function createDefinition(clinicalClient) {
  return {
    name: 'cql_libraries',
    description: 'List available CQL/HEDIS measure libraries',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'cql_libraries' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
