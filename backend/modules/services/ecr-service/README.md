# Electronic Case Reporting Service

Automated public health reporting service implementing CDC electronic Initial Case Report (eICR) submission for reportable conditions.

## Purpose

The ECR Service automates the detection, generation, and submission of electronic case reports to public health agencies. It monitors clinical data for Reportable Condition Trigger Codes (RCTC) and automatically generates eICR documents compliant with HL7 CDA R2 standards for submission to the AIMS Platform.

## Key Features

- **RCTC Rules Engine**: Real-time monitoring of reportable condition trigger codes
- **Automated eICR Generation**: HL7 CDA R2-compliant eICR document creation
- **AIMS Platform Integration**: Direct submission to CDC AIMS (Association of Immunization Managers Information System)
- **Urgency-based Processing**: Immediate, 24-hour, and 72-hour reporting based on condition severity
- **Trigger Categories**: Monitors diagnosis codes, lab results, medications, and procedures
- **Multi-tenant Support**: Complete tenant isolation for healthcare organizations
- **Retry Logic**: Automatic retry with exponential backoff for failed submissions
- **Status Tracking**: Complete audit trail from trigger detection to acknowledgment
- **Weekly RCTC Updates**: Automated value set updates from CDC sources

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ecr` | List eCRs with pagination and status filtering |
| GET | `/api/ecr/{ecrId}` | Get specific eCR by ID |
| GET | `/api/ecr/patient/{patientId}` | Get all eCRs for a patient |
| POST | `/api/ecr/evaluate` | Manually evaluate clinical codes for reportability |
| POST | `/api/ecr/{ecrId}/reprocess` | Reprocess a failed eCR |
| POST | `/api/ecr/{ecrId}/cancel` | Cancel a pending eCR |
| GET | `/api/ecr/summary` | Get eCR status summary for dashboard |
| GET | `/api/ecr/conditions` | Get list of monitored reportable conditions |
| GET | `/api/ecr/check-trigger` | Check if a code triggers reportable condition |

## Configuration

### Application Properties

```yaml
server:
  port: 8101

ecr:
  aims:
    enabled: false
    base-url: https://ecr.aimsplatform.org/api
    client-id: ${ECR_AIMS_CLIENT_ID}
    client-secret: ${ECR_AIMS_CLIENT_SECRET}
    retry:
      max-attempts: 3
      initial-interval-ms: 1000
      multiplier: 2.0

  rctc:
    value-set-update-cron: "0 0 2 * * SUN"  # Weekly Sunday 2 AM
    cache-ttl-hours: 168  # 7 days

  eicr:
    author-organization: HealthData-in-Motion
    custodian-oid: 2.16.840.1.113883.3.xxx
    include-phi: true

  triggers:
    categories:
      - diagnosis
      - lab_result
      - medication
      - procedure
```

### Urgency Levels

```yaml
ecr:
  urgency:
    immediate:
      - Anthrax
      - Botulism
      - Plague
      - Smallpox
      - Viral Hemorrhagic Fever
    within-24-hours:
      - Measles
      - Pertussis
      - Meningococcal Disease
    within-72-hours:
      - Hepatitis A
      - Salmonellosis
      - Tuberculosis
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: healthdata
    password: ${DB_PASSWORD}
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: ecr
```

## Reportable Conditions

The RCTC rules engine monitors for these condition categories:
- Communicable diseases (measles, tuberculosis, COVID-19, etc.)
- Foodborne illnesses (salmonella, E. coli, listeria, etc.)
- Vaccine-preventable diseases (pertussis, mumps, rubella, etc.)
- Sexually transmitted infections (HIV, syphilis, gonorrhea, etc.)
- Zoonotic diseases (rabies, anthrax, plague, etc.)
- Emerging threats (novel influenza, Ebola, Zika, etc.)

## eCR Status Lifecycle

1. **PENDING**: Trigger detected, eICR generation queued
2. **GENERATING**: eICR document being created
3. **READY**: eICR ready for submission
4. **SUBMITTED**: eICR submitted to AIMS Platform
5. **ACKNOWLEDGED**: Public health agency acknowledged receipt
6. **FAILED**: Submission failed (eligible for reprocessing)
7. **CANCELLED**: Manually cancelled before submission

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:ecr-service:bootRun
```

