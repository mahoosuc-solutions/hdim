# Consent Service

HIPAA-compliant patient consent management for data sharing, research, and treatment authorization.

## Purpose

Manages patient consent records and validates data access permissions, addressing the challenge that:
- HIPAA requires granular consent tracking for PHI data sharing
- Healthcare organizations must validate authorization before data access
- Consent can expire, be revoked, or have limited scope (purpose, category, data class)
- All consent changes require audit trails (7-year retention)

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Consent Service                              │
│                         (Port 8082)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── ConsentController (25+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── ConsentService                                             │
│  │   ├── CRUD operations          - Create, update, delete      │
│  │   ├── Lifecycle management     - Revoke, expire checking     │
│  │   ├── Authorization validation - Scope, category, data class │
│  │   └── Query optimization       - Active, expired, expiring   │
│  └── AuditService (HIPAA logging)                               │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── ConsentRepository (JPA + custom queries)                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── ConsentEntity                                              │
│      ├── Scope           - treatment, research, operations       │
│      ├── Category        - clinical-notes, lab-results, etc.    │
│      ├── Data Class      - demographics, medications, etc.      │
│      ├── Status          - ACTIVE, REVOKED, EXPIRED             │
│      └── Audit Fields    - createdBy, revokedBy, timestamps     │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Consent Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/consents` | Create consent |
| PUT | `/api/consents/{id}` | Update consent |
| DELETE | `/api/consents/{id}` | Delete consent |
| POST | `/api/consents/{id}/revoke` | Revoke consent |
| GET | `/api/consents/{id}` | Get consent by ID |

### Patient Queries
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}` | All consents for patient |
| GET | `/api/consents/patient/{patientId}/active` | Active consents |
| GET | `/api/consents/patient/{patientId}/revoked` | Revoked consents |
| GET | `/api/consents/patient/{patientId}/expired` | Expired consents |
| GET | `/api/consents/patient/{patientId}/expiring-soon` | Expiring in N days |

### Consent Validation
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}/check/scope/{scope}` | Check scope authorization |
| GET | `/api/consents/patient/{patientId}/check/category/{category}` | Check category authorization |
| GET | `/api/consents/patient/{patientId}/check/data-class/{dataClass}` | Check data class authorization |
| POST | `/api/consents/validate-access` | Validate data access request |

### Filtering
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}/active/scope/{scope}` | Active by scope |
| GET | `/api/consents/patient/{patientId}/active/category/{category}` | Active by category |

## Configuration

```yaml
server:
  port: 8082
  servlet:
    context-path: /consent

# HIPAA audit retention
audit:
  enabled: true
  retention-days: 2555  # 7 years for HIPAA compliance

# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_consent
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

## Consent Entity Structure

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "uuid",
  "scope": "treatment",  // treatment, research, payment, operations
  "category": "clinical-notes",  // clinical-notes, lab-results, imaging
  "dataClass": "medications",  // demographics, conditions, medications
  "authorizedPartyId": "provider-123",
  "status": "ACTIVE",  // ACTIVE, REVOKED, EXPIRED
  "startDate": "2024-01-01",
  "expirationDate": "2025-01-01",
  "revocationDate": null,
  "revocationReason": null,
  "createdBy": "user-123",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation
- **Database**: PostgreSQL with Liquibase migrations
- **Audit**: HIPAA-compliant audit service (7-year retention)
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:consent-service:bootRun

# Or via Docker
docker compose --profile consent up consent-service
```

## Testing

### Overview

The Consent Service has a comprehensive test suite covering consent CRUD operations, lifecycle management (revocation, expiration), granular authorization validation (scope, category, data class, authorized party), multi-tenant isolation, RBAC enforcement, and HIPAA compliance including 42 CFR Part 2 substance abuse data restrictions.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:consent-service:test

# Run unit tests only
./gradlew :modules:services:consent-service:test --tests "*ServiceTest"

# Run controller tests
./gradlew :modules:services:consent-service:test --tests "*ControllerTest"

# Run with coverage report
./gradlew :modules:services:consent-service:test jacocoTestReport

# Run specific test class
./gradlew :modules:services:consent-service:test --tests "ConsentServiceTest"

# Run specific nested test class
./gradlew :modules:services:consent-service:test --tests "ConsentServiceTest\$ValidateDataAccessTests"
```

### Test Organization

```
src/test/java/com/healthdata/consent/
├── service/
│   └── ConsentServiceTest.java          # 806 lines - Unit tests (10+ nested classes)
├── api/
│   └── ConsentControllerTest.java       # 723 lines - Controller tests (18 nested classes)
├── multitenant/
│   └── ConsentMultiTenantTest.java      # Multi-tenant isolation tests
├── security/
│   └── ConsentRbacTest.java             # RBAC permission tests
├── compliance/
│   └── ConsentHipaaComplianceTest.java  # HIPAA compliance tests
└── performance/
    └── ConsentPerformanceTest.java      # Performance benchmarks
