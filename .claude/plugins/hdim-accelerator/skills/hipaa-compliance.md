---
name: hipaa-compliance
description: Comprehensive guide to HIPAA compliance requirements in HDIM, including PHI handling, audit logging, cache management, and security controls
---

# HIPAA Compliance Skill

## Overview

This skill provides comprehensive guidance on implementing HIPAA-compliant code in the HDIM platform. HDIM processes **Protected Health Information (PHI)** and MUST comply with HIPAA Security Rule (45 CFR § 164.312) and Privacy Rule (45 CFR § 164.502).

**Failure to comply with HIPAA requirements can result in:**
- Civil penalties: $100 - $50,000 per violation
- Criminal penalties: Up to $250,000 in fines and 10 years imprisonment
- Loss of customer trust and business contracts
- Legal liability for data breaches

## Core HIPAA Requirements in HDIM

### 1. Audit Logging (§ 164.312(b) - Audit Controls)

**Requirement:** All access to PHI MUST be logged with sufficient detail to support forensic analysis.

#### Implementation: @Audited Annotation

```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

  @GetMapping("/{patientId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(
    action = AuditAction.READ,          // Type of access
    resourceType = "Patient",            // Resource being accessed
    encryptPayload = true                // Encrypt sensitive data in logs
  )
  public ResponseEntity<PatientResponse> getPatient(
      @PathVariable String patientId,
      @RequestHeader("X-Tenant-ID") String tenantId,
      @RequestHeader("X-User-ID") String userId) {

    PatientResponse patient = patientService.getPatient(patientId, tenantId);
    return ResponseEntity.ok(patient);
  }
}
```

**What Gets Logged:**
- **Who:** User ID from JWT token (X-User-ID header)
- **What:** Resource type + action (Patient + READ)
- **When:** Timestamp (UTC, ISO-8601 format)
- **Where:** IP address, service name, tenant ID
- **Result:** Success/failure + HTTP status code
- **Details:** Encrypted payload (optional, for sensitive operations)

**Audit Event Entity:**
```java
@Entity
@Table(name = "audit_events", indexes = {
  @Index(name = "idx_audit_tenant_time", columnList = "tenant_id,event_timestamp"),
  @Index(name = "idx_audit_user_time", columnList = "user_id,event_timestamp"),
  @Index(name = "idx_audit_resource_time", columnList = "resource_type,resource_id,event_timestamp")
})
public class AuditEventEntity {
  @Id private UUID id;
  private String tenantId;
  private String userId;
  private String userName;
  private AuditAction action;              // READ, CREATE, UPDATE, DELETE
  private String resourceType;
  private String resourceId;
  private Instant eventTimestamp;
  private String ipAddress;
  private String userAgent;
  private Integer httpStatus;
  private String serviceName;

  @Column(columnDefinition = "text")
  private String encryptedPayload;         // Encrypted using AES-256

  private String errorMessage;             // For failed operations
}
```

**Supported Audit Actions:**
- `READ` - Viewing patient data, FHIR resources, care gaps
- `CREATE` - Creating new patients, observations, conditions
- `UPDATE` - Modifying existing PHI records
- `DELETE` - Soft-deleting PHI (HIPAA requires retention)
- `SEARCH` - Query operations (batch/population-level)
- `EXPORT` - Bulk data exports for reporting
- `BATCH` - Bundle operations affecting multiple records

#### Audit Logging Best Practices

**✅ DO:**
- Use `@Audited` on ALL controller methods accessing PHI
- Set `encryptPayload = true` for sensitive operations (updates, deletes, searches with PII)
- Include meaningful resource types (e.g., "Patient", "Observation", "MedicationRequest")
- Log failed access attempts (authentication failures, authorization denials)
- Retain audit logs for **7 years** (HIPAA requirement)

**❌ DON'T:**
- Skip audit logging on "read-only" endpoints (all PHI access MUST be logged)
- Log unencrypted PHI in plain text
- Delete audit logs (use soft delete with archival to cold storage)
- Use generic resource types like "Data" or "Record" (be specific)
- Log password hashes or authentication tokens

