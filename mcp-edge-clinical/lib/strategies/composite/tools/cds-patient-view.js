function createDefinition(clinicalClient) {
  return {
    name: 'cds_patient_view',
    description: 'Get CDS Hooks clinical decision support cards',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async (args) => {
      return { content: [{ type: 'text', text: JSON.stringify({ stub: true, tool: 'cds_patient_view' }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition };
