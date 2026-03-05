function createDefinition(clinicalClient) {
  return {
    name: 'condition_search',
    description: 'Search FHIR Condition resources. Filter by patient, clinical-status, category, or code.',
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' },
        patient: { type: 'string', description: 'Patient ID for scoped search (optional)' },
        params: { type: 'object', description: 'FHIR search parameters (e.g., clinical-status, category, code, _count)', additionalProperties: { type: 'string' } }
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
      const path = `/fhir/Condition${qs ? '?' + qs : ''}`;
      try {
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: 'Condition', data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: 'Condition', error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
