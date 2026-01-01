# SDOH Integration Service

Social Determinants of Health (SDOH) integration service for the HDIM platform, implementing Gravity Project FHIR Implementation Guide standards for comprehensive SDOH screening, Z-code mapping, community resource integration, and health equity analytics.

## Features

### 1. Gravity Standard Implementation
- **AHC-HRSN Screening**: Accountable Health Communities Health-Related Social Needs screening tool
- **PRAPARE Screening**: Protocol for Responding to and Assessing Patients' Assets, Risks, and Experiences
- **Standardized SDOH Categories**: Food Insecurity, Housing Instability, Transportation, Financial Strain, Education, Employment, Utilities, Social Isolation, Interpersonal Violence, Health Literacy, Substance Use, Mental Health, Disability, Immigration
- **LOINC Code Mapping**: All screening questions mapped to standard LOINC codes

### 2. Z-Code Mapping (ICD-10-CM)
- **Automated Z-Code Assignment**: Maps SDOH findings to ICD-10-CM Z-codes (Z55-Z65)
- **Category-specific Codes**:
  - Food Insecurity: Z59.4, Z59.41, Z59.48
  - Housing Instability: Z59.0, Z59.00, Z59.01, Z59.02
  - Transportation: Z59.82
  - Financial Strain: Z59.5, Z59.6, Z59.7
  - Education: Z55.x series
  - Employment: Z56.x series
  - And more...
- **FHIR Condition Export**: Export Z-coded diagnoses as FHIR Condition resources

### 3. Community Resource Directory
- **Resource Search**: Search by category, location, zip code, or proximity
- **Resource Categories**: Food, Housing, Transportation, Utilities, Employment, Education, Financial Services, Healthcare, Mental Health, Legal Services, Childcare, Eldercare
- **Geolocation Support**: Find resources within specified radius
- **Referral Management**: Create, track, and manage patient referrals to community resources
- **Referral Status Tracking**: PENDING, CONTACTED, SCHEDULED, COMPLETED, DECLINED, CANCELLED

### 4. Health Equity Analytics
- **Disparity Measurement**: Calculate health disparities across multiple stratifications
- **Stratification Types**: Race, Ethnicity, Language, Geography, Insurance Status, Income Level, Age Group, Gender
- **Trend Analysis**: Track disparity trends over time
- **Automated Reports**: Generate comprehensive health equity reports
- **Key Findings**: AI-generated insights and recommendations

### 5. SDOH Risk Scoring
- **Composite Risk Score**: 0-100 scale risk assessment
- **Risk Levels**: LOW (0-25), MODERATE (26-50), HIGH (51-75), CRITICAL (76-100)
- **Category-specific Weights**: Different SDOH categories weighted by impact
- **Predictive Analytics**:
  - Hospitalization risk prediction
  - Emergency visit risk prediction
  - Medication adherence impact prediction
- **Risk History Tracking**: Monitor changes over time

### 6. Multi-tenant Support
- Full tenant isolation for all SDOH data
- Tenant-specific screening statistics
- Tenant-level equity reporting

## API Endpoints

### SDOH Screening
- `POST /api/v1/sdoh/screening/{patientId}` - Submit SDOH screening
- `GET /api/v1/sdoh/assessment/{patientId}` - Get patient SDOH assessment
- `GET /api/v1/sdoh/screening/questions` - Get screening questionnaire

### Z-Code Management
- `GET /api/v1/sdoh/z-codes/{patientId}` - Get SDOH Z-codes for patient

### Community Resources
- `GET /api/v1/sdoh/resources` - Search community resources
- `POST /api/v1/sdoh/referral` - Create resource referral
- `GET /api/v1/sdoh/referrals/{patientId}` - Get patient referrals
- `PUT /api/v1/sdoh/referral/{referralId}/status` - Update referral status

### Health Equity
- `GET /api/v1/sdoh/equity/report` - Generate health equity report

### Risk Scoring
- `GET /api/v1/sdoh/risk/{patientId}` - Get patient risk score

### Health Check
- `GET /api/v1/sdoh/_health` - Service health check

## Technology Stack

- **Spring Boot 3.3.5**: Core framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **HAPI FHIR 7.6.0**: FHIR resource handling
- **Jackson**: JSON processing
- **Lombok**: Boilerplate reduction
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework

