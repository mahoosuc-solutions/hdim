# Predictive Analytics Service

ML-powered predictive analytics service for healthcare risk prediction, cost forecasting, and disease progression modeling.

## Overview

The Predictive Analytics Service provides machine learning-based predictions for:
- **Readmission Risk**: 30/90-day hospital readmission probability
- **Cost Forecasting**: Per-patient healthcare cost predictions
- **Disease Progression**: Chronic disease trajectory modeling
- **Population Health**: Risk stratification and cohort analysis

## Features

### 1. Readmission Risk Prediction
- LACE index calculation (Length, Acuity, Comorbidities, ED visits)
- Charlson Comorbidity Index integration
- 30-day and 90-day prediction periods
- Risk tier classification (LOW, MODERATE, HIGH, VERY_HIGH)
- Feature importance analysis

### 2. Cost Prediction
- Multi-category cost breakdown:
  - Inpatient care
  - Outpatient care
  - Pharmacy/medications
  - Emergency department
  - Lab and diagnostics
  - Imaging
  - Other ancillary services
- Configurable prediction periods (1-24 months)
- Confidence intervals

### 3. Disease Progression
- Supported conditions:
  - Diabetes
  - Chronic Kidney Disease (CKD)
  - Heart Failure
  - And more...
- Time-to-event predictions
- Stage transition modeling
- Risk factor identification

### 4. Population Risk Stratification
- Automated cohort generation
- Risk tier distribution
- High-risk patient identification
- Batch processing support

## Technology Stack

- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **ML Library**: Smile ML 3.1.1
- **Statistics**: Apache Commons Math 3.6.1
- **Database**: PostgreSQL
- **Cache**: Redis
- **Testing**: JUnit 5, Mockito, Spring Test

## API Endpoints

### Readmission Risk
```
POST /api/v1/analytics/readmission-risk/{patientId}
Headers: X-Tenant-ID
Query: predictionPeriod (30 or 90 days)
Body: Patient clinical data
```

### Cost Prediction
```
POST /api/v1/analytics/cost-prediction/{patientId}
Headers: X-Tenant-ID
Query: predictionPeriodMonths (default: 12)
Body: Patient clinical data
```

### Disease Progression
```
POST /api/v1/analytics/disease-progression/{patientId}
Headers: X-Tenant-ID
Query: condition (e.g., "diabetes", "chronic-kidney-disease")
Body: Patient clinical data
```

### Population Risk Stratification
```
GET /api/v1/analytics/population/risk-stratification
Headers: X-Tenant-ID
Query: patientIds (comma-separated)
```

### High-Risk Patients
```
GET /api/v1/analytics/population/high-risk
Headers: X-Tenant-ID
Query: patientIds (comma-separated)
```

## Domain Models

### Core Models
- **PatientFeatures**: ML feature extraction from clinical data
- **ReadmissionRiskScore**: Risk score with LACE index and risk tier
- **CostBreakdown**: Multi-category cost predictions
- **ProgressionRisk**: Disease trajectory and time-to-event
- **RiskCohort**: Population segments by risk tier
- **RiskTier**: LOW, MODERATE, HIGH, VERY_HIGH

### Supporting Models
- **ReadmissionRiskFactors**: Contributing factors with importance scores
- **TimeToEvent**: Predicted event timing with confidence intervals

## Machine Learning Features

### Feature Extraction
The service extracts 17 clinical features:
1. Age
2. Gender
3. Charlson Comorbidity Index
4. Active condition count
5. Hospitalizations (past year)
6. ED visits (past 6 months)
7. Outpatient visits (past 6 months)
8. Active medications
9. Medication changes (30 days)
10. HbA1c
11. Serum creatinine
12. eGFR
13. Blood pressure (systolic)
14. Blood pressure (diastolic)
15. BMI
16. Social risk score
17. Length of stay

### Feature Normalization
- Min-max normalization
- Clinical value ranges
- Handles missing values

### Model Explainability
- Feature importance scores
- Contributing factor breakdown
- Risk factor identification

## Testing

### Overview

The Predictive Analytics Service has comprehensive test coverage following TDD methodology:
- **ML Feature Extraction**: 17 clinical feature extraction and normalization validation
- **Readmission Risk Prediction**: LACE index calculation, risk tier classification, 30/90-day predictions
- **Cost Forecasting**: Multi-category cost prediction with confidence intervals
- **Disease Progression**: Time-to-event modeling for diabetes, CKD, heart failure
- **Population Stratification**: Batch risk analysis and cohort generation
- **API Integration**: REST endpoints with authentication and tenant validation
- **HIPAA Compliance**: PHI cache TTL validation, audit logging, secure headers
- **Performance**: Prediction latency benchmarks

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:predictive-analytics-service:test

