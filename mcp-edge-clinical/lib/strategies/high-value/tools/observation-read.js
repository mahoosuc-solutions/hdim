function createDefinition(clinicalClient) {
  return {
    name: 'observation_read',
    description: 'Read a FHIR Observation resource by ID. Returns lab results, vitals, and clinical measurements.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Observation resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false },
    handler: async (args) => {
      const { id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/Observation/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'Observation', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'Observation', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