### 2. Cache Management (§ 164.312(e) - Transmission Security)

**Requirement:** PHI stored in cache MUST expire within 5 minutes to minimize exposure window.

#### Redis Cache Configuration

```yaml
# application.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes in milliseconds (MAXIMUM for PHI)
      cache-null-values: false
      key-prefix: "hdim:${spring.application.name}:"
      use-key-prefix: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6380}
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true
      timeout: 2000
```

#### Cacheable Method Pattern

```java
@Service
public class PatientService {

  // Cache patient data for 5 minutes
  @Cacheable(
    value = "patientData",
    key = "#tenantId + ':' + #patientId",
    unless = "#result == null"
  )
  public PatientResponse getPatient(String patientId, String tenantId) {
    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
  }

  // Evict cache on update
  @CacheEvict(
    value = "patientData",
    key = "#tenantId + ':' + #patientId"
  )
  public PatientResponse updatePatient(
      String patientId, String tenantId, UpdatePatientRequest request) {
    // Update logic...
  }
}
```

**Cache TTL Guidelines:**

| Data Type | Maximum TTL | Rationale |
|-----------|-------------|-----------|
| Patient demographics | 5 minutes | Direct PHI |
| FHIR resources (Patient, Observation) | 5 minutes | Direct PHI |
| Care gaps | 5 minutes | Contains PHI references |
| Measure evaluations (patient-specific) | 5 minutes | Derived from PHI |
| Measure definitions (metadata) | 1 hour | No PHI, safe to cache longer |
| User roles/permissions | 15 minutes | No PHI, frequently accessed |
| Terminology/value sets | 24 hours | Public data, rarely changes |

**Cache Validation:**
```bash
# Check cache TTL configuration
grep -r "time-to-live" backend/modules/services/*/src/main/resources/application*.yml

# Verify all values ≤ 300000 ms
awk '/time-to-live:/ {if ($2 > 300000) print FILENAME, $2}' application*.yml
```

### 3. HTTP Cache-Control Headers (§ 164.312(e))

**Requirement:** HTTP responses containing PHI MUST include no-cache headers to prevent browser/proxy caching.

#### Controller Response Pattern

```java
@GetMapping("/observations")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
@Audited(action = AuditAction.READ, resourceType = "Observation", encryptPayload = true)
public ResponseEntity<List<ObservationResponse>> getObservations(
    @PathVariable String patientId,
    @RequestHeader("X-Tenant-ID") String tenantId) {

  List<ObservationResponse> observations = observationService.getObservations(patientId, tenantId);

  return ResponseEntity.ok()
      .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
      .header("Pragma", "no-cache")
      .header("Expires", "0")
      .body(observations);
}
```

**Header Explanations:**
- `Cache-Control: no-store` - Prevents caching entirely (strongest directive)
- `Cache-Control: no-cache` - Requires revalidation before serving cached copy
- `Cache-Control: must-revalidate` - Forces revalidation when stale
- `Cache-Control: private` - Prevents shared caches (CDNs, proxies)
- `Pragma: no-cache` - HTTP/1.0 backwards compatibility
- `Expires: 0` - Immediate expiration (HTTP/1.0 compatibility)

#### Response Interceptor (Apply Headers Globally)

```java
@Configuration
public class SecurityHeadersConfig {

  @Bean
  public FilterRegistrationBean<CacheControlFilter> cacheControlFilter() {
    FilterRegistrationBean<CacheControlFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new CacheControlFilter());
    registrationBean.addUrlPatterns(
        "/api/v1/patients/*",
        "/api/v1/fhir/Patient/*",
        "/api/v1/fhir/Observation/*",
        "/api/v1/care-gaps/*"
    );
    return registrationBean;
  }
}

public class CacheControlFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");

    filterChain.doFilter(request, response);
  }
}
```

### 4. Multi-Tenant Isolation (§ 164.308(a)(4) - Access Control)

**Requirement:** PHI MUST be isolated by tenant to prevent cross-tenant data leakage.

