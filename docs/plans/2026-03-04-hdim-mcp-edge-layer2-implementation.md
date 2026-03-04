# MCP Edge Layer 2 — Clinical Edge Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build `mcp-edge-clinical` sidecar (port 3300) exposing all 5 HDIM clinical services via 25 MCP tools with 3 interchangeable strategies, HIPAA-compliant PHI audit logging, and comprehensive test coverage.

**Architecture:** Express HTTP server with JSON-RPC 2.0 router (same pattern as Layer 0+1). Clinical HTTP client proxies all calls through the gateway at port 18080. Strategy pattern (`CLINICAL_TOOL_STRATEGY` env var) selects which tool set loads at startup. Each strategy has its own tools, role policies, and demo fixtures.

**Tech Stack:** Node.js 20+, Express 4, Jest 30, Supertest, Pino (logging), `hdim-mcp-edge-common` shared library.

**Design Doc:** `docs/plans/2026-03-04-hdim-mcp-edge-layer2-design.md`

---

## Wave 1: Infrastructure (Sequential — 1 Agent)

These tasks must run in order. They create the shared foundation used by all strategies.

### Task 1: Package Scaffold

**Files:**
- Create: `mcp-edge-clinical/package.json`
- Create: `mcp-edge-clinical/.env.example`
- Create: `mcp-edge-clinical/.dockerignore`
- Create: `mcp-edge-clinical/Dockerfile`

**Step 1: Create package.json**

```json
{
  "name": "hdim-mcp-edge-clinical",
  "version": "0.1.0",
  "description": "HDIM MCP edge sidecar for clinical tools (port 3300)",
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
    "helmet": "^8.1.0",
    "pino": "^9.6.0"
  },
  "devDependencies": {
    "jest": "^30.0.0",
    "supertest": "^6.3.4"
  }
}
```

**Step 2: Create .env.example**

```bash
# HDIM MCP Edge — Clinical Sidecar
PORT=3300
HDIM_BASE_URL=http://localhost:18080
MCP_EDGE_API_KEY=
MCP_EDGE_ENFORCE_ROLE_AUTH=true
CLINICAL_TOOL_STRATEGY=composite
HDIM_DEMO_MODE=false
LOG_LEVEL=info
```

**Step 3: Create .dockerignore**

```
node_modules
__tests__
coverage*
.env
.env.*
!.env.example
*.test.js
*.md
```

**Step 4: Create Dockerfile**

```dockerfile
FROM node:20-slim
WORKDIR /app

COPY mcp-edge-common/ ./mcp-edge-common/
COPY mcp-edge-clinical/package.json mcp-edge-clinical/package-lock.json* ./mcp-edge-clinical/
WORKDIR /app/mcp-edge-clinical
RUN npm install --omit=dev
COPY mcp-edge-clinical/ ./

ENV NODE_ENV=production
ENV PORT=3300
EXPOSE 3300

HEALTHCHECK --interval=10s --timeout=2s --start-period=5s --retries=6 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||3300)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

USER node
CMD ["node", "index.js"]
```

**Step 5: Install dependencies**

Run: `cd mcp-edge-clinical && npm install`
Expected: `node_modules/` created, `package-lock.json` generated

**Step 6: Commit**

```bash
git add mcp-edge-clinical/package.json mcp-edge-clinical/package-lock.json mcp-edge-clinical/.env.example mcp-edge-clinical/.dockerignore mcp-edge-clinical/Dockerfile
git commit -m "feat(mcp-edge-clinical): scaffold package with deps and Dockerfile"
```

---

### Task 2: Clinical HTTP Client

**Files:**
- Create: `mcp-edge-clinical/__tests__/clinical-client.test.js`
- Create: `mcp-edge-clinical/lib/clinical-client.js`

**Step 1: Write the failing test**

```javascript
// mcp-edge-clinical/__tests__/clinical-client.test.js
const { createClinicalClient } = require('../lib/clinical-client');

describe('createClinicalClient', () => {
  let originalFetch;

  beforeEach(() => {
    originalFetch = global.fetch;
  });

  afterEach(() => {
    global.fetch = originalFetch;
    delete process.env.HDIM_BASE_URL;
    delete process.env.MCP_EDGE_API_KEY;
  });

  it('uses HDIM_BASE_URL env var', () => {
    process.env.HDIM_BASE_URL = 'http://gateway:18080';
    const client = createClinicalClient();
    expect(client.baseUrl).toBe('http://gateway:18080');
  });

  it('defaults to http://localhost:18080', () => {
    const client = createClinicalClient();
    expect(client.baseUrl).toBe('http://localhost:18080');
  });

  it('strips trailing slash from baseUrl', () => {
    const client = createClinicalClient({ baseUrl: 'http://gw:18080/' });
    expect(client.baseUrl).toBe('http://gw:18080');
  });

  describe('get()', () => {
    it('sends GET with Authorization and X-Tenant-ID headers', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 200,
        ok: true,
        text: () => Promise.resolve('{"id":"123"}')
      });

      const client = createClinicalClient({ baseUrl: 'http://gw:18080', apiKey: 'key1' });
      const res = await client.get('/patient/health-record?patient=abc', { tenantId: 'acme' });

      expect(global.fetch).toHaveBeenCalledWith(
        'http://gw:18080/patient/health-record?patient=abc',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            authorization: 'Bearer key1',
            'x-tenant-id': 'acme'
          })
        })
      );
      expect(res).toEqual({ status: 200, ok: true, body: '{"id":"123"}', url: 'http://gw:18080/patient/health-record?patient=abc' });
    });

    it('truncates response body at 20KB', async () => {
      const bigBody = 'x'.repeat(25_000);
      global.fetch = jest.fn().mockResolvedValue({
        status: 200,
        ok: true,
        text: () => Promise.resolve(bigBody)
      });

      const client = createClinicalClient({ baseUrl: 'http://gw:18080' });
      const res = await client.get('/path', { tenantId: 't1' });
      expect(res.body.length).toBeLessThan(21_000);
      expect(res.body).toContain('...[truncated]');
    });

    it('omits X-Tenant-ID when tenantId not provided', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 200,
        ok: true,
        text: () => Promise.resolve('{}')
      });

      const client = createClinicalClient({ baseUrl: 'http://gw:18080' });
      await client.get('/path');
      const headers = global.fetch.mock.calls[0][1].headers;
      expect(headers['x-tenant-id']).toBeUndefined();
    });
  });

  describe('post()', () => {
    it('sends POST with body, Authorization, and X-Tenant-ID', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 201,
        ok: true,
        text: () => Promise.resolve('{"created":true}')
      });

      const client = createClinicalClient({ baseUrl: 'http://gw:18080', apiKey: 'key2' });
      const res = await client.post('/care-gap/close', { gapId: 'g1' }, { tenantId: 'acme' });

      expect(global.fetch).toHaveBeenCalledWith(
        'http://gw:18080/care-gap/close',
        expect.objectContaining({
          method: 'POST',
          body: '{"gapId":"g1"}',
          headers: expect.objectContaining({
            authorization: 'Bearer key2',
            'x-tenant-id': 'acme',
            'content-type': 'application/json'
          })
        })
      );
      expect(res).toEqual({ status: 201, ok: true, body: '{"created":true}', url: 'http://gw:18080/care-gap/close' });
    });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-clinical && npx jest __tests__/clinical-client.test.js --no-coverage`
