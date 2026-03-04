# HDIM MCP Edge — Testing & Validation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Elevate MCP edge test coverage from 134 unit/basic-integration tests to industry-grade validation proof — including critical bug fixes from code review, comprehensive integration tests, security boundary tests, contract tests, error handling coverage, and demo mode wiring. This plan addresses every finding from the code review and test audit.

**Architecture:** Fix code review critical/important findings first, then layer tests by category: unit gap coverage → integration round-trips → security boundary → contract validation → demo mode wiring → error resilience. Each test category runs in its own test file to enable selective execution.

**Tech Stack:** Jest 30, supertest 6, Node.js 20+

**Reference:** Code review findings (3 critical, 5 important) + test audit (gaps identified across all 3 packages)

---

## Parallel Execution Map

```
Stream A (Critical Fixes):         Task 1 → 2 → 3 → 4
Stream B (Common Lib Test Gaps):   Task 5 → 6 → 7 → 8
Stream C (Platform Test Gaps):     Task 9 → 10 → 11
Stream D (DevOps Test Gaps):       Task 12 → 13 → 14
Stream E (Integration Tests):      Task 15 → 16 → 17
Stream F (Security Boundary):      Task 18 → 19
Stream G (Demo Mode Wiring):       Task 20 → 21 → 22
Stream H (Contract + Bridge):      Task 23 → 24

Dependencies:
  - Stream B-D can run in parallel (no dependencies)
  - Stream E depends on Stream A completing (fixes must land first)
  - Stream F depends on Stream A Task 1 (role policy fix)
  - Stream G depends on no other streams
  - Stream H depends on Streams B-D completing (unit gaps first)
```

---

## Stream A: Critical Code Review Fixes

### Task 1: Fix role policy drift from design spec

The code review found `clinical_admin` and `quality_officer` have `platform_info` access that the design doc does not grant. Also: two undocumented roles (`clinician`, `care_coordinator`). Per least-privilege in HIPAA context, tighten to match spec, then update design doc to document the forward-looking roles.

**Files:**
- Modify: `mcp-edge-common/lib/auth.js:18-28`
- Modify: `mcp-edge-common/__tests__/auth.test.js`
- Modify: `docs/plans/2026-03-04-hdim-mcp-edge-design.md` (role table)

**Step 1: Write failing tests for all 7 roles**

Add a comprehensive role policy test matrix that matches the design spec exactly.

```javascript
// Append to mcp-edge-common/__tests__/auth.test.js

describe('role policy matrix — design spec compliance', () => {
  const cases = [
    // [role, toolName, expectedAllowed]
    ['platform_admin', 'edge_health', true],
    ['platform_admin', 'docker_restart', true],
    ['developer', 'fhir_metadata', true],
    ['developer', 'docker_logs', true],
    ['clinical_admin', 'edge_health', true],
    ['clinical_admin', 'platform_health', true],
    ['clinical_admin', 'dashboard_stats', true],
    ['clinical_admin', 'platform_info', false],  // NOT granted per spec
    ['clinical_admin', 'fhir_metadata', false],
    ['quality_officer', 'edge_health', true],
    ['quality_officer', 'dashboard_stats', true],
    ['quality_officer', 'platform_info', false],  // NOT granted per spec
    ['quality_officer', 'fhir_metadata', false],
    ['executive', 'edge_health', true],
    ['executive', 'dashboard_stats', true],
    ['executive', 'platform_info', true],
    ['executive', 'fhir_metadata', false],
    ['clinician', 'edge_health', true],
    ['clinician', 'platform_health', true],
    ['clinician', 'dashboard_stats', false],
    ['care_coordinator', 'edge_health', true],
    ['care_coordinator', 'platform_health', false],
  ];

  it.each(cases)('role=%s tool=%s → allowed=%s', (role, toolName, expected) => {
    const result = authorizeToolCall({ toolName, role, enforce: true });
    expect(result.allowed).toBe(expected);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-common && npx jest __tests__/auth.test.js -v`
Expected: FAIL — `clinical_admin` + `platform_info` will be `true` (should be `false`)

**Step 3: Fix auth.js role policies to match spec**

```javascript
// mcp-edge-common/lib/auth.js — rolePolicies()
function rolePolicies() {
  return {
    platform_admin: [/./],
    developer: [/./],
    clinical_admin: [/^(edge_health|platform_health|dashboard_stats)$/],
    quality_officer: [/^(edge_health|dashboard_stats)$/],
    executive: [/^(edge_health|dashboard_stats|platform_info)$/],
    clinician: [/^(edge_health|platform_health)$/],
    care_coordinator: [/^(edge_health)$/]
  };
}
```

Note: Added `$` anchors to all regex patterns to prevent partial prefix matches (e.g., `edge_health_extended` should not match `edge_health` policy).

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-common && npx jest __tests__/auth.test.js -v`
Expected: PASS — all role matrix tests green

**Step 5: Update design doc role table**

Add `clinician` and `care_coordinator` rows to the design doc's role table with their approved tool access.

**Step 6: Commit**

```bash
git add mcp-edge-common/lib/auth.js mcp-edge-common/__tests__/auth.test.js docs/plans/2026-03-04-hdim-mcp-edge-design.md
git commit -m "fix(mcp-edge): align role policies with design spec, add regex anchors"
```

---

### Task 2: Fix docker_restart service validation

Code review critical: `docker_restart` passes user-supplied service name directly to Docker CLI with no validation. Healthcare platform — database restarts cause availability gaps.

**Files:**
- Modify: `mcp-edge-devops/lib/tools/docker-restart.js`
- Modify: `mcp-edge-devops/__tests__/tools/docker-restart.test.js`

**Step 1: Write failing tests for service validation**

```javascript
// Append to docker-restart.test.js

