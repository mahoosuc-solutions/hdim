# [Service Name] Service

**Port**: [PORT]
**Package**: `com.healthdata.[servicename]`
**Last Updated**: [DATE]

---

## Overview

[2-3 sentence description of what this service does and why it exists. Focus on the business value, not just technical function.]

---

## Responsibilities

- [Specific responsibility 1 - what this service owns]
- [Specific responsibility 2 - what decisions it makes]
- [Specific responsibility 3 - what data it manages]
- [Anti-responsibility - what this service does NOT do]

---

## Technology Stack

| Component | Technology | Version | Rationale |
|-----------|------------|---------|-----------|
| Runtime | Java | 21 LTS | Required by Spring Boot 3.x, modern features |
| Framework | Spring Boot | 3.x | Enterprise Java, security integrations |
| Database | PostgreSQL | 15 | ACID compliance, multi-tenant support |
| Cache | Redis | 7 | Fast caching, 5-min TTL for HIPAA |
| Messaging | Apache Kafka | 3.x | Event-driven architecture |
| FHIR | HAPI FHIR | 7.x | FHIR R4 certified |

---

## API Endpoints

### GET /api/v1/[resource]

**Purpose**: [What this endpoint does]

**Authorization**: `@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")`

**Request Headers**:
| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | Bearer token (JWT) |
| `X-Tenant-ID` | Yes | Tenant identifier for multi-tenant isolation |

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `resourceId` | String (UUID) | Resource identifier |

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |

**Response**: `200 OK`
```json
{
  "id": "uuid",
  "tenantId": "string",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

**Error Responses**:
| Status | Reason |
|--------|--------|
| 401 | Missing or invalid JWT |
| 403 | Insufficient permissions |
| 404 | Resource not found |

---

### POST /api/v1/[resource]

**Purpose**: [What this endpoint does]

**Authorization**: `@PreAuthorize("hasRole('ADMIN')")`

**Request Body**:
```json
{
  "field1": "string (required)",
  "field2": "string (optional)"
}
```

**Response**: `201 Created`
```json
{
  "id": "uuid",
  "tenantId": "string"
}
```

---

## Database Schema

### Table: `[table_name]`

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | UUID | No | Primary key |
| `tenant_id` | VARCHAR(50) | No | Tenant identifier (multi-tenant isolation) |
| `[column]` | [TYPE] | [Yes/No] | [Description] |
| `created_at` | TIMESTAMP | No | Record creation time |
| `updated_at` | TIMESTAMP | Yes | Last modification time |

**Indexes**:
- `idx_[table]_tenant_id` on `tenant_id` (required for multi-tenant queries)
- `idx_[table]_[column]` on `[column]` (for frequent lookups)

**Constraints**:
- All queries MUST filter by `tenant_id` (HIPAA multi-tenant isolation)

---

## Kafka Topics

### Publishes

| Topic | Event Type | Trigger | Payload Schema |
|-------|------------|---------|----------------|
| `[domain].events` | `[EVENT_TYPE]` | [When published] | See below |

**Payload Example**:
```json
{
  "eventType": "RESOURCE_CREATED",
  "tenantId": "string",
  "resourceId": "uuid",
  "timestamp": "ISO-8601",
  "payload": {}
}
```

### Consumes

| Topic | Event Type | Handler Class | Action |
|-------|------------|---------------|--------|
| `[domain].events` | `[EVENT_TYPE]` | `[HandlerClass]` | [What happens] |

---

## Configuration

### application.yml

```yaml
server:
  port: [PORT]

spring:
  application:
    name: [service-name]
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/${POSTGRES_DB:healthdata_qm}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6380}

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}

# HIPAA Compliance - PHI cache TTL
cache:
  phi:
    ttl-seconds: 300  # 5 minutes maximum - DO NOT INCREASE

# Gateway Trust Authentication
gateway:
  auth:
    dev-mode: ${GATEWAY_AUTH_DEV_MODE:true}
    signing-secret: ${GATEWAY_AUTH_SIGNING_SECRET:}
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `POSTGRES_HOST` | No | localhost | Database host |
| `POSTGRES_PORT` | No | 5435 | Database port |
| `POSTGRES_PASSWORD` | Yes | - | Database password |
| `REDIS_HOST` | No | localhost | Redis host |
| `REDIS_PORT` | No | 6380 | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | No | localhost:9094 | Kafka brokers |

---

## Testing

### Run Tests

```bash
# Unit tests
./gradlew :modules:services:[service-name]:test

# Integration tests (requires Docker)
./gradlew :modules:services:[service-name]:integrationTest

# All tests with coverage
./gradlew :modules:services:[service-name]:test :modules:services:[service-name]:jacocoTestReport
```

### Test Categories

| Category | Location | Purpose |
|----------|----------|---------|
| Unit | `src/test/java/.../unit/` | Isolated component testing |
| Integration | `src/test/java/.../integration/` | API and database testing |
| Security | `src/test/java/.../security/` | Authentication/authorization |

### Test Data

- Use `@Sql` annotations for database setup
- Use `TestContainers` for integration tests
- Never use production data in tests

---

## Monitoring

### Health Checks

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Overall service health |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |

### Metrics

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/metrics` | All metrics |
| `GET /actuator/prometheus` | Prometheus format |

### Key Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `[service]_requests_total` | Total requests | N/A |
| `[service]_request_duration_seconds` | Request latency | p95 > 500ms |
| `[service]_errors_total` | Error count | > 10/min |

---

## Security

### Authentication

This service uses **Gateway Trust Authentication**:

1. Kong validates JWT at gateway
2. Gateway injects `X-Auth-*` headers
3. Service trusts signed headers (no DB lookup)

**Required Headers** (injected by gateway):
- `X-Auth-User-Id`: User UUID
- `X-Auth-Username`: User login
- `X-Auth-Tenant-Ids`: Authorized tenants
- `X-Auth-Roles`: User roles
- `X-Auth-Validated`: HMAC signature

See: [Gateway Trust Architecture](/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)

### HIPAA Compliance

- [ ] PHI cache TTL <= 5 minutes
- [ ] Multi-tenant filtering on all queries
- [ ] `@Audited` annotation on PHI access methods
- [ ] No PHI in log messages
- [ ] Cache-Control headers on PHI endpoints

---

## Dependencies

### Internal Services

| Service | Purpose | Communication |
|---------|---------|---------------|
| [Service Name] | [Why needed] | REST / Kafka |

### External Dependencies

| Dependency | Purpose | Version |
|------------|---------|---------|
| PostgreSQL | Persistence | 15 |
| Redis | Caching | 7 |
| Kafka | Messaging | 3.x |

---

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| 403 Forbidden | Missing X-Tenant-ID header | Ensure header is passed from gateway |
| Connection refused | Service not started | Check Docker Compose status |
| Cache miss | PHI TTL expired | Expected behavior (5-min TTL) |

---

## Related Documentation

- [HIPAA Cache Compliance](/backend/HIPAA-CACHE-COMPLIANCE.md)
- [Gateway Trust Architecture](/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- [API Specification](/BACKEND_API_SPECIFICATION.md)
- [Terminology Glossary](/docs/TERMINOLOGY_GLOSSARY.md)
- [Related ADR](#) (if applicable)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | [DATE] | [Author] | Initial creation |

---

*This README follows the template in `/docs/templates/SERVICE_README_TEMPLATE.md`*