## Testing

### Overview

The SDOH Service was developed using Test-Driven Development (TDD) with comprehensive test coverage validating:
- **Gravity Project Screening** - AHC-HRSN (10 questions) and PRAPARE (20 questions) instruments
- **Z-Code Mapping** - ICD-10-CM Z55-Z65 code assignment and validation
- **Community Resources** - Resource search, referral creation, and status tracking
- **Health Equity Analytics** - Disparity metrics, stratification analysis, trend tracking
- **SDOH Risk Scoring** - Category weights, risk levels, predictive analytics
- **HIPAA Compliance** - PHI protection, tenant isolation, audit logging

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:sdoh-service:test

# Run specific test suite
./gradlew :modules:services:sdoh-service:test --tests "*ScreeningServiceTest"
./gradlew :modules:services:sdoh-service:test --tests "*ZCodeMapperTest"
./gradlew :modules:services:sdoh-service:test --tests "*RiskCalculatorTest"
./gradlew :modules:services:sdoh-service:test --tests "*ControllerTest"

# Run with coverage
./gradlew :modules:services:sdoh-service:test jacocoTestReport

# Run integration tests only (requires Docker)
./gradlew :modules:services:sdoh-service:integrationTest
```

### Test Coverage Summary

| Test Class | Tests | Focus Area |
|------------|-------|------------|
| `GravityScreeningServiceTest` | 22+ | AHC-HRSN/PRAPARE screening, needs identification, FHIR export |
| `ZCodeMapperTest` | 15+ | Z-code lookup, validation, diagnosis management |
| `CommunityResourceServiceTest` | 15+ | Resource search, referrals, geolocation |
| `HealthEquityAnalyzerTest` | 15+ | Disparity metrics, stratification, trends |
| `SdohRiskCalculatorTest` | 11+ | Category weights, risk levels, predictions |
| `SdohControllerTest` | 15+ | REST API endpoints, security, validation |

**Total: 93+ test methods covering screening, mapping, resources, equity, and risk scoring**

### Test Organization

```
src/test/java/com/healthdata/sdoh/
├── service/
│   ├── GravityScreeningServiceTest.java    # Gravity Project screening
│   ├── ZCodeMapperTest.java                # ICD-10-CM Z-code mapping
│   ├── CommunityResourceServiceTest.java   # Community resources
│   ├── HealthEquityAnalyzerTest.java       # Equity analytics
│   └── SdohRiskCalculatorTest.java         # Risk scoring
└── controller/
    └── SdohControllerTest.java             # REST API endpoints
```

### Unit Tests (GravityScreeningServiceTest)

Tests Gravity Project FHIR IG screening instruments:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Gravity Screening Service Tests")
class GravityScreeningServiceTest {

    @Mock
    private SdohAssessmentRepository assessmentRepository;

    @Mock
    private ZCodeMapper zCodeMapper;

    @Mock
    private SdohRiskCalculator riskCalculator;

    private GravityScreeningService screeningService;

    private static final String TENANT_ID = "tenant-001";
    private static final String PATIENT_ID = "patient-001";

    @Test
    @DisplayName("Should create AHC-HRSN questionnaire with 10 questions")
    void shouldCreateAhcHrsnQuestionnaire() {
        // When
        List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

        // Then
        assertNotNull(questions);
        assertEquals(10, questions.size());

        // Verify key questions are present
        assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-food-1")));
        assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-housing-1")));
        assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-transport-1")));
        assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-utilities-1")));
        assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-financial-1")));

        // Verify categories covered
        assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.FOOD_INSECURITY));
        assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.HOUSING_INSTABILITY));
        assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.TRANSPORTATION));
    }

    @Test
    @DisplayName("Should include required LOINC codes in questions")
    void shouldIncludeLoincCodes() {
        // When
        List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

        // Then - all questions should have valid LOINC codes
        for (SdohScreeningQuestion question : questions) {
            assertNotNull(question.getLoincCode(),
                "Question " + question.getQuestionId() + " missing LOINC code");
            assertTrue(question.getLoincCode().matches("\\d{5}-\\d"),
                "Invalid LOINC code format: " + question.getLoincCode());
        }
    }

    @Test
    @DisplayName("Should identify food insecurity need from responses")
    void shouldIdentifyFoodInsecurityNeed() {
        // Given - response indicating food insecurity
        List<SdohScreeningResponse> responses = Arrays.asList(
            SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Often true").build(),
            SdohScreeningResponse.builder().questionId("ahc-food-2").answer("Sometimes true").build()
        );

        // When
        Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

        // Then
        assertTrue(needs.containsKey(SdohCategory.FOOD_INSECURITY));
        assertTrue(needs.get(SdohCategory.FOOD_INSECURITY));
    }
}
```