describe('service name validation', () => {
  it('rejects service names with shell metacharacters', async () => {
    const tool = createDefinition(mockClient);
    const result = await tool.handler({ service: 'postgres; rm -rf /' });
    const body = JSON.parse(result.content[0].text);
    expect(body.error).toMatch(/invalid service name/i);
  });

  it('rejects empty string after trim', async () => {
    const tool = createDefinition(mockClient);
    const result = await tool.handler({ service: '   ' });
    const body = JSON.parse(result.content[0].text);
    expect(body.error).toMatch(/required/i);
  });

  it('accepts valid service names with hyphens and underscores', async () => {
    const tool = createDefinition(mockClient);
    mockClient.restart.mockResolvedValue({ ok: true, stdout: '', stderr: '' });
    const result = await tool.handler({ service: 'fhir-service_v2' });
    const body = JSON.parse(result.content[0].text);
    expect(body.ok).toBe(true);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-devops && npx jest __tests__/tools/docker-restart.test.js -v`
Expected: FAIL — metacharacter test passes through to restart

**Step 3: Add service name validation to docker-restart.js**

```javascript
// Add before the try block in handler
const SERVICE_NAME_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/;

// In handler:
const trimmed = String(service || '').trim();
if (!trimmed) {
  return { content: [{ type: 'text', text: JSON.stringify({ error: 'service is required' }) }] };
}
if (!SERVICE_NAME_PATTERN.test(trimmed)) {
  return { content: [{ type: 'text', text: JSON.stringify({ error: `Invalid service name: only alphanumeric, hyphens, dots, underscores allowed` }) }] };
}
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-devops && npx jest __tests__/tools/docker-restart.test.js -v`
Expected: PASS

**Step 5: Commit**

```bash
git add mcp-edge-devops/lib/tools/docker-restart.js mcp-edge-devops/__tests__/tools/docker-restart.test.js
git commit -m "fix(mcp-edge): add service name validation to docker_restart"
```

---

### Task 3: Add service name validation to docker_logs

Same vulnerability as docker_restart — user-supplied service name to CLI.

**Files:**
- Modify: `mcp-edge-devops/lib/tools/docker-logs.js`
- Modify: `mcp-edge-devops/__tests__/tools/docker-logs.test.js`

**Step 1: Write failing tests for service validation**

Same SERVICE_NAME_PATTERN validation as Task 2. Also add:

```javascript
describe('tail parameter validation', () => {
  it('rejects negative tail values', async () => {
    const tool = createDefinition(mockClient);
    const result = await tool.handler({ service: 'postgres', tail: -5 });
    const body = JSON.parse(result.content[0].text);
    expect(body.error).toMatch(/tail must be/i);
  });

  it('caps tail at 10000', async () => {
    const tool = createDefinition(mockClient);
    mockClient.logs.mockResolvedValue({ ok: true, stdout: 'logs', stderr: '' });
    await tool.handler({ service: 'postgres', tail: 50000 });
    expect(mockClient.logs).toHaveBeenCalledWith('postgres', 10000);
  });
});
```

**Step 2: Run → FAIL**
**Step 3: Implement validation in docker-logs.js**
**Step 4: Run → PASS**

**Step 5: Commit**

```bash
git add mcp-edge-devops/lib/tools/docker-logs.js mcp-edge-devops/__tests__/tools/docker-logs.test.js
git commit -m "fix(mcp-edge): add service/tail validation to docker_logs"
```

---

### Task 4: Wire demo mode interceptor into MCP router

Code review important: `createDemoInterceptor` exists but is never wired. Fixtures exist in platform package but are unused.

**Files:**
- Modify: `mcp-edge-common/lib/mcp-router.js`
- Create: `mcp-edge-common/__tests__/mcp-router-demo.test.js`

**Step 1: Write failing test for demo mode integration**

```javascript
const supertest = require('supertest');
const express = require('express');
const path = require('path');
const { createMcpRouter } = require('../lib/mcp-router');

describe('mcp-router demo mode', () => {
  let app, request;

  const mockTool = {
    name: 'edge_health',
    description: 'test',
    inputSchema: { type: 'object' },
    handler: async () => ({ content: [{ type: 'text', text: 'REAL_RESPONSE' }] })
  };

  beforeEach(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [mockTool],
      serverName: 'test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false,
      fixturesDir: path.join(__dirname, '..', '..', 'mcp-edge-platform', 'fixtures')
    }));
    request = supertest(app);
  });

  afterEach(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  it('returns fixture data instead of real handler in demo mode', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'edge_health', arguments: {} } });
    const text = res.body.result.content[0].text;
    expect(text).not.toContain('REAL_RESPONSE');
    expect(text).toContain('demoMode');
  });

  it('falls through to real handler when no fixture exists', async () => {
    const noFixtureTool = { ...mockTool, name: 'nonexistent_tool' };
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [noFixtureTool],
      serverName: 'test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false,
      fixturesDir: path.join(__dirname, 'nonexistent-fixtures')
    }));
    request = supertest(app);

    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'nonexistent_tool', arguments: {} } });
    expect(res.body.result.content[0].text).toBe('REAL_RESPONSE');
  });
});
```

**Step 2: Run → FAIL (fixturesDir option not supported yet)**

**Step 3: Modify mcp-router.js to accept fixturesDir and wire demo interceptor**

Add `fixturesDir` to the options. In `handleToolsCall`, after auth check and before calling `tool.handler`, check demo mode:

```javascript
const { isDemoMode, loadFixture } = require('./demo-mode');

// In handleToolsCall, after auth passes:
if (fixturesDir && isDemoMode()) {
  const fixture = loadFixture(fixturesDir, name);
  if (fixture) {
    return jsonRpcResult(id, { content: [{ type: 'text', text: JSON.stringify(fixture, null, 2) }] });
  }
}
```

**Step 4: Run → PASS**

**Step 5: Update both server.js files to pass fixturesDir**

```javascript
// mcp-edge-platform/server.js — add to createMcpRouter call:
fixturesDir: path.join(__dirname, 'fixtures')

// mcp-edge-devops/server.js — add to createMcpRouter call:
fixturesDir: path.join(__dirname, 'fixtures')
```

**Step 6: Commit**

```bash
git add mcp-edge-common/lib/mcp-router.js mcp-edge-common/__tests__/mcp-router-demo.test.js mcp-edge-platform/server.js mcp-edge-devops/server.js
git commit -m "feat(mcp-edge): wire demo mode interceptor into MCP router"
```

---

## Stream B: Common Lib Test Gap Coverage

### Task 5: normalizeToolName + extractApiKey comprehensive tests

**Files:**
- Modify: `mcp-edge-common/__tests__/auth.test.js`

**Step 1: Write tests**

```javascript
describe('normalizeToolName', () => {
  it('replaces dots with underscores', () => {
    expect(normalizeToolName('tool.name.here')).toBe('tool_name_here');
  });
  it('trims whitespace', () => {
    expect(normalizeToolName('  edge_health  ')).toBe('edge_health');
  });
  it('handles null', () => {
    expect(normalizeToolName(null)).toBe('');
  });
  it('handles undefined', () => {
    expect(normalizeToolName(undefined)).toBe('');
  });
  it('handles empty string', () => {
    expect(normalizeToolName('')).toBe('');
  });
  it('converts number to string', () => {
    expect(normalizeToolName(123)).toBe('123');
  });
});