## Testing

### Overview

The ECR Service has comprehensive test coverage validating:
- **RCTC Trigger Detection** - Reportable Condition Trigger Code evaluation (diagnosis, lab, medication, procedure)
- **eICR Document Generation** - FHIR Bundle creation conforming to HL7 eCR Implementation Guide
- **API Endpoints** - Full REST API workflow (list, evaluate, cancel, reprocess)
- **Multi-Tenant Isolation** - Complete tenant data separation
- **Urgency Level Processing** - IMMEDIATE, WITHIN_24_HOURS, WITHIN_72_HOURS, ROUTINE handling
- **HIPAA Compliance** - PHI protection in eCR documents and caching

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:ecr-service:test

# Run specific test suite
./gradlew :modules:services:ecr-service:test --tests "*TriggerServiceTest"
./gradlew :modules:services:ecr-service:test --tests "*GeneratorServiceTest"
./gradlew :modules:services:ecr-service:test --tests "*IntegrationTest"

# Run with coverage
./gradlew :modules:services:ecr-service:test jacocoTestReport

# Run integration tests only (requires Docker)
./gradlew :modules:services:ecr-service:integrationTest
```

### Test Coverage Summary

| Test Class | Tests | Focus Area |
|------------|-------|------------|
| `EcrTriggerServiceTest` | 13+ | RCTC rules engine, trigger detection, eCR creation |
| `EicrGeneratorServiceTest` | 24+ | FHIR Bundle generation, eICR profiles, resource types |
| `EcrApiIntegrationTest` | 22+ | API endpoints, status workflow, tenant isolation |

**Total: 59+ test methods covering trigger detection, document generation, and API operations**

### Test Organization

```
src/test/java/com/healthdata/ecr/
├── service/
│   ├── EcrTriggerServiceTest.java          # RCTC trigger detection
│   └── EicrGeneratorServiceTest.java       # eICR FHIR Bundle generation
└── integration/
    └── EcrApiIntegrationTest.java          # API endpoint integration tests
```

### Unit Tests (EcrTriggerServiceTest)

Tests the RCTC (Reportable Condition Trigger Codes) rules engine for detecting reportable conditions:

```java
@ExtendWith(MockitoExtension.class)
class EcrTriggerServiceTest {

    @Mock
    private RctcRulesEngine rctcRulesEngine;

    @Mock
    private ElectronicCaseReportRepository ecrRepository;

    @Mock
    private EcrProcessingService processingService;

    @InjectMocks
    private EcrTriggerService ecrTriggerService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create eCR for reportable diagnosis")
    void handleConditionCreated_withReportableDiagnosis_shouldCreateEcr() {
        // Arrange
        Map<String, Object> event = createConditionEvent("U07.1", "COVID-19");

        TriggerMatch match = TriggerMatch.builder()
            .code("U07.1")
            .codeSystem(RctcRulesEngine.ICD10CM_OID)
            .display("COVID-19")
            .conditionName("COVID-19")
            .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
            .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
            .build();

        when(rctcRulesEngine.isReportableTrigger(eq("U07.1"), anyString())).thenReturn(true);
        when(rctcRulesEngine.evaluateDiagnosis("U07.1")).thenReturn(Optional.of(match));
        when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
            .thenAnswer(inv -> {
                ElectronicCaseReportEntity ecr = inv.getArgument(0);
                ecr.setId(UUID.randomUUID());
                return ecr;
            });

        // Act
        ecrTriggerService.handleConditionCreated(event);

        // Assert
        ArgumentCaptor<ElectronicCaseReportEntity> captor = ArgumentCaptor.forClass(ElectronicCaseReportEntity.class);
        verify(ecrRepository).save(captor.capture());

        ElectronicCaseReportEntity savedEcr = captor.getValue();
        assertThat(savedEcr.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedEcr.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(savedEcr.getTriggerCode()).isEqualTo("U07.1");
        assertThat(savedEcr.getTriggerCategory()).isEqualTo(TriggerCategory.DIAGNOSIS);
        assertThat(savedEcr.getConditionName()).isEqualTo("COVID-19");
        assertThat(savedEcr.getStatus()).isEqualTo(EcrStatus.PENDING);
        assertThat(savedEcr.getUrgency()).isEqualTo(EcrUrgency.WITHIN_24_HOURS);
    }
}
```

**Key Test Areas - Trigger Detection:**

| Test Scenario | Validation |
|---------------|------------|
| Reportable diagnosis | Creates eCR with correct trigger category and urgency |
| Non-reportable diagnosis | No eCR created (e.g., common cold J06.9) |
| Duplicate within window | Skip duplicate eCR creation |
| IMMEDIATE urgency | Triggers immediate processing (Anthrax, Botulism, etc.) |
| Reportable lab result | Creates eCR with LAB_RESULT category |
| Non-laboratory observation | Ignores vital signs observations |
| Multiple trigger matches | Creates separate eCR for each match |
| Urgency mapping | Correct mapping from RCTC to ECR urgency levels |

### Unit Tests (EicrGeneratorServiceTest)

Tests eICR (electronic Initial Case Report) FHIR Bundle generation:

```java
@ExtendWith(MockitoExtension.class)
class EicrGeneratorServiceTest {

