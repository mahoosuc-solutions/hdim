// mcp-edge-clinical/lib/strategies/full-surface/role-policies.js
// Clinical RBAC role policies for the full-surface strategy (68 tools, 7 roles).
// Admin/developer/clinician roles see the entire surface. Narrower roles are scoped.

function clinicalRolePolicies() {
  return {
    clinical_admin: [/./],
    platform_admin: [/./],
    developer: [/./],
    clinician: [/./],  // Full surface — clinicians see everything
    care_coordinator: [/^patient_(read|search)$/, /^care_gap_/, /^measure_results$/],
    quality_officer: [/^measure_/, /^cql_/, /^care_gap_(stats|population)$/],
    executive: [/^(care_gap_stats|care_gap_population|measure_population|health_score)$/]
  };
}

module.exports = { clinicalRolePolicies };