describe('extractApiKey edge cases', () => {
  it('returns null for empty bearer token', () => {
    expect(extractApiKey({ headers: { authorization: 'Bearer ' } })).toBeNull();
  });
  it('returns null for Basic scheme', () => {
    expect(extractApiKey({ headers: { authorization: 'Basic abc123' } })).toBeNull();
  });
  it('returns token with whitespace trimmed', () => {
    expect(extractApiKey({ headers: { authorization: 'Bearer  abc123  ' } })).toBe('abc123');
  });
  it('handles null req', () => {
    expect(extractApiKey(null)).toBeNull();
  });
});
```

**Step 2: Run → should mostly pass (verifying existing behavior)**
**Step 3: Fix any failures**
**Step 4: Commit**

```bash
git add mcp-edge-common/__tests__/auth.test.js
git commit -m "test(mcp-edge): add normalizeToolName and extractApiKey edge case tests"
```

---

### Task 6: Health endpoint HTTP tests

**Files:**
- Modify: `mcp-edge-common/__tests__/health.test.js`

**Step 1: Write comprehensive health endpoint tests**

```javascript
const supertest = require('supertest');
const express = require('express');
const { createHealthRouter } = require('../lib/health');

describe('health endpoint HTTP behavior', () => {
  it('GET /health returns 200 with required fields', async () => {
    const app = express();
    app.use(createHealthRouter({ serviceName: 'test-svc', version: '1.0.0' }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      status: 'healthy',
      service: 'test-svc',
      version: '1.0.0'
    });
    expect(typeof res.body.uptime).toBe('number');
    expect(res.body.uptime).toBeGreaterThanOrEqual(0);
    expect(res.body.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
  });

  it('merges statusProvider output into response', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test',
      version: '1.0.0',
      statusProvider: () => ({ gatewayReachable: true, demoMode: false })
    }));
    const res = await supertest(app).get('/health');
    expect(res.body.gatewayReachable).toBe(true);
    expect(res.body.demoMode).toBe(false);
  });

  it('statusProvider can override status field', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test',
      version: '1.0.0',
      statusProvider: () => ({ status: 'degraded' })
    }));
    const res = await supertest(app).get('/health');
    expect(res.body.status).toBe('degraded');
  });
});
```

**Step 2: Run → PASS (testing existing behavior)**
**Step 3: Commit**

```bash
git add mcp-edge-common/__tests__/health.test.js
git commit -m "test(mcp-edge): add health endpoint HTTP behavior tests"
```

---

### Task 7: Demo mode comprehensive tests

**Files:**
- Modify: `mcp-edge-common/__tests__/demo-mode.test.js`

**Step 1: Write tests for all demo mode paths**

```javascript
describe('isDemoMode variants', () => {
  afterEach(() => { delete process.env.HDIM_DEMO_MODE; });

  it.each(['true', 'TRUE', '1', 'yes', 'YES'])('returns true for %s', (val) => {
    process.env.HDIM_DEMO_MODE = val;
    expect(isDemoMode()).toBe(true);
  });

  it.each(['false', '0', 'no', '', 'random'])('returns false for %s', (val) => {
    process.env.HDIM_DEMO_MODE = val;
    expect(isDemoMode()).toBe(false);
  });
});

describe('loadFixture', () => {
  const fixturesDir = path.join(__dirname, '..', '..', 'mcp-edge-platform', 'fixtures');

  it('loads valid fixture JSON', () => {
    const result = loadFixture(fixturesDir, 'edge_health');
    expect(result).not.toBeNull();
    expect(result.status).toBe('healthy');
  });

  it('returns null for nonexistent fixture', () => {
    expect(loadFixture(fixturesDir, 'nonexistent')).toBeNull();
  });

  it('returns null for invalid JSON fixture', () => {
    // Uses a temp dir with malformed JSON
    const tmpDir = path.join(__dirname, 'tmp-fixtures');
    fs.mkdirSync(tmpDir, { recursive: true });
    fs.writeFileSync(path.join(tmpDir, 'bad.json'), 'not json');
    expect(loadFixture(tmpDir, 'bad')).toBeNull();
    fs.rmSync(tmpDir, { recursive: true });
  });
});

