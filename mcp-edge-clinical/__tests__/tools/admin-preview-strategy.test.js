const { definition } = require('../../lib/tools/admin-preview-strategy');
const { StrategyManager } = require('../../lib/strategy-manager');

const stubClient = { baseUrl: 'http://localhost:18080', fetch: jest.fn() };

function createManager(overrides = {}) {
  return new StrategyManager({
    baselineStrategy: 'composite',
    allowedStrategies: ['composite', 'high-value', 'full-surface'],
    client: stubClient,
    ...overrides
  });
}

describe('admin_preview_strategy tool', () => {
  it('has correct name and schema', () => {
    expect(definition.name).toBe('admin_preview_strategy');
    expect(definition.inputSchema.required).toContain('strategy');
  });

  it('returns preview diff as JSON content', async () => {
    const mgr = createManager();
    const result = await definition.handler(
      { strategy: 'high-value' },
      { strategyManager: mgr }
    );
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.current).toBe('composite');
    expect(parsed.target).toBe('high-value');
    expect(parsed.confirmationToken).toBeDefined();
    expect(parsed.warning).toContain('Ephemeral');
  });

  it('throws for disallowed strategy', async () => {
    const mgr = createManager({ allowedStrategies: ['composite'] });
    await expect(
      definition.handler({ strategy: 'full-surface' }, { strategyManager: mgr })
    ).rejects.toThrow('not in allow-list');
  });

  it('throws when already on target', async () => {
    const mgr = createManager();
    await expect(
      definition.handler({ strategy: 'composite' }, { strategyManager: mgr })
    ).rejects.toThrow('Already on strategy');
  });
});
