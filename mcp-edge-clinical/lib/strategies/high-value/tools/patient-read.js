function createDefinition(clinicalClient) {
  return {
    name: 'patient_read',
    description: 'Read a FHIR Patient resource by ID. Returns demographics, identifiers, and contact information.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Patient resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/Patient/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'Patient', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'Patient', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
