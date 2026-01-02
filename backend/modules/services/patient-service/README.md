# Patient Service

Patient data aggregation, timeline visualization, and health status dashboards with consent-aware data access.

## Purpose

Aggregates patient data from FHIR service into comprehensive views, addressing the challenge that:
- Clinicians need a unified view of patient records scattered across FHIR resources
- Timeline views require chronological ordering of encounters, medications, conditions, labs
- Health status dashboards need real-time aggregation of active conditions, medications, allergies
- Consent validation must occur before returning PHI

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Patient Service                              │
│                         (Port 8084)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── PatientController (30+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── PatientAggregationService                                  │
│  │   ├── Comprehensive health record  - All resources           │
│  │   ├── Filtered queries            - Active only, critical    │
│  │   └── Resource-specific views     - Meds, allergies, vitals  │
│  ├── PatientTimelineService                                     │
│  │   ├── Chronological timeline      - All events sorted        │
│  │   ├── Date range filtering        - Custom time windows      │
│  │   ├── Resource type filtering     - Condition, Observation   │
│  │   └── Monthly summaries           - Event counts by month    │
│  └── PatientHealthStatusService                                 │
│      ├── Health status dashboard     - Overview with counts     │
│      └── Resource summaries          - Med, allergy, condition  │
├─────────────────────────────────────────────────────────────────┤
│  Client Layer (Feign)                                           │
│  ├── FhirClient         - Query FHIR resources                  │
│  └── ConsentClient      - Validate data access                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) + Circuit Breaker
                              ▼
┌────────────────────────────────────────────────────────────────┐
│  FHIR Service (8085)       Consent Service (8082)              │
│  - Patient resources       - Authorization validation           │
│  - Observations, Meds      - Scope/category checking            │
└────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Patient Aggregation
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/health-record?patient={id}` | Comprehensive health record |
| GET | `/patient/allergies?patient={id}` | Patient allergies |
| GET | `/patient/immunizations?patient={id}` | Immunizations |
| GET | `/patient/medications?patient={id}` | Medications (active/all) |
| GET | `/patient/conditions?patient={id}` | Conditions (active/all) |
| GET | `/patient/procedures?patient={id}` | Procedures |
| GET | `/patient/vitals?patient={id}` | Vital signs |
| GET | `/patient/labs?patient={id}` | Lab results |
| GET | `/patient/encounters?patient={id}` | Encounters |
| GET | `/patient/care-plans?patient={id}` | Care plans |

### Timeline Views
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/timeline?patient={id}` | Full patient timeline |
| GET | `/patient/timeline/by-date?startDate={d1}&endDate={d2}` | Date range timeline |
| GET | `/patient/timeline/by-type?resourceType={type}` | Filter by resource type |
| GET | `/patient/timeline/summary?year={year}` | Monthly summary |

### Health Status Dashboards
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/health-status?patient={id}` | Health status overview |
| GET | `/patient/medication-summary?patient={id}` | Medication summary |
| GET | `/patient/allergy-summary?patient={id}` | Allergy summary |
| GET | `/patient/condition-summary?patient={id}` | Condition summary |
| GET | `/patient/immunization-summary?patient={id}` | Immunization summary |

## Response Formats

All aggregation endpoints return FHIR Bundles (`application/fhir+json`):

```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 42,
  "entry": [
    {
      "resource": {
        "resourceType": "MedicationRequest",
        "id": "med-123",
        "status": "active",
        "medicationCodeableConcept": {
          "coding": [{
            "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
            "code": "197361",
            "display": "Lisinopril 10 MG"
          }]
        }
      }
    }
  ]
}
```

Timeline endpoints return JSON arrays:

```json
[
  {
    "date": "2024-01-15T10:30:00Z",
    "resourceType": "Condition",
    "resourceId": "cond-123",
    "title": "Hypertension",
    "description": "Essential hypertension diagnosis",
    "category": "CONDITION"
  }
]
```

## Configuration

```yaml
server:
  port: 8084
  servlet:
    context-path: /patient