```

### Unit Tests - ConsentServiceTest.java

The ConsentServiceTest demonstrates comprehensive service layer testing with 10+ nested test classes covering all consent operations.

#### Consent Creation Tests

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Consent Service Tests")
class ConsentServiceTest {

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConsentService consentService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String PATIENT_ID = "patient-test-001";
    private static final String CREATED_BY = "user-test-001";

    @Nested
    @DisplayName("Create Consent Tests")
    class CreateConsentTests {

        @Test
        @DisplayName("Should create consent with all required fields")
        void shouldCreateConsentWithAllRequiredFields() {
            // Given
            ConsentDto request = ConsentDto.builder()
                .patientId(PATIENT_ID)
                .scope("treatment")
                .category("clinical-notes")
                .dataClass("medications")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .build();

            when(consentRepository.save(any(ConsentEntity.class)))
                .thenAnswer(inv -> {
                    ConsentEntity entity = inv.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.createConsent(TENANT_ID, request, CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(result.getScope()).isEqualTo("treatment");
            assertThat(result.getStatus()).isEqualTo("active");
            assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);

            verify(kafkaTemplate).send(eq("consent.created"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should set default status to active")
        void shouldSetDefaultStatusToActive() {
            // Given
            ConsentDto request = createValidConsentRequest();
            when(consentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.createConsent(TENANT_ID, request, CREATED_BY);

            // Then
            assertThat(result.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should handle Kafka publish failure gracefully")
        void shouldHandleKafkaPublishFailureGracefully() {
            // Given - Kafka fails but consent should still be created
            ConsentDto request = createValidConsentRequest();
            when(consentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Kafka connection failed")));

            // When - Should not throw
            ConsentEntity result = consentService.createConsent(TENANT_ID, request, CREATED_BY);

            // Then - Consent created despite Kafka failure
            assertThat(result).isNotNull();
            assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        }
    }
}
```

#### Consent Revocation Tests

```java
@Nested
@DisplayName("Revoke Consent Tests")
class RevokeConsentTests {

    @Test
    @DisplayName("Should revoke consent successfully with reason")
    void shouldRevokeConsentSuccessfully() {
        // Given
        UUID consentId = UUID.randomUUID();
        ConsentEntity existing = createActiveConsent();

        when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
            .thenReturn(Optional.of(existing));
        when(consentRepository.save(any(ConsentEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When
        ConsentEntity result = consentService.revokeConsent(
            TENANT_ID, consentId, "Patient request", CREATED_BY);

        // Then
        assertThat(result.getStatus()).isEqualTo("revoked");
        assertThat(result.getRevocationReason()).isEqualTo("Patient request");
        assertThat(result.getRevokedBy()).isEqualTo(CREATED_BY);
        assertThat(result.getRevocationDate()).isEqualTo(LocalDate.now());

        verify(kafkaTemplate).send(eq("consent.revoked"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw when consent not found")
    void shouldThrowWhenConsentNotFound() {
        // Given
        UUID consentId = UUID.randomUUID();
        when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> consentService.revokeConsent(
                TENANT_ID, consentId, "Reason", CREATED_BY))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Consent not found");
    }
}
```

#### Consent Validation Tests (HIPAA 42 CFR Part 2 Critical)

