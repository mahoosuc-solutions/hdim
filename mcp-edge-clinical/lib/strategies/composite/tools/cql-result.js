function createDefinition(clinicalClient) {
  return {
    name: 'cql_result',
    audit: { phi: true, write: false, patientIdArg: 'patientId' },
    description: 'Get the latest CQL evaluation result for a specific patient and library.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        library: { type: 'string', description: 'CQL library identifier' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['patientId', 'library', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, library, tenantId } = args;
      try {
        const path = `/cql/api/v1/cql/evaluations/patient/${encodeURIComponent(patientId)}/library/${encodeURIComponent(library)}/latest`;
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
