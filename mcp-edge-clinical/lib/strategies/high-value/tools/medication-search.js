function createDefinition(clinicalClient) {
  return {
    name: 'medication_search',
    description: 'Search FHIR MedicationRequest resources. Filter by patient, status, intent, or medication code.',
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' },
        patient: { type: 'string', description: 'Patient ID for scoped search (optional)' },
        params: { type: 'object', description: 'FHIR search parameters (e.g., status, intent, code, _count)', additionalProperties: { type: 'string' } }
      },
      required: ['tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false, patientIdArg: 'patient' },
    handler: async (args) => {
      const { tenantId, patient, params } = args;
      const qp = new URLSearchParams();
      if (patient) qp.set('patient', patient);
      if (params) Object.entries(params).forEach(([k, v]) => qp.set(k, v));
      const qs = qp.toString();
      const path = `/fhir/MedicationRequest${qs ? '?' + qs : ''}`;
      try {
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'MedicationRequest', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'MedicationRequest', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
