function createDefinition(clinicalClient) {
  return {
    name: 'cql_batch',
    description: 'Batch-evaluate a CQL library across multiple patients.',
    inputSchema: {
      type: 'object',
      properties: {
        library: { type: 'string', description: 'CQL library identifier' },
        patientIds: {
          type: 'array',
          items: { type: 'string' },
          description: 'Array of patient UUIDs'
        },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['library', 'patientIds', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { library, patientIds, tenantId } = args;
      try {
        const path = '/cql/api/v1/cql/evaluations/batch';
        const body = { libraryId: library, patientIds };
        const res = await clinicalClient.post(path, body, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
