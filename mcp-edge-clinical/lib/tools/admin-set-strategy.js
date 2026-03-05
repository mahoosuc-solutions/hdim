const definition = {
  name: 'admin_set_strategy',
  description: 'Execute a strategy swap using a confirmation token from admin_preview_strategy. Ephemeral — restart returns to baseline.',
  inputSchema: {
    type: 'object',
    properties: {
      confirmationToken: {
        type: 'string',
        description: 'Token from admin_preview_strategy'
      }
    },
    required: ['confirmationToken'],
    additionalProperties: false
  },
  handler: async (args, { strategyManager, phiAuditLogger, req }) => {
    const result = strategyManager.executeSwap(args.confirmationToken);

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
