# Live Gateway Integration Testing — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Prove every MCP Edge tool works against the real HDIM gateway with clinically valid synthetic patient data (Synthea + hand-crafted overlays).

**Architecture:** Sidecar-internal `__tests__/live/` directories with supertest → `createApp()` at `HDIM_DEMO_MODE=false`. Shared utilities in `mcp-edge-common/lib/live-test-helpers.js`. Synthea FHIR R4 bundles + phenotype overlays at `test-data/synthetic-patients/`. CI job on master/release/* branches.

**Tech Stack:** Node.js 20, Jest 30, supertest 7, Express 4, Synthea (MITRE), FHIR R4 JSON Bundles

**Design Doc:** `docs/plans/2026-03-05-live-gateway-testing-design.md`

---

## Task 1: Shared Live Test Utilities

**Files:**
- Create: `mcp-edge-common/lib/live-test-helpers.js`
- Create: `mcp-edge-common/__tests__/live-test-helpers.test.js`

**Step 1: Write the failing test**

Create `mcp-edge-common/__tests__/live-test-helpers.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  readTestContext,
  writeTestContext,
  callTool
} = require('../lib/live-test-helpers');

const fs = require('fs');
const path = require('path');

describe('live-test-helpers', () => {
  describe('LIVE_TEST_DEFAULTS', () => {
    it('has tenantId "demo"', () => {
      expect(LIVE_TEST_DEFAULTS.tenantId).toBe('demo');
    });

    it('has gatewayUrl defaulting to localhost:18080', () => {
      expect(LIVE_TEST_DEFAULTS.gatewayUrl).toBe('http://localhost:18080');
    });
  });

  describe('isGatewayReachable', () => {
    it('returns false for unreachable URL', async () => {
      const result = await isGatewayReachable('http://localhost:19999');
      expect(result).toBe(false);
    });
  });

  describe('readTestContext / writeTestContext', () => {
    const testContextPath = '/tmp/live-test-context.json';

    afterEach(() => {
      try { fs.unlinkSync(testContextPath); } catch { /* ignore */ }
    });

    it('round-trips context through file', () => {
      const ctx = { patients: { 't2dm-managed': 'uuid-1' } };
      writeTestContext(ctx);
      const result = readTestContext();
      expect(result).toEqual(ctx);
    });

    it('readTestContext returns null when file missing', () => {
      expect(readTestContext()).toBeNull();
    });
  });

  describe('callTool', () => {
    it('sends MCP JSON-RPC 2.0 tools/call request', async () => {
      // Mock supertest request object
      let capturedPath, capturedHeaders, capturedBody;
      const mockRequest = {
        post: (p) => {
          capturedPath = p;
          const chain = {
            set: (k, v) => { capturedHeaders = capturedHeaders || {}; capturedHeaders[k] = v; return chain; },
            send: (b) => { capturedBody = b; return Promise.resolve({ status: 200, body: { jsonrpc: '2.0', id: 1, result: {} } }); }
          };
          return chain;
        }
      };

      await callTool(mockRequest, 'patient_summary', { patientId: 'p1', tenantId: 'demo' }, 'clinical_admin');
      expect(capturedPath).toBe('/mcp');
      expect(capturedHeaders['x-operator-role']).toBe('clinical_admin');
      expect(capturedBody.method).toBe('tools/call');
      expect(capturedBody.params.name).toBe('patient_summary');
      expect(capturedBody.params.arguments).toEqual({ patientId: 'p1', tenantId: 'demo' });
    });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-common/__tests__/live-test-helpers.test.js --no-coverage`
Expected: FAIL — `Cannot find module '../lib/live-test-helpers'`

**Step 3: Write minimal implementation**

Create `mcp-edge-common/lib/live-test-helpers.js`:

```js
const fs = require('fs');
const { execFileSync } = require('child_process');

const TEST_CONTEXT_PATH = '/tmp/live-test-context.json';

const LIVE_TEST_DEFAULTS = {
  tenantId: 'demo',
  gatewayUrl: process.env.HDIM_BASE_URL || 'http://localhost:18080',
};

async function isGatewayReachable(url) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    const res = await fetch(`${url}/actuator/health`, { signal: controller.signal });
    clearTimeout(timeout);
    return res.ok;
  } catch {
    return false;
  }
}

