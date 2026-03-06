# HDIM MCP Edge — Sidecar Implementation Plan

> **Status:** COMPLETE

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build two isolated MCP edge sidecars (platform + devops) with stdio bridges for Claude Desktop integration, using the west-bethel-motel pattern with CISO-required security boundaries.

**Architecture:** Three npm packages — `mcp-edge-common` (shared lib), `mcp-edge-platform` (port 3100, gateway proxy), `mcp-edge-devops` (port 3200, Docker/NX ops). Each sidecar is an Express HTTP server exposing JSON-RPC tools. Stdio bridges convert Claude Desktop stdio to HTTP. API key auth with role-based tool filtering.

**Tech Stack:** Node.js 20+, Express 4, Jest 30, better-sqlite3 (future), helmet, cors

**Reference Implementation:** `/mnt/wdblack/dev/projects/west-bethel-motel-booking-system/mcp-edge/`

---

## Parallel Execution Map

Tasks are grouped into parallel streams. Tasks within a stream are sequential.
Streams can run concurrently via multi-agent teams.

```
Stream A (mcp-edge-common):   Task 1 → 2 → 3 → 4 → 5 → 6
Stream B (mcp-edge-platform): Task 7 → 8 → 9 → 10 → 11 → 12 → 13
Stream C (mcp-edge-devops):   Task 14 → 15 → 16 → 17 → 18 → 19
Stream D (bridges + config):  Task 20 → 21 → 22 → 23

Dependencies:
  - Stream B depends on Stream A completing Task 1-5
  - Stream C depends on Stream A completing Task 1-5
  - Stream D depends on Stream B Task 7 and Stream C Task 14
```

---

## Stream A: mcp-edge-common (Shared Library)

### Task 1: Scaffold mcp-edge-common package

**Files:**
- Create: `mcp-edge-common/package.json`
- Create: `mcp-edge-common/lib/jsonrpc.js`
- Create: `mcp-edge-common/__tests__/jsonrpc.test.js`

**Step 1: Create package.json**

```json
{
  "name": "hdim-mcp-edge-common",
  "version": "0.1.0",
  "description": "Shared library for HDIM MCP edge sidecars",
  "main": "index.js",
  "scripts": {
    "test": "jest"
  },
  "engines": { "node": ">=20.0.0" },
  "devDependencies": {
    "jest": "^30.0.0"
  }
}
```

**Step 2: Write failing test for jsonrpc helpers**

```javascript
// mcp-edge-common/__tests__/jsonrpc.test.js
const { jsonRpcResult, jsonRpcError, parseJsonRpcRequest } = require('../lib/jsonrpc');

describe('jsonrpc', () => {
  describe('jsonRpcResult', () => {
    it('wraps a result with jsonrpc 2.0 envelope', () => {
      const result = jsonRpcResult(42, { tools: [] });
      expect(result).toEqual({ jsonrpc: '2.0', id: 42, result: { tools: [] } });
    });

    it('handles null id', () => {
      const result = jsonRpcResult(null, 'ok');
      expect(result).toEqual({ jsonrpc: '2.0', id: null, result: 'ok' });
    });
  });

  describe('jsonRpcError', () => {
    it('wraps an error with code and message', () => {
      const err = jsonRpcError(1, -32601, 'Method not found');
      expect(err).toEqual({
        jsonrpc: '2.0',
        id: 1,
        error: { code: -32601, message: 'Method not found' }
      });
    });

    it('includes optional data field', () => {
      const err = jsonRpcError(1, -32603, 'Internal error', { detail: 'boom' });
      expect(err.error.data).toEqual({ detail: 'boom' });
    });
  });

  describe('parseJsonRpcRequest', () => {
    it('parses valid JSON-RPC request', () => {
      const raw = '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}';
      const parsed = parseJsonRpcRequest(raw);
      expect(parsed.method).toBe('tools/list');
      expect(parsed.id).toBe(1);
    });

    it('returns null for invalid JSON', () => {
      expect(parseJsonRpcRequest('not json')).toBeNull();
    });

    it('returns null for empty string', () => {
      expect(parseJsonRpcRequest('')).toBeNull();
    });
  });
});
```

**Step 3: Run test to verify it fails**

Run: `cd mcp-edge-common && npm install && npx jest __tests__/jsonrpc.test.js -v`
Expected: FAIL — `Cannot find module '../lib/jsonrpc'`

**Step 4: Implement jsonrpc.js**

```javascript
// mcp-edge-common/lib/jsonrpc.js
function jsonRpcResult(id, result) {
  return { jsonrpc: '2.0', id, result };
}

function jsonRpcError(id, code, message, data) {
  const error = { code, message };
  if (data !== undefined) error.data = data;
  return { jsonrpc: '2.0', id, error };
}

function parseJsonRpcRequest(raw) {
  if (!raw || typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  if (!trimmed.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(trimmed);
    if (parsed.jsonrpc !== '2.0' || !parsed.method) return null;
    return parsed;
  } catch {
    return null;
  }
}

module.exports = { jsonRpcResult, jsonRpcError, parseJsonRpcRequest };
```

**Step 5: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/jsonrpc.test.js -v`
Expected: PASS (3 describe blocks, 6 tests)

**Step 6: Commit**

```bash
git add mcp-edge-common/
git commit -m "feat(mcp-edge): scaffold mcp-edge-common with jsonrpc helpers"
```

---

### Task 2: Auth module — API key validation and role extraction

**Files:**
- Create: `mcp-edge-common/lib/auth.js`
- Create: `mcp-edge-common/__tests__/auth.test.js`

**Step 1: Write failing test**

```javascript
// mcp-edge-common/__tests__/auth.test.js
const { extractApiKey, extractOperatorRole, authorizeToolCall, rolePolicies } = require('../lib/auth');

