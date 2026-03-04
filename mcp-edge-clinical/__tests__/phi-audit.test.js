const { createPhiAuditLogger } = require('../lib/phi-audit');

describe('createPhiAuditLogger — default serviceName', () => {
  it('uses default serviceName when none provided', () => {
    const logged = [];
    const mockStream = { write: (chunk) => { logged.push(JSON.parse(chunk)); } };
    const logger = createPhiAuditLogger({ stream: mockStream });

    logger.logToolAccess({
      tool: 'test_tool', role: 'admin', tenantId: 'acme',
      success: true, durationMs: 10, phi: false
    });
    expect(logged[0].source).toBe('mcp-edge-clinical');
  });
});

describe('createPhiAuditLogger', () => {
  let logged, logger;

  beforeEach(() => {
    logged = [];
    const mockStream = { write: (chunk) => { logged.push(JSON.parse(chunk)); } };
    logger = createPhiAuditLogger({ serviceName: 'mcp-edge-clinical', stream: mockStream });
  });

  describe('logToolAccess()', () => {
    it('logs PHI_ACCESS for PHI tools with patientId', () => {
      logger.logToolAccess({
        tool: 'patient_summary', role: 'clinician', tenantId: 'acme',
        patientId: 'p-123', success: true, durationMs: 234, phi: true
      });
      expect(logged).toHaveLength(1);
      expect(logged[0]).toMatchObject({
        level: 'info', action: 'PHI_ACCESS', tool: 'patient_summary',
        role: 'clinician', tenantId: 'acme', patientId: 'p-123',
        success: true, durationMs: 234
      });
    });

    it('logs TOOL_CALL for non-PHI tools without patientId', () => {
      logger.logToolAccess({
        tool: 'care_gap_stats', role: 'quality_officer', tenantId: 'acme',
        success: true, durationMs: 100, phi: false
      });
      expect(logged).toHaveLength(1);
      expect(logged[0]).toMatchObject({ action: 'TOOL_CALL', tool: 'care_gap_stats' });
      expect(logged[0].patientId).toBeUndefined();
    });

    it('logs PHI_WRITE for write operations on PHI tools', () => {
      logger.logToolAccess({
        tool: 'care_gap_close', role: 'clinician', tenantId: 'acme',
        patientId: 'p-456', success: true, durationMs: 300, phi: true, write: true
      });
      expect(logged[0].action).toBe('PHI_WRITE');
    });

    it('includes source field', () => {
      logger.logToolAccess({
        tool: 'cql_evaluate', role: 'developer', tenantId: 't1',
        patientId: 'p-789', success: true, durationMs: 50, phi: true
      });
      expect(logged[0].source).toBe('mcp-edge-clinical');
    });
  });

  describe('logAuthDenied()', () => {
    it('logs AUTH_DENIED with role and tool', () => {
      logger.logAuthDenied({ tool: 'patient_summary', role: 'executive' });
      expect(logged[0]).toMatchObject({
        action: 'AUTH_DENIED', tool: 'patient_summary', role: 'executive'
      });
    });

    it('includes source field', () => {
      logger.logAuthDenied({ tool: 'fhir_read', role: 'care_coordinator' });
      expect(logged[0].source).toBe('mcp-edge-clinical');
    });
  });
});
