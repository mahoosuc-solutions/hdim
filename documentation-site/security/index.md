# Security Architecture

## Overview

HDIM implements defense-in-depth security with multiple layers of protection designed for healthcare compliance (HIPAA, SOC2) and enterprise requirements.

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│                         INTERNET                                     │
└─────────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │    WAF / CDN Layer    │  DDoS Protection
                    │    (CloudFlare/AWS)   │  Rate Limiting
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │    API Gateway        │  JWT Validation
                    │    (Spring Cloud)     │  Auth Header Injection
                    │                       │  Tenant Isolation
                    └───────────┬───────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼───────┐   ┌───────────▼───────────┐   ┌───────▼───────┐
│ FHIR Service  │   │ Quality Measure Svc   │   │  CQL Engine   │
│ OAuth 2.0     │   │ RBAC Enforcement      │   │  PHI Encrypt  │
└───────────────┘   └───────────────────────┘   └───────────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │   PostgreSQL + Redis  │  Encryption at Rest
                    │   Tenant Isolation    │  TLS in Transit
                    └───────────────────────┘
```

---

## Authentication

### JWT Token-Based Authentication

| Property | Value |
|----------|-------|
| Algorithm | HS512 (HMAC-SHA512) |
| Access Token TTL | 15 minutes |
| Refresh Token TTL | 7 days |
| Key Strength | 256-bit minimum |

**Token Claims:**
- `sub`: User ID
- `username`: User identifier
- `tenantIds`: Authorized tenant list
- `roles`: User roles
- `iat`: Issued at
- `exp`: Expiration

### OAuth 2.0 / SMART on FHIR

For EHR integrations, HDIM supports:

- Authorization Code Flow with PKCE
- Client Credentials Grant (backend services)
- Refresh Token Flow
- Token Revocation
- Scope-based access control

**Supported Scopes:**
```
patient/*.read     - Read patient data
patient/*.write    - Write patient data
user/*.read        - Read user-context data
offline_access     - Refresh token support
launch/patient     - Patient launch context
launch/encounter   - Encounter launch context
```

### Centralized Gateway Authentication

All requests authenticate at the API Gateway, which:

1. Validates JWT signature and expiration
2. Extracts user identity and permissions
3. Injects secure headers for downstream services
4. Strips external auth headers (prevents injection)

**Injected Headers:**
| Header | Purpose |
|--------|---------|
| `X-Auth-User-Id` | Authenticated user ID |
| `X-Auth-Username` | Username |
| `X-Auth-Tenant-Ids` | Authorized tenants |
| `X-Auth-Roles` | User roles |
| `X-Auth-Validated` | HMAC signature |

---

## Authorization

### Role-Based Access Control (RBAC)

| Role | Description | Permissions |
|------|-------------|-------------|
| `VIEWER` | Read-only access | View patients, measures, reports |
| `EVALUATOR` | Clinical user | Run evaluations, close care gaps |
| `ANALYST` | Quality analyst | Population reports, analytics |
| `ADMIN` | Administrator | User management, configuration |
| `SUPER_ADMIN` | System admin | Cross-tenant, all permissions |

**Method-Level Enforcement:**
```java
@PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
public ResponseEntity<EvaluationResult> evaluate(...) { }
```

### Multi-Tenant Isolation

Every request is validated for tenant authorization:

1. Extract `X-Tenant-ID` header from request
2. Verify user has access to requested tenant
3. Reject with 403 Forbidden if unauthorized
4. All database queries filter by tenant

**Tenant Access Filter:**
- Prevents cross-tenant data access
- Validates against user's authorized tenant list
- Audit logs unauthorized attempts

---

## Encryption

### Data at Rest

| Layer | Encryption |
|-------|------------|
| Database | AES-256 (PostgreSQL TDE) |
| PHI Fields | AES-256-GCM application-level |
| Redis Cache | Encrypted connection + TTL |
| Backups | AES-256 encrypted |

**PHI Encryption Details:**
- Algorithm: AES-256-GCM
- Key Derivation: PBKDF2 with HmacSHA256
- Iterations: 65,536
- Tenant-specific keys derived from master key
- GCM provides authenticated encryption

### Data in Transit

| Connection | Protocol |
|------------|----------|
| Client → Gateway | TLS 1.3 |
| Gateway → Services | mTLS |
| Service → Database | TLS |
| WebSocket | WSS (TLS) |

---

## Audit Logging

### HIPAA-Compliant Audit Trail

All security events are logged with:

| Field | Description |
|-------|-------------|
| `timestamp` | ISO 8601 UTC |
| `eventType` | Security event category |
| `userId` | Acting user |
| `tenantId` | Tenant context |
| `resourceType` | Accessed resource |
| `action` | CRUD operation |
| `ipAddress` | Client IP |
| `userAgent` | Client identifier |
| `outcome` | Success/Failure |

**Logged Events:**
- Authentication attempts (success/failure)
- Authorization decisions
- PHI access
- Configuration changes
- WebSocket connections
- Security violations

**Output Format:**
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "eventType": "AUTHENTICATION",
  "action": "LOGIN_SUCCESS",
  "userId": "user-uuid",
  "tenantId": "demo-tenant",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

**Integrations:**
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- AWS CloudWatch
- Kafka (real-time streaming)

---

## Rate Limiting

### Multi-Layer Protection

| Layer | Limit | Scope |
|-------|-------|-------|
| Gateway | 1000 req/min | Per tenant |
| Auth Endpoints | 10 req/min | Per IP |
| CQL Engine | 100 req/min | Per user |
| Token Refresh | 20 req/min | Per user |

**Response Headers:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1705312800
Retry-After: 60
```

**HTTP 429 Response:**
```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests",
  "retryAfter": 60
}
```

---

## Input Validation

### OWASP Protection

| Attack Vector | Mitigation |
|---------------|------------|
| SQL Injection | Parameterized queries, ORM |
| CQL Injection | Input sanitization, allowlists |
| XSS | Output encoding, CSP headers |
| Path Traversal | Path validation, sandboxing |
| Header Injection | CRLF filtering |

**Validation Framework:**
- Jakarta Bean Validation
- Spring Security filters
- Custom validators for healthcare data

---

## Network Security

### Deployment Zones

```
┌─────────────────────────────────────────────────────┐
│                   PUBLIC ZONE                        │
│  ┌─────────────┐                                    │
│  │ Load Balancer│  WAF, DDoS Protection             │
│  └──────┬──────┘                                    │
└─────────┼───────────────────────────────────────────┘
          │
┌─────────┼───────────────────────────────────────────┐
│         │           DMZ                              │
│  ┌──────▼──────┐                                    │
│  │ API Gateway │  Authentication, Rate Limiting     │
│  └──────┬──────┘                                    │
└─────────┼───────────────────────────────────────────┘
          │
┌─────────┼───────────────────────────────────────────┐
│         │      APPLICATION ZONE                      │
│  ┌──────▼──────┐  ┌─────────────┐  ┌─────────────┐ │
│  │FHIR Service │  │Quality Svc  │  │CQL Engine   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────┘
          │
┌─────────┼───────────────────────────────────────────┐
│         │        DATA ZONE                           │
│  ┌──────▼──────┐  ┌─────────────┐                   │
│  │ PostgreSQL  │  │    Redis    │  Encrypted        │
│  └─────────────┘  └─────────────┘                   │
└─────────────────────────────────────────────────────┘
```

### Firewall Rules

| Source | Destination | Port | Protocol |
|--------|-------------|------|----------|
| Internet | Load Balancer | 443 | HTTPS |
| Load Balancer | Gateway | 8080 | HTTP |
| Gateway | Services | 8081-8090 | HTTP |
| Services | PostgreSQL | 5432 | TCP |
| Services | Redis | 6379 | TCP |

---

## Compliance Mapping

### HIPAA Security Rule

| Requirement | Implementation |
|-------------|----------------|
| §164.312(a)(1) Access Control | RBAC, Tenant Isolation |
| §164.312(a)(2)(i) Unique User ID | JWT sub claim |
| §164.312(a)(2)(iii) Automatic Logoff | 15-min session timeout |
| §164.312(b) Audit Controls | Comprehensive audit logging |
| §164.312(c)(1) Integrity Controls | Input validation, checksums |
| §164.312(d) Authentication | JWT, OAuth 2.0 |
| §164.312(e)(1) Transmission Security | TLS 1.3 |
| §164.312(e)(2)(ii) Encryption | AES-256-GCM |

### SOC2 Trust Principles

| Principle | Controls |
|-----------|----------|
| Security | All above security controls |
| Availability | Rate limiting, health checks |
| Confidentiality | Encryption, access control |
| Processing Integrity | Input validation, audit logs |
| Privacy | Data minimization, consent |

---

## Security Configuration

### Production Checklist

```yaml
# gateway-service application.yml
gateway:
  auth:
    enabled: true
    enforced: true                    # Require authentication
    stripExternalAuthHeaders: true    # Prevent header injection

jwt:
  secret: ${JWT_SECRET}               # From environment/vault
  accessTokenExpiration: 900000       # 15 minutes
  refreshTokenExpiration: 604800000   # 7 days

websocket:
  security:
    require-ssl: true                 # Enforce WSS
    session-timeout-minutes: 15       # Auto logoff
    max-connections-per-user: 3       # Resource limits

rate-limiting:
  enabled: true
  requests-per-minute: 1000
```

### Environment Variables

| Variable | Purpose | Required |
|----------|---------|----------|
| `JWT_SECRET` | Token signing key | Yes |
| `PHI_MASTER_KEY` | PHI encryption key | Yes |
| `PHI_SALT` | Key derivation salt | Yes |
| `DB_PASSWORD` | Database credentials | Yes |
| `REDIS_PASSWORD` | Cache credentials | Yes |

---

## Incident Response

### Security Event Classification

| Severity | Examples | Response Time |
|----------|----------|---------------|
| Critical | Data breach, auth bypass | Immediate |
| High | Multiple failed logins, injection attempt | 1 hour |
| Medium | Rate limit exceeded, invalid tokens | 24 hours |
| Low | Misconfiguration, minor violations | 1 week |

### Alerting

- Real-time alerts via PagerDuty/OpsGenie
- Slack/Teams notifications
- Email escalation chains
- Dashboard monitoring (Grafana)

---

## Penetration Testing

### Scope

- External network penetration test
- Web application security assessment
- API security testing
- Authentication/Authorization testing
- Business logic testing

### Schedule

- Annual third-party penetration test
- Quarterly automated vulnerability scans
- Continuous SAST/DAST in CI/CD

---

## Contact

For security concerns or vulnerability reports:
- Security Team: security@healthdata-in-motion.com
- Bug Bounty: [Program details]
