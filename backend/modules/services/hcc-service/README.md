# HCC Service

Hierarchical Condition Category (HCC) risk adjustment service for Medicare Advantage RAF score calculation and documentation gap management.

## Purpose

The HCC Service calculates Risk Adjustment Factor (RAF) scores using CMS HCC models V24 and V28 for Medicare Advantage plans. It provides ICD-10 to HCC crosswalk mapping, blended RAF calculations during transition years, documentation gap identification, and high-value opportunity reporting to maximize accurate risk capture and reimbursement.

## Key Features

- **Dual Model Support**: CMS HCC V24 and V28 model implementations
- **Blended RAF Calculation**: 2024 blending (67% V28 / 33% V24) per CMS guidelines
- **ICD-10 Crosswalk**: Complete ICD-10-CM to HCC mapping for both models
- **RAF Score Calculation**: Patient-level and population-level RAF scoring
- **Demographic Factors**: Age, sex, dual eligibility, institutional status adjustments
- **HCC Hierarchies**: Automatic hierarchy suppression rules
- **Disease Interactions**: Capture HCC interaction terms (diabetes + CHF, etc.)
- **Documentation Gaps**: Identify missing HCC documentation from prior year
- **High-Value Opportunities**: Rank patients by potential RAF uplift
- **Patient HCC Profiles**: Historical tracking of captured HCCs by year
- **Multi-tenant Support**: Complete tenant isolation and security

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/hcc/api/v1/hcc/patient/{patientId}/calculate` | Calculate RAF score for patient |
| GET | `/hcc/api/v1/hcc/patient/{patientId}/profile` | Get patient HCC profile and history |
| GET | `/hcc/api/v1/hcc/crosswalk` | Get ICD-10 to HCC mappings (batch) |
| GET | `/hcc/api/v1/hcc/patient/{patientId}/documentation-gaps` | Get documentation gaps |
| GET | `/hcc/api/v1/hcc/opportunities` | Get high-value RAF uplift opportunities |

## Configuration

### Application Properties

```yaml
server:
  port: 8105
  servlet:
    context-path: /hcc

hcc:
  model:
    year: 2024
    v24-weight: 0.33
    v28-weight: 0.67
  crosswalk:
    data-path: classpath:data/hcc-crosswalk/
```

### Service Integration

```yaml
fhir:
  server:
    url: http://localhost:8085/fhir

patient:
  service:
    url: http://localhost:8084/patient

care-gap:
  service:
    url: http://localhost:8086/care-gap
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_cql
    username: healthdata
    password: ${DB_PASSWORD}
```

### Cache

```yaml
spring.cache:
  type: redis
  redis:
    time-to-live: 3600000  # 1 hour
```

## HCC Models

### CMS-HCC V24 (Legacy)
- 79 HCC categories
- Community and institutional models
- Used through 2023
- 33% weight in 2024 blending

### CMS-HCC V28 (Current)
- 115 HCC categories (expanded from V24)
- Improved clinical granularity
- Enhanced disease capture
- 67% weight in 2024 blending
- 100% weight starting 2025

## RAF Score Components

### Demographic Factors
- Age (categorical: 0-34, 35-44, 45-54, 55-59, 60-64, 65-69, 70-74, 75-79, 80-84, 85-89, 90-94, 95+)
- Sex (Male/Female)
- Dual eligibility (Medicaid eligible)
- Institutional status (nursing home resident)
- Originally disabled status

### Disease Factors
- Captured HCCs from diagnosis codes
- HCC hierarchies (higher severity suppresses lower)
- Disease interactions (e.g., Diabetes + CHF)

### Blended Score (2024)
```
Blended RAF = (V24 RAF × 0.33) + (V28 RAF × 0.67)
```

## Documentation Gaps

Documentation gaps are identified when:
- HCC was captured in prior year but not current year
- Chronic condition expected to persist
- Significant RAF impact if recaptured
- Annual documentation requirement not met

### Gap Types
- **Suspected Recapture**: Prior year HCC not documented in current year
- **High Impact**: HCC with coefficient > 0.5
- **Care Opportunity**: Clinical intervention recommended
- **Chart Review**: Medical record review needed

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:hcc-service:bootRun
```

## Testing

