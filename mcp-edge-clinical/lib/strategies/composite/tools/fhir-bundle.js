function createDefinition(clinicalClient) {
  return {
    name: 'fhir_bundle',
    audit: { phi: true, write: true },
    description: 'Submit a FHIR Bundle (transaction or batch). Process multiple resources in one call.',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', enum: ['transaction', 'batch'], description: 'Bundle type' },
        entries: { type: 'array', items: { type: 'object' }, description: 'Array of Bundle entry objects' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['type', 'entries', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { type, entries, tenantId } = args;
      const bundle = { resourceType: 'Bundle', type, entry: entries };
      try {
        const res = await clinicalClient.post('/fhir/Bundle', bundle, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
