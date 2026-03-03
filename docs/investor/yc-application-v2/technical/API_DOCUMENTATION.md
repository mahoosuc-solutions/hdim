# HDIM API Documentation

> Developer guide for integrating with the HDIM platform.

---

## Overview

The HDIM API enables programmatic access to quality measurement data, care gap information, and reporting functionality. Built on RESTful principles with JSON payloads, the API supports both real-time queries and batch operations.

**Base URL:** `https://api.healthdatainmotion.com/v1`

**API Version:** v1 (current)

---

## Quick Start

### 1. Get Your API Key

API keys are available for Enterprise tier and above. Request yours at:
- Dashboard: Settings → API → Generate Key
- Email: api-support@healthdatainmotion.com

### 2. Make Your First Request

```bash
curl -X GET "https://api.healthdatainmotion.com/v1/health" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Response:**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 3. Explore the API

Use our interactive documentation at: `https://api.healthdatainmotion.com/docs`

---

## Authentication

### API Key Authentication

All API requests require authentication via Bearer token.

**Header:**
```
Authorization: Bearer YOUR_API_KEY
```

### OAuth 2.0 (Enterprise+)

For Enterprise Plus and Health System tiers, OAuth 2.0 is available.

**Token Endpoint:** `https://auth.healthdatainmotion.com/oauth/token`

**Request:**
```bash
curl -X POST "https://auth.healthdatainmotion.com/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### Scopes

| Scope | Description |
|-------|-------------|
| `read:patients` | Read patient demographic data |
| `read:measures` | Read quality measure results |
| `read:gaps` | Read care gap data |
| `write:data` | Submit clinical data |
| `admin:org` | Organization administration |

---

## Rate Limiting

| Tier | Requests/Minute | Requests/Day |
|------|-----------------|--------------|
| Enterprise | 100 | 10,000 |
| Enterprise Plus | 500 | 50,000 |
| Health System | 1,000 | Unlimited |

**Rate Limit Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705320000
```

**429 Response:**
```json
{
  "error": "rate_limit_exceeded",
  "message": "Rate limit exceeded. Retry after 60 seconds.",
  "retry_after": 60
}
```

---

## Endpoints

### Health Check

#### GET /health

Check API availability and version.

**Request:**
```bash
curl -X GET "https://api.healthdatainmotion.com/v1/health" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Response:**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "timestamp": "2025-01-15T10:30:00Z",
  "services": {
    "database": "healthy",
    "cache": "healthy",
    "fhir": "healthy"
  }
}
```

---

### Organizations

#### GET /organizations

List organizations accessible to your API key.

**Request:**
```bash
curl -X GET "https://api.healthdatainmotion.com/v1/organizations" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Response:**
```json
{
  "data": [
    {
      "id": "org_abc123",
      "name": "Valley Family Medicine",
      "type": "practice",
      "patient_count": 1200,
      "created_at": "2024-06-01T00:00:00Z"
    }
  ],
  "meta": {
    "total": 1,
    "page": 1,
    "per_page": 20
  }
}
```

#### GET /organizations/{org_id}

Get organization details.

**Response:**
```json
{
  "id": "org_abc123",
  "name": "Valley Family Medicine",
  "type": "practice",
  "npi": "1234567890",
  "address": {
    "street": "123 Main St",
    "city": "Portland",
    "state": "OR",
    "zip": "97201"
  },
  "patient_count": 1200,
  "provider_count": 3,
  "measures_enabled": 20,
  "tier": "professional",
  "created_at": "2024-06-01T00:00:00Z"
}
```

---

### Patients

#### GET /organizations/{org_id}/patients

List patients with optional filtering.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number (default: 1) |
| `per_page` | integer | Results per page (default: 20, max: 100) |
| `search` | string | Search by name or MRN |
| `has_gaps` | boolean | Filter to patients with open care gaps |
| `provider_id` | string | Filter by attributed provider |
| `measure_id` | string | Filter by specific measure gap |

**Request:**
```bash
curl -X GET "https://api.healthdatainmotion.com/v1/organizations/org_abc123/patients?has_gaps=true&per_page=50" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Response:**
```json
{
  "data": [
    {
      "id": "pat_xyz789",
      "mrn": "12345",
      "name": {
        "given": "John",
        "family": "Smith"
      },
      "birth_date": "1965-03-15",
      "gender": "male",
      "attributed_provider": {
        "id": "prov_456",
        "name": "Dr. Sarah Chen"
      },
      "gap_count": 3,
      "last_visit": "2024-12-01",
      "next_visit": "2025-01-20"
    }
  ],
  "meta": {
    "total": 150,
    "page": 1,
    "per_page": 50
  }
}
```

