---
id: "product-api-reference"
title: "API Reference & Developer Documentation"
portalType: "product"
path: "product/02-architecture/api-reference.md"
category: "architecture"
subcategory: "api"
tags: ["API", "REST", "developer", "integration", "documentation"]
summary: "Complete API reference and developer documentation for HealthData in Motion. Includes REST endpoints, authentication, data formats, code examples, and integration patterns."
estimatedReadTime: 8
difficulty: "advanced"
targetAudience: ["developer", "integration-specialist", "architect"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["API reference", "REST API", "developer documentation", "API endpoints", "integration guide"]
relatedDocuments: ["integration-patterns", "vendor-integrations", "system-architecture", "data-model"]
lastUpdated: "2025-12-01"
---

# API Reference & Developer Documentation

## Executive Summary

HealthData in Motion provides **comprehensive REST APIs** for integration, data access, and system automation. All APIs use OAuth 2.0 authentication, standard JSON formats, and RESTful design principles.

**API Capabilities**:
- Patient data access and management
- Care gap querying and updates
- Measure calculations
- Care plan CRUD operations
- Reporting and analytics
- Webhook subscriptions

## Authentication

**OAuth 2.0 Flow**:
```
POST /oauth/token
{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret",
  "grant_type": "client_credentials"
}
```

**Response**:
```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

## Patient Endpoints

**GET /patients/{id}** - Get patient details
- Returns full patient demographics
- Clinical summary
- Risk score

**GET /patients?filter=active** - Search patients
- Pagination: limit, offset
- Sorting: by name, risk score, health score
- Filtering: by status, risk level, condition

**POST /patients** - Create patient
- Merge with existing if found
- MPI matching
- FHIR-compliant data

## Care Gap Endpoints

**GET /care-gaps** - List care gaps
- Filtering: status, priority, gap type, measure
- Sorting: by date, priority, days open
- Pagination: limit 1000

**POST /care-gaps/{id}/actions** - Document gap closure
- Action type (treat, refer, educate, etc.)
- Outcome and effectiveness
- Audit trail created

**DELETE /care-gaps/{id}** - Close/archive gap

## Measure Endpoints

**GET /measures** - List available measures
**POST /measures/{id}/calculate** - Calculate measure for patient(s)
**GET /measures/{id}/results** - Get measure results

## Reporting Endpoints

**POST /reports/generate** - Generate on-demand report
- Report type, filters, format
- Returns report ID for polling

**GET /reports/{id}** - Get report status/results
**GET /reports/scheduled** - List scheduled reports

## Webhooks

**Webhook Events**:
- `patient.created`, `patient.updated`
- `gap.identified`, `gap.closed`
- `measure.calculated`
- `alert.generated`

**Subscription**:
```
POST /webhooks/subscribe
{
  "url": "https://yourapi.com/webhook",
  "events": ["gap.identified"],
  "secret": "signing_secret"
}
```

## Error Handling

**HTTP Status Codes**:
- 200: Success
- 400: Bad request
- 401: Unauthorized
- 403: Forbidden
- 404: Not found
- 500: Server error

**Error Response**:
```json
{
  "error": "error_code",
  "message": "Human-readable message",
  "details": {}
}
```

## Rate Limiting

**Default Limits**:
- 1,000 requests/minute per API key
- 100 MB/month data transfer
- Burst: 100 requests/10 seconds

**Headers**:
- `X-RateLimit-Limit`: 1000
- `X-RateLimit-Remaining`: 999
- `X-RateLimit-Reset`: 1234567890

## Code Examples

**Python SDK**:
```python
from hdim import Client

client = Client(api_key='your_key')
patient = client.get_patient('patient-123')
gaps = client.list_care_gaps(patient_id='patient-123')
for gap in gaps:
    print(f"{gap.type}: {gap.description}")
```

**JavaScript**:
```javascript
const hdim = require('hdim');
const client = new hdim.Client({apiKey: 'your_key'});

const patient = await client.patients.get('patient-123');
const gaps = await client.careGaps.list({patientId: 'patient-123'});
```

## SDK Libraries

**Available SDKs**:
- Python (pip install hdim)
- JavaScript/TypeScript (npm install @hdim/sdk)
- Java (Maven)
- C# (.NET)
- Ruby, Go, PHP (community)

## API Best Practices

**Performance**:
- Batch requests when possible
- Use pagination for large result sets
- Cache responses (4-hour TTL recommended)
- Implement exponential backoff for retries

**Security**:
- Never expose API keys
- Use OAuth 2.0 (not API keys) for server-server
- Rotate credentials regularly
- Monitor API usage for anomalies

## Changelog & Versioning

**API Versioning**: v1.0 (current), v2.0 (beta)

**Deprecation Policy**:
- 6-month notice before deprecating endpoints
- 12-month support window
- Migration guides provided

## Support & Resources

**Documentation**: https://api.hdim.com/docs
**Issues/Bugs**: support@hdim.com
**Community**: developer-forum.hdim.com
**Status**: status.hdim.com

---

**Next Steps**: See [Integration Patterns](integration-patterns.md), [Vendor Integrations](vendor-integrations.md)
