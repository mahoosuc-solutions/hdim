function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_identify',
    description: 'Identify care gaps by running CQL measures',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_identify' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