async function isDockerReachable() {
  try {
    execFileSync('docker', ['info'], { stdio: 'ignore', timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}

function readTestContext() {
  try {
    return JSON.parse(fs.readFileSync(TEST_CONTEXT_PATH, 'utf8'));
  } catch {
    return null;
  }
}

function writeTestContext(ctx) {
  fs.writeFileSync(TEST_CONTEXT_PATH, JSON.stringify(ctx, null, 2));
}

function callTool(request, toolName, args = {}, role = 'platform_admin', id = 1) {
  return request.post('/mcp')
    .set('x-operator-role', role)
    .send({
      jsonrpc: '2.0',
      id,
      method: 'tools/call',
      params: { name: toolName, arguments: args }
    });
}

module.exports = {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  isDockerReachable,
  readTestContext,
  writeTestContext,
  callTool,
  TEST_CONTEXT_PATH
};
```

**Step 4: Run test to verify it passes**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-common/__tests__/live-test-helpers.test.js --no-coverage`
Expected: PASS — all 5 tests green

**Step 5: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-common/lib/live-test-helpers.js mcp-edge-common/__tests__/live-test-helpers.test.js
git commit -m "feat(live-tests): add shared live test utilities

- LIVE_TEST_DEFAULTS (tenantId, gatewayUrl)
- isGatewayReachable / isDockerReachable skip helpers
- readTestContext / writeTestContext for shared state
- callTool helper for MCP JSON-RPC round trips"
```

---

## Task 2: Phenotype Manifest and Bundle Stubs

**Files:**
- Create: `test-data/synthetic-patients/manifest.json`
- Create: `test-data/synthetic-patients/bundles/t2dm-managed.json`
- Create: `test-data/synthetic-patients/bundles/t2dm-unmanaged.json`
- Create: `test-data/synthetic-patients/bundles/chf-polypharmacy.json`
- Create: `test-data/synthetic-patients/bundles/preventive-gaps.json`
- Create: `test-data/synthetic-patients/bundles/healthy-pediatric.json`
- Create: `test-data/synthetic-patients/bundles/multi-chronic-elderly.json`
- Create: `test-data/synthetic-patients/overlays/t2dm-managed.json`
- Create: `test-data/synthetic-patients/overlays/t2dm-unmanaged.json`
- Create: `test-data/synthetic-patients/overlays/chf-polypharmacy.json`
- Create: `test-data/synthetic-patients/overlays/preventive-gaps.json`
- Create: `test-data/synthetic-patients/overlays/healthy-pediatric.json`
- Create: `test-data/synthetic-patients/overlays/multi-chronic-elderly.json`
- Create: `test-data/synthetic-patients/generate.sh`

**Step 1: Create the manifest**

Create `test-data/synthetic-patients/manifest.json`:

```json
{
  "version": "1.0.0",
  "description": "Synthetic patient phenotypes for HDIM live gateway integration testing",
  "generator": "Synthea (MITRE) + hand-crafted overlays",
  "phenotypes": [
    {
      "id": "t2dm-managed",
      "name": "Type 2 Diabetes — Managed",
      "bundle": "bundles/t2dm-managed.json",
      "overlay": "overlays/t2dm-managed.json",
      "synthea": { "seed": 42, "module": "diabetes", "age": 58, "gender": "F" },
      "expected": {
        "careGaps": 0,
        "hba1c": { "value": 6.8, "unit": "%" },
        "riskScore": "low",
        "activeConditions": ["Type 2 diabetes mellitus"],
        "cqlCompliant": ["HbA1c-Control", "Diabetic-Eye-Exam"]
      }
    },
    {
      "id": "t2dm-unmanaged",
      "name": "Type 2 Diabetes — Unmanaged",
      "bundle": "bundles/t2dm-unmanaged.json",
      "overlay": "overlays/t2dm-unmanaged.json",
      "synthea": { "seed": 99, "module": "diabetes", "age": 67, "gender": "M" },
      "expected": {
        "careGaps": 2,
        "careGapTypes": ["retinopathy-screening", "nephropathy-screening"],
        "hba1c": { "value": 9.2, "unit": "%" },
        "riskScore": "high",
        "activeConditions": ["Type 2 diabetes mellitus"],
        "cqlCompliant": [],
        "cqlNonCompliant": ["HbA1c-Control"]
      }
    },
    {
      "id": "chf-polypharmacy",
      "name": "CHF with Polypharmacy",
      "bundle": "bundles/chf-polypharmacy.json",
      "overlay": "overlays/chf-polypharmacy.json",
      "synthea": { "seed": 137, "module": "congestive_heart_failure", "age": 74, "gender": "F" },
      "expected": {
        "medicationCount": 8,
        "riskScore": "high",
        "activeConditions": ["Congestive heart failure"],
        "recentEncounters": true
      }
    },
    {
      "id": "preventive-gaps",
      "name": "Preventive Care Gaps",
      "bundle": "bundles/preventive-gaps.json",
      "overlay": "overlays/preventive-gaps.json",
      "synthea": { "seed": 200, "module": "wellness_encounters", "age": 45, "gender": "M" },
      "expected": {
        "careGaps": 3,
        "careGapTypes": ["colonoscopy", "flu-vaccine", "lipid-panel"],
        "riskScore": "moderate",
        "activeConditions": []
      }
    },
    {
      "id": "healthy-pediatric",
      "name": "Healthy Pediatric",
      "bundle": "bundles/healthy-pediatric.json",
      "overlay": "overlays/healthy-pediatric.json",
      "synthea": { "seed": 55, "module": "wellness_encounters", "age": 12, "gender": "F" },
      "expected": {
        "careGaps": 0,
        "activeConditions": [],
        "immunizationsCurrent": true,
        "riskScore": "low"
      }
    },
    {
      "id": "multi-chronic-elderly",
      "name": "Multi-Chronic Elderly",
      "bundle": "bundles/multi-chronic-elderly.json",
      "overlay": "overlays/multi-chronic-elderly.json",
      "synthea": { "seed": 300, "module": "copd", "age": 82, "gender": "M" },
      "expected": {
        "activeConditions": ["Chronic obstructive pulmonary disease", "Chronic kidney disease stage 3", "Essential hypertension"],
        "conditionCount": 3,
        "riskScore": "high",
        "healthScore": "low"
      }
    }
  ]
}
```

**Step 2: Create FHIR R4 bundles**

Create all 6 bundles. Each is a FHIR R4 transaction Bundle. Example for `test-data/synthetic-patients/bundles/t2dm-managed.json`:

```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "t2dm-managed-001",
        "name": [{ "given": ["Maria"], "family": "Garcia" }],
        "gender": "female",
        "birthDate": "1968-03-15"
      },
      "request": { "method": "PUT", "url": "Patient/t2dm-managed-001" }
    },
    {
      "resource": {
        "resourceType": "Condition",
        "id": "t2dm-managed-cond-001",
        "subject": { "reference": "Patient/t2dm-managed-001" },
        "code": {
          "coding": [{ "system": "http://snomed.info/sct", "code": "44054006", "display": "Type 2 diabetes mellitus" }]
        },
        "clinicalStatus": {
          "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active" }]
        },
        "onsetDateTime": "2015-06-01"
      },
      "request": { "method": "PUT", "url": "Condition/t2dm-managed-cond-001" }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "id": "t2dm-managed-hba1c-001",
        "status": "final",
        "subject": { "reference": "Patient/t2dm-managed-001" },
        "code": {
          "coding": [{ "system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c" }]
        },
        "valueQuantity": { "value": 6.8, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%" },
        "effectiveDateTime": "2026-01-15"
      },
      "request": { "method": "PUT", "url": "Observation/t2dm-managed-hba1c-001" }
    },
    {
      "resource": {
        "resourceType": "MedicationRequest",
        "id": "t2dm-managed-med-001",
        "status": "active",
        "intent": "order",
        "subject": { "reference": "Patient/t2dm-managed-001" },
        "medicationCodeableConcept": {
          "coding": [{ "system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "860975", "display": "Metformin 500 MG" }]
        }
      },
      "request": { "method": "PUT", "url": "MedicationRequest/t2dm-managed-med-001" }
    },
    {
      "resource": {
        "resourceType": "Encounter",
        "id": "t2dm-managed-enc-001",
        "status": "finished",
        "class": { "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB" },
        "subject": { "reference": "Patient/t2dm-managed-001" },
        "period": { "start": "2026-01-15", "end": "2026-01-15" }
      },
      "request": { "method": "PUT", "url": "Encounter/t2dm-managed-enc-001" }
    }
  ]
}
```

Create remaining 5 bundles following same FHIR R4 transaction pattern. Key differences per phenotype:

- **`t2dm-unmanaged.json`**: Patient (67M "James Wilson"), T2DM condition, HbA1c 9.2% observation, NO retinopathy/nephropathy screening observations
- **`chf-polypharmacy.json`**: Patient (74F "Dorothy Chen"), CHF condition (EF 35% observation), 8 MedicationRequests (ACE inhibitor, beta-blocker, diuretic, etc.), recent ED encounter (class: EMER)
- **`preventive-gaps.json`**: Patient (45M "Robert Thompson"), no chronic conditions, missing colonoscopy/flu/lipid observations, only well-visit encounters
- **`healthy-pediatric.json`**: Patient (12F "Emily Davis"), no conditions, well-child encounters (AMB), Immunization resources per CDC schedule
- **`multi-chronic-elderly.json`**: Patient (82M "William Anderson"), COPD condition (SNOMED 13645005), CKD Stage 3 condition (SNOMED 433144002), Essential hypertension condition (SNOMED 59621000), multiple specialist encounters

**Step 3: Create overlay files**

Create `test-data/synthetic-patients/overlays/t2dm-managed.json`:

```json
{
  "phenotypeId": "t2dm-managed",
  "careGaps": [],
  "qualityMeasures": [
    { "measureId": "HbA1c-Control", "status": "compliant", "period": { "start": "2026-01-01", "end": "2026-12-31" } },
    { "measureId": "Diabetic-Eye-Exam", "status": "compliant", "period": { "start": "2026-01-01", "end": "2026-12-31" } }
  ],
  "cqlTargets": [
    { "libraryId": "HbA1c-Control", "expected": "compliant" },
    { "libraryId": "Diabetic-Eye-Exam", "expected": "compliant" }
  ],
  "cdsHooks": { "patientView": { "expectedCards": 0 } }
}
```

Create remaining 5 overlay files following same pattern. Key differences:

- **`t2dm-unmanaged.json`**: 2 care gaps (`retinopathy-screening`, `nephropathy-screening`), `HbA1c-Control` non-compliant, `patientView.expectedCards: 2`
- **`chf-polypharmacy.json`**: CHF-specific measures, polypharmacy CDS alert card, `expectedCards: 1`
- **`preventive-gaps.json`**: 3 care gaps (`colonoscopy`, `flu-vaccine`, `lipid-panel`), no CQL targets, `expectedCards: 0`
- **`healthy-pediatric.json`**: Empty careGaps, immunization compliance measures, `expectedCards: 0`
- **`multi-chronic-elderly.json`**: Multiple chronic condition measures, high-risk CDS cards, `expectedCards: 2`

**Step 4: Create generate.sh**

Create `test-data/synthetic-patients/generate.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

