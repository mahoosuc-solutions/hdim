function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_identify',
    audit: { phi: true, write: false, patientIdArg: 'patientId' },
    description: 'Identify care gaps for a patient by running measure evaluation.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        library: { type: 'string', description: 'Specific measure library to evaluate' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId, library } = args;
      try {
        const body = { patientId, tenantId };
        if (library) {
          body.library = library;
        }
        const res = await clinicalClient.post('/care-gap/identify', body, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
