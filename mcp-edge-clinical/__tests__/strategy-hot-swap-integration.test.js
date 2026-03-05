const supertest = require('supertest');

beforeAll(() => {
  process.env.HDIM_DEMO_MODE = 'true';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  process.env.MCP_EDGE_RATE_LIMIT_MAX = '500';
});
afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
  delete process.env.CLINICAL_TOOL_STRATEGY;
  delete process.env.MCP_EDGE_RATE_LIMIT_MAX;
});

function mcpCall(request, method, params, role = 'platform_admin') {
  return request.post('/mcp')
    .set('x-operator-role', role)
    .send({ jsonrpc: '2.0', id: 1, method, params });
}

describe('strategy hot-swap integration', () => {
  let request;
  beforeAll(() => {
    jest.resetModules();
    const { createApp } = require('../server');
    request = supertest(createApp());
  });

  it('initialize declares listChanged: true', async () => {
    const res = await mcpCall(request, 'initialize', {});
    expect(res.body.result.capabilities.tools.listChanged).toBe(true);
  });

  it('tools/list returns 29 tools on composite (25 strategy + 4 infra/admin)', async () => {
    const res = await mcpCall(request, 'tools/list', {});
    expect(res.body.result.tools.length).toBe(29);
    const names = res.body.result.tools.map(t => t.name);
    expect(names).toContain('patient_summary');
    expect(names).toContain('admin_preview_strategy');
    expect(names).toContain('admin_set_strategy');
    expect(names).toContain('admin_rollback_strategy');
    expect(names).toContain('edge_health');
  });

  describe('two-step swap flow: composite → high-value', () => {
    let confirmationToken;

    it('step 1: preview returns diff and token', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'high-value' }
      });
      expect(res.body.error).toBeUndefined();
      const preview = JSON.parse(res.body.result.content[0].text);
      expect(preview.current).toBe('composite');
      expect(preview.target).toBe('high-value');
      expect(preview.added.length).toBeGreaterThan(0);
      expect(preview.removed.length).toBeGreaterThan(0);
      expect(preview.confirmationToken).toBeDefined();
      expect(preview.warning).toContain('Ephemeral');
      confirmationToken = preview.confirmationToken;
    });

    it('step 2: set strategy with token succeeds', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_set_strategy',
        arguments: { confirmationToken }
      });
      expect(res.body.error).toBeUndefined();
      const result = JSON.parse(res.body.result.content[0].text);
      expect(result.success).toBe(true);
      expect(result.previous).toBe('composite');
      expect(result.current).toBe('high-value');
      expect(result.listChanged).toBe(true);
      expect(result.toolCounts.previous).toBe(25);
      expect(result.toolCounts.current).toBe(15);
    });

    it('tools/list now reflects high-value strategy (15 + 4 = 19)', async () => {
      const res = await mcpCall(request, 'tools/list', {});
      expect(res.body.result.tools.length).toBe(19);
      const names = res.body.result.tools.map(t => t.name);
      // high-value tools present
      expect(names).toContain('patient_read');
      expect(names).toContain('observation_read');
      // composite-only tools absent
      expect(names).not.toContain('patient_summary');
      expect(names).not.toContain('cql_batch');
      // admin tools still present
      expect(names).toContain('admin_preview_strategy');
      expect(names).toContain('edge_health');
    });

    it('_meta.listChanged is consumed after first tools/list', async () => {
      const res = await mcpCall(request, 'tools/list', {});
      // Should not have _meta.listChanged since it was consumed in previous call
      expect(res.body.result._meta).toBeUndefined();
    });
  });

  describe('rollback flow', () => {
    it('rollback returns to composite', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_rollback_strategy',
        arguments: {}
      });
      expect(res.body.error).toBeUndefined();
      const result = JSON.parse(res.body.result.content[0].text);
      expect(result.success).toBe(true);
      expect(result.previous).toBe('high-value');
      expect(result.current).toBe('composite');
      expect(result.listChanged).toBe(true);
    });

    it('tools/list shows composite tools again (29)', async () => {
      const res = await mcpCall(request, 'tools/list', {});
      expect(res.body.result.tools.length).toBe(29);
      const names = res.body.result.tools.map(t => t.name);
      expect(names).toContain('patient_summary');
      expect(names).toContain('cql_batch');
    });
  });

  describe('RBAC enforcement', () => {
    it('clinician cannot preview strategy', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'high-value' }
      }, 'clinician');
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    });

    it('care_coordinator cannot set strategy', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_set_strategy',
        arguments: { confirmationToken: 'any' }
      }, 'care_coordinator');
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    });

    it('executive cannot rollback strategy', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_rollback_strategy',
        arguments: {}
      }, 'executive');
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    });
  });

  describe('error cases', () => {
    it('preview of disallowed strategy returns error', async () => {
      // Reset modules to get clean state with restricted allow-list
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'nonexistent' }
      });
      expect(res.body.error).toBeDefined();
    });

    it('set strategy with bad token returns error', async () => {
      // First preview to have a pending token
      await mcpCall(request, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'high-value' }
      });
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_set_strategy',
        arguments: { confirmationToken: 'invalid-token' }
      });
      expect(res.body.error).toBeDefined();
    });

    it('rollback without prior swap returns error', async () => {
      // We're on composite with previous=high-value from earlier test,
      // but if we rollback we'd toggle. Let's verify rollback works.
      // First swap away, then swap back, then try double rollback
      jest.resetModules();
      const { createApp } = require('../server');
      const freshRequest = supertest(createApp());
      const res = await mcpCall(freshRequest, 'tools/call', {
        name: 'admin_rollback_strategy',
        arguments: {}
      });
      expect(res.body.error).toBeDefined();
    });

    it('preview already-active strategy returns error', async () => {
      const res = await mcpCall(request, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'composite' }
      });
      expect(res.body.error).toBeDefined();
    });
  });

  describe('allow-list enforcement', () => {
    it('CLINICAL_ALLOWED_STRATEGIES restricts available strategies', () => {
      jest.resetModules();
      process.env.CLINICAL_ALLOWED_STRATEGIES = 'composite,high-value';
      const { createApp } = require('../server');
      const restrictedRequest = supertest(createApp());

      return mcpCall(restrictedRequest, 'tools/call', {
        name: 'admin_preview_strategy',
        arguments: { strategy: 'full-surface' }
      }).then((res) => {
        expect(res.body.error).toBeDefined();
        delete process.env.CLINICAL_ALLOWED_STRATEGIES;
      });
    });
  });
});
