# PHI Audit Integration — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire phi-audit.js into the MCP router so every clinical tool call emits structured PHI access events (PHI_ACCESS, PHI_WRITE, TOOL_CALL, AUTH_DENIED), with tool-level audit annotations declaring each tool's PHI classification.

**Architecture:** Each clinical tool definition carries an `audit: { phi, write, patientIdArg }` field. The shared MCP router accepts an optional `phiAuditLogger` duck-typed object and fires it at 4 hook points (auth-denied, demo-success, handler-success, handler-error). When `phiAuditLogger` is absent (platform/devops), all audit calls short-circuit via optional chaining. Clinical `server.js` instantiates `createPhiAuditLogger()` and passes it to the router.

**Tech Stack:** Node.js 20, Express 4, Jest 30, Pino 10, Supertest 7

**Design Doc:** `docs/plans/2026-03-05-phi-audit-integration-design.md`

---

## Parallel Execution Map

Tasks are grouped into parallel streams. Tasks within a stream are sequential.
Streams can run concurrently via multi-agent teams.

```
Stream A (common router):           Task 1 → 2 → 3
Stream B (phi-audit.js fix):        Task 4
Stream C (composite annotations):   Task 5 → 6
Stream D (high-value annotations):  Task 7
Stream E (full-surface annotations): Task 8 → 9
Stream F (server wiring):           Task 10
Stream G (integration + proofs):    Task 11 → 12 → 13

Dependencies:
  - Stream C, D, E depend on Stream A completing Task 1
  - Stream F depends on Streams A, B, C, D, E all completing
  - Stream G depends on Stream F completing
```

---

## Stream A: MCP Router PHI Audit Hooks

### Task 1: Add extractPhiContext helper and phiAuditLogger option to router

**Files:**
- Modify: `mcp-edge-common/lib/mcp-router.js`
- Test: `mcp-edge-common/__tests__/mcp-router.test.js`

**Step 1: Write failing tests for extractPhiContext and phiAuditLogger hooks**

Add to `mcp-edge-common/__tests__/mcp-router.test.js`:

```javascript
describe('phiAuditLogger hooks', () => {
  let app, request, phiAuditLogger;

  const phiTool = {
    name: 'phi_tool',
    description: 'A PHI tool',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string' },
        tenantId: { type: 'string' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false, patientIdArg: 'patientId' },
    handler: async () => ({ content: [{ type: 'text', text: '{"ok":true}' }] })
  };

  const writeTool = {
    name: 'write_tool',
    description: 'A PHI write tool',
    inputSchema: {
      type: 'object',
      properties: { tenantId: { type: 'string' } },
      required: ['tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: true },
    handler: async () => ({ content: [{ type: 'text', text: '{"ok":true}' }] })
  };

  const nonPhiTool = {
    name: 'stats_tool',
    description: 'A non-PHI tool',
    inputSchema: {
      type: 'object',
      properties: { tenantId: { type: 'string' } },
      required: ['tenantId'],
      additionalProperties: false
    },
    handler: async () => ({ content: [{ type: 'text', text: '{"ok":true}' }] })
  };

  const throwingTool = {
    name: 'throw_tool',
    description: 'A tool that throws',
    inputSchema: {
      type: 'object',
      properties: { tenantId: { type: 'string' } },
      required: ['tenantId'],
      additionalProperties: false
    },
    audit: { phi: true, write: false },
    handler: async () => { throw new Error('boom'); }
  };

  beforeAll(() => {
    phiAuditLogger = {
      logToolAccess: jest.fn(),
      logAuthDenied: jest.fn()
    };
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [phiTool, writeTool, nonPhiTool, throwingTool],
      serverName: 'test-edge',
      serverVersion: '0.1.0',
      enforceRoleAuth: true,
      phiAuditLogger
    }));
    request = supertest(app);
  });

  beforeEach(() => {
    phiAuditLogger.logToolAccess.mockClear();
    phiAuditLogger.logAuthDenied.mockClear();
  });

  it('calls logToolAccess with phi:true on PHI tool success', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 1, method: 'tools/call',
        params: { name: 'phi_tool', arguments: { patientId: 'p-1', tenantId: 't-1' } } });

    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({
        tool: 'phi_tool', role: 'platform_admin', tenantId: 't-1',
        patientId: 'p-1', success: true, phi: true, write: false
      })
    );
  });

  it('calls logToolAccess with write:true on PHI write tool', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/call',
        params: { name: 'write_tool', arguments: { tenantId: 't-1' } } });

    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ tool: 'write_tool', phi: true, write: true, patientId: undefined })
    );
  });

  it('calls logToolAccess with phi:false on non-PHI tool', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 3, method: 'tools/call',
        params: { name: 'stats_tool', arguments: { tenantId: 't-1' } } });

    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ tool: 'stats_tool', phi: false, write: false })
    );
  });

  it('calls logToolAccess with success:false on tool error', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 4, method: 'tools/call',
        params: { name: 'throw_tool', arguments: { tenantId: 't-1' } } });

    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ tool: 'throw_tool', success: false, phi: true })
    );
  });

  it('calls logAuthDenied on forbidden tool call', async () => {
    await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 5, method: 'tools/call',
        params: { name: 'phi_tool', arguments: { patientId: 'p-1', tenantId: 't-1' } } });

    expect(phiAuditLogger.logAuthDenied).toHaveBeenCalledWith(
      expect.objectContaining({ tool: 'phi_tool' })
    );
    expect(phiAuditLogger.logToolAccess).not.toHaveBeenCalled();
  });

  it('does not call phiAuditLogger for unknown tools', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 6, method: 'tools/call',
        params: { name: 'nonexistent', arguments: {} } });

    expect(phiAuditLogger.logToolAccess).not.toHaveBeenCalled();
    expect(phiAuditLogger.logAuthDenied).not.toHaveBeenCalled();
  });

  it('includes durationMs as a positive number', async () => {
    await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 7, method: 'tools/call',
        params: { name: 'phi_tool', arguments: { patientId: 'p-1', tenantId: 't-1' } } });

    const call = phiAuditLogger.logToolAccess.mock.calls[0][0];
    expect(typeof call.durationMs).toBe('number');
    expect(call.durationMs).toBeGreaterThanOrEqual(0);
  });
});
```