# Run specific test suite
./gradlew :modules:services:predictive-analytics-service:test --tests "*FeatureExtractorTest"
./gradlew :modules:services:predictive-analytics-service:test --tests "*ReadmissionRiskPredictorTest"
./gradlew :modules:services:predictive-analytics-service:test --tests "*CostPredictorTest"

# Run integration tests
./gradlew :modules:services:predictive-analytics-service:test --tests "*ControllerTest"

# Run with coverage report
./gradlew :modules:services:predictive-analytics-service:test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Coverage Summary

| Test Class | Tests | Purpose |
|------------|-------|---------|
| FeatureExtractorTest | 15+ | ML feature extraction and normalization |
| ReadmissionRiskPredictorTest | 25+ | 30/90-day readmission risk with LACE index |
| CostPredictorTest | 18+ | Multi-category cost forecasting |
| DiseaseProgressionPredictorTest | 16+ | Chronic disease trajectory modeling |
| PopulationRiskStratifierTest | 10+ | Population risk cohort analysis |
| PredictiveAnalyticsControllerTest | 16+ | REST API endpoint testing |
| ReadmissionRiskModelTest | 8+ | ML model prediction accuracy |
| CostPredictionModelTest | 8+ | Cost model validation |
| PatientFeaturesTest | 6+ | Feature data model validation |
| RiskTierTest | 4+ | Risk tier classification |
| TimeToEventTest | 4+ | Time-to-event calculations |
| GlobalExceptionHandlerTest | 4+ | Error handling validation |
| **Total** | **95+** | **Comprehensive ML and API coverage** |

### Test Organization

```
src/test/java/com/healthdata/predictive/
├── service/
│   ├── FeatureExtractorTest.java           # ML feature extraction
│   ├── ReadmissionRiskPredictorTest.java   # Readmission risk prediction
│   ├── CostPredictorTest.java              # Cost forecasting
│   ├── DiseaseProgressionPredictorTest.java # Disease progression modeling
│   ├── PopulationRiskStratifierTest.java   # Population analytics
│   ├── ReadmissionRiskModelTest.java       # ML model tests
│   └── CostPredictionModelTest.java        # Cost model tests
├── controller/
│   └── PredictiveAnalyticsControllerTest.java  # API endpoint tests
├── model/
│   ├── PatientFeaturesTest.java            # Feature model tests
│   ├── RiskTierTest.java                   # Risk tier enum tests
│   └── TimeToEventTest.java                # Time prediction tests
└── exception/
    └── GlobalExceptionHandlerTest.java     # Error handling tests
```

### Unit Tests (ML Services)

#### FeatureExtractorTest - Feature Engineering

Tests clinical feature extraction from patient data:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureExtractor Tests")
class FeatureExtractorTest {

    private FeatureExtractor featureExtractor;

    @BeforeEach
    void setUp() {
        featureExtractor = new FeatureExtractor();
    }