describe('auth', () => {
  describe('extractApiKey', () => {
    it('extracts Bearer token from authorization header', () => {
      const req = { headers: { authorization: 'Bearer hdim_abc123' } };
      expect(extractApiKey(req)).toBe('hdim_abc123');
    });

    it('returns null when no authorization header', () => {
      expect(extractApiKey({ headers: {} })).toBeNull();
    });

    it('returns null for non-Bearer scheme', () => {
      const req = { headers: { authorization: 'Basic abc' } };
      expect(extractApiKey(req)).toBeNull();
    });
  });

  describe('extractOperatorRole', () => {
    it('extracts from x-operator-role header', () => {
      const req = { headers: { 'x-operator-role': 'platform_admin' } };
      expect(extractOperatorRole(req)).toBe('platform_admin');
    });

    it('normalizes to lowercase', () => {
      const req = { headers: { 'x-operator-role': 'DEVELOPER' } };
      expect(extractOperatorRole(req)).toBe('developer');
    });

    it('returns null when missing', () => {
      expect(extractOperatorRole({ headers: {} })).toBeNull();
    });
  });

  describe('authorizeToolCall', () => {
    it('allows platform_admin to call any tool', () => {
      const result = authorizeToolCall({ toolName: 'docker_restart', role: 'platform_admin', enforce: true });
      expect(result.allowed).toBe(true);
    });

    it('blocks unknown roles', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: 'hacker', enforce: true });
      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('unknown_role');
    });

    it('allows all tools when enforcement is disabled', () => {
      const result = authorizeToolCall({ toolName: 'anything', role: null, enforce: false });
      expect(result.allowed).toBe(true);
      expect(result.reason).toBe('role_auth_disabled');
    });

    it('blocks missing role when enforced', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: null, enforce: true });
      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('missing_operator_role');
    });

    it('allows developer to call edge_health', () => {
      const result = authorizeToolCall({ toolName: 'edge_health', role: 'developer', enforce: true });
      expect(result.allowed).toBe(true);
    });

    it('blocks executive from devops tools', () => {
      const result = authorizeToolCall({ toolName: 'docker_restart', role: 'executive', enforce: true });
      expect(result.allowed).toBe(false);
    });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-common && npx jest __tests__/auth.test.js -v`
Expected: FAIL — `Cannot find module '../lib/auth'`

**Step 3: Implement auth.js**

```javascript
// mcp-edge-common/lib/auth.js
function normalizeToolName(name) {
  return String(name || '').trim().replace(/\./g, '_');
}

function extractApiKey(req) {
  const header = req?.headers?.authorization || '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice(7).trim();
  return token || null;
}

function extractOperatorRole(req) {
  const headers = req?.headers || {};
  const role = headers['x-operator-role'] || headers['x-mcp-role'] || null;
  return role ? String(role).trim().toLowerCase() : null;
}

function rolePolicies() {
  return {
    platform_admin: [/./],
    developer: [/./],
    clinical_admin: [/^(edge_health|platform_health|dashboard_stats|platform_info)/],
    quality_officer: [/^(edge_health|dashboard_stats|platform_info)/],
    executive: [/^(edge_health|dashboard_stats|platform_info)/],
    clinician: [/^(edge_health|platform_health)/],
    care_coordinator: [/^(edge_health)/]
  };
}

function authorizeToolCall({ toolName, role, enforce }) {
  const normalized = normalizeToolName(toolName);

  if (!enforce) {
    return { allowed: true, normalized, reason: 'role_auth_disabled' };
  }

  if (!role) {
    return { allowed: false, normalized, reason: 'missing_operator_role' };
  }

  const policies = rolePolicies();
  const matchers = policies[role];
  if (!matchers || matchers.length === 0) {
    return { allowed: false, normalized, reason: 'unknown_role' };
  }

  const allowed = matchers.some((m) => m.test(normalized));
  return { allowed, normalized, reason: allowed ? 'allowed' : 'forbidden_for_role' };
}

module.exports = {
  normalizeToolName,
  extractApiKey,
  extractOperatorRole,
  rolePolicies,
  authorizeToolCall
};
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/auth.test.js -v`
Expected: PASS (3 describe blocks, 9 tests)

**Step 5: Commit**

```bash
git add mcp-edge-common/lib/auth.js mcp-edge-common/__tests__/auth.test.js
git commit -m "feat(mcp-edge): add auth module with API key extraction and role-based tool filtering"
```

---

### Task 3: Health endpoint factory

**Files:**
- Create: `mcp-edge-common/lib/health.js`
- Create: `mcp-edge-common/__tests__/health.test.js`

**Step 1: Write failing test**

```javascript
// mcp-edge-common/__tests__/health.test.js
const { createHealthRouter } = require('../lib/health');

describe('health', () => {
  describe('createHealthRouter', () => {
    it('returns an express router', () => {
      const router = createHealthRouter({ serviceName: 'test-edge', version: '0.1.0' });
      expect(router).toBeDefined();
      expect(typeof router).toBe('function');
    });
  });
});
```

Note: Full integration test in Task 8 (platform sidecar). Unit test here just verifies the factory.

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-common && npx jest __tests__/health.test.js -v`
Expected: FAIL — `Cannot find module '../lib/health'`

**Step 3: Implement health.js**

```javascript
// mcp-edge-common/lib/health.js
const express = require('express');

function createHealthRouter({ serviceName, version, statusProvider }) {
  const router = express.Router();
  const startTime = Date.now();

  function buildStatus() {
    const base = {
      status: 'healthy',
      service: serviceName,
      version,
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString()
    };
    if (statusProvider) {
      return { ...base, ...statusProvider() };
    }
    return base;
  }

  router.get('/health', (req, res) => {
    res.json(buildStatus());
  });

  return router;
}

module.exports = { createHealthRouter };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/health.test.js -v`
Expected: PASS

**Step 5: Commit**

```bash
git add mcp-edge-common/lib/health.js mcp-edge-common/__tests__/health.test.js
git commit -m "feat(mcp-edge): add health endpoint factory"
```

---

### Task 4: Base MCP router (JSON-RPC dispatcher)

**Files:**
- Create: `mcp-edge-common/lib/mcp-router.js`
- Create: `mcp-edge-common/__tests__/mcp-router.test.js`

**Step 1: Write failing test**

