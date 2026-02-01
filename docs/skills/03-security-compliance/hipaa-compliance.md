# HIPAA Compliance & PHI Protection

**Difficulty Level:** ⭐⭐⭐⭐⭐ (Expert - Non-Negotiable)
**Time Investment:** 1.5-2 weeks
**Prerequisite Skills:** Spring Boot basics, REST APIs, caching
**Related Skills:** Multi-tenant architecture, data protection, authentication
**Critical:** Every line of HDIM code must comply with HIPAA

---

## Overview

### What is This Skill?

**HIPAA (Health Insurance Portability and Accountability Act)** is a U.S. federal law that sets standards for protecting health information privacy and security. The **Privacy Rule** controls how PHI (Protected Health Information) is used/disclosed. The **Security Rule** establishes safeguards for electronic PHI.

**In HDIM Context:** All code handling patient data must comply with HIPAA Privacy & Security Rules or face:
- **$100-$50,000 per violation** fines
- **Criminal liability** (up to 10 years imprisonment)
- **Business termination** (loss of healthcare contracts)
- **Reputational damage** (patient trust destroyed)

**Non-negotiable requirement:** Every developer must understand and implement HIPAA requirements in every commit.

### Why is This Important for HDIM?

HIPAA is not optional—it's legally mandated. Every piece of code directly impacts patient privacy and organizational liability.

| Business Impact | HIPAA Requirement |
|---|---|
| **Patient Trust** | Protect all PHI, never breach |
| **Legal Compliance** | Follow Privacy & Security Rules exactly |
| **Audit Success** | Maintain complete audit trails |
| **Contract Requirements** | Healthcare customers mandate HIPAA |
| **Revenue Protection** | Non-compliance = contract termination |
| **Reputational** | Breaches destroy credibility |

**Practical Reality:** Healthcare organizations lose contracts, face fines, and cease operations over HIPAA violations. HDIM compliance is essential to survival.

### Business Impact

- ❌ **One violation** = $100+ fine + audit review
- ❌ **Repeated violations** = $50,000 per violation
- ❌ **Data breach** = Mandatory notification + investigation
- ✅ **Full compliance** = Audit pass + contract renewals
- ✅ **Audit trail** = Proof of compliance for regulators

### Key Services Affected

ALL 51 HDIM services handle PHI. Critical services:

- **fhir-service** (8085) - Patient clinical data
- **patient-event-service** (8084) - Patient demographics
- **quality-measure-service** (8087) - Clinical evaluation results
- **care-gap-service** (8086) - Patient care gaps
- **gateway-service** (8001-8004) - All data access points

**Key Principle:** If service touches patient data → Must comply with HIPAA

---

## Key Concepts

### Concept 1: PHI (Protected Health Information)

**Definition:** Any information in medical records or health plans that can identify an individual.

**PHI Examples (DO NOT log, cache, or store without protection):**

| Category | Examples | Risk Level |
|---|---|---|
| **Identifiers** | Name, SSN, MRN, email, phone, address | 🔴 Highest |
| **Diagnoses** | Type 2 Diabetes, Hypertension, HIV | 🔴 Highest |
| **Medical History** | Past surgeries, allergies, medications | 🔴 Highest |
| **Test Results** | HbA1c 7.2%, Blood pressure 120/80 | 🔴 Highest |
| **Treatment Plans** | Medication regimen, surgery schedule | 🔴 Highest |
| **Insurance Info** | Member ID, coverage dates | 🟠 High |
| **Payment History** | Claims paid, deductible met | 🟠 High |
| **Dates** | Birth date, admission date, discharge date | 🟠 High |

**Protected Endpoints (All PHI):**
```
GET  /api/v1/patients/{id}           → Patient demographics
GET  /api/v1/patients/{id}/fhir      → FHIR clinical data
POST /api/v1/measures/evaluate       → Evaluation results
GET  /api/v1/care-gaps               → Care gaps (identify patients)
GET  /api/v1/quality-measures        → Measure results (identify patients)
```

