const definition = {
  name: 'admin_rollback_strategy',
  description: 'Rollback to the previous clinical tool strategy. No confirmation token needed.',
  inputSchema: {
    type: 'object',
    properties: {},
    additionalProperties: false
  },
  handler: async (_args, { strategyManager, phiAuditLogger, req }) => {
    const result = strategyManager.rollback();

    if (phiAuditLogger) {
      const role = req?.headers?.['x-operator-role'] || req?.headers?.['x-mcp-role'] || 'unknown';
      phiAuditLogger.logStrategyChange({
        previousStrategy: result.previous,
        newStrategy: result.current,
        role,
        previousToolCount: result.toolCounts.previous,
        newToolCount: result.toolCounts.current
      });
    }

    return {
      content: [{ type: 'text', text: JSON.stringify({ ...result, listChanged: true }, null, 2) }]
    };
  }
};

module.exports = { definition };
