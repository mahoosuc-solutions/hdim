// mcp-edge-clinical/lib/strategies/composite/role-policies.js
// Clinical RBAC role policies for the composite strategy (25 tools, 7 roles).
// Each role maps to an array of regex matchers tested against the tool name.

function clinicalRolePolicies() {
  return {
    clinical_admin: [/./],
    platform_admin: [/./],
    developer: [/./],
    clinician: [
      /^(patient_summary|patient_timeline|patient_risk|patient_list|pre_visit_plan|care_gap_list|care_gap_identify|care_gap_close|care_gap_provider|fhir_read|fhir_search|fhir_create|fhir_bundle|cds_patient_view|health_score|measure_evaluate|measure_results|measure_score|cql_evaluate|cql_result)$/
    ],
    care_coordinator: [
      /^(patient_summary|patient_list|pre_visit_plan|care_gap_list|care_gap_identify|care_gap_close|care_gap_stats|care_gap_population|care_gap_provider)$/
    ],
    quality_officer: [
      /^(measure_evaluate|measure_results|measure_score|measure_population|cql_evaluate|cql_batch|cql_libraries|cql_result|care_gap_stats|care_gap_population)$/
    ],
    executive: [
      /^(care_gap_stats|care_gap_population|measure_population|health_score)$/
    ]
  };
}

module.exports = { clinicalRolePolicies };