**Why it matters:**
- PHI mishandling = breach notification + fines
- Accidental logging of PHI = violation
- Improper caching of PHI = violation
- Cross-tenant PHI access = major violation

---

### Concept 2: Cache Control for PHI (CRITICAL)

**HIPAA Requirement:** PHI must not be cached in browser or intermediate caches.

**Why:** Patient data visible in browser cache = privacy violation if someone accesses patient's computer.

**Requirement:**
```
Cache-Control: no-store, no-cache, must-revalidate, private
Pragma: no-cache
Expires: 0
```

**What each means:**
- `no-store` - Don't store in any cache (browser, proxy, CDN)
- `no-cache` - Don't use cached copy without revalidating
- `must-revalidate` - Check cache validity before using
- `private` - Only browser cache (not shared proxies)
- `Pragma: no-cache` - Legacy browsers support
- `Expires: 0` - Immediately expired (legacy)

**Incorrect (HIPAA VIOLATION):**
```java
@GetMapping("/api/v1/patients/{id}")
public ResponseEntity<PatientResponse> getPatient(@PathVariable String id) {
    // ❌ WRONG: No cache control headers
    return ResponseEntity.ok(patientService.getPatient(id));
}
```

**Correct (HIPAA COMPLIANT):**
```java
@GetMapping("/api/v1/patients/{id}")
public ResponseEntity<PatientResponse> getPatient(
        @PathVariable String id,
        HttpServletResponse response) {

    // ✅ REQUIRED: Cache control headers
    response.setHeader("Cache-Control",
        "no-store, no-cache, must-revalidate, private");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");

    return ResponseEntity.ok(patientService.getPatient(id));
}
```

**Redis Cache TTL (HIPAA REQUIREMENT):**

```java
@Service
public class PatientService {
    @Cacheable(
        value = "patients",
        key = "#id",
        unless = "#result == null"
    )
    public Patient getPatient(String id) {
        return patientRepository.findById(id).orElse(null);
    }
}

// Cache configuration
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.create(factory);
    }
}

// CRITICAL: TTL Configuration
@Bean
public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return builder -> builder
        .withCacheConfiguration("patients",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // ✅ MAX 5 MINUTES FOR PHI
                .disableCachingNullValues()
        )
        .withCacheConfiguration("measurements",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // ✅ MAX 5 MINUTES
        );
}
```

**Why it matters:**
- Browser cache = unencrypted storage on disk
- 5-minute Redis TTL = limits exposure window
- Missing headers = HIPAA violation = audit failure

---

### Concept 3: Audit Logging (Compliance Requirement)

**HIPAA Requirement:** Log ALL PHI access with who, when, what.

**Must Log:**
```
Timestamp: 2024-01-20T10:30:00Z
User: dr_smith@healthcare.com (MD Role)
Action: ACCESSED
Resource: Patient #p-123 FHIR data
Result: SUCCESS
```

**Correct Implementation (HIPAA COMPLIANT):**

```java
@Service
@Slf4j
public class PatientService {
    private final AuditService auditService;

    @Audited(eventType = "PATIENT_VIEW")  // ✅ Required annotation
    @PreAuthorize("hasRole('CLINICIAN')")
    public PatientResponse getPatient(String patientId, String tenantId) {
        // Access PHI
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Patient", patientId));

        // Audit automatically logs this access via @Audited annotation
        return mapToResponse(patient);
    }
}

// Aspect that implements audit logging
@Aspect
@Component
@Slf4j
public class AuditAspect {
    @Around("@annotation(audited)")
    public Object auditMethodCall(ProceedingJoinPoint joinPoint,
            Audited audited) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        String eventType = audited.eventType();
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        try {
            Object result = joinPoint.proceed();

            // ✅ Log successful PHI access
            log.info("AUDIT: event_type={} user={} method={} result=SUCCESS",
                eventType, userId, methodName);

            // Publish to audit topic for persistent storage
            auditService.logAccess(AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .timestamp(Instant.now())
                .result("SUCCESS")
                .build());

            return result;

        } catch (Exception ex) {
            // ✅ Log failed attempts
            log.warn("AUDIT: event_type={} user={} method={} result=FAILED " +
                "error={}", eventType, userId, methodName, ex.getMessage());

            auditService.logAccess(AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .timestamp(Instant.now())
                .result("FAILED")
                .error(ex.getMessage())
                .build());

            throw ex;
        }
    }
}
```