    @InjectMocks
    private EicrGeneratorService eicrGeneratorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eicrGeneratorService, "authorOrganization", "Test Health System");
        ReflectionTestUtils.setField(eicrGeneratorService, "custodianOid", "2.16.840.1.113883.3.12345");
    }

    @Test
    @DisplayName("Should generate valid FHIR Bundle")
    void generateEicr_shouldCreateValidBundle() {
        // Arrange
        ElectronicCaseReportEntity ecr = createDiagnosisEcr();
        PatientData patientData = createPatientData();
        EncounterData encounterData = createEncounterData();
        TriggerData triggerData = createTriggerData();

        // Act
        Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

        // Assert
        assertThat(bundle).isNotNull();
        assertThat(bundle.getId()).isNotNull();
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.DOCUMENT);
        assertThat(bundle.getTimestamp()).isNotNull();
        assertThat(bundle.getEntry()).isNotEmpty();
    }

    @Test
    @DisplayName("Should include all required resources")
    void generateEicr_shouldIncludeRequiredResources() {
        // Arrange
        ElectronicCaseReportEntity ecr = createDiagnosisEcr();
        PatientData patientData = createPatientData();
        EncounterData encounterData = createEncounterData();
        TriggerData triggerData = createTriggerData();

        // Act
        Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

        // Assert - Check for required resource types
        boolean hasComposition = false;
        boolean hasPatient = false;
        boolean hasPractitioner = false;
        boolean hasOrganization = false;
        boolean hasEncounter = false;
        boolean hasCondition = false;

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            if (resource instanceof Composition) hasComposition = true;
            if (resource instanceof Patient) hasPatient = true;
            if (resource instanceof Practitioner) hasPractitioner = true;
            if (resource instanceof Organization) hasOrganization = true;
            if (resource instanceof Encounter) hasEncounter = true;
            if (resource instanceof Condition) hasCondition = true;
        }

        assertThat(hasComposition).isTrue();
        assertThat(hasPatient).isTrue();
        assertThat(hasPractitioner).isTrue();
        assertThat(hasOrganization).isTrue();
        assertThat(hasEncounter).isTrue();
        assertThat(hasCondition).isTrue();
    }

    @Test
    @DisplayName("Composition should have LOINC document type code")
    void generateEicr_compositionShouldHaveTypeCode() {
        // Arrange
        ElectronicCaseReportEntity ecr = createDiagnosisEcr();
        PatientData patientData = createPatientData();
        TriggerData triggerData = createTriggerData();

        // Act
        Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);
        Composition composition = (Composition) bundle.getEntry().get(0).getResource();

        // Assert - LOINC code 55751-2 for Public health Case report
        assertThat(composition.getType().getCoding()).isNotEmpty();
        assertThat(composition.getType().getCoding().get(0).getCode()).isEqualTo("55751-2");
    }
}
```

**Key Test Areas - eICR Generation:**

| Test Category | Validations |
|---------------|-------------|
| **Bundle Structure** | DOCUMENT type, timestamp, non-empty entries |
| **Composition** | eICR profile URL, LOINC 55751-2 type code, Reason for Report section |
| **Patient Resource** | us-ph-patient profile, demographics, address |
| **Trigger Categories** | DIAGNOSIS→Condition, LAB_RESULT→Observation, MEDICATION→MedicationAdministration, PROCEDURE→Procedure |
| **Encounter** | Class code (EMER/IMP/AMB), period with start/end times |
| **Full URLs** | All entries have urn:uuid: prefix |
| **Optional Data** | Works without encounter data |
| **Organization** | Author and custodian organizations included |

### Integration Tests (EcrApiIntegrationTest)

Tests full API workflow with TestContainers PostgreSQL:

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.default_schema=ecr"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EcrApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("ecrtestdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElectronicCaseReportRepository ecrRepository;

    @Autowired
    private RctcTriggerCodeRepository triggerCodeRepository;

    @BeforeEach
    void setUp() {
        seedRctcTriggerCodes();
    }

    @Test
    @DisplayName("Should return paginated list of eCRs")
    void listEcrs_shouldReturnPaginatedResults() throws Exception {
        createTestEcr(TENANT_ID, PATIENT_ID, EcrStatus.PENDING);

        mockMvc.perform(get("/api/ecr")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should create eCRs for reportable diagnosis codes")
    void evaluateCodes_withReportableDiagnosis_shouldCreateEcrs() throws Exception {
        EvaluationRequest request = new EvaluationRequest();
        request.setPatientId(UUID.randomUUID());
        request.setDiagnosisCodes(Arrays.asList("U07.1", "B05.9")); // COVID-19 and Measles

        mockMvc.perform(post("/api/ecr/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should assign IMMEDIATE urgency for anthrax")
    void evaluate_anthrax_shouldBeImmediate() throws Exception {
        EvaluationRequest request = new EvaluationRequest();
        request.setPatientId(UUID.randomUUID());
        request.setDiagnosisCodes(Arrays.asList("A22.9")); // Anthrax

        mockMvc.perform(post("/api/ecr/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].urgency").value("IMMEDIATE"));
    }

    private void seedRctcTriggerCodes() {
        if (triggerCodeRepository.count() == 0) {
            triggerCodeRepository.saveAll(Arrays.asList(
                RctcTriggerCodeEntity.builder()
                    .code("U071")
                    .codeSystem("2.16.840.1.113883.6.90")
                    .display("COVID-19")
                    .triggerType(TriggerType.DIAGNOSIS)
                    .conditionName("COVID-19")
                    .urgency(Urgency.WITHIN_24_HOURS)
                    .isActive(true)
                    .build(),
                RctcTriggerCodeEntity.builder()
                    .code("A229")
                    .codeSystem("2.16.840.1.113883.6.90")
                    .display("Anthrax")
                    .triggerType(TriggerType.DIAGNOSIS)
                    .conditionName("Anthrax")
                    .urgency(Urgency.IMMEDIATE)
                    .isActive(true)
                    .build()
            ));
        }
    }
}
```

