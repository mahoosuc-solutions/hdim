# hdim-mcp-edge-common

Shared library for all HDIM MCP edge sidecars. Provides the MCP JSON-RPC 2.0 router, RBAC authorization, demo mode, rate limiting, CORS, PHI-safe audit logging, and parameter validation.

## Modules

| Module | Export | Purpose |
|--------|--------|---------|
| `jsonrpc` | `jsonRpcResult`, `jsonRpcError` | JSON-RPC 2.0 response builders |
| `auth` | `extractOperatorRole`, `authorizeToolCall` | RBAC role extraction and authorization |
| `health` | `createHealthRouter` | `/health` endpoint for Docker healthchecks |
| `mcp-router` | `createMcpRouter` | Express router handling `initialize`, `tools/list`, `tools/call` |
| `demo-mode` | `isDemoMode`, `loadFixture` | Fixture-based demo responses |
| `rate-limit` | `createRateLimiter` | Express rate limiter (100 req/15min) |
| `cors-config` | `createCorsOptions` | CORS configuration |
| `audit-log` | `createAuditLogger`, `scrubSensitive` | Pino structured logger with PHI scrubbing |
| `param-validator` | `validateToolParams` | AJV-based JSON Schema validation |

## Usage

```js
const { createMcpRouter, createHealthRouter, createAuditLogger } = require('hdim-mcp-edge-common');
```

## Testing

```bash
npm test
```
