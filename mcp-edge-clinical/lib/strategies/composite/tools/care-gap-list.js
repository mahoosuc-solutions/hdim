function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_list',
    description: 'List care gaps for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_list' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
