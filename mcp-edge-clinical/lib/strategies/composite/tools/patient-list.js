function createDefinition(clinicalClient) {
  return {
    name: 'patient_list',
    audit: { phi: true, write: false },
    description: 'List patients with pagination. No patient-specific PHI in request.',
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' },
        page: { type: 'number', description: 'Page number (0-based, default 0)' },
        size: { type: 'number', description: 'Page size (default 20)' }
      },
      required: ['tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { tenantId, page, size } = args;
      try {
        const res = await clinicalClient.get(
          `/api/v1/patients?page=${page || 0}&size=${size || 20}`,
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
