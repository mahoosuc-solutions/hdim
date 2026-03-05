const path = require('node:path');

// Stub client — strategy loadTools receives this
const stubClient = { baseUrl: 'http://localhost:18080', fetch: jest.fn() };

// We need to clear module cache between tests to avoid stale strategy loads
beforeEach(() => {
  jest.restoreAllMocks();
});

function createManager(overrides = {}) {
  // Fresh require to avoid cached state
  const { StrategyManager } = require('../lib/strategy-manager');
  return new StrategyManager({
    baselineStrategy: 'composite',
    allowedStrategies: ['composite', 'high-value', 'full-surface'],
    client: stubClient,
    logger: null,
    phiAuditLogger: null,
    ...overrides
  });
}

describe('StrategyManager — constructor', () => {
  it('initializes with baseline strategy', () => {
    const mgr = createManager();
    expect(mgr.strategyName).toBe('composite');
    expect(mgr.baselineStrategy).toBe('composite');
  });

  it('loads tools from baseline strategy', () => {
    const mgr = createManager();
    const tools = mgr.tools;
    expect(tools.length).toBeGreaterThan(0);
    // Composite has 25 tools
    expect(tools.some((t) => t.name === 'patient_summary')).toBe(true);
  });

  it('loads role policies from baseline strategy', () => {
    const mgr = createManager();
    expect(mgr.rolePolicies).toBeDefined();
    expect(mgr.rolePolicies.platform_admin).toBeDefined();
  });

  it('fixturesDir points to current strategy', () => {
    const mgr = createManager();
    expect(mgr.fixturesDir).toContain(path.join('strategies', 'composite', 'fixtures'));
  });
});

describe('StrategyManager — registerAdminTools', () => {
  it('admin tools appear in tools list', () => {
    const mgr = createManager();
    const baseCount = mgr.tools.length;
    mgr.registerAdminTools([
      { name: 'admin_preview_strategy', handler: jest.fn() },
      { name: 'edge_health', handler: jest.fn() }
    ]);
    expect(mgr.tools.length).toBe(baseCount + 2);
    expect(mgr.toolMap.has('admin_preview_strategy')).toBe(true);
  });

  it('admin tools survive strategy swap', () => {
    const mgr = createManager();
    mgr.registerAdminTools([{ name: 'admin_tool', handler: jest.fn() }]);
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    expect(mgr.toolMap.has('admin_tool')).toBe(true);
  });
});

describe('StrategyManager — previewStrategy', () => {
  it('returns diff between current and target strategy', () => {
    const mgr = createManager();
    const result = mgr.previewStrategy('high-value');
    expect(result.current).toBe('composite');
    expect(result.target).toBe('high-value');
    expect(Array.isArray(result.added)).toBe(true);
    expect(Array.isArray(result.removed)).toBe(true);
    expect(Array.isArray(result.unchanged)).toBe(true);
    expect(result.confirmationToken).toBeDefined();
    expect(result.expiresAt).toBeDefined();
    expect(result.warning).toContain('Ephemeral swap');
    expect(result.warning).toContain('composite');
  });

  it('throws for disallowed strategy', () => {
    const mgr = createManager({ allowedStrategies: ['composite'] });
    expect(() => mgr.previewStrategy('high-value')).toThrow('not in allow-list');
  });

  it('throws when already on target strategy', () => {
    const mgr = createManager();
    expect(() => mgr.previewStrategy('composite')).toThrow('Already on strategy');
  });

  it('new preview replaces previous pending token', () => {
    const mgr = createManager();
    const first = mgr.previewStrategy('high-value');
    const second = mgr.previewStrategy('high-value');
    expect(second.confirmationToken).not.toBe(first.confirmationToken);
    // First token should no longer work
    expect(() => mgr.executeSwap(first.confirmationToken)).toThrow('Invalid confirmation token');
  });
});