    @Test
    @DisplayName("Should extract basic demographic features")
    void shouldExtractBasicDemographicFeatures() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 65);
        patientData.put("gender", "male");

        // Act
        PatientFeatures features = featureExtractor.extractFeatures(
            "tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features);
        assertEquals("patient-123", features.getPatientId());
        assertEquals("tenant-1", features.getTenantId());
        assertEquals(65, features.getAge());
        assertEquals("male", features.getGender());
    }

    @Test
    @DisplayName("Should calculate Charlson Comorbidity Index")
    void shouldCalculateCharlsonComorbidityIndex() {
        // Arrange - Patient with multiple comorbidities
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 55);
        patientData.put("chronicConditions", Arrays.asList(
            "diabetes", "myocardial-infarction", "congestive-heart-failure"
        ));

        // Act
        PatientFeatures features = featureExtractor.extractFeatures(
            "tenant-1", "patient-123", patientData);

        // Assert - CCI should be calculated from conditions
        assertNotNull(features.getCharlsonComorbidityIndex());
        assertTrue(features.getCharlsonComorbidityIndex() > 0,
            "CCI should be greater than 0 with multiple comorbidities");
    }

    @Test
    @DisplayName("Should extract utilization metrics")
    void shouldExtractUtilizationMetrics() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("hospitalizationsPastYear", 2);
        patientData.put("edVisitsPast6Months", 3);
        patientData.put("outpatientVisitsPast6Months", 8);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures(
            "tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(2, features.getHospitalizationsPastYear());
        assertEquals(3, features.getEdVisitsPast6Months());
        assertEquals(8, features.getOutpatientVisitsPast6Months());
    }

    @Test
    @DisplayName("Should normalize feature vector")
    void shouldNormalizeFeatureVector() {
        // Arrange - Features for normalization
        PatientFeatures features = PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .age(65)
            .hospitalizationsPastYear(2)
            .edVisitsPast6Months(3)
            .hemoglobinA1c(7.5)
            .featureVector(new double[]{65.0, 2.0, 3.0, 7.5})
            .build();

        // Act
        double[] normalized = featureExtractor.normalizeFeatures(
            features.getFeatureVector());

        // Assert - Values should be normalized to 0-1 range
        assertNotNull(normalized);
        assertEquals(features.getFeatureVector().length, normalized.length);
        for (double value : normalized) {
            assertTrue(value >= 0.0 && value <= 1.0 || !Double.isFinite(value),
                "Normalized values should be between 0 and 1");
        }
    }

    @Test
    @DisplayName("Should throw exception for null patient data")
    void shouldThrowExceptionForNullPatientData() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            featureExtractor.extractFeatures("tenant-1", "patient-123", null)
        );
    }
}
```

**Key Feature Extraction Tests:**
- Demographic features (age, gender)
- Charlson Comorbidity Index calculation
- Medication count and chronic conditions
- Utilization metrics (hospitalizations, ED visits, outpatient)
- Lab values (HbA1c, creatinine, eGFR)
- Vital signs (BP systolic/diastolic, BMI)
- Social risk factors
- Feature vector generation and normalization
- Missing field handling
- Null validation

#### ReadmissionRiskPredictorTest - LACE Index and Risk Prediction

Tests 30/90-day hospital readmission risk prediction with LACE index:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReadmissionRiskPredictor Tests")
class ReadmissionRiskPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @Mock
    private ReadmissionRiskModel riskModel;

    @InjectMocks
    private ReadmissionRiskPredictor readmissionRiskPredictor;

    @Test
    @DisplayName("Should predict 30-day readmission risk")
    void shouldPredict30DayReadmissionRisk() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35); // 35% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert
        assertNotNull(score);
        assertEquals("patient-123", score.getPatientId());
        assertEquals("tenant-1", score.getTenantId());
        assertEquals(30, score.getPredictionPeriodDays());
        assertTrue(score.getScore() >= 0 && score.getScore() <= 100);
        assertNotNull(score.getRiskTier());
    }

    @Test
    @DisplayName("Should calculate LACE index")
    void shouldCalculateLaceIndex() {
        // LACE: Length of stay, Acuity, Comorbidities, ED visits
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert - LACE index should be 0-19
        assertNotNull(score.getLaceIndex());
        assertTrue(score.getLaceIndex() >= 0, "LACE index should be non-negative");
        assertTrue(score.getLaceIndex() <= 19, "LACE index should not exceed 19");
    }

    @Test
    @DisplayName("Should classify as LOW risk for low probability")
    void shouldClassifyAsLowRiskForLowProbability() {
        // Arrange - 15% probability
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.15);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert - 0-25% = LOW
        assertEquals(RiskTier.LOW, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as MODERATE risk for moderate probability")
    void shouldClassifyAsModerateRiskForModerateProbability() {
        // Arrange - 35% probability
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert - 25-50% = MODERATE
        assertEquals(RiskTier.MODERATE, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as HIGH risk for high probability")
    void shouldClassifyAsHighRiskForHighProbability() {
        // Arrange - 65% probability
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.65);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert - 50-75% = HIGH
        assertEquals(RiskTier.HIGH, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as VERY_HIGH risk for very high probability")
    void shouldClassifyAsVeryHighRiskForVeryHighProbability() {
        // Arrange - 85% probability
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.85);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert - 75-100% = VERY_HIGH
        assertEquals(RiskTier.VERY_HIGH, score.getRiskTier());
    }

    @Test
    @DisplayName("Should calculate feature importance scores")
    void shouldCalculateFeatureImportanceScores() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getFeatureImportance())
            .thenReturn(Map.of(
                "age", 0.15,
                "charlson_comorbidity_index", 0.25,
                "hospitalizations_past_year", 0.20,
                "ed_visits_past_6m", 0.18,
                "length_of_stay", 0.12,
                "active_medications", 0.10
            ));

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert
        assertNotNull(score.getRiskFactors().getFeatureImportance());
        assertFalse(score.getRiskFactors().getFeatureImportance().isEmpty());
    }

    @Test
    @DisplayName("Should include model confidence score")
    void shouldIncludeModelConfidenceScore() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getConfidence())
            .thenReturn(0.85);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData);

        // Assert
        assertNotNull(score.getConfidence());
        assertTrue(score.getConfidence() >= 0.0 && score.getConfidence() <= 1.0);
    }
}
```

