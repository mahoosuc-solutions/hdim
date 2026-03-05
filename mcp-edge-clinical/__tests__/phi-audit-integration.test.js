/**
 * PHI Audit Integration Test
 *
 * Full round-trip: clinical tool call through MCP router → phiAuditLogger fires
 * with correct action type, phi flag, and patientId extraction.
 *
 * Verifies 4 hook points: PHI_ACCESS, PHI_WRITE, TOOL_CALL, AUTH_DENIED
 */
const supertest = require('supertest');

describe('PHI audit integration (demo mode, composite)', () => {
  let request, auditLog;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    jest.resetModules();

    // Intercept phi-audit logger to capture audit events
    auditLog = [];
    const realPhiAudit = require('../lib/phi-audit');
    const origCreate = realPhiAudit.createPhiAuditLogger;
    jest.spyOn(realPhiAudit, 'createPhiAuditLogger').mockImplementation((opts) => {
      const logger = origCreate(opts);
      const origAccess = logger.logToolAccess;
      const origDenied = logger.logAuthDenied;
      logger.logToolAccess = (entry) => {
        auditLog.push({ method: 'logToolAccess', ...entry });
        origAccess(entry);
      };
      logger.logAuthDenied = (entry) => {
        auditLog.push({ method: 'logAuthDenied', ...entry });
        origDenied(entry);
      };
      return logger;
    });

    const { createApp } = require('../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
    jest.restoreAllMocks();
  });

  beforeEach(() => {
    auditLog.length = 0;
  });

  function callTool(name, args, role = 'clinical_admin') {
    return request.post('/mcp')
      .set('x-operator-role', role)
      .send({
        jsonrpc: '2.0', id: 1,
        method: 'tools/call',
        params: { name, arguments: args }
      });
  }

  describe('PHI_ACCESS — PHI read tool with patientId', () => {
    it('patient_summary emits PHI_ACCESS with patientId', async () => {
      const res = await callTool('patient_summary', { patientId: 'p-123', tenantId: 'acme' });
      expect(res.status).toBe(200);

      const entry = auditLog.find(e => e.method === 'logToolAccess' && e.tool === 'patient_summary');
      expect(entry).toBeDefined();
      expect(entry.phi).toBe(true);
      expect(entry.write).toBe(false);
      expect(entry.patientId).toBe('p-123');
      expect(entry.success).toBe(true);
      expect(typeof entry.durationMs).toBe('number');
    });
  });

  describe('PHI_WRITE — PHI write tool', () => {
    it('care_gap_close emits PHI_WRITE', async () => {
      const res = await callTool('care_gap_close', {
        gapId: 'g-1', tenantId: 'acme', closedBy: 'dr-1', reason: 'Resolved'
      });
      expect(res.status).toBe(200);

      const entry = auditLog.find(e => e.method === 'logToolAccess' && e.tool === 'care_gap_close');
      expect(entry).toBeDefined();
      expect(entry.phi).toBe(true);
      expect(entry.write).toBe(true);
      expect(entry.success).toBe(true);
    });
  });

  describe('TOOL_CALL — non-PHI tool', () => {
    it('care_gap_stats emits TOOL_CALL (phi:false)', async () => {
      const res = await callTool('care_gap_stats', { tenantId: 'acme' });
      expect(res.status).toBe(200);

      const entry = auditLog.find(e => e.method === 'logToolAccess' && e.tool === 'care_gap_stats');
      expect(entry).toBeDefined();
      expect(entry.phi).toBe(false);
      expect(entry.patientId).toBeUndefined();
      expect(entry.success).toBe(true);
    });
  });

  describe('AUTH_DENIED — forbidden tool', () => {
    it('patient_summary denied for executive emits AUTH_DENIED', async () => {
      const res = await callTool('patient_summary', { patientId: 'p-1', tenantId: 'acme' }, 'executive');

      const entry = auditLog.find(e => e.method === 'logAuthDenied' && e.tool === 'patient_summary');
      expect(entry).toBeDefined();
      expect(entry.role).toBe('executive');
    });
  });

  describe('edge_health — infrastructure tool has no audit annotation', () => {
    it('edge_health emits TOOL_CALL (phi:false) since no audit field', async () => {
      const res = await callTool('edge_health', {}, 'platform_admin');
      expect(res.status).toBe(200);

      const entry = auditLog.find(e => e.method === 'logToolAccess' && e.tool === 'edge_health');
      expect(entry).toBeDefined();
      expect(entry.phi).toBe(false);
    });
  });

  describe('patientId extraction for search tools', () => {
    it('fhir_search with patient arg extracts patientId', async () => {
      const res = await callTool('fhir_search', {
        resourceType: 'Observation', tenantId: 'acme', patient: 'p-456'
      });
      expect(res.status).toBe(200);

      const entry = auditLog.find(e => e.method === 'logToolAccess' && e.tool === 'fhir_search');
      expect(entry).toBeDefined();
      expect(entry.phi).toBe(true);
      expect(entry.patientId).toBe('p-456');
    });
  });
});
