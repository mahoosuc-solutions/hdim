function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_stats',
    audit: { phi: false, write: false, patientIdArg: undefined },
    description: 'Get aggregate care gap statistics (non-PHI). Closure rates, distribution by measure.',
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { tenantId } = args;
      try {
        const res = await clinicalClient.get('/care-gap/stats', { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