The HCC Service has comprehensive test coverage across 5 test files with 68+ test methods covering unit tests for RAF calculation, documentation gap detection, recapture tracking, controller endpoints, and full API integration tests.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:hcc-service:test

# Run specific test class
./gradlew :modules:services:hcc-service:test --tests "RafCalculationServiceTest"

# Run tests by category
./gradlew :modules:services:hcc-service:test --tests "*ServiceTest"
./gradlew :modules:services:hcc-service:test --tests "*ControllerTest"
./gradlew :modules:services:hcc-service:test --tests "*IntegrationTest"

# Run with coverage report
./gradlew :modules:services:hcc-service:test jacocoTestReport

# Run integration tests only (requires Docker)
./gradlew :modules:services:hcc-service:integrationTest
```

### Test Coverage Summary

| Test Category | Test Files | Test Methods | Coverage Areas |
|--------------|------------|--------------|----------------|
| RAF Calculation | 1 | 14 | Score calculation, HCC hierarchies, crosswalk |
| Documentation Gaps | 1 | 14 | Gap detection, lifecycle, specificity |
| Recapture Tracking | 1 | 14 | Opportunity detection, worklists, rates |
| Controller | 1 | 14 | REST API endpoints |
| Integration | 1 | 12 | Full API with TestContainers |
| **Total** | **5** | **68+** | **Full HCC service coverage** |

### Test Organization

```
src/test/java/com/healthdata/hcc/
├── service/
│   ├── RafCalculationServiceTest.java      # RAF score calculation (14 tests)
│   ├── DocumentationGapServiceTest.java    # Gap detection (14 tests)
│   └── RecaptureTrackingServiceTest.java   # Recapture tracking (14 tests)
├── controller/
│   └── HccControllerTest.java              # REST API endpoints (14 tests)
└── integration/
    └── HccApiIntegrationTest.java          # Full API integration (12 tests)
```

---

### Unit Tests (Service Layer)

#### RafCalculationServiceTest - RAF Score Calculation

Tests for RAF calculation logic, HCC mapping, hierarchy suppression, and model blending.

```java
@ExtendWith(MockitoExtension.class)
class RafCalculationServiceTest {

    @Mock
    private DiagnosisHccMapRepository diagnosisMapRepository;

    @Mock
    private PatientHccProfileRepository profileRepository;

