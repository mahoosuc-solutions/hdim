const crypto = require('node:crypto');
const path = require('node:path');

const VALID_STRATEGIES = ['composite', 'high-value', 'full-surface'];
const TOKEN_TTL_MS = 60_000;

class StrategyManager {
  constructor({ baselineStrategy, allowedStrategies, client, logger, phiAuditLogger }) {
    if (!VALID_STRATEGIES.includes(baselineStrategy)) {
      throw new Error(`Unknown clinical tool strategy: "${baselineStrategy}". Valid: ${VALID_STRATEGIES.join(', ')}`);
    }
    this._baselineStrategy = baselineStrategy;
    this._allowedStrategies = allowedStrategies || VALID_STRATEGIES;
    this._client = client;
    this._logger = logger;
    this._phiAuditLogger = phiAuditLogger;

    this._currentStrategyName = baselineStrategy;
    this._previousStrategyName = null;
    this._strategyTools = this._loadStrategyTools(baselineStrategy);
    this._rolePolicies = this._loadRolePolicies(baselineStrategy);
    this._adminTools = [];
    this._pendingToken = null;
    this._listChangedFlag = false;
  }

  // --- Read accessors (router calls these per-request) ---

  get strategyName() {
    return this._currentStrategyName;
  }

  get tools() {
    return [...this._strategyTools, ...this._adminTools];
  }

  get toolMap() {
    return new Map(this.tools.map((t) => [t.name, t]));
  }

  get rolePolicies() {
    return this._rolePolicies;
  }

  get fixturesDir() {
    return path.join(__dirname, 'strategies', this._currentStrategyName, 'fixtures');
  }

  get baselineStrategy() {
    return this._baselineStrategy;
  }

  // --- Admin operations ---

  registerAdminTools(tools) {
    this._adminTools = tools;
  }

  previewStrategy(target) {
    if (!this._allowedStrategies.includes(target)) {
      throw new Error(
        `Strategy "${target}" not in allow-list. Allowed: ${this._allowedStrategies.join(', ')}`
      );
    }
    if (target === this._currentStrategyName) {
      throw new Error(`Already on strategy "${target}"`);
    }

    const targetTools = this._loadStrategyTools(target);
    const targetPolicies = this._loadRolePolicies(target);
    const currentNames = new Set(this._strategyTools.map((t) => t.name));
    const targetNames = new Set(targetTools.map((t) => t.name));

    const added = [...targetNames].filter((n) => !currentNames.has(n));
    const removed = [...currentNames].filter((n) => !targetNames.has(n));
    const unchanged = [...currentNames].filter((n) => targetNames.has(n));

    const policyChanges = this._diffPolicies(this._rolePolicies, targetPolicies);

    const token = crypto.randomBytes(16).toString('hex');
    const expiresAt = new Date(Date.now() + TOKEN_TTL_MS).toISOString();

    // Single pending token — new preview replaces any previous
    this._pendingToken = {
      token,
      target,
      targetTools,
      targetPolicies,
      expiresAt: Date.now() + TOKEN_TTL_MS,
      consumed: false
    };

    return {
      current: this._currentStrategyName,
      target,
      added,
      removed,
      unchanged,
      policyChanges,
      confirmationToken: token,
      expiresAt,
      warning: `Ephemeral swap. Restart baseline: ${this._baselineStrategy}`
    };
  }

  executeSwap(token) {
    const pending = this._pendingToken;

    if (!pending || pending.consumed) {
      throw new Error('No pending swap token');
    }

    // Consume on any attempt (success or failure)
    pending.consumed = true;

    if (pending.token !== token) {
      throw new Error('Invalid confirmation token');
    }
    if (Date.now() > pending.expiresAt) {
      throw new Error('Confirmation token expired');
    }

    const previous = this._currentStrategyName;
    const previousToolCount = this._strategyTools.length;

    // Atomic swap (synchronous property assignment)
    this._previousStrategyName = previous;
    this._currentStrategyName = pending.target;
    this._strategyTools = pending.targetTools;
    this._rolePolicies = pending.targetPolicies;
    this._listChangedFlag = true;

    return {
      success: true,
      previous,
      current: this._currentStrategyName,
      toolCounts: {
        previous: previousToolCount,
        current: this._strategyTools.length
      }
    };
  }

  rollback() {
    if (!this._previousStrategyName) {
      throw new Error('No previous strategy to rollback to');
    }
    if (this._previousStrategyName === this._currentStrategyName) {
      throw new Error('Already on previous strategy');
    }

    const target = this._previousStrategyName;
    const previous = this._currentStrategyName;
    const previousToolCount = this._strategyTools.length;

    const targetTools = this._loadStrategyTools(target);
    const targetPolicies = this._loadRolePolicies(target);

    this._previousStrategyName = previous;
    this._currentStrategyName = target;
    this._strategyTools = targetTools;
    this._rolePolicies = targetPolicies;
    this._listChangedFlag = true;

    // Invalidate any pending token
    if (this._pendingToken) {
      this._pendingToken.consumed = true;
    }

    return {
      success: true,
      previous,
      current: target,
      toolCounts: {
        previous: previousToolCount,
        current: targetTools.length
      }
    };
  }

  consumeListChangedFlag() {
    if (this._listChangedFlag) {
      this._listChangedFlag = false;
      return true;
    }
    return false;
  }

  // --- Internal helpers ---

  _loadStrategyTools(strategyName) {
    const strategy = require(`./strategies/${strategyName}`);
    return strategy.loadTools(this._client);
  }

  _loadRolePolicies(strategyName) {
    try {
      const { clinicalRolePolicies } = require(`./strategies/${strategyName}/role-policies`);
      return clinicalRolePolicies();
    } catch {
      return undefined;
    }
  }

  _diffPolicies(currentPolicies, targetPolicies) {
    if (!currentPolicies && !targetPolicies) return {};
    const current = currentPolicies || {};
    const target = targetPolicies || {};
    const allRoles = new Set([...Object.keys(current), ...Object.keys(target)]);
    const changes = {};
    for (const role of allRoles) {
      const cStr = (current[role] || []).map(String).join(',');
      const tStr = (target[role] || []).map(String).join(',');
      if (cStr !== tStr) {
        changes[role] = { from: cStr || '(none)', to: tStr || '(none)' };
      }
    }
    return changes;
  }
}

module.exports = { StrategyManager, VALID_STRATEGIES };
