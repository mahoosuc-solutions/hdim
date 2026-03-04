function createDefinition(clinicalClient) {
  return {
    name: 'pre_visit_plan',
    description: 'Get pre-visit planning summary for a patient',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'pre_visit_plan' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
