function createDefinition(clinicalClient) {
  return {
    name: 'patient_timeline',
    description: 'Get patient clinical timeline by date range.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        startDate: { type: 'string', description: 'Start date (ISO 8601, e.g. 2025-01-01)' },
        endDate: { type: 'string', description: 'End date (ISO 8601, e.g. 2025-12-31)' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId, startDate, endDate } = args;
      try {
        let path = `/patient/timeline/by-date?patient=${encodeURIComponent(patientId)}`;
        if (startDate) path += `&startDate=${startDate}`;
        if (endDate) path += `&endDate=${endDate}`;
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