**❌ WRONG - NO AUDIT LOGGING:**
```java
@GetMapping("/patients/{id}")
public ResponseEntity<PatientResponse> getPatient(@PathVariable String id) {
    // ❌ VIOLATION: No audit logging
    return ResponseEntity.ok(patientService.getPatient(id));
}
```

**Why it matters:**
- Audit trail = proof of compliance
- Missing logs = cannot prove who accessed PHI
- HIPAA requires 7+ year retention
- Regulators audit these logs

---

### Concept 4: Multi-Tenant Isolation (Prevents Cross-Tenant Leaks)

**HIPAA Requirement:** Prevent patient data from one tenant accessing another tenant's data.

**Risk:** Tenant A patient data accidentally visible to Tenant B = major breach.

**Correct Implementation:**

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // ✅ REQUIRED: Filter by tenantId
    @Query("SELECT p FROM Patient p " +
           "WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenantId(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId);

    // ❌ WRONG: No tenant filtering
    // Optional<Patient> findById(UUID id);  // VIOLATES HIPAA
}

@Service
public class PatientService {
    @PreAuthorize("hasRole('CLINICIAN')")
    public PatientResponse getPatient(
            String patientId,
            String tenantId) {  // ✅ Tenant from request header

        // ✅ REQUIRED: Use tenant-filtered query
        Patient patient = patientRepository
            .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Patient", patientId));

        return mapToResponse(patient);
    }
}

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {  // ✅ Get tenant

        // ✅ Pass tenant to service
        return ResponseEntity.ok(
            patientService.getPatient(id, tenantId));
    }
}
```

**Why it matters:**
- Every query must filter by tenantId
- Missing filter = cross-tenant data access
- = Major HIPAA violation
- = Potential data breach

---

### Concept 5: Encryption (Data at Rest & in Transit)

**HIPAA Requirements:**

1. **Data at Rest:** AES-256 encryption in database
2. **Data in Transit:** TLS 1.3+ for all connections
3. **Key Management:** Secure key storage (HashiCorp Vault)

**Encryption in Transit (TLS 1.3):**

```yaml
# application.yml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: hdim-cert
    protocol: TLSv1.3
    enabled-protocols: TLSv1.3
    ciphers: TLS_AES_256_GCM_SHA384  # Strong cipher suites only
```

**Encryption at Rest (Database):**

```java
@Entity
@Table(name = "patients")
@Data
public class Patient {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    // PHI fields encrypted in database
    @Convert(converter = EncryptedStringConverter.class)
    private String firstName;  // Stored encrypted in DB

    @Convert(converter = EncryptedStringConverter.class)
    private String lastName;

    @Convert(converter = EncryptedStringConverter.class)
    private String ssn;
}