```java
@Nested
@DisplayName("Validate Data Access Tests")
class ValidateDataAccessTests {

    @Test
    @DisplayName("Should permit access when matching consent exists")
    void shouldPermitAccessWhenMatchingConsentExists() {
        // Given
        ConsentEntity consent = createActiveConsent();
        consent.setProvisionType("permit");
        consent.setScope("read");
        consent.setCategory("treatment");
        consent.setDataClass("general");

        when(consentRepository.findActiveConsentsByPatient(
                eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
            .thenReturn(List.of(consent));

        // When
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
            TENANT_ID, PATIENT_ID, "read", "treatment", "general", null);

        // Then
        assertThat(result.isPermitted()).isTrue();
        assertThat(result.getReason()).contains("Active consent found");
    }

    @Test
    @DisplayName("Should deny access when scope does not match")
    void shouldDenyAccessWhenScopeMismatch() {
        // Given - Consent for "read", request for "write"
        ConsentEntity consent = createActiveConsent();
        consent.setScope("read");

        when(consentRepository.findActiveConsentsByPatient(
                eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
            .thenReturn(List.of(consent));

        // When
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
            TENANT_ID, PATIENT_ID, "write", "treatment", null, null);

        // Then
        assertThat(result.isPermitted()).isFalse();
        assertThat(result.getReason()).contains("No matching consent");
    }

    @Test
    @DisplayName("Should deny access when data class is substance-abuse without explicit consent")
    void shouldDenySubstanceAbuseDataWithoutExplicitConsent() {
        // Given - HIPAA 42 CFR Part 2 requires explicit consent for substance abuse data
        ConsentEntity consent = createActiveConsent();
        consent.setDataClass("general");  // Not substance-abuse

        when(consentRepository.findActiveConsentsByPatient(
                eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
            .thenReturn(List.of(consent));

        // When - Request for substance-abuse data
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
            TENANT_ID, PATIENT_ID, "read", "treatment", "substance-abuse", null);

        // Then - Denied due to 42 CFR Part 2 requirements
        assertThat(result.isPermitted()).isFalse();
        assertThat(result.getReason()).contains("No matching consent");
    }

    @Test
    @DisplayName("Should permit substance-abuse data access with explicit consent")
    void shouldPermitSubstanceAbuseDataWithExplicitConsent() {
        // Given - Explicit consent for substance-abuse data class
        ConsentEntity consent = createActiveConsent();
        consent.setScope("read");
        consent.setCategory("treatment");
        consent.setDataClass("substance-abuse");
        consent.setProvisionType("permit");

        when(consentRepository.findActiveConsentsByPatient(
                eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
            .thenReturn(List.of(consent));

        // When
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
            TENANT_ID, PATIENT_ID, "read", "treatment", "substance-abuse", null);

        // Then
        assertThat(result.isPermitted()).isTrue();
    }

    @Test
    @DisplayName("Should check authorized party when provided")
    void shouldCheckAuthorizedParty() {
        // Given - Consent for specific provider
        ConsentEntity consent = createActiveConsent();
        consent.setAuthorizedPartyId("provider-123");

        when(consentRepository.findActiveConsentsByPatient(
                eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
            .thenReturn(List.of(consent));

        // When - Request from different provider
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
            TENANT_ID, PATIENT_ID, "read", "treatment", null, "provider-456");

        // Then
        assertThat(result.isPermitted()).isFalse();
        assertThat(result.getReason()).contains("No matching consent");
    }
}
```

#### Consent Lifecycle Tests

```java
@Nested
@DisplayName("Get Expiring Consents Tests")
class GetExpiringConsentsTests {

    @Test
    @DisplayName("Should find consents expiring within specified days")
    void shouldFindConsentsExpiringSoon() {
        // Given
        LocalDate today = LocalDate.now();
        int daysUntilExpiry = 30;

        ConsentEntity expiringSoon = createActiveConsent();
        expiringSoon.setValidTo(today.plusDays(15));

        when(consentRepository.findByTenantIdAndPatientIdAndValidToBetween(
                eq(TENANT_ID), eq(PATIENT_ID), eq(today), eq(today.plusDays(daysUntilExpiry))))
            .thenReturn(List.of(expiringSoon));

        // When
        List<ConsentEntity> result = consentService.getConsentsExpiringSoon(
            TENANT_ID, PATIENT_ID, daysUntilExpiry);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValidTo()).isBefore(today.plusDays(daysUntilExpiry));
    }
}

@Nested
@DisplayName("Process Expired Consents Tests")
class ProcessExpiredConsentsTests {

    @Test
    @DisplayName("Should process and expire all outdated consents")
    void shouldProcessExpiredConsents() {
        // Given
        ConsentEntity expired1 = createActiveConsent();
        expired1.setValidTo(LocalDate.now().minusDays(1));
        ConsentEntity expired2 = createActiveConsent();
        expired2.setValidTo(LocalDate.now().minusDays(5));

        when(consentRepository.findByStatusAndValidToBefore(eq("active"), any(LocalDate.class)))
            .thenReturn(List.of(expired1, expired2));
        when(consentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When
        int processedCount = consentService.processExpiredConsents();

        // Then
        assertThat(processedCount).isEqualTo(2);
        verify(consentRepository).saveAll(argThat(list ->
            ((List<ConsentEntity>) list).stream()
                .allMatch(c -> c.getStatus().equals("expired"))));
        verify(kafkaTemplate, times(2)).send(eq("consent.expired"), anyString(), anyString());
    }
}
```

### Unit Tests - ConsentControllerTest.java

The ConsentControllerTest has 18 nested test classes covering all 25+ REST endpoints.