describe('createDemoInterceptor end-to-end', () => {
  const fixturesDir = path.join(__dirname, '..', '..', 'mcp-edge-platform', 'fixtures');

  afterEach(() => { delete process.env.HDIM_DEMO_MODE; });

  it('returns fixture when demo mode enabled and fixture exists', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const interceptor = createDemoInterceptor(fixturesDir);
    const realHandler = jest.fn();
    const result = await interceptor('edge_health', {}, realHandler);
    expect(realHandler).not.toHaveBeenCalled();
    expect(result.content[0].text).toContain('healthy');
  });

  it('calls real handler when demo mode disabled', async () => {
    const interceptor = createDemoInterceptor(fixturesDir);
    const realHandler = jest.fn().mockResolvedValue({ content: [{ type: 'text', text: 'real' }] });
    const result = await interceptor('edge_health', {}, realHandler);
    expect(realHandler).toHaveBeenCalled();
    expect(result.content[0].text).toBe('real');
  });

  it('calls real handler when fixture not found in demo mode', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const interceptor = createDemoInterceptor(fixturesDir);
    const realHandler = jest.fn().mockResolvedValue({ content: [{ type: 'text', text: 'fallback' }] });
    const result = await interceptor('nonexistent_tool', {}, realHandler);
    expect(realHandler).toHaveBeenCalled();
    expect(result.content[0].text).toBe('fallback');
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-common/__tests__/demo-mode.test.js
git commit -m "test(mcp-edge): add comprehensive demo mode tests"
```

---

### Task 8: MCP router error handling + JSON-RPC protocol edge cases

**Files:**
- Modify: `mcp-edge-common/__tests__/mcp-router.test.js`

**Step 1: Write tests for untested router paths**

```javascript
describe('JSON-RPC protocol validation', () => {
  it('rejects non-2.0 JSON-RPC version', async () => {
    const res = await request.post('/mcp').send({ jsonrpc: '1.0', id: 1, method: 'initialize' });
    expect(res.body.error.code).toBe(-32600);
  });

  it('rejects missing jsonrpc field', async () => {
    const res = await request.post('/mcp').send({ id: 1, method: 'initialize' });
    expect(res.body.error.code).toBe(-32600);
  });

  it('handles tools/call with missing params', async () => {
    const res = await request.post('/mcp').send({ jsonrpc: '2.0', id: 1, method: 'tools/call' });
    expect(res.body.error.code).toBe(-32602);
  });

  it('returns tool execution error with detail on handler throw', async () => {
    // Create router with a tool that always throws
    const throwingTool = {
      name: 'throwing_tool',
      description: 'test',
      inputSchema: { type: 'object' },
      handler: async () => { throw new Error('deliberate failure'); }
    };
    // ... setup and test, verify error.data.detail contains 'deliberate failure'
  });

  it('handles non-Error throws from tool handler', async () => {
    const stringThrowTool = {
      name: 'string_throw',
      description: 'test',
      inputSchema: { type: 'object' },
      handler: async () => { throw 'a string error'; }
    };
    // ... verify error.data.detail contains 'a string error'
  });
});

describe('notification handling', () => {
  it('returns 204 for notifications/initialized', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/initialized'
    });
    expect(res.status).toBe(204);
  });

  it('returns 204 for notifications/cancelled', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', method: 'notifications/cancelled'
    });
    expect(res.status).toBe(204);
  });
});

describe('role-based authorization in router context', () => {
  // With enforceRoleAuth: true
  it('denies tool call when no x-operator-role header', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'test_tool', arguments: {} }
    });
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('missing_operator_role');
  });

  it('denies tool call for unauthorized role', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({
        jsonrpc: '2.0', id: 1, method: 'tools/call',
        params: { name: 'docker_restart', arguments: {} }
      });
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('forbidden_for_role');
  });
});
```

**Step 2: Run → Verify failures**
**Step 3: Fix any needed implementation adjustments**
**Step 4: Commit**

```bash
git add mcp-edge-common/__tests__/mcp-router.test.js
git commit -m "test(mcp-edge): add MCP router protocol validation and auth tests"
```

---

## Stream C: Platform Sidecar Test Gaps

### Task 9: Platform client — mock fetch, test get/post

This is the biggest test gap. `get()` and `post()` methods have zero tests.

**Files:**
- Rewrite: `mcp-edge-platform/__tests__/platform-client.test.js`

**Step 1: Write comprehensive platform client tests with mocked fetch**

```javascript
const { createPlatformClient } = require('../lib/platform-client');