// Encryption converter
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final Cipher cipher;

    public EncryptedStringConverter() {
        this.cipher = initializeCipher();
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        // Encrypt before storing
        return encryptAES256(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Decrypt when loading
        return decryptAES256(dbData);
    }

    private String encryptAES256(String data) {
        // Use key from Vault
        SecretKey key = loadKeyFromVault("hdim-encryption-key");
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decryptAES256(String encryptedData) {
        SecretKey key = loadKeyFromVault("hdim-encryption-key");
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }
}
```

**Why it matters:**
- Encrypted PHI harder to exploit if database breached
- TLS prevents man-in-the-middle attacks
- Vault stores keys securely
- Required for audit compliance

---

## Implementation Guide

### Step 1: Add Cache Control Headers to All PHI Endpoints

Every response with PHI must have cache control headers.

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PhiResponseFilter {

    @PostMapping("/patients")
    @PreAuthorize("hasRole('CLINICIAN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        // ✅ SET CACHE CONTROL BEFORE RETURNING
        response.setHeader("Cache-Control",
            "no-store, no-cache, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        PatientResponse result = patientService.createPatient(
            request, tenantId);

        return ResponseEntity
            .created(URI.create("/api/v1/patients/" + result.getId()))
            .body(result);
    }

    @GetMapping("/patients/{id}")
    @PreAuthorize("hasRole('CLINICIAN')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        // ✅ REQUIRED: Cache control headers
        response.setHeader("Cache-Control",
            "no-store, no-cache, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        return ResponseEntity.ok(patientService.getPatient(id, tenantId));
    }
}

// OR use a global filter
@Component
@RequiredArgsConstructor
public class PhiCacheControlFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Check if endpoint is PHI endpoint
        String uri = httpRequest.getRequestURI();
        if (isPHIEndpoint(uri)) {
            // ✅ Add cache control headers for all PHI endpoints
            httpResponse.setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, private");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }

        chain.doFilter(request, response);
    }

    private boolean isPHIEndpoint(String uri) {
        return uri.contains("/patients") ||
               uri.contains("/fhir") ||
               uri.contains("/measures") ||
               uri.contains("/care-gaps");
    }
}
```

---

### Step 2: Implement Audit Logging

Automatically log all PHI access.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String eventType();
    String[] roles() default {};
}

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;

    @Around("@annotation(com.healthdata.audit.Audited)")
    public Object auditAccess(ProceedingJoinPoint joinPoint,
            Audited audited) throws Throwable {

        // 1. Get context
        String methodName = joinPoint.getSignature().getName();
        String eventType = audited.eventType();
        Authentication auth = SecurityContextHolder.getContext()
            .getAuthentication();
        String userId = auth != null ? auth.getName() : "UNKNOWN";
        String tenantId = getTenantIdFromContext();
        Object[] args = joinPoint.getArgs();

        // 2. Execute method
        try {
            Object result = joinPoint.proceed();

            // 3. Log success
            auditService.logAccess(AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .tenantId(tenantId)
                .resourceId(extractResourceId(args, result))
                .action("ACCESS")
                .result("SUCCESS")
                .timestamp(Instant.now())
                .build());

            return result;

        } catch (Exception ex) {
            // 4. Log failure
            auditService.logAccess(AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .tenantId(tenantId)
                .action("ACCESS")
                .result("FAILED")
                .error(ex.getMessage())
                .timestamp(Instant.now())
                .build());

            throw ex;
        }
    }

    private String getTenantIdFromContext() {
        // From request header
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest()
                .getHeader("X-Tenant-ID");
        }
        return null;
    }

    private String extractResourceId(Object[] args, Object result) {
        // Extract ID from request or response
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return null;
    }
}

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository repository;
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public void logAccess(AuditEvent event) {
        // 1. Store in database
        AuditLog log = new AuditLog();
        log.setEventType(event.getEventType());
        log.setUserId(event.getUserId());
        log.setTenantId(event.getTenantId());
        log.setResourceId(event.getResourceId());
        log.setResult(event.getResult());
        log.setTimestamp(event.getTimestamp());

        repository.save(log);

        // 2. Publish to Kafka (immutable audit trail)
        kafkaTemplate.send("audit.events",
            event.getTenantId(), event);

        log.info("HIPAA Audit: eventType={} user={} tenant={} result={}",
            event.getEventType(), event.getUserId(),
            event.getTenantId(), event.getResult());
    }
}