**Key Test Areas - Gravity Screening:**

| Test Scenario | Validation |
|---------------|------------|
| AHC-HRSN questionnaire | 10 questions with correct categories |
| PRAPARE questionnaire | 20 questions for comprehensive screening |
| LOINC code validation | All questions have valid LOINC codes (xxxxx-x format) |
| Answer options | Each question has ≥2 answer options |
| Food insecurity detection | "Often true"/"Sometimes true" responses trigger need |
| Housing need detection | "Often worried" response triggers need |
| Transportation detection | "Yes" response triggers need |
| Negative responses | No needs identified when all responses negative |
| Assessment status | IN_PROGRESS → COMPLETED transitions |
| Old assessment archiving | Cutoff date-based ARCHIVED status |
| Completion percentage | Correctly calculates answered/total ratio |
| Required question validation | Rejects when required questions unanswered |

### Unit Tests (ZCodeMapperTest)

Tests ICD-10-CM Z-code (Z55-Z65) mapping:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Z-Code Mapper Tests")
class ZCodeMapperTest {

    @Mock
    private SdohDiagnosisRepository diagnosisRepository;

    private ZCodeMapper zCodeMapper;

    @Test
    @DisplayName("Should get Z-codes for food insecurity category")
    void shouldGetZCodesForFoodInsecurity() {
        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(SdohCategory.FOOD_INSECURITY);

        // Then
        assertNotNull(zCodes);
        assertEquals(3, zCodes.size());
        assertTrue(zCodes.contains("Z59.4"));   // Lack of adequate food/water
        assertTrue(zCodes.contains("Z59.41"));  // Food insecurity
        assertTrue(zCodes.contains("Z59.48"));  // Other food insecurity
    }

    @Test
    @DisplayName("Should get Z-codes for housing instability category")
    void shouldGetZCodesForHousingInstability() {
        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(SdohCategory.HOUSING_INSTABILITY);

        // Then
        assertNotNull(zCodes);
        assertEquals(4, zCodes.size());
        assertTrue(zCodes.contains("Z59.0"));   // Homelessness
        assertTrue(zCodes.contains("Z59.00"));  // Homelessness unspecified
        assertTrue(zCodes.contains("Z59.01"));  // Sheltered homelessness
        assertTrue(zCodes.contains("Z59.02"));  // Unsheltered homelessness
    }

    @Test
    @DisplayName("Should validate correct Z-code format")
    void shouldValidateCorrectZCodeFormat() {
        // Valid formats
        assertTrue(zCodeMapper.isValidZCode("Z59.4"));
        assertTrue(zCodeMapper.isValidZCode("Z59.41"));
        assertTrue(zCodeMapper.isValidZCode("Z55.0"));
        assertTrue(zCodeMapper.isValidZCode("Z59"));

        // Invalid formats
        assertFalse(zCodeMapper.isValidZCode("ABC123"));
        assertFalse(zCodeMapper.isValidZCode("59.4"));   // Missing Z prefix
        assertFalse(zCodeMapper.isValidZCode("Z5.4"));   // Only one digit after Z
        assertFalse(zCodeMapper.isValidZCode(null));
        assertFalse(zCodeMapper.isValidZCode(""));
    }