    @InjectMocks
    private RafCalculationService rafCalculationService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Set default blending weights (2024: 33% V24, 67% V28)
        ReflectionTestUtils.setField(rafCalculationService, "v24Weight", 0.33);
        ReflectionTestUtils.setField(rafCalculationService, "v28Weight", 0.67);
    }

    @Nested
    @DisplayName("calculateRaf() tests")
    class CalculateRafTests {

        @Test
        @DisplayName("Should calculate RAF with single diagnosis code")
        void calculateRaf_withSingleDiagnosis_shouldReturnScores() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.9"); // Diabetes
            DemographicFactors factors = createDefaultDemographicFactors();

            DiagnosisHccMapEntity mapping = DiagnosisHccMapEntity.builder()
                .icd10Code("E11.9")
                .hccCodeV24("HCC19")
                .hccCodeV28("HCC37")
                .hccNameV24("Diabetes without Complication")
                .build();

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(List.of(mapping));

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(result.getRafScoreV24()).isNotNull();
            assertThat(result.getRafScoreV28()).isNotNull();
            assertThat(result.getRafScoreBlended()).isNotNull();
            assertThat(result.getHccsV24()).contains("HCC19");
            assertThat(result.getHccsV28()).contains("HCC37");
        }

        @Test
        @DisplayName("Should apply blended weights correctly")
        void calculateRaf_shouldApplyBlendedWeights() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.9");
            DemographicFactors factors = createDefaultDemographicFactors();

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert
            assertThat(result.getV24Weight()).isEqualTo(0.33);
            assertThat(result.getV28Weight()).isEqualTo(0.67);

            // Blended = V24 * 0.33 + V28 * 0.67
            BigDecimal expectedBlended = result.getRafScoreV24().multiply(BigDecimal.valueOf(0.33))
                .add(result.getRafScoreV28().multiply(BigDecimal.valueOf(0.67)));
            assertThat(result.getRafScoreBlended().doubleValue())
                .isCloseTo(expectedBlended.doubleValue(), Offset.offset(0.00001));
        }
    }

    @Nested
    @DisplayName("HCC Hierarchy tests")
    class HccHierarchyTests {

        @Test
        @DisplayName("Should apply diabetes hierarchy - HCC17 trumps HCC19")
        void calculateRaf_withDiabetesHierarchy_shouldKeepHighestSeverity() {
            // Arrange - E11.65 (complications) + E11.9 (without)
            List<String> diagnosisCodes = List.of("E11.65", "E11.9");
            DemographicFactors factors = createDefaultDemographicFactors();

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.65")
                    .hccCodeV24("HCC17") // Acute complications - higher severity
                    .hccCodeV28("HCC17")
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.9")
                    .hccCodeV24("HCC19") // Without complications - lower severity
                    .hccCodeV28("HCC37")
                    .build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(mappings);

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert - HCC17 should suppress HCC19 in V24
            assertThat(result.getHccsV24()).contains("HCC17");
            assertThat(result.getHccsV24()).doesNotContain("HCC19");
        }

        @Test
        @DisplayName("Should apply cancer hierarchy - HCC8 trumps HCC9")
        void calculateRaf_withCancerHierarchy_shouldKeepMetastatic() {
            // Arrange - Metastatic cancer suppresses primary lung cancer
            List<String> diagnosisCodes = List.of("C78.0", "C34.1");

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("C78.0")
                    .hccCodeV24("HCC8") // Metastatic (highest severity)
                    .hccCodeV28("HCC8")
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("C34.1")
                    .hccCodeV24("HCC9") // Lung primary (lower severity)
                    .hccCodeV28("HCC9")
                    .build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(mappings);

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, createDefaultDemographicFactors());

            // Assert - HCC8 should suppress HCC9
            assertThat(result.getHccsV24()).contains("HCC8");
            assertThat(result.getHccsV24()).doesNotContain("HCC9");
        }
    }

    private DemographicFactors createDefaultDemographicFactors() {
        return DemographicFactors.builder()
            .age(65)
            .sex("M")
            .dualEligible(false)
            .institutionalized(false)
            .medicaidStatus("FULL")
            .originalReasonForEntitlement("AGE")
            .build();
    }
}
```

#### DocumentationGapServiceTest - Gap Detection

Tests for documentation gap detection, V28 transition gaps, and specificity opportunities.

```java
@ExtendWith(MockitoExtension.class)
class DocumentationGapServiceTest {

    @Mock
    private DiagnosisHccMapRepository diagnosisMapRepository;

    @Mock
    private DocumentationGapRepository gapRepository;

    @InjectMocks
    private DocumentationGapService documentationGapService;

    @Nested
    @DisplayName("analyzeDocumentationGaps() tests")
    class AnalyzeDocumentationGapsTests {

        @Test
        @DisplayName("Should detect unspecified diabetes gap")
        void analyzeGaps_withUnspecifiedDiabetes_shouldCreateGap() {
            // Arrange
            List<String> diagnoses = List.of("E11.9"); // Unspecified diabetes
            DiagnosisHccMapEntity mapping = createDiagnosisMapping(
                "E11.9", "HCC19", "HCC37", true);

            when(diagnosisMapRepository.findByIcd10Codes(diagnoses))
                .thenReturn(List.of(mapping));
            when(gapRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            List<DocumentationGapEntity> gaps = documentationGapService
                .analyzeDocumentationGaps(TENANT_ID, PATIENT_ID, diagnoses, 2025);

            // Assert
            assertThat(gaps).isNotEmpty();
            assertThat(gaps).anyMatch(g ->
                g.getGapType() == DocumentationGapEntity.GapType.UNSPECIFIED);
        }

        @Test
        @DisplayName("Should detect V28 transition gap")
        void analyzeGaps_withV28Change_shouldCreateTransitionGap() {
            // Arrange - Code that changed mapping in V28
            DiagnosisHccMapEntity mapping = DiagnosisHccMapEntity.builder()
                .icd10Code("I10.0")
                .hccCodeV24("HCC85")
                .hccCodeV28(null) // No longer maps in V28
                .changedInV28(true)
                .v28ChangeDescription("Removed from HCC in V28")
                .coefficientV24(0.302)
                .build();

            when(diagnosisMapRepository.findByIcd10Codes(anyList()))
                .thenReturn(List.of(mapping));

            // Act
            List<DocumentationGapEntity> gaps = documentationGapService
                .analyzeDocumentationGaps(TENANT_ID, PATIENT_ID, List.of("I10.0"), 2025);

            // Assert
            assertThat(gaps).anyMatch(g ->
                g.getGapType() == DocumentationGapEntity.GapType.V28_SPECIFICITY);
            assertThat(gaps).anyMatch(g -> g.getPriority().equals("HIGH"));
        }
    }

