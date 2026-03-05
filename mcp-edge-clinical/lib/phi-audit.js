// PHI-specific audit logger for clinical tool calls.
// Provides structured action types (PHI_ACCESS, PHI_WRITE, TOOL_CALL, AUTH_DENIED)
// on top of the generic pino audit logger. Ready for integration into mcp-router
// when per-tool PHI tracking is enabled (v0.2.0).
const { createAuditLogger } = require('hdim-mcp-edge-common');

function createPhiAuditLogger({ serviceName, stream } = {}) {
  const name = serviceName || 'mcp-edge-clinical';
  const pino = createAuditLogger({ serviceName: name, stream });

  function logToolAccess({ tool, role, tenantId, patientId, success, durationMs, phi, write }) {
    const action = !phi ? 'TOOL_CALL' : write ? 'PHI_WRITE' : 'PHI_ACCESS';
    const record = { action, source: name, tool, role, tenantId, success, durationMs, phi: !!phi };
    if (phi && patientId) record.patientId = patientId;
    pino.info(record);
  }

  function logAuthDenied({ tool, role }) {
    pino.warn({ action: 'AUTH_DENIED', source: name, tool, role });
  }

  function logStrategyChange({ previousStrategy, newStrategy, role, previousToolCount, newToolCount }) {
    pino.info({
      action: 'STRATEGY_CHANGED',
      source: name,
      previousStrategy,
      newStrategy,
      role,
      previousToolCount,
      newToolCount
    });
  }

  return { logToolAccess, logAuthDenied, logStrategyChange };
}

module.exports = { createPhiAuditLogger };
