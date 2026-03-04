function normalizeToolName(name) {
  return String(name || '').trim().replace(/\./g, '_');
}

function extractApiKey(req) {
  const header = req?.headers?.authorization || '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice(7).trim();
  return token || null;
}

function extractOperatorRole(req) {
  const headers = req?.headers || {};
  const role = headers['x-operator-role'] || headers['x-mcp-role'] || null;
  return role ? String(role).trim().toLowerCase() : null;
}

function rolePolicies() {
  return {
    platform_admin: [/./],
    developer: [/./],
    clinical_admin: [/^(edge_health|platform_health|dashboard_stats)$/],
    quality_officer: [/^(edge_health|dashboard_stats)$/],
    executive: [/^(edge_health|dashboard_stats|platform_info)$/],
    clinician: [/^(edge_health|platform_health)$/],
    care_coordinator: [/^(edge_health)$/]
  };
}

function authorizeToolCall({ toolName, role, enforce }) {
  const normalized = normalizeToolName(toolName);

  if (!enforce) {
    return { allowed: true, normalized, reason: 'role_auth_disabled' };
  }

  if (!role) {
    return { allowed: false, normalized, reason: 'missing_operator_role' };
  }

  const policies = rolePolicies();
  const matchers = policies[role];
  if (!matchers || matchers.length === 0) {
    return { allowed: false, normalized, reason: 'unknown_role' };
  }

  const allowed = matchers.some((m) => m.test(normalized));
  return { allowed, normalized, reason: allowed ? 'allowed' : 'forbidden_for_role' };
}

module.exports = {
  normalizeToolName,
  extractApiKey,
  extractOperatorRole,
  rolePolicies,
  authorizeToolCall
};