@Entity
@Table(name = "audit_log")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String tenantId;

    private String resourceId;

    @Column(nullable = false)
    private String result;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Index(columnList = "tenant_id, user_id, timestamp")
}
```

---

### Step 3: Enforce Multi-Tenant Filtering

Every query must include tenant_id filter.

```java
@Repository
public interface PatientRepository
    extends JpaRepository<Patient, UUID> {

    // ✅ CORRECT: Tenant filtering
    @Query("SELECT p FROM Patient p " +
           "WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenant(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId);

    // ✅ CORRECT: Tenant + status filtering
    @Query("SELECT p FROM Patient p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.status = :status " +
           "ORDER BY p.createdAt DESC")
    Page<Patient> findByTenantAndStatus(
        @Param("tenantId") String tenantId,
        @Param("status") String status,
        Pageable pageable);

    // ❌ WRONG: No tenant filtering (HIPAA VIOLATION)
    // Optional<Patient> findById(UUID id);
}

@Service
public class PatientService {
    private final PatientRepository repository;

    @Audited(eventType = "PATIENT_VIEW")
    @PreAuthorize("hasRole('CLINICIAN')")
    public PatientResponse getPatient(String patientId, String tenantId) {
        // ✅ REQUIRED: Pass tenant to repository
        Patient patient = repository
            .findByIdAndTenant(
                UUID.fromString(patientId),
                tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Patient", patientId));

        return mapToResponse(patient);
    }
}
```

---

### Step 4: Configure Redis Cache TTL

Limit PHI caching to 5 minutes maximum.

```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory) {

        return RedisCacheManager.create(
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // ✅ MAX 5 MIN FOR PHI
                .disableCachingNullValues(),
            connectionFactory
        );
    }

    @Bean
    public CacheManagerCustomizer<RedisCacheManager> customize() {
        return cacheManager -> cacheManager
            .registerCustomCaches(
                Map.of(
                    "patients",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5)),
                    "measurements",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5)),
                    "reference_data",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                )
            );
    }
}
```

---

## Real-World HIPAA Violations (What NOT to Do)

### Violation 1: Logging PHI

```java
// ❌ HIPAA VIOLATION: Logging patient data
log.info("Processing patient: {} with HbA1c: {}",
         patient.getName(), patient.getHbA1c());  // Patient name + result in log!

// ✅ CORRECT: Log only what's necessary
log.info("Processing patient {} with result: {}",
         patientId, "success");  // Don't log actual values
```

### Violation 2: Caching Without TTL

```java
// ❌ VIOLATION: Cached forever (or very long)
@Cacheable("patients")  // No TTL specified!
public Patient getPatient(String id) {
    return patientRepository.findById(id);
}

// ✅ CORRECT: 5-minute TTL
@Cacheable(value = "patients", unless = "#result == null")
@CachePut(value = "patients", key = "#result.id",
          condition = "#result != null")
public Patient getPatient(String id) {
    return patientRepository.findById(id);
}
// With 5-minute TTL configured in CacheConfig
```

### Violation 3: Cross-Tenant Access

```java
// ❌ VIOLATION: No tenant filtering
@GetMapping("/patients/{id}")
public PatientResponse getPatient(@PathVariable String id) {
    // Any tenant can access any patient!
    return patientService.getPatient(id);
}

// ✅ CORRECT: Tenant-filtered access
@GetMapping("/patients/{id}")
public PatientResponse getPatient(
        @PathVariable String id,
        @RequestHeader("X-Tenant-ID") String tenantId) {
    // Only that tenant can access their patient
    return patientService.getPatient(id, tenantId);
}
```

### Violation 4: Missing Audit Logging

```java
// ❌ VIOLATION: No audit trail
@PreAuthorize("hasRole('CLINICIAN')")
@GetMapping("/patients/{id}")
public PatientResponse getPatient(@PathVariable String id) {
    // Who accessed what? Unknown!
    return patientService.getPatient(id);
}

