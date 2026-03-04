// mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js
// Metadata for all 20 FHIR R4 resource types supported by HDIM.
// Each entry controls which tool variants (read/search/create) the factory generates.

const RESOURCE_REGISTRY = [
  { type: 'Patient', searchable: true, creatable: true },
  { type: 'Observation', searchable: true, creatable: true },
  { type: 'Encounter', searchable: true, creatable: true },
  { type: 'Condition', searchable: true, creatable: true },
  { type: 'MedicationRequest', searchable: true, creatable: true },
  { type: 'MedicationAdministration', searchable: true, creatable: false },
  { type: 'Immunization', searchable: true, creatable: true },
  { type: 'AllergyIntolerance', searchable: true, creatable: true },
  { type: 'Procedure', searchable: true, creatable: true },
  { type: 'DiagnosticReport', searchable: true, creatable: false },
  { type: 'DocumentReference', searchable: true, creatable: true },
  { type: 'CarePlan', searchable: true, creatable: true },
  { type: 'Goal', searchable: true, creatable: true },
  { type: 'Coverage', searchable: true, creatable: false },
  { type: 'Appointment', searchable: true, creatable: true },
  { type: 'Task', searchable: true, creatable: true },
  { type: 'Bundle', searchable: false, creatable: false },
  { type: 'Organization', searchable: true, creatable: false },
  { type: 'Practitioner', searchable: true, creatable: false },
  { type: 'PractitionerRole', searchable: true, creatable: false },
];

module.exports = { RESOURCE_REGISTRY };