**Key Test Areas - API Integration:**

| Endpoint | Test Coverage |
|----------|---------------|
| `GET /api/ecr` | Pagination, status filtering |
| `GET /api/ecr/{ecrId}` | Retrieve by ID, 404 for non-existent |
| `GET /api/ecr/patient/{patientId}` | List patient eCRs, empty for no eCRs |
| `POST /api/ecr/evaluate` | Create eCRs for reportable codes, empty for non-reportable |
| `POST /api/ecr/{ecrId}/cancel` | Cancel PENDING, 400 for SUBMITTED |
| `POST /api/ecr/{ecrId}/reprocess` | Requeue FAILED, 400 for non-FAILED |
| `GET /api/ecr/summary` | Status counts for dashboard |
| `GET /api/ecr/conditions` | Monitored condition list |
| `GET /api/ecr/check-trigger` | Reportability check with urgency |

### Multi-Tenant Isolation Tests

Tests ensure complete tenant data separation:

```java
@Nested
@DisplayName("Tenant isolation tests")
class TenantIsolationTests {

    @Test
    @DisplayName("Should not return other tenant's eCRs")
    void getEcr_differentTenant_shouldReturn404() throws Exception {
        ElectronicCaseReportEntity ecr = createTestEcr("tenant1", UUID.randomUUID(), EcrStatus.PENDING);

        mockMvc.perform(get("/api/ecr/{ecrId}", ecr.getId())
                .header("X-Tenant-ID", "tenant2"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should not return other tenant's patient eCRs")
    void getPatientEcrs_differentTenant_shouldReturnEmptyList() throws Exception {
        UUID patientId = UUID.randomUUID();
        createTestEcr("tenant1", patientId, EcrStatus.PENDING);

        mockMvc.perform(get("/api/ecr/patient/{patientId}", patientId)
                .header("X-Tenant-ID", "tenant2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
```