// ✅ CORRECT: Audit every access
@PreAuthorize("hasRole('CLINICIAN')")
@Audited(eventType = "PATIENT_ACCESS")
@GetMapping("/patients/{id}")
public PatientResponse getPatient(@PathVariable String id) {
    // Audited: who accessed, when, what result
    return patientService.getPatient(id);
}
```

---

## Best Practices

### DO's ✅

- ✅ **Cache control headers on EVERY PHI endpoint** - No exceptions
- ✅ **Audit EVERY PHI access** - WHO, WHEN, WHAT
- ✅ **Filter by tenantId in EVERY query** - No cross-tenant access
- ✅ **5-minute Redis TTL MAX** - Limit exposure window
- ✅ **Encrypt data at rest** - AES-256 minimum
- ✅ **Use TLS 1.3+** - All connections encrypted
- ✅ **Encrypt keys in Vault** - Not hardcoded
- ✅ **Log failures** - Failed access attempts matter
- ✅ **Retain audit logs 7+ years** - Regulatory requirement
- ✅ **Test multi-tenant isolation** - Verify no leaks

### DON'Ts ❌

- ❌ **Log PHI to stdout/file** - Will be discovered in audit
- ❌ **Cache without TTL** - Data persists indefinitely
- ❌ **Query without tenant filter** - Cross-tenant leaks
- ❌ **Return HTTP 404 for missing tenants** - Reveals data exists
- ❌ **Store keys in config files** - Use Vault
- ❌ **Use HTTP instead of HTTPS** - Unencrypted transmission
- ❌ **Disable authentication** - No access control
- ❌ **Store unencrypted backups** - Restore-time breach
- ❌ **Share credentials** - Can't audit who accessed
- ❌ **Skip audit logging** - No compliance proof

---

## Testing Strategies

### Unit Test: Cache Control Headers

```java
@ExtendWith(MockitoExtension.class)
class PhiCacheControlTest {
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private PatientController controller;