// Mock global fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('platform-client', () => {
  afterEach(() => mockFetch.mockReset());

  describe('get()', () => {
    it('sends GET request with correct URL', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{"status":"UP"}' });
      const client = createPlatformClient({ baseUrl: 'http://gateway:8080' });
      const result = await client.get('/actuator/health');
      expect(mockFetch).toHaveBeenCalledWith(
        'http://gateway:8080/actuator/health',
        expect.objectContaining({ method: 'GET' })
      );
      expect(result).toEqual({ status: 200, ok: true, body: '{"status":"UP"}', url: 'http://gateway:8080/actuator/health' });
    });

    it('sends Authorization Bearer header when API key set', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: 'test-key-123' });
      await client.get('/health');
      const headers = mockFetch.mock.calls[0][1].headers;
      expect(headers.authorization).toBe('Bearer test-key-123');
    });

    it('omits Authorization header when no API key', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: '' });
      await client.get('/health');
      const headers = mockFetch.mock.calls[0][1].headers;
      expect(headers.authorization).toBeUndefined();
    });

    it('truncates responses over 20KB', async () => {
      const largeBody = 'x'.repeat(25_000);
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => largeBody });
      const client = createPlatformClient();
      const result = await client.get('/large');
      expect(result.body.length).toBeLessThan(21_000);
      expect(result.body).toContain('...[truncated]');
    });

    it('reports non-200 status without throwing', async () => {
      mockFetch.mockResolvedValue({ status: 500, ok: false, text: async () => 'Internal Server Error' });
      const client = createPlatformClient();
      const result = await client.get('/fail');
      expect(result.ok).toBe(false);
      expect(result.status).toBe(500);
    });

    it('uses AbortSignal.timeout for request timeout', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ timeout: 5000 });
      await client.get('/test');
      expect(mockFetch.mock.calls[0][1].signal).toBeDefined();
    });

    it('propagates fetch errors', async () => {
      mockFetch.mockRejectedValue(new Error('ECONNREFUSED'));
      const client = createPlatformClient();
      await expect(client.get('/fail')).rejects.toThrow('ECONNREFUSED');
    });
  });

  describe('post()', () => {
    it('sends POST with JSON body', async () => {
      mockFetch.mockResolvedValue({ status: 201, ok: true, text: async () => '{"created":true}' });
      const client = createPlatformClient();
      const result = await client.post('/api/v1/demo/scenarios/default', { force: true });
      expect(mockFetch.mock.calls[0][1].method).toBe('POST');
      expect(mockFetch.mock.calls[0][1].body).toBe('{"force":true}');
      expect(result.status).toBe(201);
    });

    it('allows per-request API key override', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: 'default-key' });
      await client.post('/test', {}, { apiKey: 'override-key' });
      expect(mockFetch.mock.calls[0][1].headers.authorization).toBe('Bearer override-key');
    });
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/platform-client.test.js
git commit -m "test(mcp-edge): add comprehensive platform client get/post tests with mocked fetch"
```

---

### Task 10: Platform integration tests — full tool round-trips

**Files:**
- Create: `mcp-edge-platform/__tests__/integration.test.js`

**Step 1: Write full HTTP round-trip tests for tool execution**

Test edge_health, platform_info (static tools), and one dynamic tool (service_catalog with mocked client).

```javascript
const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge integration — tool execution', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('tools/call edge_health returns healthy response', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({
        jsonrpc: '2.0', id: 1, method: 'tools/call',
        params: { name: 'edge_health', arguments: {} }
      });
    expect(res.status).toBe(200);
    expect(res.body.jsonrpc).toBe('2.0');
    expect(res.body.id).toBe(1);
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.status).toBe('healthy');
    expect(content.service).toBe('hdim-platform-edge');
  });

  it('tools/call platform_info returns config data', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({
        jsonrpc: '2.0', id: 2, method: 'tools/call',
        params: { name: 'platform_info', arguments: {} }
      });
    const content = JSON.parse(res.body.result.content[0].text);
    expect(content.service).toBe('hdim-platform-edge');
    expect(content.version).toBe('0.1.0');
  });

  it('tools/call service_dependencies returns full graph', async () => {
    // This tool is on devops, use devops app for this test
  });

  it('unknown tool returns -32602', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({
        jsonrpc: '2.0', id: 3, method: 'tools/call',
        params: { name: 'nonexistent', arguments: {} }
      });
    expect(res.body.error.code).toBe(-32602);
  });

  it('missing operator role returns -32603 when enforce enabled', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });
    expect(res.body.error.code).toBe(-32603);
    expect(res.body.error.data.reason).toBe('missing_operator_role');
  });

  it('executive role can access dashboard_stats but not fhir_metadata', async () => {
    const allowed = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({
        jsonrpc: '2.0', id: 5, method: 'tools/call',
        params: { name: 'edge_health', arguments: {} }
      });
    expect(allowed.body.result).toBeDefined();
    expect(allowed.body.error).toBeUndefined();

    const denied = await request.post('/mcp')
      .set('x-operator-role', 'executive')
      .send({
        jsonrpc: '2.0', id: 6, method: 'tools/call',
        params: { name: 'fhir_metadata', arguments: {} }
      });
    expect(denied.body.error).toBeDefined();
    expect(denied.body.error.data.reason).toBe('forbidden_for_role');
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/integration.test.js
git commit -m "test(mcp-edge): add platform full tool execution integration tests"
```

---

### Task 11: Platform server security header + request limit tests

**Files:**
- Create: `mcp-edge-platform/__tests__/security.test.js`

**Step 1: Write security-focused tests**

```javascript
const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge security', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('sets security headers via helmet', async () => {
    const res = await request.get('/health');
    expect(res.headers['x-content-type-options']).toBe('nosniff');
    expect(res.headers['x-frame-options']).toBeDefined();
  });

  it('rejects request body over 1mb', async () => {
    const largeBody = { data: 'x'.repeat(1_100_000) };
    const res = await request.post('/mcp')
      .set('content-type', 'application/json')
      .send(JSON.stringify(largeBody));
    expect(res.status).toBe(413);
  });

  it('handles malformed JSON gracefully', async () => {
    const res = await request.post('/mcp')
      .set('content-type', 'application/json')
      .send('not json at all');
    expect(res.status).toBe(400);
  });

  it('returns CORS headers', async () => {
    const res = await request.options('/mcp').set('Origin', 'http://evil.com');
    expect(res.headers['access-control-allow-origin']).toBeDefined();
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/security.test.js
git commit -m "test(mcp-edge): add platform security header and request limit tests"
```

---

## Stream D: DevOps Sidecar Test Gaps

### Task 12: Docker client — mock spawn, test all methods

**Files:**
- Rewrite: `mcp-edge-devops/__tests__/docker-client.test.js`

**Step 1: Write tests that mock child_process.spawn**

```javascript
jest.mock('node:child_process', () => ({
  spawn: jest.fn()
}));

const { spawn } = require('node:child_process');
const { createDockerClient, runCommand } = require('../lib/docker-client');
const EventEmitter = require('node:events');

function createMockProcess(exitCode = 0, stdout = '', stderr = '') {
  const proc = new EventEmitter();
  proc.stdout = new EventEmitter();
  proc.stderr = new EventEmitter();
  proc.kill = jest.fn();

  setImmediate(() => {
    if (stdout) proc.stdout.emit('data', Buffer.from(stdout));
    if (stderr) proc.stderr.emit('data', Buffer.from(stderr));
    proc.emit('close', exitCode);
  });
  return proc;
}

describe('runCommand', () => {
  it('resolves with ok:true on exit code 0', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'output'));
    const result = await runCommand('docker', ['ps']);
    expect(result.ok).toBe(true);
    expect(result.stdout).toBe('output');
    expect(result.exitCode).toBe(0);
  });

  it('resolves with ok:false on non-zero exit', async () => {
    spawn.mockReturnValue(createMockProcess(1, '', 'error msg'));
    const result = await runCommand('docker', ['ps']);
    expect(result.ok).toBe(false);
    expect(result.stderr).toBe('error msg');
  });

  it('resolves with ok:false and timedOut:true on timeout', async () => {
    const proc = new EventEmitter();
    proc.stdout = new EventEmitter();
    proc.stderr = new EventEmitter();
    proc.kill = jest.fn(() => { proc.emit('close', null); });
    spawn.mockReturnValue(proc);

    const result = await runCommand('sleep', ['100'], { timeoutMs: 50 });
    expect(result.ok).toBe(false);
    expect(result.timedOut).toBe(true);
    expect(proc.kill).toHaveBeenCalledWith('SIGTERM');
  });

  it('resolves with ok:false on spawn error', async () => {
    const proc = new EventEmitter();
    proc.stdout = new EventEmitter();
    proc.stderr = new EventEmitter();
    proc.kill = jest.fn();
    spawn.mockReturnValue(proc);
    setImmediate(() => proc.emit('error', new Error('ENOENT')));

    const result = await runCommand('nonexistent', []);
    expect(result.ok).toBe(false);
    expect(result.stderr).toContain('ENOENT');
  });
});

