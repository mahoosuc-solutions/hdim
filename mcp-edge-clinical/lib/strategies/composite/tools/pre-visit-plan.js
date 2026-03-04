function createDefinition(clinicalClient) {
  return {
    name: 'pre_visit_plan',
    description: 'Generate pre-visit summary for a patient-provider encounter.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        providerId: { type: 'string', description: 'Provider UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['patientId', 'providerId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, providerId, tenantId } = args;
      try {
        const res = await clinicalClient.get(
          `/api/v1/providers/${encodeURIComponent(providerId)}/patients/${encodeURIComponent(patientId)}/pre-visit-summary`,
          { tenantId }
        );
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
