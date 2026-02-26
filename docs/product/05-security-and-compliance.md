# HDIM — Security, Compliance & Data Governance

*For CISOs, compliance officers, security reviewers, and HIPAA auditors evaluating the platform.*

---

## Compliance Summary

| Standard | Status | Evidence |
|----------|--------|----------|
| **HIPAA** (Privacy Rule, Security Rule) | ✅ Built-in | PHI caching controls, audit logging, access controls, encryption |
| **HIPAA §164.312(a)** — Access Control | ✅ Enforced | JWT auth, RBAC (11 roles), tenant isolation |
| **HIPAA §164.312(a)(2)(iii)** — Automatic Logoff | ✅ Implemented | 15-minute idle timeout with audit trail |
| **HIPAA §164.312(b)** — Audit Controls | ✅ Implemented | 100% HTTP audit interceptor, event store |
| **HIPAA §164.312(c)** — Integrity Controls | ✅ Enforced | Event sourcing (immutable log), schema validation |
| **HIPAA §164.312(e)** — Transmission Security | ✅ Enforced | TLS everywhere, no-store cache headers |
| **NCQA HEDIS** | ✅ Certified measures | CQL-based, auditable evaluation logic |
| **FHIR R4** | ✅ Conformant | HAPI FHIR 7.x, CapabilityStatement published |
| **SOC 2 Type II** | 📋 Architecture-ready | Controls mapped; audit pending deployment context |
| **HITRUST CSF** | 📋 Aligned | Comprehensive control mapping available |

---

## Authentication Architecture

### Gateway-Trust Model

HDIM uses a **gateway-trust architecture** where the API gateway handles all authentication, and downstream services trust authenticated headers.

```
┌─────────┐       TLS        ┌───────────────┐      Trusted Headers     ┌───────────────┐
│  Client  │ ───────────────► │  Edge Gateway │ ──────────────────────►  │  Microservice │
│          │  JWT Cookie      │  (nginx)      │  X-Auth-User-ID          │  (Spring Boot)│
│          │                  │               │  X-Auth-Roles             │               │
│          │                  │  Validates    │  X-Auth-Tenant-ID         │  Trusts       │
│          │                  │  JWT token    │  X-Auth-Email             │  headers      │
└─────────┘                   └───────────────┘                           └───────────────┘
```

**Key decisions:**
- **JWT stored in HttpOnly cookies** — not localStorage, not Authorization headers. Prevents XSS token theft.
- **SameSite=Strict** — prevents CSRF attacks.
- **Secure flag** — cookie only sent over HTTPS.
- **Refresh token rotation** — refresh tokens are single-use, rotated on each use.
- **Token expiry** — access tokens: 15 minutes. Refresh tokens: 7 days.

### Authentication Flow

```
1. User POSTs credentials to /api/v1/auth/login
2. Admin Gateway validates credentials
3. Gateway issues JWT access + refresh tokens (HttpOnly cookies)
4. Client requests hit Edge Gateway → routed to domain gateway
5. Domain gateway validates JWT signature
6. Gateway injects trusted headers (user ID, roles, tenant IDs)
7. Service reads headers, applies RBAC + tenant filtering
```

### Multi-Factor Authentication

MFA is supported via the gateway authentication layer. Configurable per tenant:
- TOTP (time-based one-time password)
- Email verification codes
- SSO delegation (SAML/OIDC)

---

## Role-Based Access Control (RBAC)

### Role Hierarchy

| Role | Scope | Capabilities |
|------|-------|-------------|
| **SUPER_ADMIN** | System-wide | Full system access, tenant management, user provisioning |
| **ADMIN** | Tenant | Tenant config, user management, all tenant data |
| **EVALUATOR** | Tenant | Run CQL evaluations, view/manage quality measures |
| **ANALYST** | Tenant | View reports, dashboards, population analytics |
| **VIEWER** | Tenant | Read-only access to authorized data |
| **PROVIDER** | Panel | Patient care gaps, pre-visit plans, panel management |
| **CARE_MANAGER** | Assigned patients | Gap closure, care plans, patient outreach |
| **QUALITY_LEAD** | Department | Measure management, provider scorecards |
| **AUDITOR** | Tenant (read-only) | Audit logs, compliance reports, PHI access logs |
| **INTEGRATION** | API scope | Machine-to-machine API access |
| **MEMBER** | Self | Member portal access (PHR, appointments) |

### Enforcement Points

Access control is enforced at **every layer**:

