const VALID_RESOURCE_TYPES = [
  'Patient', 'Observation', 'Encounter', 'Condition', 'MedicationRequest',
  'MedicationAdministration', 'Immunization', 'AllergyIntolerance', 'Procedure',
  'DiagnosticReport', 'DocumentReference', 'CarePlan', 'Goal', 'Coverage',
  'Appointment', 'Task', 'Bundle', 'Organization', 'Practitioner', 'PractitionerRole'
];

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_read',
    description: 'Read a FHIR R4 resource by type and ID. Supports all 20 HDIM resource types.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        id: { type: 'string', description: 'Resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['resourceType', 'id', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/${resourceType}/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition, VALID_RESOURCE_TYPES };
