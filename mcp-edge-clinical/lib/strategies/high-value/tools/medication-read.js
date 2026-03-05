function createDefinition(clinicalClient) {
  return {
    name: 'medication_read',
    audit: { phi: true, write: false, patientIdArg: undefined },
    description: 'Read a FHIR MedicationRequest resource by ID. Returns prescriptions and medication orders.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'MedicationRequest resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/MedicationRequest/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'MedicationRequest', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'MedicationRequest', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
