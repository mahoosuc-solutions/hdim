const crypto = require('node:crypto');
const rateLimit = require('express-rate-limit');

function createRateLimiter(options = {}) {
  const windowMs = options.windowMs
    || Number(process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS)
    || 60_000;
  const max = options.max
    || Number(process.env.MCP_EDGE_RATE_LIMIT_MAX)
    || 100;
  const metrics = options.metrics || null;

  return rateLimit({
    windowMs,
    max,
    standardHeaders: true,
    legacyHeaders: false,
    skip: (req) => req.path === '/health' || req.path === '/metrics',
    keyGenerator: (req) => {
      const header = req.headers?.authorization || '';
      if (header.startsWith('Bearer ') && header.length > 7) {
        const token = header.slice(7).trim();
        return crypto.createHash('sha256').update(token).digest('hex').slice(0, 8);
      }
      return req.ip || 'unknown';
    },
    handler: (req, res) => {
      if (metrics) metrics.rateLimitCounter.inc();
      res.status(429).json({
        jsonrpc: '2.0',
        id: null,
        error: {
          code: -32000,
          message: 'Rate limit exceeded',
          data: { retryAfterMs: windowMs }
        }
      });
    }
  });
}

module.exports = { createRateLimiter };