#### GET /organizations/{org_id}/patients/{patient_id}

Get patient details including care gaps.

**Response:**
```json
{
  "id": "pat_xyz789",
  "mrn": "12345",
  "name": {
    "given": "John",
    "family": "Smith"
  },
  "birth_date": "1965-03-15",
  "gender": "male",
  "address": {
    "street": "456 Oak Ave",
    "city": "Portland",
    "state": "OR",
    "zip": "97202"
  },
  "phone": "503-555-1234",
  "email": "john.smith@email.com",
  "attributed_provider": {
    "id": "prov_456",
    "name": "Dr. Sarah Chen",
    "npi": "9876543210"
  },
  "conditions": [
    {
      "code": "E11.9",
      "display": "Type 2 Diabetes Mellitus",
      "onset_date": "2020-01-15"
    }
  ],
  "care_gaps": [
    {
      "id": "gap_001",
      "measure_id": "CDC",
      "measure_name": "Comprehensive Diabetes Care: HbA1c Control",
      "status": "open",
      "due_date": "2025-03-31",
      "last_action": null
    }
  ],
  "quality_summary": {
    "measures_applicable": 8,
    "measures_met": 5,
    "compliance_rate": 0.625
  }
}
```

---

### Quality Measures

#### GET /measures

List all available quality measures.

**Response:**
```json
{
  "data": [
    {
      "id": "CDC",
      "name": "Comprehensive Diabetes Care",
      "description": "Percentage of patients with diabetes who had HbA1c control <8%",
      "category": "chronic",
      "program": ["HEDIS", "MIPS"],
      "denominator_description": "Patients 18-75 with diabetes",
      "numerator_description": "HbA1c <8% during measurement period",
      "exclusions": ["Hospice", "ESRD"],
      "version": "2024"
    }
  ],
  "meta": {
    "total": 61
  }
}
```

#### GET /organizations/{org_id}/measures

Get measure performance for an organization.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `measure_id` | string | Filter by specific measure |
| `provider_id` | string | Filter by provider |
| `site_id` | string | Filter by site |
| `as_of` | date | Performance as of date |

**Response:**
```json
{
  "data": [
    {
      "measure_id": "CDC",
      "measure_name": "Comprehensive Diabetes Care: HbA1c Control",
      "denominator": 245,
      "numerator": 189,
      "exclusions": 12,
      "rate": 0.771,
      "benchmark": {
        "national_50th": 0.72,
        "national_90th": 0.85
      },
      "trend": {
        "previous_rate": 0.68,
        "change": 0.091
      }
    }
  ],
  "as_of": "2025-01-15",
  "meta": {
    "total": 20
  }
}
```

---

### Care Gaps

#### GET /organizations/{org_id}/care-gaps

List all open care gaps.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | `open`, `closed`, `excluded` |
| `measure_id` | string | Filter by measure |
| `provider_id` | string | Filter by provider |
| `due_before` | date | Gaps due before date |
| `priority` | string | `high`, `medium`, `low` |