# Synthea wrapper — generates FHIR R4 bundles for each phenotype.
# Bundles are committed to git; CI does NOT need Synthea installed.
# Run this only when phenotype definitions change.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUNDLES_DIR="$SCRIPT_DIR/bundles"
MANIFEST="$SCRIPT_DIR/manifest.json"

if ! command -v java &> /dev/null; then
  echo "ERROR: Java 11+ required for Synthea" >&2
  exit 1
fi

SYNTHEA_JAR="${SYNTHEA_JAR:-$HOME/synthea/build/libs/synthea-with-dependencies.jar}"
if [ ! -f "$SYNTHEA_JAR" ]; then
  echo "ERROR: Synthea JAR not found at $SYNTHEA_JAR" >&2
  echo "Set SYNTHEA_JAR env var or install Synthea: https://github.com/synthetichealth/synthea" >&2
  exit 1
fi

echo "Generating synthetic patient bundles..."
echo "Manifest: $MANIFEST"
echo "Output: $BUNDLES_DIR"

PHENOTYPE_COUNT=$(node -e "console.log(require('$MANIFEST').phenotypes.length)")
echo "Phenotypes: $PHENOTYPE_COUNT"

for i in $(seq 0 $((PHENOTYPE_COUNT - 1))); do
  ID=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].id)")
  SEED=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.seed)")
  MODULE=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.module)")
  AGE=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.age)")
  GENDER=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.gender)")

  echo "  Generating: $ID (seed=$SEED, module=$MODULE, age=$AGE, gender=$GENDER)"

  java -jar "$SYNTHEA_JAR" \
    --exporter.fhir.transaction_bundle true \
    --exporter.baseDirectory "$BUNDLES_DIR/tmp-$ID" \
    --generate.seed "$SEED" \
    -m "$MODULE" \
    -a "$AGE-$AGE" \
    -g "$GENDER" \
    -p 1 \
    --exporter.years_of_history 10

  mv "$BUNDLES_DIR/tmp-$ID/fhir/"*.json "$BUNDLES_DIR/$ID.json" 2>/dev/null || true
  rm -rf "$BUNDLES_DIR/tmp-$ID"
