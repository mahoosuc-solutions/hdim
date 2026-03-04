const express = require('express');
const supertest = require('supertest');
const { Writable } = require('node:stream');
const { createMcpRouter } = require('../lib/mcp-router');
const { createAuditLogger, scrubSensitive } = require('../lib/audit-log');

function captureLogger(serviceName) {
  const entries = [];
  const stream = new Writable({
    write(chunk, enc, cb) { entries.push(JSON.parse(chunk.toString())); cb(); }
  });
  return { logger: createAuditLogger({ serviceName, stream }), entries };
}

const tools = [
  {
    name: 'test_tool',
    description: 'Works',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'ok' }] })
  },
  {
    name: 'error_tool',
    description: 'Fails',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => { throw new Error('fail'); }
  }
];

describe('PROOF: Audit Trail — HIPAA 164.312(b), SOC2 CC7.2, NIST AU-2/AU-3', () => {
  let app, request, entries;

  beforeAll(() => {
    const capture = captureLogger('proof-svc');
    entries = capture.entries;
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools, serverName: 'proof', serverVersion: '0.1.0',
      enforceRoleAuth: true, logger: capture.logger
    }));
    request = supertest(app);
  });

  beforeEach(() => { entries.length = 0; });

  it('logs role identity on every tool call', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'developer')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'test_tool', arguments: {} } });
    expect(entries.find(e => e.role === 'developer')).toBeDefined();
  });

  it('logs tool name on every tool call', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call', params: { name: 'test_tool', arguments: {} } });
    expect(entries.find(e => e.tool === 'test_tool')).toBeDefined();
  });

  it('logs duration_ms as a non-negative number', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'test_tool', arguments: {} } });
    const entry = entries.find(e => e.msg === 'tool_call');
    expect(entry.duration_ms).toBeGreaterThanOrEqual(0);
  });

  it('logs success: true on successful calls', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 4, method: 'tools/call', params: { name: 'test_tool', arguments: {} } });
    expect(entries.find(e => e.success === true)).toBeDefined();
  });

  it('logs tool_error with error_code on failed calls', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 5, method: 'tools/call', params: { name: 'error_tool', arguments: {} } });
    const entry = entries.find(e => e.msg === 'tool_error');
    expect(entry).toBeDefined();
    expect(entry.error_code).toBe(-32603);
  });

  it('scrubs Bearer tokens from strings', () => {
    expect(scrubSensitive('Authorization: Bearer abc123xyz')).toBe('Authorization: Bearer [REDACTED]');
  });

  it('scrubs patient_id from objects', () => {
    expect(scrubSensitive({ patient_id: '12345', status: 'ok' })).toEqual({ patient_id: '[REDACTED]', status: 'ok' });
  });

  it('scrubs ssn from objects', () => {
    expect(scrubSensitive({ ssn: '123-45-6789' })).toEqual({ ssn: '[REDACTED]' });
  });

  it('scrubs mrn from objects', () => {
    expect(scrubSensitive({ mrn: 'MRN001' })).toEqual({ mrn: '[REDACTED]' });
  });

  it('logs tool_forbidden for unauthorized access attempts', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'care_coordinator')
      .send({ jsonrpc: '2.0', id: 6, method: 'tools/call', params: { name: 'test_tool', arguments: {} } });
    const entry = entries.find(e => e.msg === 'tool_forbidden');
    expect(entry).toBeDefined();
    expect(entry.role).toBe('care_coordinator');
  });
});
