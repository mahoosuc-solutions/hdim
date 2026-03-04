const supertest = require('supertest');

describe('loadStrategy — invalid strategy name', () => {
  it('throws for unknown strategy name', () => {
    jest.isolateModules(() => {
      const savedStrategy = process.env.CLINICAL_TOOL_STRATEGY;
      process.env.CLINICAL_TOOL_STRATEGY = 'nonexistent';
      try {
        const { createApp } = require('../server');
        expect(() => createApp()).toThrow('Unknown clinical tool strategy');
      } finally {
        if (savedStrategy !== undefined) {
          process.env.CLINICAL_TOOL_STRATEGY = savedStrategy;
        } else {
          delete process.env.CLINICAL_TOOL_STRATEGY;
        }
      }
    });
  });
});

describe('loadRolePolicies — strategy without role-policies', () => {
  it('returns undefined when role-policies module does not exist', () => {
    jest.isolateModules(() => {
      // Mock require so that the role-policies require inside loadRolePolicies throws
      const serverPath = require.resolve('../server');
      // We need to test the catch branch in loadRolePolicies.
      // We do this by mocking the role-policies module to throw.
      const strategyName = 'composite';
      const rolePoliciesPath = require.resolve(`../lib/strategies/${strategyName}/role-policies`);

      // Pre-cache a throwing module for role-policies
      jest.doMock(rolePoliciesPath, () => { throw new Error('MODULE_NOT_FOUND'); });

      const savedStrategy = process.env.CLINICAL_TOOL_STRATEGY;
      const savedDemo = process.env.HDIM_DEMO_MODE;
      process.env.CLINICAL_TOOL_STRATEGY = strategyName;
      process.env.HDIM_DEMO_MODE = 'true';
      try {
        const { createApp } = require('../server');
        // Should not throw — it should gracefully fall back
        const app = createApp();
        expect(app).toBeDefined();
      } finally {
        jest.dontMock(rolePoliciesPath);
        if (savedStrategy !== undefined) {
          process.env.CLINICAL_TOOL_STRATEGY = savedStrategy;
        } else {
          delete process.env.CLINICAL_TOOL_STRATEGY;
        }
        if (savedDemo !== undefined) {
          process.env.HDIM_DEMO_MODE = savedDemo;
        } else {
          delete process.env.HDIM_DEMO_MODE;
        }
      }
    });
  });
});

describe('clinical edge server', () => {
  let app, request;

  beforeAll(() => {
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    process.env.HDIM_DEMO_MODE = 'true';
    const { createApp } = require('../server');
    app = createApp();
    request = supertest(app);
  });

  afterAll(() => {
    delete process.env.CLINICAL_TOOL_STRATEGY;
    delete process.env.HDIM_DEMO_MODE;
  });

  it('responds to /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-clinical-edge');
    expect(res.body.status).toBe('healthy');
  });

  it('responds to MCP initialize', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-clinical-edge');
    expect(res.body.result.protocolVersion).toBe('2025-11-25');
  });

  it('lists 26 tools from composite strategy', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
    expect(res.status).toBe(200);
    const toolNames = res.body.result.tools.map(t => t.name);
    expect(toolNames).toContain('patient_summary');
    expect(toolNames).toContain('fhir_read');
    expect(toolNames).toContain('care_gap_list');
    expect(toolNames).toContain('measure_evaluate');
    expect(toolNames).toContain('cql_evaluate');
    expect(toolNames).toContain('edge_health');
    expect(toolNames.length).toBe(26);
  });

  it('can call a tool in demo mode', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: 'patient_summary', arguments: { patientId: 'demo-1', tenantId: 'acme' } } });
    expect(res.body.result).toBeDefined();
    expect(res.body.error).toBeUndefined();
  });
});
