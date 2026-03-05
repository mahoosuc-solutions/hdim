function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_list',
    audit: { phi: true, write: false, patientIdArg: 'patientId' },
    description: 'List open or high-priority care gaps for a patient.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        status: {
          type: 'string',
          enum: ['open', 'high-priority', 'overdue', 'upcoming'],
          default: 'open',
          description: 'Care gap status filter'
        }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId, status } = args;
      try {
        const endpointMap = {
          'open': '/care-gap/open',
          'high-priority': '/care-gap/high-priority',
          'overdue': '/care-gap/overdue',
          'upcoming': '/care-gap/upcoming'
        };
        const endpoint = endpointMap[status] || '/care-gap/open';
        const path = `${endpoint}?patient=${encodeURIComponent(patientId)}`;
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
