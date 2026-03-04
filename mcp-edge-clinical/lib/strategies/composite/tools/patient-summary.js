function createDefinition(clinicalClient) {
  return {
    name: 'patient_summary',
    description: 'Get comprehensive patient health record summary (FHIR Bundle with all clinical data).',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId } = args;
      try {
        const res = await clinicalClient.get(
          `/patient/health-record?patient=${encodeURIComponent(patientId)}`,
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
