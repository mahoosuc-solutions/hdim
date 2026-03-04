const rateLimit = require('express-rate-limit');

function createRateLimiter(options = {}) {
  const windowMs = options.windowMs
    || Number(process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS)
    || 60_000;
  const max = options.max
    || Number(process.env.MCP_EDGE_RATE_LIMIT_MAX)
    || 100;

  return rateLimit({
    windowMs,
    max,
    standardHeaders: true,
    legacyHeaders: false,
    skip: (req) => req.path === '/health',
    message: {
      jsonrpc: '2.0',
      id: null,
      error: {
        code: -32000,
        message: 'Rate limit exceeded',
        data: { retryAfterMs: windowMs }
      }
    }
  });
}

module.exports = { createRateLimiter };