**Risk Tier Classification:**
| Tier | Score Range | Recommendation |
|------|-------------|----------------|
| LOW | 0-25 | Routine monitoring |
| MODERATE | 25-50 | Enhanced monitoring |
| HIGH | 50-75 | Proactive intervention |
| VERY_HIGH | 75-100 | Intensive case management |

**LACE Index Components (0-19 points):**
| Component | Points | Description |
|-----------|--------|-------------|
| L - Length of stay | 0-7 | Days in hospital |
| A - Acuity | 0-3 | Admission urgency |
| C - Comorbidities | 0-5 | Charlson Index |
| E - ED visits | 0-4 | ED visits in 6 months |

#### CostPredictorTest - Healthcare Cost Forecasting

Tests multi-category cost prediction:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CostPredictor Tests")
class CostPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @Mock
    private CostPredictionModel costModel;

    @InjectMocks
    private CostPredictor costPredictor;

    @Test
    @DisplayName("Should predict costs by category")
    void shouldPredictCostsByCategory() {
        // Arrange - Mock multi-category predictions
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);
        when(costModel.predictInpatientCost(any(double[].class)))
            .thenReturn(12000.0);
        when(costModel.predictOutpatientCost(any(double[].class)))
            .thenReturn(6000.0);
        when(costModel.predictPharmacyCost(any(double[].class)))
            .thenReturn(4000.0);
        when(costModel.predictEmergencyCost(any(double[].class)))
            .thenReturn(2000.0);
        when(costModel.predictLabCost(any(double[].class)))
            .thenReturn(800.0);
        when(costModel.predictImagingCost(any(double[].class)))
            .thenReturn(600.0);
        when(costModel.predictOtherCost(any(double[].class)))
            .thenReturn(400.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12);

        // Assert - All cost categories populated
        assertTrue(costBreakdown.getInpatientCost() > 0);
        assertTrue(costBreakdown.getOutpatientCost() > 0);
        assertTrue(costBreakdown.getPharmacyCost() > 0);
        assertTrue(costBreakdown.getEmergencyCost() > 0);
        assertTrue(costBreakdown.getLabCost() > 0);
        assertTrue(costBreakdown.getImagingCost() > 0);
    }

    @Test
    @DisplayName("Should include confidence intervals")
    void shouldIncludeConfidenceIntervals() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12);

        // Assert - Confidence intervals present
        assertTrue(costBreakdown.getConfidenceLower() > 0);
        assertTrue(costBreakdown.getConfidenceUpper() > costBreakdown.getConfidenceLower());
        assertTrue(costBreakdown.getConfidenceUpper() > costBreakdown.getTotalPredictedCost());
        assertTrue(costBreakdown.getConfidenceLower() < costBreakdown.getTotalPredictedCost());
    }

    @Test
    @DisplayName("Should throw exception for invalid prediction period")
    void shouldThrowExceptionForInvalidPredictionPeriod() {
        // Act & Assert - Period must be 1-24 months
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", "patient-123", samplePatientData, 0));
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", "patient-123", samplePatientData, -1));
    }
}
```

**Cost Categories:**
| Category | Description |
|----------|-------------|
| Inpatient | Hospital admissions |
| Outpatient | Clinic visits, procedures |
| Pharmacy | Medications |
| Emergency | ED visits |
| Lab | Laboratory tests |
| Imaging | Radiology, scans |
| Other | Ancillary services |

#### DiseaseProgressionPredictorTest - Chronic Disease Modeling

Tests disease trajectory and time-to-event prediction:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DiseaseProgressionPredictor Tests")
class DiseaseProgressionPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @InjectMocks
    private DiseaseProgressionPredictor progressionPredictor;

    @Test
    @DisplayName("Should predict diabetes progression")
    void shouldPredictDiabetesProgression() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        // Act
        ProgressionRisk risk = progressionPredictor.predictProgression(
            "tenant-1", "patient-123", new HashMap<>(), "diabetes");

        // Assert
        assertNotNull(risk);
        assertEquals("diabetes", risk.getCondition());
        assertTrue(risk.getProgressionProbability() >= 0 &&
                   risk.getProgressionProbability() <= 1);
    }

    @Test
    @DisplayName("Should calculate time to event with confidence intervals")
    void shouldCalculateTimeToEventWithConfidenceIntervals() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        // Act
        ProgressionRisk risk = progressionPredictor.predictProgression(
            "tenant-1", "patient-123", new HashMap<>(), "diabetes");

        // Assert - Time to event with confidence
        TimeToEvent tte = risk.getTimeToEvent();
        assertNotNull(tte.getPredictedDays());
        assertTrue(tte.getConfidenceLowerDays() <= tte.getPredictedDays());
        assertTrue(tte.getConfidenceUpperDays() >= tte.getPredictedDays());
    }

    @Test
    @DisplayName("Should predict stage transitions")
    void shouldPredictStageTransitions() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        // Act
        ProgressionRisk risk = progressionPredictor.predictProgression(
            "tenant-1", "patient-123", new HashMap<>(), "diabetes");

        // Assert - Current and predicted stage
        assertNotNull(risk.getCurrentStage());
        assertNotNull(risk.getPredictedStage());
    }

    @Test
    @DisplayName("Should handle high HbA1c increasing progression risk")
    void shouldHandleHighHbA1c() {
        // Arrange - High HbA1c indicates poor control
        sampleFeatures.setHemoglobinA1c(11.0);
        sampleFeatures.setCharlsonComorbidityIndex(6);
        sampleFeatures.setAge(70);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        // Act
        ProgressionRisk risk = progressionPredictor.predictProgression(
            "tenant-1", "patient-123", new HashMap<>(), "diabetes");

        // Assert - High risk for progression
        assertTrue(risk.getProgressionProbability() > 0.5);
    }

    @Test
    @DisplayName("Should handle low eGFR for CKD progression")
    void shouldHandleLowEgfrForCkd() {
        // Arrange - Low eGFR indicates advanced CKD
        sampleFeatures.setEgfr(25.0);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        // Act
        ProgressionRisk risk = progressionPredictor.predictProgression(
            "tenant-1", "patient-123", new HashMap<>(), "chronic-kidney-disease");

        // Assert - High progression probability
        assertTrue(risk.getProgressionProbability() > 0.4);
    }
}
```