    @Test
    @DisplayName("Should export diagnosis to FHIR Condition resource")
    void shouldExportToFhirCondition() {
        // Given
        SdohDiagnosis diagnosis = SdohDiagnosis.builder()
            .diagnosisId("diag-001")
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .zCode("Z59.41")
            .zCodeDescription("Food insecurity")
            .category(SdohCategory.FOOD_INSECURITY)
            .status(SdohDiagnosis.DiagnosisStatus.ACTIVE)
            .build();

        // When
        String fhirJson = zCodeMapper.exportToFhirCondition(diagnosis);

        // Then
        assertNotNull(fhirJson);
        assertTrue(fhirJson.contains("\"resourceType\": \"Condition\""));
        assertTrue(fhirJson.contains("\"code\": \"Z59.41\""));
        assertTrue(fhirJson.contains("http://hl7.org/fhir/sid/icd-10-cm"));
    }
}
```

**Key Test Areas - Z-Code Mapping:**

| Test Scenario | Validation |
|---------------|------------|
| Food insecurity Z-codes | Z59.4, Z59.41, Z59.48 |
| Housing Z-codes | Z59.0, Z59.00, Z59.01, Z59.02 |
| Transportation Z-code | Z59.82 |
| Z-code format validation | Zxx.x or Zxx.xx pattern required |
| Category from Z-code | Reverse lookup (Z59.4 → FOOD_INSECURITY) |
| Z-code description | Correct human-readable descriptions |
| Diagnosis creation | Auto-assigns first Z-code for category |
| Diagnosis status update | ACTIVE → RESOLVED transitions |
| FHIR Condition export | Valid resourceType, code, system |
| Needs to Z-code mapping | Maps true needs to primary Z-code |

### Unit Tests (SdohRiskCalculatorTest)

Tests SDOH risk scoring and predictions:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SDOH Risk Calculator Tests")
class SdohRiskCalculatorTest {

    @Mock
    private SdohRiskScoreRepository riskScoreRepository;

    private SdohRiskCalculator riskCalculator;

    @Test
    @DisplayName("Should calculate scores for multiple categories")
    void shouldCalculateScoresForMultipleCategories() {
        // Given - multiple needs identified
        Map<SdohCategory, Boolean> needs = new HashMap<>();
        needs.put(SdohCategory.FOOD_INSECURITY, true);      // 0.15 weight
        needs.put(SdohCategory.HOUSING_INSTABILITY, true);  // 0.20 weight
        needs.put(SdohCategory.TRANSPORTATION, true);       // 0.10 weight

        // When
        Map<SdohCategory, Double> scores = riskCalculator.calculateCategoryScores(needs);

        // Then
        assertEquals(3, scores.size());
        assertEquals(0.15, scores.get(SdohCategory.FOOD_INSECURITY), 0.001);
        assertEquals(0.20, scores.get(SdohCategory.HOUSING_INSTABILITY), 0.001);
        assertEquals(0.10, scores.get(SdohCategory.TRANSPORTATION), 0.001);
    }

    @Test
    @DisplayName("Should assign risk levels correctly")
    void shouldAssignRiskLevels() {
        // Risk level thresholds: LOW (0-25), MODERATE (26-50), HIGH (51-75), CRITICAL (76-100)
        assertEquals(SdohRiskScore.RiskLevel.LOW, SdohRiskScore.RiskLevel.fromScore(0));
        assertEquals(SdohRiskScore.RiskLevel.LOW, SdohRiskScore.RiskLevel.fromScore(25));
        assertEquals(SdohRiskScore.RiskLevel.MODERATE, SdohRiskScore.RiskLevel.fromScore(26));
        assertEquals(SdohRiskScore.RiskLevel.MODERATE, SdohRiskScore.RiskLevel.fromScore(50));
        assertEquals(SdohRiskScore.RiskLevel.HIGH, SdohRiskScore.RiskLevel.fromScore(51));
        assertEquals(SdohRiskScore.RiskLevel.HIGH, SdohRiskScore.RiskLevel.fromScore(75));
        assertEquals(SdohRiskScore.RiskLevel.CRITICAL, SdohRiskScore.RiskLevel.fromScore(76));
        assertEquals(SdohRiskScore.RiskLevel.CRITICAL, SdohRiskScore.RiskLevel.fromScore(100));
    }

    @Test
    @DisplayName("Should identify INCREASING trend when score increases >5 points")
    void shouldIdentifyIncreasingTrend() {
        // Given - first score 30, latest score 40 (increase of 10)
        List<SdohRiskScore> history = Arrays.asList(
            SdohRiskScore.builder().totalScore(40.0).calculatedAt(LocalDateTime.now()).build(),
            SdohRiskScore.builder().totalScore(30.0).calculatedAt(LocalDateTime.now().minusDays(1)).build()
        );

        // When
        String trend = riskCalculator.identifyTrend(history);

        // Then
        assertEquals("INCREASING", trend);
    }
}
```