#### Repository Pattern with Tenant Filtering

```java
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

  // ✅ CORRECT - Always includes tenantId
  @Query("SELECT p FROM PatientEntity p " +
         "WHERE p.tenantId = :tenantId AND p.id = :id")
  Optional<PatientEntity> findByIdAndTenant(
      @Param("id") String id,
      @Param("tenantId") String tenantId);

  // ✅ CORRECT - List with tenant filter
  @Query("SELECT p FROM PatientEntity p " +
         "WHERE p.tenantId = :tenantId " +
         "ORDER BY p.lastName, p.firstName")
  List<PatientEntity> findAllByTenant(
      @Param("tenantId") String tenantId,
      Pageable pageable);

  // ❌ INCORRECT - Missing tenant filter (CRITICAL VIOLATION)
  @Query("SELECT p FROM PatientEntity p WHERE p.id = :id")
  Optional<PatientEntity> findById(@Param("id") String id);
}
```

#### Entity-Level Tenant Enforcement

```java
@Entity
@Table(name = "patients", indexes = {
  @Index(name = "idx_patient_tenant", columnList = "tenant_id"),
  @Index(name = "idx_patient_tenant_mrn", columnList = "tenant_id,mrn", unique = true)
})
public class PatientEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  @Column(name = "mrn", nullable = false)
  private String mrn;  // Medical Record Number (unique per tenant)

  // Prevent modification of tenantId after creation
  @PrePersist
  protected void onCreate() {
    if (tenantId == null) {
      throw new IllegalStateException("tenantId must be set before persisting");
    }
  }

  @PreUpdate
  protected void onUpdate() {
    // Hibernate will prevent tenantId changes due to updatable=false
    // This is a defensive check
  }
}
```

#### Service-Level Tenant Validation

```java
@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final TenantContext tenantContext;

  public PatientResponse getPatient(String patientId) {
    String tenantId = tenantContext.getCurrentTenantId();

    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
  }

  @Transactional
  public PatientResponse createPatient(CreatePatientRequest request) {
    String tenantId = tenantContext.getCurrentTenantId();

    // Verify tenant has permission to create patients
    if (!tenantContext.hasPermission("patient:create")) {
      throw new AccessDeniedException("Insufficient permissions to create patient");
    }

    PatientEntity entity = new PatientEntity();
    entity.setTenantId(tenantId);  // Set tenant at creation
    entity.setMrn(request.getMrn());
    entity.setFirstName(request.getFirstName());
    entity.setLastName(request.getLastName());

    return mapToResponse(patientRepository.save(entity));
  }
}
```

### 5. Data Encryption (§ 164.312(a)(2)(iv) - Encryption and Decryption)

**Requirement:** PHI at rest and in transit MUST be encrypted using industry-standard algorithms.

#### Database Encryption (At Rest)

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5435/${DB_NAME}?ssl=true&sslmode=require
    username: ${DB_USER}
    password: ${DB_PASSWORD}

# PostgreSQL encryption configuration
# - Transparent Data Encryption (TDE) enabled at infrastructure level
# - Column-level encryption for highly sensitive fields (SSN, payment info)
```

#### SSL/TLS Configuration (In Transit)

```yaml
# Gateway SSL termination
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: hdim-gateway

# Internal service communication (mutual TLS)
feign:
  client:
    config:
      default:
        url: https://patient-service:8084
        ssl:
          trust-store: classpath:truststore.p12
          trust-store-password: ${TRUSTSTORE_PASSWORD}
```

#### Field-Level Encryption (Sensitive Data)

```java
@Entity
public class PatientEntity {

  @Column(name = "ssn")
  @Convert(converter = EncryptedStringConverter.class)
  private String ssn;  // Automatically encrypted/decrypted

  @Column(name = "email")
  private String email;  // Not encrypted (not considered PHI)
}

// JPA Converter for automatic encryption
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private final EncryptionService encryptionService;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    return encryptionService.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return encryptionService.decrypt(dbData);
  }
}

@Service
public class EncryptionService {