Expected: FAIL — `Cannot find module '../lib/clinical-client'`

**Step 3: Write implementation**

```javascript
// mcp-edge-clinical/lib/clinical-client.js
const DEFAULT_BASE_URL = 'http://localhost:18080';
const DEFAULT_TIMEOUT = 15_000;

function createClinicalClient({ baseUrl, apiKey, timeout } = {}) {
  const normalizedBase = (baseUrl || process.env.HDIM_BASE_URL || DEFAULT_BASE_URL)
    .trim().replace(/\/$/, '');
  const defaultApiKey = apiKey || process.env.MCP_EDGE_API_KEY || '';
  const requestTimeout = timeout || DEFAULT_TIMEOUT;

  function buildHeaders(tenantId, overrideApiKey) {
    const headers = {
      accept: 'application/json',
      'content-type': 'application/json'
    };
    const key = overrideApiKey || defaultApiKey;
    if (key) headers.authorization = `Bearer ${key}`;
    if (tenantId) headers['x-tenant-id'] = tenantId;
    return headers;
  }

  async function get(path, { tenantId, apiKey: overrideKey } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(tenantId, overrideKey),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  async function post(path, body, { tenantId, apiKey: overrideKey } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(tenantId, overrideKey),
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  return { baseUrl: normalizedBase, get, post };
}

module.exports = { createClinicalClient };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-clinical && npx jest __tests__/clinical-client.test.js --no-coverage`
Expected: 7 tests PASS

**Step 5: Commit**

```bash
git add mcp-edge-clinical/lib/clinical-client.js mcp-edge-clinical/__tests__/clinical-client.test.js
git commit -m "feat(mcp-edge-clinical): add clinical HTTP client with tenant header support"
```

---

### Task 3: PHI Audit Logger

**Files:**
- Create: `mcp-edge-clinical/__tests__/phi-audit.test.js`
- Create: `mcp-edge-clinical/lib/phi-audit.js`

**Step 1: Write the failing test**

```javascript
// mcp-edge-clinical/__tests__/phi-audit.test.js
const { createPhiAuditLogger } = require('../lib/phi-audit');

describe('createPhiAuditLogger', () => {
  let logged, logger;

  beforeEach(() => {
    logged = [];
    const mockStream = { write: (chunk) => { logged.push(JSON.parse(chunk)); } };
    logger = createPhiAuditLogger({ serviceName: 'mcp-edge-clinical', stream: mockStream });
  });

  describe('logToolAccess()', () => {
    it('logs PHI_ACCESS for PHI tools with patientId', () => {
      logger.logToolAccess({
        tool: 'patient_summary',
        role: 'clinician',
        tenantId: 'acme',
        patientId: 'p-123',
        success: true,
        durationMs: 234,
        phi: true
      });
      expect(logged).toHaveLength(1);
      expect(logged[0]).toMatchObject({
        level: 'info',
        action: 'PHI_ACCESS',
        tool: 'patient_summary',
        role: 'clinician',
        tenantId: 'acme',
        patientId: 'p-123',
        success: true,
        durationMs: 234
      });
    });

    it('logs TOOL_CALL for non-PHI tools without patientId', () => {
      logger.logToolAccess({
        tool: 'care_gap_stats',
        role: 'quality_officer',
        tenantId: 'acme',
        success: true,
        durationMs: 100,
        phi: false
      });
      expect(logged).toHaveLength(1);
      expect(logged[0]).toMatchObject({
        action: 'TOOL_CALL',
        tool: 'care_gap_stats'
      });
      expect(logged[0].patientId).toBeUndefined();
    });

    it('logs PHI_WRITE for write operations on PHI tools', () => {
      logger.logToolAccess({
        tool: 'care_gap_close',
        role: 'clinician',
        tenantId: 'acme',
        patientId: 'p-456',
        success: true,
        durationMs: 300,
        phi: true,
        write: true
      });
      expect(logged[0].action).toBe('PHI_WRITE');
    });
  });

  describe('logAuthDenied()', () => {
    it('logs AUTH_DENIED with role and tool', () => {
      logger.logAuthDenied({ tool: 'patient_summary', role: 'executive' });
      expect(logged[0]).toMatchObject({
        action: 'AUTH_DENIED',
        tool: 'patient_summary',
        role: 'executive'
      });
    });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-clinical && npx jest __tests__/phi-audit.test.js --no-coverage`
