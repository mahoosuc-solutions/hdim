function createDefinition(clinicalClient) {
  return {
    name: 'encounter_read',
    description: 'Read a FHIR Encounter resource by ID. Returns visit details, participants, and diagnoses.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Encounter resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false },
    handler: async (args) => {
      const { id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/Encounter/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'Encounter', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'Encounter', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
