# HIPAA Compliance Checklist

## ⚠️ CRITICAL: This Application Handles PHI

All code modifications MUST comply with HIPAA regulations. Violations can result in severe legal and financial consequences.

## Mandatory Reading
- `backend/HIPAA-CACHE-COMPLIANCE.md` - Complete cache compliance guide
- Review this checklist before ANY PHI-related code changes

---

## 1. Cache TTL Requirements

### ❌ NEVER DO THIS
```java
@Cacheable(value = "patientData", key = "#patientId")
// Redis TTL not configured or > 5 minutes
```

### ✅ ALWAYS DO THIS
```java
// PHI cache TTL MUST be <= 5 minutes (300 seconds)
@Cacheable(value = "patientData", key = "#patientId")

// In application.yml:
spring:
  cache:
    redis:
      time-to-live: 300000  # 5 minutes in milliseconds
```

### Verification
```bash
# Check Redis TTL
redis-cli TTL patientData::12345
# Must return value <= 300
```

---

## 2. HTTP Cache Headers for PHI

### ❌ NEVER DO THIS
```java
// Missing cache-control headers
return ResponseEntity.ok(patientData);
```

### ✅ ALWAYS DO THIS
```java
// All PHI responses MUST include no-cache headers
@GetMapping("/patients/{id}")
public ResponseEntity<Patient> getPatient(@PathVariable String id) {
    Patient patient = patientService.getPatient(id);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
        .header("Pragma", "no-cache")
        .header("Expires", "0")
        .body(patient);
}
```

### Common Patterns
```java
// Utility method for PHI responses
public static <T> ResponseEntity<T> phiResponse(T body) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
        .header("Pragma", "no-cache")
        .header("Expires", "0")
        .body(body);
}
```

---

## 3. Audit Logging Requirements

### ❌ NEVER DO THIS
```java
public Patient getPatient(String patientId) {
    return patientRepository.findById(patientId).orElseThrow();
    // No audit trail!
}
```

### ✅ ALWAYS DO THIS
```java
@Audited(eventType = "PHI_ACCESS")
public Patient getPatient(String patientId, String userId, String tenantId) {
    // Audit log automatically created
    return patientRepository.findById(patientId).orElseThrow();
}

// Or manual audit logging
public Patient getPatient(String patientId) {
    auditService.logAccess(
        userId,
        "PHI_ACCESS",
        "Patient",
        patientId,
        "VIEW"
    );
    return patientRepository.findById(patientId).orElseThrow();
}
```

### Required Audit Fields
- User ID (who accessed)
- Timestamp (when accessed)
- Resource type (Patient, Observation, etc.)
- Resource ID (which specific record)
- Action (VIEW, CREATE, UPDATE, DELETE)
- Tenant ID (multi-tenant context)

---

## 4. Multi-Tenant Isolation

### ❌ NEVER DO THIS
```java
@Query("SELECT p FROM Patient p WHERE p.id = :id")
Optional<Patient> findById(@Param("id") String id);
// Missing tenant filter - HIPAA violation!
```

### ✅ ALWAYS DO THIS
```java
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(
    @Param("id") String id,
    @Param("tenantId") String tenantId
);

// Service layer enforces tenant
public Patient getPatient(String patientId, String tenantId) {
    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
}
```

### Tenant Filter Enforcement
```java
// Use @PreAuthorize for tenant access validation
@PreAuthorize("@tenantSecurity.hasAccessToPatient(#patientId)")
public Patient getPatient(String patientId) { ... }
```

---

## 5. Data at Rest Encryption

### Database Configuration
```yaml
# PostgreSQL encryption
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_qm?ssl=true&sslmode=require
    hikari:
      connection-init-sql: SET application_name='hdim-service'
```

### Redis Encryption
```yaml
# Redis SSL/TLS
spring:
  redis:
    ssl: true
    host: ${REDIS_HOST}
    port: 6380
```

---

## 6. Data in Transit Encryption

### API Endpoints
```java
// Enforce HTTPS in production
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.requiresChannel()
            .anyRequest()
            .requiresSecure(); // Force HTTPS
        return http.build();
    }
}
```

### External API Calls
```java
// Use TLS for all external FHIR/EHR calls
RestTemplate restTemplate = new RestTemplate();
// Configured with SSL certificate validation
```

---

## 7. Data Retention & Deletion

### Required Capabilities
```java
// Implement hard delete for compliance
@Transactional
public void deletePatientData(String patientId, String tenantId) {
    // Delete all PHI for patient
    patientRepository.deleteByIdAndTenant(patientId, tenantId);
    observationRepository.deleteByPatientIdAndTenant(patientId, tenantId);
    conditionRepository.deleteByPatientIdAndTenant(patientId, tenantId);
    // ... delete all related PHI

    auditService.logDeletion(userId, "PATIENT_DATA_DELETION", patientId);
}
```