```javascript
// mcp-edge-common/__tests__/mcp-router.test.js
const express = require('express');
const { createMcpRouter } = require('../lib/mcp-router');

// Minimal test tools
const testTools = [
  {
    name: 'test_tool',
    description: 'A test tool',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => ({ content: [{ type: 'text', text: 'hello' }] })
  }
];

describe('mcp-router', () => {
  let app;
  let request;

  beforeAll(async () => {
    const supertest = (await import('supertest')).default;
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: testTools,
      serverName: 'test-edge',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('responds to initialize', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('test-edge');
    expect(res.body.result.capabilities.tools).toBeDefined();
  });

  it('lists tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/list', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.tools).toHaveLength(1);
    expect(res.body.result.tools[0].name).toBe('test_tool');
  });

  it('calls a tool', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'test_tool', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.result.content[0].text).toBe('hello');
  });

  it('returns method not found for unknown method', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'unknown/method', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32601);
  });

  it('returns tool not found for unknown tool', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 5, method: 'tools/call',
      params: { name: 'nonexistent', arguments: {} }
    });
    expect(res.status).toBe(200);
    expect(res.body.error.code).toBe(-32602);
  });

  it('handles notifications (no id) with 204', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/initialized', params: {}
    });
    expect(res.status).toBe(204);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-common && npm install express supertest && npx jest __tests__/mcp-router.test.js -v`
Expected: FAIL — `Cannot find module '../lib/mcp-router'`

**Step 3: Implement mcp-router.js**

```javascript
// mcp-edge-common/lib/mcp-router.js
const express = require('express');
const { jsonRpcResult, jsonRpcError } = require('./jsonrpc');
const { extractOperatorRole, authorizeToolCall } = require('./auth');

const MCP_PROTOCOL_VERSION = '2025-11-25';

function createMcpRouter({ tools, serverName, serverVersion, enforceRoleAuth = true }) {
  const router = express.Router();
  const toolMap = new Map(tools.map((t) => [t.name, t]));

  function handleInitialize(id) {
    return jsonRpcResult(id, {
      protocolVersion: MCP_PROTOCOL_VERSION,
      serverInfo: { name: serverName, version: serverVersion },
      capabilities: { tools: { listChanged: false } }
    });
  }

  function handleToolsList(id) {
    const toolDefs = tools.map(({ name, description, inputSchema }) => ({
      name, description, inputSchema
    }));
    return jsonRpcResult(id, { tools: toolDefs });
  }

  async function handleToolsCall(id, params, req) {
    const { name, arguments: args } = params || {};
    const tool = toolMap.get(name);
    if (!tool) {
      return jsonRpcError(id, -32602, `Tool not found: ${name}`);
    }

    const role = extractOperatorRole(req);
    const authResult = authorizeToolCall({
      toolName: name, role, enforce: enforceRoleAuth
    });
    if (!authResult.allowed) {
      return jsonRpcError(id, -32603, `Forbidden: ${authResult.reason}`, {
        tool: name, role, reason: authResult.reason
      });
    }

    try {
      const result = await tool.handler(args || {}, { req });
      return jsonRpcResult(id, result);
    } catch (err) {
      return jsonRpcError(id, -32603, 'Tool execution error', {
        tool: name, detail: err?.message || String(err)
      });
    }
  }

  router.post('/mcp', async (req, res) => {
    const { jsonrpc, id, method, params } = req.body || {};

    if (jsonrpc !== '2.0') {
      return res.json(jsonRpcError(id ?? null, -32600, 'Invalid JSON-RPC version'));
    }

    // Notifications (no id) get 204
    if (id === undefined || id === null) {
      if (method === 'notifications/initialized' || method === 'notifications/cancelled') {
        return res.status(204).end();
      }
    }

    let response;
    switch (method) {
      case 'initialize':
        response = handleInitialize(id);
        break;
      case 'tools/list':
        response = handleToolsList(id);
        break;
      case 'tools/call':
        response = await handleToolsCall(id, params, req);
        break;
      default:
        response = jsonRpcError(id, -32601, `Method not found: ${method}`);
    }

    res.json(response);
  });

  return router;
}

module.exports = { createMcpRouter };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/mcp-router.test.js -v`
Expected: PASS (6 tests)

**Step 5: Commit**

```bash
git add mcp-edge-common/lib/mcp-router.js mcp-edge-common/__tests__/mcp-router.test.js
git commit -m "feat(mcp-edge): add base MCP JSON-RPC router with tool dispatch and role auth"
```

---

### Task 5: Demo mode middleware

**Files:**
- Create: `mcp-edge-common/lib/demo-mode.js`
- Create: `mcp-edge-common/__tests__/demo-mode.test.js`

**Step 1: Write failing test**

```javascript
// mcp-edge-common/__tests__/demo-mode.test.js
const { isDemoMode, loadFixture, createDemoInterceptor } = require('../lib/demo-mode');

describe('demo-mode', () => {
  const originalEnv = process.env.HDIM_DEMO_MODE;

  afterEach(() => {
    if (originalEnv === undefined) delete process.env.HDIM_DEMO_MODE;
    else process.env.HDIM_DEMO_MODE = originalEnv;
  });

  describe('isDemoMode', () => {
    it('returns false by default', () => {
      delete process.env.HDIM_DEMO_MODE;
      expect(isDemoMode()).toBe(false);
    });

    it('returns true when env is set', () => {
      process.env.HDIM_DEMO_MODE = 'true';
      expect(isDemoMode()).toBe(true);
    });
  });

  describe('loadFixture', () => {
    it('returns null for unknown fixture', () => {
      expect(loadFixture('/nonexistent', 'no_tool')).toBeNull();
    });
  });

  describe('createDemoInterceptor', () => {
    it('returns a function', () => {
      const interceptor = createDemoInterceptor('/nonexistent');
      expect(typeof interceptor).toBe('function');
    });

    it('passes through when not in demo mode', async () => {
      delete process.env.HDIM_DEMO_MODE;
      const interceptor = createDemoInterceptor('/nonexistent');
      const handler = async () => ({ content: [{ type: 'text', text: 'real' }] });
      const result = await interceptor('some_tool', {}, handler);
      expect(result.content[0].text).toBe('real');
    });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-common && npx jest __tests__/demo-mode.test.js -v`
Expected: FAIL — `Cannot find module '../lib/demo-mode'`

**Step 3: Implement demo-mode.js**