done

echo "Done! Generated $PHENOTYPE_COUNT bundles."
echo "Review bundles, apply overlays, then commit."
```

Make executable: `chmod +x test-data/synthetic-patients/generate.sh`

**Step 5: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add test-data/synthetic-patients/
git commit -m "feat(live-tests): add synthetic patient phenotypes and manifest

- 6 patient phenotypes with clinically valid FHIR R4 bundles
- Phenotype overlays for care gaps, CQL targets, quality measures
- Synthea generation script (bundles committed, CI doesn't need Synthea)
- Manifest with expected outcomes per phenotype"
```

---

## Task 3: Test Data Loader

**Files:**
- Create: `test-data/synthetic-patients/load-test-patients.js`
- Create: `test-data/synthetic-patients/__tests__/load-test-patients.test.js`

**Step 1: Write the failing test**

Create `test-data/synthetic-patients/__tests__/load-test-patients.test.js`:

```js
const path = require('path');
const fs = require('fs');

describe('load-test-patients — manifest validation', () => {
  const manifestPath = path.join(__dirname, '..', 'manifest.json');

  it('manifest exists and is valid JSON', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    expect(manifest.phenotypes).toBeInstanceOf(Array);
    expect(manifest.phenotypes.length).toBe(6);
  });

  it('every phenotype references existing bundle and overlay files', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundlePath = path.join(__dirname, '..', p.bundle);
      const overlayPath = path.join(__dirname, '..', p.overlay);
      expect(fs.existsSync(bundlePath)).toBe(true);
      expect(fs.existsSync(overlayPath)).toBe(true);
    }
  });

  it('every bundle is valid FHIR R4 transaction Bundle', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundle = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.bundle), 'utf8'));
      expect(bundle.resourceType).toBe('Bundle');
      expect(bundle.type).toBe('transaction');
      expect(bundle.entry).toBeInstanceOf(Array);
      expect(bundle.entry.length).toBeGreaterThan(0);
      for (const entry of bundle.entry) {
        expect(entry.resource).toBeDefined();
        expect(entry.resource.resourceType).toBeDefined();
        expect(entry.request).toBeDefined();
        expect(entry.request.method).toBeDefined();
        expect(entry.request.url).toBeDefined();
      }
    }
  });

  it('every bundle has a Patient resource', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundle = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.bundle), 'utf8'));
      const patients = bundle.entry.filter(e => e.resource.resourceType === 'Patient');
      expect(patients.length).toBeGreaterThanOrEqual(1);
    }
  });

  it('every overlay references its phenotype', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const overlay = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.overlay), 'utf8'));
      expect(overlay.phenotypeId).toBe(p.id);
    }
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest test-data/synthetic-patients/__tests__/load-test-patients.test.js --no-coverage`
Expected: FAIL if bundles/overlays from Task 2 are incomplete, PASS if they exist

**Step 3: Write the loader**

Create `test-data/synthetic-patients/load-test-patients.js`:

```js
#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const MANIFEST_PATH = path.join(__dirname, 'manifest.json');
const CONTEXT_PATH = '/tmp/live-test-context.json';

async function loadPatients(gatewayUrl, tenantId) {
  const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8'));
  const context = { patients: {}, resources: {} };

  console.log(`Loading ${manifest.phenotypes.length} phenotypes into ${gatewayUrl}...`);

  for (const phenotype of manifest.phenotypes) {
    const bundlePath = path.join(__dirname, phenotype.bundle);
    const bundle = JSON.parse(fs.readFileSync(bundlePath, 'utf8'));

    console.log(`  Loading: ${phenotype.id} (${bundle.entry.length} resources)`);

    const res = await fetch(`${gatewayUrl}/fhir`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/fhir+json',
        'Accept': 'application/fhir+json',
        'x-tenant-id': tenantId
      },
      body: JSON.stringify(bundle)
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Failed to load ${phenotype.id}: ${res.status} ${text}`);
    }

    const responseBundle = await res.json();

    const patientEntry = responseBundle.entry?.find(e =>
      e.response?.location?.startsWith('Patient/')
    );
    if (patientEntry) {
      const patientId = patientEntry.response.location.split('/')[1];
      context.patients[phenotype.id] = patientId;
    }

    context.resources[phenotype.id] = {};
    for (const entry of (responseBundle.entry || [])) {
      if (entry.response?.location) {
        const [resourceType, id] = entry.response.location.split('/');
        if (!context.resources[phenotype.id][resourceType]) {
          context.resources[phenotype.id][resourceType] = [];
        }
        context.resources[phenotype.id][resourceType].push(id);
      }
    }

    console.log(`    Patient ID: ${context.patients[phenotype.id] || 'unknown'}`);
  }

  fs.writeFileSync(CONTEXT_PATH, JSON.stringify(context, null, 2));
  console.log(`\nContext written to ${CONTEXT_PATH}`);
  console.log(`Loaded ${Object.keys(context.patients).length} patients.`);

  return context;
}

if (require.main === module) {
  const args = process.argv.slice(2);
  const gatewayUrl = args.includes('--gateway-url')
    ? args[args.indexOf('--gateway-url') + 1]
    : process.env.HDIM_BASE_URL || 'http://localhost:18080';
  const tenantId = args.includes('--tenant-id')
    ? args[args.indexOf('--tenant-id') + 1]
    : 'demo';

  loadPatients(gatewayUrl, tenantId)
    .then(() => process.exit(0))
    .catch(err => { console.error(err); process.exit(1); });
}