**Request:**
```bash
curl -X GET "https://api.healthdatainmotion.com/v1/organizations/org_abc123/care-gaps?status=open&priority=high" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Response:**
```json
{
  "data": [
    {
      "id": "gap_001",
      "patient": {
        "id": "pat_xyz789",
        "name": "John Smith",
        "mrn": "12345"
      },
      "measure": {
        "id": "CDC",
        "name": "Comprehensive Diabetes Care: HbA1c Control"
      },
      "status": "open",
      "priority": "high",
      "due_date": "2025-03-31",
      "days_open": 45,
      "attributed_provider": {
        "id": "prov_456",
        "name": "Dr. Sarah Chen"
      },
      "next_visit": "2025-01-20",
      "recommended_action": "Order HbA1c test"
    }
  ],
  "summary": {
    "total_open": 150,
    "high_priority": 25,
    "due_this_week": 12
  },
  "meta": {
    "total": 150,
    "page": 1,
    "per_page": 20
  }
}
```

#### POST /organizations/{org_id}/care-gaps/{gap_id}/close

Close a care gap (when evidence is submitted outside normal data flow).

**Request:**
```json
{
  "closure_date": "2025-01-15",
  "closure_reason": "evidence_submitted",
  "evidence": {
    "type": "observation",
    "code": "4548-4",
    "value": 6.8,
    "unit": "%",
    "date": "2025-01-15"
  },
  "notes": "HbA1c result from Quest Labs"
}
```

**Response:**
```json
{
  "id": "gap_001",
  "status": "closed",
  "closure_date": "2025-01-15",
  "closure_reason": "evidence_submitted",
  "closed_by": "api"
}
```

---

### Reports

#### POST /organizations/{org_id}/reports

Generate a quality report.

**Request:**
```json
{
  "report_type": "quality_summary",
  "format": "pdf",
  "parameters": {
    "period_start": "2024-01-01",
    "period_end": "2024-12-31",
    "measures": ["CDC", "CBP", "BCS"],
    "group_by": "provider"
  },
  "delivery": {
    "method": "webhook",
    "url": "https://your-server.com/reports/callback"
  }
}
```

**Response:**
```json
{
  "report_id": "rpt_abc123",
  "status": "processing",
  "estimated_completion": "2025-01-15T10:35:00Z"
}
```

#### GET /organizations/{org_id}/reports/{report_id}

Get report status and download URL.

**Response:**
```json
{
  "report_id": "rpt_abc123",
  "status": "completed",
  "download_url": "https://reports.healthdatainmotion.com/download/rpt_abc123?token=...",
  "expires_at": "2025-01-16T10:30:00Z",
  "format": "pdf",
  "size_bytes": 245678
}
```

---

### Data Ingestion

#### POST /organizations/{org_id}/data/fhir

Submit FHIR Bundle for processing.

**Request:**
```bash
curl -X POST "https://api.healthdatainmotion.com/v1/organizations/org_abc123/data/fhir" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/fhir+json" \
  -d @bundle.json
```

**Example Bundle:**
```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "patient-123",
        "name": [{"given": ["John"], "family": "Smith"}],
        "birthDate": "1965-03-15",
        "gender": "male"
      },
      "request": {
        "method": "PUT",
        "url": "Patient/patient-123"
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "id": "obs-456",
        "status": "final",
        "code": {
          "coding": [{"system": "http://loinc.org", "code": "4548-4"}]
        },
        "subject": {"reference": "Patient/patient-123"},
        "valueQuantity": {"value": 7.2, "unit": "%"},
        "effectiveDateTime": "2025-01-10"
      },
      "request": {
        "method": "PUT",
        "url": "Observation/obs-456"
      }
    }
  ]
}
```

**Response:**
```json
{
  "transaction_id": "txn_xyz789",
  "status": "accepted",
  "resources_processed": 2,
  "quality_updates": {
    "measures_evaluated": 3,
    "gaps_closed": 1
  }
}
```

---

### Webhooks

#### POST /organizations/{org_id}/webhooks

Register a webhook for real-time notifications.

**Request:**
```json
{
  "url": "https://your-server.com/hdim/webhook",
  "events": ["gap.opened", "gap.closed", "measure.updated"],
  "secret": "your-webhook-secret"
}
```

**Response:**
```json
{
  "webhook_id": "wh_abc123",
  "url": "https://your-server.com/hdim/webhook",
  "events": ["gap.opened", "gap.closed", "measure.updated"],
  "status": "active",
  "created_at": "2025-01-15T10:30:00Z"
}
```

**Webhook Payload Example:**
```json
{
  "event": "gap.closed",
  "timestamp": "2025-01-15T10:35:00Z",
  "data": {
    "gap_id": "gap_001",
    "patient_id": "pat_xyz789",
    "measure_id": "CDC",
    "closure_reason": "numerator_met"
  },
  "signature": "sha256=..."
}
```

---

## Error Handling

### Error Response Format

```json
{
  "error": {
    "code": "invalid_request",
    "message": "The request body is missing required field 'patient_id'",
    "details": {
      "field": "patient_id",
      "reason": "required"
    },
    "request_id": "req_xyz789"
  }
}
```

### Error Codes

| HTTP Code | Error Code | Description |
|-----------|------------|-------------|
| 400 | `invalid_request` | Malformed request |
| 401 | `unauthorized` | Invalid or missing API key |
| 403 | `forbidden` | Insufficient permissions |
| 404 | `not_found` | Resource not found |
| 409 | `conflict` | Resource conflict |
| 422 | `validation_error` | Validation failed |
| 429 | `rate_limit_exceeded` | Rate limit exceeded |
| 500 | `internal_error` | Server error |
| 503 | `service_unavailable` | Temporary unavailability |

---

## SDKs & Libraries

### Official SDKs

| Language | Package | Repository |
|----------|---------|------------|
| Python | `hdim-python` | github.com/hdim/hdim-python |
| Node.js | `@hdim/node` | github.com/hdim/hdim-node |
| Java | `hdim-java` | github.com/hdim/hdim-java |

### Python Example

```python
from hdim import Client