    @Nested
    @DisplayName("addressGap() tests")
    class AddressGapTests {

        @Test
        @DisplayName("Should mark gap as addressed with new code")
        void addressGap_shouldUpdateStatusAndCode() {
            // Arrange
            UUID gapId = UUID.randomUUID();
            DocumentationGapEntity gap = createGap(DocumentationGapEntity.GapStatus.OPEN);
            gap.setId(gapId);

            when(gapRepository.findById(gapId)).thenReturn(Optional.of(gap));
            when(gapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            DocumentationGapEntity addressed = documentationGapService
                .addressGap(gapId, "Dr. Smith", "E11.65");

            // Assert
            assertThat(addressed.getStatus())
                .isEqualTo(DocumentationGapEntity.GapStatus.ADDRESSED);
            assertThat(addressed.getAddressedBy()).isEqualTo("Dr. Smith");
            assertThat(addressed.getAddressedAt()).isNotNull();
        }
    }
}
```

#### RecaptureTrackingServiceTest - Recapture Opportunities

Tests for HCC recapture detection, worklist generation, and recapture rate calculation.

```java
@ExtendWith(MockitoExtension.class)
class RecaptureTrackingServiceTest {

    @Mock
    private RecaptureOpportunityRepository recaptureRepository;

    @Mock
    private PatientHccProfileRepository profileRepository;

    @InjectMocks
    private RecaptureTrackingService recaptureTrackingService;

    @Nested
    @DisplayName("identifyRecaptureOpportunities() tests")
    class IdentifyRecaptureOpportunitiesTests {

        @Test
        @DisplayName("Should identify chronic HCCs not recaptured")
        void identifyOpportunities_withChronicHccsNotRecaptured_shouldCreate() {
            // Arrange
            PatientHccProfileEntity priorProfile = createPriorProfile(
                List.of("HCC17", "HCC85", "HCC111", "HCC137") // All chronic
            );
            PatientHccProfileEntity currentProfile = createCurrentProfile(
                List.of("HCC17") // Only HCC17 recaptured
            );

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(
                    TENANT_ID, PATIENT_ID, 2024))
                .thenReturn(Optional.of(priorProfile));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(
                    TENANT_ID, PATIENT_ID, 2025))
                .thenReturn(Optional.of(currentProfile));

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.identifyRecaptureOpportunities(
                    TENANT_ID, PATIENT_ID, 2025);

            // Assert - HCC85, HCC111, HCC137 not recaptured
            assertThat(opportunities).hasSize(3);
            assertThat(opportunities).allMatch(o -> !o.getIsRecaptured());
            assertThat(opportunities).anyMatch(o -> o.getHccCode().equals("HCC85"));
        }
    }

    @Nested
    @DisplayName("calculateRecaptureRate() tests")
    class CalculateRecaptureRateTests {

        @Test
        @DisplayName("Should calculate recapture rate correctly")
        void calculateRecaptureRate_shouldComputeStats() {
            // Arrange
            Object[] stats = new Object[]{
                10L,  // total opportunities
                7L,   // recaptured
                BigDecimal.valueOf(1.5),  // total RAF at risk
                BigDecimal.valueOf(1.05)  // RAF secured
            };

            when(recaptureRepository.getRecaptureStatsByTenant(TENANT_ID, 2025))
                .thenReturn(List.of(stats));

            // Act
            RecaptureRateSummary summary = recaptureTrackingService
                .calculateRecaptureRate(TENANT_ID, 2025);

            // Assert
            assertThat(summary.getTotalOpportunities()).isEqualTo(10);
            assertThat(summary.getRecapturedCount()).isEqualTo(7);
            assertThat(summary.getPendingCount()).isEqualTo(3);
            assertThat(summary.getRecaptureRate()).isCloseTo(70.0, Offset.offset(0.01));
            assertThat(summary.getTotalRafAtRisk()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
            assertThat(summary.getRafSecured()).isEqualByComparingTo(BigDecimal.valueOf(1.05));
        }
    }

