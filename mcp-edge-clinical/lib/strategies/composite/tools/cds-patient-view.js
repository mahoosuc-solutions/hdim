function createDefinition(clinicalClient) {
  return {
    name: 'cds_patient_view',
    description: 'Trigger CDS Hooks patient-view to get clinical decision support cards for a patient chart opening.',
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
        const body = {
          hookInstance: `mcp-edge-${Date.now()}`,
          hook: 'patient-view',
          context: { userId: 'mcp-edge-user', patientId }
        };
        const res = await clinicalClient.post('/quality-measure/cds-services/patient-view', body, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
