const supertest = require('supertest');
const express = require('express');
const path = require('path');
const { createMcpRouter } = require('../lib/mcp-router');

describe('mcp-router demo mode integration', () => {
  const fixturesDir = path.join(__dirname, '..', '..', 'mcp-edge-platform', 'fixtures');

  const mockTool = {
    name: 'edge_health',
    description: 'test',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'REAL_HANDLER_RESPONSE' }] })
  };

  function buildApp(opts = {}) {
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [mockTool],
      serverName: 'test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false,
      fixturesDir: opts.fixturesDir || fixturesDir,
      ...opts
    }));
    return supertest(app);
  }

  afterEach(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  it('returns fixture data instead of real handler when demo mode enabled', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const request = buildApp();
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.result).toBeDefined();
    const text = res.body.result.content[0].text;
    expect(text).not.toContain('REAL_HANDLER_RESPONSE');
    expect(text).toContain('demoMode');
  });

  it('calls real handler when demo mode is disabled', async () => {
    process.env.HDIM_DEMO_MODE = 'false';
    const request = buildApp();
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.result.content[0].text).toBe('REAL_HANDLER_RESPONSE');
  });

  it('falls through to real handler when fixture not found', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const request = buildApp({ fixturesDir: path.join(__dirname, 'nonexistent-dir') });
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.result.content[0].text).toBe('REAL_HANDLER_RESPONSE');
  });

  it('does not intercept when no fixturesDir configured', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [mockTool],
      serverName: 'test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
      // no fixturesDir
    }));
    const request = supertest(app);
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.result.content[0].text).toBe('REAL_HANDLER_RESPONSE');
  });
});
