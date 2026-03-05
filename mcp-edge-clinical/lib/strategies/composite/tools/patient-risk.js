function createDefinition(clinicalClient) {
  return {
    name: 'patient_risk',
    audit: { phi: true, write: false, patientIdArg: 'patientId' },
    description: 'Get patient risk assessment and stratification scores.',
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
          `/patient/risk-assessment?patient=${encodeURIComponent(patientId)}`,
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