**Step 2: Run tests to verify they fail**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-common && npx jest --no-coverage __tests__/mcp-router.test.js -t "phiAuditLogger"`
Expected: FAIL — phiAuditLogger.logToolAccess never called

**Step 3: Implement router changes**

In `mcp-edge-common/lib/mcp-router.js`, add `phiAuditLogger` to the destructured options and add `extractPhiContext` helper:

```javascript
function createMcpRouter({ tools, serverName, serverVersion, enforceRoleAuth = true, fixturesDir, logger, rolePolicies, phiAuditLogger }) {
  // ... existing code ...
```

Add private helper before `createMcpRouter`:

```javascript
function extractPhiContext(tool, args) {
  const a = tool.audit;
  if (!a) return { phi: false, write: false, patientId: undefined };
  const patientId = a.patientIdArg ? (args?.[a.patientIdArg] ?? undefined) : undefined;
  return { phi: !!a.phi, write: !!a.write, patientId };
}
```

In `handleToolsCall`, add 4 hooks:

**Hook 1 — after auth denial (after `logger.warn`):**
```javascript
if (phiAuditLogger) phiAuditLogger.logAuthDenied({ tool: name, role });
```

**Hook 2 — after demo mode success (after `logger.info` in demo branch):**
```javascript
if (phiAuditLogger) {
  const ctx = extractPhiContext(tool, args);
  phiAuditLogger.logToolAccess({
    tool: name, role, tenantId: args?.tenantId ?? null,
    patientId: ctx.patientId, success: true, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
  });
}
```

**Hook 3 — after handler success (after `logger.info` in try block):**
```javascript
if (phiAuditLogger) {
  const ctx = extractPhiContext(tool, args);
  phiAuditLogger.logToolAccess({
    tool: name, role, tenantId: (args || {}).tenantId ?? null,
    patientId: ctx.patientId, success: true, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
  });
}
```

**Hook 4 — after handler error (after `logger.error` in catch block):**
```javascript
if (phiAuditLogger) {
  const ctx = extractPhiContext(tool, args);
  phiAuditLogger.logToolAccess({
    tool: name, role, tenantId: (args || {}).tenantId ?? null,
    patientId: ctx.patientId, success: false, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
  });
}
```

**Step 4: Run tests to verify they pass**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-common && npx jest --no-coverage __tests__/mcp-router.test.js`
Expected: ALL PASS

**Step 5: Run full common suite for regression**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-common && npx jest --no-coverage`
Expected: ALL PASS (224+ tests)

**Step 6: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-common/lib/mcp-router.js mcp-edge-common/__tests__/mcp-router.test.js
git commit -m "feat(mcp-edge-common): add phiAuditLogger hooks to MCP router

Add optional phiAuditLogger option to createMcpRouter with 4 hook points:
auth-denied, demo-success, handler-success, handler-error. Extract PHI
context from tool.audit annotation. Zero-cost for non-clinical sidecars."
```

---

### Task 2: Verify platform and devops are unaffected

**Files:**
- None modified — regression only

**Step 1: Run platform tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-platform && npx jest --no-coverage`
Expected: ALL PASS (170 tests)

**Step 2: Run devops tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-devops && npx jest --no-coverage`
Expected: ALL PASS (203 tests)

---

### Task 3: Add extractPhiContext unit tests

**Files:**
- Test: `mcp-edge-common/__tests__/mcp-router.test.js`

**Step 1: Write targeted extractPhiContext edge case tests**

Add to the existing test file:

```javascript
describe('extractPhiContext via tool calls', () => {
  let app, request, phiAuditLogger;

  beforeAll(() => {
    phiAuditLogger = { logToolAccess: jest.fn(), logAuthDenied: jest.fn() };
    const toolWithoutAudit = {
      name: 'no_audit_tool',
      description: 'Tool without audit field',
      inputSchema: { type: 'object', properties: {}, additionalProperties: false },
      handler: async () => ({ content: [{ type: 'text', text: '{}' }] })
    };
    const toolWithNullPatientArg = {
      name: 'null_patient_tool',
      description: 'PHI tool where patient arg is missing from call',
      inputSchema: {
        type: 'object',
        properties: {
          tenantId: { type: 'string' },
          patient: { type: 'string' }
        },
        required: ['tenantId'],
        additionalProperties: false
      },
      audit: { phi: true, write: false, patientIdArg: 'patient' },
      handler: async () => ({ content: [{ type: 'text', text: '{}' }] })
    };
    app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [toolWithoutAudit, toolWithNullPatientArg],
      serverName: 'test-edge', serverVersion: '0.1.0',
      enforceRoleAuth: false, phiAuditLogger
    }));
    request = supertest(app);
  });

  beforeEach(() => { phiAuditLogger.logToolAccess.mockClear(); });

  it('treats missing audit field as non-PHI', async () => {
    await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'no_audit_tool', arguments: {} }
    });
    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ phi: false, write: false, patientId: undefined })
    );
  });

  it('emits undefined patientId when optional patient arg is missing', async () => {
    await request.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/call',
      params: { name: 'null_patient_tool', arguments: { tenantId: 't-1' } }
    });
    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ phi: true, patientId: undefined })
    );
  });

  it('emits patientId when optional patient arg is present', async () => {
    await request.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'null_patient_tool', arguments: { tenantId: 't-1', patient: 'p-99' } }
    });
    expect(phiAuditLogger.logToolAccess).toHaveBeenCalledWith(
      expect.objectContaining({ phi: true, patientId: 'p-99' })
    );
  });
});
```

**Step 2: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-common && npx jest --no-coverage __tests__/mcp-router.test.js`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-common/__tests__/mcp-router.test.js
git commit -m "test(mcp-edge-common): add extractPhiContext edge case tests"
```

---

## Stream B: phi-audit.js — Persist phi Field

### Task 4: Add phi field to audit log output

**Files:**
- Modify: `mcp-edge-clinical/lib/phi-audit.js`
- Modify: `mcp-edge-clinical/__tests__/phi-audit.test.js`

**Step 1: Write failing test**

Add to `phi-audit.test.js` inside `describe('logToolAccess()')`:

```javascript
it('persists phi field in log output', () => {
  logger.logToolAccess({
    tool: 'patient_summary', role: 'clinician', tenantId: 'acme',
    patientId: 'p-1', success: true, durationMs: 100, phi: true
  });
  expect(logged[0].phi).toBe(true);
});

it('persists phi:false in log output for non-PHI tools', () => {
  logger.logToolAccess({
    tool: 'care_gap_stats', role: 'admin', tenantId: 'acme',
    success: true, durationMs: 50, phi: false
  });
  expect(logged[0].phi).toBe(false);
});
```

**Step 2: Run tests to verify failure**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage __tests__/phi-audit.test.js -t "persists phi"`
Expected: FAIL — `logged[0].phi` is undefined

**Step 3: Fix phi-audit.js to persist phi field**

In `mcp-edge-clinical/lib/phi-audit.js`, change the `logToolAccess` function:

```javascript
function logToolAccess({ tool, role, tenantId, patientId, success, durationMs, phi, write }) {
  const action = !phi ? 'TOOL_CALL' : write ? 'PHI_WRITE' : 'PHI_ACCESS';
  const record = { action, source: name, tool, role, tenantId, success, durationMs, phi: !!phi };
  if (phi && patientId) record.patientId = patientId;
  pino.info(record);
}
```

The change: `{ action, source: name, tool, role, tenantId, success, durationMs }` becomes `{ action, source: name, tool, role, tenantId, success, durationMs, phi: !!phi }`.

**Step 4: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage __tests__/phi-audit.test.js`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add mcp-edge-clinical/lib/phi-audit.js mcp-edge-clinical/__tests__/phi-audit.test.js
git commit -m "feat(mcp-edge-clinical): persist phi boolean in audit log output"
```

---

## Stream C: Composite Strategy Annotations

### Task 5: Annotate composite tools (13 PHI-read tools)

**Files:**
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/patient-summary.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/patient-timeline.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/patient-risk.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/patient-list.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/pre-visit-plan.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-list.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-identify.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-provider.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/measure-evaluate.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/measure-results.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/measure-score.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/cds-patient-view.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/health-score.js`

**Step 1: Add `audit` field to each tool definition**

For each tool that returns `{ name, description, inputSchema, handler }`, add `audit` after `inputSchema`:

Tools with `patientId` arg:
```javascript
// patient-summary.js, patient-timeline.js, patient-risk.js, pre-visit-plan.js,
// care-gap-list.js, care-gap-identify.js, measure-evaluate.js, measure-results.js,
// measure-score.js, cds-patient-view.js, health-score.js
audit: { phi: true, write: false, patientIdArg: 'patientId' },
```

Tools without `patientId` arg:
```javascript
// patient-list.js (returns patient list — PHI but no single patient scope)
audit: { phi: true, write: false },

// care-gap-provider.js (returns provider-level gaps — may contain patient refs)
audit: { phi: true, write: false },
```

**Step 2: Run existing tests (regression)**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS — adding `audit` field doesn't break anything

**Step 3: Commit**

```bash
git add mcp-edge-clinical/lib/strategies/composite/tools/patient-summary.js \
  mcp-edge-clinical/lib/strategies/composite/tools/patient-timeline.js \
  mcp-edge-clinical/lib/strategies/composite/tools/patient-risk.js \
  mcp-edge-clinical/lib/strategies/composite/tools/patient-list.js \
  mcp-edge-clinical/lib/strategies/composite/tools/pre-visit-plan.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-list.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-identify.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-provider.js \
  mcp-edge-clinical/lib/strategies/composite/tools/measure-evaluate.js \
  mcp-edge-clinical/lib/strategies/composite/tools/measure-results.js \
  mcp-edge-clinical/lib/strategies/composite/tools/measure-score.js \
  mcp-edge-clinical/lib/strategies/composite/tools/cds-patient-view.js \
  mcp-edge-clinical/lib/strategies/composite/tools/health-score.js
git commit -m "feat(mcp-edge-clinical): add PHI audit annotations to 13 composite read tools"
```

---

### Task 6: Annotate composite tools (remaining 12: FHIR, CQL, writes, non-PHI)

**Files:**
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/fhir-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/fhir-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/fhir-create.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/fhir-bundle.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-close.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-stats.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/care-gap-population.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/measure-population.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/cql-evaluate.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/cql-batch.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/cql-libraries.js`
- Modify: `mcp-edge-clinical/lib/strategies/composite/tools/cql-result.js`

**Step 1: Add audit annotations**

PHI write tools:
```javascript
// fhir-create.js, fhir-bundle.js
audit: { phi: true, write: true },

// care-gap-close.js
audit: { phi: true, write: true },
```

PHI read tools with special patientId args:
```javascript
// fhir-read.js (id is resource ID, not patientId)
audit: { phi: true, write: false },

// fhir-search.js (optional 'patient' param)
audit: { phi: true, write: false, patientIdArg: 'patient' },

// cql-evaluate.js, cql-batch.js, cql-result.js
audit: { phi: true, write: false, patientIdArg: 'patientId' },
```

Non-PHI aggregate tools:
```javascript
// care-gap-stats.js, care-gap-population.js, measure-population.js, cql-libraries.js
audit: { phi: false, write: false },
```

**Step 2: Run tests (regression)**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-clinical/lib/strategies/composite/tools/fhir-read.js \
  mcp-edge-clinical/lib/strategies/composite/tools/fhir-search.js \
  mcp-edge-clinical/lib/strategies/composite/tools/fhir-create.js \
  mcp-edge-clinical/lib/strategies/composite/tools/fhir-bundle.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-close.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-stats.js \
  mcp-edge-clinical/lib/strategies/composite/tools/care-gap-population.js \
  mcp-edge-clinical/lib/strategies/composite/tools/measure-population.js \
  mcp-edge-clinical/lib/strategies/composite/tools/cql-evaluate.js \
  mcp-edge-clinical/lib/strategies/composite/tools/cql-batch.js \
  mcp-edge-clinical/lib/strategies/composite/tools/cql-libraries.js \
  mcp-edge-clinical/lib/strategies/composite/tools/cql-result.js
git commit -m "feat(mcp-edge-clinical): add PHI audit annotations to 12 composite FHIR/CQL/write/aggregate tools"
```

---

## Stream D: High-Value Strategy Annotations

### Task 7: Annotate high-value tools

**Files:**
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/patient-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/patient-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/observation-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/observation-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/condition-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/condition-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/medication-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/medication-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/encounter-read.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/encounter-search.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/care-gap-list.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/care-gap-close.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/care-gap-stats.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/measure-evaluate.js`
- Modify: `mcp-edge-clinical/lib/strategies/high-value/tools/measure-results.js`

**Step 1: Add audit annotations**

FHIR read tools (resource ID, not patient ID):
```javascript
// patient-read.js, observation-read.js, condition-read.js, medication-read.js, encounter-read.js
audit: { phi: true, write: false },
```

FHIR search tools (optional patient param):
```javascript
// patient-search.js, observation-search.js, condition-search.js, medication-search.js, encounter-search.js
audit: { phi: true, write: false, patientIdArg: 'patient' },
```

Reused domain tools (same annotations as composite):
```javascript
// care-gap-list.js
audit: { phi: true, write: false, patientIdArg: 'patientId' },

// care-gap-close.js
audit: { phi: true, write: true },

// care-gap-stats.js
audit: { phi: false, write: false },

// measure-evaluate.js, measure-results.js
audit: { phi: true, write: false, patientIdArg: 'patientId' },
```

**Step 2: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-clinical/lib/strategies/high-value/tools/*.js
git commit -m "feat(mcp-edge-clinical): add PHI audit annotations to 15 high-value tools"
```

---

## Stream E: Full-Surface Strategy Annotations

### Task 8: Add phi field to resource registry

**Files:**
- Modify: `mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js`

**Step 1: Add `phi: boolean` to each registry entry**

```javascript
const RESOURCE_REGISTRY = [
  { type: 'Patient', searchable: true, creatable: true, phi: true },
  { type: 'Observation', searchable: true, creatable: true, phi: true },
  { type: 'Encounter', searchable: true, creatable: true, phi: true },
  { type: 'Condition', searchable: true, creatable: true, phi: true },
  { type: 'MedicationRequest', searchable: true, creatable: true, phi: true },
  { type: 'MedicationAdministration', searchable: true, creatable: false, phi: true },
  { type: 'Immunization', searchable: true, creatable: true, phi: true },
  { type: 'AllergyIntolerance', searchable: true, creatable: true, phi: true },
  { type: 'Procedure', searchable: true, creatable: true, phi: true },
  { type: 'DiagnosticReport', searchable: true, creatable: false, phi: true },
  { type: 'DocumentReference', searchable: true, creatable: true, phi: true },
  { type: 'CarePlan', searchable: true, creatable: true, phi: true },
  { type: 'Goal', searchable: true, creatable: true, phi: true },
  { type: 'Coverage', searchable: true, creatable: false, phi: true },
  { type: 'Appointment', searchable: true, creatable: true, phi: true },
  { type: 'Task', searchable: true, creatable: true, phi: true },
  { type: 'Bundle', searchable: false, creatable: false, phi: true },
  { type: 'Organization', searchable: true, creatable: false, phi: false },
  { type: 'Practitioner', searchable: true, creatable: false, phi: false },
  { type: 'PractitionerRole', searchable: true, creatable: false, phi: false },
];
```

**Step 2: Commit**

```bash
git add mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js
git commit -m "feat(mcp-edge-clinical): add phi boolean to full-surface resource registry"
```

---

### Task 9: Thread phi into tool-factory audit annotations

**Files:**
- Modify: `mcp-edge-clinical/lib/strategies/full-surface/tool-factory.js`

**Step 1: Add audit to createReadTool**

After `inputSchema` in the returned object (before `handler`):

```javascript
audit: { phi: !!resource.phi, write: false },
```

**Step 2: Add audit to createSearchTool**

```javascript
audit: { phi: !!resource.phi, write: false, patientIdArg: 'patient' },
```

**Step 3: Add audit to createCreateTool**

```javascript
audit: { phi: !!resource.phi, write: true },
```

**Step 4: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add mcp-edge-clinical/lib/strategies/full-surface/tool-factory.js
git commit -m "feat(mcp-edge-clinical): thread phi from resource registry into tool-factory audit annotations"
```

---

## Stream F: Server Wiring

### Task 10: Wire phiAuditLogger into clinical server.js

**Files:**
- Modify: `mcp-edge-clinical/server.js`

**Step 1: Add import and instantiation**

After the existing `require('hdim-mcp-edge-common')` line, add:
```javascript
const { createPhiAuditLogger } = require('./lib/phi-audit');
```

Inside `createApp()`, after the existing `const logger = createAuditLogger(...)` line, add:
```javascript
const phiAuditLogger = createPhiAuditLogger({ serviceName: 'hdim-clinical-edge' });
```

Add `phiAuditLogger` to the `createMcpRouter` call:
```javascript
app.use(createMcpRouter({
  tools,
  serverName: 'hdim-clinical-edge',
  serverVersion: '0.1.0',
  enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
  fixturesDir: path.join(__dirname, 'lib', 'strategies', strategyName, 'fixtures'),
  logger,
  rolePolicies: loadRolePolicies(strategyName),
  phiAuditLogger
}));
```

**Step 2: Run all clinical tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-clinical/server.js
git commit -m "feat(mcp-edge-clinical): wire phiAuditLogger into clinical MCP router"
```

---

## Stream G: Integration Tests and Proof Tests

### Task 11: PHI audit integration test

**Files:**
- Create: `mcp-edge-clinical/__tests__/phi-audit-integration.test.js`

**Step 1: Write integration test**

```javascript
const { createApp } = require('../server');
const supertest = require('supertest');

describe('PHI audit integration (composite strategy)', () => {
  let request, logged;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';

    // Capture PHI audit output by intercepting pino writes
    logged = [];
    const origCreateApp = createApp;
    // We test indirectly: the phiAuditLogger writes to stdout via pino.
    // For integration testing, we verify the HTTP response is correct
    // and test PHI audit via the mock-stream approach in unit tests.
    // This integration test verifies end-to-end that annotated tools
    // work correctly with the router and server wiring.

    const app = createApp();
    request = supertest(app);
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  it('patient_summary call succeeds (PHI tool)', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({
        jsonrpc: '2.0', id: 1, method: 'tools/call',
        params: { name: 'patient_summary', arguments: { patientId: 'p-1', tenantId: 't-1' } }
      });
    expect(res.status).toBe(200);
    expect(res.body.result).toBeDefined();
    expect(res.body.error).toBeUndefined();
  });

  it('care_gap_close call succeeds (PHI write tool)', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({
        jsonrpc: '2.0', id: 2, method: 'tools/call',
        params: { name: 'care_gap_close', arguments: { gapId: 'g-1', tenantId: 't-1', closedBy: 'dr-x', reason: 'resolved' } }
      });
    expect(res.status).toBe(200);
    expect(res.body.result).toBeDefined();
  });

  it('care_gap_stats call succeeds (non-PHI tool)', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({
        jsonrpc: '2.0', id: 3, method: 'tools/call',
        params: { name: 'care_gap_stats', arguments: { tenantId: 't-1' } }
      });
    expect(res.status).toBe(200);
    expect(res.body.result).toBeDefined();
  });

  it('forbidden call returns error (triggers AUTH_DENIED)', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'care_coordinator')
      .send({
        jsonrpc: '2.0', id: 4, method: 'tools/call',
        params: { name: 'patient_summary', arguments: { patientId: 'p-1', tenantId: 't-1' } }
      });
    expect(res.body.error).toBeDefined();
    expect(res.body.error.data.reason).toMatch(/forbidden/);
  });

  it('all composite tools have audit annotation', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'platform_admin')
      .send({ jsonrpc: '2.0', id: 5, method: 'tools/list', params: {} });

    // tools/list doesn't expose audit, but we can verify tool count
    // The annotation contract test (Task 12) covers the actual audit field check
    expect(res.body.result.tools.length).toBeGreaterThanOrEqual(25);
  });
});
```

**Step 2: Run test**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage __tests__/phi-audit-integration.test.js`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-clinical/__tests__/phi-audit-integration.test.js
git commit -m "test(mcp-edge-clinical): add PHI audit integration tests for composite strategy"
```

---

### Task 12: Audit annotation contract proof test

**Files:**
- Create: `mcp-edge-clinical/__tests__/phi-audit-contract-proof.test.js`

**Step 1: Write contract proof test**

This test loads all tools from every strategy and verifies each has a valid `audit` field (or is explicitly the infrastructure `edge_health` tool).

```javascript
const { createClinicalClient } = require('../lib/clinical-client');

// Mock the client so we don't need a running gateway
jest.mock('../lib/clinical-client', () => ({
  createClinicalClient: () => ({
    get: jest.fn().mockResolvedValue({ status: 200, ok: true, body: {} }),
    post: jest.fn().mockResolvedValue({ status: 201, ok: true, body: {} })
  })
}));

const STRATEGIES = ['composite', 'high-value', 'full-surface'];
const INFRA_TOOLS = ['edge_health']; // no audit required

describe('PHI audit annotation contract', () => {
  for (const strategyName of STRATEGIES) {
    describe(`${strategyName} strategy`, () => {
      let tools;

      beforeAll(() => {
        const client = createClinicalClient();
        const strategy = require(`../lib/strategies/${strategyName}`);
        tools = strategy.loadTools(client);
        tools.push(require('../lib/tools/edge-health').definition);
      });

      it('every tool has audit field or is infrastructure', () => {
        for (const tool of tools) {
          if (INFRA_TOOLS.includes(tool.name)) continue;
          expect(tool.audit).toBeDefined();
          expect(typeof tool.audit.phi).toBe('boolean');
          expect(typeof tool.audit.write).toBe('boolean');
        }
      });

      it('PHI tools with patientIdArg reference a valid schema property', () => {
        for (const tool of tools) {
          if (!tool.audit?.patientIdArg) continue;
          const schemaProps = Object.keys(tool.inputSchema.properties || {});
          expect(schemaProps).toContain(tool.audit.patientIdArg);
        }
      });

      it('write tools are PHI tools', () => {
        for (const tool of tools) {
          if (!tool.audit?.write) continue;
          expect(tool.audit.phi).toBe(true);
        }
      });

      it('no tool has unexpected audit fields', () => {
        const VALID_KEYS = ['phi', 'write', 'patientIdArg'];
        for (const tool of tools) {
          if (!tool.audit) continue;
          const keys = Object.keys(tool.audit);
          for (const key of keys) {
            expect(VALID_KEYS).toContain(key);
          }
        }
      });
    });
  }
});
```

**Step 2: Run test**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage __tests__/phi-audit-contract-proof.test.js`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add mcp-edge-clinical/__tests__/phi-audit-contract-proof.test.js
git commit -m "test(mcp-edge-clinical): add PHI audit annotation contract proof — all strategies"
```

---

### Task 13: Full regression + final commit

**Files:**
- None modified — validation only

**Step 1: Run all common tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-common && npx jest --no-coverage`
Expected: ALL PASS

**Step 2: Run all clinical tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL PASS

**Step 3: Run all platform tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-platform && npx jest --no-coverage`
Expected: ALL PASS

**Step 4: Run all devops tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-devops && npx jest --no-coverage`
Expected: ALL PASS

**Step 5: Report test totals**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npm run test:mcp-edge 2>&1 | tail -5`
Expected: All suites pass, test count > 1,307 (baseline) + ~20 new tests