### HIPAA Compliance Tests

Tests PHI handling in eCR documents:

```java
@Nested
@DisplayName("HIPAA Compliance tests")
class HipaaComplianceTests {

    @Test
    @DisplayName("eCR responses must include no-cache headers")
    void ecrResponses_shouldIncludeNoCacheHeaders() throws Exception {
        ElectronicCaseReportEntity ecr = createTestEcr(TENANT_ID, PATIENT_ID, EcrStatus.PENDING);

        mockMvc.perform(get("/api/ecr/{ecrId}", ecr.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control",
                containsString("no-store")));
    }

    @Test
    @DisplayName("PHI cache TTL must not exceed 5 minutes")
    void phiCacheTtl_shouldBeCompliant() {
        // Verify RCTC cache TTL is 7 days (non-PHI)
        // Verify eCR cache TTL is <= 5 minutes (contains PHI)
        Cache ecrCache = cacheManager.getCache("ecr-documents");
        assertThat(ecrCache).isNotNull();

        if (ecrCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) ecrCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("PHI cache TTL exceeds 5 minutes");
        }
    }

    @Test
    @DisplayName("eICR Bundle should include patient demographics")
    void eicrBundle_shouldIncludePatientDemographics() {
        Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

        Patient patient = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Patient)
            .map(e -> (Patient) e.getResource())
            .findFirst()
            .orElseThrow();

        // Verify PHI is properly included (required for public health reporting)
        assertThat(patient.getName()).isNotEmpty();
        assertThat(patient.getBirthDate()).isNotNull();
        assertThat(patient.getAddress()).isNotEmpty();
    }
}
```

### Performance Tests

Benchmarks for eCR processing latency:

```java
@Nested
@DisplayName("Performance tests")
class PerformanceTests {

    @Test
    @DisplayName("RCTC trigger evaluation should complete within 50ms")
    void triggerEvaluation_shouldBePerformant() {
        // Given
        int evaluationCount = 100;
        List<String> codes = Arrays.asList("U07.1", "B05.9", "A22.9");

        // When
        Instant start = Instant.now();
        for (int i = 0; i < evaluationCount; i++) {
            for (String code : codes) {
                rctcRulesEngine.isReportableTrigger(code, "2.16.840.1.113883.6.90");
            }
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerEvaluation = totalMs / (double) (evaluationCount * codes.size());

        assertThat(avgMsPerEvaluation)
            .isLessThan(50.0)
            .withFailMessage("Avg evaluation time %.2fms exceeds 50ms target", avgMsPerEvaluation);
    }

    @Test
    @DisplayName("eICR Bundle generation should complete within 500ms")
    void bundleGeneration_shouldBePerformant() {
        // Given
        int generationCount = 50;

        // When
        Instant start = Instant.now();
        for (int i = 0; i < generationCount; i++) {
            eicrGeneratorService.generateEicr(
                createDiagnosisEcr(),
                createPatientData(),
                createEncounterData(),
                createTriggerData()
            );
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerBundle = totalMs / (double) generationCount;

        assertThat(avgMsPerBundle)
            .isLessThan(500.0)
            .withFailMessage("Avg bundle generation time %.2fms exceeds 500ms target", avgMsPerBundle);

        System.out.printf("Performance: %d bundles in %dms (avg: %.2fms)%n",
            generationCount, totalMs, avgMsPerBundle);
    }
}
```

### Test Configuration

**TestContainers PostgreSQL Setup:**

```java
@Testcontainers(disabledWithoutDocker = true)
class EcrApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("ecrtestdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
```

**RCTC Trigger Code Seeding:**

