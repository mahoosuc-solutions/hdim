function createDefinition(clinicalClient) {
  return {
    name: 'cql_batch',
    description: 'Batch evaluate CQL library for multiple patients',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'cql_batch' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