    @Nested
    @DisplayName("getRecaptureWorklist() tests")
    class GetRecaptureWorklistTests {

        @Test
        @DisplayName("Should group opportunities by priority")
        void getRecaptureWorklist_shouldGroupByPriority() {
            // Arrange
            List<RecaptureOpportunityEntity> opportunities = List.of(
                createOpportunityWithPriority("HCC85", "HIGH"),
                createOpportunityWithPriority("HCC111", "HIGH"),
                createOpportunityWithPriority("HCC17", "MEDIUM"),
                createOpportunityWithPriority("HCC19", "LOW")
            );

            when(recaptureRepository.findPendingByTenant(TENANT_ID, 2025))
                .thenReturn(opportunities);

            // Act
            Map<String, List<RecaptureOpportunityEntity>> worklist =
                recaptureTrackingService.getRecaptureWorklist(TENANT_ID, 2025);

            // Assert
            assertThat(worklist).containsKeys("HIGH", "MEDIUM", "LOW");
            assertThat(worklist.get("HIGH")).hasSize(2);
            assertThat(worklist.get("MEDIUM")).hasSize(1);
            assertThat(worklist.get("LOW")).hasSize(1);
        }
    }
}
```

---

### Integration Tests (API Endpoints)

#### HccApiIntegrationTest - Full API Testing

Tests for REST API endpoints with TestContainers PostgreSQL.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class HccApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("hcctest")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("hcc.v24-weight", () -> "0.33");
        registry.add("hcc.v28-weight", () -> "0.67");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosisHccMapRepository diagnosisMapRepository;

    @BeforeEach
    void setUp() {
        seedCrosswalkData();
    }

    @Nested
    @DisplayName("POST /api/v1/hcc/patient/{patientId}/calculate tests")
    class CalculateRafTests {

        @Test
        @DisplayName("Should calculate RAF score with valid diagnosis codes")
        void calculateRaf_withValidDiagnoses_shouldReturn200() throws Exception {
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList("E11.9", "J44.1"));
            request.setAge(65);
            request.setSex("M");
            request.setDualEligible(false);
            request.setInstitutionalized(false);

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", PATIENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.rafScoreV24").isNumber())
                .andExpect(jsonPath("$.rafScoreV28").isNumber())
                .andExpect(jsonPath("$.rafScoreBlended").isNumber())
                .andExpect(jsonPath("$.hccsV24").isArray())
                .andExpect(jsonPath("$.hccsV28").isArray())
                .andExpect(jsonPath("$.v24Weight").value(0.33))
                .andExpect(jsonPath("$.v28Weight").value(0.67));
        }

        @Test
        @DisplayName("Should filter non-HCC diagnoses")
        void calculateRaf_withNonHccDiagnosis_shouldFilterOut() throws Exception {
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList("I10")); // Hypertension - not HCC
            request.setAge(65);
            request.setSex("M");

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", PATIENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisCount").value(1))
                .andExpect(jsonPath("$.hccCountV24").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hcc/crosswalk tests")
    class CrosswalkTests {

        @Test
        @DisplayName("Should return crosswalk mappings for valid ICD-10 codes")
        void getCrosswalk_withValidCodes_shouldReturnMappings() throws Exception {
            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", "E11.9", "J44.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['E11.9']").exists())
                .andExpect(jsonPath("$['E11.9'].hccCodeV24").value("HCC19"))
                .andExpect(jsonPath("$['E11.9'].hccCodeV28").value("HCC37"))
                .andExpect(jsonPath("$['J44.1']").exists())
                .andExpect(jsonPath("$['J44.1'].hccCodeV24").value("HCC111"));
        }
    }

    @Nested
    @DisplayName("Tenant isolation tests")
    class TenantIsolationTests {

        @Test
        @DisplayName("Should not return other tenant's profiles")
        void getProfile_differentTenant_shouldReturn404() throws Exception {
            // Create profile for tenant1
            UUID patientId = UUID.randomUUID();
            PatientHccProfileEntity profile = PatientHccProfileEntity.builder()
                .tenantId("tenant1")
                .patientId(patientId)
                .profileYear(2025)
                .rafScoreV24(new BigDecimal("1.5"))
                .rafScoreV28(new BigDecimal("1.4"))
                .rafScoreBlended(new BigDecimal("1.433"))
                .build();
            profileRepository.save(profile);

            // Try to access from tenant2 - should get 404
            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/profile", patientId)
                    .header("X-Tenant-ID", "tenant2")
                    .param("year", "2025"))
                .andExpect(status().isNotFound());
        }
    }
}
```

---

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
class MultiTenantIsolationTest {