module.exports = { loadPatients };
```

**Step 4: Run test to verify it passes**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest test-data/synthetic-patients/__tests__/load-test-patients.test.js --no-coverage`
Expected: PASS — all 5 manifest/bundle validation tests green

**Step 5: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add test-data/synthetic-patients/load-test-patients.js test-data/synthetic-patients/__tests__/
git commit -m "feat(live-tests): add test data loader with manifest validation

- load-test-patients.js: POSTs FHIR bundles to gateway, writes context
- Manifest validation tests: bundle structure, overlay references, FHIR compliance"
```

---

## Task 4: Jest Configuration for Live Tests

**Files:**
- Modify: `mcp-edge-platform/jest.config.js`
- Modify: `mcp-edge-clinical/jest.config.js`
- Modify: `mcp-edge-devops/jest.config.js`

**Step 1: Read each jest.config.js**

Read all 3 files first to see current state.

**Step 2: Add live test exclusion**

For each sidecar `jest.config.js`, add `'\\.live\\.test\\.js$'` to `testPathIgnorePatterns`:

```js
module.exports = {
  testMatch: ['**/__tests__/**/*.test.js'],
  testPathIgnorePatterns: ['/node_modules/', '\\.live\\.test\\.js$'],
  // ... rest unchanged (collectCoverageFrom, coverageProvider, coverageThreshold, forceExit)
};
```

**Step 3: Verify unit tests still pass**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest --projects mcp-edge-platform mcp-edge-clinical mcp-edge-devops --no-coverage`
Expected: PASS — same test count as before

**Step 4: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-platform/jest.config.js mcp-edge-clinical/jest.config.js mcp-edge-devops/jest.config.js
git commit -m "chore: exclude *.live.test.js from default jest runs"
```

---

## Task 5: Platform Live Tests

**Files:**
- Create: `mcp-edge-platform/__tests__/live/platform-tools.live.test.js`

**Step 1: Write the live test file**

Create `mcp-edge-platform/__tests__/live/platform-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request;
let gatewayReachable = false;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  if (!gatewayReachable) {
    console.warn('Gateway unreachable — skipping platform live tests');
    return;
  }

  process.env.HDIM_DEMO_MODE = 'false';
  jest.resetModules();
  const { createApp } = require('../../server');
  const supertest = require('supertest');
  request = supertest(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
});

