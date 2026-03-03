# HDIM n8n Workflow Templates

> Production-ready n8n workflow JSON files for common HDIM integration scenarios.

## Overview

This directory contains importable n8n workflow definitions that can be directly loaded into any n8n instance. Each workflow is designed for a specific customer scenario and includes:

- Complete node configurations
- FHIR transformation logic
- Error handling
- Credential placeholders
- Monitoring/alerting hooks

## Directory Structure

```
n8n-workflows/
├── README.md                    # This file
├── common/                      # Shared/reusable workflows
│   └── hdim-api-auth.workflow.json
├── fqhc/                        # FQHC-specific workflows
│   └── quest-lab-import.workflow.json
├── rural-hospital/              # Rural hospital workflows
│   └── meditech-daily-sync.workflow.json
└── ipa/                         # IPA multi-practice workflows
    ├── athena-fhir-sync.workflow.json
    └── universal-csv-import.workflow.json
```

## Workflow Catalog

### Common Workflows

| Workflow | Description | Use Case |
|----------|-------------|----------|
| [hdim-api-auth](common/hdim-api-auth.workflow.json) | OAuth 2.0 authentication sub-workflow | All integrations |

### FQHC Workflows

| Workflow | Description | Frequency | Records |
|----------|-------------|-----------|---------|
| [quest-lab-import](fqhc/quest-lab-import.workflow.json) | Import Quest lab results via SFTP | Every 4 hours | ~500/day |

**Features:**
- SFTP polling for new CSV files
- LOINC code mapping
- FHIR Observation transformation
- Reference range interpretation
- Batch upload with retry logic
- File archiving after processing

### Rural Hospital Workflows

| Workflow | Description | Frequency | Records |
|----------|-------------|-----------|---------|
| [meditech-daily-sync](rural-hospital/meditech-daily-sync.workflow.json) | Daily sync from Meditech Expanse | Daily 4 AM | ~750/day |

**Features:**
- Multi-file type processing (patients, conditions, meds, labs, encounters)
- Meditech-specific code mapping
- Readmission tracking support
- Daily summary email notifications

### IPA Workflows

| Workflow | Description | Practices | Frequency |
|----------|-------------|-----------|-----------|
| [athena-fhir-sync](ipa/athena-fhir-sync.workflow.json) | athenahealth FHIR R4 sync | 18 | Daily 2 AM |
| [universal-csv-import](ipa/universal-csv-import.workflow.json) | Multi-EHR CSV import | 28 | Weekly Monday |

**Features:**
- Practice-level configuration
- Multi-EHR column mapping (DrChrono, Practice Fusion, Kareo, Elation)
- Automatic file type detection
- Practice attribution tagging
- Aggregate reporting

## How to Import

### Method 1: n8n UI

1. Open n8n dashboard
2. Click **Workflows** → **Import from File**
3. Select the `.workflow.json` file
4. Configure credentials (see below)
5. Activate workflow

### Method 2: n8n CLI

```bash
n8n import:workflow --input=/path/to/workflow.json
```

### Method 3: n8n API

```bash
curl -X POST "http://localhost:5678/api/v1/workflows" \
  -H "X-N8N-API-KEY: your-api-key" \
  -H "Content-Type: application/json" \
  -d @workflow.json
```

## Required Credentials

Each workflow requires specific credentials to be configured in n8n:

### HDIM API (All Workflows)

```
Name: HDIM API
Type: OAuth2
Client ID: [from HDIM dashboard]
Client Secret: [from HDIM dashboard]
Token URL: https://auth.healthdatainmotion.com/oauth/token
Scope: fhir:read fhir:write
```

### SFTP Credentials

```
Name: [varies by workflow]
Type: SFTP
Host: [customer SFTP host]
Port: 22
Username: [service account]
Private Key: [SSH private key]
```

### EHR API Credentials

| EHR | Auth Type | Required Fields |
|-----|-----------|-----------------|
| athenahealth | HTTP Basic | Client ID, Client Secret |
| Epic | OAuth 2.0 | Client ID, Private Key (JWT) |
| Cerner | OAuth 2.0 | Client ID, Client Secret |
| eClinicalWorks | OAuth 2.0 | Client ID, Client Secret |

## Environment Variables

Workflows reference these environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `HDIM_TENANT_ID` | HDIM tenant identifier | `tenant-abc123` |
| `ALERT_EMAIL` | Error notification recipient | `it@practice.com` |
| `SUMMARY_EMAIL` | Daily summary recipient | `admin@practice.com` |

Set in n8n:
```bash
export HDIM_TENANT_ID=tenant-abc123
export ALERT_EMAIL=alerts@example.com
```

## Customization

### Adding New EHR Mappings

1. Open the workflow JSON
2. Find the column mapping code node
3. Add new EHR configuration:

```javascript
const columnMaps = {
  // Add new EHR here
  'new-ehr': {
    mrn: 'patient_number',
    firstName: 'first',
    lastName: 'last',
    dob: 'birth_date',
    gender: 'sex'
  }
};
```

### Adjusting Schedules

Modify the `scheduleTrigger` node parameters:

```json
{
  "parameters": {
    "rule": {
      "interval": [{
        "field": "cronExpression",
        "cronExpression": "0 6 * * 1-5"  // Weekdays 6 AM
      }]
    }
  }
}
```

### Adding New Resource Types

1. Add route in the Switch node
2. Create transform code node
3. Connect to merge node

## Monitoring

### Execution Logs

All workflows log structured JSON for monitoring:

```json
{
  "timestamp": "2024-12-15T04:00:00Z",
  "workflow": "FQHC Quest Lab Import",
  "status": "SUCCESS",
  "bundlesProcessed": 5,
  "recordsUploaded": 487
}
```

### Alerting

Error nodes send alerts via:
- Email (SMTP)
- Slack webhook
- PagerDuty (critical)

### Health Checks

Recommended: Create a separate workflow that runs every 5 minutes to verify:
- SFTP connectivity
- HDIM API availability
- Credential validity

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| SFTP timeout | Network/firewall | Check connectivity, increase timeout |
| OAuth 401 | Expired credentials | Refresh client secret |
| FHIR validation error | Bad data | Check transform logic |
| Batch too large | >1000 resources | Reduce BATCH_SIZE |

### Debug Mode

Enable detailed logging:
1. Open workflow settings
2. Set **Save Successful Executions** = Yes
3. Set **Save Failed Executions** = Yes
4. Review execution history

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Dec 2025 | Initial release |

---

*For support: integrations@healthdatainmotion.com*