    @Autowired
    private PatientHccProfileRepository profileRepository;

    @Test
    @DisplayName("Should isolate HCC profiles between tenants")
    void shouldIsolateProfilesBetweenTenants() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        UUID patientId = UUID.randomUUID();

        PatientHccProfileEntity profile1 = createProfile(tenant1, patientId, 1.5);
        PatientHccProfileEntity profile2 = createProfile(tenant2, patientId, 1.8);
        profileRepository.saveAll(List.of(profile1, profile2));

        // When
        Optional<PatientHccProfileEntity> t1Profile = profileRepository
            .findByTenantIdAndPatientIdAndProfileYear(tenant1, patientId, 2025);
        Optional<PatientHccProfileEntity> t2Profile = profileRepository
            .findByTenantIdAndPatientIdAndProfileYear(tenant2, patientId, 2025);

        // Then
        assertThat(t1Profile).isPresent();
        assertThat(t2Profile).isPresent();
        assertThat(t1Profile.get().getRafScoreBlended().doubleValue()).isEqualTo(1.5);
        assertThat(t2Profile.get().getRafScoreBlended().doubleValue()).isEqualTo(1.8);
    }
}
```

---

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RoleBasedAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin should be able to calculate RAF scores")
    void adminCanCalculateRaf() throws Exception {
        mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", UUID.randomUUID())
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRafRequestJson()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Analyst should be able to view profiles but not modify")
    void analystCanViewButNotModify() throws Exception {
        // Analyst CAN view profiles
        mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/profile", UUID.randomUUID())
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "analyst-001")
                .header("X-Auth-Roles", "ANALYST")
                .param("year", "2025"))
            .andExpect(status().isNotFound()); // 404 = allowed to query

        // Analyst CANNOT delete profiles
        mockMvc.perform(delete("/api/v1/hcc/patient/{patientId}/profile", UUID.randomUUID())
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "analyst-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Viewer should have read-only access to HCC data")
    void viewerHasReadOnlyAccess() throws Exception {
        // Viewer CAN view crosswalk
        mockMvc.perform(get("/api/v1/hcc/crosswalk")
                .param("icd10Codes", "E11.9"))
            .andExpect(status().isOk());

        // Viewer CANNOT calculate RAF (write operation)
        mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", UUID.randomUUID())
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRafRequestJson()))
            .andExpect(status().isForbidden());
    }
}
```

---

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
class HipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("HCC profile cache TTL must not exceed 5 minutes")
    void hccProfileCacheTtlMustBeCompliant() {
        Cache hccCache = cacheManager.getCache("hccProfiles");

        assertThat(hccCache).isNotNull();

        if (hccCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) hccCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("HCC cache TTL exceeds 5 minutes (HIPAA violation)");
        }
    }

    @Test
    @DisplayName("RAF calculation responses must include no-cache headers")
    void rafResponsesMustIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", UUID.randomUUID())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRafRequestJson()))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("Test data must use synthetic ICD-10 codes")
    void testDataMustUseSyntheticPatterns() {
        // Verify test data generators don't use real patient identifiers
        PatientHccProfileEntity testProfile = createTestProfile();

        assertThat(testProfile.getPatientId())
            .isNotNull()
            .withFailMessage("Test profiles must have non-null synthetic patient IDs");

        // Ensure diagnosis codes are from standard test set
        assertThat(testProfile.getDiagnosisCodes())
            .allMatch(code -> code.matches("^[A-Z]\\d{2}(\\.\\d{1,2})?$"))
            .withFailMessage("Test diagnosis codes must follow ICD-10 format");
    }
}
```

---

### Performance Tests

```java
@SpringBootTest
@Testcontainers
class PerformanceTest {