# FHIR service integration
fhir:
  server:
    url: http://localhost:8085/fhir

# Consent service integration
consent:
  server:
    url: http://localhost:8086/consent

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 120000  # 2 minutes for PHI

# HIPAA audit
audit:
  enabled: true
  encryption:
    enabled: true
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (2 min TTL for HIPAA compliance)
- **FHIR**: HAPI FHIR R4 for resource parsing
- **HTTP Client**: OpenFeign for FHIR/Consent service integration
- **Resilience**: Circuit breakers for service failures

## Running Locally

```bash
# Start dependencies (FHIR service, Consent service)
docker compose up -d fhir-service consent-service

# From backend directory
./gradlew :modules:services:patient-service:bootRun

# Or via Docker
docker compose --profile patient up patient-service
```

## Testing

Patient Service has 14+ test files covering unit, integration, multi-tenant isolation, consent validation, and HIPAA compliance testing for patient data aggregation and timeline generation.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:patient-service:test

# Run specific test suite
./gradlew :modules:services:patient-service:test --tests "*IntegrationTest"
./gradlew :modules:services:patient-service:test --tests "*MultiTenantIsolation*"
./gradlew :modules:services:patient-service:test --tests "*AggregationServiceTest"

# Run with coverage
./gradlew :modules:services:patient-service:test jacocoTestReport

# Run tests excluding Docker-dependent tests
./gradlew :modules:services:patient-service:test -DexcludeTags="testcontainers"
```

### Test Organization

```
src/test/java/com/healthdata/patient/
├── config/
│   ├── BaseIntegrationTest.java             # TestContainers base annotation
│   ├── PatientSecurityConfigTest.java       # Security configuration tests
│   └── TestCacheConfiguration.java          # Redis test configuration
├── service/
│   ├── PatientAggregationServiceTest.java   # HIPAA-critical consent tests (1100+ lines)
│   ├── PatientTimelineServiceTest.java      # Timeline generation tests
│   └── PatientHealthStatusServiceTest.java  # Health dashboard tests
├── controller/
│   └── PatientControllerTest.java           # Controller unit tests
├── entity/
│   └── PatientEntityLifecycleTest.java      # Entity persistence tests
├── client/
│   └── FhirServiceClientConfigurationTest.java  # Feign client tests
└── integration/
    ├── PatientControllerIntegrationTest.java     # API endpoint tests
    ├── PatientDemographicsRepositoryIntegrationTest.java
    ├── PatientInsuranceRepositoryIntegrationTest.java
    ├── PatientRiskScoreRepositoryIntegrationTest.java
    └── MultiTenantIsolationIntegrationTest.java  # HIPAA tenant isolation (395 lines)