```java
// Controller level — Spring Security annotation
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
@GetMapping("/patients/{id}/measures")
public ResponseEntity<MeasureResults> getPatientMeasures(...) { ... }

// Service level — tenant filtering on every query
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);

// Gateway level — route-based access rules
# Only ADMIN and SUPER_ADMIN can access admin APIs
location /admin/ { ... requires ADMIN role ... }
```

---

## Multi-Tenant Data Isolation

HDIM enforces tenant isolation at **six layers** — not just at the application level:

| Layer | Mechanism | What It Prevents |
|-------|-----------|-----------------|
| **1. Gateway** | Route validation per tenant | Cross-tenant API access |
| **2. Application** | `TrustedTenantAccessFilter` on every request | User accessing wrong tenant |
| **3. Query** | `WHERE tenant_id = :tenantId` on every SQL query | Cross-tenant data leak |
| **4. Database** | Separate schemas or databases per service | Physical data separation |
| **5. Cache** | Tenant-scoped Redis key prefixes + TTL | Cross-tenant cache poisoning |
| **6. Events** | Kafka topic partitioning by tenant | Cross-tenant event leak |

### What This Means for Compliance

- **No shared tables between tenants.** Each tenant's data lives in tenant-scoped queries at minimum, with physical separation available.
- **No cross-tenant queries are possible.** Every repository method requires `tenantId` as a parameter.
- **Cache isolation.** Redis keys include tenant ID prefix. PHI cache entries TTL ≤ 5 minutes.
- **Audit isolation.** Each tenant has independent audit trails.

---

## PHI Protection Measures

### Data at Rest

| Control | Implementation |
|---------|---------------|
| **Database encryption** | PostgreSQL TDE (Transparent Data Encryption) via storage layer |
| **Backup encryption** | AES-256 encrypted backups |
| **Key management** | HashiCorp Vault for secret/key management |
| **Data retention** | Configurable per tenant; automated purge policies |

### Data in Transit

| Control | Implementation |
|---------|---------------|
| **External TLS** | TLS 1.2+ on all external endpoints |
| **Internal TLS** | Service-to-service communication encrypted |
| **Cache headers** | `Cache-Control: no-store, no-cache, must-revalidate` on all PHI responses |
| **Cookie security** | HttpOnly, Secure, SameSite=Strict |

### Data in Use

| Control | Implementation |
|---------|---------------|
| **PHI cache TTL** | Maximum 5 minutes for any cached PHI (HIPAA requirement) |
| **No browser console logging** | ESLint-enforced; build fails on `console.log` |
| **PHI-filtered logging** | `LoggerService` automatically strips PHI from log output |
| **Session timeout** | 15-minute idle timeout with 2-minute warning |
| **Memory protection** | No PHI stored in browser localStorage or sessionStorage |

### PHI Code Patterns (Enforced)

```java
// REQUIRED: Cache TTL ≤ 5 minutes for PHI
@Cacheable(value = "patientData", key = "#patientId")
// Redis TTL configured: spring.cache.redis.time-to-live=300000 (5 min)

// REQUIRED: No-store headers on PHI endpoints
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

// REQUIRED: Audit annotation on all PHI access
@Audited(eventType = "PHI_ACCESS")
public Patient getPatient(String patientId) { ... }

// REQUIRED: Tenant scoping on all queries
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(String id, String tenantId);
```

---

## Audit Infrastructure

### Automatic Audit Coverage

HDIM provides **100% audit coverage** of API interactions — no manual instrumentation required.

**HTTP Audit Interceptor (Frontend):**
- Every API call from the Clinical Portal is automatically logged
- Captures: user ID, action type, resource type, resource ID, timestamp, duration, success/failure
- Fire-and-forget batching with offline resilience
- No developer action required — interceptor is globally registered

**Event Store (Backend):**
- All state changes recorded as immutable events via event sourcing
- Events include: who, what, when, and the full before/after state
- Event replay capability for forensic investigation
- Append-only log — events cannot be modified or deleted

### Audit Log Fields

Every audit entry contains:

| Field | Example | Purpose |
|-------|---------|---------|
| `userId` | `user-abc-123` | Who performed the action |
| `tenantId` | `acme-health` | Which tenant context |
| `action` | `READ`, `CREATE`, `UPDATE`, `DELETE` | What was done |
| `resourceType` | `Patient`, `CareGap`, `Evaluation` | What type of resource |
| `resourceId` | `patient-456` | Which specific resource |
| `timestamp` | `2026-02-15T14:30:00Z` | When (UTC) |
| `duration` | `127ms` | How long |
| `outcome` | `SUCCESS` / `FAILURE` | Result |
| `ipAddress` | `10.0.1.15` | Where (network) |
| `sessionId` | `session-xyz` | Session context |

