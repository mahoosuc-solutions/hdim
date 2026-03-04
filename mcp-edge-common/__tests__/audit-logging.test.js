const express = require('express');
const supertest = require('supertest');
const { Writable } = require('node:stream');
const { createMcpRouter } = require('../lib/mcp-router');
const { createAuditLogger } = require('../lib/audit-log');

function captureLogger(serviceName) {
  const entries = [];
  const stream = new Writable({
    write(chunk, enc, cb) { entries.push(JSON.parse(chunk.toString())); cb(); }
  });
  const logger = createAuditLogger({ serviceName, stream });
  return { logger, entries };
}

const testTools = [
  {
    name: 'good_tool',
    description: 'Works fine',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'ok' }] })
  },
  {
    name: 'bad_tool',
    description: 'Always throws',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => { throw new Error('boom'); }
  }
];

describe('audit logging middleware', () => {
  let app, request, entries;

  beforeAll(() => {
    const capture = captureLogger('audit-test');
    entries = capture.entries;
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: testTools,
      serverName: 'audit-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false,
      logger: capture.logger
    }));
    request = supertest(app);
  });

  beforeEach(() => { entries.length = 0; });

  it('emits tool_call audit entry on successful tool call', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'good_tool', arguments: {} } });

    const entry = entries.find(e => e.msg === 'tool_call');
    expect(entry).toBeDefined();
    expect(entry.tool).toBe('good_tool');
    expect(entry.role).toBe('platform_admin');
    expect(entry.success).toBe(true);
    expect(typeof entry.duration_ms).toBe('number');
    expect(entry.duration_ms).toBeGreaterThanOrEqual(0);
  });

  it('emits tool_error audit entry on failed tool call', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call', params: { name: 'bad_tool', arguments: {} } });

    const entry = entries.find(e => e.msg === 'tool_error');
    expect(entry).toBeDefined();
    expect(entry.tool).toBe('bad_tool');
    expect(entry.role).toBe('platform_admin');
    expect(entry.error_code).toBe(-32603);
    expect(typeof entry.duration_ms).toBe('number');
  });

  it('does not emit audit entry for initialize or tools/list', async () => {
    await request.post('/mcp').send({ jsonrpc: '2.0', id: 3, method: 'initialize', params: {} });
    await request.post('/mcp').send({ jsonrpc: '2.0', id: 4, method: 'tools/list', params: {} });

    const toolEntries = entries.filter(e => e.msg === 'tool_call' || e.msg === 'tool_error');
    expect(toolEntries).toHaveLength(0);
  });

  it('scrubs Bearer tokens from tool error responses', async () => {
    const phiTools = [{
      name: 'bearer_error_tool',
      description: 'Throws error with Bearer token',
      inputSchema: { type: 'object', properties: {}, additionalProperties: false },
      handler: async () => { throw new Error('Auth failed: Bearer eyJhbGciOiJSUzI1NiJ9.secret'); }
    }];

    const capture = captureLogger('scrub-test');
    const scrubApp = express();
    scrubApp.use(express.json());
    scrubApp.use(createMcpRouter({
      tools: phiTools, serverName: 'scrub-test', serverVersion: '0.1.0',
      enforceRoleAuth: false, logger: capture.logger
    }));

    const res = await supertest(scrubApp).post('/mcp').send({
      jsonrpc: '2.0', id: 99, method: 'tools/call',
      params: { name: 'bearer_error_tool', arguments: {} }
    });

    expect(res.body.error.data.detail).not.toContain('eyJhbGciOiJSUzI1NiJ9');
    expect(res.body.error.data.detail).toContain('Bearer [REDACTED]');
  });

  it('includes demo flag in audit entry', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 5, method: 'tools/call', params: { name: 'good_tool', arguments: {} } });
    delete process.env.HDIM_DEMO_MODE;

    const entry = entries.find(e => e.msg === 'tool_call');
    expect(entry.demo).toBe(true);
  });
});