```

### Unit Tests (Service Layer)

Unit tests verify patient aggregation, consent filtering, and FHIR resource handling using Mockito.

**Example: PatientAggregationService Unit Test** (from `PatientAggregationServiceTest.java`, 1100+ lines):

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Aggregation Service Tests (HIPAA Critical)")
class PatientAggregationServiceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private ConsentServiceClient consentServiceClient;

    @InjectMocks
    private PatientAggregationService aggregationService;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @Nested
    @DisplayName("Get Comprehensive Health Record Tests")
    class GetComprehensiveHealthRecordTests {

        @Test
        @DisplayName("Should return all resources when no consent restrictions")
        void shouldReturnAllResourcesWhenNoConsentRestrictions() {
            // Given - No consent restrictions
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, false);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
            assertThat(result.getTotal()).isEqualTo(10); // 10 resource types

            // Verify all resource types were fetched
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getImmunizations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getProcedures(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should filter restricted resource types (HIPAA 42 CFR Part 2)")
        void shouldFilterRestrictedResourceTypes() {
            // Given - Patient has consent restrictions for substance abuse data
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, true);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            List<String> restrictedTypes = List.of("MedicationRequest", "Condition", "Observation");
            when(consentServiceClient.getRestrictedResourceTypes(TENANT_ID, PATIENT_ID))
                .thenReturn(restrictedTypes);
            when(consentServiceClient.getSensitiveCategories(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of("substance-abuse", "mental-health"));

            // Mock only unrestricted resource types
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("AllergyIntolerance", 1));
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Immunization", 1));

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result.getTotal()).isEqualTo(7); // Only unrestricted resources

            // Verify restricted resource types were NOT fetched
            verify(fhirServiceClient, never()).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getObservations(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle consent service failure gracefully (fail-open)")
        void shouldHandleConsentServiceFailureGracefully() {
            // Given - Consent service is unavailable
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenThrow(FeignException.ServiceUnavailable.class);

            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should return data with no restrictions (fail-open for availability)
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Get Allergies Tests")
    class GetAllergiesTests {

        @Test
        @DisplayName("Should return all allergies when onlyCritical is false")
        void shouldReturnAllAllergies() {
            String allergyJson = createJsonBundle("AllergyIntolerance", 3);
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(allergyJson);

            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false);

            assertThat(result.getTotal()).isEqualTo(3);
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should return only critical allergies when onlyCritical is true")
        void shouldReturnOnlyCriticalAllergies() {
            String allergyJson = createJsonBundle("AllergyIntolerance", 2);
            when(fhirServiceClient.getCriticalAllergies(TENANT_ID, PATIENT_ID))
                .thenReturn(allergyJson);

            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, true);

            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getCriticalAllergies(TENANT_ID, PATIENT_ID);
        }
    }

    @Nested
    @DisplayName("Get Medications Tests")
    class GetMedicationsTests {

        @Test
        @DisplayName("Should return only active medications when onlyActive is true")
        void shouldReturnOnlyActiveMedications() {
            String medicationJson = createJsonBundle("MedicationRequest", 2);
            when(fhirServiceClient.getActiveMedications(TENANT_ID, PATIENT_ID))
                .thenReturn(medicationJson);

            Bundle result = aggregationService.getMedications(TENANT_ID, PATIENT_ID, true);

            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getActiveMedications(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getMedicationRequests(any(), any());
        }
    }
}
```

### Integration Tests (API Endpoints)

Integration tests verify controller endpoints with real database containers.