```java
private void seedRctcTriggerCodes() {
    // Note: ICD-10 codes stored normalized (without dots) for lookup
    if (triggerCodeRepository.count() == 0) {
        List<RctcTriggerCodeEntity> codes = Arrays.asList(
            RctcTriggerCodeEntity.builder()
                .code("U071")  // Normalized from U07.1
                .codeSystem("2.16.840.1.113883.6.90")  // ICD-10-CM OID
                .display("COVID-19")
                .triggerType(TriggerType.DIAGNOSIS)
                .conditionName("COVID-19")
                .urgency(Urgency.WITHIN_24_HOURS)
                .isActive(true)
                .build(),
            RctcTriggerCodeEntity.builder()
                .code("94500-6")  // LOINC code as-is
                .codeSystem("2.16.840.1.113883.6.1")  // LOINC OID
                .display("SARS-CoV-2 RNA")
                .triggerType(TriggerType.LAB_RESULT)
                .conditionName("COVID-19")
                .urgency(Urgency.WITHIN_24_HOURS)
                .isActive(true)
                .build()
        );
        triggerCodeRepository.saveAll(codes);
    }
}
```

### Best Practices

| Category | Best Practice |
|----------|---------------|
| **RCTC Codes** | Use OID constants: ICD10CM_OID, LOINC_OID, RXNORM_OID, SNOMED_OID |
| **Code Normalization** | ICD-10 codes stored without dots (U07.1 → U071) for consistent lookup |
| **Urgency Levels** | Test all 4 levels: IMMEDIATE, WITHIN_24_HOURS, WITHIN_72_HOURS, ROUTINE |
| **Trigger Categories** | Test all 4 categories: DIAGNOSIS, LAB_RESULT, MEDICATION, PROCEDURE |
| **Bundle Validation** | Verify DOCUMENT type, urn:uuid: full URLs, Composition as first entry |
| **Profile URLs** | Validate eicr-composition and us-ph-patient profile meta |
| **LOINC Code** | Verify Composition type code 55751-2 (Public health Case report) |
| **Duplicate Prevention** | Test existsPendingForTrigger window check |
| **Status Lifecycle** | Test valid transitions: PENDING→CANCELLED, FAILED→reprocess |
| **Tenant Isolation** | Always include X-Tenant-ID header, verify cross-tenant denial |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `RctcRulesEngine returns false` | Code not normalized or not in RCTC value set | Normalize ICD-10 (remove dots), verify code exists in trigger repository |
| `Bundle missing Composition` | Generator failed to create Composition first | Check eicrGeneratorService creates Composition before other resources |
| `Wrong trigger category` | Observation category not "laboratory" | Verify observation.category = "laboratory" for lab results |
| `Cancel fails with 400` | eCR already SUBMITTED or ACKNOWLEDGED | Only PENDING/READY/GENERATING can be cancelled |
| `Reprocess fails with 400` | eCR not in FAILED status | Only FAILED eCRs can be reprocessed |
| `Tenant isolation failure` | Missing tenantId in repository query | Ensure all queries include tenantId filter |
| `IMMEDIATE not processed` | processingService.processImmediately() not called | Verify IMMEDIATE urgency triggers immediate processing |
| `eICR missing Patient address` | PatientData.address is null | Include AddressData in PatientData builder |
| `TestContainers timeout` | Docker not running or slow | Start Docker Desktop, increase timeout |
| `RCTC seed data missing` | setUp() not called before test | Ensure @BeforeEach seeds trigger codes |

### Building

```bash
./gradlew :modules:services:ecr-service:build
```

## Example Usage

### Evaluate Clinical Codes

```bash
curl -X POST http://localhost:8101/api/ecr/evaluate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "diagnosisCodes": ["A01.0", "B20"],
    "labCodes": ["94500-6"]
  }'
```

### Check Trigger Code

```bash
curl "http://localhost:8101/api/ecr/check-trigger?code=A01.0&codeSystem=2.16.840.1.113883.6.90"
```

## Integration

The ECR Service integrates with:
- **FHIR Service**: Retrieves patient clinical data
- **Patient Service**: Gets patient demographics
- **AIMS Platform**: Submits eICR documents to public health
- **Event Router**: Publishes eCR lifecycle events

## Standards Compliance

- HL7 CDA R2 for eICR documents
- HL7 FHIR R4 for data retrieval
- CDC RCTC value sets
- ICD-10-CM, LOINC, RxNorm code systems

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