describe('dockerClient methods', () => {
  it('ps() calls docker compose ps --format json', async () => {
    spawn.mockReturnValue(createMockProcess(0, '[{"Service":"pg","State":"running"}]'));
    const client = createDockerClient({ composeFile: 'dc.yml', dockerBin: 'docker' });
    const result = await client.ps();
    expect(result.ok).toBe(true);
    expect(result.services).toHaveLength(1);
    expect(spawn).toHaveBeenCalledWith('docker',
      ['compose', '-f', 'dc.yml', 'ps', '--format', 'json'],
      expect.any(Object)
    );
  });

  it('logs() calls docker compose logs with tail', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'log output'));
    const client = createDockerClient({ composeFile: 'dc.yml' });
    await client.logs('postgres', 50);
    expect(spawn).toHaveBeenCalledWith(expect.any(String),
      expect.arrayContaining(['logs', 'postgres', '--tail', '50']),
      expect.any(Object)
    );
  });

  it('restart() calls docker compose restart with 60s timeout', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'restarted'));
    const client = createDockerClient();
    await client.restart('redis');
    expect(spawn).toHaveBeenCalledWith(expect.any(String),
      expect.arrayContaining(['restart', 'redis']),
      expect.objectContaining({ cwd: expect.any(String) })
    );
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-devops/__tests__/docker-client.test.js
git commit -m "test(mcp-edge): add docker client runCommand and method tests with mocked spawn"
```

---

### Task 13: DevOps integration tests — full tool round-trips

**Files:**
- Create: `mcp-edge-devops/__tests__/integration.test.js`

Same pattern as Task 10 but for devops. Test:
- `edge_health` tool execution round-trip
- `service_dependencies` tool execution (static, no external deps)
- Unknown tool → `-32602`
- Missing role → `-32603`
- Role-restricted access (executive can't use docker_restart)

**Step 1: Write, Step 2: Run → PASS, Step 3: Commit**

```bash
git add mcp-edge-devops/__tests__/integration.test.js
git commit -m "test(mcp-edge): add devops full tool execution integration tests"
```

---

### Task 14: DevOps server security tests

Same pattern as Task 11 but for devops sidecar.

**Files:**
- Create: `mcp-edge-devops/__tests__/security.test.js`

**Step 1: Write, Step 2: Run → PASS, Step 3: Commit**

```bash
git add mcp-edge-devops/__tests__/security.test.js
git commit -m "test(mcp-edge): add devops security header and request limit tests"
```

---

## Stream E: Security Boundary Tests (depends on Stream A)

### Task 15: Cross-sidecar isolation boundary proof

This is the CISO proof. Each sidecar only exposes its designated tools. The platform sidecar has no Docker commands. The devops sidecar has no PHI-adjacent tools.

**Files:**
- Create: `mcp-edge-platform/__tests__/boundary.test.js`
- Create: `mcp-edge-devops/__tests__/boundary.test.js`

**Step 1: Write boundary tests**

```javascript
// mcp-edge-platform/__tests__/boundary.test.js
describe('platform edge isolation boundary', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  const PLATFORM_TOOLS = [
    'edge_health', 'platform_health', 'platform_info',
    'fhir_metadata', 'service_catalog', 'dashboard_stats',
    'demo_status', 'demo_seed'
  ];

  const DEVOPS_TOOLS = [
    'docker_status', 'docker_logs', 'docker_restart',
    'service_dependencies', 'compose_config', 'build_status'
  ];

  it('exposes exactly the 8 platform tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    const names = res.body.result.tools.map(t => t.name).sort();
    expect(names).toEqual(PLATFORM_TOOLS.sort());
  });

  it.each(DEVOPS_TOOLS)('does NOT expose devops tool %s', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({
        jsonrpc: '2.0', id: 1, method: 'tools/call',
        params: { name: tool, arguments: {} }
      });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.code).toBe(-32602);
  });
});