**Key Test Areas - Risk Scoring:**

| Category Weight | Value |
|-----------------|-------|
| Housing Instability | 0.20 |
| Food Insecurity | 0.15 |
| Financial Strain | 0.15 |
| Employment | 0.12 |
| Transportation | 0.10 |
| Education | 0.08 |
| Interpersonal Violence | 0.08 |
| Social Isolation | 0.07 |
| Utilities | 0.05 |

| Test Scenario | Validation |
|---------------|------------|
| Category weights | Correct weight per SDOH category |
| Risk level assignment | LOW/MODERATE/HIGH/CRITICAL thresholds |
| Hospitalization risk | (score/100) * 0.30 formula |
| Emergency visit risk | (score/100) * 0.40 formula |
| Medication adherence | Negative impact -(score/100) * 0.30 |
| INCREASING trend | >5 point increase detected |
| DECREASING trend | >5 point decrease detected |
| STABLE trend | ±5 point range maintained |
| INSUFFICIENT_DATA | <2 history entries |
| Score persistence | Risk scores saved to repository |

### Controller Tests (SdohControllerTest)

Tests REST API endpoints:

```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SDOH Controller Tests")
class SdohControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GravityScreeningService screeningService;

    @MockBean
    private ZCodeMapper zCodeMapper;

    @MockBean
    private CommunityResourceService resourceService;

    @MockBean
    private HealthEquityAnalyzer equityAnalyzer;

    @MockBean
    private SdohRiskCalculator riskCalculator;

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("POST /api/v1/sdoh/screening/{patientId} - Submit screening")
    void testSubmitScreening() throws Exception {
        // Given
        when(screeningService.submitScreening(anyString(), anyString(), anyString(), anyList()))
            .thenReturn(mockAssessment);

        // When & Then
        mockMvc.perform(post("/api/v1/sdoh/screening/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "screeningTool", "AHC-HRSN",
                    "responses", responses
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assessmentId").value("assessment-001"))
            .andExpect(jsonPath("$.patientId").value(patientId));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/z-codes/{patientId} - Get patient Z-codes")
    void testGetPatientZCodes() throws Exception {
        // Given
        List<SdohDiagnosis> diagnoses = Arrays.asList(
            SdohDiagnosis.builder()
                .diagnosisId("d1")
                .zCode("Z59.4")
                .zCodeDescription("Food insecurity")
                .category(SdohCategory.FOOD_INSECURITY)
                .build()
        );

        when(zCodeMapper.getActiveDiagnoses(anyString(), anyString()))
            .thenReturn(diagnoses);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/z-codes/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].zCode").value("Z59.4"));
    }
}
```

**Key Test Areas - API Endpoints:**

| Endpoint | Test Coverage |
|----------|---------------|
| `POST /api/v1/sdoh/screening/{patientId}` | Submit screening responses |
| `GET /api/v1/sdoh/assessment/{patientId}` | Get most recent assessment, 404 handling |
| `GET /api/v1/sdoh/z-codes/{patientId}` | Get active SDOH diagnoses |
| `GET /api/v1/sdoh/resources` | Search by city/state or category |
| `POST /api/v1/sdoh/referral` | Create resource referral (201 response) |
| `GET /api/v1/sdoh/referrals/{patientId}` | Get patient referrals |
| `PUT /api/v1/sdoh/referral/{referralId}/status` | Update referral status |
| `GET /api/v1/sdoh/equity/report` | Generate health equity report |
| `GET /api/v1/sdoh/risk/{patientId}` | Get risk score history |
| `GET /api/v1/sdoh/screening/questions` | Get screening questionnaire |
| `GET /api/v1/sdoh/_health` | Health check (UP status) |

### Multi-Tenant Isolation Tests

Tests tenant data separation:

