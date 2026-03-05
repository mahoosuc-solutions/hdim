function createDefinition(clinicalClient) {
  return {
    name: 'cql_libraries',
    audit: { phi: false, write: false, patientIdArg: undefined },
    description: 'List available CQL measure libraries (non-PHI reference data).',
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' },
        status: { type: 'string', description: 'Optional status filter (e.g. "active", "draft")' }
      },
      required: ['tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { tenantId, status } = args;
      try {
        let path = '/cql/api/v1/cql/libraries';
        if (status) {
          path += `?status=${encodeURIComponent(status)}`;
        }
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