### Retention Policies
```sql
-- Example retention policy
DELETE FROM audit_logs
WHERE created_at < NOW() - INTERVAL '7 years'
AND tenant_id = 'TENANT001';
```

---

## 8. Logging Security

### ❌ NEVER LOG PHI
```java
// BAD - Logs contain PHI
log.info("Retrieved patient: {}", patient.toString());
log.debug("Patient SSN: {}", patient.getSsn());
```

### ✅ LOG ONLY IDENTIFIERS
```java
// GOOD - No PHI in logs
log.info("Retrieved patient with ID: {}", patient.getId());
log.debug("Patient access by user: {} for tenant: {}", userId, tenantId);
```

### Safe Logging Pattern
```java
// Use MDC for context, not PHI
MDC.put("userId", userId);
MDC.put("tenantId", tenantId);
MDC.put("patientId", patientId); // ID is OK, not PHI content
log.info("Patient data accessed");
MDC.clear();
```

---

## 9. Access Control (RBAC)

### Required Annotations
```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    @Audited(eventType = "PATIENT_ACCESS")
    public ResponseEntity<Patient> getPatient(
        @PathVariable String id,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        // Implementation
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_CREATE")
    public ResponseEntity<Patient> createPatient(...) {
        // Implementation
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(eventType = "PATIENT_DELETE")
    public ResponseEntity<Void> deletePatient(...) {
        // Implementation
    }
}
```

---

## 10. Testing PHI Compliance

### Required Tests
```java
@Test
void shouldNotCachePhiBeyondFiveMinutes() {
    // Test cache TTL
    Patient patient = patientService.getPatient("123", "tenant1");

    // Wait 5 minutes
    Thread.sleep(300000);

    // Verify cache miss (forces DB query)
    verify(patientRepository, times(2)).findByIdAndTenant(any(), any());
}

@Test
void shouldIncludeNoCacheHeadersForPhi() throws Exception {
    mockMvc.perform(get("/api/v1/patients/123"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("no-store")))
        .andExpect(header().string("Cache-Control", containsString("no-cache")))
        .andExpect(header().string("Pragma", "no-cache"));
}

@Test
void shouldEnforceTenantIsolation() {
    // User from tenant1 trying to access tenant2 data
    assertThrows(TenantAccessDeniedException.class, () -> {
        patientService.getPatient("123", "tenant2");
    });
}

@Test
void shouldCreateAuditLogForPhiAccess() {
    patientService.getPatient("123", "tenant1");

    verify(auditService).logAccess(
        eq(userId),
        eq("PHI_ACCESS"),
        eq("Patient"),
        eq("123"),
        eq("VIEW")
    );
}
```

---

## Pre-Commit Checklist

Before committing PHI-related code:

- [ ] Cache TTL ≤ 5 minutes for all PHI caches
- [ ] `Cache-Control: no-store, no-cache` headers on PHI endpoints
- [ ] `@Audited` annotation on all PHI access methods
- [ ] Multi-tenant filtering in ALL queries (`WHERE tenantId = ?`)
- [ ] `@PreAuthorize` on all API endpoints
- [ ] No PHI in log statements
- [ ] TLS/SSL for data in transit
- [ ] Encryption at rest configured
- [ ] Unit tests for cache TTL compliance
- [ ] Integration tests for tenant isolation
- [ ] Audit logging tests pass

---

## Code Review Questions

When reviewing PHI-related code, ask:

1. **Cache**: Is cache TTL ≤ 5 minutes?
2. **Headers**: Does the response include no-cache headers?
3. **Audit**: Is access logged for compliance?
4. **Isolation**: Does the query filter by tenantId?
5. **Auth**: Is there role-based access control?
6. **Logs**: Are logs free of PHI content?
7. **Encryption**: Is TLS/SSL enforced?
8. **Tests**: Do tests verify compliance?

---

## Emergency Response

If PHI exposure is suspected:

1. **Immediately notify security team**
2. **Review audit logs** for unauthorized access
3. **Check cache configurations** for TTL violations
4. **Verify encryption** is active on all channels
5. **Document incident** per HIPAA Breach Notification Rule
6. **Notify affected parties** if >500 records (within 60 days)

---

## Resources

- HIPAA Security Rule: https://www.hhs.gov/hipaa/for-professionals/security/
- Breach Notification Rule: https://www.hhs.gov/hipaa/for-professionals/breach-notification/
- Internal: `backend/HIPAA-CACHE-COMPLIANCE.md`
