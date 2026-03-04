// mcp-edge-clinical/lib/strategies/high-value/role-policies.js
// Clinical RBAC role policies for the high-value strategy (15 tools, 7 roles).
// Each role maps to an array of regex matchers tested against the tool name.

function clinicalRolePolicies() {
  return {
    clinical_admin: [/./],
    platform_admin: [/./],
    developer: [/./],
    clinician: [
      /^(patient_read|patient_search|observation_read|observation_search|condition_read|condition_search|medication_read|medication_search|encounter_read|encounter_search|care_gap_list|care_gap_close|measure_evaluate|measure_results)$/
    ],
    care_coordinator: [
      /^(patient_read|patient_search|care_gap_list|care_gap_close|care_gap_stats)$/
    ],
    quality_officer: [
      /^(measure_evaluate|measure_results|care_gap_stats)$/
    ],
    executive: [
      /^(care_gap_stats)$/
    ]
  };
}

module.exports = { clinicalRolePolicies };