```javascript
// mcp-edge-common/lib/demo-mode.js
const fs = require('node:fs');
const path = require('node:path');

function isDemoMode() {
  return ['1', 'true', 'yes'].includes(
    String(process.env.HDIM_DEMO_MODE || '').trim().toLowerCase()
  );
}

function loadFixture(fixturesDir, toolName) {
  const filePath = path.join(fixturesDir, `${toolName}.json`);
  try {
    const raw = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function createDemoInterceptor(fixturesDir) {
  return async function intercept(toolName, args, realHandler) {
    if (!isDemoMode()) {
      return realHandler(args);
    }

    const fixture = loadFixture(fixturesDir, toolName);
    if (fixture) {
      return { content: [{ type: 'text', text: JSON.stringify(fixture, null, 2) }] };
    }

    // No fixture — fall through to real handler
    return realHandler(args);
  };
}

module.exports = { isDemoMode, loadFixture, createDemoInterceptor };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/demo-mode.test.js -v`
Expected: PASS (5 tests)

**Step 5: Commit**

```bash
git add mcp-edge-common/lib/demo-mode.js mcp-edge-common/__tests__/demo-mode.test.js
git commit -m "feat(mcp-edge): add demo mode with fixture loading and tool interceptor"
```

---

### Task 6: Package index and final common lib wiring

**Files:**
- Create: `mcp-edge-common/index.js`

**Step 1: Create index.js that re-exports all modules**

```javascript
// mcp-edge-common/index.js
module.exports = {
  ...require('./lib/jsonrpc'),
  ...require('./lib/auth'),
  ...require('./lib/health'),
  ...require('./lib/mcp-router'),
  ...require('./lib/demo-mode')
};
```

**Step 2: Update package.json dependencies**

Add express and supertest to package.json (express is a peer dep for health/router):

```json
{
  "name": "hdim-mcp-edge-common",
  "version": "0.1.0",
  "description": "Shared library for HDIM MCP edge sidecars",
  "main": "index.js",
  "scripts": {
    "test": "jest"
  },
  "engines": { "node": ">=20.0.0" },
  "dependencies": {
    "express": "^4.21.2"
  },
  "devDependencies": {
    "jest": "^30.0.0",
    "supertest": "^6.3.4"
  }
}
```

**Step 3: Run all common tests**

Run: `cd mcp-edge-common && npm install && npx jest --verbose`
Expected: PASS — all tests across 4 test files

**Step 4: Commit**

```bash
git add mcp-edge-common/index.js mcp-edge-common/package.json
git commit -m "feat(mcp-edge): wire up mcp-edge-common index with all exports"
```

---

## Stream B: mcp-edge-platform (Sidecar 1 — Port 3100)

### Task 7: Scaffold platform sidecar with server + health

**Files:**
- Create: `mcp-edge-platform/package.json`
- Create: `mcp-edge-platform/server.js`
- Create: `mcp-edge-platform/index.js`
- Create: `mcp-edge-platform/__tests__/server.test.js`

**Step 1: Create package.json**

```json
{
  "name": "hdim-mcp-edge-platform",
  "version": "0.1.0",
  "description": "HDIM MCP edge sidecar for platform tools (port 3100)",
  "main": "index.js",
  "scripts": {
    "start": "node index.js",
    "dev": "NODE_ENV=development node index.js",
    "test": "jest"
  },
  "engines": { "node": ">=20.0.0" },
  "dependencies": {
    "hdim-mcp-edge-common": "file:../mcp-edge-common",
    "cors": "^2.8.5",
    "express": "^4.21.2",
    "helmet": "^8.1.0"
  },
  "devDependencies": {
    "jest": "^30.0.0",
    "supertest": "^6.3.4"
  }
}
```

**Step 2: Write failing test for server health**

```javascript
// mcp-edge-platform/__tests__/server.test.js
const { createApp } = require('../server');

describe('platform edge server', () => {
  let app;
  let request;

  beforeAll(async () => {
    const supertest = (await import('supertest')).default;
    app = createApp();
    request = supertest(app);
  });

  it('responds to GET /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-platform-edge');
    expect(res.body.status).toBe('healthy');
  });

  it('responds to POST /mcp with initialize', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'initialize', params: {}
    });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-platform-edge');
  });
});
```

**Step 3: Run test to verify it fails**

Run: `cd mcp-edge-platform && npm install && npx jest __tests__/server.test.js -v`
Expected: FAIL — `Cannot find module '../server'`

**Step 4: Implement server.js and index.js**

```javascript
// mcp-edge-platform/server.js
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter } = require('hdim-mcp-edge-common');

function loadTools() {
  // Tools will be added in subsequent tasks
  return [];
}

function createApp() {
  const app = express();
  app.use(helmet());
  app.use(cors({ origin: '*', credentials: false }));
  app.use(express.json({ limit: '1mb' }));

  const tools = loadTools();

  app.use(createHealthRouter({
    serviceName: 'hdim-platform-edge',
    version: '0.1.0'
  }));

  app.use(createMcpRouter({
    tools,
    serverName: 'hdim-platform-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false'
  }));

  return app;
}

module.exports = { createApp };
```

```javascript
// mcp-edge-platform/index.js
const { createApp } = require('./server');

const port = Number(process.env.PORT || 3100);
const app = createApp();

const server = app.listen(port, () => {
  console.log(`[hdim-platform-edge] listening on :${port}`);
});

function shutdown(signal) {
  console.log(`[hdim-platform-edge] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
```

**Step 5: Run test to verify it passes**

Run: `cd mcp-edge-platform && npx jest __tests__/server.test.js -v`
Expected: PASS (2 tests)

**Step 6: Commit**

```bash
git add mcp-edge-platform/
git commit -m "feat(mcp-edge): scaffold platform edge sidecar with health and MCP endpoints"
```

---

### Task 8: Platform client — HTTP client for gateway

**Files:**
- Create: `mcp-edge-platform/lib/platform-client.js`
- Create: `mcp-edge-platform/__tests__/platform-client.test.js`

**Step 1: Write failing test**

```javascript
// mcp-edge-platform/__tests__/platform-client.test.js
const { createPlatformClient } = require('../lib/platform-client');

