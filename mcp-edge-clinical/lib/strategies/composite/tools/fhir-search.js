const { VALID_RESOURCE_TYPES } = require('./fhir-read');

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_search',
    audit: { phi: true, write: false, patientIdArg: 'patient' },
    description: 'Search FHIR R4 resources by type with query parameters. Use patient param for patient-scoped searches.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        patient: { type: 'string', description: 'Patient ID for scoped search (optional)' },
        params: { type: 'object', description: 'Additional FHIR search params (e.g., category, _count, _offset)', additionalProperties: { type: 'string' } }
      },
      required: ['resourceType', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, tenantId, patient, params } = args;
      const qp = new URLSearchParams();
      if (patient) qp.set('patient', patient);
      if (params) Object.entries(params).forEach(([k, v]) => qp.set(k, v));
      const qs = qp.toString();
      const path = `/fhir/${resourceType}${qs ? '?' + qs : ''}`;
      try {
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
