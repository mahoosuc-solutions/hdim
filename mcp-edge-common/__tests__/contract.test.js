/**
 * T20: MCP response contract validation tests
 *
 * Validates the JSON-RPC 2.0 / MCP protocol response contract
 * for BOTH the platform and devops edge sidecars.
 */
const supertest = require('supertest');

const sidecars = [
  { label: 'platform', loader: () => require('../../mcp-edge-platform/server').createApp() },
  { label: 'devops',   loader: () => require('../../mcp-edge-devops/server').createApp() }
];

let savedEnv;

beforeAll(() => {
  savedEnv = { ...process.env };
  process.env.HDIM_DEMO_MODE = 'true';
  process.env.MCP_EDGE_ENFORCE_ROLE_AUTH = 'false';
});

afterAll(() => {
  process.env.HDIM_DEMO_MODE = savedEnv.HDIM_DEMO_MODE || '';
  process.env.MCP_EDGE_ENFORCE_ROLE_AUTH = savedEnv.MCP_EDGE_ENFORCE_ROLE_AUTH || '';
  if (!savedEnv.HDIM_DEMO_MODE) delete process.env.HDIM_DEMO_MODE;
  if (!savedEnv.MCP_EDGE_ENFORCE_ROLE_AUTH) delete process.env.MCP_EDGE_ENFORCE_ROLE_AUTH;
});

function rpc(request, id, method, params = {}) {
  return request.post('/mcp').send({ jsonrpc: '2.0', id, method, params });
}

describe.each(sidecars)('$label sidecar', ({ label, loader }) => {
  let request;

  beforeAll(() => {
    const app = loader();
    request = supertest(app);
  });

  describe('initialize contract', () => {
    let body;

    beforeAll(async () => {
      const res = await rpc(request, 1, 'initialize');
      expect(res.status).toBe(200);
      body = res.body;
    });

    it('result.protocolVersion matches 2025-11-25', () => {
      expect(body.result.protocolVersion).toBe('2025-11-25');
    });

    it('result.capabilities has tools key', () => {
      expect(body.result.capabilities).toHaveProperty('tools');
    });

    it('result.serverInfo has name and version', () => {
      expect(typeof body.result.serverInfo.name).toBe('string');
      expect(body.result.serverInfo.name.length).toBeGreaterThan(0);
      expect(typeof body.result.serverInfo.version).toBe('string');
      expect(body.result.serverInfo.version.length).toBeGreaterThan(0);
    });
  });

  describe('tools/list contract', () => {
    let tools;

    beforeAll(async () => {
      const res = await rpc(request, 2, 'tools/list');
      expect(res.status).toBe(200);
      tools = res.body.result.tools;
    });

    it('result.tools is a non-empty array', () => {
      expect(Array.isArray(tools)).toBe(true);
      expect(tools.length).toBeGreaterThan(0);
    });

    it('each tool has name (string), description (string), inputSchema (object with type "object")', () => {
      for (const tool of tools) {
        expect(typeof tool.name).toBe('string');
        expect(tool.name.length).toBeGreaterThan(0);
        expect(typeof tool.description).toBe('string');
        expect(tool.description.length).toBeGreaterThan(0);
        expect(typeof tool.inputSchema).toBe('object');
        expect(tool.inputSchema).not.toBeNull();
        expect(tool.inputSchema.type).toBe('object');
      }
    });
  });

  describe('tools/call contract (edge_health)', () => {
    let content;

    beforeAll(async () => {
      const res = await rpc(request, 3, 'tools/call', {
        name: 'edge_health', arguments: {}
      });
      expect(res.status).toBe(200);
      content = res.body.result.content;
    });

    it('result.content is an array', () => {
      expect(Array.isArray(content)).toBe(true);
      expect(content.length).toBeGreaterThan(0);
    });

    it('each content item has type and text', () => {
      for (const item of content) {
        expect(typeof item.type).toBe('string');
        expect(typeof item.text).toBe('string');
      }
    });

    it('text is valid parseable JSON', () => {
      for (const item of content) {
        expect(() => JSON.parse(item.text)).not.toThrow();
      }
    });
  });

  describe('error contract', () => {
    let error;

    beforeAll(async () => {
      const res = await rpc(request, 99, 'tools/call', {
        name: 'nonexistent_tool_that_does_not_exist', arguments: {}
      });
      expect(res.status).toBe(200);
      error = res.body.error;
    });

    it('unknown tool returns error with code, message, data', () => {
      expect(typeof error.code).toBe('number');
      expect(typeof error.message).toBe('string');
      expect(error.message.length).toBeGreaterThan(0);
      expect(typeof error.data).toBe('object');
      expect(error.data).not.toBeNull();
    });

    it('error data has reason field', () => {
      expect(typeof error.data.reason).toBe('string');
      expect(error.data.reason.length).toBeGreaterThan(0);
    });
  });
});