Expected: FAIL — `Cannot find module '../lib/phi-audit'`

**Step 3: Write implementation**

```javascript
// mcp-edge-clinical/lib/phi-audit.js
const { createAuditLogger } = require('hdim-mcp-edge-common');

function createPhiAuditLogger({ serviceName, stream } = {}) {
  const pino = createAuditLogger({ serviceName: serviceName || 'mcp-edge-clinical', stream });

  function logToolAccess({ tool, role, tenantId, patientId, success, durationMs, phi, write }) {
    const action = !phi ? 'TOOL_CALL' : write ? 'PHI_WRITE' : 'PHI_ACCESS';
    const record = {
      action,
      source: serviceName || 'mcp-edge-clinical',
      tool,
      role,
      tenantId,
      success,
      durationMs
    };
    if (phi && patientId) record.patientId = patientId;
    pino.info(record);
  }

  function logAuthDenied({ tool, role }) {
    pino.warn({
      action: 'AUTH_DENIED',
      source: serviceName || 'mcp-edge-clinical',
      tool,
      role
    });
  }

  return { logToolAccess, logAuthDenied };
}

module.exports = { createPhiAuditLogger };
```

**Step 4: Run test to verify it passes**

Run: `cd mcp-edge-clinical && npx jest __tests__/phi-audit.test.js --no-coverage`
Expected: 4 tests PASS

**Step 5: Commit**

```bash
git add mcp-edge-clinical/lib/phi-audit.js mcp-edge-clinical/__tests__/phi-audit.test.js
git commit -m "feat(mcp-edge-clinical): add PHI audit logger with HIPAA-compliant event types"
```

---

### Task 4: Server + Strategy Loader

**Files:**
- Create: `mcp-edge-clinical/__tests__/server.test.js`
- Create: `mcp-edge-clinical/server.js`
- Create: `mcp-edge-clinical/index.js`

**Step 1: Write the failing test**

```javascript
// mcp-edge-clinical/__tests__/server.test.js
const supertest = require('supertest');

describe('clinical edge server', () => {
  let app, request;

  beforeAll(() => {
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    process.env.HDIM_DEMO_MODE = 'true';
    const { createApp } = require('../server');
    app = createApp();
    request = supertest(app);
  });

  afterAll(() => {
    delete process.env.CLINICAL_TOOL_STRATEGY;
    delete process.env.HDIM_DEMO_MODE;
  });

  it('responds to /health', async () => {
    const res = await request.get('/health');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('hdim-clinical-edge');
    expect(res.body.status).toBe('ok');
  });

  it('responds to MCP initialize', async () => {
    const res = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
    expect(res.status).toBe(200);
    expect(res.body.result.serverInfo.name).toBe('hdim-clinical-edge');
    expect(res.body.result.protocolVersion).toBe('2025-11-25');
  });

  it('lists tools from composite strategy', async () => {
    const res = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
    expect(res.status).toBe(200);
    const toolNames = res.body.result.tools.map(t => t.name);
    expect(toolNames).toContain('patient_summary');
    expect(toolNames).toContain('fhir_read');
    expect(toolNames).toContain('care_gap_list');
    expect(toolNames).toContain('measure_evaluate');
    expect(toolNames).toContain('cql_evaluate');
    expect(toolNames.length).toBe(25);
  });

  it('rejects unknown strategy', () => {
    process.env.CLINICAL_TOOL_STRATEGY = 'nonexistent';
    jest.resetModules();
    expect(() => {
      require('../server').createApp();
    }).toThrow(/Unknown clinical tool strategy/);
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd mcp-edge-clinical && npx jest __tests__/server.test.js --no-coverage`
Expected: FAIL — `Cannot find module '../server'`

**Step 3: Write server.js**

```javascript
// mcp-edge-clinical/server.js
const express = require('express');
const path = require('node:path');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter } = require('hdim-mcp-edge-common');
const { createClinicalClient } = require('./lib/clinical-client');

const VALID_STRATEGIES = ['composite', 'high-value', 'full-surface'];

function loadStrategy(strategyName, client) {
  if (!VALID_STRATEGIES.includes(strategyName)) {
    throw new Error(`Unknown clinical tool strategy: "${strategyName}". Valid: ${VALID_STRATEGIES.join(', ')}`);
  }
  const strategy = require(`./lib/strategies/${strategyName}`);
  return strategy.loadTools(client);
}

function createApp() {
  const app = express();
  app.use(helmet());
  app.use(cors({ origin: '*', credentials: false }));
  app.use(express.json({ limit: '1mb' }));

  const strategyName = process.env.CLINICAL_TOOL_STRATEGY || 'composite';
  const client = createClinicalClient();
  const tools = loadStrategy(strategyName, client);

  app.use(createHealthRouter({
    serviceName: 'hdim-clinical-edge',
    version: '0.1.0'
  }));

  app.use(createMcpRouter({
    tools,
    serverName: 'hdim-clinical-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
    fixturesDir: path.join(__dirname, 'lib', 'strategies', strategyName, 'fixtures')
  }));

  return app;
}

module.exports = { createApp };
```

**Step 4: Write index.js**

```javascript
// mcp-edge-clinical/index.js
const { createApp } = require('./server');

const port = Number(process.env.PORT || 3300);
const app = createApp();

const server = app.listen(port, () => {
  console.log(`[hdim-clinical-edge] listening on :${port} (strategy: ${process.env.CLINICAL_TOOL_STRATEGY || 'composite'})`);
});

function shutdown(signal) {
  console.log(`[hdim-clinical-edge] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
