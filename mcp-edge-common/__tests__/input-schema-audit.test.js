const platformApp = require('../../mcp-edge-platform/server').createApp;
const devopsApp = require('../../mcp-edge-devops/server').createApp;
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

describe('PROOF: InputSchema Audit — all tools enforce additionalProperties: false', () => {
  async function getTools(app) {
    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    return res.body.result.tools;
  }

  it('platform edge — all 8 tools have strict inputSchema', async () => {
    const tools = await getTools(platformApp());
    expect(tools).toHaveLength(8);
    for (const tool of tools) {
      expect(tool.inputSchema).toBeDefined();
      expect(tool.inputSchema.type).toBe('object');
      expect(tool.inputSchema.additionalProperties).toBe(false);
    }
  });

  it('devops edge — all 7 tools have strict inputSchema', async () => {
    const tools = await getTools(devopsApp());
    expect(tools).toHaveLength(7);
    for (const tool of tools) {
      expect(tool.inputSchema).toBeDefined();
      expect(tool.inputSchema.type).toBe('object');
      expect(tool.inputSchema.additionalProperties).toBe(false);
    }
  });
});