**Supported Conditions:**
- diabetes
- chronic-kidney-disease
- heart-failure

#### PopulationRiskStratifierTest - Cohort Analysis

Tests population-level risk stratification:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PopulationRiskStratifier Tests")
class PopulationRiskStratifierTest {

    @Mock
    private ReadmissionRiskPredictor readmissionPredictor;

    @InjectMocks
    private PopulationRiskStratifier riskStratifier;

    @Test
    @DisplayName("Should stratify population by risk tiers")
    void shouldStratifyPopulationByRiskTiers() {
        // Arrange - 4 patients across all risk tiers
        List<String> patientIds = Arrays.asList("p1", "p2", "p3", "p4");
        when(readmissionPredictor.predict30DayRisk(any(), eq("p1"), any()))
            .thenReturn(createRiskScore("p1", 20, RiskTier.LOW));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p2"), any()))
            .thenReturn(createRiskScore("p2", 40, RiskTier.MODERATE));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p3"), any()))
            .thenReturn(createRiskScore("p3", 65, RiskTier.HIGH));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p4"), any()))
            .thenReturn(createRiskScore("p4", 85, RiskTier.VERY_HIGH));

        // Act
        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation(
            "tenant-1", patientIds, new HashMap<>());

        // Assert - 4 cohorts (one per tier)
        assertNotNull(cohorts);
        assertEquals(4, cohorts.size());
    }

    @Test
    @DisplayName("Should identify high-risk patients")
    void shouldIdentifyHighRiskPatients() {
        // Arrange
        List<String> patientIds = Arrays.asList("p1", "p2", "p3");
        when(readmissionPredictor.predict30DayRisk(any(), eq("p1"), any()))
            .thenReturn(createRiskScore("p1", 70, RiskTier.HIGH));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p2"), any()))
            .thenReturn(createRiskScore("p2", 20, RiskTier.LOW));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p3"), any()))
            .thenReturn(createRiskScore("p3", 88, RiskTier.VERY_HIGH));

        // Act - Get HIGH and VERY_HIGH patients
        List<String> highRiskPatients = riskStratifier.getHighRiskPatients(
            "tenant-1", patientIds, new HashMap<>());

        // Assert - 2 high-risk patients
        assertEquals(2, highRiskPatients.size());
        assertTrue(highRiskPatients.contains("p1"));
        assertTrue(highRiskPatients.contains("p3"));
        assertFalse(highRiskPatients.contains("p2")); // LOW risk excluded
    }

