const { definition } = require('../../lib/tools/admin-rollback-strategy');
const { StrategyManager } = require('../../lib/strategy-manager');

const stubClient = { baseUrl: 'http://localhost:18080', fetch: jest.fn() };

function createManager() {
  return new StrategyManager({
    baselineStrategy: 'composite',
    allowedStrategies: ['composite', 'high-value', 'full-surface'],
    client: stubClient
  });
}

describe('admin_rollback_strategy tool', () => {
  it('has correct name and schema', () => {
    expect(definition.name).toBe('admin_rollback_strategy');
    expect(definition.inputSchema.properties).toEqual({});
  });

  it('rolls back to previous strategy', async () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);

    const result = await definition.handler(
      {},
      { strategyManager: mgr, req: { headers: { 'x-operator-role': 'platform_admin' } } }
    );
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.success).toBe(true);
    expect(parsed.previous).toBe('high-value');
    expect(parsed.current).toBe('composite');
    expect(parsed.listChanged).toBe(true);
  });

  it('throws when no previous strategy', async () => {
    const mgr = createManager();
    await expect(
      definition.handler({}, { strategyManager: mgr, req: { headers: {} } })
    ).rejects.toThrow('No previous strategy');
  });

  it('logs strategy change via phiAuditLogger', async () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    mgr.executeSwap(preview.confirmationToken);
    const logStrategyChange = jest.fn();
    await definition.handler(
      {},
      {
        strategyManager: mgr,
        phiAuditLogger: { logStrategyChange },
        req: { headers: { 'x-mcp-role': 'platform_admin' } }
      }
    );
    expect(logStrategyChange).toHaveBeenCalledWith(
      expect.objectContaining({
        previousStrategy: 'high-value',
        newStrategy: 'composite',
        role: 'platform_admin'
      })
    );
  });
});
