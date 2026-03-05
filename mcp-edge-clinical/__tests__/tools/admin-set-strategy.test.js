const { definition } = require('../../lib/tools/admin-set-strategy');
const { StrategyManager } = require('../../lib/strategy-manager');

const stubClient = { baseUrl: 'http://localhost:18080', fetch: jest.fn() };

function createManager() {
  return new StrategyManager({
    baselineStrategy: 'composite',
    allowedStrategies: ['composite', 'high-value', 'full-surface'],
    client: stubClient
  });
}

describe('admin_set_strategy tool', () => {
  it('has correct name and schema', () => {
    expect(definition.name).toBe('admin_set_strategy');
    expect(definition.inputSchema.required).toContain('confirmationToken');
  });

  it('executes swap with valid token', async () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    const result = await definition.handler(
      { confirmationToken: preview.confirmationToken },
      { strategyManager: mgr, req: { headers: { 'x-operator-role': 'platform_admin' } } }
    );
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.success).toBe(true);
    expect(parsed.previous).toBe('composite');
    expect(parsed.current).toBe('high-value');
    expect(parsed.listChanged).toBe(true);
  });

  it('throws for invalid token', async () => {
    const mgr = createManager();
    mgr.previewStrategy('high-value');
    await expect(
      definition.handler(
        { confirmationToken: 'bad-token' },
        { strategyManager: mgr, req: { headers: {} } }
      )
    ).rejects.toThrow('Invalid confirmation token');
  });

  it('logs strategy change via phiAuditLogger', async () => {
    const mgr = createManager();
    const preview = mgr.previewStrategy('high-value');
    const logStrategyChange = jest.fn();
    await definition.handler(
      { confirmationToken: preview.confirmationToken },
      {
        strategyManager: mgr,
        phiAuditLogger: { logStrategyChange },
        req: { headers: { 'x-operator-role': 'platform_admin' } }
      }
    );
    expect(logStrategyChange).toHaveBeenCalledWith(
      expect.objectContaining({
        previousStrategy: 'composite',
        newStrategy: 'high-value',
        role: 'platform_admin'
      })
    );
  });
});
