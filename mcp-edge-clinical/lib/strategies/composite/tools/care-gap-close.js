function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_close',
    description: 'Close a care gap with reason and closure action',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'care_gap_close' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
