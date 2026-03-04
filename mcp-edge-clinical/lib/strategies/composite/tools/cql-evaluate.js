function createDefinition(clinicalClient) {
  return {
    name: 'cql_evaluate',
    description: 'Evaluate a CQL/HEDIS measure library for a patient. Returns numerator/denominator compliance.',
    inputSchema: {
      type: 'object',
      properties: {
        library: { type: 'string', description: 'CQL library identifier (e.g. "HbA1c-Control")' },
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['library', 'patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { library, patientId, tenantId } = args;
      try {
        const path = `/cql/evaluate?library=${encodeURIComponent(library)}&patient=${encodeURIComponent(patientId)}`;
        const res = await clinicalClient.post(path, {}, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
