# HDIM API - Getting Started Guide

**Status:** ✅ Production Ready
**Last Updated:** January 24, 2026
**API Version:** v1.2.0

## Overview

This guide helps you get started with the HDIM Platform APIs for healthcare quality measure evaluation, FHIR R4 resource management, and care gap detection.

**Target Audience:** Developers, Integration Partners, QA Engineers

**Prerequisites:**
- Basic understanding of REST APIs
- Healthcare interoperability knowledge (FHIR, HEDIS) recommended but not required
- API testing tool (Postman, Insomnia, or cURL)

---

## Table of Contents

1. [Quick Start (5 minutes)](#quick-start-5-minutes)
2. [Authentication](#authentication)
3. [API Basics](#api-basics)
4. [Common Workflows](#common-workflows)
5. [Error Handling](#error-handling)
6. [Rate Limits](#rate-limits)
7. [Webhooks](#webhooks)
8. [Next Steps](#next-steps)

---

## Quick Start (5 minutes)

### Step 1: Obtain API Credentials

**Development Environment:**
```bash
# Test credentials (pre-configured in Docker)
Username: test_evaluator
Password: password123
Tenant ID: TENANT-001
```

**Production Environment:**
Contact your HDIM administrator to obtain:
- Username
- Password
- Tenant ID
- API Base URL

### Step 2: Get JWT Token

```bash
# Request JWT token
curl -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_evaluator",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "refreshToken": "refresh_token_here",
  "user": {
    "username": "test_evaluator",
    "roles": ["ROLE_EVALUATOR"],
    "tenantId": "TENANT-001"
  }
}
```

**Save the token** - you'll need it for all subsequent requests.

### Step 3: Make Your First API Call

```bash
# List quality measures
curl -X GET http://localhost:8087/quality-measure/api/v1/measures \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: TENANT-001" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "measures": [
    {
      "id": "COL-001",
      "name": "Colorectal Cancer Screening",
      "description": "HEDIS COL measure for colorectal cancer screening",
      "version": "2024",
      "status": "ACTIVE"
    }
  ],
  "totalCount": 1,
  "page": 0,
  "pageSize": 20
}
```

✅ **Success!** You've made your first API call.

---

## Authentication

### JWT Token-Based Authentication

HDIM uses **JWT (JSON Web Token)** for stateless authentication. All API requests require a valid JWT token.

#### Token Lifecycle

| Stage | Duration | Action |
|-------|----------|--------|
| **Token Validity** | 60 minutes | Use for API requests |
| **Refresh Window** | 7 days | Refresh token before expiry |
| **Session Timeout** | 15 minutes idle | Re-authenticate after timeout |

#### Obtaining Tokens

**Login Endpoint:**
```
POST /auth/login
```

**Request:**
```json
{
  "username": "your_username",
  "password": "your_password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "refreshToken": "refresh_token_here",
  "user": {
    "username": "your_username",
    "roles": ["ROLE_EVALUATOR", "ROLE_ANALYST"],
    "tenantId": "TENANT-001"
  }
}
```

#### Refreshing Tokens

**Refresh Endpoint:**
```
POST /auth/refresh
```

**Request:**
```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response:**
```json
{
  "token": "new_jwt_token_here",
  "expiresIn": 3600
}
```

#### Using Tokens

Include JWT token in **Authorization header** for all API requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| **SUPER_ADMIN** | Full system access, all operations |
| **ADMIN** | Tenant-level admin, manage users, view all data |
| **EVALUATOR** | Run evaluations, view results, create assignments |
| **ANALYST** | View reports, download QRDA exports |
| **VIEWER** | Read-only access to dashboards |

**Example:** Only users with `EVALUATOR` or `ADMIN` roles can trigger quality measure evaluations.

---

## API Basics

### Base URLs

| Environment | Base URL | Purpose |
|-------------|----------|---------|
| **Development** | `http://localhost:8001` | Local Docker development |
| **Staging** | `https://staging-api.hdim.example.com` | Pre-production testing |
| **Production** | `https://api.hdim.example.com` | Live production |

### Required Headers

Every API request must include:

| Header | Description | Example |
|--------|-------------|---------|
| **Authorization** | Bearer JWT token | `Bearer eyJhbG...` |
| **X-Tenant-ID** | Tenant identifier for multi-tenancy | `TENANT-001` |
| **Content-Type** | Request body format | `application/json` |
| **Accept** | Response format | `application/json` |

**Example Request:**
```bash
curl -X GET http://localhost:8087/quality-measure/api/v1/measures \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: TENANT-001" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json"
```

### Request/Response Format

**All requests and responses use JSON format.**

**Request Example:**
```json
POST /quality-measure/api/v1/evaluations
{
  "patientId": "PAT-12345",
  "measureId": "COL-001",
  "evaluationDate": "2026-01-24T00:00:00Z"
}
```

**Response Example:**
```json
{
  "evaluationId": "EVAL-98765",
  "patientId": "PAT-12345",
  "measureId": "COL-001",
  "result": "MET",
  "numerator": true,
  "denominator": true,
  "exclusion": false,
  "evaluatedAt": "2026-01-24T10:30:45Z"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| **200** | OK | Request successful |
| **201** | Created | Resource created successfully |
| **204** | No Content | Request successful, no response body |
| **400** | Bad Request | Invalid request format or parameters |
| **401** | Unauthorized | Missing or invalid JWT token |
| **403** | Forbidden | Insufficient permissions for operation |
| **404** | Not Found | Resource not found |
| **409** | Conflict | Resource already exists |
| **422** | Unprocessable Entity | Validation error |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Server-side error |
| **503** | Service Unavailable | Service temporarily down |

### Pagination

**List endpoints** support pagination:

```bash
GET /quality-measure/api/v1/measures?page=0&size=20&sort=name,asc
```

**Parameters:**
- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `name,asc`, `createdAt,desc`)

**Response:**
```json
{
  "content": [ /* array of resources */ ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 0,
  "pageSize": 20,
  "first": true,
  "last": false
}
```

### Filtering

**Filter by query parameters:**

```bash
# Filter measures by status
GET /quality-measure/api/v1/measures?status=ACTIVE

# Filter by date range
GET /quality-measure/api/v1/evaluations?startDate=2026-01-01&endDate=2026-01-31

# Combine filters
GET /quality-measure/api/v1/measures?status=ACTIVE&version=2024
```

---

## Common Workflows

### Workflow 1: Search for a Patient

**Use Case:** Find a patient before running evaluations or viewing care gaps.

**Endpoint:**
```
GET /patient/api/v1/patients
```

**Request:**
```bash
curl -X GET "http://localhost:8084/patient/api/v1/patients?lastName=Smith&dateOfBirth=1980-05-15" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: TENANT-001"
```

**Response:**
```json
{
  "patients": [
    {
      "id": "PAT-12345",
      "firstName": "John",
      "lastName": "Smith",
      "dateOfBirth": "1980-05-15",
      "gender": "MALE",
      "mrn": "MRN-987654",
      "active": true
    }
  ],
  "totalCount": 1
}
```

**Next Steps:** Use `patientId` for subsequent operations.

---

### Workflow 2: Evaluate a Quality Measure

**Use Case:** Run a HEDIS quality measure evaluation for a patient.

**Step 1: Get Available Measures**
```bash
GET /quality-measure/api/v1/measures
```

**Step 2: Trigger Evaluation**
```bash
POST /quality-measure/api/v1/evaluations
```

**Request:**
```json
{
  "patientId": "PAT-12345",
  "measureId": "COL-001",
  "evaluationDate": "2026-01-24T00:00:00Z"
}
```

**Response:**
```json
{
  "evaluationId": "EVAL-98765",
  "patientId": "PAT-12345",
  "measureId": "COL-001",
  "result": "MET",
  "numerator": true,
  "denominator": true,
  "exclusion": false,
  "evaluatedAt": "2026-01-24T10:30:45Z",
  "details": {
    "screeningDate": "2025-06-15",
    "screeningType": "Colonoscopy",
    "complianceGap": null
  }
}
```

**Step 3: Retrieve Evaluation Results**
```bash
GET /quality-measure/api/v1/evaluations/EVAL-98765
```

---

### Workflow 3: Detect Care Gaps

**Use Case:** Identify care gaps for a patient to improve quality measure compliance.

**Endpoint:**
```
GET /care-gap/api/v1/patients/{patientId}/care-gaps
```

**Request:**
```bash
curl -X GET "http://localhost:8086/care-gap/api/v1/patients/PAT-12345/care-gaps?status=OPEN" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: TENANT-001"
```

**Response:**
```json
{
  "careGaps": [
    {
      "id": "GAP-54321",
      "patientId": "PAT-12345",
      "measureId": "DIA-HBA1C",
      "gapType": "MISSING_LAB",
      "status": "OPEN",
      "priority": "HIGH",
      "dueDate": "2026-02-15",
      "recommendation": "Schedule HbA1c lab test within 30 days",
      "identifiedAt": "2026-01-24T10:00:00Z"
    }
  ],
  "totalCount": 1
}
```

**Close Care Gap:**
```bash
POST /care-gap/api/v1/care-gaps/GAP-54321/close
```

**Request:**
```json
{
  "closureReason": "LAB_COMPLETED",
  "closureDate": "2026-01-25T00:00:00Z",
  "notes": "HbA1c completed on 2026-01-25, value: 6.2%"
}
```

---

### Workflow 4: Create Prior Authorization

**Use Case:** Submit a prior authorization request for a medical procedure.

**Endpoint:**
```
POST /prior-auth/api/v1/requests
```

**Request:**
```json
{
  "patientId": "PAT-12345",
  "procedureCode": "CPT-81001",
  "procedureName": "Colonoscopy with biopsy",
  "diagnosisCode": "ICD-10-K92.1",
  "providerNPI": "1234567890",
  "requestedDate": "2026-02-15",
  "urgency": "ROUTINE",
  "clinicalJustification": "Patient is due for colorectal cancer screening (HEDIS COL measure)."
}
```

**Response:**
```json
{
  "authorizationId": "AUTH-11111",
  "status": "PENDING",
  "submittedAt": "2026-01-24T10:30:00Z",
  "expectedDecisionDate": "2026-01-27T00:00:00Z",
  "referenceNumber": "REF-998877"
}
```

**Check Status:**
```bash
GET /prior-auth/api/v1/requests/AUTH-11111
```

---

## Error Handling

### Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-01-24T10:30:45Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid patient ID format",
  "path": "/patient/api/v1/patients/INVALID-ID",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Key Fields:**
- `timestamp` - When error occurred
- `status` - HTTP status code
- `error` - HTTP status text
- `message` - Human-readable error description
- `path` - API endpoint that failed
- `traceId` - Unique identifier for debugging (include in support tickets)

### Common Errors

#### 401 Unauthorized - Missing or Invalid Token

**Error:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid"
}
```

**Fix:**
1. Verify token is included in `Authorization` header
2. Check token hasn't expired (60-minute validity)
3. Refresh token if expired
4. Re-authenticate if refresh token expired

#### 403 Forbidden - Insufficient Permissions

**Error:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "User does not have EVALUATOR role required for this operation"
}
```

**Fix:**
1. Verify your user account has required role
2. Contact HDIM administrator to request role assignment

#### 404 Not Found - Resource Not Found

**Error:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Patient with ID PAT-99999 not found in tenant TENANT-001"
}
```

**Fix:**
1. Verify resource ID is correct
2. Ensure resource exists in your tenant
3. Check for typos in ID

#### 409 Conflict - Resource Already Exists

**Error:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Measure assignment already exists for patient PAT-12345 and measure COL-001"
}
```

**Fix:**
1. Check if resource already exists
2. Use PUT/PATCH to update instead of POST
3. Delete existing resource before re-creating

#### 422 Unprocessable Entity - Validation Error

**Error:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "errors": [
    {
      "field": "dateOfBirth",
      "message": "Date of birth cannot be in the future"
    },
    {
      "field": "measureId",
      "message": "Measure ID is required"
    }
  ]
}
```

**Fix:**
1. Review `errors` array for specific validation failures
2. Correct invalid fields
3. Retry request with valid data

#### 429 Too Many Requests - Rate Limit Exceeded

**Error:**
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Retry after 60 seconds.",
  "retryAfter": 60
}
```

**Fix:**
1. Wait for `retryAfter` seconds before retrying
2. Implement exponential backoff in your client
3. Contact support if you need higher rate limits

#### 500 Internal Server Error - Server-Side Error

**Error:**
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Fix:**
1. Include `traceId` in support ticket
2. Check API status page for known issues
3. Retry request after a few minutes
4. Contact support if error persists

---

## Rate Limits

### Rate Limit Tiers

| Tier | Requests per Minute | Burst Limit |
|------|---------------------|-------------|
| **Development** | 60 | 100 |
| **Production** | 600 | 1000 |
| **Enterprise** | Custom | Custom |

### Rate Limit Headers

API responses include rate limit information:

```
X-RateLimit-Limit: 600
X-RateLimit-Remaining: 542
X-RateLimit-Reset: 1706097000
```

**Headers:**
- `X-RateLimit-Limit` - Total requests allowed per minute
- `X-RateLimit-Remaining` - Requests remaining in current window
- `X-RateLimit-Reset` - Unix timestamp when limit resets

### Best Practices

1. **Monitor rate limit headers** - Track remaining quota
2. **Implement retry logic** - Use exponential backoff on 429 errors
3. **Batch requests** - Use bulk endpoints where available
4. **Cache responses** - Reduce redundant API calls
5. **Request limit increase** - Contact support for enterprise limits

**Example Retry Logic (JavaScript):**
```javascript
async function apiRequestWithRetry(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    const response = await fetch(url, options);

    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get('Retry-After') || '60');
      await sleep(retryAfter * 1000);
      continue;
    }

    return response;
  }
  throw new Error('Max retries exceeded');
}
```

---

## Webhooks

### Overview

Webhooks allow you to receive real-time notifications when events occur in the HDIM platform.

**Supported Events:**
- `evaluation.completed` - Quality measure evaluation finished
- `caregap.identified` - New care gap detected
- `caregap.closed` - Care gap closed
- `prior_auth.approved` - Prior authorization approved
- `prior_auth.denied` - Prior authorization denied

### Registering Webhooks

**Endpoint:**
```
POST /webhooks/api/v1/subscriptions
```

**Request:**
```json
{
  "url": "https://your-app.example.com/webhooks/hdim",
  "events": ["evaluation.completed", "caregap.identified"],
  "secret": "your_webhook_secret",
  "active": true
}
```

**Response:**
```json
{
  "subscriptionId": "SUB-12345",
  "url": "https://your-app.example.com/webhooks/hdim",
  "events": ["evaluation.completed", "caregap.identified"],
  "active": true,
  "createdAt": "2026-01-24T10:30:00Z"
}
```

### Webhook Payload

**Webhook Request:**
```json
POST https://your-app.example.com/webhooks/hdim
{
  "eventId": "EVT-98765",
  "eventType": "evaluation.completed",
  "timestamp": "2026-01-24T10:30:45Z",
  "tenantId": "TENANT-001",
  "data": {
    "evaluationId": "EVAL-98765",
    "patientId": "PAT-12345",
    "measureId": "COL-001",
    "result": "MET"
  }
}
```

**Headers:**
```
X-HDIM-Signature: sha256=a1b2c3d4e5f6...
X-HDIM-Event-Type: evaluation.completed
X-HDIM-Event-ID: EVT-98765
```

### Signature Validation

**Verify webhook authenticity:**

```javascript
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');

  return `sha256=${expectedSignature}` === signature;
}

// Usage
app.post('/webhooks/hdim', (req, res) => {
  const signature = req.headers['x-hdim-signature'];
  const isValid = verifyWebhookSignature(req.body, signature, 'your_webhook_secret');

  if (!isValid) {
    return res.status(401).send('Invalid signature');
  }

  // Process webhook
  console.log('Event:', req.body.eventType);
  res.status(200).send('OK');
});
```

### Webhook Retry Logic

**Retry Behavior:**
- HDIM retries failed webhooks up to 5 times
- Exponential backoff: 1m, 5m, 15m, 1h, 6h
- Webhooks are marked as failed after 5 failed attempts

**Response Requirements:**
- Your endpoint must respond with **2xx status code** within **30 seconds**
- Non-2xx responses trigger retries
- Timeouts are treated as failures

### Managing Webhooks

**List Subscriptions:**
```bash
GET /webhooks/api/v1/subscriptions
```

**Update Subscription:**
```bash
PUT /webhooks/api/v1/subscriptions/SUB-12345
```

**Delete Subscription:**
```bash
DELETE /webhooks/api/v1/subscriptions/SUB-12345
```

**View Delivery Logs:**
```bash
GET /webhooks/api/v1/subscriptions/SUB-12345/deliveries
```

---

## Next Steps

### 1. Explore API Documentation

- **[OpenAPI Specifications](./README.md)** - Complete API reference
- **[Postman Collections](../developer-portal/postman/)** - Import pre-configured requests
- **[Service Catalog](../services/SERVICE_CATALOG.md)** - Discover all 50+ services

### 2. Interactive API Explorer

**Swagger UI:**
```bash
# Start services
docker compose up -d

# Access Swagger UI for each service
http://localhost:8087/quality-measure/swagger-ui.html   # Quality Measure
http://localhost:8084/patient/swagger-ui.html           # Patient Service
http://localhost:8085/fhir/swagger-ui.html              # FHIR Service
```

### 3. Generate Client Libraries

```bash
# Install OpenAPI Generator
npm install -g @openapitools/openapi-generator-cli

# Generate TypeScript client
openapi-generator-cli generate \
  -i docs/api/openapi-quality-measure-v1.2.0.json \
  -g typescript-axios \
  -o client/typescript/quality-measure

# Generate Python client
openapi-generator-cli generate \
  -i docs/api/openapi-patient-v1.2.0.json \
  -g python \
  -o client/python/patient
```

### 4. Join Developer Community

- **Support:** support@hdim-platform.com
- **Developer Portal:** https://developers.hdim.example.com
- **Slack Channel:** #hdim-api-developers

### 5. Production Checklist

Before going to production:

- [ ] Obtain production credentials
- [ ] Configure production base URL
- [ ] Implement JWT token refresh logic
- [ ] Add error handling for all API calls
- [ ] Implement rate limit monitoring
- [ ] Set up webhook signature validation
- [ ] Configure logging for audit trail
- [ ] Test multi-tenant isolation
- [ ] Review HIPAA compliance requirements
- [ ] Load test your integration

---

## Troubleshooting

### "Connection refused" Error

**Problem:** Cannot connect to API

**Solutions:**
1. Verify services are running: `docker compose ps`
2. Check correct port numbers (see [Service Catalog](../services/SERVICE_CATALOG.md))
3. Verify Docker network configuration

### "Token expired" Error

**Problem:** JWT token has expired

**Solutions:**
1. Use refresh token to obtain new JWT
2. Implement automatic token refresh in your client
3. Re-authenticate if refresh token expired

### "Tenant not found" Error

**Problem:** Invalid or missing `X-Tenant-ID` header

**Solutions:**
1. Verify `X-Tenant-ID` header is included in request
2. Check tenant ID matches your account
3. Contact administrator to confirm tenant ID

### "Service unavailable" (503) Error

**Problem:** Service is temporarily down

**Solutions:**
1. Check service health: `docker compose ps SERVICE_NAME`
2. View service logs: `docker compose logs -f SERVICE_NAME`
3. Restart service: `docker compose restart SERVICE_NAME`
4. Check [monitoring dashboard](http://localhost:3000) (Grafana)

---

## Related Documentation

- **[OpenAPI Specifications](./README.md)** - Complete API reference
- **[CLAUDE.md](../../CLAUDE.md)** - HDIM development quick reference
- **[HIPAA Compliance](../../backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
- **[Gateway Trust Architecture](../../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Authentication flow
- **[Distributed Tracing](../../backend/docs/DISTRIBUTED_TRACING_GUIDE.md)** - Observability

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** ✅ Production Ready