  // AES-256 encryption with GCM mode
  public String encrypt(String plaintext) {
    // Implementation using javax.crypto.Cipher
    // Key management via HashiCorp Vault or AWS KMS
  }

  public String decrypt(String ciphertext) {
    // Decryption implementation
  }
}
```

### 6. Access Control (§ 164.308(a)(4) - Information Access Management)

**Requirement:** Implement role-based access control (RBAC) with minimum necessary principle.

#### Role Hierarchy

```
SUPER_ADMIN
  └─ Full system access (all tenants)
     └─ ADMIN
        └─ Tenant-level administration
           └─ EVALUATOR
              └─ Run evaluations, view results
                 └─ ANALYST
                    └─ View reports (no patient-level detail)
                       └─ VIEWER
                          └─ Read-only access
```

#### Controller-Level Authorization

```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

  // Only ADMIN and EVALUATOR can view patient details
  @GetMapping("/{patientId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.READ, resourceType = "Patient", encryptPayload = true)
  public ResponseEntity<PatientResponse> getPatient(...) { ... }

  // Only ADMIN can create patients
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Audited(action = AuditAction.CREATE, resourceType = "Patient", encryptPayload = true)
  public ResponseEntity<PatientResponse> createPatient(...) { ... }

  // Only ADMIN can delete patients
  @DeleteMapping("/{patientId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Audited(action = AuditAction.DELETE, resourceType = "Patient", encryptPayload = true)
  public ResponseEntity<Void> deletePatient(...) { ... }
}
```

#### Service-Level Authorization (Fine-Grained)

```java
@Service
public class PatientService {

  public PatientResponse getPatient(String patientId, String tenantId, String userId) {
    // Verify user has access to this tenant
    if (!userBelongsToTenant(userId, tenantId)) {
      throw new AccessDeniedException("User does not have access to this tenant");
    }

    // Verify user has READ permission
    if (!hasPermission(userId, "patient:read")) {
      throw new AccessDeniedException("User lacks patient:read permission");
    }

    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
  }
}
```

### 7. Data Retention and Disposal (§ 164.310(d)(2)(i) - Disposal)

**Requirement:** PHI retention for 7 years minimum, then secure disposal.

#### Soft Delete Pattern

```java
@Entity
@Table(name = "patients")
@SQLDelete(sql = "UPDATE patients SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class PatientEntity {

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }
}

@Service
public class PatientService {

  @Transactional
  public void deletePatient(String patientId, String tenantId) {
    PatientEntity patient = patientRepository.findByIdAndTenant(patientId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

    // Soft delete (sets deleted_at timestamp)
    patientRepository.delete(patient);

    // Schedule hard delete after 7 years (HIPAA retention period)
    scheduleHardDelete(patientId, Instant.now().plus(7, ChronoUnit.YEARS));
  }
}
```

#### Scheduled Purge Job

```java
@Component
public class DataRetentionJob {