```

**Step 5: Create composite strategy stub** (tools will be populated in Wave 2)

```javascript
// mcp-edge-clinical/lib/strategies/composite/index.js
const { createClinicalClient } = require('../../clinical-client');

function loadTools(client) {
  return [
    require('./tools/fhir-read').createDefinition(client),
    require('./tools/fhir-search').createDefinition(client),
    require('./tools/fhir-create').createDefinition(client),
    require('./tools/fhir-bundle').createDefinition(client),
    require('./tools/patient-summary').createDefinition(client),
    require('./tools/patient-timeline').createDefinition(client),
    require('./tools/patient-risk').createDefinition(client),
    require('./tools/patient-list').createDefinition(client),
    require('./tools/pre-visit-plan').createDefinition(client),
    require('./tools/care-gap-list').createDefinition(client),
    require('./tools/care-gap-identify').createDefinition(client),
    require('./tools/care-gap-close').createDefinition(client),
    require('./tools/care-gap-stats').createDefinition(client),
    require('./tools/care-gap-population').createDefinition(client),
    require('./tools/care-gap-provider').createDefinition(client),
    require('./tools/measure-evaluate').createDefinition(client),
    require('./tools/measure-results').createDefinition(client),
    require('./tools/measure-score').createDefinition(client),
    require('./tools/measure-population').createDefinition(client),
    require('./tools/cds-patient-view').createDefinition(client),
    require('./tools/health-score').createDefinition(client),
    require('./tools/cql-evaluate').createDefinition(client),
    require('./tools/cql-batch').createDefinition(client),
    require('./tools/cql-libraries').createDefinition(client),
    require('./tools/cql-result').createDefinition(client),
  ];
}

module.exports = { loadTools };
```

**NOTE:** The server.test.js will not pass until ALL 25 tool files exist. Create placeholder stubs for each tool file (done in Wave 2 Task 5-9). The server test acts as the integration gate — it must be the LAST test to pass in Wave 2.

**Step 6: Commit**

```bash
git add mcp-edge-clinical/server.js mcp-edge-clinical/index.js mcp-edge-clinical/__tests__/server.test.js mcp-edge-clinical/lib/strategies/composite/index.js
git commit -m "feat(mcp-edge-clinical): add server with strategy loader and entry point"
```

---

## Wave 2: Composite Tool Definitions (Parallel — 5 Agents)

Each agent creates tool files, test files, and fixture files for their domain. All agents can run simultaneously since tools are independent.

**Common pattern for EVERY tool file:**

```javascript
// mcp-edge-clinical/lib/strategies/composite/tools/{tool-name}.js
function createDefinition(clinicalClient) {
  return {
    name: '{tool_name}',
    description: '{Human-readable description for LLM}',
    inputSchema: {
      type: 'object',
      properties: { /* ... */ },
      required: [ /* ... */ ],
      additionalProperties: false
    },
    handler: async (args, { req }) => {
      // ... call clinicalClient.get() or .post()
      // ... return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] }
    }
  };
}
module.exports = { createDefinition };
```

**Common test pattern for EVERY tool:**

```javascript
// mcp-edge-clinical/__tests__/strategies/composite/tools/{tool-name}.test.js
const { createDefinition } = require('../../../../lib/strategies/composite/tools/{tool-name}');

describe('{tool_name} tool', () => {
  let mockClient, definition;
  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => expect(definition.name).toBe('{tool_name}'));
  it('has inputSchema with required fields', () => { /* check required[] */ });
  it('calls gateway with correct path', async () => { /* verify mockClient call */ });
  it('forwards tenantId as X-Tenant-ID', async () => { /* verify header forwarding */ });
  it('returns data in MCP content format', async () => { /* verify content[0].text */ });
  it('returns error on failure', async () => { /* verify error handling */ });
  it('does not leak PHI in error messages', async () => { /* verify no patient data in errors */ });
});
```

### Task 5: FHIR Composite Tools (Agent A — 4 tools)

**Files to create:**
- `mcp-edge-clinical/lib/strategies/composite/tools/fhir-read.js`
- `mcp-edge-clinical/lib/strategies/composite/tools/fhir-search.js`
- `mcp-edge-clinical/lib/strategies/composite/tools/fhir-create.js`
- `mcp-edge-clinical/lib/strategies/composite/tools/fhir-bundle.js`
- `mcp-edge-clinical/__tests__/strategies/composite/tools/fhir-read.test.js`
- `mcp-edge-clinical/__tests__/strategies/composite/tools/fhir-search.test.js`
- `mcp-edge-clinical/__tests__/strategies/composite/tools/fhir-create.test.js`
- `mcp-edge-clinical/__tests__/strategies/composite/tools/fhir-bundle.test.js`
- `mcp-edge-clinical/lib/strategies/composite/fixtures/fhir_read.json`
- `mcp-edge-clinical/lib/strategies/composite/fixtures/fhir_search.json`
- `mcp-edge-clinical/lib/strategies/composite/fixtures/fhir_create.json`
- `mcp-edge-clinical/lib/strategies/composite/fixtures/fhir_bundle.json`

**fhir-read.js implementation:**

```javascript
const VALID_RESOURCE_TYPES = [
  'Patient', 'Observation', 'Encounter', 'Condition', 'MedicationRequest',
  'MedicationAdministration', 'Immunization', 'AllergyIntolerance', 'Procedure',
  'DiagnosticReport', 'DocumentReference', 'CarePlan', 'Goal', 'Coverage',
  'Appointment', 'Task', 'Bundle', 'Organization', 'Practitioner', 'PractitionerRole'
];

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_read',
    description: 'Read a FHIR R4 resource by type and ID. Supports all 20 HDIM resource types.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        id: { type: 'string', description: 'Resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['resourceType', 'id', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, id, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/fhir/${resourceType}/${id}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition, VALID_RESOURCE_TYPES };