// mcp-edge-devops/__tests__/boundary.test.js — mirror for devops
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/boundary.test.js mcp-edge-devops/__tests__/boundary.test.js
git commit -m "test(mcp-edge): add CISO isolation boundary proof tests"
```

---

### Task 16: Role-based access control exhaustive matrix

Test every role × every tool combination. This is the audit-ready proof.

**Files:**
- Create: `mcp-edge-platform/__tests__/rbac-matrix.test.js`

**Step 1: Write exhaustive RBAC matrix**

```javascript
// Define the full matrix from the design doc
const ROLE_TOOL_MATRIX = {
  platform_admin:   { edge_health: true, platform_health: true, platform_info: true, fhir_metadata: true, service_catalog: true, dashboard_stats: true, demo_status: true, demo_seed: true },
  developer:        { edge_health: true, platform_health: true, platform_info: true, fhir_metadata: true, service_catalog: true, dashboard_stats: true, demo_status: true, demo_seed: true },
  clinical_admin:   { edge_health: true, platform_health: true, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  quality_officer:  { edge_health: true, platform_health: false, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  executive:        { edge_health: true, platform_health: false, platform_info: true, fhir_metadata: false, service_catalog: false, dashboard_stats: true, demo_status: false, demo_seed: false },
  clinician:        { edge_health: true, platform_health: true, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: false, demo_status: false, demo_seed: false },
  care_coordinator: { edge_health: true, platform_health: false, platform_info: false, fhir_metadata: false, service_catalog: false, dashboard_stats: false, demo_status: false, demo_seed: false },
};

describe('RBAC exhaustive matrix — platform edge', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  for (const [role, tools] of Object.entries(ROLE_TOOL_MATRIX)) {
    for (const [tool, allowed] of Object.entries(tools)) {
      it(`${role} ${allowed ? 'CAN' : 'CANNOT'} call ${tool}`, async () => {
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
    }
  }
});
```

This generates 56 test cases (7 roles × 8 tools) from a single declarative matrix.

**Step 2: Run → PASS (after Task 1 role fix)**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/rbac-matrix.test.js
git commit -m "test(mcp-edge): add exhaustive RBAC matrix — 56 role×tool test cases"
```

---

### Task 17: Error response PHI leak detection

Healthcare-specific: verify that no error response contains PHI-adjacent data, API keys, or stack traces.

**Files:**
- Create: `mcp-edge-platform/__tests__/phi-leak.test.js`

**Step 1: Write PHI leak detection tests**

```javascript
describe('error response PHI leak detection', () => {
  const SENSITIVE_PATTERNS = [
    /patient/i,
    /ssn|social.security/i,
    /mrn|medical.record/i,
    /dob|date.of.birth/i,
    /Bearer\s+\S{10,}/,   // API keys in error messages
    /stack.*at\s+\S+\.js/, // Stack traces
    /password/i,
    /secret/i,
  ];

  it('unknown tool error contains no sensitive data', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'unknown', arguments: {} } });
    const body = JSON.stringify(res.body);
    for (const pattern of SENSITIVE_PATTERNS) {
      expect(body).not.toMatch(pattern);
    }
  });

  it('forbidden error contains no API key', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'care_coordinator')
      .set('authorization', 'Bearer hdim_secret_key_12345')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'fhir_metadata', arguments: {} } });
    expect(JSON.stringify(res.body)).not.toContain('hdim_secret_key');
  });

  it('tool execution error contains no stack trace', async () => {
    // edge_health is static so won't throw, but this tests the error wrapper
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'nonexistent', arguments: {} } });
    expect(JSON.stringify(res.body)).not.toMatch(/at\s+\w+\s+\(.*\.js:\d+:\d+\)/);
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/phi-leak.test.js
git commit -m "test(mcp-edge): add PHI leak detection tests for error responses"
```

---

## Stream F: DevOps Demo Fixtures

### Task 18: Create devops demo fixtures

Code review noted devops has no fixtures directory.

**Files:**
- Create: `mcp-edge-devops/fixtures/edge_health.json`
- Create: `mcp-edge-devops/fixtures/docker_status.json`
- Create: `mcp-edge-devops/fixtures/docker_logs.json`
- Create: `mcp-edge-devops/fixtures/docker_restart.json`
- Create: `mcp-edge-devops/fixtures/service_dependencies.json`
- Create: `mcp-edge-devops/fixtures/compose_config.json`
- Create: `mcp-edge-devops/fixtures/build_status.json`

**Step 1: Create all 7 fixture files**

```json
// edge_health.json
{ "status": "healthy", "service": "hdim-devops-edge", "version": "0.1.0", "uptime": 7200, "demoMode": true }

// docker_status.json
{ "ok": true, "services": [
  {"Service":"postgres","State":"running","Status":"Up 2 hours"},
  {"Service":"redis","State":"running","Status":"Up 2 hours"},
  {"Service":"gateway-edge","State":"running","Status":"Up 1 hour"}
]}

// docker_logs.json
{ "ok": true, "service": "gateway-edge", "logs": "[2026-03-04T10:00:00] INFO  Started GatewayApplication in 4.2s\n[2026-03-04T10:00:01] INFO  Listening on port 18080" }

// docker_restart.json
{ "ok": true, "service": "gateway-edge", "stdout": "gateway-edge\n" }

// service_dependencies.json
{ "dependencies": { "gateway-edge": ["gateway-admin-service"], "fhir-service": ["postgres"] } }

// compose_config.json
{ "ok": true, "composeFile": "docker-compose.demo.yml", "config": "services:\n  postgres:\n    image: postgres:16\n" }

// build_status.json
{ "ok": true, "affectedProjects": ["gateway-edge", "fhir-service"], "count": 2 }
```

**Step 2: Commit**

```bash
git add mcp-edge-devops/fixtures/
git commit -m "feat(mcp-edge): add devops demo mode fixtures for all 7 tools"
```

---

### Task 19: Demo mode integration test — both sidecars

**Files:**
- Create: `mcp-edge-platform/__tests__/demo-mode-integration.test.js`
- Create: `mcp-edge-devops/__tests__/demo-mode-integration.test.js`

**Step 1: Write demo mode integration tests**

For each sidecar, start the app with `HDIM_DEMO_MODE=true`, call every tool, verify fixture data is returned instead of real gateway/docker calls.

```javascript
describe('platform edge demo mode — all tools return fixtures', () => {
  let request;
  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    request = supertest(createApp());
  });
  afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

  const TOOLS_WITH_FIXTURES = [
    'edge_health', 'platform_health', 'fhir_metadata',
    'service_catalog', 'dashboard_stats', 'demo_status',
    'demo_seed', 'platform_info'
  ];

  it.each(TOOLS_WITH_FIXTURES)('tool %s returns fixture data in demo mode', async (tool) => {
    const params = tool === 'demo_seed'
      ? { name: tool, arguments: { scenarioName: 'default' } }
      : { name: tool, arguments: {} };
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params });
    expect(res.body.error).toBeUndefined();
    expect(res.body.result.content).toHaveLength(1);
    // Fixture data should parse as valid JSON
    expect(() => JSON.parse(res.body.result.content[0].text)).not.toThrow();
  });
});
```

**Step 2: Run → PASS (after Task 4 wires interceptor)**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/demo-mode-integration.test.js mcp-edge-devops/__tests__/demo-mode-integration.test.js
git commit -m "test(mcp-edge): add demo mode integration tests for both sidecars"
```

---

## Stream G: Contract & Bridge Tests

### Task 20: JSON-RPC response contract validation

Every tool must return `{ content: [{ type: 'text', text: <valid-json-string> }] }`. This is a contract test — schema validation of every tool response.

**Files:**
- Create: `mcp-edge-platform/__tests__/contract.test.js`
- Create: `mcp-edge-devops/__tests__/contract.test.js`

**Step 1: Write contract tests**

```javascript
describe('MCP tool response contract', () => {
  const TOOLS = ['edge_health', 'platform_info']; // static tools that don't need mocks

  it.each(TOOLS)('tool %s returns MCP-compliant response', async (tool) => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: tool, arguments: {} } });

    // JSON-RPC envelope
    expect(res.body.jsonrpc).toBe('2.0');
    expect(res.body.id).toBe(1);
    expect(res.body.error).toBeUndefined();

    // MCP tool response shape
    const result = res.body.result;
    expect(result.content).toBeInstanceOf(Array);
    expect(result.content.length).toBeGreaterThanOrEqual(1);
    expect(result.content[0].type).toBe('text');
    expect(typeof result.content[0].text).toBe('string');

    // Tool output is valid JSON
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed).toBeDefined();
    expect(typeof parsed).toBe('object');
  });

  it('tools/list response has correct schema for all tools', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/list', params: {}
    });
    for (const tool of res.body.result.tools) {
      expect(tool.name).toMatch(/^[a-z][a-z0-9_]*$/);
      expect(typeof tool.description).toBe('string');
      expect(tool.description.length).toBeGreaterThan(0);
      expect(tool.inputSchema).toBeDefined();
      expect(tool.inputSchema.type).toBe('object');
    }
  });

  it('error responses follow JSON-RPC error schema', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 99, method: 'tools/call',
      params: { name: 'nonexistent' }
    }).set('x-operator-role', 'platform_admin');
    expect(res.body.jsonrpc).toBe('2.0');
    expect(res.body.id).toBe(99);
    expect(res.body.error.code).toEqual(expect.any(Number));
    expect(typeof res.body.error.message).toBe('string');
  });
});
```

**Step 2: Run → PASS**
**Step 3: Commit**

```bash
git add mcp-edge-platform/__tests__/contract.test.js mcp-edge-devops/__tests__/contract.test.js
git commit -m "test(mcp-edge): add MCP response contract validation tests"
```

---

### Task 21: Bridge stdio parsing tests

Code review noted bridge scripts have zero tests. Design doc requires "Bridge tests: stdio parsing, tool name mapping."

**Files:**
- Create: `scripts/mcp/__tests__/bridge-helpers.test.mjs`

Test the pure functions: `toVscodeSafeToolName`, `registerToolAlias`, `mapRequestForEdge`, `mapResponseForVscode`. These functions are currently inlined in the bridge files — extract them for testability.

**Step 1: Extract bridge helpers to a shared module**

Create `scripts/mcp/bridge-helpers.mjs` with the pure functions extracted from the bridge.

**Step 2: Write tests**

```javascript
import { toVscodeSafeToolName, registerToolAlias, mapRequestForEdge, mapResponseForVscode } from '../bridge-helpers.mjs';