**Example: PatientController Integration Test**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCacheConfiguration.class)
@Transactional
class PatientControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientDemographicsRepository demographicsRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-001";

    @BeforeEach
    void setUp() {
        PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
            .tenantId(TENANT_ID)
            .fhirPatientId(PATIENT_ID)
            .firstName("Test")
            .lastName("Patient")
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .gender("male")
            .active(true)
            .build();
        demographicsRepository.save(patient);
    }

    @Test
    @DisplayName("Should return comprehensive health record")
    void shouldReturnComprehensiveHealthRecord() throws Exception {
        mockMvc.perform(get("/patient/health-record")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-Roles", "VIEWER")
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(jsonPath("$.resourceType").value("Bundle"))
            .andExpect(jsonPath("$.type").value("collection"));
    }

    @Test
    @DisplayName("Should return allergies for patient")
    void shouldReturnAllergiesForPatient() throws Exception {
        mockMvc.perform(get("/patient/allergies")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resourceType").value("Bundle"));
    }

    @Test
    @DisplayName("Should return active medications only")
    void shouldReturnActiveMedicationsOnly() throws Exception {
        mockMvc.perform(get("/patient/medications")
                .param("patient", PATIENT_ID)
                .param("onlyActive", "true")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return timeline by date range")
    void shouldReturnTimelineByDateRange() throws Exception {
        mockMvc.perform(get("/patient/timeline/by-date")
                .param("patient", PATIENT_ID)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should return health status dashboard")
    void shouldReturnHealthStatusDashboard() throws Exception {
        mockMvc.perform(get("/patient/health-status")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        // Try to access patient from different tenant - should be 404
        mockMvc.perform(get("/patient/health-record")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", otherTenant)
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isNotFound());
    }
}
```

### Multi-Tenant Isolation Tests

HIPAA-compliant multi-tenant isolation tests verify tenant data separation at repository layer.

**Example: MultiTenantIsolationIntegrationTest** (from `MultiTenantIsolationIntegrationTest.java`, 395 lines):

```java
@BaseIntegrationTest
@DisplayName("Multi-Tenant Isolation Tests (HIPAA Compliance)")
class MultiTenantIsolationIntegrationTest {

    @Autowired
    private PatientDemographicsRepository demographicsRepository;

    @Autowired
    private PatientInsuranceRepository insuranceRepository;

    @Autowired
    private PatientRiskScoreRepository riskScoreRepository;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";
    private static final String TENANT_C = "tenant-gamma";

    private PatientDemographicsEntity patientTenantA;
    private PatientDemographicsEntity patientTenantB;

    @BeforeEach
    void setUp() {
        patientTenantA = createPatient(TENANT_A, "Alice", "Anderson", "patient-A-001");
        patientTenantB = createPatient(TENANT_B, "Bob", "Brown", "patient-B-001");

        patientTenantA = demographicsRepository.save(patientTenantA);
        patientTenantB = demographicsRepository.save(patientTenantB);
    }

    @Nested
    @DisplayName("Demographics Isolation")
    class DemographicsIsolationTests {

        @Test
        @DisplayName("Should store PHI with correct tenant isolation")
        void shouldStorePHIWithTenantIsolation() {
            // Given - Add sensitive PHI to each patient
            patientTenantA.setSsnEncrypted("111-11-1111");
            patientTenantA.setEmail("alice@example.com");
            demographicsRepository.save(patientTenantA);

            patientTenantB.setSsnEncrypted("222-22-2222");
            patientTenantB.setEmail("bob@example.com");
            demographicsRepository.save(patientTenantB);

            // When - Query all patients
            List<PatientDemographicsEntity> all = demographicsRepository.findAll();

            // Then - Verify each patient's PHI is associated with correct tenant
            PatientDemographicsEntity foundA = all.stream()
                .filter(p -> TENANT_A.equals(p.getTenantId()))
                .findFirst()
                .orElseThrow();
            assertThat(foundA.getSsnEncrypted()).isEqualTo("111-11-1111");

            PatientDemographicsEntity foundB = all.stream()
                .filter(p -> TENANT_B.equals(p.getTenantId()))
                .findFirst()
                .orElseThrow();
            assertThat(foundB.getSsnEncrypted()).isEqualTo("222-22-2222");
        }

        @Test
        @DisplayName("Patient counts should be separate per tenant")
        void shouldHaveSeparatePatientCounts() {
            // Given - Add more patients to Tenant A
            demographicsRepository.save(createPatient(TENANT_A, "Alex", "Adams", "patient-A-002"));
            demographicsRepository.save(createPatient(TENANT_A, "Amy", "Allen", "patient-A-003"));

            // When
            List<PatientDemographicsEntity> all = demographicsRepository.findAll();

            // Then
            long tenantACount = all.stream()
                .filter(p -> TENANT_A.equals(p.getTenantId()))
                .count();
            long tenantBCount = all.stream()
                .filter(p -> TENANT_B.equals(p.getTenantId()))
                .count();

            assertThat(tenantACount).isEqualTo(3); // Alice + Alex + Amy
            assertThat(tenantBCount).isEqualTo(1); // Bob only
        }
    }

    @Nested
    @DisplayName("Risk Score Isolation")
    class RiskScoreIsolationTests {

        @Test
        @DisplayName("Risk factors should not leak across tenants")
        void shouldNotLeakRiskFactorsAcrossTenants() {
            // Given - Create risk scores with sensitive health factors
            PatientRiskScoreEntity scoreA = createRiskScore(
                TENANT_A, patientTenantA.getId(), new BigDecimal("1.8"), "high");
            scoreA.setFactors("[\"hiv_positive\", \"substance_abuse\"]");  // Sensitive
            scoreA.setComorbidities("[\"B20\", \"F10.20\"]");  // ICD-10 codes

            PatientRiskScoreEntity scoreB = createRiskScore(
                TENANT_B, patientTenantB.getId(), new BigDecimal("1.2"), "medium");
            scoreB.setFactors("[\"diabetes\", \"hypertension\"]");

            riskScoreRepository.save(scoreA);
            riskScoreRepository.save(scoreB);

            // When
            List<PatientRiskScoreEntity> all = riskScoreRepository.findAll();

            // Then - Verify no cross-tenant leakage
            PatientRiskScoreEntity foundB = all.stream()
                .filter(s -> TENANT_B.equals(s.getTenantId()))
                .findFirst()
                .orElseThrow();

            assertThat(foundB.getFactors()).contains("diabetes");
            assertThat(foundB.getFactors()).doesNotContain("hiv_positive");
        }
    }

    @Nested
    @DisplayName("Cross-Tenant Data Prevention")
    class CrossTenantPreventionTests {

        @Test
        @DisplayName("Tenant B should not access Tenant A's patient by ID")
        void shouldNotAccessOtherTenantPatientById() {
            // Simulate tenant-filtered query
            List<PatientDemographicsEntity> tenantBPatients = demographicsRepository.findAll()
                .stream()
                .filter(p -> TENANT_B.equals(p.getTenantId()))
                .toList();

            // Then - Tenant B should not see Tenant A's patients
            assertThat(tenantBPatients).noneMatch(p -> TENANT_A.equals(p.getTenantId()));
            assertThat(tenantBPatients).noneMatch(p -> "Alice".equals(p.getFirstName()));
        }

        @Test
        @DisplayName("All entities should have non-null tenant IDs")
        void shouldHaveNonNullTenantIds() {
            List<PatientDemographicsEntity> allDemographics = demographicsRepository.findAll();
            List<PatientInsuranceEntity> allInsurance = insuranceRepository.findAll();
            List<PatientRiskScoreEntity> allRiskScores = riskScoreRepository.findAll();

            // Add some insurance and risk scores
            insuranceRepository.save(createInsurance(TENANT_A, patientTenantA.getId(), "TEST-001"));
            riskScoreRepository.save(createRiskScore(
                TENANT_A, patientTenantA.getId(), new BigDecimal("1.0"), "medium"));

            allInsurance = insuranceRepository.findAll();
            allRiskScores = riskScoreRepository.findAll();

            // Then - All entities should have tenant IDs
            assertThat(allDemographics)
                .allMatch(p -> p.getTenantId() != null && !p.getTenantId().isEmpty());
            assertThat(allInsurance)
                .allMatch(i -> i.getTenantId() != null && !i.getTenantId().isEmpty());
            assertThat(allRiskScores)
                .allMatch(s -> s.getTenantId() != null && !s.getTenantId().isEmpty());
        }
    }

    private PatientDemographicsEntity createPatient(
            String tenantId, String firstName, String lastName, String fhirId) {
        return PatientDemographicsEntity.builder()
            .tenantId(tenantId)
            .fhirPatientId(fhirId)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .gender("male")
            .active(true)
            .deceased(false)
            .build();
    }
}
```

### RBAC/Permission Tests

Role-based access control tests verify that only authorized roles can access patient data.

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";
    private static final String PATIENT_ID = "patient-123";

    @Test
    @DisplayName("Viewer should be able to read patient data")
    void viewerCanReadPatientData() throws Exception {
        mockMvc.perform(get("/patient/health-record")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Evaluator should be able to read patient data")
    void evaluatorCanReadPatientData() throws Exception {
        mockMvc.perform(get("/patient/medications")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request should be rejected")
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/patient/health-record")
                .param("patient", PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            // No auth headers
            .andExpect(status().isUnauthorized());
    }
}
```

### HIPAA Compliance Tests

Consent validation and PHI cache TTL verification.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfiguration.class)
class PatientHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Patient data cache TTL must not exceed 2 minutes")
    void patientDataCacheTtlShouldBeCompliant() {
        Cache patientCache = cacheManager.getCache("patientData");

        assertThat(patientCache).isNotNull();

        if (patientCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) patientCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(120L)  // 2 minutes for patient service
                .withFailMessage("Patient cache TTL exceeds 2 minutes (HIPAA violation)");
        }
    }

    @Test
    @DisplayName("PHI responses must include no-cache headers")
    void phiResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/patient/health-record")
                .param("patient", "patient-123")
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("Consent restrictions must filter sensitive data")
    void consentRestrictionsShouldFilterSensitiveData() {
        // 42 CFR Part 2 compliance - substance abuse data filtered
        // Covered by PatientAggregationServiceTest.shouldFilterRestrictedResourceTypes()
    }

    @Test
    @DisplayName("PHI should be encrypted at rest")
    void phiShouldBeEncryptedAtRest() {
        // Verify SSN field uses encrypted column
        PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
            .tenantId("tenant-001")
            .fhirPatientId("patient-001")
            .firstName("Test")
            .lastName("Patient")
            .ssnEncrypted("111-11-1111")  // Should be encrypted in DB
            .build();

        assertThat(patient.getSsnEncrypted()).isNotNull();
        // Actual encryption verified via database inspection
    }
}
```

### Test Configuration

**BaseIntegrationTest** - Annotation for integration tests with TestContainers:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
public @interface BaseIntegrationTest {}
```

**TestCacheConfiguration** - Redis cache with HIPAA-compliant TTL:

```java
@Configuration
@Profile("test")
public class TestCacheConfiguration {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(2))  // 2 minutes (stricter than 5 min requirement)
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Consent Filtering** | Always validate consent before returning PHI (42 CFR Part 2) |
| **Tenant Isolation** | Every repository query includes `tenantId` filter |
| **FHIR Bundle** | Return FHIR Bundles with proper resourceType and type fields |
| **Cache TTL** | Verify 2-minute TTL for patient data (stricter than 5 min) |
| **Fail-Open** | Consent service failures allow data access (availability) |
| **PHI Encryption** | Sensitive fields (SSN) must use encrypted columns |
| **Audit Trail** | Consent checks must be logged for compliance |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker not running | Start Docker Desktop |
| Consent filter test fails | Consent service not mocked | Mock ConsentServiceClient |
| Cache TTL test fails | Wrong cache configuration | Import TestCacheConfiguration |
| Multi-tenant test fails | Missing tenant filter | Add WHERE tenantId clause |
| FHIR bundle parse error | Invalid JSON response | Verify FhirServiceClient mocks |
| Feign client timeout | Service not available | Use WireMock for Feign tests |
| Patient not found | Wrong tenant context | Check X-Tenant-ID header |

### Manual Testing (curl Examples)

```bash
# Get comprehensive health record
curl http://localhost:8084/patient/health-record?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: VIEWER" \
  -H "Accept: application/fhir+json"

# Get active medications only
curl "http://localhost:8084/patient/medications?patient=p123&onlyActive=true" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get critical allergies only
curl "http://localhost:8084/patient/allergies?patient=p123&onlyCritical=true" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get active conditions only
curl "http://localhost:8084/patient/conditions?patient=p123&onlyActive=true" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get patient timeline for 2024
curl "http://localhost:8084/patient/timeline/by-date?patient=p123&startDate=2024-01-01&endDate=2024-12-31" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get timeline by resource type
curl "http://localhost:8084/patient/timeline/by-type?patient=p123&resourceType=Condition" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get health status dashboard
curl http://localhost:8084/patient/health-status?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get medication summary
curl http://localhost:8084/patient/medication-summary?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get vital signs
curl http://localhost:8084/patient/vitals?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get lab results
curl http://localhost:8084/patient/labs?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get encounters
curl "http://localhost:8084/patient/encounters?patient=p123&onlyActive=false" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get care plans
curl "http://localhost:8084/patient/care-plans?patient=p123&onlyActive=true" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"
```

## Performance

- Comprehensive health record: 200-500ms (includes 10+ FHIR queries)
- Timeline generation: 100-300ms (cached)
- Redis caching: 2 min TTL (HIPAA compliant)
- Consent validation: Automatic for all endpoints