client = Client(api_key="YOUR_API_KEY")

# Get organization measures
measures = client.measures.list(org_id="org_abc123")
for measure in measures:
    print(f"{measure.name}: {measure.rate:.1%}")

# Get open care gaps
gaps = client.care_gaps.list(
    org_id="org_abc123",
    status="open",
    priority="high"
)
for gap in gaps:
    print(f"{gap.patient.name}: {gap.measure.name}")
```

### Node.js Example

```javascript
const { Client } = require('@hdim/node');

const client = new Client({ apiKey: 'YOUR_API_KEY' });

// Get organization measures
const measures = await client.measures.list({ orgId: 'org_abc123' });
measures.data.forEach(m => {
  console.log(`${m.name}: ${(m.rate * 100).toFixed(1)}%`);
});

// Get open care gaps
const gaps = await client.careGaps.list({
  orgId: 'org_abc123',
  status: 'open',
  priority: 'high'
});
```

---

## Best Practices

### 1. Pagination

Always handle pagination for list endpoints:

```python
all_gaps = []
page = 1
while True:
    response = client.care_gaps.list(org_id="org_abc123", page=page)
    all_gaps.extend(response.data)
    if page >= response.meta.total_pages:
        break
    page += 1
```

### 2. Rate Limiting

Implement exponential backoff:

```python
import time

def make_request_with_retry(func, max_retries=3):
    for attempt in range(max_retries):
        try:
            return func()
        except RateLimitError as e:
            if attempt == max_retries - 1:
                raise
            time.sleep(2 ** attempt)
```

### 3. Webhook Verification

Always verify webhook signatures:

```python
import hmac
import hashlib

def verify_webhook(payload, signature, secret):
    expected = hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(f"sha256={expected}", signature)
```

### 4. Error Handling

Handle errors gracefully:

```python
try:
    patient = client.patients.get(org_id="org_abc123", patient_id="pat_xyz")
except NotFoundError:
    print("Patient not found")
except AuthorizationError:
    print("Access denied")
except HDIMError as e:
    print(f"API error: {e.message}")
```

---

## Changelog

### v1.0.0 (December 2025)
- Initial API release
- Core endpoints: organizations, patients, measures, care gaps
- FHIR data ingestion
- Webhook support
- Report generation

---

## Support

**Documentation:** https://api.healthdatainmotion.com/docs
**Status Page:** https://status.healthdatainmotion.com
**Support Email:** api-support@healthdatainmotion.com
**Developer Slack:** [Request access]

---

*Last Updated: December 2025*
