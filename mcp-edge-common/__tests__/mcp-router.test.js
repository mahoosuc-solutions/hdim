const express = require('express');
const supertest = require('supertest');
const { createMcpRouter } = require('../lib/mcp-router');

const testTools = [
  {
    name: 'test_tool',
    description: 'A test tool',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'hello' }] })
  }
];

describe('mcp-router', () => {
  let app;
  let request;

  beforeAll(() => {
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: testTools,
      serverName: 'test-edge',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('responds to initialize', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('test-edge');
    expect(res.body.result.capabilities.tools).toBeDefined();
  });

  it('lists tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/list', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.tools).toHaveLength(1);
    expect(res.body.result.tools[0].name).toBe('test_tool');
  });

  it('calls a tool', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'test_tool', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.result.content[0].text).toBe('hello');
  });

  it('returns method not found for unknown method', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'unknown/method', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32601);
  });

  it('returns tool not found for unknown tool', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 5, method: 'tools/call',
      params: { name: 'nonexistent', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32602);
  });

  it('handles notifications (no id) with 204', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/initialized', params: {}
    });
    expect(res.status).toBe(204);
  });
});

describe('mcp-router protocol validation', () => {
  let app;
  let request;

  beforeAll(() => {
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: testTools,
      serverName: 'proto-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('rejects non-2.0 jsonrpc version with -32600', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '1.0', id: 10, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32600);
  });

  it('rejects missing jsonrpc field with -32600', async () => {
    const res = await request.post('/mcp').send({
      id: 11, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32600);
  });

  it('tools/call with missing params returns -32602 (tool not found)', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 12, method: 'tools/call'
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32602);
  });

  it('notifications/initialized returns 204', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/initialized', params: {}
    });
    expect(res.status).toBe(204);
  });

  it('notifications/cancelled returns 204', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/cancelled', params: {}
    });
    expect(res.status).toBe(204);
  });
});

describe('mcp-router tool handler errors', () => {
  it('returns -32603 when tool handler throws Error', async () => {
    const errorTools = [
      {
        name: 'throw_error',
        description: 'Throws an Error',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => { throw new Error('handler boom'); }
      }
    ];
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: errorTools,
      serverName: 'err-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 20, method: 'tools/call',
      params: { name: 'throw_error', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.detail).toBe('handler boom');
  });

  it('returns -32603 when tool handler throws string', async () => {
    const stringTools = [
      {
        name: 'throw_string',
        description: 'Throws a string',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => { throw 'string error'; }
      }
    ];
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: stringTools,
      serverName: 'err-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 21, method: 'tools/call',
      params: { name: 'throw_string', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.detail).toBe('string error');
  });
});

describe('mcp-router auth enforcement', () => {
  let app;
  let request;

  beforeAll(() => {
    const restrictedTools = [
      {
        name: 'docker_restart',
        description: 'Restart docker',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => ({ content: [{ type: 'text', text: 'restarted' }] })
      }
    ];
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: restrictedTools,
      serverName: 'auth-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: true
    }));
    request = supertest(app);
  });

  it('returns -32603 with reason missing_operator_role when no role header', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 30, method: 'tools/call',
      params: { name: 'docker_restart', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('missing_operator_role');
  });

  it('returns -32603 with reason forbidden_for_role for unauthorized role', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({
        jsonrpc: '2.0', id: 31, method: 'tools/call',
        params: { name: 'docker_restart', arguments: {} }
      });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('forbidden_for_role');
  });
});

describe('mcp-router param validation', () => {
  it('rejects extra properties with -32602', async () => {
    const strictTools = [{
      name: 'strict_tool',
      description: 'Has strict schema',
      inputSchema: {
        type: 'object',
        properties: { service: { type: 'string' } },
        required: ['service'],
        additionalProperties: false
      },
      handler: async (args) => ({ content: [{ type: 'text', text: JSON.stringify(args) }] })
    }];
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: strictTools,
      serverName: 'param-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 50, method: 'tools/call',
      params: { name: 'strict_tool', arguments: { service: 'gw', evil: 'injected' } }
    });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('invalid_params');
  });

  it('rejects wrong type with -32602', async () => {
    const strictTools = [{
      name: 'typed_tool',
      description: 'Has typed schema',
      inputSchema: {
        type: 'object',
        properties: { count: { type: 'integer' } },
        additionalProperties: false
      },
      handler: async (args) => ({ content: [{ type: 'text', text: JSON.stringify(args) }] })
    }];
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: strictTools,
      serverName: 'param-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 51, method: 'tools/call',
      params: { name: 'typed_tool', arguments: { count: 'not-a-number' } }
    });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
    expect(res.body.error.data.reason).toBe('invalid_params');
  });

  it('allows valid params through', async () => {
    const strictTools = [{
      name: 'valid_tool',
      description: 'Valid test',
      inputSchema: {
        type: 'object',
        properties: { name: { type: 'string' } },
        required: ['name'],
        additionalProperties: false
      },
      handler: async (args) => ({ content: [{ type: 'text', text: args.name }] })
    }];
    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: strictTools,
      serverName: 'param-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 52, method: 'tools/call',
      params: { name: 'valid_tool', arguments: { name: 'hello' } }
    });
    expect(res.body.result).toBeDefined();
    expect(res.body.result.content[0].text).toBe('hello');
  });
});
