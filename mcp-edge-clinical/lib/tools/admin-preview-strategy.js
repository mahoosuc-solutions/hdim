const definition = {
  name: 'admin_preview_strategy',
  description: 'Preview the diff of switching to a different clinical tool strategy. Returns added/removed tools and a confirmation token for the swap.',
  inputSchema: {
    type: 'object',
    properties: {
      strategy: {
        type: 'string',
        description: 'Target strategy name (composite, high-value, or full-surface)'
      }
    },
    required: ['strategy'],
    additionalProperties: false
  },
  handler: async (args, { strategyManager }) => {
    const result = strategyManager.previewStrategy(args.strategy);
    return {
      content: [{ type: 'text', text: JSON.stringify(result, null, 2) }]
    };
  }
};

module.exports = { definition };
