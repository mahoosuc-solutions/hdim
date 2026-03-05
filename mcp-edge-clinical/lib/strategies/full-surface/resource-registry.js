// mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js
// Metadata for all 20 FHIR R4 resource types supported by HDIM.
// Each entry controls which tool variants (read/search/create) the factory generates.

const RESOURCE_REGISTRY = [
  { type: 'Patient', searchable: true, creatable: true, phi: true },
  { type: 'Observation', searchable: true, creatable: true, phi: true },
  { type: 'Encounter', searchable: true, creatable: true, phi: true },
  { type: 'Condition', searchable: true, creatable: true, phi: true },
  { type: 'MedicationRequest', searchable: true, creatable: true, phi: true },
  { type: 'MedicationAdministration', searchable: true, creatable: false, phi: true },
  { type: 'Immunization', searchable: true, creatable: true, phi: true },
  { type: 'AllergyIntolerance', searchable: true, creatable: true, phi: true },
  { type: 'Procedure', searchable: true, creatable: true, phi: true },
  { type: 'DiagnosticReport', searchable: true, creatable: false, phi: true },
  { type: 'DocumentReference', searchable: true, creatable: true, phi: true },
  { type: 'CarePlan', searchable: true, creatable: true, phi: true },
  { type: 'Goal', searchable: true, creatable: true, phi: true },
  { type: 'Coverage', searchable: true, creatable: false, phi: true },
  { type: 'Appointment', searchable: true, creatable: true, phi: true },
  { type: 'Task', searchable: true, creatable: true, phi: true },
  { type: 'Bundle', searchable: false, creatable: false, phi: true },
  { type: 'Organization', searchable: true, creatable: false, phi: false },
  { type: 'Practitioner', searchable: true, creatable: false, phi: false },
  { type: 'PractitionerRole', searchable: true, creatable: false, phi: false },
];

module.exports = { RESOURCE_REGISTRY };
