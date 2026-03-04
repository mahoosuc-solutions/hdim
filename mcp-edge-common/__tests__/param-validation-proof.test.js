const express = require('express');
const supertest = require('supertest');
const { createMcpRouter } = require('../lib/mcp-router');

const strictTools = [
  {
    name: 'strict_service',
    description: 'Requires service name',
    inputSchema: {
      type: 'object',
      properties: {
        service: { type: 'string', maxLength: 100 },
        tail: { type: 'integer', minimum: 1, maximum: 10000 }
      },
      required: ['service'],
      additionalProperties: false
    },
    handler: async (args) => ({ content: [{ type: 'text', text: JSON.stringify(args) }] })
  },
  {
    name: 'no_params',
    description: 'Takes no params',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'ok' }] })
  }
];

describe('PROOF: Param Validation — HIPAA 164.312(c)(1), NIST SI-10, CIS Input', () => {
  let app, request;

  beforeAll(() => {
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: strictTools,
      serverName: 'param-proof',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('rejects extra properties with -32602 and reason invalid_params', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'strict_service', arguments: { service: 'gw', evil: 'code' } }
    });
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('invalid_params');
  });

  it('rejects missing required field with -32602', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/call',
      params: { name: 'strict_service', arguments: {} }
    });
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('invalid_params');
  });

  it('rejects type mismatch (string where integer expected)', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'strict_service', arguments: { service: 'gw', tail: 'not-a-number' } }
    });
    expect(res.body.error.code).toBe(-32602);
  });

  it('allows valid params through to handler', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'tools/call',
      params: { name: 'strict_service', arguments: { service: 'gateway', tail: 50 } }
    });
    expect(res.body.result).toBeDefined();
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.service).toBe('gateway');
  });

  it('allows empty params for tools that take no params', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 5, method: 'tools/call',
      params: { name: 'no_params', arguments: {} }
    });
    expect(res.body.result).toBeDefined();
  });

  it('rejects null params for tools with required fields', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 6, method: 'tools/call',
      params: { name: 'strict_service', arguments: null }
    });
    expect(res.body.error.code).toBe(-32602);
  });
});