describe('platform-client', () => {
  it('creates a client with default base URL', () => {
    const client = createPlatformClient();
    expect(client.baseUrl).toBe('http://localhost:18080');
  });

  it('creates a client with custom base URL', () => {
    const client = createPlatformClient({ baseUrl: 'http://gateway:8080' });
    expect(client.baseUrl).toBe('http://gateway:8080');
  });

  it('strips trailing slash from base URL', () => {
    const client = createPlatformClient({ baseUrl: 'http://localhost:18080/' });
    expect(client.baseUrl).toBe('http://localhost:18080');
  });

  it('exposes get and post methods', () => {
    const client = createPlatformClient();
    expect(typeof client.get).toBe('function');
    expect(typeof client.post).toBe('function');
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-platform && npx jest __tests__/platform-client.test.js -v`
Expected: FAIL

**Step 3: Implement platform-client.js**

```javascript
// mcp-edge-platform/lib/platform-client.js
const DEFAULT_BASE_URL = 'http://localhost:18080';
const DEFAULT_TIMEOUT = 15_000;

function createPlatformClient({ baseUrl, apiKey, timeout } = {}) {
  const normalizedBase = (baseUrl || process.env.HDIM_BASE_URL || DEFAULT_BASE_URL)
    .trim().replace(/\/$/, '');
  const defaultApiKey = apiKey || process.env.MCP_EDGE_API_KEY || '';
  const requestTimeout = timeout || DEFAULT_TIMEOUT;

  function buildHeaders(overrideApiKey) {
    const headers = {
      accept: 'application/json',
      'content-type': 'application/json'
    };
    const key = overrideApiKey || defaultApiKey;
    if (key) headers.authorization = `Bearer ${key}`;
    return headers;
  }

  async function get(path, { apiKey: overrideKey } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(overrideKey),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  async function post(path, body, { apiKey: overrideKey } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(overrideKey),
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  return { baseUrl: normalizedBase, get, post };
}

module.exports = { createPlatformClient };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-platform && npx jest __tests__/platform-client.test.js -v`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add mcp-edge-platform/lib/platform-client.js mcp-edge-platform/__tests__/platform-client.test.js
git commit -m "feat(mcp-edge): add platform HTTP client for gateway proxy"
```

---

### Task 9: Platform tools — edge_health + platform_health + platform_info

**Files:**
- Create: `mcp-edge-platform/lib/tools/edge-health.js`
- Create: `mcp-edge-platform/lib/tools/platform-health.js`
- Create: `mcp-edge-platform/lib/tools/platform-info.js`
- Create: `mcp-edge-platform/__tests__/tools/edge-health.test.js`
- Create: `mcp-edge-platform/__tests__/tools/platform-health.test.js`
- Create: `mcp-edge-platform/__tests__/tools/platform-info.test.js`

**Step 1: Write failing tests for all three tools**

```javascript
// mcp-edge-platform/__tests__/tools/edge-health.test.js
const { definition } = require('../../lib/tools/edge-health');

describe('edge_health tool', () => {
  it('has correct name', () => {
    expect(definition.name).toBe('edge_health');
  });

  it('returns health status', async () => {
    const result = await definition.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe('healthy');
    expect(parsed.service).toBe('hdim-platform-edge');
    expect(typeof parsed.uptime).toBe('number');
  });
});
```

```javascript
// mcp-edge-platform/__tests__/tools/platform-health.test.js
const { createDefinition } = require('../../lib/tools/platform-health');

describe('platform_health tool', () => {
  it('has correct name', () => {
    const def = createDefinition({ get: jest.fn() });
    expect(def.name).toBe('platform_health');
  });

  it('returns gateway health on success', async () => {
    const mockClient = {
      get: jest.fn().mockResolvedValue({
        status: 200, ok: true, body: '{"status":"UP"}', url: 'http://localhost:18080/actuator/health'
      })
    };
    const def = createDefinition(mockClient);
    const result = await def.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.gateway.status).toBe(200);
    expect(mockClient.get).toHaveBeenCalledWith('/actuator/health');
  });

  it('returns error details on failure', async () => {
    const mockClient = { get: jest.fn().mockRejectedValue(new Error('ECONNREFUSED')) };
    const def = createDefinition(mockClient);
    const result = await def.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.gateway.ok).toBe(false);
    expect(parsed.gateway.error).toContain('ECONNREFUSED');
  });
});
```

```javascript
// mcp-edge-platform/__tests__/tools/platform-info.test.js
const { definition } = require('../../lib/tools/platform-info');

describe('platform_info tool', () => {
  it('has correct name', () => {
    expect(definition.name).toBe('platform_info');
  });

  it('returns platform info', async () => {
    const result = await definition.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.name).toBe('hdim-platform-edge');
    expect(parsed.defaultGatewayUrl).toBeDefined();
  });
});
```

**Step 2: Run tests to verify they fail**

Run: `cd mcp-edge-platform && npx jest __tests__/tools/ -v`
Expected: FAIL — modules not found

**Step 3: Implement all three tools**

```javascript
// mcp-edge-platform/lib/tools/edge-health.js
const startTime = Date.now();

const definition = {
  name: 'edge_health',
  description: 'Local MCP edge health/status (does not call upstream platform)',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const payload = {
      status: 'healthy',
      service: 'hdim-platform-edge',
      version: '0.1.0',
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString(),
      demoMode: process.env.HDIM_DEMO_MODE === 'true'
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
```

```javascript
// mcp-edge-platform/lib/tools/platform-health.js
function createDefinition(platformClient) {
  return {
    name: 'platform_health',
    description: 'Gateway actuator health check (/actuator/health)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      let gateway;
      try {
        const res = await platformClient.get('/actuator/health');
        gateway = { status: res.status, ok: res.ok, body: res.body };
      } catch (err) {
        gateway = { ok: false, error: err?.message || String(err) };
      }
      return { content: [{ type: 'text', text: JSON.stringify({ gateway }, null, 2) }] };
    }
  };
}

module.exports = { createDefinition };
```

```javascript
// mcp-edge-platform/lib/tools/platform-info.js
const DEFAULT_GATEWAY_URL = 'http://localhost:18080';

const definition = {
  name: 'platform_info',
  description: 'HDIM platform MCP edge info: base URLs, version, configuration',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const payload = {
      name: 'hdim-platform-edge',
      version: '0.1.0',
      defaultGatewayUrl: DEFAULT_GATEWAY_URL,
      envGatewayUrl: process.env.HDIM_BASE_URL || null,
      demoMode: process.env.HDIM_DEMO_MODE === 'true',
      protocol: '2025-11-25'
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
```

**Step 4: Run tests to verify they pass**

Run: `cd mcp-edge-platform && npx jest __tests__/tools/ -v`
Expected: PASS (6 tests across 3 files)

**Step 5: Commit**

```bash
git add mcp-edge-platform/lib/tools/ mcp-edge-platform/__tests__/tools/
git commit -m "feat(mcp-edge): add edge_health, platform_health, platform_info tools"
```

---

### Task 10: Platform tools — fhir_metadata + service_catalog

**Files:**
- Create: `mcp-edge-platform/lib/tools/fhir-metadata.js`
- Create: `mcp-edge-platform/lib/tools/service-catalog.js`
- Create: `mcp-edge-platform/__tests__/tools/fhir-metadata.test.js`
- Create: `mcp-edge-platform/__tests__/tools/service-catalog.test.js`

**Step 1: Write failing tests**

```javascript
// mcp-edge-platform/__tests__/tools/fhir-metadata.test.js
const { createDefinition } = require('../../lib/tools/fhir-metadata');

describe('fhir_metadata tool', () => {
  it('has correct name', () => {
    const def = createDefinition({ get: jest.fn() });
    expect(def.name).toBe('fhir_metadata');
  });

  it('returns FHIR capability statement', async () => {
    const mockClient = {
      get: jest.fn().mockResolvedValue({
        status: 200, ok: true,
        body: '{"resourceType":"CapabilityStatement","fhirVersion":"4.0.1"}'
      })
    };
    const def = createDefinition(mockClient);
    const result = await def.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.status).toBe(200);
    expect(mockClient.get).toHaveBeenCalledWith('/fhir/metadata');
  });
});
```

```javascript
// mcp-edge-platform/__tests__/tools/service-catalog.test.js
const { createDefinition } = require('../../lib/tools/service-catalog');

describe('service_catalog tool', () => {
  it('has correct name', () => {
    const def = createDefinition({ get: jest.fn() });
    expect(def.name).toBe('service_catalog');
  });

  it('returns service catalog with health status', async () => {
    const mockClient = {
      get: jest.fn().mockResolvedValue({ status: 200, ok: true, body: '{"status":"UP"}' })
    };
    const def = createDefinition(mockClient);
    const result = await def.handler({});
    const parsed = JSON.parse(result.content[0].text);
    expect(Array.isArray(parsed.services)).toBe(true);
    expect(parsed.services.length).toBeGreaterThan(0);
  });
});
```

**Step 2: Run tests to verify they fail**

Run: `cd mcp-edge-platform && npx jest __tests__/tools/fhir-metadata.test.js __tests__/tools/service-catalog.test.js -v`
Expected: FAIL

**Step 3: Implement both tools**

```javascript
// mcp-edge-platform/lib/tools/fhir-metadata.js
function createDefinition(platformClient) {
  return {
    name: 'fhir_metadata',
    description: 'FHIR R4 capability statement from the HDIM FHIR service',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const res = await platformClient.get('/fhir/metadata');
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ status: res.status, ok: res.ok, metadata: res.body }, null, 2)
          }]
        };
      } catch (err) {
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2)
          }]
        };
      }
    }
  };
}

module.exports = { createDefinition };
```

```javascript
// mcp-edge-platform/lib/tools/service-catalog.js
const SERVICE_CATALOG = [
  { name: 'gateway-edge', category: 'gateway', port: 18080, healthPath: '/actuator/health' },
  { name: 'fhir-service', category: 'clinical-core', port: 8084, healthPath: '/fhir/metadata' },
  { name: 'quality-measure-service', category: 'quality', port: 8085, healthPath: '/actuator/health' },
  { name: 'care-gap-service', category: 'quality', port: 8086, healthPath: '/actuator/health' },
  { name: 'cql-engine-service', category: 'quality', port: 8081, healthPath: '/actuator/health' },
  { name: 'demo-seeding-service', category: 'demo', port: 8098, healthPath: '/demo/actuator/health' },
  { name: 'audit-query-service', category: 'audit', port: 8089, healthPath: '/actuator/health' },
  { name: 'clinical-workflow-service', category: 'clinical', port: 8090, healthPath: '/actuator/health' },
  { name: 'postgres', category: 'data', port: 5435, healthPath: null },
  { name: 'redis', category: 'data', port: 6380, healthPath: null },
  { name: 'kafka', category: 'messaging', port: 9094, healthPath: null }
];

function createDefinition(platformClient) {
  return {
    name: 'service_catalog',
    description: 'List all HDIM microservices with health status and metadata',
    inputSchema: {
      type: 'object',
      properties: {
        category: { type: 'string', description: 'Filter by category: gateway, clinical-core, quality, data, messaging' }
      },
      additionalProperties: false
    },
    handler: async (args) => {
      let services = SERVICE_CATALOG;
      if (args?.category) {
        services = services.filter((s) => s.category === args.category);
      }

      const results = await Promise.all(
        services.map(async (svc) => {
          if (!svc.healthPath) return { ...svc, status: 'unknown', detail: 'no health endpoint' };
          try {
            const res = await platformClient.get(svc.healthPath);
            return { ...svc, status: res.ok ? 'healthy' : 'unhealthy', httpStatus: res.status };
          } catch (err) {
            return { ...svc, status: 'unreachable', error: err?.message || String(err) };
          }
        })
      );

      return {
        content: [{
          type: 'text',
          text: JSON.stringify({ services: results, checkedAt: new Date().toISOString() }, null, 2)
        }]
      };
    }
  };
}

module.exports = { createDefinition, SERVICE_CATALOG };
```

**Step 4: Run tests to verify they pass**

Run: `cd mcp-edge-platform && npx jest __tests__/tools/fhir-metadata.test.js __tests__/tools/service-catalog.test.js -v`
Expected: PASS

**Step 5: Commit**

```bash
git add mcp-edge-platform/lib/tools/fhir-metadata.js mcp-edge-platform/lib/tools/service-catalog.js mcp-edge-platform/__tests__/tools/
git commit -m "feat(mcp-edge): add fhir_metadata and service_catalog tools"
```

---

### Task 11: Platform tools — dashboard_stats + demo_status + demo_seed

**Files:**
- Create: `mcp-edge-platform/lib/tools/dashboard-stats.js`
- Create: `mcp-edge-platform/lib/tools/demo-status.js`
- Create: `mcp-edge-platform/lib/tools/demo-seed.js`
- Create: `mcp-edge-platform/__tests__/tools/dashboard-stats.test.js`
- Create: `mcp-edge-platform/__tests__/tools/demo-status.test.js`
- Create: `mcp-edge-platform/__tests__/tools/demo-seed.test.js`

Follow same TDD pattern: write test → verify fail → implement → verify pass → commit.

Test patterns match Tasks 9-10. Each tool uses `createDefinition(platformClient)` factory pattern.

- `dashboard_stats` → `GET /analytics/dashboards`
- `demo_status` → `GET /api/v1/demo/status`
- `demo_seed` → `POST /api/v1/demo/scenarios/{scenarioName}` with `scenarioName` input param

**Commit message:** `feat(mcp-edge): add dashboard_stats, demo_status, demo_seed tools`

---

### Task 12: Wire all platform tools into server.js

**Files:**
- Modify: `mcp-edge-platform/server.js`
- Modify: `mcp-edge-platform/__tests__/server.test.js`

**Step 1: Update server.test.js to verify all 8 tools are listed**

```javascript
// Add to existing server.test.js
it('lists all 8 platform tools', async () => {
  const res = await request.post('/mcp').send({
    jsonrpc: '2.0', id: 10, method: 'tools/list', params: {}
  });
  expect(res.body.result.tools).toHaveLength(8);
  const names = res.body.result.tools.map((t) => t.name).sort();
  expect(names).toEqual([
    'dashboard_stats', 'demo_seed', 'demo_status',
    'edge_health', 'fhir_metadata', 'platform_health',
    'platform_info', 'service_catalog'
  ]);
});
```

**Step 2: Run test to verify it fails (currently 0 tools)**

**Step 3: Update server.js loadTools()**

```javascript
// mcp-edge-platform/server.js — updated loadTools
const { createPlatformClient } = require('./lib/platform-client');

function loadTools() {
  const client = createPlatformClient();
  return [
    require('./lib/tools/edge-health').definition,
    require('./lib/tools/platform-health').createDefinition(client),
    require('./lib/tools/platform-info').definition,
    require('./lib/tools/fhir-metadata').createDefinition(client),
    require('./lib/tools/service-catalog').createDefinition(client),
    require('./lib/tools/dashboard-stats').createDefinition(client),
    require('./lib/tools/demo-status').createDefinition(client),
    require('./lib/tools/demo-seed').createDefinition(client)
  ];
}
```

**Step 4: Run test to verify it passes**

**Step 5: Commit**

```bash
git add mcp-edge-platform/server.js mcp-edge-platform/__tests__/server.test.js
git commit -m "feat(mcp-edge): wire all 8 platform tools into server"
```

---

### Task 13: Platform sidecar Dockerfile + demo fixtures

**Files:**
- Create: `mcp-edge-platform/Dockerfile`
- Create: `mcp-edge-platform/.dockerignore`
- Create: `mcp-edge-platform/.env.example`
- Create: `mcp-edge-platform/fixtures/edge_health.json`
- Create: `mcp-edge-platform/fixtures/platform_health.json`

**Step 1: Create Dockerfile**

```dockerfile
FROM node:20-slim
WORKDIR /app
COPY mcp-edge-common/ ./mcp-edge-common/
COPY mcp-edge-platform/package.json mcp-edge-platform/package-lock.json* ./mcp-edge-platform/
WORKDIR /app/mcp-edge-platform
RUN npm install --omit=dev
COPY mcp-edge-platform/ ./
ENV NODE_ENV=production
ENV PORT=3100
EXPOSE 3100
HEALTHCHECK --interval=10s --timeout=2s --start-period=5s --retries=6 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||3100)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
USER node
CMD ["node", "index.js"]
```

Note: Dockerfile context is hdim-master root (to access mcp-edge-common).

**Step 2: Create .env.example**

```
PORT=3100
HDIM_BASE_URL=http://localhost:18080
MCP_EDGE_API_KEY=
MCP_EDGE_ENFORCE_ROLE_AUTH=true
HDIM_DEMO_MODE=false
```

**Step 3: Create sample demo fixtures**

```json
// mcp-edge-platform/fixtures/edge_health.json
{ "status": "healthy", "service": "hdim-platform-edge", "version": "0.1.0", "uptime": 3600, "demoMode": true }
```

```json
// mcp-edge-platform/fixtures/platform_health.json
{ "gateway": { "status": 200, "ok": true, "body": "{\"status\":\"UP\"}" } }
```

**Step 4: Commit**

```bash
git add mcp-edge-platform/Dockerfile mcp-edge-platform/.dockerignore mcp-edge-platform/.env.example mcp-edge-platform/fixtures/
git commit -m "feat(mcp-edge): add platform Dockerfile, env config, demo fixtures"
```

---

## Stream C: mcp-edge-devops (Sidecar 2 — Port 3200)

### Task 14: Scaffold devops sidecar with server + health

Same pattern as Task 7 but for `mcp-edge-devops/` on port 3200.
Service name: `hdim-devops-edge`. No gateway access, no PHI.

**Commit message:** `feat(mcp-edge): scaffold devops edge sidecar with health and MCP endpoints`

---

### Task 15: Docker client — CLI wrapper

**Files:**
- Create: `mcp-edge-devops/lib/docker-client.js`
- Create: `mcp-edge-devops/__tests__/docker-client.test.js`

Wraps `docker compose` commands. Key methods:
- `ps(composeFile)` → `docker compose -f {file} ps --format json`
- `logs(composeFile, service, tail)` → `docker compose -f {file} logs {service} --tail {n}`
- `restart(composeFile, service)` → `docker compose -f {file} restart {service}`
- `config(composeFile)` → `docker compose -f {file} config`

Port the `runCommand` and `parseComposePsJson` logic from existing `hdim-docker-mcp.mjs`.

**Commit message:** `feat(mcp-edge): add docker CLI client wrapper`

---

### Task 16: DevOps tools — edge_health + docker_status + docker_logs

**Files:**
- Create: `mcp-edge-devops/lib/tools/edge-health.js`
- Create: `mcp-edge-devops/lib/tools/docker-status.js`
- Create: `mcp-edge-devops/lib/tools/docker-logs.js`
- Create corresponding test files

Tools:
- `edge_health` — same pattern as platform, service name `hdim-devops-edge`
- `docker_status` — calls `dockerClient.ps()`, returns structured service states
- `docker_logs` — calls `dockerClient.logs()`, params: `{ service: string, tail: number }`

**Commit message:** `feat(mcp-edge): add docker_status, docker_logs tools`

---

### Task 17: DevOps tools — docker_restart + service_dependencies

**Files:**
- Create: `mcp-edge-devops/lib/tools/docker-restart.js`
- Create: `mcp-edge-devops/lib/tools/service-dependencies.js`
- Create corresponding test files

Tools:
- `docker_restart` — calls `dockerClient.restart()`, params: `{ service: string }`
- `service_dependencies` — returns the SERVICE_DEPENDENCIES map from hdim-docker-mcp.mjs (static, no CLI call)

**Commit message:** `feat(mcp-edge): add docker_restart, service_dependencies tools`

---

### Task 18: DevOps tools — compose_config + build_status

**Files:**
- Create: `mcp-edge-devops/lib/tools/compose-config.js`
- Create: `mcp-edge-devops/lib/tools/build-status.js`
- Create corresponding test files

Tools:
- `compose_config` — calls `dockerClient.config()`, validates env vars against CONFIG_CONTRACTS
- `build_status` — runs `npx nx show projects --affected` or similar, returns build state

**Commit message:** `feat(mcp-edge): add compose_config, build_status tools`

---

### Task 19: Wire all devops tools + Dockerfile

Same pattern as Tasks 12-13 but for devops sidecar. 7 tools total.
Dockerfile includes Docker CLI binary.

**Commit message:** `feat(mcp-edge): wire all 7 devops tools, add Dockerfile`

---

## Stream D: Bridges + Configuration

### Task 20: Platform stdio bridge

**Files:**
- Create: `scripts/mcp/mcp-edge-platform-bridge.mjs`
- Create: `scripts/mcp/__tests__/mcp-edge-platform-bridge.test.mjs`

Copy pattern from `/mnt/wdblack/dev/projects/west-bethel-motel-booking-system/scripts/mcp/mcp-edge-http-bridge.mjs`.
Default URL: `http://localhost:3100/mcp`.

**Commit message:** `feat(mcp-edge): add platform edge stdio bridge for Claude Desktop`

---

### Task 21: DevOps stdio bridge

**Files:**
- Create: `scripts/mcp/mcp-edge-devops-bridge.mjs`

Same as Task 20, default URL: `http://localhost:3200/mcp`.

**Commit message:** `feat(mcp-edge): add devops edge stdio bridge for Claude Desktop`

---

### Task 22: MCP client configuration

**Files:**
- Modify: `.mcp.json`
- Modify: `.vscode/mcp.json` (if exists)

**Step 1: Update .mcp.json**

Add `hdim-platform-edge` and `hdim-devops-edge` servers alongside existing entries:

```json
{
  "mcpServers": {
    "MCP_DOCKER": { "type": "stdio", "command": "node", "args": ["scripts/mcp/hdim-docker-mcp.mjs"] },
    "nx-mcp": { "type": "stdio", "command": "node", "args": ["scripts/mcp/nx-mcp.mjs"] },
    "hdim-platform": { "type": "stdio", "command": "node", "args": ["scripts/mcp/hdim-platform-mcp.mjs"] },
    "hdim-platform-edge": {
      "type": "stdio",
      "command": "node",
      "args": ["scripts/mcp/mcp-edge-platform-bridge.mjs"],
      "env": {
        "MCP_EDGE_URL": "http://localhost:3100/mcp",
        "MCP_EDGE_OPERATOR_ROLE": "platform_admin"
      }
    },
    "hdim-devops-edge": {
      "type": "stdio",
      "command": "node",
      "args": ["scripts/mcp/mcp-edge-devops-bridge.mjs"],
      "env": {
        "MCP_EDGE_URL": "http://localhost:3200/mcp",
        "MCP_EDGE_OPERATOR_ROLE": "platform_admin"
      }
    }
  }
}
```

**Commit message:** `feat(mcp-edge): add edge sidecars to MCP client configuration`

---

### Task 23: Docker compose for both sidecars

**Files:**
- Create: `docker-compose.mcp-edge.yml`

```yaml
services:
  mcp-edge-platform:
    build:
      context: .
      dockerfile: mcp-edge-platform/Dockerfile
    env_file: ./mcp-edge-platform/.env
    restart: unless-stopped
    ports:
      - "3100:3100"
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3100/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]
      interval: 10s
      timeout: 2s
      start_period: 5s
      retries: 6

  mcp-edge-devops:
    build:
      context: .
      dockerfile: mcp-edge-devops/Dockerfile
    env_file: ./mcp-edge-devops/.env
    restart: unless-stopped
    ports:
      - "3200:3200"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3200/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]
      interval: 10s
      timeout: 2s
      start_period: 5s
      retries: 6
```

**Smoke test:**

```bash
docker compose -f docker-compose.mcp-edge.yml up -d
curl http://localhost:3100/health
curl http://localhost:3200/health
# Both should return {"status":"healthy",...}
```

**Commit message:** `feat(mcp-edge): add docker-compose for both edge sidecars`

---

## Verification Checklist

After all streams complete:

- [ ] `cd mcp-edge-common && npm test` — all unit tests pass
- [ ] `cd mcp-edge-platform && npm test` — all unit tests pass
- [ ] `cd mcp-edge-devops && npm test` — all unit tests pass
- [ ] `docker compose -f docker-compose.mcp-edge.yml up -d` — both containers healthy
- [ ] `curl localhost:3100/health` — returns healthy JSON
- [ ] `curl localhost:3200/health` — returns healthy JSON
- [ ] Platform bridge: `echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | MCP_EDGE_URL=http://localhost:3100/mcp node scripts/mcp/mcp-edge-platform-bridge.mjs` — lists 8 tools
- [ ] DevOps bridge: same pattern — lists 7 tools
- [ ] Demo mode: `HDIM_DEMO_MODE=true node mcp-edge-platform/index.js` — responds with fixtures
