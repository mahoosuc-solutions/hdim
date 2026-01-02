# FHIR Service

HL7 FHIR R4 server with SMART on FHIR authorization, bulk data export, and multi-tenant resource management.

## Purpose

Provides a FHIR R4-compliant API for healthcare data storage and retrieval, addressing the challenge that:
- Healthcare interoperability requires standardized FHIR R4 resource management
- SMART on FHIR authorization enables secure app integration
- Bulk data export supports quality reporting and population health analytics
- Multi-tenant isolation ensures data separation across healthcare organizations

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       FHIR Service                               │
│                         (Port 8085)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer (REST + FHIR Operations)                      │
│  ├── PatientController           - CRUD, search                 │
│  ├── ObservationController       - Labs, vitals                 │
│  ├── ConditionController         - Diagnoses                    │
│  ├── MedicationRequestController - Prescriptions                │
│  ├── AllergyIntoleranceController                               │
│  ├── ImmunizationController                                     │
│  ├── EncounterController         - Visits                       │
│  ├── ProcedureController                                        │
│  ├── CarePlanController                                         │
│  ├── DiagnosticReportController                                │
│  └── 10+ additional FHIR resource controllers                   │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── Resource Services         - Business logic per resource    │
│  ├── SearchService             - FHIR search parameters         │
│  └── ValidationService         - FHIR validation                │
├─────────────────────────────────────────────────────────────────┤
│  SMART on FHIR Layer                                            │
│  ├── SmartAuthorizationController - OAuth2 authorization        │
│  ├── SmartConfigurationController - .well-known/smart-config    │
│  └── ScopeValidator              - patient/*.read, user/*.*     │
├─────────────────────────────────────────────────────────────────┤
│  Bulk Export Layer                                              │
│  ├── BulkExportController      - $export operations             │
│  ├── BulkExportService         - Async NDJSON generation        │
│  └── ExportJobManager          - Job tracking, polling          │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── JPA Repositories (one per FHIR resource type)              │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── FHIR R4 Resources (mapped to PostgreSQL)                   │
│      ├── PatientEntity, ObservationEntity, etc.                 │
│      └── Metadata: tenantId, version, lastUpdated               │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### FHIR CRUD Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/fhir/Patient` | Create patient |
| GET | `/fhir/Patient/{id}` | Read patient |
| GET | `/fhir/Patient?name={name}` | Search patients |
| PUT | `/fhir/Patient/{id}` | Update patient |
| DELETE | `/fhir/Patient/{id}` | Soft delete patient |

**Supported Resources**: Patient, Observation, Condition, MedicationRequest, AllergyIntolerance, Immunization, Encounter, Procedure, CarePlan, DiagnosticReport, DocumentReference, Coverage, Goal, and more.

### SMART on FHIR
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/fhir/.well-known/smart-configuration` | SMART configuration |
| GET | `/fhir/oauth/authorize` | Authorization endpoint |
| POST | `/fhir/oauth/token` | Token endpoint |

### Bulk Data Export
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/fhir/$export` | System-level export |
| GET | `/fhir/Patient/$export` | Patient-level export |
| GET | `/fhir/Group/{id}/$export` | Group-level export |
| GET | `/fhir/bulkstatus/{jobId}` | Poll export status |
| GET | `/fhir/bulkdata/{fileName}` | Download NDJSON file |

## FHIR Search Parameters

Common search parameters supported across resources:

| Parameter | Example | Description |
|-----------|---------|-------------|
| `_id` | `?_id=123` | Search by logical ID |
| `_lastUpdated` | `?_lastUpdated=gt2024-01-01` | Last modified date |
| `_count` | `?_count=20` | Results per page |
| `patient` | `?patient=Patient/123` | Filter by patient |
| `date` | `?date=ge2024-01-01` | Date range filter |
| `code` | `?code=http://loinc.org|8480-6` | Filter by code |

## Configuration

```yaml
server:
  port: 8085
  servlet:
    context-path: /fhir

# FHIR settings
fhir:
  version: R4
  validation:
    enabled: true
    strict: false

  # Bulk export settings
  bulk-export:
    export-directory: /tmp/fhir-exports
    max-concurrent-exports: 5
    chunk-size: 1000
    retention-days: 7

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 120000  # 2 minutes for PHI

# HIPAA audit
audit:
  enabled: true
  retention-days: 2555  # 7 years
  include-phi: false

# Kafka integration
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    acks: all
    retries: 3
```

## FHIR Resource Example

**Patient Resource (FHIR R4)**:

```json
{
  "resourceType": "Patient",
  "id": "patient-123",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2024-01-15T10:00:00Z"
  },
  "identifier": [{
    "system": "http://hospital.org/mrn",
    "value": "MRN12345"
  }],
  "name": [{
    "family": "Smith",
    "given": ["John", "Robert"]
  }],
  "gender": "male",
  "birthDate": "1980-05-15",
  "address": [{
    "line": ["123 Main St"],
    "city": "Boston",
    "state": "MA",
    "postalCode": "02101"
  }]
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache, Actuator
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (2 min TTL for HIPAA compliance)
- **FHIR**: HAPI FHIR R4 (parsing, validation, structure definitions)
- **Messaging**: Kafka for resource change events
- **Security**: JWT + SMART on FHIR OAuth2
- **Audit**: HIPAA audit service (7-year retention)

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:fhir-service:bootRun

# Or via Docker
docker compose --profile fhir up fhir-service
```

## Testing

### Overview

FHIR Service has comprehensive test coverage with **53+ test files** covering FHIR resource operations, SMART on FHIR authorization, bulk data export, and multi-tenant isolation.

| Test Type | Count | Purpose |
|-----------|-------|---------|
| Service Tests | 15+ | FHIR resource CRUD operations with Mockito |
| Controller Tests | 12+ | REST API endpoint testing |
| Bulk Export Tests | 5+ | $export operation, NDJSON generation |
| SMART on FHIR Tests | 7+ | OAuth2 authorization, scopes |
| Subscription Tests | 5+ | WebSocket real-time notifications |
| Security Tests | 4+ | Auth filters, scope validation |
| Validation Tests | 3+ | FHIR R4 resource validation |

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:fhir-service:test

# Run specific test suite
./gradlew :modules:services:fhir-service:test --tests "*ServiceTest"
./gradlew :modules:services:fhir-service:test --tests "*BulkExport*"
./gradlew :modules:services:fhir-service:test --tests "*Smart*"

# Run with coverage report
./gradlew :modules:services:fhir-service:test jacocoTestReport

# Run single test class
./gradlew :modules:services:fhir-service:test --tests "PatientServiceTest"
```

### Test Organization

```
src/test/java/com/healthdata/fhir/
├── service/                        # Resource service unit tests
│   ├── PatientServiceTest.java     # CRUD, cache, Kafka events
│   ├── ObservationServiceTest.java
│   ├── ConditionServiceTest.java
│   ├── MedicationRequestServiceTest.java
│   └── ... (12 more resource services)
├── rest/                           # Controller tests
│   ├── PatientControllerTest.java
│   ├── ObservationControllerTest.java
│   └── ... (10 more controllers)
├── bulk/                           # Bulk data export tests
│   ├── BulkExportServiceTest.java  # Export kickoff, status, cleanup
│   ├── BulkExportProcessorTest.java
│   ├── BulkExportControllerTest.java
│   └── BulkExportJobTest.java
├── security/
│   ├── smart/                      # SMART on FHIR tests
│   │   ├── SmartAuthorizationServiceTest.java
│   │   ├── SmartAuthorizationControllerTest.java
│   │   ├── SmartConfigurationControllerTest.java
│   │   ├── SmartScopeTest.java
│   │   └── SmartClientTest.java
│   └── AuthContextFilterTest.java
├── subscription/                   # Real-time notification tests
│   ├── SubscriptionServiceTest.java
│   ├── SubscriptionControllerTest.java
│   └── SubscriptionWebSocketHandlerTest.java
├── validation/                     # FHIR validation tests
│   └── PatientValidatorTest.java
├── config/                         # Configuration tests
│   ├── FhirSecurityConfigTest.java
│   └── FhirSecurityConfigProdTest.java
└── persistence/                    # Entity tests
    └── EntityLifecycleTest.java
```

### Unit Tests (Service Layer)

Unit tests verify FHIR resource operations with mocked dependencies.

**Example: PatientServiceTest.java** (228 lines)

```java
class PatientServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String PATIENT_ID = "8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95";
    private static final String CACHE_KEY = TENANT + ":" + PATIENT_ID;

    @Mock private PatientRepository patientRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;
    @Mock private PatientValidator validator;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("fhir-patients")).thenReturn(cache);
        patientService = new PatientService(patientRepository, validator, kafkaTemplate, cacheManager);
    }

    @Test
    void createPatientShouldPersistAndPublish() {
        // Given
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.addName().setFamily("Chen").addGiven("Maya");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);

        PatientEntity saved = PatientEntity.builder()
            .id(UUID.fromString(PATIENT_ID))
            .tenantId(TENANT)
            .resourceType("Patient")
            .resourceJson("{\"resourceType\":\"Patient\"}")
            .build();

        when(validator.validate(patient)).thenReturn(ValidationResult.ok());
        when(patientRepository.save(any())).thenReturn(saved);

        // When
        Patient result = patientService.createPatient(TENANT, patient, "user-1");

        // Then
        assertThat(result.getId()).isEqualTo(PATIENT_ID);
        verify(kafkaTemplate).send(eq("fhir.patients.created"), eq(PATIENT_ID), any());
        verify(cache).put(eq(CACHE_KEY), any(Patient.class));
    }

    @Test
    void getPatientShouldUseCacheBeforeRepository() {
        // Given
        Patient cached = new Patient();
        cached.setId(PATIENT_ID);
        when(cache.get(CACHE_KEY, Patient.class)).thenReturn(cached);

        // When
        Optional<Patient> result = patientService.getPatient(TENANT, PATIENT_ID);

        // Then - cache hit means no repository call
        assertThat(result).isPresent();
        verify(patientRepository, never()).findActiveByTenantIdAndId(eq(TENANT), any());
    }

    @Test
    void deletePatientShouldEvictCacheAndPublish() {
        // Given
        PatientEntity entity = createTestEntity(PATIENT_ID, TENANT);
        when(patientRepository.findByTenantIdAndId(TENANT, UUID.fromString(PATIENT_ID)))
            .thenReturn(Optional.of(entity));

        // When
        patientService.deletePatient(TENANT, PATIENT_ID, "user-3");

        // Then
        verify(cache).evict(CACHE_KEY);
        verify(kafkaTemplate).send(eq("fhir.patients.deleted"), eq(PATIENT_ID), any());
    }
}
```

**Key FHIR-Specific Patterns**:
- Use HAPI FHIR `Patient`, `Observation`, etc. from `org.hl7.fhir.r4.model`
- Cache keys: `{tenantId}:{resourceId}` for tenant-namespaced caching
- Kafka topics: `fhir.{resource}.created`, `fhir.{resource}.updated`, etc.
- Validator returns `ValidationResult` for FHIR R4 compliance

### Bulk Data Export Tests

Tests for async $export operations per FHIR Bulk Data Access specification.

**Example: BulkExportServiceTest.java** (301 lines)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Export Service Tests")
class BulkExportServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock private BulkExportRepository exportRepository;
    @Mock private BulkExportProcessor exportProcessor;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should kick off export with defaults and start processing")
    void shouldKickOffExport() {
        // Given
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(5);
        BulkExportService service = new BulkExportService(
            exportRepository, exportProcessor, config, kafkaTemplate);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(exportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        UUID jobId = service.kickOffExport(
            TENANT_ID,
            BulkExportJob.ExportLevel.SYSTEM,
            null, null, null, List.of(),
            "/fhir/$export", "user");

        // Then
        assertThat(jobId).isNotNull();
        verify(exportProcessor).processExport(jobId);
        verify(kafkaTemplate).send(eq("fhir.bulk-export.initiated"), eq(jobId.toString()), any());
    }

    @Test
    @DisplayName("Should reject when export limit exceeded")
    void shouldRejectWhenLimitExceeded() {
        // Given
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(1);
        BulkExportService service = new BulkExportService(...);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> service.kickOffExport(
            TENANT_ID, ExportLevel.SYSTEM, ...))
            .isInstanceOf(ExportLimitExceededException.class);
    }

    @Test
    @DisplayName("Should cancel export job")
    void shouldCancelJob() {
        // Given
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = createPendingJob(jobId, TENANT_ID);
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID))
            .thenReturn(Optional.of(job));

        // When
        service.cancelJob(TENANT_ID, jobId, "user");

        // Then
        verify(exportRepository).save(any());
        verify(kafkaTemplate).send(eq("fhir.bulk-export.cancelled"), eq(jobId.toString()), any());
    }
}
```

### SMART on FHIR Tests

Tests for OAuth2 authorization per SMART App Launch specification.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SMART Authorization Service Tests")
class SmartAuthorizationServiceTest {

    @Test
    @DisplayName("Should validate patient/*.read scope")
    void shouldValidatePatientReadScope() {
        // Given
        String scope = "patient/Patient.read patient/Observation.read";
        String patientId = "patient-123";

        // When
        boolean canRead = smartService.canAccessResource(scope, "Patient", patientId);

        // Then
        assertThat(canRead).isTrue();
    }

    @Test
    @DisplayName("Should reject write with read-only scope")
    void shouldRejectWriteWithReadScope() {
        // Given
        String scope = "patient/Patient.read";

        // When
        boolean canWrite = smartService.canWriteResource(scope, "Patient", "patient-123");

        // Then
        assertThat(canWrite).isFalse();
    }

    @Test
    @DisplayName("Should issue authorization code for valid client")
    void shouldIssueAuthCode() {
        // Given
        SmartClient client = createTestClient("client-123");
        when(clientRepository.findByClientId("client-123"))
            .thenReturn(Optional.of(client));

        // When
        String authCode = smartService.authorize(
            "client-123",
            "https://app.example.com/callback",
            "launch patient/Patient.read",
            "state-123");

        // Then
        assertThat(authCode).isNotBlank();
        assertThat(authCode).hasSize(32);
    }
}
```

### Multi-Tenant Isolation Tests

Ensures FHIR resources are isolated per tenant.

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FHIR Multi-Tenant Isolation Tests")
class FhirMultiTenantTest {

    private static final String TENANT_1 = "hospital-a";
    private static final String TENANT_2 = "hospital-b";

    @Test
    @DisplayName("Should isolate patients by tenant")
    void shouldIsolatePatientsByTenant() throws Exception {
        // Create patient for tenant 1
        mockMvc.perform(post("/fhir/Patient")
                .header("X-Tenant-ID", TENANT_1)
                .contentType("application/fhir+json")
                .content(createPatientJson("Smith")))
            .andExpect(status().isCreated());

        // Create patient for tenant 2
        mockMvc.perform(post("/fhir/Patient")
                .header("X-Tenant-ID", TENANT_2)
                .contentType("application/fhir+json")
                .content(createPatientJson("Jones")))
            .andExpect(status().isCreated());

        // Search tenant 1 - should only see "Smith"
        mockMvc.perform(get("/fhir/Patient")
                .header("X-Tenant-ID", TENANT_1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entry", hasSize(1)))
            .andExpect(jsonPath("$.entry[0].resource.name[0].family").value("Smith"));

        // Search tenant 2 - should only see "Jones"
        mockMvc.perform(get("/fhir/Patient")
                .header("X-Tenant-ID", TENANT_2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entry", hasSize(1)))
            .andExpect(jsonPath("$.entry[0].resource.name[0].family").value("Jones"));
    }

    @Test
    @DisplayName("Should isolate bulk exports by tenant")
    void shouldIsolateBulkExportsByTenant() throws Exception {
        // Start export for tenant 1
        MvcResult result1 = mockMvc.perform(get("/fhir/$export")
                .header("X-Tenant-ID", TENANT_1)
                .header("Prefer", "respond-async"))
            .andExpect(status().isAccepted())
            .andReturn();

        String jobId1 = extractJobId(result1);

        // Tenant 2 should NOT see tenant 1's export
        mockMvc.perform(get("/fhir/bulkstatus/" + jobId1)
                .header("X-Tenant-ID", TENANT_2))
            .andExpect(status().isNotFound());
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
@DisplayName("FHIR HIPAA Compliance Tests")
class FhirHipaaComplianceTest {

    @Test
    @DisplayName("PHI cache TTL must not exceed 2 minutes")
    void phiCacheTtlShouldBeCompliant() {
        // FHIR Service uses 2-minute TTL (stricter than 5-min requirement)
        Cache patientCache = cacheManager.getCache("fhir-patients");
        assertThat(patientCache).isNotNull();
        // Verify in application.yml: spring.cache.redis.time-to-live: 120000
    }

    @Test
    @DisplayName("FHIR responses should include Cache-Control headers")
    void fhirResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/fhir/Patient/123")
                .header("X-Tenant-ID", "tenant-1"))
            .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("FHIR operations should generate audit events")
    void fhirOperationsShouldBeAudited() {
        // Create patient
        patientService.createPatient(TENANT_ID, testPatient, "user-1");

        // Verify Kafka audit event
        verify(kafkaTemplate).send(eq("fhir.audit.phi-access"), any(), any());
    }
}
```

### Subscription/WebSocket Tests

Tests for real-time FHIR subscription notifications.

```java
@SpringBootTest
@DisplayName("FHIR Subscription Tests")
class SubscriptionServiceTest {

    @Test
    @DisplayName("Should create subscription for patient changes")
    void shouldCreateSubscription() {
        // Given
        SubscriptionRequest request = SubscriptionRequest.builder()
            .criteria("Patient?_id=patient-123")
            .channel(ChannelType.WEBSOCKET)
            .build();

        // When
        Subscription sub = subscriptionService.createSubscription(TENANT_ID, request);

        // Then
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getCriteria()).isEqualTo("Patient?_id=patient-123");
    }

    @Test
    @DisplayName("Should notify subscribers when patient updated")
    void shouldNotifyOnPatientUpdate() {
        // Given
        createSubscription(TENANT_ID, "Patient?_id=patient-123");

        // When
        patientService.updatePatient(TENANT_ID, "patient-123", updatedPatient, "user-1");

        // Then
        verify(webSocketHandler).broadcast(TENANT_ID, any(SubscriptionNotification.class));
    }
}
```

### Test Configuration

**application-test.yml**:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  cache:
    type: simple  # Use simple cache instead of Redis for tests

  kafka:
    bootstrap-servers: ""  # Disabled in tests

fhir:
  validation:
    enabled: true
    strict: false

  bulk-export:
    export-directory: /tmp/fhir-test-exports
    max-concurrent-exports: 2
    chunk-size: 100

logging:
  level:
    com.healthdata.fhir: DEBUG
    ca.uhn.fhir: WARN
```

### Best Practices

1. **FHIR Resource Testing**
   - Use HAPI FHIR model classes (`org.hl7.fhir.r4.model.*`)
   - Validate resources against FHIR R4 profiles
   - Test search parameters per FHIR specification

2. **Bulk Export Testing**
   - Use temp directories for NDJSON files
   - Clean up export files after tests
   - Test concurrent export limits

3. **SMART on FHIR Testing**
   - Test all scope combinations (patient/*.read, user/*.*)
   - Verify launch context propagation
   - Test token refresh flows

4. **Tenant Isolation**
   - Every test verifies `tenantId` in queries
   - Test cross-tenant access denial
   - Verify tenant ID in FHIR Meta

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| HAPI FHIR parse error | Invalid FHIR JSON | Validate against FHIR R4 profile |
| Bulk export timeout | Large dataset | Reduce chunk-size in test config |
| SMART scope denied | Wrong scope format | Use `patient/Patient.read` format |
| Subscription not firing | WebSocket not connected | Mock WebSocketHandler |
| Cache miss in tests | Wrong cache name | Check `@Cacheable` value matches |

### Manual Testing (curl)

```bash
# Create patient
curl -X POST http://localhost:8085/fhir/Patient \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{"family": "Smith", "given": ["John"]}],
    "gender": "male",
    "birthDate": "1980-05-15"
  }'

# Search patients
curl http://localhost:8085/fhir/Patient?name=Smith \
  -H "X-Tenant-ID: tenant-1"

# Bulk export (async)
curl -X GET http://localhost:8085/fhir/$export \
  -H "X-Tenant-ID: tenant-1" \
  -H "Prefer: respond-async"

# Poll export status
curl http://localhost:8085/fhir/bulkstatus/{jobId}
```

## SMART on FHIR Scopes

Supported SMART scopes:

- `patient/*.read` - Read all patient data
- `patient/Patient.read` - Read patient demographics
- `patient/Observation.read` - Read observations
- `user/*.read` - Read all data (provider access)
- `user/*.write` - Write all data (provider access)

## Bulk Export Format

NDJSON (newline-delimited JSON) files:

```
{"resourceType":"Patient","id":"p1","name":[{"family":"Smith"}]}
{"resourceType":"Patient","id":"p2","name":[{"family":"Jones"}]}
```

## Performance

- Single resource CRUD: 10-50ms (cached)
- Search queries: 50-200ms (indexed)
- Bulk export: 1000 resources/sec
- Redis caching: 2 min TTL (HIPAA compliant)