#### Controller Test Setup

```java
@WebMvcTest(ConsentController.class)
@DisplayName("Consent Controller Tests")
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsentService consentService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String PATIENT_ID = "patient-test-001";

    @Nested
    @DisplayName("POST /api/consents Tests")
    class CreateConsentTests {

        @Test
        @DisplayName("Should create consent - returns 201")
        void shouldCreateConsent() throws Exception {
            // Given
            ConsentDto request = ConsentDto.builder()
                .patientId(PATIENT_ID)
                .scope("treatment")
                .category("clinical-notes")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .build();

            ConsentEntity saved = createConsentEntity();
            when(consentService.createConsent(eq(TENANT_ID), any(ConsentDto.class), anyString()))
                .thenReturn(saved);

            // When/Then
            mockMvc.perform(post("/api/consents")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.scope").value("treatment"));
        }

        @Test
        @DisplayName("Should reject invalid consent - returns 400")
        void shouldRejectInvalidConsent() throws Exception {
            // Given - Missing required fields
            ConsentDto request = ConsentDto.builder()
                .scope("treatment")  // Missing patientId
                .build();

            // When/Then
            mockMvc.perform(post("/api/consents")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}
```

#### Consent Revocation Endpoint Tests

```java
@Nested
@DisplayName("POST /api/consents/{id}/revoke Tests")
class RevokeConsentTests {

    @Test
    @DisplayName("Should revoke consent with reason")
    void shouldRevokeConsentWithReason() throws Exception {
        // Given
        UUID consentId = UUID.randomUUID();
        ConsentEntity revoked = createConsentEntity();
        revoked.setStatus("revoked");
        revoked.setRevocationReason("Patient request");

        when(consentService.revokeConsent(
                eq(TENANT_ID), eq(consentId), eq("Patient request"), anyString()))
            .thenReturn(revoked);

        // When/Then
        mockMvc.perform(post("/api/consents/{id}/revoke", consentId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .param("reason", "Patient request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("revoked"))
            .andExpect(jsonPath("$.revocationReason").value("Patient request"));
    }
}
```

#### Consent Validation Endpoint Tests

```java
@Nested
@DisplayName("POST /api/consents/validate-access Tests")
class ValidateDataAccessTests {

    @Test
    @DisplayName("Should validate data access - permitted")
    void shouldValidateDataAccessPermitted() throws Exception {
        // Given
        ConsentController.DataAccessRequest request = new ConsentController.DataAccessRequest();
        request.setPatientId(PATIENT_ID);
        request.setScope("read");
        request.setCategory("treatment");
        request.setDataClass("general");
        request.setAuthorizedPartyId("org-123");

        ConsentService.ConsentValidationResult result =
            ConsentService.ConsentValidationResult.permitted("Access permitted by active consent");

        when(consentService.validateDataAccess(
                eq(TENANT_ID), eq(PATIENT_ID), eq("read"), eq("treatment"),
                eq("general"), eq("org-123")))
            .thenReturn(result);

        // When/Then
        mockMvc.perform(post("/api/consents/validate-access")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "EVALUATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permitted").value(true))
            .andExpect(jsonPath("$.reason").value("Access permitted by active consent"));
    }

    @Test
    @DisplayName("Should validate data access - denied for substance-abuse")
    void shouldValidateDataAccessDenied() throws Exception {
        // Given - Request for substance-abuse data without consent
        ConsentController.DataAccessRequest request = new ConsentController.DataAccessRequest();
        request.setPatientId(PATIENT_ID);
        request.setScope("read");
        request.setCategory("treatment");
        request.setDataClass("substance-abuse");

        ConsentService.ConsentValidationResult result =
            ConsentService.ConsentValidationResult.denied(
                "42 CFR Part 2: No consent for substance abuse data");

        when(consentService.validateDataAccess(
                eq(TENANT_ID), eq(PATIENT_ID), eq("read"), eq("treatment"),
                eq("substance-abuse"), isNull()))
            .thenReturn(result);

        // When/Then
        mockMvc.perform(post("/api/consents/validate-access")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "EVALUATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permitted").value(false))
            .andExpect(jsonPath("$.reason").value(containsString("42 CFR Part 2")));
    }
}
```

#### Consent Check Endpoint Tests

