function createDefinition(clinicalClient) {
  return {
    name: 'measure_evaluate',
    description: 'Evaluate quality measures for a patient. Runs HEDIS/CQL calculation.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        measureId: { type: 'string', description: 'Specific measure ID (optional, evaluates all if omitted)' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId, measureId } = args;
      try {
        const body = { patientId };
        if (measureId) {
          body.measureId = measureId;
        }
        const res = await clinicalClient.post('/quality-measure/calculate', body, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