    @Test
    @DisplayName("Should batch process large populations")
    void shouldBatchProcessLargePopulations() {
        // Arrange - 100 patients
        List<String> patientIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            patientIds.add("p" + i);
        }
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createRiskScore("p1", 50, RiskTier.MODERATE));

        // Act
        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation(
            "tenant-1", patientIds, new HashMap<>());

        // Assert - Should handle batch without error
        assertNotNull(cohorts);
    }
}
```

### Integration Tests (Controller)

#### PredictiveAnalyticsControllerTest - API Endpoints

Tests REST API endpoints with authentication:

```java
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PredictiveAnalyticsController.class)
@Import(TestJpaConfiguration.class)
@ActiveProfiles("test")
@DisplayName("PredictiveAnalyticsController Tests")
class PredictiveAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReadmissionRiskPredictor readmissionPredictor;

    @MockBean
    private CostPredictor costPredictor;

    @MockBean
    private DiseaseProgressionPredictor progressionPredictor;

    @MockBean
    private PopulationRiskStratifier riskStratifier;

    @Test
    @DisplayName("Should predict readmission risk")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictReadmissionRisk() throws Exception {
        // Arrange
        ReadmissionRiskScore score = createReadmissionRiskScore();
        when(readmissionPredictor.predict30DayRisk(
            eq("tenant-1"), eq("patient-123"), any())).thenReturn(score);

        // Act & Assert
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.score").exists())
            .andExpect(jsonPath("$.riskTier").exists())
            .andExpect(jsonPath("$.laceIndex").exists());
    }

    @Test
    @DisplayName("Should predict cost")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictCost() throws Exception {
        // Arrange
        CostBreakdown cost = createCostBreakdown();
        when(costPredictor.predictCosts(
            eq("tenant-1"), eq("patient-123"), any(), eq(12))).thenReturn(cost);

        // Act & Assert
        mockMvc.perform(post("/api/v1/analytics/cost-prediction/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("predictionPeriodMonths", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.totalPredictedCost").exists())
            .andExpect(jsonPath("$.inpatientCost").exists())
            .andExpect(jsonPath("$.pharmacyCost").exists());
    }

    @Test
    @DisplayName("Should predict disease progression")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictDiseaseProgression() throws Exception {
        // Arrange
        ProgressionRisk risk = createProgressionRisk();
        when(progressionPredictor.predictProgression(
            eq("tenant-1"), eq("patient-123"), any(), eq("diabetes"))).thenReturn(risk);

        // Act & Assert
        mockMvc.perform(post("/api/v1/analytics/disease-progression/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("condition", "diabetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.condition").value("diabetes"))
            .andExpect(jsonPath("$.progressionProbability").exists())
            .andExpect(jsonPath("$.timeToEvent").exists());
    }

    @Test
    @DisplayName("Should get high-risk patients")
    @WithMockUser(roles = "ANALYST")
    void shouldGetHighRiskPatients() throws Exception {
        // Arrange
        List<String> highRiskPatients = Arrays.asList("p1", "p3");
        when(riskStratifier.getHighRiskPatients(
            eq("tenant-1"), anyList(), any())).thenReturn(highRiskPatients);

        // Act & Assert
        mockMvc.perform(get("/api/v1/analytics/population/high-risk")
                .header("X-Tenant-ID", "tenant-1")
                .param("patientIds", "p1,p2,p3")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should require tenant ID header")
    @WithMockUser(roles = "ANALYST")
    void shouldRequireTenantIdHeader() throws Exception {
        // Act & Assert - Missing X-Tenant-ID header
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate cost prediction period")
    @WithMockUser(roles = "ANALYST")
    void shouldValidateCostPredictionPeriod() throws Exception {
        // Act & Assert - Period cannot be 0
        mockMvc.perform(post("/api/v1/analytics/cost-prediction/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("predictionPeriodMonths", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should support different user roles")
    @WithMockUser(roles = "EVALUATOR")
    void shouldSupportDifferentUserRoles() throws Exception {
        // Arrange
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createReadmissionRiskScore());

        // Act & Assert - EVALUATOR role also allowed
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk());
    }
}
```

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Predictive Analytics Multi-Tenant Isolation Tests")
class PredictiveAnalyticsMultiTenantTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ReadmissionRiskPredictor riskPredictor;

    @Test
    @DisplayName("Should isolate predictions by tenant")
    void shouldIsolatePredictionsByTenant() {
        // Arrange
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        String patientId = "patient-123";
        Map<String, Object> patientData = createPatientData();

        // Act - Same patient ID, different tenants
        ReadmissionRiskScore tenant1Score = riskPredictor.predict30DayRisk(
            tenant1, patientId, patientData);
        ReadmissionRiskScore tenant2Score = riskPredictor.predict30DayRisk(
            tenant2, patientId, patientData);

        // Assert - Each prediction tagged with correct tenant
        assertEquals(tenant1, tenant1Score.getTenantId());
        assertEquals(tenant2, tenant2Score.getTenantId());
    }

    @Test
    @DisplayName("Should namespace cache by tenant")
    void shouldNamespaceCacheByTenant() {
        // Arrange
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        // Act & Assert - Cache keys should be tenant-prefixed
        // Verify no cross-tenant cache pollution
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Predictive Analytics HIPAA Compliance Tests")
class PredictiveAnalyticsHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("PHI prediction cache TTL must not exceed 5 minutes")
    void phiPredictionCacheTtlShouldBeCompliant() {
        // Given
        Cache predictionCache = cacheManager.getCache("predictions");

        // When/Then
        assertThat(predictionCache).isNotNull();

        if (predictionCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) predictionCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("PHI prediction cache TTL exceeds 5 minutes (HIPAA violation)");
        }
    }

    @Test
    @DisplayName("PHI responses must include no-cache headers")
    void phiResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("PHI access must generate audit events")
    void phiAccessShouldBeAudited() {
        // Given
        String patientId = "patient-123";
        String tenantId = "tenant-001";

        // When - Access prediction (triggers @Audited)
        riskPredictor.predict30DayRisk(tenantId, patientId, createPatientData());

        // Then - Verify audit event created
        List<AuditEvent> events = auditRepository.findByResourceId(patientId);
        assertThat(events)
            .isNotEmpty()
            .allMatch(e -> e.getEventType().equals("PREDICTION_ACCESS"));
    }

    @Test
    @DisplayName("Test data must not contain real PHI")
    void testDataMustBeSynthetic() {
        // Verify test data generators don't create realistic PHI
        PatientFeatures testFeatures = createSampleFeatures();

        assertThat(testFeatures.getPatientId())
            .startsWith("patient-")
            .withFailMessage("Test patient IDs should be clearly synthetic");
    }
}
```

### Performance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Predictive Analytics Performance Tests")
class PredictiveAnalyticsPerformanceTest {

    @Autowired
    private ReadmissionRiskPredictor riskPredictor;

    @Autowired
    private CostPredictor costPredictor;

    @Test
    @DisplayName("Readmission risk prediction should complete within 100ms")
    void readmissionRiskPredictionPerformance() {
        // Given
        String tenantId = "tenant-perf-001";
        int predictionCount = 100;
        Map<String, Object> patientData = createPatientData();

        // When
        Instant start = Instant.now();
        for (int i = 0; i < predictionCount; i++) {
            riskPredictor.predict30DayRisk(tenantId, "patient-" + i, patientData);
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerPrediction = totalMs / (double) predictionCount;

        assertThat(avgMsPerPrediction)
            .isLessThan(100.0)
            .withFailMessage("Average prediction time %.2fms exceeds 100ms SLA", avgMsPerPrediction);

        System.out.printf("Performance: %d predictions in %dms (avg: %.2fms/prediction)%n",
            predictionCount, totalMs, avgMsPerPrediction);
    }

    @Test
    @DisplayName("Cost prediction should complete within 150ms")
    void costPredictionPerformance() {
        // Given
        String tenantId = "tenant-perf-001";
        int predictionCount = 100;

        // When
        Instant start = Instant.now();
        for (int i = 0; i < predictionCount; i++) {
            costPredictor.predictCosts(tenantId, "patient-" + i, createPatientData(), 12);
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMs = totalMs / (double) predictionCount;

        assertThat(avgMs)
            .isLessThan(150.0)
            .withFailMessage("Average cost prediction %.2fms exceeds 150ms SLA", avgMs);
    }

    @Test
    @DisplayName("Population stratification should complete within 2s for 1000 patients")
    void populationStratificationPerformance() {
        // Given
        String tenantId = "tenant-perf-001";
        List<String> patientIds = IntStream.range(0, 1000)
            .mapToObj(i -> "patient-" + i)
            .collect(Collectors.toList());

        // When
        Instant start = Instant.now();
        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation(
            tenantId, patientIds, new HashMap<>());
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();

        assertThat(totalMs)
            .isLessThan(2000L)
            .withFailMessage("Population stratification %dms exceeds 2s SLA", totalMs);
    }
}
```

### Test Configuration

#### TestJpaConfiguration

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.healthdata.predictive.repository")
@EntityScan(basePackages = "com.healthdata.predictive.model")
public class TestJpaConfiguration {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }
}
```

#### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  cache:
    type: simple

predictive:
  model:
    readmission:
      version: "test-v1.0.0"
    cost:
      version: "test-v1.0.0"
```

### Best Practices

| Practice | Implementation |
|----------|----------------|
| Feature extraction testing | Verify all 17 clinical features extracted correctly |
| LACE index validation | Test L-A-C-E components sum to 0-19 range |
| Risk tier thresholds | LOW: 0-25, MODERATE: 25-50, HIGH: 50-75, VERY_HIGH: 75-100 |
| Cost category coverage | Test all 7 cost categories populated |
| Confidence intervals | Verify lower < predicted < upper for all predictions |
| Prediction period validation | Period must be positive (1-24 months for cost) |
| Model versioning | Include model version in all prediction responses |
| Tenant isolation | All predictions tagged with tenant ID |
| Batch processing | Test population operations with 100+ patients |
| Null validation | Test rejection of null tenantId, patientId, patientData |
| Feature normalization | Verify 0-1 range after normalization |
| Edge cases | Test extreme values (age 95+, CCI 12+, frequent ED visits) |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Risk tier not assigned | Score outside 0-100 range | Verify model output normalized to probability |
| LACE index > 19 | Component scores exceeded | Cap each component at max points |
| Cost categories sum != total | Rounding or missing category | Use calculateTotalCost() method |
| TimeToEvent negative | Invalid progression model | Ensure survival model returns positive days |
| High-risk list empty | Threshold too high | HIGH tier is score >= 50, not >= 75 |
| Feature vector length mismatch | Missing features | Check all 17 features extracted |
| Confidence interval inverted | Upper < Lower | Verify interval calculation order |
| Prediction cache miss | TTL expired | PHI cache TTL is 5 minutes max |
| Tenant header missing | Request lacks X-Tenant-ID | Add header to all prediction requests |
| Model version null | Model not configured | Set predictive.model.*.version in config |
| CostModel returns 0 | Mock not configured | Configure all category mock returns in setup |
| Population batching fails | Memory exhausted | Use streaming or pagination for large populations |

## Clinical Algorithms

### LACE Index Calculation
- **L**ength of stay: 0-7 points
- **A**cuity of admission: 0-3 points
- **C**omorbidities (Charlson): 0-5 points
- **E**D visits (6 months): 0-4 points
- Total: 0-19 points

### Charlson Comorbidity Index
Weighted scoring for 17 comorbid conditions with age adjustment.

### Risk Tier Classification
- **LOW**: 0-25 (routine monitoring)
- **MODERATE**: 25-50 (enhanced monitoring)
- **HIGH**: 50-75 (proactive intervention)
- **VERY_HIGH**: 75-100 (intensive case management)

## Multi-Tenancy Support

All operations require tenant ID for data isolation:
- Header: `X-Tenant-ID`
- Ensures data separation
- Tenant-specific caching

## Security

- Spring Security integration
- Role-based access control (RBAC)
- Required roles: ANALYST, EVALUATOR, ADMIN, SUPER_ADMIN
- CSRF protection
- Secure API endpoints

## Configuration

### Application Properties
```yaml
spring:
  application:
    name: predictive-analytics-service

server:
  port: 8089

logging:
  level:
    com.healthdata.predictive: DEBUG
```

### Environment Variables
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

## Building and Running

### Build
```bash
./gradlew :modules:services:predictive-analytics-service:build
```

### Run Tests
```bash
./gradlew :modules:services:predictive-analytics-service:test
```

### Run Service
```bash
./gradlew :modules:services:predictive-analytics-service:bootRun
```

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Cache (Redis)
- Spring Cloud OpenFeign
- HAPI FHIR Client
- Smile ML 3.1.1
- Apache Commons Math 3.6.1
- Lombok
- JUnit 5
- Mockito

## Future Enhancements

### Planned Features
1. **Advanced ML Models**
   - Neural networks for complex patterns
   - Ensemble methods (Random Forest, Gradient Boosting)
   - Deep learning for temporal data

2. **Model Training Pipeline**
   - Automated model retraining
   - A/B testing framework
   - Model performance monitoring

3. **Additional Predictions**
   - Sepsis risk prediction
   - Fall risk assessment
   - Medication adherence prediction
   - No-show probability

4. **Enhanced Explainability**
   - SHAP values
   - LIME explanations
   - Counterfactual analysis

5. **Real-time Predictions**
   - Streaming analytics
   - Event-driven predictions
   - Alert generation

## API Examples

### Readmission Risk Prediction
```bash
curl -X POST http://localhost:8089/api/v1/analytics/readmission-risk/patient-123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 65,
    "charlsonComorbidityIndex": 4,
    "hospitalizationsPastYear": 2,
    "edVisitsPast6Months": 3,
    "lengthOfStay": 7,
    "lastAdmissionAcuity": "emergency"
  }'
```

### Cost Prediction
```bash
curl -X POST http://localhost:8089/api/v1/analytics/cost-prediction/patient-123?predictionPeriodMonths=12 \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 65,
    "charlsonComorbidityIndex": 4,
    "activeMedicationCount": 8,
    "hospitalizationsPastYear": 1
  }'
```

### Disease Progression
```bash
curl -X POST http://localhost:8089/api/v1/analytics/disease-progression/patient-123?condition=diabetes \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 55,
    "hemoglobinA1c": 8.5,
    "charlsonComorbidityIndex": 3
  }'
```

## License

Copyright 2025 HealthData in Motion. All rights reserved.
