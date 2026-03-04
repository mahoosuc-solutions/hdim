const { extractApiKey, extractOperatorRole, authorizeToolCall, rolePolicies, normalizeToolName } = require('../lib/auth');

describe('auth', () => {
  describe('extractApiKey', () => {
    it('extracts Bearer token from authorization header', () => {
      const req = { headers: { authorization: 'Bearer hdim_abc123' } };
      expect(extractApiKey(req)).toBe('hdim_abc123');
    });

    it('returns null when no authorization header', () => {
      expect(extractApiKey({ headers: {} })).toBeNull();
    });

    it('returns null for non-Bearer scheme', () => {
      const req = { headers: { authorization: 'Basic abc' } };
      expect(extractApiKey(req)).toBeNull();
    });
  });

  describe('extractOperatorRole', () => {
    it('extracts from x-operator-role header', () => {
      const req = { headers: { 'x-operator-role': 'platform_admin' } };
      expect(extractOperatorRole(req)).toBe('platform_admin');
    });

    it('normalizes to lowercase', () => {
      const req = { headers: { 'x-operator-role': 'DEVELOPER' } };
      expect(extractOperatorRole(req)).toBe('developer');
    });

    it('returns null when missing', () => {
      expect(extractOperatorRole({ headers: {} })).toBeNull();
    });
  });

  describe('authorizeToolCall', () => {
    it('allows platform_admin to call any tool', () => {
      const result = authorizeToolCall({ toolName: 'docker_restart', role: 'platform_admin', enforce: true });
      expect(result.allowed).toBe(true);
    });

    it('blocks unknown roles', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: 'hacker', enforce: true });
      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('unknown_role');
    });

    it('allows all tools when enforcement is disabled', () => {
      const result = authorizeToolCall({ toolName: 'anything', role: null, enforce: false });
      expect(result.allowed).toBe(true);
      expect(result.reason).toBe('role_auth_disabled');
    });

    it('blocks missing role when enforced', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: null, enforce: true });
      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('missing_operator_role');
    });

    it('allows developer to call edge_health', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: 'developer', enforce: true });
      expect(result.allowed).toBe(true);
    });

    it('blocks executive from devops tools', () => {
      const result = authorizeToolCall({ toolName: 'docker_restart', role: 'executive', enforce: true });
      expect(result.allowed).toBe(false);
    });
  });

  describe('role policy matrix — design spec compliance', () => {
    const cases = [
      ['platform_admin', 'edge_health', true],
      ['platform_admin', 'docker_restart', true],
      ['platform_admin', 'any_tool_at_all', true],
      ['developer', 'fhir_metadata', true],
      ['developer', 'docker_logs', true],
      ['clinical_admin', 'edge_health', true],
      ['clinical_admin', 'platform_health', true],
      ['clinical_admin', 'dashboard_stats', true],
      ['clinical_admin', 'platform_info', false],
      ['clinical_admin', 'fhir_metadata', false],
      ['clinical_admin', 'service_catalog', false],
      ['quality_officer', 'edge_health', true],
      ['quality_officer', 'dashboard_stats', true],
      ['quality_officer', 'platform_info', false],
      ['quality_officer', 'platform_health', false],
      ['quality_officer', 'fhir_metadata', false],
      ['executive', 'edge_health', true],
      ['executive', 'dashboard_stats', true],
      ['executive', 'platform_info', true],
      ['executive', 'fhir_metadata', false],
      ['executive', 'service_catalog', false],
      ['clinician', 'edge_health', true],
      ['clinician', 'platform_health', true],
      ['clinician', 'dashboard_stats', false],
      ['clinician', 'fhir_metadata', false],
      ['care_coordinator', 'edge_health', true],
      ['care_coordinator', 'platform_health', false],
      ['care_coordinator', 'dashboard_stats', false],
    ];

    it.each(cases)('role=%s tool=%s → allowed=%s', (role, toolName, expected) => {
      const result = authorizeToolCall({ toolName, role, enforce: true });
      expect(result.allowed).toBe(expected);
    });
  });

  describe('regex anchor safety — tool name prefix attacks', () => {
    it('edge_health_extended should NOT match edge_health policy', () => {
      const result = authorizeToolCall({ toolName: 'edge_health_extended', role: 'care_coordinator', enforce: true });
      expect(result.allowed).toBe(false);
    });

    it('dashboard_stats_admin should NOT match dashboard_stats policy', () => {
      const result = authorizeToolCall({ toolName: 'dashboard_stats_admin', role: 'quality_officer', enforce: true });
      expect(result.allowed).toBe(false);
    });

    it('platform_info_secret should NOT match platform_info policy', () => {
      const result = authorizeToolCall({ toolName: 'platform_info_secret', role: 'executive', enforce: true });
      expect(result.allowed).toBe(false);
    });
  });
});

describe('normalizeToolName edge cases', () => {
  it('replaces dots with underscores', () => {
    expect(normalizeToolName('tool.name.here')).toBe('tool_name_here');
  });
  it('trims whitespace', () => {
    expect(normalizeToolName('  edge_health  ')).toBe('edge_health');
  });
  it('handles null', () => {
    expect(normalizeToolName(null)).toBe('');
  });
  it('handles undefined', () => {
    expect(normalizeToolName(undefined)).toBe('');
  });
  it('handles empty string', () => {
    expect(normalizeToolName('')).toBe('');
  });
  it('converts number to string', () => {
    expect(normalizeToolName(123)).toBe('123');
  });
});

describe('extractApiKey edge cases', () => {
  it('returns null for empty bearer token', () => {
    expect(extractApiKey({ headers: { authorization: 'Bearer ' } })).toBeNull();
  });
  it('returns null for Basic scheme', () => {
    expect(extractApiKey({ headers: { authorization: 'Basic abc123' } })).toBeNull();
  });
  it('returns token with whitespace trimmed', () => {
    expect(extractApiKey({ headers: { authorization: 'Bearer  abc123  ' } })).toBe('abc123');
  });
  it('handles null req', () => {
    expect(extractApiKey(null)).toBeNull();
  });
  it('handles missing headers object', () => {
    expect(extractApiKey({})).toBeNull();
  });
  it('handles undefined req', () => {
    expect(extractApiKey(undefined)).toBeNull();
  });
});