describe('StrategyManager — executeSwap', () => {
  it('swaps strategy with valid token', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    const result = mgr.executeSwap(preview.confirmationToken);
    expect(result.success).toBe(true);
    expect(result.previous).toBe('composite');
    expect(result.current).toBe('high-value');
    expect(result.toolCounts.previous).toBe(25);
    expect(result.toolCounts.current).toBe(15);
    expect(mgr.strategyName).toBe('high-value');
  });

  it('throws for invalid token', () => {
    const mgr = createManager();
    mgr.previewStrategy('high-value');
    expect(() => mgr.executeSwap('wrong-token')).toThrow('Invalid confirmation token');
  });

  it('throws when no pending swap', () => {
    const mgr = createManager();
    expect(() => mgr.executeSwap('any-token')).toThrow('No pending swap token');
  });

  it('token is single-use — cannot reuse', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    // Swap back to composite to allow re-preview
    mgr.rollback();
    // Try to reuse old token
    expect(() => mgr.executeSwap(preview.confirmationToken)).toThrow('No pending swap token');
  });

  it('consumes token on failed attempt', () => {
    const mgr = createManager();
    mgr.previewStrategy('high-value');
    // Wrong token consumes the pending
    expect(() => mgr.executeSwap('bad-token')).toThrow('Invalid confirmation token');
    expect(() => mgr.executeSwap('anything')).toThrow('No pending swap token');
  });

  it('throws for expired token', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    // Manually expire the token
    mgr._pendingToken.expiresAt = Date.now() - 1;
    expect(() => mgr.executeSwap(preview.confirmationToken)).toThrow('token expired');
  });

  it('sets listChanged flag after swap', () => {
    const mgr = createManager();
    expect(mgr.consumeListChangedFlag()).toBe(false);
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    expect(mgr.consumeListChangedFlag()).toBe(true);
    expect(mgr.consumeListChangedFlag()).toBe(false); // consumed
  });
});

describe('StrategyManager — rollback', () => {
  it('rolls back to previous strategy', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    expect(mgr.strategyName).toBe('high-value');
    const result = mgr.rollback();
    expect(result.success).toBe(true);
    expect(result.previous).toBe('high-value');
    expect(result.current).toBe('composite');
    expect(mgr.strategyName).toBe('composite');
  });

  it('throws when no previous strategy', () => {
    const mgr = createManager();
    expect(() => mgr.rollback()).toThrow('No previous strategy');
  });

  it('double rollback toggles back and forth', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    mgr.rollback();
    expect(mgr.strategyName).toBe('composite');
    // Second rollback goes back to high-value (toggle behavior)
    mgr.rollback();
    expect(mgr.strategyName).toBe('high-value');
  });

  it('invalidates pending token on rollback', () => {
    const mgr = createManager();
    // Swap to high-value
    const p1 = mgr.previewStrategy('high-value');
    mgr.executeSwap(p1.confirmationToken);
    // Preview full-surface but rollback before confirming
    const p2 = mgr.previewStrategy('full-surface');
    mgr.rollback();
    // full-surface token should be consumed
    expect(() => mgr.executeSwap(p2.confirmationToken)).toThrow('No pending swap token');
  });

  it('sets listChanged flag after rollback', () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    mgr.consumeListChangedFlag(); // clear
    mgr.rollback();
    expect(mgr.consumeListChangedFlag()).toBe(true);
  });
});

describe('StrategyManager — toolMap', () => {
  it('returns Map of all tools including admin', () => {
    const mgr = createManager();
    mgr.registerAdminTools([{ name: 'edge_health', handler: jest.fn() }]);
    const map = mgr.toolMap;
    expect(map).toBeInstanceOf(Map);
    expect(map.has('patient_summary')).toBe(true);
    expect(map.has('edge_health')).toBe(true);
  });
});

describe('StrategyManager — fixturesDir updates on swap', () => {
  it('fixturesDir reflects current strategy', () => {
    const mgr = createManager();
    expect(mgr.fixturesDir).toContain('composite');
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    expect(mgr.fixturesDir).toContain('high-value');
  });
});
