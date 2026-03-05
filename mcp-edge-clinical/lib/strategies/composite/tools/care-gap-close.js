function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_close',
    audit: { phi: true, write: true, patientIdArg: undefined },
    description: 'Close a care gap with clinical evidence. This is a WRITE operation.',
    inputSchema: {
      type: 'object',
      properties: {
        gapId: { type: 'string', description: 'Care gap UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        closedBy: { type: 'string', description: 'User or provider who closed the gap' },
        reason: { type: 'string', description: 'Clinical reason for closure' }
      },
      required: ['gapId', 'tenantId', 'closedBy', 'reason'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { gapId, tenantId, closedBy, reason } = args;
      try {
        const body = { careGapId: gapId, closedBy, closureReason: reason, tenantId };
        const res = await clinicalClient.post('/care-gap/close', body, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
