const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  readTestContext,
  writeTestContext,
  callTool
} = require('../lib/live-test-helpers');

const fs = require('fs');
const path = require('path');

describe('live-test-helpers', () => {
  describe('LIVE_TEST_DEFAULTS', () => {
    it('has tenantId "demo"', () => {
      expect(LIVE_TEST_DEFAULTS.tenantId).toBe('demo');
    });

    it('has gatewayUrl defaulting to localhost:18080', () => {
      expect(LIVE_TEST_DEFAULTS.gatewayUrl).toBe('http://localhost:18080');
    });
  });

  describe('isGatewayReachable', () => {
    it('returns false for unreachable URL', async () => {
      const result = await isGatewayReachable('http://localhost:19999');
      expect(result).toBe(false);
    }, 10000);
  });

  describe('readTestContext / writeTestContext', () => {
    const testContextPath = '/tmp/live-test-context.json';

    afterEach(() => {
      try { fs.unlinkSync(testContextPath); } catch { /* ignore */ }
    });

    it('round-trips context through file', () => {
      const ctx = { patients: { 't2dm-managed': 'uuid-1' } };
      writeTestContext(ctx);
      const result = readTestContext();
      expect(result).toEqual(ctx);
    });

    it('readTestContext returns null when file missing', () => {
      expect(readTestContext()).toBeNull();
    });
  });

  describe('callTool', () => {
    it('sends MCP JSON-RPC 2.0 tools/call request', async () => {
      let capturedPath, capturedHeaders, capturedBody;
      const mockRequest = {
        post: (p) => {
          capturedPath = p;
          const chain = {
            set: (k, v) => { capturedHeaders = capturedHeaders || {}; capturedHeaders[k] = v; return chain; },
            send: (b) => { capturedBody = b; return Promise.resolve({ status: 200, body: { jsonrpc: '2.0', id: 1, result: {} } }); }
          };
          return chain;
        }
      };

      await callTool(mockRequest, 'patient_summary', { patientId: 'p1', tenantId: 'demo' }, 'clinical_admin');
      expect(capturedPath).toBe('/mcp');
      expect(capturedHeaders['x-operator-role']).toBe('clinical_admin');
      expect(capturedBody.method).toBe('tools/call');
      expect(capturedBody.params.name).toBe('patient_summary');
      expect(capturedBody.params.arguments).toEqual({ patientId: 'p1', tenantId: 'demo' });
    });
  });
});