describe('bridge helpers', () => {
  describe('toVscodeSafeToolName', () => {
    it('passes through valid names', () => {
      expect(toVscodeSafeToolName('edge_health')).toBe('edge_health');
    });
    it('replaces dots with underscores', () => {
      expect(toVscodeSafeToolName('tool.name')).toBe('tool_name');
    });
    it('replaces special chars', () => {
      expect(toVscodeSafeToolName('tool@name!')).toBe('tool_name_');
    });
  });

  describe('mapResponseForVscode', () => {
    it('aliases tool names in tools/list response', () => {
      const response = {
        result: {
          tools: [{ name: 'edge_health', description: 'test' }]
        }
      };
      const mapped = mapResponseForVscode(response);
      expect(mapped.result.tools[0].name).toBe('edge_health');
    });

    it('passes through non-tools responses unchanged', () => {
      const response = { result: { protocolVersion: '2025-11-25' } };
      expect(mapResponseForVscode(response)).toEqual(response);
    });
  });

  describe('mapRequestForEdge', () => {
    it('maps aliased tool name back to original in tools/call', () => {
      // Register an alias first
      registerToolAlias('original.tool');
      const request = { method: 'tools/call', params: { name: 'original_tool' } };
      const mapped = mapRequestForEdge(request);
      expect(mapped.params.name).toBe('original.tool');
    });

    it('passes through non-tools/call methods', () => {
      const request = { method: 'initialize', params: {} };
      expect(mapRequestForEdge(request)).toEqual(request);
    });
  });
});
```

**Step 3: Run → PASS**
**Step 4: Update bridge files to import from bridge-helpers.mjs**
**Step 5: Commit**

```bash
git add scripts/mcp/bridge-helpers.mjs scripts/mcp/__tests__/bridge-helpers.test.mjs scripts/mcp/mcp-edge-platform-bridge.mjs scripts/mcp/mcp-edge-devops-bridge.mjs
git commit -m "test(mcp-edge): extract bridge helpers, add stdio bridge tests"
```

---

## Final Validation

### Task 22: Run full test suite, generate coverage report

**Files:** None (test execution only)

**Step 1: Run all tests across all packages**

```bash
cd mcp-edge-common && npx jest --coverage --verbose 2>&1 | tee /tmp/common-coverage.txt
cd mcp-edge-platform && npx jest --coverage --verbose 2>&1 | tee /tmp/platform-coverage.txt
cd mcp-edge-devops && npx jest --coverage --verbose 2>&1 | tee /tmp/devops-coverage.txt
```

**Step 2: Verify all tests pass and coverage meets targets**

Coverage targets:
- Statements: >90%
- Branches: >85%
- Functions: >95%
- Lines: >90%

**Step 3: Fix any failing tests**

**Step 4: Commit coverage config if needed**

---

### Task 23: Write validation summary report

**Files:**
- Create: `docs/plans/2026-03-04-hdim-mcp-edge-validation-report.md`

Document:
1. Test count by category (unit, integration, security, contract, RBAC, PHI-leak, demo, bridge)
2. Coverage percentages per package
3. Code review findings addressed (checklist)
4. Isolation boundary proof (CISO evidence)
5. RBAC matrix proof (56 test cases)
6. Demo mode verification
7. Remaining items for Layer 2-4

**Commit:**

```bash
git add docs/plans/2026-03-04-hdim-mcp-edge-validation-report.md
git commit -m "docs(mcp-edge): add testing and validation proof report"
```

---

## Verification Checklist

After all streams complete:

- [ ] `cd mcp-edge-common && npx jest --verbose` — all tests pass including new auth matrix
- [ ] `cd mcp-edge-platform && npx jest --verbose` — all tests pass including integration, security, RBAC, contract, PHI-leak, demo-mode, boundary
- [ ] `cd mcp-edge-devops && npx jest --verbose` — all tests pass including integration, security, contract, demo-mode, boundary
- [ ] Role policy in auth.js matches design doc exactly (with $ anchors)
- [ ] docker_restart validates service names
- [ ] docker_logs validates service names and tail values
- [ ] Demo mode interceptor is wired and returns fixtures
- [ ] DevOps fixtures directory exists with all 7 fixture files
- [ ] Bridge helpers extracted and tested
- [ ] All error responses free of PHI/secrets/stack traces
- [ ] RBAC matrix tests cover all 7 roles × 8 platform tools (56 cases)
- [ ] Isolation boundary tests prove sidecars don't cross
- [ ] Coverage report generated with >90% line coverage
