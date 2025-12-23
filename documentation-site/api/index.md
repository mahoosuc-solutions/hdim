# API Reference

The HDIM Clinical Portal provides a comprehensive RESTful API for quality measurement, patient management, and care gap tracking.

## Base URL

All API requests should be made to:

```
https://api.yourdomain.com
```

For local development:
```
http://localhost:8080
```

---

## Authentication

All API endpoints require JWT authentication. Include the token in the `Authorization` header:

```http
Authorization: Bearer <your-jwt-token>
```

### Obtaining a Token

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "your-password"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

---

## Multi-Tenancy

All API requests require the `X-Tenant-ID` header to identify the tenant/organization:

```http
X-Tenant-ID: demo-tenant
```

---

## API Endpoints

### Core Services

| Service | Base Path | Description |
|---------|-----------|-------------|
| [Patient Service](./patient-service) | `/patient` | Patient data and health records |
| [Quality Measure Service](./quality-measure-service) | `/quality-measure` | Quality measure calculations and reports |
| [CQL Engine Service](./cql-engine-service) | `/api/v1/cql` | CQL evaluation engine |
| [Care Gap Service](./care-gap-service) | `/care-gap` | Care gap identification and management |

### Supporting Services

| Service | Base Path | Description |
|---------|-----------|-------------|
| Gateway Service | `/api/gateway` | API gateway and routing |
| FHIR Server | `/fhir` | FHIR R4 resource server |

---

## Common Patterns

### Pagination

List endpoints support pagination via query parameters:

```http
GET /quality-measure/results?page=0&size=20
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (0-indexed) |
| `size` | 20 | Items per page |

**Response includes pagination metadata:**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

### Error Responses

All errors follow a consistent format:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/quality-measure/calculate"
}
```

**Common HTTP Status Codes:**

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful delete) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 500 | Internal Server Error |

### Dates and Times

- Dates use ISO 8601 format: `YYYY-MM-DD`
- Timestamps use ISO 8601 with timezone: `2025-01-15T10:30:00Z`

---

## Role-Based Access

API endpoints enforce role-based access control:

| Role | Permissions |
|------|-------------|
| `ANALYST` | Read operations (GET) |
| `EVALUATOR` | Read + Execute (GET, POST) |
| `ADMIN` | Read + Execute + Delete |
| `SUPER_ADMIN` | Full access |

---

## Rate Limiting

API requests are rate-limited per tenant:

| Tier | Requests/min | Burst |
|------|--------------|-------|
| Standard | 100 | 150 |
| Premium | 500 | 750 |

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705318200
```

---

## FHIR Support

The API supports FHIR R4 resources:

- Patient resources are available at `/fhir/Patient`
- Clinical data follows FHIR specifications
- Responses can use `application/fhir+json` content type

---

## Quick Start Examples

### 1. Get Patient Health Record

```bash
curl -X GET "https://api.example.com/patient/health-record?patient=patient-123" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: demo-tenant" \
  -H "Accept: application/fhir+json"
```

### 2. Run Quality Evaluation

```bash
curl -X POST "https://api.example.com/quality-measure/calculate?patient=uuid&measure=CMS130" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: demo-tenant" \
  -H "Content-Type: application/json"
```

### 3. Get Care Gaps

```bash
curl -X GET "https://api.example.com/care-gap/open?patient=uuid" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: demo-tenant"
```

---

## SDKs and Tools

### Postman Collection

Download our Postman collection for easy API testing:
- [HDIM API Collection](/downloads/hdim-api-collection.json)

### OpenAPI Specification

OpenAPI 3.0 specification available at:
```
https://api.example.com/v3/api-docs
```

---

## Support

For API support:
- Check the [Troubleshooting Guide](./troubleshooting)
- Contact: api-support@example.com