    @Autowired
    private RafCalculationService rafCalculationService;

    @Test
    @DisplayName("RAF calculation should complete within 100ms per patient")
    void rafCalculationShouldBeUnder100ms() {
        // Given
        String tenantId = "tenant-perf-001";
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        List<String> diagnosisCodes = List.of("E11.9", "I10", "J44.1", "N18.3");
        DemographicFactors factors = DemographicFactors.builder()
            .age(72)
            .sex("M")
            .dualEligible(true)
            .institutionalized(false)
            .build();

        // When
        for (int i = 0; i < iterations; i++) {
            UUID patientId = UUID.randomUUID();
            Instant start = Instant.now();
            rafCalculationService.calculateRaf(tenantId, patientId, diagnosisCodes, factors);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(100L)
            .withFailMessage("p95 RAF calculation latency %dms exceeds 100ms target", p95);

        System.out.printf("RAF Calculation Performance: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(iterations / 2),
            p95,
            latencies.get((int) (iterations * 0.99)));
    }

    @Test
    @DisplayName("Crosswalk lookup should complete within 50ms for batch of 20 codes")
    void crosswalkLookupShouldBeUnder50ms() {
        // Given
        List<String> icd10Codes = List.of(
            "E11.9", "I10", "J44.1", "N18.3", "I48.91",
            "F32.9", "G43.909", "M54.5", "K21.0", "J45.909",
            "E78.5", "I25.10", "G47.33", "M17.11", "K76.0",
            "N40.0", "E03.9", "D64.9", "G20", "I63.9"
        );
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            rafCalculationService.batchCrosswalk(icd10Codes);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(50L)
            .withFailMessage("p95 crosswalk lookup latency %dms exceeds 50ms target", p95);
    }
}
```

---

### Test Configuration

#### BaseIntegrationTest Setup

```java
@Testcontainers
public abstract class BaseHccIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("hcctest")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("hcc.v24-weight", () -> "0.33");
        registry.add("hcc.v28-weight", () -> "0.67");
    }

    protected void seedCrosswalkData(DiagnosisHccMapRepository repository) {
        List<DiagnosisHccMapEntity> mappings = Arrays.asList(
            DiagnosisHccMapEntity.builder()
                .icd10Code("E11.9")
                .hccCodeV24("HCC19")
                .hccCodeV28("HCC37")
                .hccNameV24("Diabetes without Complication")
                .coefficientV24(0.105)
                .coefficientV28(0.118)
                .build(),
            // Additional mappings...
        );
        repository.saveAll(mappings);
    }
}
```

---

### Best Practices

#### HCC-Specific Testing Guidelines

1. **RAF Calculation Testing**
   - Test both V24 and V28 model outputs separately
   - Verify blended score formula: `(V24 × 0.33) + (V28 × 0.67)`
   - Test demographic factor variations (age bands, sex, dual eligibility)

2. **HCC Hierarchy Testing**
   - Test all hierarchy suppression rules (HCC17→HCC19, HCC8→HCC9, etc.)
   - Verify only highest-severity HCC is counted per hierarchy
   - Document hierarchy rules in test names

3. **Crosswalk Testing**
   - Test both HCC-mapped and non-HCC ICD-10 codes
   - Verify null handling for codes without HCC mapping
   - Test batch crosswalk performance

4. **Documentation Gap Testing**
   - Test specificity gap detection (unspecified codes)
   - Test V28 transition gaps (codes that changed between models)
   - Test gap lifecycle (OPEN → ADDRESSED/REJECTED)

5. **Recapture Testing**
   - Test chronic HCC identification (persistent conditions)
   - Test year-over-year comparison logic
   - Verify RAF impact calculations

6. **HIPAA Compliance**
   - All test data must use synthetic patient IDs
   - Verify cache TTL compliance (≤5 minutes for PHI)
   - Test no-cache header presence on API responses

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| RAF score mismatch | Wrong blending weights | Check `v24Weight` and `v28Weight` in test setup |
| Hierarchy not applied | Missing hierarchy rules | Verify `HccHierarchyRules` configuration |
| Profile not found | Missing tenant filter | Check `findByTenantIdAndPatientIdAndProfileYear` call |
| Crosswalk empty | No seed data | Call `seedCrosswalkData()` in `@BeforeEach` |
| Gap detection fails | String/Enum comparison bug | Use `.equals()` with enum values, not strings |
| Recapture rate 0% | No prior year profile | Create prior year profile in test setup |
| Integration test timeout | TestContainers slow | Increase timeout or use `@Testcontainers(disabledWithoutDocker = true)` |

#### Common Test Execution Issues

```bash
# If crosswalk data missing
# Ensure seedCrosswalkData() called in @BeforeEach