```java
@Nested
@DisplayName("GET /api/consents/patient/{patientId}/check/* Tests")
class ConsentCheckTests {

    @Test
    @DisplayName("Should check consent for scope")
    void shouldCheckConsentForScope() throws Exception {
        // Given
        when(consentService.hasActiveConsentForScope(TENANT_ID, PATIENT_ID, "treatment"))
            .thenReturn(true);

        // When/Then
        mockMvc.perform(get("/api/consents/patient/{patientId}/check/scope/{scope}",
                    PATIENT_ID, "treatment")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasConsent").value(true));
    }

    @Test
    @DisplayName("Should check consent for data class")
    void shouldCheckConsentForDataClass() throws Exception {
        // Given - No consent for mental-health data
        when(consentService.hasActiveConsentForDataClass(TENANT_ID, PATIENT_ID, "mental-health"))
            .thenReturn(false);

        // When/Then
        mockMvc.perform(get("/api/consents/patient/{patientId}/check/data-class/{dataClass}",
                    PATIENT_ID, "mental-health")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasConsent").value(false));
    }
}
```

### Integration Tests

Integration tests use TestContainers for real PostgreSQL database and verify full request/response cycles.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class ConsentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        BaseTestContainersConfiguration.configurePostgres(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-integration-001";

    @BeforeEach
    void setUp() {
        consentRepository.deleteAll();
    }

    @Test
    @DisplayName("Full consent lifecycle: create, validate, revoke")
    void shouldCompleteConsentLifecycle() throws Exception {
        // Step 1: Create consent
        ConsentDto createRequest = ConsentDto.builder()
            .patientId("patient-001")
            .scope("treatment")
            .category("clinical-notes")
            .dataClass("general")
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now().plusYears(1))
            .consentDate(LocalDate.now())
            .build();

        String createResponse = mockMvc.perform(post("/api/consents")
                .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("active"))
            .andReturn().getResponse().getContentAsString();

        String consentId = JsonPath.read(createResponse, "$.id");

        // Step 2: Validate data access - should be permitted
        ConsentController.DataAccessRequest validateRequest = new ConsentController.DataAccessRequest();
        validateRequest.setPatientId("patient-001");
        validateRequest.setScope("treatment");
        validateRequest.setCategory("clinical-notes");

        mockMvc.perform(post("/api/consents/validate-access")
                .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permitted").value(true));

        // Step 3: Revoke consent
        mockMvc.perform(post("/api/consents/{id}/revoke", consentId)
                .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                .param("reason", "Patient requested revocation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("revoked"));

        // Step 4: Validate again - should be denied
        mockMvc.perform(post("/api/consents/validate-access")
                .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permitted").value(false));
    }
}
```

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class ConsentMultiTenantTest {

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        consentRepository.deleteAll();
    }

    @Nested
    @DisplayName("Consent Data Isolation Tests")
    class ConsentDataIsolationTests {

        @Test
        @DisplayName("Consents should be isolated by tenant")
        void consentsShouldBeIsolatedByTenant() {
            // Given
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            String patientId = "patient-shared-mrn";  // Same patient ID in both tenants

            ConsentEntity consent1 = createAndSaveConsent(tenant1, patientId, "treatment");
            ConsentEntity consent2 = createAndSaveConsent(tenant2, patientId, "research");
            ConsentEntity consent3 = createAndSaveConsent(tenant1, patientId, "payment");

            // When
            List<ConsentEntity> tenant1Consents = consentRepository.findByTenantIdAndPatientId(
                tenant1, patientId);
            List<ConsentEntity> tenant2Consents = consentRepository.findByTenantIdAndPatientId(
                tenant2, patientId);

            // Then
            assertThat(tenant1Consents)
                .hasSize(2)
                .extracting(ConsentEntity::getTenantId)
                .containsOnly(tenant1);

            assertThat(tenant2Consents)
                .hasSize(1)
                .extracting(ConsentEntity::getTenantId)
                .containsOnly(tenant2);

            // Verify no cross-tenant access
            assertThat(tenant1Consents).doesNotContain(consent2);
            assertThat(tenant2Consents).doesNotContain(consent1, consent3);
        }

        @Test
        @DisplayName("Consent validation should respect tenant boundaries")
        void consentValidationShouldRespectTenantBoundaries() {
            // Given - Consent exists in tenant1
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            String patientId = "patient-001";

            createAndSaveConsent(tenant1, patientId, "treatment");

            // When - Validate from tenant2
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                tenant2, patientId, "treatment", "clinical-notes", null, null);

            // Then - Denied because consent is in different tenant
            assertThat(result.isPermitted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Revocation Isolation Tests")
    class RevocationIsolationTests {

        @Test
        @DisplayName("Revoking consent in one tenant should not affect other tenants")
        void revokingConsentShouldNotAffectOtherTenants() {
            // Given
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            String patientId = "patient-001";

            ConsentEntity consent1 = createAndSaveConsent(tenant1, patientId, "treatment");
            ConsentEntity consent2 = createAndSaveConsent(tenant2, patientId, "treatment");

            // When - Revoke in tenant1
            consentService.revokeConsent(tenant1, consent1.getId(), "Test", "user-001");

            // Then - Tenant2 consent should still be active
            ConsentEntity tenant2Consent = consentRepository
                .findByTenantIdAndId(tenant2, consent2.getId()).orElseThrow();
            assertThat(tenant2Consent.getStatus()).isEqualTo("active");
        }
    }
}
```

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ConsentRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Nested
    @DisplayName("Consent Management RBAC")
    class ConsentManagementRbacTests {

        @Test
        @DisplayName("Admin can create consents")
        void adminCanCreateConsents() throws Exception {
            mockMvc.perform(post("/api/consents")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createConsentJson()))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Evaluator cannot create consents")
        void evaluatorCannotCreateConsents() throws Exception {
            mockMvc.perform(post("/api/consents")
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createConsentJson()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Viewer cannot revoke consents")
        void viewerCannotRevokeConsents() throws Exception {
            mockMvc.perform(post("/api/consents/{id}/revoke", UUID.randomUUID())
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID))
                    .param("reason", "Test"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Consent Validation RBAC")
    class ConsentValidationRbacTests {

        @Test
        @DisplayName("Evaluator can validate data access")
        void evaluatorCanValidateDataAccess() throws Exception {
            mockMvc.perform(post("/api/consents/validate-access")
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createValidationRequestJson()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Viewer can check consent status")
        void viewerCanCheckConsentStatus() throws Exception {
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/scope/{scope}",
                        "patient-001", "treatment")
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID)))
                .andExpect(status().isOk());
        }
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class ConsentHipaaComplianceTest {

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("42 CFR Part 2 Compliance Tests")
    class Part2ComplianceTests {

        @Test
        @DisplayName("Substance abuse data requires explicit consent")
        void substanceAbuseDataRequiresExplicitConsent() {
            // Given - General consent, not for substance abuse
            ConsentEntity generalConsent = createActiveConsent("general");
            consentRepository.save(generalConsent);

            // When - Validate access to substance abuse data
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                TENANT_ID, PATIENT_ID, "read", "treatment", "substance-abuse", null);

            // Then - Must be denied per 42 CFR Part 2
            assertThat(result.isPermitted()).isFalse();
            assertThat(result.getReason()).contains("No matching consent");
        }

        @Test
        @DisplayName("Mental health data requires explicit consent")
        void mentalHealthDataRequiresExplicitConsent() {
            // Given - General consent
            ConsentEntity generalConsent = createActiveConsent("general");
            consentRepository.save(generalConsent);

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                TENANT_ID, PATIENT_ID, "read", "treatment", "mental-health", null);

            // Then
            assertThat(result.isPermitted()).isFalse();
        }

        @Test
        @DisplayName("HIV data requires explicit consent")
        void hivDataRequiresExplicitConsent() {
            // Given - General consent
            ConsentEntity generalConsent = createActiveConsent("general");
            consentRepository.save(generalConsent);

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                TENANT_ID, PATIENT_ID, "read", "treatment", "hiv", null);

            // Then
            assertThat(result.isPermitted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Audit Retention Tests")
    class AuditRetentionTests {

        @Test
        @DisplayName("Audit configuration should specify 7-year retention")
        void auditRetentionShouldBe7Years() {
            // Given
            int requiredRetentionDays = 2555;  // ~7 years

            // When - Check application configuration
            // This would typically read from application.yml
            int configuredRetention = auditConfig.getRetentionDays();

            // Then
            assertThat(configuredRetention)
                .isGreaterThanOrEqualTo(requiredRetentionDays)
                .withFailMessage("HIPAA requires 7-year audit retention");
        }

        @Test
        @DisplayName("Consent changes should generate audit events")
        void consentChangesShouldGenerateAuditEvents() {
            // Given
            String patientId = "patient-001";

            // When - Create consent
            ConsentDto request = createValidConsentRequest();
            ConsentEntity created = consentService.createConsent(TENANT_ID, request, "user-001");

            // Then - Verify audit event was logged
            List<AuditEvent> auditEvents = auditRepository.findByResourceTypeAndResourceId(
                "Consent", created.getId().toString());

            assertThat(auditEvents)
                .isNotEmpty()
                .anyMatch(e -> e.getEventType().equals("CONSENT_CREATED"));
        }

        @Test
        @DisplayName("Revocation should include reason in audit trail")
        void revocationShouldIncludeReasonInAudit() {
            // Given
            ConsentEntity consent = createAndSaveConsent(TENANT_ID, PATIENT_ID, "treatment");
            String revocationReason = "Patient requested via phone call";

            // When
            consentService.revokeConsent(TENANT_ID, consent.getId(), revocationReason, "user-001");

            // Then
            List<AuditEvent> auditEvents = auditRepository.findByResourceTypeAndResourceId(
                "Consent", consent.getId().toString());

            assertThat(auditEvents)
                .anyMatch(e -> e.getEventType().equals("CONSENT_REVOKED") &&
                    e.getMetadata().contains(revocationReason));
        }
    }

    @Nested
    @DisplayName("Cache Compliance Tests")
    class CacheComplianceTests {

        @Test
        @DisplayName("Consent cache TTL should not exceed 5 minutes")
        void consentCacheTtlShouldBeCompliant() {
            // Given
            Cache consentCache = cacheManager.getCache("consents");

            // Then
            assertThat(consentCache).isNotNull();

            if (consentCache instanceof RedisCache) {
                RedisCacheConfiguration config = ((RedisCache) consentCache).getCacheConfiguration();
                assertThat(config.getTtl().getSeconds())
                    .isLessThanOrEqualTo(300L)
                    .withFailMessage("Consent cache TTL exceeds 5 minutes (HIPAA violation)");
            }
        }
    }

    @Nested
    @DisplayName("Response Header Tests")
    class ResponseHeaderTests {

        @Test
        @DisplayName("Consent responses should include no-cache headers")
        void consentResponsesShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/consents/patient/{patientId}/active", PATIENT_ID)
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID)))
                .andExpect(header().string("Cache-Control",
                    allOf(
                        containsString("no-store"),
                        containsString("no-cache"),
                        containsString("must-revalidate")
                    )))
                .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    @Nested
    @DisplayName("Synthetic Data Tests")
    class SyntheticDataTests {

        @Test
        @DisplayName("Test data must use synthetic patterns")
        void testDataMustUseSyntheticPatterns() {
            // Given - Create synthetic test patient
            Patient testPatient = SyntheticDataGenerator.createPatient(TENANT_ID);

            // Then - Verify synthetic patterns
            assertThat(testPatient.getIdentifierFirstRep().getValue())
                .startsWith("TEST-MRN-")
                .withFailMessage("Test MRNs should be clearly synthetic");

            assertThat(testPatient.getNameFirstRep().getGivenAsSingleString())
                .matches("(Test|Sample|Demo|Mock|Synthetic)-.*")
                .withFailMessage("Test patient names should be synthetic");
        }
    }
}
```

### Performance Tests

```java
@SpringBootTest
@Testcontainers
@Import(BaseTestContainersConfiguration.class)
class ConsentPerformanceTest {

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ConsentRepository consentRepository;

    private static final String TENANT_ID = "tenant-perf-001";

    @BeforeEach
    void setUp() {
        consentRepository.deleteAll();
    }

    @Test
    @DisplayName("Consent validation should complete within 50ms")
    void consentValidationPerformance() {
        // Given - Create 100 consents for the patient
        String patientId = "patient-perf-001";
        IntStream.range(0, 100).forEach(i ->
            createAndSaveConsent(TENANT_ID, patientId, "scope-" + i));

        // When - Validate access 1000 times
        int iterations = 1000;
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            consentService.validateDataAccess(
                TENANT_ID, patientId, "treatment", "clinical-notes", "general", null);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p50 = latencies.get(iterations / 2);
        long p95 = latencies.get((int) (iterations * 0.95));
        long p99 = latencies.get((int) (iterations * 0.99));

        assertThat(p95)
            .isLessThan(50L)
            .withFailMessage("Consent validation p95 %dms exceeds 50ms SLA", p95);

        System.out.printf("Consent Validation: p50=%dms, p95=%dms, p99=%dms%n", p50, p95, p99);
    }

    @Test
    @DisplayName("Bulk consent creation should scale linearly")
    void bulkConsentCreationPerformance() {
        // Given
        String patientId = "patient-bulk-001";
        int batchSize = 100;

        List<ConsentDto> requests = IntStream.range(0, batchSize)
            .mapToObj(i -> ConsentDto.builder()
                .patientId(patientId)
                .scope("treatment")
                .category("category-" + i)
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .build())
            .collect(Collectors.toList());

        // When
        Instant start = Instant.now();
        requests.forEach(req -> consentService.createConsent(TENANT_ID, req, "user-001"));
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerConsent = totalMs / (double) batchSize;

        assertThat(avgMsPerConsent)
            .isLessThan(20.0)
            .withFailMessage("Avg consent creation time %.2fms exceeds 20ms", avgMsPerConsent);

        System.out.printf("Bulk Creation: %d consents in %dms (avg: %.2fms/consent)%n",
            batchSize, totalMs, avgMsPerConsent);
    }

    @Test
    @DisplayName("Expired consent processing should complete within 5 seconds for 10K records")
    void expiredConsentProcessingPerformance() {
        // Given - Create 10,000 expired consents
        int count = 10000;
        List<ConsentEntity> expiredConsents = IntStream.range(0, count)
            .mapToObj(i -> {
                ConsentEntity consent = createActiveConsent(TENANT_ID, "patient-" + (i % 100));
                consent.setValidTo(LocalDate.now().minusDays(1));
                return consent;
            })
            .collect(Collectors.toList());
        consentRepository.saveAll(expiredConsents);

        // When
        Instant start = Instant.now();
        int processed = consentService.processExpiredConsents();
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();

        assertThat(processed).isEqualTo(count);
        assertThat(totalMs)
            .isLessThan(5000L)
            .withFailMessage("Processing %d expired consents took %dms (exceeds 5s)", count, totalMs);

        System.out.printf("Expired Processing: %d consents in %dms (%.2f/sec)%n",
            count, totalMs, count * 1000.0 / totalMs);
    }
}
```

### Test Configuration

The consent-service uses shared test utilities from `platform:test-fixtures`:

```java
@Configuration
@Profile("test")
public class ConsentTestConfiguration {

    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))  // HIPAA-compliant 5-minute TTL
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .withCacheConfiguration("consents",
                config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Synthetic Data** | Use `SyntheticDataGenerator` for HIPAA-compliant test patients |
| **42 CFR Part 2** | Always test substance-abuse, mental-health, HIV data class restrictions |
| **Gateway Trust Headers** | Use `GatewayTrustTestHeaders` utility for authentication |
| **Tenant Isolation** | Every test verifies tenantId filtering in queries |
| **Lifecycle Testing** | Test full consent lifecycle: create → validate → revoke → deny |
| **Kafka Events** | Verify consent events published (created, revoked, expired, deleted) |
| **Audit Trail** | Verify 7-year retention configuration and revocation reasons |
| **Cache TTL** | Verify 5-minute max TTL for consent cache |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Consent not found | Wrong tenant ID in query | Verify `X-Tenant-ID` header matches consent |
| Validation always denied | Missing consent scope/category/dataClass match | Check all 4 dimensions: scope, category, dataClass, authorizedParty |
| 42 CFR Part 2 denials | Requesting sensitive data without explicit consent | Create consent with specific dataClass (substance-abuse, mental-health, hiv) |
| Kafka event not published | Serialization error or Kafka unavailable | Check logs for ObjectMapper errors; Kafka failures are swallowed |
| Revocation fails | Consent already revoked or expired | Check current status before revoke; only active consents can be revoked |
| Cache returning stale data | TTL not configured correctly | Verify RedisCacheConfiguration has 5-minute TTL |
| TestContainers timeout | Docker not running or slow pull | Ensure Docker Desktop running; pre-pull postgres:15 image |

### Manual Testing

```bash
# Create consent
curl -X POST http://localhost:8082/consent/api/consents \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-uuid",
    "scope": "treatment",
    "category": "clinical-notes",
    "dataClass": "general",
    "validFrom": "2024-01-01",
    "validTo": "2025-01-01",
    "consentDate": "2024-01-01"
  }'

# Validate data access
curl -X POST http://localhost:8082/consent/api/consents/validate-access \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: EVALUATOR" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-uuid",
    "scope": "treatment",
    "category": "clinical-notes",
    "dataClass": "general"
  }'

# Check consent for scope
curl -X GET "http://localhost:8082/consent/api/consents/patient/patient-uuid/check/scope/treatment" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: VIEWER"

# Get active consents
curl -X GET "http://localhost:8082/consent/api/consents/patient/patient-uuid/active" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: VIEWER"

# Get expiring consents (within 30 days)
curl -X GET "http://localhost:8082/consent/api/consents/patient/patient-uuid/expiring-soon?days=30" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: VIEWER"

# Revoke consent
curl -X POST "http://localhost:8082/consent/api/consents/{consent-id}/revoke?reason=Patient%20request" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: ADMIN"

# Health check
curl -X GET http://localhost:8082/consent/api/consents/_health
```

## HIPAA Compliance

- 7-year audit retention for all consent changes
- Revocation reasons tracked for legal compliance
- Expiration checking prevents access to expired consents
- Granular authorization: scope + category + data class
- PHI not logged in audit trails (`include-phi: false`)
