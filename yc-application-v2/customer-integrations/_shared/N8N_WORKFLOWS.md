# n8n Workflow Examples

> Pre-built and customizable n8n workflow templates for integrating legacy systems, custom data sources, and non-standard formats with HDIM.

## Table of Contents

1. [Overview](#overview)
2. [Deployment Options](#deployment-options)
3. [Common Workflows](#common-workflows)
   - [CSV File Import](#csv-file-import)
   - [SFTP Lab Results](#sftp-lab-results)
   - [HL7v2 Message Processing](#hl7v2-message-processing)
   - [Database Direct Connect](#database-direct-connect)
   - [Multi-Source Aggregation](#multi-source-aggregation)
4. [HDIM API Integration](#hdim-api-integration)
5. [Error Handling](#error-handling)
6. [Monitoring & Alerting](#monitoring--alerting)

---

## Overview

n8n is an open-source workflow automation platform that enables rapid, custom integrations without extensive development. HDIM uses n8n to connect legacy systems, process non-standard data formats, and aggregate data from multiple sources.

### Why n8n?

| Benefit | Description |
|---------|-------------|
| **Speed** | Build integrations in days, not months |
| **Flexibility** | Connect any data source with 400+ nodes |
| **Visual** | Low-code drag-and-drop interface |
| **Open Source** | No vendor lock-in, self-hostable |
| **Debuggable** | Step-by-step execution with data preview |

### When to Use n8n

- Legacy EHR without FHIR API
- Custom file formats (CSV, Excel, XML)
- SFTP/FTP data feeds
- Direct database connections
- HL7v2 message processing (via transform)
- Multi-system data aggregation
- Custom transformation logic

---

## Deployment Options

### Option 1: HDIM-Managed Cloud (Recommended)

```
┌─────────────────────────────────────────────────────────────┐
│                     Customer Environment                    │
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                │
│  │  EHR    │    │   Lab   │    │ Claims  │                │
│  └────┬────┘    └────┬────┘    └────┬────┘                │
│       │              │              │                       │
│       └──────────────┼──────────────┘                       │
│                      │                                      │
│              ┌───────▼───────┐                             │
│              │  SFTP/Secure  │                             │
│              │   File Drop   │                             │
│              └───────┬───────┘                             │
└──────────────────────┼──────────────────────────────────────┘
                       │ TLS 1.3
              ┌────────▼────────┐
              │   HDIM Cloud    │
              │  ┌───────────┐  │
              │  │   n8n     │  │
              │  │ (managed) │  │
              │  └─────┬─────┘  │
              │        │        │
              │  ┌─────▼─────┐  │
              │  │  HDIM API │  │
              │  └───────────┘  │
              └─────────────────┘
```

**Best for:** Most customers, minimal IT overhead
**Setup:** 2-3 days
**Maintenance:** HDIM handles updates and monitoring

### Option 2: Customer On-Premise

```
┌─────────────────────────────────────────────────────────────┐
│                     Customer Environment                    │
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                │
│  │  EHR    │    │   Lab   │    │ Claims  │                │
│  └────┬────┘    └────┬────┘    └────┬────┘                │
│       │              │              │                       │
│       └──────────────┼──────────────┘                       │
│                      │                                      │
│              ┌───────▼───────┐                             │
│              │     n8n       │                             │
│              │  (on-prem)    │                             │
│              └───────┬───────┘                             │
│                      │ TLS 1.3                             │
└──────────────────────┼──────────────────────────────────────┘
                       │
              ┌────────▼────────┐
              │   HDIM Cloud    │
              │  ┌───────────┐  │
              │  │  HDIM API │  │
              │  └───────────┘  │
              └─────────────────┘
```

**Best for:** Strict data residency, PHI must stay on-prem
**Setup:** 3-5 days
**Maintenance:** Customer manages n8n instance

### Option 3: Hybrid

n8n runs on-prem for data transformation, connects to HDIM cloud for quality measurement.

---

## Common Workflows

### CSV File Import

**Use case:** Daily/weekly CSV exports from legacy EHR

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  SFTP    │───►│  Parse   │───►│Transform │───►│  HDIM    │
│  Trigger │    │   CSV    │    │ to FHIR  │    │   API    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

#### Workflow JSON

```json
{
  "name": "CSV Patient Import",
  "nodes": [
    {
      "name": "SFTP Trigger",
      "type": "n8n-nodes-base.sftpTrigger",
      "parameters": {
        "host": "sftp.customer.com",
        "port": 22,
        "path": "/exports/patients/",
        "filePattern": "patients_*.csv",
        "pollInterval": 3600
      }
    },
    {
      "name": "Parse CSV",
      "type": "n8n-nodes-base.spreadsheetFile",
      "parameters": {
        "operation": "fromFile",
        "fileFormat": "csv",
        "includeHeaderRow": true
      }
    },
    {
      "name": "Transform to FHIR",
      "type": "n8n-nodes-base.code",
      "parameters": {
        "language": "javascript",
        "code": "// Transform CSV row to FHIR Patient\nconst patient = {\n  resourceType: 'Patient',\n  id: $input.item.json.mrn,\n  identifier: [{\n    system: 'http://customer.com/mrn',\n    value: $input.item.json.mrn\n  }],\n  name: [{\n    family: $input.item.json.last_name,\n    given: [$input.item.json.first_name]\n  }],\n  gender: $input.item.json.gender.toLowerCase() === 'm' ? 'male' : 'female',\n  birthDate: $input.item.json.dob\n};\nreturn { json: patient };"
      }
    },
    {
      "name": "HDIM API",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "https://api.healthdatainmotion.com/v1/fhir/Patient",
        "authentication": "genericCredentialType",
        "genericAuthType": "oAuth2Api",
        "sendBody": true,
        "bodyParameters": {
          "body": "={{ $json }}"
        }
      }
    }
  ]
}
```

---

### SFTP Lab Results

**Use case:** Lab system drops results files on SFTP

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  SFTP    │───►│  Parse   │───►│  Match   │───►│Transform │───►│  HDIM    │
│  Watch   │    │   File   │    │ Patient  │    │ to FHIR  │    │   API    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

#### Key Components

```javascript
// Parse lab result and create FHIR Observation
const observation = {
  resourceType: 'Observation',
  status: 'final',
  category: [{
    coding: [{
      system: 'http://terminology.hl7.org/CodeSystem/observation-category',
      code: 'laboratory'
    }]
  }],
  code: {
    coding: [{
      system: 'http://loinc.org',
      code: $input.item.json.loinc_code,
      display: $input.item.json.test_name
    }]
  },
  subject: {
    reference: `Patient/${patientId}`
  },
  effectiveDateTime: $input.item.json.result_date,
  valueQuantity: {
    value: parseFloat($input.item.json.result_value),
    unit: $input.item.json.unit,
    system: 'http://unitsofmeasure.org'
  }
};
```

---

### HL7v2 Message Processing

**Use case:** Legacy system sends HL7v2 ADT/ORU messages

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  MLLP    │───►│  Parse   │───►│ Extract  │───►│Transform │───►│  HDIM    │
│ Listener │    │  HL7v2   │    │ Segments │    │ to FHIR  │    │   API    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

#### HL7v2 Parser Code

```javascript
// Parse HL7v2 message segments
function parseHL7(message) {
  const segments = message.split('\r');
  const parsed = {};

  segments.forEach(segment => {
    const fields = segment.split('|');
    const segmentType = fields[0];

    switch(segmentType) {
      case 'PID':
        parsed.patient = {
          mrn: fields[3].split('^')[0],
          lastName: fields[5].split('^')[0],
          firstName: fields[5].split('^')[1],
          dob: fields[7],
          gender: fields[8]
        };
        break;
      case 'OBX':
        if (!parsed.observations) parsed.observations = [];
        parsed.observations.push({
          code: fields[3].split('^')[0],
          display: fields[3].split('^')[1],
          value: fields[5],
          unit: fields[6].split('^')[0],
          status: fields[11]
        });
        break;
    }
  });

  return parsed;
}
```

#### Transform to FHIR

```javascript
// Convert parsed HL7v2 to FHIR resources
const bundle = {
  resourceType: 'Bundle',
  type: 'transaction',
  entry: []
};

// Add Patient
bundle.entry.push({
  resource: {
    resourceType: 'Patient',
    identifier: [{ value: parsed.patient.mrn }],
    name: [{
      family: parsed.patient.lastName,
      given: [parsed.patient.firstName]
    }],
    birthDate: formatDate(parsed.patient.dob),
    gender: parsed.patient.gender === 'M' ? 'male' : 'female'
  },
  request: { method: 'PUT', url: `Patient/${parsed.patient.mrn}` }
});

// Add Observations
parsed.observations.forEach(obs => {
  bundle.entry.push({
    resource: {
      resourceType: 'Observation',
      status: 'final',
      code: { coding: [{ code: obs.code, display: obs.display }] },
      subject: { reference: `Patient/${parsed.patient.mrn}` },
      valueQuantity: { value: parseFloat(obs.value), unit: obs.unit }
    },
    request: { method: 'POST', url: 'Observation' }
  });
});

return { json: bundle };
```

---

### Database Direct Connect

**Use case:** Direct connection to EHR database (read-only)

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Cron    │───►│  Query   │───►│Transform │───►│  HDIM    │
│ Schedule │    │   DB     │    │ to FHIR  │    │   API    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

#### SQL Query Node

```sql
-- Daily patient sync query
SELECT
  p.patient_id as mrn,
  p.first_name,
  p.last_name,
  p.date_of_birth as dob,
  p.gender,
  p.phone,
  p.email
FROM patients p
WHERE p.modified_date >= DATE_SUB(NOW(), INTERVAL 1 DAY)
  AND p.status = 'ACTIVE';
```

#### Database Credentials

Supported databases:
- PostgreSQL
- MySQL / MariaDB
- Microsoft SQL Server
- Oracle
- MongoDB

---

### Multi-Source Aggregation

**Use case:** IPA with 15 different EHRs

```
                    ┌──────────┐
                    │  n8n     │
                    │  Hub     │
                    └────┬─────┘
                         │
    ┌────────────────────┼────────────────────┐
    │                    │                    │
┌───▼───┐           ┌────▼────┐          ┌────▼────┐
│ EHR 1 │           │  EHR 2  │          │  EHR N  │
│ FHIR  │           │   CSV   │          │  HL7v2  │
└───┬───┘           └────┬────┘          └────┬────┘
    │                    │                    │
    └────────────────────┼────────────────────┘
                         │
                    ┌────▼─────┐
                    │ Aggregate│
                    │ & Dedupe │
                    └────┬─────┘
                         │
                    ┌────▼─────┐
                    │  HDIM    │
                    │   API    │
                    └──────────┘
```

#### Aggregation Logic

```javascript
// Deduplicate patients across sources
function deduplicatePatients(patients) {
  const seen = new Map();

  patients.forEach(patient => {
    // Create matching key
    const key = `${patient.lastName.toLowerCase()}_${patient.firstName.toLowerCase()}_${patient.dob}`;

    if (seen.has(key)) {
      // Merge identifiers
      const existing = seen.get(key);
      existing.identifier = [...existing.identifier, ...patient.identifier];
    } else {
      seen.set(key, patient);
    }
  });

  return Array.from(seen.values());
}
```

---

## HDIM API Integration

### Authentication

```javascript
// OAuth 2.0 client credentials flow
const tokenResponse = await $http.request({
  method: 'POST',
  url: 'https://auth.healthdatainmotion.com/oauth/token',
  body: {
    grant_type: 'client_credentials',
    client_id: $credentials.hdim.clientId,
    client_secret: $credentials.hdim.clientSecret,
    scope: 'fhir:write'
  }
});

const accessToken = tokenResponse.access_token;
```

### FHIR Resource Upload

```javascript
// Upload FHIR Bundle
const response = await $http.request({
  method: 'POST',
  url: 'https://api.healthdatainmotion.com/v1/fhir',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/fhir+json'
  },
  body: bundle
});
```

### Batch Upload (Large Datasets)

```javascript
// Chunk large datasets
const BATCH_SIZE = 100;
const batches = [];

for (let i = 0; i < resources.length; i += BATCH_SIZE) {
  const batch = resources.slice(i, i + BATCH_SIZE);
  batches.push({
    resourceType: 'Bundle',
    type: 'batch',
    entry: batch.map(r => ({
      resource: r,
      request: { method: 'POST', url: r.resourceType }
    }))
  });
}

// Process batches sequentially
for (const batch of batches) {
  await uploadBatch(batch);
  await sleep(100); // Rate limiting
}
```

---

## Error Handling

### Retry Logic

```javascript
// Exponential backoff retry
async function retryWithBackoff(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      const delay = Math.pow(2, i) * 1000;
      await sleep(delay);
    }
  }
}
```

### Error Logging

```javascript
// Log errors for troubleshooting
if (response.statusCode >= 400) {
  const error = {
    timestamp: new Date().toISOString(),
    workflow: 'CSV Patient Import',
    source: 'HDIM API',
    statusCode: response.statusCode,
    message: response.body,
    input: $input.item.json.mrn
  };

  // Send to error queue
  await $http.request({
    method: 'POST',
    url: 'https://api.healthdatainmotion.com/v1/errors',
    body: error
  });
}
```

### Dead Letter Queue

Failed records are stored for manual review:

```javascript
// Store failed record
await $http.request({
  method: 'POST',
  url: 'https://api.healthdatainmotion.com/v1/dlq',
  body: {
    workflowId: $workflow.id,
    executionId: $execution.id,
    record: $input.item.json,
    error: errorMessage,
    retryCount: 0
  }
});
```

---

## Monitoring & Alerting

### Health Check Workflow

```json
{
  "name": "Integration Health Check",
  "nodes": [
    {
      "name": "Cron",
      "type": "n8n-nodes-base.cron",
      "parameters": { "cronExpression": "0 */5 * * * *" }
    },
    {
      "name": "Check HDIM API",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "https://api.healthdatainmotion.com/health"
      }
    },
    {
      "name": "Alert on Failure",
      "type": "n8n-nodes-base.slack",
      "parameters": {
        "channel": "#hdim-alerts",
        "text": "HDIM API health check failed"
      }
    }
  ]
}
```

### Execution Metrics

Track in HDIM dashboard:
- Workflow execution count
- Success/failure rate
- Average execution time
- Records processed per run
- Error breakdown by type

### Alert Channels

| Severity | Channel | Response |
|----------|---------|----------|
| Critical | PagerDuty | Immediate |
| High | Slack + Email | 1 hour |
| Medium | Email | 4 hours |
| Low | Dashboard | Next business day |

---

## Importable Workflow Templates

Ready-to-use n8n workflow JSON files are available in the [n8n-workflows](../n8n-workflows/) directory:

### Common Workflows

| Workflow | File | Description |
|----------|------|-------------|
| HDIM API Auth | [hdim-api-auth.workflow.json](../n8n-workflows/common/hdim-api-auth.workflow.json) | Reusable OAuth 2.0 authentication sub-workflow |

### FQHC Workflows

| Workflow | File | Description |
|----------|------|-------------|
| Quest Lab Import | [quest-lab-import.workflow.json](../n8n-workflows/fqhc/quest-lab-import.workflow.json) | SFTP-based Quest Diagnostics lab import with LOINC mapping |

### Rural Hospital Workflows

| Workflow | File | Description |
|----------|------|-------------|
| Meditech Daily Sync | [meditech-daily-sync.workflow.json](../n8n-workflows/rural-hospital/meditech-daily-sync.workflow.json) | Multi-resource daily sync from Meditech Expanse |

### IPA Workflows

| Workflow | File | Description |
|----------|------|-------------|
| athena FHIR Sync | [athena-fhir-sync.workflow.json](../n8n-workflows/ipa/athena-fhir-sync.workflow.json) | Multi-practice FHIR R4 sync from athenahealth |
| Universal CSV Import | [universal-csv-import.workflow.json](../n8n-workflows/ipa/universal-csv-import.workflow.json) | Multi-EHR CSV import (DrChrono, Practice Fusion, Kareo, Elation) |

### How to Import

1. **n8n UI**: Workflows → Import from File → Select `.workflow.json`
2. **n8n CLI**: `n8n import:workflow --input=workflow.json`
3. **n8n API**: POST to `/api/v1/workflows` with JSON body

See [n8n-workflows/README.md](../n8n-workflows/README.md) for complete documentation.

---

## Pricing

| Component | Cost |
|-----------|------|
| HDIM-managed n8n | Included with Enterprise tier |
| Custom workflow development | $1,500-5,000 per workflow |
| Workflow modifications | $150/hour |
| On-prem n8n setup | $2,500 one-time |

---

*n8n Workflows Version: 1.0*
*n8n Version: 1.x*
*Last Updated: December 2025*