    @Test
    void shouldSetCacheControlHeaders_ForPHIEndpoint() {
        // ARRANGE
        String patientId = UUID.randomUUID().toString();
        ArgumentCaptor<String> headerCaptor =
            ArgumentCaptor.forClass(String.class);

        // ACT
        controller.getPatient(patientId, "tenant-001", response);

        // ASSERT
        verify(response, atLeast(3)).setHeader(anyString(), anyString());

        verify(response).setHeader("Cache-Control",
            "no-store, no-cache, must-revalidate, private");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
    }
}
```

### Integration Test: Audit Logging

```java
@SpringBootTest
class AuditLoggingIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    @WithMockUser(username = "dr_smith", roles = "CLINICIAN")
    void shouldAuditPhiAccess() throws Exception {
        // ARRANGE
        auditRepository.deleteAll();

        // ACT
        mockMvc.perform(get("/api/v1/patients/p-123")
            .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk());

        // ASSERT
        List<AuditLog> logs = auditRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEventType())
            .isEqualTo("PATIENT_ACCESS");
        assertThat(logs.get(0).getUserId())
            .isEqualTo("dr_smith");
        assertThat(logs.get(0).getResult())
            .isEqualTo("SUCCESS");
    }
}
```

### Integration Test: Multi-Tenant Isolation

```java
@Test
void shouldIsolateTenants() throws Exception {
    // Create patient in tenant-001
    mockMvc.perform(post("/api/v1/patients")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Tenant-ID", "tenant-001")
        .content(jsonPatient("John Doe")))
        .andExpect(status().isCreated());

    // Try to access from tenant-002 (should fail)
    mockMvc.perform(get("/api/v1/patients/p-123")
        .header("X-Tenant-ID", "tenant-002"))
        .andExpect(status().isNotFound());

    // Verify patient accessible only in tenant-001
    mockMvc.perform(get("/api/v1/patients/p-123")
        .header("X-Tenant-ID", "tenant-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("John"));
}
```

### Testing Checklist

- [ ] Cache control headers present
- [ ] All required cache headers set correctly
- [ ] PHI never logged to stdout/file
- [ ] Audit events created for every PHI access
- [ ] Failed access attempts logged
- [ ] Tenant filtering prevents cross-tenant access
- [ ] Redis TTL set to 5 minutes max
- [ ] Encryption enabled for sensitive fields
- [ ] TLS 1.3 configured
- [ ] Vault keys not hardcoded
- [ ] Multi-tenant isolation verified

---

## Troubleshooting

### Issue 1: Audit Test Failures

**Symptoms:**
- Audit logs not created
- @Audited annotation seems ignored

**Root cause:**
- Aspect not initialized
- Method not called through Spring proxy
- Transaction issues

**Solution:**
```java
// Verify @EnableAspectJAutoProxy is enabled
@SpringBootApplication
@EnableAspectJAutoProxy  // ✅ REQUIRED
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// Verify method called through proxy (not self)
@Service
public class PatientService {
    @Audited(eventType = "PATIENT_VIEW")
    public Patient getPatient(String id) {
        return patientRepository.findById(id);
    }

    public void someMethod() {
        // ❌ WRONG: Self-call, audit not triggered
        // getPatient("p-123");

        // ✅ CORRECT: Let Spring inject
    }
}
```

---

### Issue 2: Performance Impact from Audit Logging

**Symptoms:**
- API latency increased significantly
- Queries taking 2x-3x longer

**Root cause:**
- Synchronous audit logging (waiting for database)
- Too much detail in audit events

**Solution:**
```java
@Aspect
@Component
@Slf4j
public class AsyncAuditAspect {
    @Async  // ✅ Async audit logging
    public void logAccessAsync(AuditEvent event) {
        auditService.logAccess(event);
    }

    @Around("@annotation(com.healthdata.audit.Audited)")
    public Object auditAsynchronously(ProceedingJoinPoint joinPoint)
            throws Throwable {

        // Execute method synchronously
        Object result = joinPoint.proceed();

        // Log asynchronously
        String eventType = extractEventType(joinPoint);
        logAccessAsync(AuditEvent.builder()
            .eventType(eventType)
            .timestamp(Instant.now())
            .build());

        return result;
    }
}
```

---

## HIPAA Compliance Checklist

**Must Complete Before Every Commit:**

### Cache Control
- [ ] All PHI endpoints have Cache-Control headers
- [ ] Headers include: no-store, no-cache, must-revalidate, private
- [ ] Pragma: no-cache set
- [ ] Expires: 0 set

### Audit Logging
- [ ] @Audited annotation on all PHI access methods
- [ ] Audit logs include: user, time, action, result
- [ ] Failed attempts logged
- [ ] Log messages don't contain PHI

### Multi-Tenant Isolation
- [ ] All queries filter by tenantId
- [ ] No queries without tenant filter
- [ ] Tenant ID from request header
- [ ] Cross-tenant access prevented

### Encryption
- [ ] Data in transit: TLS 1.3+
- [ ] Data at rest: AES-256
- [ ] Encryption keys in Vault
- [ ] No keys hardcoded

### Security
- [ ] @PreAuthorize on PHI endpoints
- [ ] Role-based access control
- [ ] Authentication required
- [ ] Failed attempts logged

---

## Key Takeaways

1. **Core Concept:** HIPAA = legal requirement, not optional
2. **Implementation:** Cache control + audit + multi-tenant + encryption
3. **Common Pitfall:** Logging PHI or missing cache headers
4. **Why It Matters:** One violation = $100+ fine + audit failure

---

## References

### HDIM Documentation

- [Multi-Tenant Architecture](../01-architecture/multi-tenant-architecture.md)
- [CQRS + Event Sourcing](../01-architecture/cqrs-event-sourcing.md)

### External Resources

- **HHS HIPAA Guidance:** www.hhs.gov/hipaa
- **OCR HIPAA Audit Protocol:** www.hhs.gov/hipaa/for-professionals/audit-protocol
- **HIPAA Violation Examples:** www.hhs.gov/ocr/privacy/hipaa/enforcement
- **Breach Notification Rule:** www.hhs.gov/hipaa/for-professionals/breach-notification

---

**Last Updated:** January 20, 2026
**Version:** 1.0 - Foundation Release
**Status:** ✅ Complete
**Critical:** This guide is mandatory for all HDIM developers

**← Previous: [HEDIS Quality Measures](../02-healthcare-domain/hedis-quality-measures.md)** | **Next: [Multi-Tenant Architecture](../01-architecture/multi-tenant-architecture.md) →**