  @Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
  public void purgeExpiredRecords() {
    Instant cutoffDate = Instant.now().minus(7, ChronoUnit.YEARS);

    // Find records deleted >7 years ago
    List<PatientEntity> expiredPatients = patientRepository
        .findByDeletedAtBefore(cutoffDate);

    for (PatientEntity patient : expiredPatients) {
      // Permanently delete from database
      patientRepository.hardDelete(patient.getId());

      // Delete associated records (cascading deletion)
      observationRepository.hardDeleteByPatientId(patient.getId());
      conditionRepository.hardDeleteByPatientId(patient.getId());

      // Log disposal for compliance
      auditService.logDisposal(patient.getId(), "7-year retention period expired");
    }
  }
}
```

## Common HIPAA Violations in Code

### Violation 1: Missing Audit Logging

```java
// ❌ INCORRECT - No @Audited annotation
@GetMapping("/patients/{patientId}")
public ResponseEntity<PatientResponse> getPatient(@PathVariable String patientId) {
  return ResponseEntity.ok(patientService.getPatient(patientId));
}

// ✅ CORRECT - Audit logging enabled
@GetMapping("/patients/{patientId}")
@Audited(action = AuditAction.READ, resourceType = "Patient", encryptPayload = true)
public ResponseEntity<PatientResponse> getPatient(@PathVariable String patientId) {
  return ResponseEntity.ok(patientService.getPatient(patientId));
}
```

### Violation 2: Excessive Cache TTL

```yaml
# ❌ INCORRECT - Cache TTL exceeds 5 minutes
spring:
  cache:
    redis:
      time-to-live: 3600000  # 1 hour (HIPAA violation!)

# ✅ CORRECT - Compliant cache TTL
spring:
  cache:
    redis:
      time-to-live: 300000  # 5 minutes (maximum)
```

### Violation 3: Missing Tenant Isolation

```java
// ❌ INCORRECT - No tenant filtering (data leakage risk)
@Query("SELECT p FROM PatientEntity p WHERE p.id = :id")
Optional<PatientEntity> findById(@Param("id") String id);

// ✅ CORRECT - Tenant isolation enforced
@Query("SELECT p FROM PatientEntity p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<PatientEntity> findByIdAndTenant(
    @Param("id") String id,
    @Param("tenantId") String tenantId);
```

### Violation 4: Logging PHI in Plain Text

```java
// ❌ INCORRECT - Logs PHI (patient name, DOB)
logger.info("Creating patient: {} {}, DOB: {}",
    request.getFirstName(), request.getLastName(), request.getBirthDate());

// ✅ CORRECT - Logs only identifiers
logger.info("Creating patient with MRN: {}", request.getMrn());
```

### Violation 5: Storing PHI in Non-Encrypted Fields

```java
// ❌ INCORRECT - SSN stored in plain text
@Column(name = "ssn")
private String ssn;

// ✅ CORRECT - Automatic encryption via converter
@Column(name = "ssn")
@Convert(converter = EncryptedStringConverter.class)
private String ssn;
```

## HIPAA Compliance Checklist

Before committing code that accesses PHI, verify:

- [ ] All PHI endpoints have `@Audited` annotations
- [ ] `@Audited` includes correct `action` and `resourceType` parameters
- [ ] `encryptPayload = true` for sensitive operations
- [ ] Cache TTL ≤ 5 minutes (300000 ms) for PHI
- [ ] HTTP responses include `Cache-Control: no-store` headers
- [ ] All repository queries include `tenantId` filtering
- [ ] Entities have non-null, non-updatable `tenantId` column
- [ ] SSL/TLS enabled for database connections
- [ ] Mutual TLS configured for inter-service communication
- [ ] Field-level encryption for highly sensitive data (SSN, payment info)
- [ ] `@PreAuthorize` annotations on all API endpoints
- [ ] Role hierarchy enforced (minimum necessary access)
- [ ] Soft delete implemented (hard delete after 7 years)
- [ ] No PHI logged in plain text (use identifiers only)
- [ ] Audit logs retained for 7 years
- [ ] Unit tests validate tenant isolation
- [ ] Integration tests verify audit logging

## Related Documentation

- **HIPAA Security Rule:** https://www.hhs.gov/hipaa/for-professionals/security/
- **HIPAA Privacy Rule:** https://www.hhs.gov/hipaa/for-professionals/privacy/
- **HDIM Compliance Guide:** `backend/HIPAA-CACHE-COMPLIANCE.md`
- **Audit Logging Guide:** `backend/docs/AUDIT_LOGGING_GUIDE.md`
- **Security Architecture:** `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Coding Standards:** `backend/docs/CODING_STANDARDS.md`

## Compliance Resources

- **HIPAA Compliance Agent:** `.claude/plugins/hdim-accelerator/agents/hipaa-compliance-agent.md`
- **Audit Query API:** Endpoint for compliance reporting and forensic analysis
- **Tenant Isolation Tests:** `backend/modules/shared/testing/src/test/java/TenantIsolationTest.java`
- **Encryption Tests:** `backend/modules/shared/infrastructure/audit/src/test/java/EncryptionServiceTest.java`

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**Compliance Status:** HIPAA Security Rule + Privacy Rule Compliant