```java
@Nested
@DisplayName("Tenant Isolation Tests")
class TenantIsolationTests {

    @Test
    @DisplayName("Should only return assessments for specified tenant")
    void shouldIsolateAssessmentsByTenant() {
        // Given
        when(assessmentRepository.findMostRecentByTenantIdAndPatientId("tenant-1", PATIENT_ID))
            .thenReturn(Optional.of(createAssessmentEntity("tenant-1")));
        when(assessmentRepository.findMostRecentByTenantIdAndPatientId("tenant-2", PATIENT_ID))
            .thenReturn(Optional.empty());

        // When
        Optional<SdohAssessment> tenant1Result = screeningService.getMostRecentAssessment("tenant-1", PATIENT_ID);
        Optional<SdohAssessment> tenant2Result = screeningService.getMostRecentAssessment("tenant-2", PATIENT_ID);

        // Then
        assertTrue(tenant1Result.isPresent());
        assertFalse(tenant2Result.isPresent());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("Missing tenant header should return 400")
    void testMissingTenantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId))
            .andExpect(status().isBadRequest());
    }
}
```

### HIPAA Compliance Tests

Tests PHI handling requirements:

```java
@Nested
@DisplayName("HIPAA Compliance Tests")
class HipaaComplianceTests {

    @Test
    @DisplayName("Assessment responses should not exceed PHI cache TTL")
    void phiCacheTtl_shouldBeCompliant() {
        Cache assessmentCache = cacheManager.getCache("sdoh-assessments");
        assertThat(assessmentCache).isNotNull();

        if (assessmentCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) assessmentCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("PHI cache TTL exceeds 5 minutes");
        }
    }

    @Test
    @DisplayName("FHIR exports should include ICD-10-CM system URL")
    void fhirExport_shouldIncludeStandardSystem() {
        String fhirJson = zCodeMapper.exportToFhirCondition(diagnosis);

        assertTrue(fhirJson.contains("http://hl7.org/fhir/sid/icd-10-cm"));
    }

    @Test
    @DisplayName("Unauthorized access should be rejected")
    void testUnauthorizedAccess() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId));
        });
    }
}
```

### Performance Tests

Benchmarks for SDOH processing:

```java
@Nested
@DisplayName("Performance Tests")
class PerformanceTests {

    @Test
    @DisplayName("Risk score calculation should complete within 50ms")
    void riskScoreCalculation_shouldBePerformant() {
        // Given
        int calculationCount = 100;
        SdohAssessment assessment = createAssessmentWithNeeds();

        // When
        Instant start = Instant.now();
        for (int i = 0; i < calculationCount; i++) {
            riskCalculator.calculateRiskScore(assessment);
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMs = totalMs / (double) calculationCount;

        assertThat(avgMs)
            .isLessThan(50.0)
            .withFailMessage("Avg calculation time %.2fms exceeds 50ms target", avgMs);
    }

    @Test
    @DisplayName("Z-code lookup should complete within 10ms")
    void zCodeLookup_shouldBePerformant() {
        // Given
        int lookupCount = 1000;
        SdohCategory[] categories = SdohCategory.values();

        // When
        Instant start = Instant.now();
        for (int i = 0; i < lookupCount; i++) {
            zCodeMapper.getZCodesForCategory(categories[i % categories.length]);
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMs = totalMs / (double) lookupCount;

        assertThat(avgMs)
            .isLessThan(10.0)
            .withFailMessage("Avg lookup time %.2fms exceeds 10ms target", avgMs);
    }
}
```

### Test Configuration

**Test Security Configuration:**

```java
@TestConfiguration
public class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/v1/sdoh/_health").permitAll()
            .anyRequest().authenticated();
    }
}
```

**Test Cache Configuration:**

```java
@TestConfiguration
public class TestCacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new SimpleCacheManager() {{
            setCaches(Arrays.asList(
                new ConcurrentMapCache("sdoh-assessments"),
                new ConcurrentMapCache("sdoh-z-codes"),
                new ConcurrentMapCache("community-resources")
            ));
        }};
    }
}
```

### Best Practices

