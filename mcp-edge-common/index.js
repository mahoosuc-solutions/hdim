// mcp-edge-common/index.js
module.exports = {
  ...require('./lib/jsonrpc'),
  ...require('./lib/auth'),
  ...require('./lib/health'),
  ...require('./lib/mcp-router'),
  ...require('./lib/demo-mode'),
  ...require('./lib/rate-limit'),
  ...require('./lib/cors-config'),
  ...require('./lib/audit-log'),
  ...require('./lib/param-validator'),
  ...require('./lib/metrics'),
  ...require('./lib/circuit-breaker')
};
