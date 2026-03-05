function createDefinition(clinicalClient) {
  return {
    name: 'condition_read',
    description: 'Read a FHIR Condition resource by ID. Returns diagnoses, problems, and health concerns.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Condition resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false },
    handler: async (args) => {
      const { id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/Condition/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'Condition', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'Condition', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