| Category | Best Practice |
|----------|---------------|
| **Screening Tools** | Test both AHC-HRSN (10 questions) and PRAPARE (20 questions) |
| **LOINC Codes** | Validate xxxxx-x format for all screening questions |
| **Z-Code Range** | Test within Z55-Z65 SDOH code range only |
| **Z-Code Format** | Use Zxx, Zxx.x, or Zxx.xx patterns |
| **Category Weights** | Verify weights sum to 1.0 across all categories |
| **Risk Levels** | LOW (0-25), MODERATE (26-50), HIGH (51-75), CRITICAL (76-100) |
| **Trend Detection** | >5 point change triggers INCREASING/DECREASING |
| **Referral Status** | Test PENDING → CONTACTED → SCHEDULED → COMPLETED flow |
| **FHIR Export** | Verify ICD-10-CM system URL in Condition resources |
| **Tenant Isolation** | Always include X-Tenant-ID header, verify cross-tenant denial |
| **Required Questions** | Validate all required questions answered before submission |
| **Negative Responses** | Verify no needs identified when all responses negative |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `LOINC code validation fails` | Wrong format | Use xxxxx-x pattern (5 digits, dash, 1 digit) |
| `Z-code not found` | Code outside Z55-Z65 range | Verify SDOH-specific Z-code range |
| `Risk level mismatch` | Boundary condition | Use correct thresholds (25, 50, 75) |
| `Trend shows INSUFFICIENT_DATA` | <2 history entries | Ensure at least 2 risk scores exist |
| `Missing tenant header 400` | Required header missing | Always send X-Tenant-ID header |
| `Unauthorized 401/403` | Role not authorized | Use ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN |
| `FHIR export invalid` | Missing system URL | Include http://hl7.org/fhir/sid/icd-10-cm |
| `Screening submission fails` | Empty responses | Provide at least one response |
| `Category weight incorrect` | Wrong weight value | Verify against category weight table |
| `Referral status update fails` | Invalid transition | Follow PENDING → CONTACTED → SCHEDULED → COMPLETED path |

## Database Schema

### Tables
- `sdoh_assessments` - SDOH screening assessments
- `sdoh_diagnoses` - Z-coded SDOH diagnoses
- `community_resources` - Community resource directory
- `resource_referrals` - Patient referrals to resources
- `sdoh_risk_scores` - SDOH risk scores

## Configuration

### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hdim_sdoh
    username: hdim_user
    password: hdim_pass
```

### Redis Cache
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Server
```yaml
server:
  port: 8089
```

## Security

- JWT-based authentication
- Role-based access control (RBAC)
- Required roles: ANALYST, EVALUATOR, ADMIN, SUPER_ADMIN
- Tenant isolation via X-Tenant-ID header

## Standards Compliance

### Gravity Project FHIR IG
- Implements Gravity Project FHIR Implementation Guide
- SDOH Clinical Care FHIR profiles
- Standardized terminology and value sets

### ICD-10-CM
- Complete Z55-Z65 code range support
- Accurate code descriptions
- Category-to-code mappings

### LOINC
- Screening questions coded with LOINC
- AHC-HRSN and PRAPARE LOINC codes

### CMS Health Equity
- Supports CMS health equity measures
- Stratification reporting
- Disparity tracking

## Development

### Build
```bash
./gradlew :modules:services:sdoh-service:build
```

### Run
```bash
./gradlew :modules:services:sdoh-service:bootRun
```

### Docker
```bash
docker build -t hdim-sdoh-service .
docker run -p 8089:8089 hdim-sdoh-service
```

## API Documentation

Interactive API documentation available at:
- Swagger UI: http://localhost:8089/swagger-ui.html
- OpenAPI Spec: http://localhost:8089/v3/api-docs

## Example Usage

### Submit SDOH Screening
```bash
curl -X POST http://localhost:8089/api/v1/sdoh/screening/patient-001 \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "screeningTool": "AHC-HRSN",
    "responses": [
      {
        "questionId": "ahc-food-1",
        "answer": "Often true"
      }
    ]
  }'
```

### Search Community Resources
```bash
curl http://localhost:8089/api/v1/sdoh/resources?category=FOOD&city=Boston&state=MA \
  -H "X-Tenant-ID: tenant-001"
```

### Generate Health Equity Report
```bash
curl "http://localhost:8089/api/v1/sdoh/equity/report?startDate=2024-01-01&endDate=2024-12-31" \
  -H "X-Tenant-ID: tenant-001"
```

## License

Copyright © 2024 Mahoosuc Solutions. All rights reserved.

## Support

For support and questions, contact the HDIM development team.