# If blending weights wrong
# Set via ReflectionTestUtils:
ReflectionTestUtils.setField(service, "v24Weight", 0.33);
ReflectionTestUtils.setField(service, "v28Weight", 0.67);

# Run single test for debugging
./gradlew test --tests "RafCalculationServiceTest.calculateRaf*"

# Check test containers are running
docker ps  # Should show postgres container
```

---

### Building

```bash
./gradlew :modules:services:hcc-service:build
```

## Example Usage

### Calculate RAF Score

```bash
curl -X POST http://localhost:8105/hcc/api/v1/hcc/patient/patient-123/calculate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "diagnosisCodes": ["E11.9", "I50.9", "J44.9"],
    "age": 72,
    "sex": "M",
    "dualEligible": true,
    "institutionalized": false
  }'
```

Response:
```json
{
  "patientId": "patient-123",
  "v24Raf": 1.523,
  "v28Raf": 1.678,
  "blendedRaf": 1.634,
  "capturedHccs": [
    {"model": "V28", "hccCode": "HCC19", "description": "Diabetes without Complication", "coefficient": 0.318},
    {"model": "V28", "hccCode": "HCC85", "description": "Congestive Heart Failure", "coefficient": 0.323},
    {"model": "V28", "hccCode": "HCC111", "description": "Chronic Obstructive Pulmonary Disease", "coefficient": 0.328}
  ],
  "interactions": [
    {"code": "D1_CHF", "coefficient": 0.154}
  ]
}
```

### Get Documentation Gaps

```bash
curl http://localhost:8105/hcc/api/v1/hcc/patient/patient-123/documentation-gaps?year=2024 \
  -H "X-Tenant-ID: tenant-001"
```

### Get High-Value Opportunities

```bash
curl "http://localhost:8105/hcc/api/v1/hcc/opportunities?year=2024&minUplift=0.1" \
  -H "X-Tenant-ID: tenant-001"
```

## Common HCC Categories

### High-Impact HCCs (Coefficient > 0.5)
- HCC8: Metastatic Cancer (2.659)
- HCC9: Lung and Other Severe Cancers (1.395)
- HCC17: Diabetes with Chronic Complications (0.318 + interactions)
- HCC18: Diabetes with Acute Complications (0.318 + interactions)
- HCC85: Congestive Heart Failure (0.323)
- HCC86: Acute Myocardial Infarction (0.184)

### Common Chronic Conditions
- HCC19: Diabetes without Complication (0.318)
- HCC108: Vascular Disease (0.288)
- HCC111: COPD (0.328)
- HCC112: Fibrosis of Lung (0.247)

## Hierarchies

HCC hierarchies ensure only the most severe condition is counted:
- HCC17 (Diabetes with chronic complications) suppresses HCC18, HCC19
- HCC80 (Acute Stroke) suppresses HCC81, HCC82
- HCC85 (CHF) suppresses HCC86 (AMI)

## Integration

The HCC Service integrates with:
- **FHIR Service**: Retrieves patient conditions/diagnoses
- **Patient Service**: Gets patient demographics
- **Care Gap Service**: Creates documentation gap tasks
- **Event Router**: Publishes HCC calculation events

## Standards Compliance

- CMS HCC Risk Adjustment Model V24/V28
- ICD-10-CM Official Guidelines
- Medicare Managed Care Manual Chapter 7
- CMS Rate Announcement methodology

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