### Session Timeout Auditing

Per HIPAA §164.312(a)(2)(iii):

| Event | Logged Fields |
|-------|--------------|
| **Idle timeout** | User ID, idle duration (15 min), warning shown (yes/no), timestamp |
| **Explicit logout** | User ID, session duration, timestamp |
| **Token expiry** | User ID, token type (access/refresh), timestamp |

---

## Incident Response Capabilities

### Forensic Investigation

Because HDIM uses **event sourcing**, every state change is a permanent, immutable record:

1. **Who accessed what?** → Query audit logs by user ID + resource type
2. **What changed?** → Event store shows before/after for every mutation
3. **When did it happen?** → Millisecond-precision timestamps on all events
4. **What was the system state at time X?** → Event replay reconstructs exact state at any point in time

### Breach Detection Signals

| Signal | Detection Mechanism |
|--------|-------------------|
| Unusual access patterns | Audit log analysis (volume, timing, scope) |
| Cross-tenant access attempt | Gateway filter blocks + alert |
| Failed authentication spikes | Gateway rate limiting + logging |
| Bulk data export | Explicit audit event for FHIR $export |
| Admin actions | Elevated permission audit events |

---

## Infrastructure Security

### Network Architecture

```
Internet
  │
  ▼
┌─────────────────┐
│  WAF / CDN      │  DDoS protection, rate limiting
└────────┬────────┘
         │
┌────────▼────────┐
│  Edge Gateway   │  TLS termination, JWT validation
│  (nginx)        │
└────────┬────────┘
         │ (internal network)
┌────────▼────────┐
│  Domain Gateways│  Route-level authorization
│  (Spring Boot)  │
└────────┬────────┘
         │
┌────────▼────────┐
│  Services       │  Application-level access control
│  (Spring Boot)  │
└────────┬────────┘
         │
┌────────▼────────┐
│  Data Stores    │  PostgreSQL, Redis, Kafka
│                 │  No direct external access
└─────────────────┘
```

### Container Security

| Control | Implementation |
|---------|---------------|
| **Base images** | Minimal JRE images (Eclipse Temurin) |
| **Non-root execution** | All containers run as non-root user |
| **Read-only filesystem** | Where supported |
| **Resource limits** | CPU and memory limits on all containers |
| **Image scanning** | Vulnerability scanning in CI/CD pipeline |
| **No secrets in images** | Secrets injected at runtime via environment/Vault |

### Secret Management

| Secret Type | Storage |
|-------------|---------|
| Database credentials | HashiCorp Vault / Docker Secrets |
| JWT signing keys | Vault / mounted secrets |
| API keys | Vault / environment variables |
| TLS certificates | Cert-Manager (K8s) / mounted volumes |

---

## Compliance Deployment Options

| Requirement | SaaS | Private Cloud | On-Premises |
|------------|-------|---------------|-------------|
| **Data residency** | US multi-region | Customer-chosen region | Customer data center |
| **Network isolation** | VPC + VPN | Dedicated VPC | Air-gapped available |
| **Key management** | Managed KMS | Customer-managed keys | HSM supported |
| **Audit log export** | API + SIEM integration | Same + custom retention | Full local control |
| **Penetration testing** | Vendor-managed schedule | Customer-coordinated | Customer-managed |
| **BAA** | Standard BAA included | Custom BAA available | Not required (on-prem) |

---

## Security Review Checklist

For your security team evaluating HDIM:

- [x] **Authentication:** JWT with HttpOnly cookies, refresh rotation, MFA support
- [x] **Authorization:** RBAC with 11 roles, enforced at gateway + service + query levels
- [x] **Tenant isolation:** 6-layer enforcement (gateway, app, query, database, cache, events)
- [x] **PHI caching:** ≤5-minute TTL, no-store headers, no browser storage
- [x] **Audit logging:** 100% API coverage, immutable event store, session tracking
- [x] **Encryption at rest:** Database TDE, encrypted backups, Vault key management
- [x] **Encryption in transit:** TLS 1.2+ on all connections
- [x] **Session management:** 15-min idle timeout, audit logging on timeout/logout
- [x] **Logging:** PHI-filtered (LoggerService), no console.log in production
- [x] **Container security:** Non-root, minimal images, resource limits
- [x] **Secret management:** Vault integration, no secrets in code or images
- [x] **Input validation:** Spring validation framework, FHIR resource parsing
- [x] **Error handling:** Global error handler, no stack traces to clients
- [x] **CSRF protection:** SameSite=Strict cookies
- [x] **XSS prevention:** HttpOnly cookies, Angular sanitization, CSP headers

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
