const { VALID_RESOURCE_TYPES } = require('./fhir-read');

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_create',
    audit: { phi: true, write: true },
    description: 'Create a FHIR R4 resource. Provide the full resource object.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        resource: { type: 'object', description: 'FHIR resource JSON object' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['resourceType', 'resource', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, resource, tenantId } = args;
      try {
        const res = await clinicalClient.post(`/fhir/${resourceType}`, resource, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
