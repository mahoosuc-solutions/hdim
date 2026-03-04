function createDefinition(clinicalClient) {
  return {
    name: 'cql_result',
    description: 'Get latest CQL evaluation result for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'cql_result' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