describe('platform edge — live gateway', () => {
  beforeEach(() => {
    if (!gatewayReachable) pending();
  });

  it('edge_health returns healthy status', async () => {
    const res = await callTool(request, 'edge_health');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(['healthy', 'degraded']).toContain(data.status);
  });

  it('platform_health returns gateway health from actuator', async () => {
    const res = await callTool(request, 'platform_health');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(200);
  });

  it('platform_info returns version and config', async () => {
    const res = await callTool(request, 'platform_info');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.name).toBe('hdim-platform-edge');
    expect(data.version).toBeDefined();
  });

  it('fhir_metadata returns FHIR capability statement', async () => {
    const res = await callTool(request, 'fhir_metadata');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });

  it('service_catalog returns available services', async () => {
    const res = await callTool(request, 'service_catalog');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('dashboard_stats returns operational statistics', async () => {
    const res = await callTool(request, 'dashboard_stats');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('demo_status returns demo mode state', async () => {
    const res = await callTool(request, 'demo_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  it('demo_seed populates gateway with test data', async () => {
    const res = await callTool(request, 'demo_seed', { scenarioName: 'hedis-evaluation' });
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 2: Verify test is excluded from default runs**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-platform --no-coverage --listTests | grep live`
Expected: No output

**Step 3: Verify test is included in live runs**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-platform --testPathPattern=live --no-coverage --listTests`
Expected: `platform-tools.live.test.js` in list

**Step 4: Run live test — verify skip**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-platform --testPathPattern=live --no-coverage`
Expected: Tests pending/skipped with "Gateway unreachable" warning

**Step 5: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-platform/__tests__/live/
git commit -m "feat(live-tests): add platform sidecar live tests

- 8 tool round-trip tests against real gateway
- Graceful skip when gateway unreachable
- Validates edge_health, platform_health, fhir_metadata, etc."
```

---

## Task 6: Clinical Live Test Setup (Seed + Discovery)

**Files:**
- Create: `mcp-edge-clinical/__tests__/live/setup.live.test.js`

**Step 1: Write the setup test**

Create `mcp-edge-clinical/__tests__/live/setup.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  writeTestContext,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request;
let gatewayReachable = false;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  if (!gatewayReachable) {
    console.warn('Gateway unreachable — skipping clinical live setup');
    return;
  }

  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  const supertest = require('supertest');
  request = supertest(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
  delete process.env.CLINICAL_TOOL_STRATEGY;
});

describe('clinical live setup — seed and discover', () => {
  beforeEach(() => {
    if (!gatewayReachable) pending();
  });

  it('discovers patients via fhir_search', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Patient',
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');

    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();

    const patients = data.data.entry || [];
    expect(patients.length).toBeGreaterThan(0);

    const ctx = readTestContext() || { patients: {}, resources: {} };
    if (patients.length > 0) {
      ctx.discoveredPatientId = patients[0].resource.id;
    }
    writeTestContext(ctx);
    expect(ctx.discoveredPatientId).toBeDefined();
  });

  it('discovers observations for a patient', async () => {
    const ctx = readTestContext();
    if (!ctx?.discoveredPatientId) pending();

    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Observation',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');

    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('discovers conditions for a patient', async () => {
    const ctx = readTestContext();
    if (!ctx?.discoveredPatientId) pending();

    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Condition',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');

    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 2: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-clinical/__tests__/live/setup.live.test.js --no-coverage`
Expected: Tests pending/skipped

**Step 3: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-clinical/__tests__/live/setup.live.test.js
git commit -m "feat(live-tests): add clinical live test setup (seed + discovery)

- Discovers patient IDs via fhir_search
- Writes context to /tmp/live-test-context.json
- Runs first in --runInBand sequence"
```

---

## Task 7: Clinical FHIR Live Tests

**Files:**
- Create: `mcp-edge-clinical/__tests__/live/fhir-tools.live.test.js`

**Step 1: Write the test file**

Create `mcp-edge-clinical/__tests__/live/fhir-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) {
    console.warn('Gateway/context unavailable — skipping FHIR live tests');
    gatewayReachable = false;
    return;
  }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
  delete process.env.CLINICAL_TOOL_STRATEGY;
});

describe('clinical FHIR tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('fhir_read retrieves a Patient by ID', async () => {
    const res = await callTool(request, 'fhir_read', {
      resourceType: 'Patient',
      resourceId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(200);
    expect(data.data.resourceType).toBe('Patient');
    expect(data.data.id).toBe(ctx.discoveredPatientId);
  });

  it('fhir_search finds patients', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Patient',
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data.resourceType).toBe('Bundle');
    expect(data.data.entry.length).toBeGreaterThan(0);
  });

  it('fhir_search finds observations for a patient', async () => {
    const res = await callTool(request, 'fhir_search', {
      resourceType: 'Observation',
      patient: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('fhir_create creates an Observation resource', async () => {
    const res = await callTool(request, 'fhir_create', {
      resourceType: 'Observation',
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      resource: {
        resourceType: 'Observation',
        status: 'final',
        subject: { reference: `Patient/${ctx.discoveredPatientId}` },
        code: {
          coding: [{ system: 'http://loinc.org', code: '8310-5', display: 'Body temperature' }]
        },
        valueQuantity: { value: 37.0, unit: 'Cel', system: 'http://unitsofmeasure.org', code: 'Cel' }
      }
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.status).toBe(201);
  });
});
```

**Step 2: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-clinical/__tests__/live/fhir-tools.live.test.js --no-coverage`
Expected: Tests pending/skipped

**Step 3: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-clinical/__tests__/live/fhir-tools.live.test.js
git commit -m "feat(live-tests): add FHIR tool live tests

- fhir_read, fhir_search, fhir_create round-trips
- Response contract validation (resourceType, status, ok)
- Depends on setup.live.test.js for patient context"
```

---

## Task 8: Clinical Patient & Care Gap Live Tests

**Files:**
- Create: `mcp-edge-clinical/__tests__/live/patient-tools.live.test.js`
- Create: `mcp-edge-clinical/__tests__/live/care-gap-tools.live.test.js`

**Step 1: Write patient tools test**

Create `mcp-edge-clinical/__tests__/live/patient-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

describe('patient tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('patient_summary returns health record bundle', async () => {
    const res = await callTool(request, 'patient_summary', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });

  it('patient_timeline returns encounter timeline', async () => {
    const res = await callTool(request, 'patient_timeline', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('patient_risk returns risk assessment', async () => {
    const res = await callTool(request, 'patient_risk', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('patient_list returns patient roster', async () => {
    const res = await callTool(request, 'patient_list', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
    expect(data.data).toBeDefined();
  });
});
```

**Step 2: Write care gap tools test**

Create `mcp-edge-clinical/__tests__/live/care-gap-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

describe('care gap tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('care_gap_list returns open care gaps for a patient', async () => {
    const res = await callTool(request, 'care_gap_list', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('care_gap_identify detects care gaps for a patient', async () => {
    const res = await callTool(request, 'care_gap_identify', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('care_gap_stats returns aggregate statistics', async () => {
    const res = await callTool(request, 'care_gap_stats', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('care_gap_population returns population-level gaps', async () => {
    const res = await callTool(request, 'care_gap_population', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('care_gap_close closes a care gap (if one exists)', async () => {
    const listRes = await callTool(request, 'care_gap_list', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    const listData = JSON.parse(listRes.body.result.content[0].text);

    const gaps = listData.data?.gaps || listData.data?.entry || [];
    if (gaps.length === 0) {
      console.warn('No care gaps to close — skipping close test');
      return;
    }

    const gapId = gaps[0].id || gaps[0].gapId;
    const res = await callTool(request, 'care_gap_close', {
      gapId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      closedBy: 'live-test-runner',
      reason: 'Automated live test verification'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 3: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-clinical/__tests__/live/patient-tools.live.test.js mcp-edge-clinical/__tests__/live/care-gap-tools.live.test.js --no-coverage`
Expected: All tests pending/skipped

**Step 4: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-clinical/__tests__/live/patient-tools.live.test.js mcp-edge-clinical/__tests__/live/care-gap-tools.live.test.js
git commit -m "feat(live-tests): add patient and care gap live tests

- patient_summary, patient_timeline, patient_risk, patient_list
- care_gap_list, care_gap_identify, care_gap_stats, care_gap_population
- care_gap_close (write test, runs conditionally)"
```

---

## Task 9: Clinical Quality Measure & CQL Live Tests

**Files:**
- Create: `mcp-edge-clinical/__tests__/live/quality-tools.live.test.js`
- Create: `mcp-edge-clinical/__tests__/live/cql-tools.live.test.js`

**Step 1: Write quality measure test**

Create `mcp-edge-clinical/__tests__/live/quality-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

describe('quality measure tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('measure_evaluate evaluates a quality measure', async () => {
    const res = await callTool(request, 'measure_evaluate', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      measureId: 'HbA1c-Control'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('measure_results returns results for a patient', async () => {
    const res = await callTool(request, 'measure_results', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('measure_score returns compliance score', async () => {
    const res = await callTool(request, 'measure_score', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('measure_population returns population-level measure data', async () => {
    const res = await callTool(request, 'measure_population', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 2: Write CQL test**

Create `mcp-edge-clinical/__tests__/live/cql-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

describe('CQL tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('cql_evaluate evaluates a CQL library against a patient', async () => {
    const res = await callTool(request, 'cql_evaluate', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryId: 'HbA1c-Control'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('cql_batch evaluates multiple CQL libraries', async () => {
    const res = await callTool(request, 'cql_batch', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryIds: ['HbA1c-Control']
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('cql_result retrieves stored CQL results', async () => {
    const res = await callTool(request, 'cql_result', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId,
      libraryId: 'HbA1c-Control'
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('cql_libraries lists available CQL libraries', async () => {
    const res = await callTool(request, 'cql_libraries', {
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 3: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-clinical/__tests__/live/quality-tools.live.test.js mcp-edge-clinical/__tests__/live/cql-tools.live.test.js --no-coverage`
Expected: All tests pending/skipped

**Step 4: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-clinical/__tests__/live/quality-tools.live.test.js mcp-edge-clinical/__tests__/live/cql-tools.live.test.js
git commit -m "feat(live-tests): add quality measure and CQL live tests

- measure_evaluate, measure_results, measure_score, measure_population
- cql_evaluate, cql_batch, cql_result, cql_libraries"
```

---

## Task 10: Clinical CDS Live Tests

**Files:**
- Create: `mcp-edge-clinical/__tests__/live/cds-tools.live.test.js`

**Step 1: Write CDS test**

Create `mcp-edge-clinical/__tests__/live/cds-tools.live.test.js`:

```js
const {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  callTool,
  readTestContext
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, gatewayReachable = false, ctx;

beforeAll(async () => {
  gatewayReachable = await isGatewayReachable(LIVE_TEST_DEFAULTS.gatewayUrl);
  ctx = gatewayReachable ? readTestContext() : null;
  if (!gatewayReachable || !ctx?.discoveredPatientId) { gatewayReachable = false; return; }
  process.env.HDIM_DEMO_MODE = 'false';
  process.env.CLINICAL_TOOL_STRATEGY = 'composite';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; delete process.env.CLINICAL_TOOL_STRATEGY; });

describe('CDS tools — live gateway', () => {
  beforeEach(() => { if (!gatewayReachable) pending(); });

  it('cds_patient_view returns CDS cards for a patient', async () => {
    const res = await callTool(request, 'cds_patient_view', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('health_score returns patient health score', async () => {
    const res = await callTool(request, 'health_score', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });

  it('pre_visit_plan generates a pre-visit summary', async () => {
    const res = await callTool(request, 'pre_visit_plan', {
      patientId: ctx.discoveredPatientId,
      tenantId: LIVE_TEST_DEFAULTS.tenantId
    }, 'clinical_admin');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data.ok).toBe(true);
  });
});
```

**Step 2: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-clinical/__tests__/live/cds-tools.live.test.js --no-coverage`
Expected: All tests pending/skipped

**Step 3: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-clinical/__tests__/live/cds-tools.live.test.js
git commit -m "feat(live-tests): add CDS live tests

- cds_patient_view, health_score, pre_visit_plan
- Response contract validation for CDS cards"
```

---

## Task 11: DevOps Live Tests

**Files:**
- Create: `mcp-edge-devops/__tests__/live/devops-tools.live.test.js`

**Step 1: Write devops live test**

Create `mcp-edge-devops/__tests__/live/devops-tools.live.test.js`:

```js
const {
  isDockerReachable,
  callTool
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request, dockerReachable = false;

beforeAll(async () => {
  dockerReachable = await isDockerReachable();
  if (!dockerReachable) {
    console.warn('Docker unreachable — skipping devops live tests');
    return;
  }
  process.env.HDIM_DEMO_MODE = 'false';
  jest.resetModules();
  const { createApp } = require('../../server');
  request = require('supertest')(createApp());
});

afterAll(() => { delete process.env.HDIM_DEMO_MODE; });

describe('devops tools — live Docker', () => {
  beforeEach(() => { if (!dockerReachable) pending(); });

  it('docker_status reports running containers', async () => {
    const res = await callTool(request, 'docker_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  it('docker_logs retrieves logs from a container', async () => {
    const statusRes = await callTool(request, 'docker_status');
    const statusData = JSON.parse(statusRes.body.result.content[0].text);
    const containers = statusData.containers || statusData.data?.containers || [];

    if (containers.length === 0) {
      console.warn('No containers found — skipping log test');
      return;
    }

    const containerName = containers[0].name || containers[0].Names;
    const res = await callTool(request, 'docker_logs', { containerName, lines: 10 });
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  it('service_dependencies returns dependency graph', async () => {
    const res = await callTool(request, 'service_dependencies');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  it('compose_config reads docker-compose configuration', async () => {
    const res = await callTool(request, 'compose_config');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  it('build_status reports container image info', async () => {
    const res = await callTool(request, 'build_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });
});
```

**Step 2: Verify skip mechanism**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest mcp-edge-devops/__tests__/live/devops-tools.live.test.js --no-coverage`
Expected: Tests pending/skipped if Docker unavailable, or pass if Docker running

**Step 3: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add mcp-edge-devops/__tests__/live/
git commit -m "feat(live-tests): add devops sidecar live tests

- docker_status, docker_logs, service_dependencies
- compose_config, build_status
- Graceful skip when Docker unreachable"
```

---

## Task 12: CI Workflow — Live Tests Job

**Files:**
- Modify: `.github/workflows/mcp-edge-ci.yml`

**Step 1: Read current CI workflow**

Read: `.github/workflows/mcp-edge-ci.yml` to see the current structure.

**Step 2: Add live-tests job after existing jobs**

Add the following job to the workflow:

```yaml
  live-tests:
    name: MCP Edge Live Integration
    runs-on: ubuntu-latest
    timeout-minutes: 15
    if: github.ref == 'refs/heads/master' || startsWith(github.ref, 'refs/heads/release/')
    needs: [test]

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Start HDIM stack
        run: docker compose -f docker-compose.demo.yml up -d

      - name: Wait for gateway health
        run: |
          echo "Waiting for gateway..."
          for i in $(seq 1 60); do
            if curl -sf http://localhost:18080/actuator/health > /dev/null 2>&1; then
              echo "Gateway healthy after ${i}s"
              break
            fi
            if [ $i -eq 60 ]; then
              echo "Gateway failed to start"
              docker compose -f docker-compose.demo.yml logs
              exit 1
            fi
            sleep 2
          done

      - name: Install dependencies
        run: npm ci

      - name: Load synthetic patients
        run: node test-data/synthetic-patients/load-test-patients.js
        env:
          HDIM_BASE_URL: http://localhost:18080

      - name: Run live tests
        run: npx jest --testPathPattern=live --runInBand --no-coverage --forceExit
        env:
          HDIM_BASE_URL: http://localhost:18080
          HDIM_DEMO_MODE: 'false'
          LIVE_TESTS: 'true'
          MCP_EDGE_ENFORCE_ROLE_AUTH: 'true'

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: live-test-results
          path: /tmp/live-test-context.json
          retention-days: 7

      - name: Teardown
        if: always()
        run: docker compose -f docker-compose.demo.yml down -v
```

**Step 3: Verify YAML validity**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && python3 -c "import yaml; yaml.safe_load(open('.github/workflows/mcp-edge-ci.yml'))" && echo "YAML valid"`
Expected: "YAML valid"

**Step 4: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add .github/workflows/mcp-edge-ci.yml
git commit -m "ci: add live integration test job

- Runs on master and release/* branches only
- Starts full HDIM stack via docker-compose.demo.yml
- Loads synthetic patients, runs live tests in-band
- Gated behind test job (unit tests must pass first)"
```

---

## Task 13: Clinical Validation Documentation

**Files:**
- Create: `docs/clinical-validation/phenotype-profiles.md`
- Create: `docs/clinical-validation/synthea-methodology.md`
- Create: `docs/clinical-validation/validation-matrix.md`

**Step 1: Create phenotype profiles**

Create `docs/clinical-validation/phenotype-profiles.md` with the 6 phenotype profiles. Each profile includes:
- Demographics and clinical data
- Clinical rationale with guideline references (ADA, USPSTF, ACC/AHA, ACIP, GOLD, KDIGO)
- Expected platform behavior (specific tool → expected outcome)

See design doc Section "Phenotype-Specific Assertions" for the exact expected outcomes per phenotype.

**Step 2: Create Synthea methodology doc**

Create `docs/clinical-validation/synthea-methodology.md` covering:
- What Synthea is (MITRE open-source, Apache 2.0)
- Peer-reviewed validation (Walonoski et al. 2018, JAMIA 25(3):230-238)
- Regulatory adoption (ONC, CMS, HL7 FHIR)
- Our approach: base Synthea generation + hand-crafted overlays
- Reproducibility: deterministic seeds, committed bundles, manifest

**Step 3: Create validation matrix**

Create `docs/clinical-validation/validation-matrix.md` with:
- Phenotype × Tool matrix (18 rows mapping phenotype → tool → expected outcome → clinical basis)
- Coverage summary (7 tool categories × phenotypes used)
- Total: 40+ tool round-trip tests across 6 phenotypes

**Step 4: Commit**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add docs/clinical-validation/
git commit -m "docs: add clinical validation documentation

- Phenotype profiles with clinical rationale and literature references
- Synthea methodology (MITRE provenance, JAMIA peer review, ONC/CMS usage)
- Validation matrix mapping phenotypes to tools and expected outcomes"
```

---

## Task 14: Integration Smoke Test

**Files:** None new — verifies everything works together.

**Step 1: Run full unit test suite**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest --projects mcp-edge-common mcp-edge-platform mcp-edge-clinical mcp-edge-devops --coverage --ci`
Expected: PASS — all existing tests pass, coverage >= 95%

**Step 2: Run live tests in skip mode**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest --testPathPattern=live --runInBand --no-coverage --forceExit`
Expected: All live tests show as pending/skipped (no gateway running)

**Step 3: Verify no live tests leaked into default runs**

Run: `cd /mnt/wdblack/dev/projects/hdim-master && npx jest --projects mcp-edge-platform mcp-edge-clinical mcp-edge-devops --listTests | grep live`
Expected: No output (live tests excluded by testPathIgnorePatterns)

**Step 4: Final commit (if anything remains unstaged)**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git status
# Only commit if there are remaining changes
```

---

## Summary

| Task | Files | Tests Added | Description |
|------|-------|-------------|-------------|
| 1 | 2 | 5 | Shared live test utilities |
| 2 | 14 | 0 | Phenotype manifest, bundles, overlays, generate.sh |
| 3 | 2 | 5 | Test data loader + manifest validation |
| 4 | 3 | 0 | Jest config excludes *.live.test.js |
| 5 | 1 | 8 | Platform live tests |
| 6 | 1 | 3 | Clinical setup (seed + discovery) |
| 7 | 1 | 4 | FHIR tool live tests |
| 8 | 2 | 9 | Patient + care gap live tests |
| 9 | 2 | 8 | Quality measure + CQL live tests |
| 10 | 1 | 3 | CDS live tests |
| 11 | 1 | 5 | DevOps live tests |
| 12 | 1 | 0 | CI workflow live-tests job |
| 13 | 3 | 0 | Clinical validation docs |
| 14 | 0 | 0 | Integration smoke test |

**Total:** ~30 new files, ~50 new live tests, 14 commits
