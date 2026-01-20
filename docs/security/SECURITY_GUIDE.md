# HDIM Security Guide

Comprehensive security reference for developers, operators, and compliance teams. Consolidates HIPAA, authentication, multi-tenant isolation, and production hardening requirements.

---

## Quick Reference

| Topic | Requirement | Enforcement | Link |
|-------|-------------|-------------|------|
| **PHI Cache TTL** | ≤5 minutes | Redis config + code review | [PHI Cache](#phi-cache-management) |
| **Audit Logging** | All PHI access | @Audited annotation | [Audit Trail](#audit-logging) |
| **Tenant Isolation** | Row-level filtering | Database constraints | [Multi-Tenant](#multi-tenant-isolation) |
| **Authentication** | Gateway validates JWT | TrustedHeaderAuthFilter | [JWT Auth](#authentication-architecture) |
| **Authorization** | @PreAuthorize on endpoints | Spring Security | [RBAC](#role-based-access-control) |
| **Data Encryption** | In-transit (TLS), at-rest (DB) | PostgreSQL + HTTPS | [Encryption](#data-encryption) |

---

## PHI (Protected Health Information) Management

### Definition

**PHI includes any patient information** that could identify an individual:
- Names, contact information
- Dates (birth, admission)
- Medical record numbers
- Diagnosis, medication, allergies
- Test results, lab values
- Genetic information

### PHI Cache Requirements

**CRITICAL: Cache TTL ≤ 5 minutes for ALL PHI**

```java
// ✅ CORRECT - TTL enforced
@Cacheable(value = "patientData", key = "#patientId")
// Redis TTL: 300 seconds (5 minutes)
public Patient getPatient(String patientId) {
    return repository.findById(patientId);
}

// ❌ WRONG - No TTL
@Cacheable(value = "userData", key = "#userId")
// Could cache indefinitely!
public User getUser(String userId) {
    return repository.findById(userId);
}
```

### Cache Control Headers

**Every response containing PHI must include headers**:

```java
@GetMapping("/api/v1/patients/{id}")
public ResponseEntity<PatientResponse> getPatient(@PathVariable String id) {
    HttpHeaders headers = new HttpHeaders();

    // Prevent caching in browsers/proxies
    headers.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    headers.set("Pragma", "no-cache");
    headers.set("Expires", "0");

    PatientResponse response = service.getPatient(id);
    return ResponseEntity.ok().headers(headers).body(response);
}
```

### PHI Identification Checklist

Before caching or storing patient data:

```
☐ Does it contain patient identifier (name, MRN, DOB)?
☐ Is it clinical or medical information?
☐ Could it be used to identify someone?
☐ Is it in a healthcare context?

If ANY is YES → It's PHI → Apply 5-minute cache TTL
```

---

## Audit Logging

### Requirement: Log All PHI Access

**HIPAA requires audit trail** of who accessed what patient data when.

```java
@Service
public class PatientService {

    private final AuditService auditService;

    @Audited(eventType = "PHI_ACCESS")
    public Patient getPatient(String patientId, String tenantId) {
        // Automatically logged with:
        // - User ID
        // - Patient ID accessed
        // - Timestamp
        // - Tenant context
        // - Success/failure
        return repository.findByIdAndTenant(patientId, tenantId);
    }

    @Audited(eventType = "PHI_CREATE")
    public Patient createPatient(CreatePatientRequest request, String tenantId) {
        Patient patient = new Patient(...);
        Patient saved = repository.save(patient);

        // Automatically logged with:
        // - User ID who created
        // - New patient ID
        // - Fields created
        // - Timestamp

        return saved;
    }

    @Audited(eventType = "PHI_UPDATE")
    public Patient updatePatient(String patientId, UpdateRequest request, String tenantId) {
        // Automatically logged with what changed
        return repository.save(...);
    }
}
```

### Audit Log Access Control

Audit logs themselves are sensitive. Restrict access:

```java
@GetMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")  // ADMIN only
public List<AuditLogResponse> getAuditLogs() {
    // Only admins can see who accessed what
    return auditService.getAllLogs();
}
```

---

## Multi-Tenant Isolation

### Core Principle

**Data from Tenant A must NEVER be visible to Tenant B**

### Implementation Pattern

Every table must have `tenant_id` column:

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)  // ← REQUIRED
    private String tenantId;

    @Column(name = "first_name")
    private String firstName;
    // ... more fields
}
```

Every query must filter by tenant:

```java
// ✅ CORRECT - Filters by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(
    @Param("id") String id,
    @Param("tenantId") String tenantId);

// ❌ WRONG - Missing tenant filter
@Query("SELECT p FROM Patient p WHERE p.id = :id")
Optional<Patient> findById(@Param("id") String id);
```

### Database Constraints

Enforce isolation at database level using Liquibase:

```xml
<!-- Enforce tenant_id presence -->
<addNotNullConstraint tableName="patients" columnName="tenant_id"/>

<!-- Enforce uniqueness per tenant -->
<addUniqueConstraint
    tableName="patients"
    columnNames="tenant_id,patient_id"
    constraintName="uc_patient_per_tenant"/>

<!-- Index for performance -->
<createIndex indexName="idx_patients_tenant_id" tableName="patients">
    <column name="tenant_id"/>
</createIndex>
```

### Testing Tenant Isolation

```java
@Test
void testTenantIsolation_CrossTenantCannotAccessData() {
    // Create patient in tenant1
    String patientId = "p123";
    Patient p1 = Patient.builder()
        .id(UUID.randomUUID())
        .tenantId("tenant1")
        .firstName("John")
        .build();
    repository.save(p1);

    // tenant2 tries to access tenant1's patient
    Optional<Patient> result = repository.findByIdAndTenant(patientId, "tenant2");

    // Should NOT find it (isolation enforced)
    assertThat(result).isEmpty();
}
```

### Tenant Validation in API

Every API endpoint must validate tenant access:

```java
@GetMapping("/api/v1/patients/{patientId}")
public ResponseEntity<PatientResponse> getPatient(
        @PathVariable String patientId,
        @RequestHeader("X-Tenant-ID") String tenantId) {

    // 1. Validate header present
    if (tenantId == null || tenantId.isEmpty()) {
        return ResponseEntity.badRequest().build();
    }

    // 2. Validate user authorized for this tenant
    if (!securityContext.isAuthorizedForTenant(tenantId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // 3. Query with tenant filter
    Patient patient = service.getPatient(patientId, tenantId);

    return ResponseEntity.ok(patient);
}
```

---

## Authentication Architecture

### JWT Validation Flow

**Gateway validates JWT once. Services trust headers.**

```
┌─────────┐
│ Client  │
│ JWT:    │
│ header  │
└────┬────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ gateway-*-service (8001-8004)           │
│ ✓ Validates JWT (decode + verify)       │
│ ✓ Extracts claims (user_id, roles)      │
│ ✓ Injects X-Auth-* headers              │
│ ✓ Signs headers with HMAC               │
└────┬────────────────────────────────────┘
     │ X-Auth-User-Id: user123
     │ X-Auth-Tenant-Ids: t1,t2
     │ X-Auth-Roles: ADMIN
     │ X-Auth-Validated: hmac_sig
     ▼
┌─────────────────────────────────┐
│ Backend Service                 │
│ ✓ Trusts X-Auth-* headers       │
│ ✓ No JWT re-validation          │
│ ✓ No database lookups           │
│ ✓ Verifies HMAC signature       │
└─────────────────────────────────┘
```

### Why No Re-validation?

- **Performance**: JWT validation happens once, not in every service
- **Consistency**: Single source of truth (gateway)
- **Scalability**: Reduces CPU load in backend services
- **Security**: HMAC signature prevents header spoofing

### Invalid Token Response

```java
if (!jwtService.isValid(token)) {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("Invalid or expired JWT"));
}
```

---

## Role-Based Access Control (RBAC)

### Role Hierarchy

```
SUPER_ADMIN (full system access)
    ↓
ADMIN (tenant-level admin)
    ↓
EVALUATOR (run measures, view results)
    ↓
ANALYST (reports, analytics only)
    ↓
VIEWER (read-only access)
```

### Endpoint Protection

```java
@GetMapping("/api/v1/patients")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // Requires ADMIN or EVALUATOR
public List<PatientResponse> listPatients() {
    return service.listPatients();
}

@DeleteMapping("/api/v1/patients/{id}")
@PreAuthorize("hasRole('ADMIN')")  // ADMIN only
public ResponseEntity<?> deletePatient(@PathVariable String id) {
    service.deletePatient(id);
    return ResponseEntity.ok().build();
}

@GetMapping("/api/v1/admin/config")
@PreAuthorize("hasRole('SUPER_ADMIN')")  // SUPER_ADMIN only
public SystemConfigResponse getConfig() {
    return service.getConfig();
}
```

### Test User Accounts (Development Only)

| Username | Password | Role |
|----------|----------|------|
| test_superadmin | password123 | SUPER_ADMIN |
| test_admin | password123 | ADMIN |
| test_evaluator | password123 | EVALUATOR |
| test_analyst | password123 | ANALYST |
| test_viewer | password123 | VIEWER |

**Never use these passwords in production.**

---

## Data Encryption

### In-Transit (HTTPS/TLS)

**All API communication must use HTTPS**

```yaml
# Production configuration
server:
  ssl:
    enabled: true
    key-store: ${KEYSTORE_PATH}
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### At-Rest (Database)

**PostgreSQL encryption options**:

```sql
-- Enable SSL for PostgreSQL
ssl = on

-- Encrypt connection between app and database
host    all             all             0.0.0.0/0               md5 ssl

-- Use certificate for verification
sslmode = require
```

### Column-Level Encryption (Optional)

For extra-sensitive data:

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private UUID id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "ssn_encrypted")
    private String socialSecurityNumber;  // Stored encrypted in DB
}
```

---

## Production Security Hardening

### Pre-Deployment Checklist

```
AUTHENTICATION & AUTHORIZATION
☐ All endpoints have @PreAuthorize
☐ JWT validation working in gateway
☐ X-Auth-* headers being injected
☐ HMAC signature configured
☐ Test user accounts disabled in production

HIPAA COMPLIANCE
☐ PHI cache TTL ≤ 5 minutes (verified)
☐ Cache-Control headers on all PHI responses
☐ @Audited on all PHI access methods
☐ Audit log retention policy set
☐ Audit logs encrypted and protected

MULTI-TENANT ISOLATION
☐ All tables have tenant_id column
☐ All queries filter by tenant_id
☐ Database constraints enforcing isolation
☐ Tenant isolation tests passing
☐ X-Tenant-ID header validation working

DATA ENCRYPTION
☐ HTTPS/TLS enabled
☐ Database connection using SSL
☐ Secrets not in code (use environment variables)
☐ Private keys protected
☐ Certificates up-to-date

SECRETS MANAGEMENT
☐ Database passwords in HashiCorp Vault (not code)
☐ API keys not in code
☐ JWT signing secret in vault
☐ HMAC secret in vault
☐ No secrets in git history

LOGGING & MONITORING
☐ Audit logging enabled
☐ Access logs stored securely
☐ Sensitive data masked in logs
☐ Log retention policy configured
☐ Alerting for suspicious activity

NETWORK SECURITY
☐ Firewall rules restricting access
☐ PostgreSQL not exposed to internet
☐ Redis not exposed to internet
☐ Kafka not exposed to internet
☐ VPN required for admin access
```

### Secrets Configuration

**Never commit secrets. Use environment variables:**

```bash
# ✅ CORRECT
export DB_PASSWORD=$(cat ~/.secrets/db-password)
./gradlew run

# ❌ WRONG
# Password in application.yml
spring.datasource.password: my-secret-password
```

---

## Vulnerability Management

### Common HIPAA Violations

| Violation | Example | Fix |
|-----------|---------|-----|
| **Infinite cache TTL** | @Cacheable no TTL | Add TTL ≤ 300s |
| **Missing tenant filter** | `findById(id)` | Add `findByIdAndTenant(id, tenantId)` |
| **No audit logging** | Service method without @Audited | Add @Audited annotation |
| **Hardcoded secrets** | `password: "secret123"` | Use environment variables |
| **Missing HTTPS** | HTTP API endpoint | Enable SSL/TLS |

### Security Code Review Checklist

Before approving PR:

```
☐ No secrets in code
☐ PHI has cache TTL ≤ 300s
☐ Queries filter by tenant_id
☐ Endpoints have @PreAuthorize
☐ Audit logging for PHI access
☐ Error messages don't leak information
☐ No SQL injection vulnerability
☐ No XXE/XML vulnerability
☐ Input validation present
☐ Output encoding correct
```

---

## Compliance References

### HIPAA (Health Insurance Portability and Accountability Act)

**Requirements HDIM must follow**:
- ✓ Access controls (authentication + authorization)
- ✓ Audit logging (who accessed what when)
- ✓ Data encryption (in-transit + at-rest)
- ✓ Breach notification (incident response plan)
- ✓ Workforce security (staff training)

### HITRUST Certification

HDIM is designed to support HITRUST compliance:
- Multi-tenant isolation
- Role-based access control
- Comprehensive audit trail
- Encryption capabilities
- Incident response procedures

---

## Incident Response

### Data Breach Discovery

If PHI exposure suspected:

1. **Isolate**: Stop services if necessary
2. **Identify**: What data, which tenants, time window
3. **Assess**: Risk level, number of individuals affected
4. **Notify**: HIPAA breach notification rules apply
5. **Document**: Timeline, response actions
6. **Prevent**: Implement fix to prevent recurrence

### Example: Cache TTL Violation Detected

```
INCIDENT: Patient data cached > 5 minutes
1. Stop affected service
2. Query cache to identify exposed patients
3. Assess: How many? How long exposed?
4. Notify: Affected tenants
5. Fix: Reduce cache TTL
6. Verify: Cache compliance tests passing
7. Redeploy: With fix
8. Follow-up: Post-incident review
```

---

## Security Resources

### Internal Documentation
- **[HIPAA Compliance](../../backend/HIPAA-CACHE-COMPLIANCE.md)** - Detailed PHI rules
- **[Gateway Trust Architecture](../../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Auth details
- **[Coding Standards](../../backend/docs/CODING_STANDARDS.md)** - Security patterns
- **[Entity-Migration Guide](../../backend/docs/ENTITY_MIGRATION_GUIDE.md)** - Database patterns

### Architecture Decisions
- **[ADR-007: Gateway-Trust Auth](../architecture/decisions/ADR-007-gateway-trust-authentication.md)**
- **[ADR-009: Multi-Tenant Isolation](../architecture/decisions/ADR-009-multi-tenant-isolation.md)**
- **[ADR-010: HIPAA PHI Cache TTL](../architecture/decisions/ADR-010-hipaa-phi-cache-ttl.md)**

### External Standards
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
- [HITRUST Framework](https://hitrustalliance.net/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Docs](https://spring.io/projects/spring-security)

---

## Getting Help

### Security Questions?
- Post in #security Slack channel
- Contact: Security Lead
- Security review required for: PHI access, auth changes, encryption

### Incident Report
- Non-emergency: security@hdim-team.org
- Emergency: Page on-call security engineer

---

## Summary

**HDIM Security Pillars**:

1. **Authentication**: JWT validated at gateway once
2. **Authorization**: @PreAuthorize on all endpoints
3. **Isolation**: tenant_id filtering on all queries
4. **Encryption**: HTTPS + database SSL
5. **Audit**: @Audited on all PHI access
6. **Cache**: TTL ≤ 5 minutes for PHI
7. **Compliance**: HIPAA-ready architecture

Follow these practices and HDIM remains security-hardened for healthcare operations.

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Compliance: HIPAA, HITRUST-ready_
_Status: Production-Ready Security Framework_