```

**fhir-search.js implementation:**

```javascript
const { VALID_RESOURCE_TYPES } = require('./fhir-read');

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_search',
    description: 'Search FHIR R4 resources by type with query parameters. Use patient param for patient-scoped searches.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        patient: { type: 'string', description: 'Patient ID for scoped search (optional)' },
        params: { type: 'object', description: 'Additional FHIR search params (e.g., category, _count, _offset)', additionalProperties: { type: 'string' } }
      },
      required: ['resourceType', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, tenantId, patient, params } = args;
      const qp = new URLSearchParams();
      if (patient) qp.set('patient', patient);
      if (params) Object.entries(params).forEach(([k, v]) => qp.set(k, v));
      const qs = qp.toString();
      const path = `/fhir/${resourceType}${qs ? '?' + qs : ''}`;
      try {
        const res = await clinicalClient.get(path, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**fhir-create.js implementation:**

```javascript
const { VALID_RESOURCE_TYPES } = require('./fhir-read');

function createDefinition(clinicalClient) {
  return {
    name: 'fhir_create',
    description: 'Create a FHIR R4 resource. Provide the full resource object.',
    inputSchema: {
      type: 'object',
      properties: {
        resourceType: { type: 'string', enum: VALID_RESOURCE_TYPES, description: 'FHIR resource type' },
        resource: { type: 'object', description: 'FHIR resource JSON object' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['resourceType', 'resource', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { resourceType, resource, tenantId } = args;
      try {
        const res = await clinicalClient.post(`/fhir/${resourceType}`, resource, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**fhir-bundle.js implementation:**

```javascript
function createDefinition(clinicalClient) {
  return {
    name: 'fhir_bundle',
    description: 'Submit a FHIR Bundle (transaction or batch). Process multiple resources in one call.',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', enum: ['transaction', 'batch'], description: 'Bundle type' },
        entries: { type: 'array', items: { type: 'object' }, description: 'Array of Bundle entry objects' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['type', 'entries', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { type, entries, tenantId } = args;
      const bundle = { resourceType: 'Bundle', type, entry: entries };
      try {
        const res = await clinicalClient.post('/fhir/Bundle', bundle, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**Demo fixture example** (`fixtures/fhir_read.json`):

```json
{
  "status": 200,
  "ok": true,
  "resourceType": "Patient",
  "data": "{\"resourceType\":\"Patient\",\"id\":\"demo-patient-001\",\"name\":[{\"family\":\"Smith\",\"given\":[\"Jane\"]}],\"gender\":\"female\",\"birthDate\":\"1975-03-15\"}",
  "demoMode": true
}
```

**Commit message:** `feat(mcp-edge-clinical): add FHIR composite tools (read, search, create, bundle)`

---

### Task 6: Patient Domain Tools (Agent B — 5 tools)

**Files to create (5 tools + 5 tests + 5 fixtures):**
- `tools/patient-summary.js` → `GET /patient/health-record?patient={id}` (tenantId required)
- `tools/patient-timeline.js` → `GET /patient/timeline/by-date?patient={id}&startDate=&endDate=` (tenantId required)
- `tools/patient-risk.js` → `GET /patient/risk-assessment?patient={id}` (tenantId required)
- `tools/patient-list.js` → `GET /api/v1/patients?page=&size=` (tenantId required)
- `tools/pre-visit-plan.js` → `GET /api/v1/providers/{providerId}/patients/{patientId}/pre-visit-summary` (tenantId required)

**patient-summary.js implementation:**

```javascript
function createDefinition(clinicalClient) {
  return {
    name: 'patient_summary',
    description: 'Get comprehensive patient health record summary (FHIR Bundle with all clinical data).',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId } = args;
      try {
        const res = await clinicalClient.get(`/patient/health-record?patient=${encodeURIComponent(patientId)}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**Follow the same pattern for the other 4 tools.** Each tool:
1. Takes `patientId` + `tenantId` (and domain-specific params)
2. Calls `clinicalClient.get(path, { tenantId })`
3. Returns `{ content: [{ type: 'text', text: JSON.stringify(...) }] }`
4. Error handler returns `{ ok: false, error: message }` — no patient data in errors

**patient-timeline.js:** Additional optional params `startDate` (string), `endDate` (string). Path: `/patient/timeline/by-date?patient={id}&startDate={}&endDate={}`

**patient-risk.js:** Path: `/patient/risk-assessment?patient={id}`

**patient-list.js:** No patientId required. Params: `page` (number, default 0), `size` (number, default 20). Path: `/api/v1/patients?page={}&size={}`

**pre-visit-plan.js:** Additional required param `providerId`. Path: `/api/v1/providers/{providerId}/patients/{patientId}/pre-visit-summary`

**Commit message:** `feat(mcp-edge-clinical): add patient domain tools (summary, timeline, risk, list, pre-visit)`

---

### Task 7: Care Gap Domain Tools (Agent C — 6 tools)

**Files to create (6 tools + 6 tests + 6 fixtures):**
- `tools/care-gap-list.js` → `GET /care-gap/open?patient={id}` or `/care-gap/high-priority?patient={id}` based on optional `status` param
- `tools/care-gap-identify.js` → `POST /care-gap/identify` body: `{ patientId, tenantId }` (optional `library` for specific measure)
- `tools/care-gap-close.js` → `POST /care-gap/close` body: `{ careGapId: gapId, closedBy, closureReason, tenantId }`
- `tools/care-gap-stats.js` → `GET /care-gap/stats` (non-PHI, aggregate)
- `tools/care-gap-population.js` → `GET /care-gap/population-report` (non-PHI, aggregate)
- `tools/care-gap-provider.js` → `GET /care-gap/providers/{providerId}/prioritized` (PHI)

**care-gap-list.js implementation:**

```javascript
function createDefinition(clinicalClient) {
  return {
    name: 'care_gap_list',
    description: 'List open or high-priority care gaps for a patient.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' },
        status: { type: 'string', enum: ['open', 'high-priority', 'overdue', 'upcoming'], description: 'Gap filter (default: open)' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId, status } = args;
      const endpoint = {
        'open': '/care-gap/open',
        'high-priority': '/care-gap/high-priority',
        'overdue': '/care-gap/overdue',
        'upcoming': '/care-gap/upcoming'
      }[status || 'open'] || '/care-gap/open';
      try {
        const res = await clinicalClient.get(`${endpoint}?patient=${encodeURIComponent(patientId)}`, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**care-gap-close.js** — This is a WRITE tool. Required params: `gapId`, `tenantId`, `closedBy`, `reason`. POST body.

**care-gap-stats.js** and **care-gap-population.js** — Non-PHI. Only require `tenantId`.

**Commit message:** `feat(mcp-edge-clinical): add care gap domain tools (list, identify, close, stats, population, provider)`

---

### Task 8: Quality Measure Domain Tools (Agent D — 6 tools)

**Files to create (6 tools + 6 tests + 6 fixtures):**
- `tools/measure-evaluate.js` → `POST /quality-measure/calculate` body: `{ patientId, measureId (optional) }`
- `tools/measure-results.js` → `GET /quality-measure/results?patient={id}`
- `tools/measure-score.js` → `GET /quality-measure/score?patient={id}`
- `tools/measure-population.js` → `GET /quality-measure/report/population` (non-PHI)
- `tools/cds-patient-view.js` → `POST /quality-measure/cds-services/patient-view` (CDS Hooks format)
- `tools/health-score.js` → `GET /quality-measure/patients/{id}/health-score`

**cds-patient-view.js implementation** (most complex in this domain):

```javascript
function createDefinition(clinicalClient) {
  return {
    name: 'cds_patient_view',
    description: 'Trigger CDS Hooks patient-view to get clinical decision support cards for a patient chart opening.',
    inputSchema: {
      type: 'object',
      properties: {
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { patientId, tenantId } = args;
      const hookPayload = {
        hookInstance: `mcp-edge-${Date.now()}`,
        hook: 'patient-view',
        context: { userId: 'mcp-edge-user', patientId }
      };
      try {
        const res = await clinicalClient.post('/quality-measure/cds-services/patient-view', hookPayload, { tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**Commit message:** `feat(mcp-edge-clinical): add quality measure domain tools (evaluate, results, score, population, CDS, health-score)`

---

### Task 9: CQL Engine Domain Tools (Agent E — 4 tools)

**Files to create (4 tools + 4 tests + 4 fixtures):**
- `tools/cql-evaluate.js` → `POST /cql/evaluate?library={name}&patient={id}`
- `tools/cql-batch.js` → `POST /cql/api/v1/cql/evaluations/batch` body: `{ libraryId, patientIds }`
- `tools/cql-libraries.js` → `GET /cql/api/v1/cql/libraries` (non-PHI reference data)
- `tools/cql-result.js` → `GET /cql/api/v1/cql/evaluations/patient/{patientId}/library/{library}/latest`

**cql-evaluate.js implementation:**

```javascript
function createDefinition(clinicalClient) {
  return {
    name: 'cql_evaluate',
    description: 'Evaluate a CQL/HEDIS measure library for a patient. Returns numerator/denominator compliance.',
    inputSchema: {
      type: 'object',
      properties: {
        library: { type: 'string', description: 'CQL library name (e.g., "Diabetes-HbA1c-Control")' },
        patientId: { type: 'string', description: 'Patient UUID' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['library', 'patientId', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { library, patientId, tenantId } = args;
      try {
        const res = await clinicalClient.post(
          `/cql/evaluate?library=${encodeURIComponent(library)}&patient=${encodeURIComponent(patientId)}`,
          {},
          { tenantId }
        );
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, library, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, library, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
```

**cql-batch.js:** Required params: `library`, `patientIds` (string array), `tenantId`. POST body with `{ libraryId: library, patientIds }`.

**cql-libraries.js:** Non-PHI. Only `tenantId` required. Optional `status` filter.

**cql-result.js:** Required: `patientId`, `library`, `tenantId`. Path with both interpolated.

**Commit message:** `feat(mcp-edge-clinical): add CQL engine domain tools (evaluate, batch, libraries, result)`

---

## Wave 3: Cross-Cutting Validation (Parallel — 4 Agents)

### Task 10: Composite Role Policies + RBAC Matrix (Agent F)

**Files:**
- Create: `mcp-edge-clinical/lib/strategies/composite/role-policies.js`
- Create: `mcp-edge-clinical/__tests__/strategies/composite/role-policies.test.js`
- Create: `mcp-edge-clinical/__tests__/rbac-matrix.test.js`

**role-policies.js implementation:**

```javascript
// mcp-edge-clinical/lib/strategies/composite/role-policies.js
function clinicalRolePolicies() {
  return {
    clinical_admin: [/./],
    platform_admin: [/./],
    developer: [/./],
    clinician: [/^(patient_summary|patient_timeline|patient_risk|patient_list|pre_visit_plan|care_gap_list|care_gap_identify|care_gap_close|care_gap_provider|fhir_read|fhir_search|fhir_create|fhir_bundle|cds_patient_view|health_score|measure_evaluate|measure_results|measure_score|cql_evaluate|cql_result)$/],
    care_coordinator: [/^(patient_summary|patient_list|pre_visit_plan|care_gap_list|care_gap_identify|care_gap_close|care_gap_stats|care_gap_population|care_gap_provider)$/],
    quality_officer: [/^(measure_evaluate|measure_results|measure_score|measure_population|cql_evaluate|cql_batch|cql_libraries|cql_result|care_gap_stats|care_gap_population)$/],
    executive: [/^(care_gap_stats|care_gap_population|measure_population|health_score)$/]
  };
}
module.exports = { clinicalRolePolicies };
```

**RBAC matrix test** — Same pattern as `mcp-edge-platform/__tests__/rbac-matrix.test.js`:
- Define `ROLE_TOOL_MATRIX` with all 7 roles × 25 tools = 175 test cases
- Use `it.each` to generate tests
- Enable demo mode so tools return fixture data instead of hitting the gateway

**Commit message:** `feat(mcp-edge-clinical): add composite RBAC role policies with exhaustive matrix test`

---

### Task 11: PHI Leak + Security Boundary Tests (Agent G)

**Files:**
- Create: `mcp-edge-clinical/__tests__/phi-leak.test.js`
- Create: `mcp-edge-clinical/__tests__/security.test.js`
- Create: `mcp-edge-clinical/__tests__/boundary.test.js`

**phi-leak.test.js** — Same pattern as platform-edge, plus clinical-specific checks:
- Error responses for PHI tools must not contain patient IDs, SSNs, MRNs
- Forbidden role errors must not contain API keys
- Failed tool executions must not contain stack traces
- Add clinical-specific patterns: FHIR resource body fragments, birth dates, names

**security.test.js** — Test:
- Requests without `X-Operator-Role` are rejected when auth enforced
- Unknown roles are rejected
- Missing `jsonrpc: '2.0'` is rejected
- Oversized payloads (>1MB) are rejected

**boundary.test.js** — Test:
- Empty tool arguments
- Missing required fields
- Extra fields rejected (additionalProperties: false)
- Very long strings (>10KB patientId)
- SQL injection patterns in patientId
- Path traversal in resourceType

**Commit message:** `test(mcp-edge-clinical): add PHI leak, security, and boundary validation tests`

---

### Task 12: Demo Mode Integration (Agent H)

**Files:**
- Create all 25 fixture files in `mcp-edge-clinical/lib/strategies/composite/fixtures/`
- Create: `mcp-edge-clinical/__tests__/demo-integration.test.js`

**Fixture naming:** `{tool_name}.json` — e.g., `patient_summary.json`, `fhir_read.json`, `care_gap_list.json`

Each fixture follows the pattern from platform-edge:
```json
{
  "status": 200,
  "ok": true,
  "data": "... synthetic clinical data ...",
  "demoMode": true
}
```

**Use realistic synthetic data** — real HEDIS measure names, realistic vital signs, plausible care gaps. No real patient data.

**demo-integration.test.js** — Enable `HDIM_DEMO_MODE=true`, call every tool via JSON-RPC, verify each returns valid fixture data.

**Commit message:** `feat(mcp-edge-clinical): add demo mode fixtures and integration test for all 25 tools`

---

### Task 13: Bridge + Config (Agent I)

**Files:**
- Create: `scripts/mcp/mcp-edge-clinical-bridge.mjs`
- Modify: `.mcp.json` — add `hdim-clinical-edge` entry
- Modify: `docker-compose.mcp-edge.yml` — add `mcp-edge-clinical` service

**mcp-edge-clinical-bridge.mjs** — Copy from `mcp-edge-platform-bridge.mjs`, change:
- Default endpoint: `http://localhost:3300/mcp`
- Env vars: `MCP_EDGE_URL`, `MCP_EDGE_API_KEY`, `MCP_EDGE_OPERATOR_ROLE`
- Same bridge-helpers imports and stdio handling

**docker-compose.mcp-edge.yml addition:**

```yaml
  mcp-edge-clinical:
    build:
      context: .
      dockerfile: mcp-edge-clinical/Dockerfile
    ports: ["3300:3300"]
    env_file: ./mcp-edge-clinical/.env.example
    environment:
      CLINICAL_TOOL_STRATEGY: composite
      HDIM_DEMO_MODE: "false"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3300/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]
      interval: 30s
      timeout: 5s
      retries: 3
```

**.mcp.json addition:**

```json
"hdim-clinical-edge": {
  "type": "stdio",
  "command": "node",
  "args": ["scripts/mcp/mcp-edge-clinical-bridge.mjs"],
  "env": {
    "MCP_EDGE_URL": "http://localhost:3300/mcp",
    "MCP_EDGE_OPERATOR_ROLE": "clinical_admin"
  }
}
```

**Commit message:** `feat(mcp-edge-clinical): add stdio bridge, Docker Compose config, and .mcp.json entry`

---

## Wave 4: Integration Validation (Sequential — 1 Agent)

### Task 14: Full Integration Tests

**Files:**
- Create: `mcp-edge-clinical/__tests__/integration.test.js`

**Test full JSON-RPC round-trip in demo mode:**

```javascript
const supertest = require('supertest');

describe('clinical edge integration (demo mode)', () => {
  let request;

  beforeAll(() => {
    process.env.HDIM_DEMO_MODE = 'true';
    process.env.CLINICAL_TOOL_STRATEGY = 'composite';
    const { createApp } = require('../server');
    request = supertest(createApp());
  });

  afterAll(() => {
    delete process.env.HDIM_DEMO_MODE;
    delete process.env.CLINICAL_TOOL_STRATEGY;
  });

  it('initialize → tools/list → tools/call round trip', async () => {
    // 1. Initialize
    const init = await request.post('/mcp')
      .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
    expect(init.body.result.protocolVersion).toBe('2025-11-25');

    // 2. List tools
    const list = await request.post('/mcp')
      .set('x-operator-role', 'clinical_admin')
      .send({ jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
    expect(list.body.result.tools.length).toBe(25);

    // 3. Call each tool (demo mode returns fixtures)
    for (const tool of list.body.result.tools) {
      const call = await request.post('/mcp')
        .set('x-operator-role', 'clinical_admin')
        .send({ jsonrpc: '2.0', id: 3, method: 'tools/call', params: { name: tool.name, arguments: {} } });
      expect(call.body.result).toBeDefined();
      expect(call.body.error).toBeUndefined();
    }
  });
});
```

**Step: Run full test suite**

Run: `cd mcp-edge-clinical && npx jest --no-coverage`
Expected: ALL tests pass

**Step: Run with coverage**

Run: `cd mcp-edge-clinical && npx jest --coverage`
Expected: ≥99% statements, ≥93% branches, 100% functions

**Commit message:** `test(mcp-edge-clinical): add full JSON-RPC integration test for composite strategy`

---

## Wave 5: Alternative Strategies (Parallel — 2 Agents)

### Task 15: High-Value Strategy (Agent J)

**Files:**
- Create: `mcp-edge-clinical/lib/strategies/high-value/index.js`
- Create: `mcp-edge-clinical/lib/strategies/high-value/role-policies.js`
- Create: 15 tool files in `mcp-edge-clinical/lib/strategies/high-value/tools/`
- Create: 15 fixture files in `mcp-edge-clinical/lib/strategies/high-value/fixtures/`
- Create: 15 test files in `mcp-edge-clinical/__tests__/strategies/high-value/tools/`
- Create: `mcp-edge-clinical/__tests__/strategies/high-value/role-policies.test.js`

**Tool list (15):**
- `patient_read`, `patient_search` (FHIR Patient)
- `observation_read`, `observation_search` (FHIR Observation)
- `condition_read`, `condition_search` (FHIR Condition)
- `medication_read`, `medication_search` (FHIR MedicationRequest)
- `encounter_read`, `encounter_search` (FHIR Encounter)
- `care_gap_list`, `care_gap_close`, `care_gap_stats`
- `measure_evaluate`, `measure_results`

Each `{resource}_read` tool takes `id` + `tenantId`, calls `GET /fhir/{Resource}/{id}`.
Each `{resource}_search` tool takes `tenantId` + optional `patient` + `params`, calls `GET /fhir/{Resource}?...`.

**Commit message:** `feat(mcp-edge-clinical): add high-value strategy with 15 resource-specific tools`

---

### Task 16: Full-Surface Strategy (Agent K)

**Files:**
- Create: `mcp-edge-clinical/lib/strategies/full-surface/index.js`
- Create: `mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js`
- Create: `mcp-edge-clinical/lib/strategies/full-surface/tool-factory.js`
- Create: `mcp-edge-clinical/lib/strategies/full-surface/role-policies.js`
- Create: `mcp-edge-clinical/__tests__/strategies/full-surface/tool-factory.test.js`
- Create: `mcp-edge-clinical/__tests__/strategies/full-surface/resource-registry.test.js`
- Create: `mcp-edge-clinical/__tests__/strategies/full-surface/role-policies.test.js`

**resource-registry.js** — Exports array of 20 FHIR resource types with metadata:

```javascript
const RESOURCE_REGISTRY = [
  { type: 'Patient', searchable: true, creatable: true },
  { type: 'Observation', searchable: true, creatable: true },
  // ... all 20
];
```

**tool-factory.js** — Generates tool definitions dynamically:

```javascript
function generateFhirTools(registry, client) {
  const tools = [];
  for (const resource of registry) {
    tools.push(createReadTool(resource, client));
    if (resource.searchable) tools.push(createSearchTool(resource, client));
    if (resource.creatable) tools.push(createCreateTool(resource, client));
  }
  return tools;
}
```

Then `index.js` calls the factory and appends the domain tools (care gap, measure, CQL) from shared implementations.

**Generate demo fixtures dynamically** — Factory creates fixture files at test time.

**Commit message:** `feat(mcp-edge-clinical): add full-surface strategy with dynamic tool factory for 60+ tools`

---

## Wave 6: Final Validation + Report (Sequential — 1 Agent)

### Task 17: Validation Report

**Files:**
- Create: `docs/plans/2026-03-04-hdim-mcp-edge-layer2-validation-report.md`

**Steps:**

1. Run: `cd mcp-edge-clinical && npx jest --coverage` — capture summary
2. Run with each strategy: `CLINICAL_TOOL_STRATEGY=composite`, `high-value`, `full-surface`
3. Generate validation report matching Layer 0+1 format:
   - Total tests, suites, pass rate
   - Coverage by file
   - Critical fixes applied
   - Test categories breakdown
4. Commit report

**Step: Final commit**

```bash
git add -A mcp-edge-clinical/ scripts/mcp/mcp-edge-clinical-bridge.mjs docker-compose.mcp-edge.yml .mcp.json docs/plans/
git commit -m "feat(mcp-edge-clinical): complete Layer 2 clinical-edge with 3 strategies and validation report"
```

---

## Summary

| Wave | Tasks | Agents | Duration Est. | Dependencies |
|------|-------|--------|--------------|-------------|
| 1 | Tasks 1-4 | 1 (sequential) | ~15 min | None |
| 2 | Tasks 5-9 | 5 (parallel) | ~20 min | Wave 1 |
| 3 | Tasks 10-13 | 4 (parallel) | ~15 min | Wave 2 |
| 4 | Task 14 | 1 (sequential) | ~5 min | Wave 3 |
| 5 | Tasks 15-16 | 2 (parallel) | ~15 min | Wave 2 |
| 6 | Task 17 | 1 (sequential) | ~5 min | Waves 4+5 |
| **Total** | **17 tasks** | **Up to 5 parallel** | **~75 min** | |

**Expected deliverables:**
- `mcp-edge-clinical/` — 3 strategies, ~100 source files
- ~500+ tests across all strategies
- ≥99% statement coverage
- Full demo mode with synthetic fixtures
- Docker Compose + bridge + .mcp.json config
- Validation report
