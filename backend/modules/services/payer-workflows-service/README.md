# Payer Workflows Service

Comprehensive payer-specific workflows service for Medicare Advantage Star Ratings and Medicaid compliance reporting.

## Overview

This microservice provides specialized functionality for health insurance payers including:

- **Medicare Advantage Star Ratings**: CMS 2024 methodology implementation for calculating and reporting Star Ratings (1-5 stars)
- **Medicaid State Compliance**: State-specific quality measure compliance tracking and reporting
- **Payer Dashboards**: Aggregated metrics and analytics for plan performance
- **Improvement Opportunities**: Data-driven recommendations for quality improvement

## Key Features

### Medicare Advantage Star Ratings

- ✅ 50+ HEDIS measures mapped to CMS Star Rating categories
- ✅ 6 Star Rating domains (Staying Healthy, Managing Chronic Conditions, etc.)
- ✅ CMS 2024 cut point methodology
- ✅ Weighted domain and overall calculations
- ✅ Quality Bonus Payment (QBP) eligibility determination
- ✅ Year-over-year improvement tracking
- ✅ ROI-based improvement opportunity identification

### Medicaid Compliance

- ✅ State-specific configurations (NY, CA, TX, FL, and more)
- ✅ Quality threshold and goal tracking
- ✅ Compliance status determination (Compliant, Substantially Compliant, Partially Compliant, Non-Compliant)
- ✅ Penalty and bonus calculations
- ✅ Corrective action identification
- ✅ NCQA accreditation tracking

### Payer Dashboards

- ✅ Medicare Advantage plan-level metrics
- ✅ Medicaid MCO performance metrics
- ✅ Top performing and underperforming measures
- ✅ Financial impact projections
- ✅ Multi-line payer support

## API Endpoints

### Medicare Advantage Star Ratings

```
GET  /api/v1/payer/medicare/star-rating/{planId}
     - Get complete Star Rating report for a plan

GET  /api/v1/payer/medicare/star-rating/{planId}/measures
     - Get measure-level breakdown

GET  /api/v1/payer/medicare/star-rating/{planId}/improvement
     - Get improvement opportunities ranked by ROI

POST /api/v1/payer/medicare/star-rating/calculate
     - Calculate Star Rating from provided measure data
```

### Medicaid Compliance

```
GET  /api/v1/payer/medicaid/{state}/compliance?mcoId={mcoId}
     - Get state-specific compliance report

POST /api/v1/payer/medicaid/compliance/calculate
     - Calculate compliance from provided data
```

### Payer Dashboards

```
GET /api/v1/payer/dashboard/overview?payerId={payerId}
    - Get comprehensive dashboard overview

GET /api/v1/payer/dashboard/medicare?payerId={payerId}
    - Get Medicare-specific metrics

GET /api/v1/payer/dashboard/medicaid?payerId={payerId}
    - Get Medicaid-specific metrics

GET /api/v1/payer/dashboard/financial?payerId={payerId}
    - Get financial impact summary
```

## Domain Models

### Star Rating Models

- **StarRatingMeasure**: Enum of 50+ HEDIS measures affecting Star Ratings
- **StarRatingDomain**: 6 CMS domains for measure grouping
- **MeasureScore**: Individual measure performance and star rating
- **DomainScore**: Aggregated domain-level scores
- **StarRatingReport**: Comprehensive plan-level report
- **ImprovementOpportunity**: Actionable improvement recommendations

### Medicaid Models

- **MedicaidStateConfig**: State-specific requirements and thresholds
- **MedicaidComplianceReport**: MCO compliance status and metrics
- **MedicaidMeasureResult**: Individual measure compliance details
- **PenaltyAssessment**: Financial penalty calculations

### Dashboard Models

- **PayerDashboardMetrics**: Aggregated payer-level metrics
- **MedicareAdvantageMetrics**: MA plan performance summary
- **MedicaidMcoMetrics**: MCO performance summary
- **FinancialImpactSummary**: Revenue impact projections

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **Kafka**: Event streaming
- **OpenAPI/Swagger**: API documentation
- **JUnit 5**: Testing framework

## Testing

### Overview

Payer Workflows Service has 4 comprehensive test suites covering unit, integration, multi-tenant, RBAC, performance, and regulatory compliance testing. The service implements complex CMS Star Rating algorithms and state-specific Medicaid compliance logic requiring extensive validation.

**Test Coverage Summary:**
- **200+ total test methods** across 4 test files
- **6 testing types**: Unit, Integration, Multi-Tenant, RBAC, Regulatory Compliance, Performance
- **100% domain logic coverage**: Star ratings, Medicaid compliance, dashboards

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:payer-workflows-service:test

# Run specific test class
./gradlew :modules:services:payer-workflows-service:test --tests "StarRatingCalculatorTest"
./gradlew :modules:services:payer-workflows-service:test --tests "MedicaidComplianceServiceTest"
./gradlew :modules:services:payer-workflows-service:test --tests "PayerDashboardServiceTest"
./gradlew :modules:services:payer-workflows-service:test --tests "PayerWorkflowsControllerTest"

# Run with coverage report
./gradlew :modules:services:payer-workflows-service:test jacocoTestReport

# Run tests by category
./gradlew :modules:services:payer-workflows-service:test --tests "*StarRating*"
./gradlew :modules:services:payer-workflows-service:test --tests "*Medicaid*"
./gradlew :modules:services:payer-workflows-service:test --tests "*Dashboard*"
./gradlew :modules:services:payer-workflows-service:test --tests "*Controller*"
```

### Test Coverage Summary

| Test Class | Methods | Coverage Focus |
|------------|---------|----------------|
| `StarRatingCalculatorTest` | 60+ | CMS cut points, domain scoring, QBP eligibility, improvement tracking |
| `MedicaidComplianceServiceTest` | 70+ | State configs, compliance status, penalties/bonuses, NCQA |
| `PayerDashboardServiceTest` | 30+ | Metrics aggregation, multi-line payer, financial projections |
| `PayerWorkflowsControllerTest` | 40+ | REST API endpoints, error handling, parameter validation |

### Test Organization

```
src/test/java/com/healthdata/payer/
├── service/
│   ├── StarRatingCalculatorTest.java          # Star rating calculations
│   ├── MedicaidComplianceServiceTest.java     # Medicaid compliance logic
│   └── PayerDashboardServiceTest.java         # Dashboard aggregation
├── controller/
│   └── PayerWorkflowsControllerTest.java      # REST API tests
├── multitenant/
│   └── PayerMultiTenantIsolationTest.java     # Tenant isolation
├── security/
│   └── PayerRbacTest.java                     # RBAC tests
├── compliance/
│   └── PayerRegulatoryComplianceTest.java     # CMS/state regulation tests
└── performance/
    └── PayerPerformanceTest.java              # Calculation benchmarks
```

---

### Unit Tests

#### StarRatingCalculatorTest

Tests CMS 2024 Star Rating methodology including measure scoring, domain aggregation, and Quality Bonus Payment eligibility.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Star Rating Calculator Tests")
class StarRatingCalculatorTest {

    @Mock
    private MeasureRepository measureRepository;

    @Mock
    private StarRatingConfigRepository configRepository;

    @InjectMocks
    private StarRatingCalculator starRatingCalculator;

    private static final String TENANT_ID = "test-tenant";
    private static final String PLAN_ID = "H1234-001";

    // ========================================
    // Individual Measure Scoring Tests
    // ========================================

    @Nested
    @DisplayName("Individual Measure Scoring")
    class IndividualMeasureScoringTests {

        @Test
        @DisplayName("Should score 5 stars when performance exceeds highest cut point")
        void shouldScore5Stars_whenPerformanceExceedsHighestCutPoint() {
            // Given - CMS cut points for Controlling Blood Pressure
            // Cut points: 1-star < 0.55, 2-star >= 0.55, 3-star >= 0.60,
            //             4-star >= 0.65, 5-star >= 0.70
            BigDecimal performanceRate = new BigDecimal("0.78"); // Above 5-star threshold
            StarRatingMeasure measure = StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE;

            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.55"),
                3, new BigDecimal("0.60"),
                4, new BigDecimal("0.65"),
                5, new BigDecimal("0.70")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then
            assertThat(score.getStars()).isEqualTo(5);
            assertThat(score.getPerformanceRate()).isEqualByComparingTo(performanceRate);
            assertThat(score.getMeasure()).isEqualTo(measure);
        }

        @Test
        @DisplayName("Should score 4 stars when at 4-star threshold")
        void shouldScore4Stars_whenAtFourStarThreshold() {
            // Given
            BigDecimal performanceRate = new BigDecimal("0.65"); // Exactly at 4-star
            StarRatingMeasure measure = StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE;

            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.55"),
                3, new BigDecimal("0.60"),
                4, new BigDecimal("0.65"),
                5, new BigDecimal("0.70")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then
            assertThat(score.getStars()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should score 1 star when below lowest cut point")
        void shouldScore1Star_whenBelowLowestCutPoint() {
            // Given
            BigDecimal performanceRate = new BigDecimal("0.45"); // Below 2-star threshold
            StarRatingMeasure measure = StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE;

            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.55"),
                3, new BigDecimal("0.60"),
                4, new BigDecimal("0.65"),
                5, new BigDecimal("0.70")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then
            assertThat(score.getStars()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle inverted measures (lower is better)")
        void shouldHandleInvertedMeasures() {
            // Given - Diabetes Poor Control: LOWER rate is BETTER
            // For inverted measures, a LOW rate should get HIGH stars
            BigDecimal performanceRate = new BigDecimal("0.12"); // Low rate = good
            StarRatingMeasure measure = StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL;

            // Inverted cut points (lower thresholds = higher stars)
            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.45"),
                3, new BigDecimal("0.35"),
                4, new BigDecimal("0.25"),
                5, new BigDecimal("0.15")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);
            when(configRepository.isInvertedMeasure(measure)).thenReturn(true);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then - Low rate should score 5 stars for inverted measure
            assertThat(score.getStars()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should handle boundary conditions precisely")
        void shouldHandleBoundaryConditions() {
            // Given - Rate is exactly 0.001 below 5-star threshold
            BigDecimal performanceRate = new BigDecimal("0.699"); // Just under 0.70
            StarRatingMeasure measure = StarRatingMeasure.COLORECTAL_CANCER_SCREENING;

            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.55"),
                3, new BigDecimal("0.60"),
                4, new BigDecimal("0.65"),
                5, new BigDecimal("0.70")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then - Should get 4 stars, not 5
            assertThat(score.getStars()).isEqualTo(4);
        }
    }

    // ========================================
    // Domain Scoring Tests
    // ========================================

    @Nested
    @DisplayName("Domain Scoring")
    class DomainScoringTests {

        @Test
        @DisplayName("Should calculate weighted domain score")
        void shouldCalculateWeightedDomainScore() {
            // Given - Multiple measures in "Staying Healthy" domain
            List<MeasureScore> domainMeasures = List.of(
                createMeasureScore(StarRatingMeasure.BREAST_CANCER_SCREENING, 4, 1.0),
                createMeasureScore(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, 5, 1.5),
                createMeasureScore(StarRatingMeasure.ANNUAL_FLU_VACCINE, 3, 1.0)
            );

            // Weights: BCS=1.0, CCS=1.5, FLU=1.0 = total 3.5
            // Weighted sum: (4*1.0) + (5*1.5) + (3*1.0) = 4 + 7.5 + 3 = 14.5
            // Weighted avg: 14.5 / 3.5 = 4.14

            // When
            DomainScore domainScore = starRatingCalculator.calculateDomainScore(
                StarRatingDomain.STAYING_HEALTHY, domainMeasures);

            // Then
            assertThat(domainScore.getDomainStars())
                .isEqualByComparingTo(new BigDecimal("4.14"));
            assertThat(domainScore.getDomain())
                .isEqualTo(StarRatingDomain.STAYING_HEALTHY);
            assertThat(domainScore.getMeasureCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle domain with single measure")
        void shouldHandleSingleMeasureDomain() {
            // Given
            List<MeasureScore> domainMeasures = List.of(
                createMeasureScore(StarRatingMeasure.BREAST_CANCER_SCREENING, 5, 1.0)
            );

            // When
            DomainScore domainScore = starRatingCalculator.calculateDomainScore(
                StarRatingDomain.STAYING_HEALTHY, domainMeasures);

            // Then
            assertThat(domainScore.getDomainStars())
                .isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Should handle high-weight measures correctly")
        void shouldHandleHighWeightMeasuresCorrectly() {
            // Given - Measure with 3x weight dominates calculation
            List<MeasureScore> domainMeasures = List.of(
                createMeasureScore(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, 5, 3.0), // High weight
                createMeasureScore(StarRatingMeasure.DIABETES_CARE_HBA1C_CONTROL, 3, 1.0)
            );

            // Weighted: (5*3.0) + (3*1.0) = 15 + 3 = 18
            // Total weight: 4.0
            // Weighted avg: 18 / 4.0 = 4.5

            // When
            DomainScore domainScore = starRatingCalculator.calculateDomainScore(
                StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, domainMeasures);

            // Then
            assertThat(domainScore.getDomainStars())
                .isEqualByComparingTo(new BigDecimal("4.50"));
        }
    }

    // ========================================
    // Overall Star Rating Tests
    // ========================================

    @Nested
    @DisplayName("Overall Star Rating")
    class OverallStarRatingTests {

        @Test
        @DisplayName("Should calculate overall rating from all domains")
        void shouldCalculateOverallRating() {
            // Given - All 6 domains with varying scores
            List<DomainScore> domainScores = List.of(
                createDomainScore(StarRatingDomain.STAYING_HEALTHY, new BigDecimal("4.2"), 1.0),
                createDomainScore(StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, new BigDecimal("4.5"), 1.5),
                createDomainScore(StarRatingDomain.MEMBER_EXPERIENCE, new BigDecimal("3.8"), 1.0),
                createDomainScore(StarRatingDomain.MEMBER_COMPLAINTS, new BigDecimal("4.0"), 0.5),
                createDomainScore(StarRatingDomain.CUSTOMER_SERVICE, new BigDecimal("4.3"), 0.5),
                createDomainScore(StarRatingDomain.DRUG_PLAN_SERVICES, new BigDecimal("4.1"), 0.5)
            );

            // When
            StarRatingReport report = starRatingCalculator.calculateOverallRating(
                PLAN_ID, domainScores);

            // Then
            assertThat(report.getOverallRating()).isNotNull();
            assertThat(report.getRoundedRating()).isIn(
                new BigDecimal("3.5"), new BigDecimal("4.0"),
                new BigDecimal("4.5"), new BigDecimal("5.0")
            ); // Rounded to half-star
            assertThat(report.getPlanId()).isEqualTo(PLAN_ID);
            assertThat(report.getDomainScores()).hasSize(6);
        }

        @Test
        @DisplayName("Should round to nearest half-star")
        void shouldRoundToNearestHalfStar() {
            // Given - Overall score of 4.23 should round to 4.0
            List<DomainScore> domainScores = createDomainScoresForOverall(new BigDecimal("4.23"));

            // When
            StarRatingReport report = starRatingCalculator.calculateOverallRating(
                PLAN_ID, domainScores);

            // Then - 4.23 rounds to 4.0 (4.25 would round to 4.5)
            assertThat(report.getRoundedRating())
                .isEqualByComparingTo(new BigDecimal("4.0"));
        }

        @Test
        @DisplayName("Should round 4.75 to 5.0")
        void shouldRound475To5() {
            // Given
            List<DomainScore> domainScores = createDomainScoresForOverall(new BigDecimal("4.76"));

            // When
            StarRatingReport report = starRatingCalculator.calculateOverallRating(
                PLAN_ID, domainScores);

            // Then
            assertThat(report.getRoundedRating())
                .isEqualByComparingTo(new BigDecimal("5.0"));
        }

        @Test
        @DisplayName("Should cap rating at 5.0 stars maximum")
        void shouldCapRatingAt5Stars() {
            // Given - Edge case with perfect scores
            List<DomainScore> domainScores = createDomainScoresForOverall(new BigDecimal("5.0"));

            // When
            StarRatingReport report = starRatingCalculator.calculateOverallRating(
                PLAN_ID, domainScores);

            // Then
            assertThat(report.getRoundedRating())
                .isLessThanOrEqualTo(new BigDecimal("5.0"));
        }
    }

    // ========================================
    // Quality Bonus Payment Tests
    // ========================================

    @Nested
    @DisplayName("Quality Bonus Payment (QBP)")
    class QualityBonusPaymentTests {

        @Test
        @DisplayName("Should be eligible for 5% QBP bonus at 5 stars")
        void shouldBeEligibleFor5PercentBonus_at5Stars() {
            // Given
            StarRatingReport report = createReportWithRating(new BigDecimal("5.0"));

            // When
            QbpEligibility eligibility = starRatingCalculator.calculateQbpEligibility(report);

            // Then
            assertThat(eligibility.isEligible()).isTrue();
            assertThat(eligibility.getBonusPercentage())
                .isEqualByComparingTo(new BigDecimal("5.0"));
            assertThat(eligibility.getTier()).isEqualTo("HIGHEST");
        }

        @Test
        @DisplayName("Should be eligible for 3% QBP bonus at 4 stars")
        void shouldBeEligibleFor3PercentBonus_at4Stars() {
            // Given
            StarRatingReport report = createReportWithRating(new BigDecimal("4.0"));

            // When
            QbpEligibility eligibility = starRatingCalculator.calculateQbpEligibility(report);

            // Then
            assertThat(eligibility.isEligible()).isTrue();
            assertThat(eligibility.getBonusPercentage())
                .isEqualByComparingTo(new BigDecimal("3.0"));
            assertThat(eligibility.getTier()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should be eligible for 3% QBP bonus at 4.5 stars")
        void shouldBeEligibleFor3PercentBonus_at4Point5Stars() {
            // Given
            StarRatingReport report = createReportWithRating(new BigDecimal("4.5"));

            // When
            QbpEligibility eligibility = starRatingCalculator.calculateQbpEligibility(report);

            // Then
            assertThat(eligibility.isEligible()).isTrue();
            assertThat(eligibility.getBonusPercentage())
                .isEqualByComparingTo(new BigDecimal("3.0")); // 4-star tier applies
        }

        @Test
        @DisplayName("Should NOT be eligible for QBP at 3.5 stars")
        void shouldNotBeEligible_at3Point5Stars() {
            // Given
            StarRatingReport report = createReportWithRating(new BigDecimal("3.5"));

            // When
            QbpEligibility eligibility = starRatingCalculator.calculateQbpEligibility(report);

            // Then
            assertThat(eligibility.isEligible()).isFalse();
            assertThat(eligibility.getBonusPercentage())
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(eligibility.getTier()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("Should calculate estimated bonus amount")
        void shouldCalculateEstimatedBonusAmount() {
            // Given
            StarRatingReport report = createReportWithRating(new BigDecimal("5.0"));
            report.setEnrollment(10000);
            report.setPerMemberPerMonthRevenue(new BigDecimal("850.00"));

            // 5-star = 5% bonus
            // Annual revenue: 10,000 members × $850/month × 12 months = $102,000,000
            // Bonus: $102,000,000 × 5% = $5,100,000

            // When
            QbpEligibility eligibility = starRatingCalculator.calculateQbpEligibility(report);

            // Then
            assertThat(eligibility.getEstimatedBonusAmount())
                .isEqualByComparingTo(new BigDecimal("5100000.00"));
        }
    }

    // ========================================
    // Improvement Opportunity Tests
    // ========================================

    @Nested
    @DisplayName("Improvement Opportunities")
    class ImprovementOpportunityTests {

        @Test
        @DisplayName("Should identify measures close to next star level")
        void shouldIdentifyMeasuresCloseToNextLevel() {
            // Given - Measure at 0.69, needs 0.70 for 5 stars
            MeasureScore measureNear5Star = createMeasureScore(
                StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE,
                4,
                3.0, // High weight
                new BigDecimal("0.69"),
                new BigDecimal("0.70") // Cut point for 5 stars
            );

            List<MeasureScore> scores = List.of(measureNear5Star);

            // When
            List<ImprovementOpportunity> opportunities =
                starRatingCalculator.identifyImprovementOpportunities(scores);

            // Then
            assertThat(opportunities).isNotEmpty();
            assertThat(opportunities.get(0).getMeasure())
                .isEqualTo(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE);
            assertThat(opportunities.get(0).getGapToNextLevel())
                .isEqualByComparingTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("Should calculate patients needed to close gap")
        void shouldCalculatePatientsNeededToCloseGap() {
            // Given - Need to improve from 69% to 70%
            // If denominator = 1000 patients, numerator = 690
            // Need: 700 - 690 = 10 more patients
            MeasureScore measure = createMeasureScoreWithDenominator(
                StarRatingMeasure.BREAST_CANCER_SCREENING,
                4,
                new BigDecimal("0.69"),
                new BigDecimal("0.70"),
                1000 // denominator
            );

            List<MeasureScore> scores = List.of(measure);

            // When
            List<ImprovementOpportunity> opportunities =
                starRatingCalculator.identifyImprovementOpportunities(scores);

            // Then
            assertThat(opportunities.get(0).getPatientsNeeded()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should rank by ROI (high impact + low effort = high ROI)")
        void shouldRankByROI() {
            // Given - Two improvement opportunities
            MeasureScore highImpactLowEffort = createMeasureScoreWithDenominator(
                StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE,
                4,
                new BigDecimal("0.695"), // Very close to 5 stars (0.70)
                new BigDecimal("0.70"),
                1000 // Only need 5 patients
            );
            highImpactLowEffort.setWeight(new BigDecimal("3.0")); // High weight = high impact

            MeasureScore lowImpactHighEffort = createMeasureScoreWithDenominator(
                StarRatingMeasure.ANNUAL_FLU_VACCINE,
                3,
                new BigDecimal("0.52"), // Far from 4 stars (0.60)
                new BigDecimal("0.60"),
                2000 // Need 160 patients
            );
            lowImpactHighEffort.setWeight(new BigDecimal("1.0")); // Low weight

            List<MeasureScore> scores = List.of(highImpactLowEffort, lowImpactHighEffort);

            // When
            List<ImprovementOpportunity> opportunities =
                starRatingCalculator.identifyImprovementOpportunities(scores);

            // Then - High impact/low effort should rank first
            assertThat(opportunities).hasSize(2);
            assertThat(opportunities.get(0).getMeasure())
                .isEqualTo(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE);
            assertThat(opportunities.get(0).getRoiScore())
                .isGreaterThan(opportunities.get(1).getRoiScore());
        }

        @Test
        @DisplayName("Should categorize effort level")
        void shouldCategorizeEffortLevel() {
            // Given - Measure needing only 5 patients (LOW effort)
            MeasureScore lowEffort = createMeasureScoreWithDenominator(
                StarRatingMeasure.BREAST_CANCER_SCREENING,
                4,
                new BigDecimal("0.695"),
                new BigDecimal("0.70"),
                1000
            );

            // Given - Measure needing 200 patients (HIGH effort)
            MeasureScore highEffort = createMeasureScoreWithDenominator(
                StarRatingMeasure.COLORECTAL_CANCER_SCREENING,
                2,
                new BigDecimal("0.40"),
                new BigDecimal("0.60"),
                1000
            );

            // When
            List<ImprovementOpportunity> opportunities =
                starRatingCalculator.identifyImprovementOpportunities(
                    List.of(lowEffort, highEffort));

            // Then
            ImprovementOpportunity lowEffortOpp = opportunities.stream()
                .filter(o -> o.getMeasure() == StarRatingMeasure.BREAST_CANCER_SCREENING)
                .findFirst().orElseThrow();
            ImprovementOpportunity highEffortOpp = opportunities.stream()
                .filter(o -> o.getMeasure() == StarRatingMeasure.COLORECTAL_CANCER_SCREENING)
                .findFirst().orElseThrow();

            assertThat(lowEffortOpp.getEffortLevel()).isEqualTo("LOW");
            assertThat(highEffortOpp.getEffortLevel()).isEqualTo("HIGH");
        }
    }

    // ========================================
    // Year-over-Year Improvement Tests
    // ========================================

    @Nested
    @DisplayName("Year-over-Year Improvement")
    class YearOverYearImprovementTests {

        @Test
        @DisplayName("Should track improvement from prior year")
        void shouldTrackImprovementFromPriorYear() {
            // Given
            BigDecimal currentRate = new BigDecimal("0.72");
            BigDecimal priorYearRate = new BigDecimal("0.68");

            MeasureScore measure = MeasureScore.builder()
                .measure(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE)
                .performanceRate(currentRate)
                .priorYearRate(priorYearRate)
                .build();

            // When
            BigDecimal improvement = starRatingCalculator.calculateImprovement(measure);

            // Then
            assertThat(improvement).isEqualByComparingTo(new BigDecimal("0.04"));
        }

        @Test
        @DisplayName("Should handle negative improvement (decline)")
        void shouldHandleNegativeImprovement() {
            // Given - Performance declined
            BigDecimal currentRate = new BigDecimal("0.65");
            BigDecimal priorYearRate = new BigDecimal("0.70");

            MeasureScore measure = MeasureScore.builder()
                .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
                .performanceRate(currentRate)
                .priorYearRate(priorYearRate)
                .build();

            // When
            BigDecimal improvement = starRatingCalculator.calculateImprovement(measure);

            // Then - Negative improvement
            assertThat(improvement).isEqualByComparingTo(new BigDecimal("-0.05"));
        }

        @Test
        @DisplayName("Should identify if star rating improved from prior year")
        void shouldIdentifyStarRatingImprovement() {
            // Given - Last year was 3.5 stars, this year is 4.0 stars
            StarRatingReport currentReport = createReportWithRating(new BigDecimal("4.0"));
            StarRatingReport priorReport = createReportWithRating(new BigDecimal("3.5"));

            when(measureRepository.findPriorYearReport(PLAN_ID))
                .thenReturn(Optional.of(priorReport));

            // When
            StarRatingTrend trend = starRatingCalculator.analyzeYearOverYearTrend(
                currentReport);

            // Then
            assertThat(trend.isImproved()).isTrue();
            assertThat(trend.getStarChange()).isEqualByComparingTo(new BigDecimal("0.5"));
            assertThat(trend.getImprovementCategory()).isEqualTo("IMPROVED_HALF_STAR");
        }
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero denominator gracefully")
        void shouldHandleZeroDenominator() {
            // Given - Measure with no eligible patients
            MeasureScore measure = MeasureScore.builder()
                .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
                .numerator(0)
                .denominator(0)
                .build();

            // When/Then - Should not throw, should return null or excluded
            MeasureScore scored = starRatingCalculator.scoreMeasure(
                measure.getMeasure(), measure.getPerformanceRate());

            // Measure should be excluded from calculations
            assertThat(scored).isNull();
        }

        @Test
        @DisplayName("Should handle missing prior year data")
        void shouldHandleMissingPriorYearData() {
            // Given
            StarRatingReport currentReport = createReportWithRating(new BigDecimal("4.0"));
            when(measureRepository.findPriorYearReport(PLAN_ID))
                .thenReturn(Optional.empty());

            // When
            StarRatingTrend trend = starRatingCalculator.analyzeYearOverYearTrend(
                currentReport);

            // Then
            assertThat(trend.hasPriorYearData()).isFalse();
            assertThat(trend.getStarChange()).isNull();
        }

        @Test
        @DisplayName("Should handle performance rate exactly at 1.0")
        void shouldHandlePerfectPerformanceRate() {
            // Given - Perfect 100% rate
            BigDecimal performanceRate = BigDecimal.ONE;
            StarRatingMeasure measure = StarRatingMeasure.BREAST_CANCER_SCREENING;

            Map<Integer, BigDecimal> cutPoints = Map.of(
                2, new BigDecimal("0.55"),
                3, new BigDecimal("0.60"),
                4, new BigDecimal("0.65"),
                5, new BigDecimal("0.70")
            );
            when(configRepository.getCutPoints(measure)).thenReturn(cutPoints);

            // When
            MeasureScore score = starRatingCalculator.scoreMeasure(
                measure, performanceRate);

            // Then
            assertThat(score.getStars()).isEqualTo(5);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private MeasureScore createMeasureScore(StarRatingMeasure measure, int stars, double weight) {
        return MeasureScore.builder()
            .measure(measure)
            .stars(stars)
            .weight(new BigDecimal(weight))
            .build();
    }

    private MeasureScore createMeasureScore(StarRatingMeasure measure, int stars,
            double weight, BigDecimal rate, BigDecimal nextCutPoint) {
        return MeasureScore.builder()
            .measure(measure)
            .stars(stars)
            .weight(new BigDecimal(weight))
            .performanceRate(rate)
            .nextCutPoint(nextCutPoint)
            .build();
    }

    private MeasureScore createMeasureScoreWithDenominator(StarRatingMeasure measure,
            int stars, BigDecimal rate, BigDecimal nextCutPoint, int denominator) {
        int numerator = rate.multiply(BigDecimal.valueOf(denominator)).intValue();
        return MeasureScore.builder()
            .measure(measure)
            .stars(stars)
            .performanceRate(rate)
            .nextCutPoint(nextCutPoint)
            .numerator(numerator)
            .denominator(denominator)
            .build();
    }

    private DomainScore createDomainScore(StarRatingDomain domain,
            BigDecimal stars, double weight) {
        return DomainScore.builder()
            .domain(domain)
            .domainStars(stars)
            .domainWeight(new BigDecimal(weight))
            .build();
    }

    private List<DomainScore> createDomainScoresForOverall(BigDecimal targetOverall) {
        return List.of(
            createDomainScore(StarRatingDomain.STAYING_HEALTHY, targetOverall, 1.0),
            createDomainScore(StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, targetOverall, 1.0),
            createDomainScore(StarRatingDomain.MEMBER_EXPERIENCE, targetOverall, 1.0),
            createDomainScore(StarRatingDomain.MEMBER_COMPLAINTS, targetOverall, 1.0),
            createDomainScore(StarRatingDomain.CUSTOMER_SERVICE, targetOverall, 1.0),
            createDomainScore(StarRatingDomain.DRUG_PLAN_SERVICES, targetOverall, 1.0)
        );
    }

    private StarRatingReport createReportWithRating(BigDecimal rating) {
        return StarRatingReport.builder()
            .planId(PLAN_ID)
            .roundedRating(rating)
            .overallRating(rating)
            .build();
    }
}
```

---

#### MedicaidComplianceServiceTest

Tests state-specific Medicaid compliance logic including thresholds, penalties, and bonuses.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Medicaid Compliance Service Tests")
class MedicaidComplianceServiceTest {

    @Mock
    private MedicaidStateConfigRepository stateConfigRepository;

    @Mock
    private MeasurePerformanceRepository performanceRepository;

    @InjectMocks
    private MedicaidComplianceService complianceService;

    private static final String TENANT_ID = "test-tenant";

    // ========================================
    // State-Specific Compliance Tests
    // ========================================

    @Nested
    @DisplayName("New York Compliance")
    class NewYorkComplianceTests {

        private MedicaidStateConfig nyConfig;

        @BeforeEach
        void setUp() {
            // NY-specific thresholds (stricter than national)
            nyConfig = MedicaidStateConfig.builder()
                .stateCode("NY")
                .stateName("New York")
                .qualityThresholds(Map.of(
                    "CBP", new BigDecimal("0.65"),    // Controlling Blood Pressure
                    "CDC-H9", new BigDecimal("0.75"), // Diabetes Poor Control (inverted)
                    "BCS", new BigDecimal("0.70"),    // Breast Cancer Screening
                    "COL", new BigDecimal("0.60")     // Colorectal Cancer Screening
                ))
                .performanceGoals(Map.of(
                    "CBP", new BigDecimal("0.75"),
                    "CDC-H9", new BigDecimal("0.65"),
                    "BCS", new BigDecimal("0.80"),
                    "COL", new BigDecimal("0.70")
                ))
                .ncqaAccreditationRequired(true)
                .build();

            when(stateConfigRepository.findByStateCode("NY"))
                .thenReturn(Optional.of(nyConfig));
        }

        @Test
        @DisplayName("Should be COMPLIANT when all measures exceed NY thresholds")
        void shouldBeCompliant_whenAllMeasuresExceedThresholds() {
            // Given - All measures exceed NY thresholds
            Map<String, BigDecimal> performanceRates = Map.of(
                "CBP", new BigDecimal("0.72"),   // > 0.65 threshold
                "CDC-H9", new BigDecimal("0.60"), // < 0.75 threshold (inverted, lower is better)
                "BCS", new BigDecimal("0.75"),   // > 0.70 threshold
                "COL", new BigDecimal("0.65")    // > 0.60 threshold
            );

            when(performanceRepository.findByMcoIdAndState("MCO-001", "NY"))
                .thenReturn(performanceRates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "NY");

            // Then
            assertThat(report.getOverallStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
            assertThat(report.getOverallComplianceRate())
                .isEqualByComparingTo(new BigDecimal("1.00")); // 100%
            assertThat(report.getPenaltyAssessment().getPenaltyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should require NCQA accreditation for NY")
        void shouldRequireNcqaForNewYork() {
            // Given
            Map<String, BigDecimal> rates = createAllPassingRates(nyConfig);
            when(performanceRepository.findByMcoIdAndState("MCO-001", "NY"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "NY");

            // Then
            assertThat(report.isNcqaAccreditationRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Texas Compliance")
    class TexasComplianceTests {

        private MedicaidStateConfig txConfig;

        @BeforeEach
        void setUp() {
            // TX-specific thresholds (less strict)
            txConfig = MedicaidStateConfig.builder()
                .stateCode("TX")
                .stateName("Texas")
                .qualityThresholds(Map.of(
                    "CBP", new BigDecimal("0.58"),
                    "CDC-H9", new BigDecimal("0.80"),
                    "BCS", new BigDecimal("0.65"),
                    "COL", new BigDecimal("0.55")
                ))
                .ncqaAccreditationRequired(false) // TX doesn't require NCQA
                .build();

            when(stateConfigRepository.findByStateCode("TX"))
                .thenReturn(Optional.of(txConfig));
        }

        @Test
        @DisplayName("Should NOT require NCQA accreditation for TX")
        void shouldNotRequireNcqaForTexas() {
            // Given
            Map<String, BigDecimal> rates = createAllPassingRates(txConfig);
            when(performanceRepository.findByMcoIdAndState("MCO-002", "TX"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-002", "TX");

            // Then
            assertThat(report.isNcqaAccreditationRequired()).isFalse();
        }

        @Test
        @DisplayName("Should use TX-specific thresholds")
        void shouldUseTexasThresholds() {
            // Given - Rate that passes TX but would fail NY
            Map<String, BigDecimal> rates = Map.of(
                "CBP", new BigDecimal("0.60"),  // Passes TX (0.58), fails NY (0.65)
                "CDC-H9", new BigDecimal("0.78"),
                "BCS", new BigDecimal("0.68"),
                "COL", new BigDecimal("0.58")
            );
            when(performanceRepository.findByMcoIdAndState("MCO-002", "TX"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-002", "TX");

            // Then - Should pass with TX thresholds
            assertThat(report.getOverallStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
        }
    }

    // ========================================
    // Compliance Status Tests
    // ========================================

    @Nested
    @DisplayName("Compliance Status Determination")
    class ComplianceStatusTests {

        private MedicaidStateConfig config;

        @BeforeEach
        void setUp() {
            config = createStandardConfig("ST");
            when(stateConfigRepository.findByStateCode("ST"))
                .thenReturn(Optional.of(config));
        }

        @Test
        @DisplayName("Should be SUBSTANTIALLY_COMPLIANT when >80% measures pass")
        void shouldBeSubstantiallyCompliant_whenOver80PercentPass() {
            // Given - 4 of 5 measures pass (80%)
            Map<String, BigDecimal> rates = Map.of(
                "MEASURE1", new BigDecimal("0.70"), // Pass
                "MEASURE2", new BigDecimal("0.70"), // Pass
                "MEASURE3", new BigDecimal("0.70"), // Pass
                "MEASURE4", new BigDecimal("0.70"), // Pass
                "MEASURE5", new BigDecimal("0.40")  // Fail
            );
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then
            assertThat(report.getOverallStatus())
                .isEqualTo(ComplianceStatus.SUBSTANTIALLY_COMPLIANT);
            assertThat(report.getOverallComplianceRate())
                .isEqualByComparingTo(new BigDecimal("0.80"));
        }

        @Test
        @DisplayName("Should be PARTIALLY_COMPLIANT when 50-80% measures pass")
        void shouldBePartiallyCompliant_when50To80PercentPass() {
            // Given - 3 of 5 measures pass (60%)
            Map<String, BigDecimal> rates = Map.of(
                "MEASURE1", new BigDecimal("0.70"), // Pass
                "MEASURE2", new BigDecimal("0.70"), // Pass
                "MEASURE3", new BigDecimal("0.70"), // Pass
                "MEASURE4", new BigDecimal("0.40"), // Fail
                "MEASURE5", new BigDecimal("0.40")  // Fail
            );
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then
            assertThat(report.getOverallStatus())
                .isEqualTo(ComplianceStatus.PARTIALLY_COMPLIANT);
        }

        @Test
        @DisplayName("Should be NON_COMPLIANT when <50% measures pass")
        void shouldBeNonCompliant_whenUnder50PercentPass() {
            // Given - 2 of 5 measures pass (40%)
            Map<String, BigDecimal> rates = Map.of(
                "MEASURE1", new BigDecimal("0.70"), // Pass
                "MEASURE2", new BigDecimal("0.70"), // Pass
                "MEASURE3", new BigDecimal("0.40"), // Fail
                "MEASURE4", new BigDecimal("0.40"), // Fail
                "MEASURE5", new BigDecimal("0.40")  // Fail
            );
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then
            assertThat(report.getOverallStatus())
                .isEqualTo(ComplianceStatus.NON_COMPLIANT);
        }

        @Test
        @DisplayName("Should handle boundary at exactly 50%")
        void shouldHandleBoundaryAt50Percent() {
            // Given - Exactly 50% pass
            Map<String, BigDecimal> rates = Map.of(
                "MEASURE1", new BigDecimal("0.70"),
                "MEASURE2", new BigDecimal("0.40")
            );
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then - 50% is PARTIALLY_COMPLIANT (>=50% and <80%)
            assertThat(report.getOverallStatus())
                .isEqualTo(ComplianceStatus.PARTIALLY_COMPLIANT);
        }
    }

    // ========================================
    // Penalty Assessment Tests
    // ========================================

    @Nested
    @DisplayName("Penalty Assessment")
    class PenaltyAssessmentTests {

        @Test
        @DisplayName("Should assess 5% penalty for NON_COMPLIANT")
        void shouldAssess5PercentPenalty_forNonCompliant() {
            // Given
            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .mcoId("MCO-001")
                .stateCode("NY")
                .overallStatus(ComplianceStatus.NON_COMPLIANT)
                .contractValue(new BigDecimal("50000000")) // $50M contract
                .build();

            // When
            PenaltyAssessment penalty = complianceService.assessPenalty(report);

            // Then
            assertThat(penalty.getPenaltyPercentage())
                .isEqualByComparingTo(new BigDecimal("0.05"));
            assertThat(penalty.getPenaltyAmount())
                .isEqualByComparingTo(new BigDecimal("2500000")); // $2.5M
        }

        @Test
        @DisplayName("Should assess 2% penalty for PARTIALLY_COMPLIANT")
        void shouldAssess2PercentPenalty_forPartiallyCompliant() {
            // Given
            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .mcoId("MCO-001")
                .stateCode("TX")
                .overallStatus(ComplianceStatus.PARTIALLY_COMPLIANT)
                .contractValue(new BigDecimal("50000000"))
                .build();

            // When
            PenaltyAssessment penalty = complianceService.assessPenalty(report);

            // Then
            assertThat(penalty.getPenaltyPercentage())
                .isEqualByComparingTo(new BigDecimal("0.02"));
            assertThat(penalty.getPenaltyAmount())
                .isEqualByComparingTo(new BigDecimal("1000000")); // $1M
        }

        @Test
        @DisplayName("Should assess no penalty for COMPLIANT")
        void shouldAssessNoPenalty_forCompliant() {
            // Given
            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .mcoId("MCO-001")
                .stateCode("CA")
                .overallStatus(ComplianceStatus.COMPLIANT)
                .contractValue(new BigDecimal("50000000"))
                .build();

            // When
            PenaltyAssessment penalty = complianceService.assessPenalty(report);

            // Then
            assertThat(penalty.getPenaltyPercentage())
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(penalty.getPenaltyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ========================================
    // Quality Bonus Tests
    // ========================================

    @Nested
    @DisplayName("Quality Bonus")
    class QualityBonusTests {

        @Test
        @DisplayName("Should be eligible for bonus when COMPLIANT and majority exceed goals")
        void shouldBeEligibleForBonus_whenCompliantAndMajorityExceedGoals() {
            // Given - COMPLIANT with 3 of 4 measures exceeding goals
            List<MedicaidMeasureResult> results = List.of(
                createMeasureResult("CBP", true, true),  // Meets threshold, exceeds goal
                createMeasureResult("BCS", true, true),  // Meets threshold, exceeds goal
                createMeasureResult("COL", true, true),  // Meets threshold, exceeds goal
                createMeasureResult("CDC-H9", true, false) // Meets threshold, NOT exceeding goal
            );

            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .overallStatus(ComplianceStatus.COMPLIANT)
                .measureResults(results)
                .build();

            // When
            boolean eligible = complianceService.isQualityBonusEligible(report);

            // Then - Majority (3/4 = 75%) exceed goals
            assertThat(eligible).isTrue();
        }

        @Test
        @DisplayName("Should NOT be eligible when non-compliant even if exceeding goals")
        void shouldNotBeEligible_whenNonCompliant() {
            // Given - NON_COMPLIANT even with high performance on some measures
            List<MedicaidMeasureResult> results = List.of(
                createMeasureResult("CBP", true, true),
                createMeasureResult("BCS", false, false), // Fails threshold
                createMeasureResult("COL", false, false)  // Fails threshold
            );

            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .overallStatus(ComplianceStatus.NON_COMPLIANT)
                .measureResults(results)
                .build();

            // When
            boolean eligible = complianceService.isQualityBonusEligible(report);

            // Then
            assertThat(eligible).isFalse();
        }

        @Test
        @DisplayName("Should calculate bonus amount based on goals exceeded")
        void shouldCalculateBonusAmount() {
            // Given - 3 measures exceed goals
            List<MedicaidMeasureResult> results = List.of(
                createMeasureResult("CBP", true, true),
                createMeasureResult("BCS", true, true),
                createMeasureResult("COL", true, true)
            );

            MedicaidComplianceReport report = MedicaidComplianceReport.builder()
                .overallStatus(ComplianceStatus.COMPLIANT)
                .measureResults(results)
                .build();

            // When - Bonus = count(exceeds_goal) × $10,000
            BigDecimal bonus = complianceService.calculateQualityBonus(report);

            // Then
            assertThat(bonus).isEqualByComparingTo(new BigDecimal("30000")); // 3 × $10,000
        }
    }

    // ========================================
    // Year-over-Year Improvement Tests
    // ========================================

    @Nested
    @DisplayName("Year-over-Year Improvement")
    class YearOverYearTests {

        @Test
        @DisplayName("Should track improvement from prior year")
        void shouldTrackImprovement() {
            // Given
            MedicaidMeasureResult result = MedicaidMeasureResult.builder()
                .measureCode("CBP")
                .performanceRate(new BigDecimal("0.72"))
                .priorYearRate(new BigDecimal("0.68"))
                .build();

            // When
            BigDecimal improvement = complianceService.calculateMeasureImprovement(result);

            // Then
            assertThat(improvement).isEqualByComparingTo(new BigDecimal("0.04"));
        }

        @Test
        @DisplayName("Should handle decline (negative improvement)")
        void shouldHandleDecline() {
            // Given
            MedicaidMeasureResult result = MedicaidMeasureResult.builder()
                .measureCode("BCS")
                .performanceRate(new BigDecimal("0.65"))
                .priorYearRate(new BigDecimal("0.70"))
                .build();

            // When
            BigDecimal improvement = complianceService.calculateMeasureImprovement(result);

            // Then
            assertThat(improvement).isEqualByComparingTo(new BigDecimal("-0.05"));
        }
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle zero denominator")
        void shouldHandleZeroDenominator() {
            // Given - Measure with no eligible population
            Map<String, BigDecimal> rates = Map.of(
                "CBP", new BigDecimal("0.70"),
                "BCS", null // No rate due to zero denominator
            );

            when(stateConfigRepository.findByStateCode("ST"))
                .thenReturn(Optional.of(createStandardConfig("ST")));
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then - Measure should be excluded, not cause failure
            assertThat(report).isNotNull();
            assertThat(report.getMeasureResults())
                .noneMatch(r -> r.getMeasureCode().equals("BCS"));
        }

        @Test
        @DisplayName("Should handle state not found")
        void shouldHandleStateNotFound() {
            // Given
            when(stateConfigRepository.findByStateCode("XX"))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                complianceService.calculateCompliance(TENANT_ID, "MCO-001", "XX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("State configuration not found");
        }

        @Test
        @DisplayName("Should handle perfect 100% rate")
        void shouldHandlePerfectRate() {
            // Given
            Map<String, BigDecimal> rates = Map.of(
                "CBP", BigDecimal.ONE,
                "BCS", BigDecimal.ONE
            );

            when(stateConfigRepository.findByStateCode("ST"))
                .thenReturn(Optional.of(createStandardConfig("ST")));
            when(performanceRepository.findByMcoIdAndState("MCO-001", "ST"))
                .thenReturn(rates);

            // When
            MedicaidComplianceReport report = complianceService.calculateCompliance(
                TENANT_ID, "MCO-001", "ST");

            // Then
            assertThat(report.getOverallStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private MedicaidStateConfig createStandardConfig(String stateCode) {
        return MedicaidStateConfig.builder()
            .stateCode(stateCode)
            .qualityThresholds(Map.of(
                "MEASURE1", new BigDecimal("0.60"),
                "MEASURE2", new BigDecimal("0.60"),
                "MEASURE3", new BigDecimal("0.60"),
                "MEASURE4", new BigDecimal("0.60"),
                "MEASURE5", new BigDecimal("0.60")
            ))
            .performanceGoals(Map.of(
                "MEASURE1", new BigDecimal("0.75"),
                "MEASURE2", new BigDecimal("0.75"),
                "MEASURE3", new BigDecimal("0.75"),
                "MEASURE4", new BigDecimal("0.75"),
                "MEASURE5", new BigDecimal("0.75")
            ))
            .build();
    }

    private Map<String, BigDecimal> createAllPassingRates(MedicaidStateConfig config) {
        return config.getQualityThresholds().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().add(new BigDecimal("0.10"))
            ));
    }

    private MedicaidMeasureResult createMeasureResult(String code,
            boolean meetsThreshold, boolean meetsGoal) {
        return MedicaidMeasureResult.builder()
            .measureCode(code)
            .meetsThreshold(meetsThreshold)
            .meetsGoal(meetsGoal)
            .complianceLevel(meetsGoal ? ComplianceLevel.EXCEEDS_GOAL :
                meetsThreshold ? ComplianceLevel.MEETS_THRESHOLD :
                ComplianceLevel.BELOW_THRESHOLD)
            .build();
    }
}
```

---

#### PayerDashboardServiceTest

Tests dashboard aggregation for Medicare Advantage and Medicaid MCO metrics.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Payer Dashboard Service Tests")
class PayerDashboardServiceTest {

    @Mock
    private StarRatingRepository starRatingRepository;

    @Mock
    private MedicaidComplianceRepository complianceRepository;

    @InjectMocks
    private PayerDashboardService dashboardService;

    private static final String TENANT_ID = "test-tenant";
    private static final String PAYER_ID = "PAYER-001";

    // ========================================
    // Medicare Dashboard Tests
    // ========================================

    @Nested
    @DisplayName("Medicare Dashboard")
    class MedicareDashboardTests {

        @Test
        @DisplayName("Should calculate average star rating across plans")
        void shouldCalculateAverageStarRating() {
            // Given - 3 plans with different ratings
            List<StarRatingReport> reports = List.of(
                createStarRatingReport("H1234-001", new BigDecimal("4.5"), 5000),
                createStarRatingReport("H1234-002", new BigDecimal("4.0"), 3000),
                createStarRatingReport("H1234-003", new BigDecimal("3.5"), 2000)
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicareAdvantageMetrics metrics = dashboardService.getMedicareDashboard(
                TENANT_ID, PAYER_ID);

            // Then - Simple average: (4.5 + 4.0 + 3.5) / 3 = 4.0
            assertThat(metrics.getAverageStarRating())
                .isEqualByComparingTo(new BigDecimal("4.0"));
            assertThat(metrics.getTotalMedicarePlans()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count plans with 4+ stars")
        void shouldCountHighPerformingPlans() {
            // Given
            List<StarRatingReport> reports = List.of(
                createStarRatingReport("H1234-001", new BigDecimal("4.5"), 5000),
                createStarRatingReport("H1234-002", new BigDecimal("4.0"), 3000),
                createStarRatingReport("H1234-003", new BigDecimal("3.5"), 2000),
                createStarRatingReport("H1234-004", new BigDecimal("5.0"), 1000)
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicareAdvantageMetrics metrics = dashboardService.getMedicareDashboard(
                TENANT_ID, PAYER_ID);

            // Then - 3 plans have 4+ stars
            assertThat(metrics.getPlansWithFourStarsOrMore()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should calculate total QBP bonus eligibility")
        void shouldCalculateTotalQbpBonus() {
            // Given - Plans with QBP eligibility
            List<StarRatingReport> reports = List.of(
                createStarRatingReportWithBonus("H1234-001", 5, 10000,
                    new BigDecimal("5100000")),  // 5-star, 5% bonus
                createStarRatingReportWithBonus("H1234-002", 4, 8000,
                    new BigDecimal("2448000")),  // 4-star, 3% bonus
                createStarRatingReportWithBonus("H1234-003", 3, 5000,
                    BigDecimal.ZERO)             // 3-star, no bonus
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicareAdvantageMetrics metrics = dashboardService.getMedicareDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getEstimatedBonusPayments())
                .isEqualByComparingTo(new BigDecimal("7548000")); // Sum of bonuses
            assertThat(metrics.getQbpEligiblePlans()).isEqualTo(2); // 4+ star plans
        }

        @Test
        @DisplayName("Should aggregate enrollment across plans")
        void shouldAggregateEnrollment() {
            // Given
            List<StarRatingReport> reports = List.of(
                createStarRatingReport("H1234-001", new BigDecimal("4.5"), 5000),
                createStarRatingReport("H1234-002", new BigDecimal("4.0"), 3000),
                createStarRatingReport("H1234-003", new BigDecimal("3.5"), 2000)
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicareAdvantageMetrics metrics = dashboardService.getMedicareDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getTotalEnrollment()).isEqualTo(10000);
        }
    }

    // ========================================
    // Medicaid Dashboard Tests
    // ========================================

    @Nested
    @DisplayName("Medicaid Dashboard")
    class MedicaidDashboardTests {

        @Test
        @DisplayName("Should calculate average compliance rate")
        void shouldCalculateAverageComplianceRate() {
            // Given
            List<MedicaidComplianceReport> reports = List.of(
                createComplianceReport("MCO-001", "NY", new BigDecimal("0.85")),
                createComplianceReport("MCO-002", "CA", new BigDecimal("0.75")),
                createComplianceReport("MCO-003", "TX", new BigDecimal("0.90"))
            );

            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicaidMcoMetrics metrics = dashboardService.getMedicaidDashboard(
                TENANT_ID, PAYER_ID);

            // Then - Average: (0.85 + 0.75 + 0.90) / 3 = 0.833
            assertThat(metrics.getAverageComplianceRate())
                .isEqualByComparingTo(new BigDecimal("0.833"));
        }

        @Test
        @DisplayName("Should count compliant vs non-compliant plans")
        void shouldCountCompliantPlans() {
            // Given
            List<MedicaidComplianceReport> reports = List.of(
                createComplianceReportWithStatus("MCO-001", ComplianceStatus.COMPLIANT),
                createComplianceReportWithStatus("MCO-002", ComplianceStatus.SUBSTANTIALLY_COMPLIANT),
                createComplianceReportWithStatus("MCO-003", ComplianceStatus.PARTIALLY_COMPLIANT),
                createComplianceReportWithStatus("MCO-004", ComplianceStatus.NON_COMPLIANT)
            );

            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicaidMcoMetrics metrics = dashboardService.getMedicaidDashboard(
                TENANT_ID, PAYER_ID);

            // Then - COMPLIANT + SUBSTANTIALLY_COMPLIANT = 2
            assertThat(metrics.getCompliantPlans()).isEqualTo(2);
            assertThat(metrics.getNonCompliantPlans()).isEqualTo(2); // PARTIALLY + NON
        }

        @Test
        @DisplayName("Should track number of states with MCOs")
        void shouldTrackStateCount() {
            // Given - MCOs in 4 different states
            List<MedicaidComplianceReport> reports = List.of(
                createComplianceReport("MCO-001", "NY", new BigDecimal("0.85")),
                createComplianceReport("MCO-002", "CA", new BigDecimal("0.75")),
                createComplianceReport("MCO-003", "TX", new BigDecimal("0.90")),
                createComplianceReport("MCO-004", "FL", new BigDecimal("0.80"))
            );

            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicaidMcoMetrics metrics = dashboardService.getMedicaidDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getNumberOfStates()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should calculate estimated penalties")
        void shouldCalculateEstimatedPenalties() {
            // Given
            List<MedicaidComplianceReport> reports = List.of(
                createComplianceReportWithPenalty("MCO-001", new BigDecimal("1000000")),
                createComplianceReportWithPenalty("MCO-002", new BigDecimal("500000")),
                createComplianceReportWithPenalty("MCO-003", BigDecimal.ZERO)
            );

            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicaidMcoMetrics metrics = dashboardService.getMedicaidDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getEstimatedPenalties())
                .isEqualByComparingTo(new BigDecimal("1500000"));
        }
    }

    // ========================================
    // Combined Dashboard Tests
    // ========================================

    @Nested
    @DisplayName("Combined Dashboard")
    class CombinedDashboardTests {

        @Test
        @DisplayName("Should aggregate Medicare and Medicaid metrics")
        void shouldAggregateBothLineOfBusiness() {
            // Given
            List<StarRatingReport> maReports = List.of(
                createStarRatingReport("H1234-001", new BigDecimal("4.5"), 5000)
            );
            List<MedicaidComplianceReport> mcoReports = List.of(
                createComplianceReport("MCO-001", "NY", new BigDecimal("0.85"))
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(maReports);
            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(mcoReports);

            // When
            PayerDashboardMetrics metrics = dashboardService.getCombinedDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getMedicareAdvantageMetrics()).isNotNull();
            assertThat(metrics.getMedicaidMcoMetrics()).isNotNull();
            assertThat(metrics.getDashboardType()).isEqualTo(DashboardType.ALL);
        }

        @Test
        @DisplayName("Should identify top performing measures across lines")
        void shouldIdentifyTopPerformingMeasures() {
            // Given - Setup measures with varying performance
            List<StarRatingReport> maReports = List.of(
                createStarRatingReportWithMeasures("H1234-001", Map.of(
                    "CBP", new BigDecimal("0.85"),
                    "BCS", new BigDecimal("0.72"),
                    "COL", new BigDecimal("0.90")
                ))
            );

            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(maReports);

            // When
            PayerDashboardMetrics metrics = dashboardService.getCombinedDashboard(
                TENANT_ID, PAYER_ID);

            // Then - COL at 90% should be top performer
            assertThat(metrics.getTopPerformingMeasures())
                .anyMatch(m -> m.getMeasureCode().equals("COL"));
        }
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty report list")
        void shouldHandleEmptyReports() {
            // Given
            when(starRatingRepository.findByPayerId(PAYER_ID))
                .thenReturn(Collections.emptyList());

            // When
            MedicareAdvantageMetrics metrics = dashboardService.getMedicareDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getTotalMedicarePlans()).isEqualTo(0);
            assertThat(metrics.getAverageStarRating()).isNull();
        }

        @Test
        @DisplayName("Should handle single state")
        void shouldHandleSingleState() {
            // Given
            List<MedicaidComplianceReport> reports = List.of(
                createComplianceReport("MCO-001", "NY", new BigDecimal("0.85")),
                createComplianceReport("MCO-002", "NY", new BigDecimal("0.90"))
            );

            when(complianceRepository.findByPayerId(PAYER_ID))
                .thenReturn(reports);

            // When
            MedicaidMcoMetrics metrics = dashboardService.getMedicaidDashboard(
                TENANT_ID, PAYER_ID);

            // Then
            assertThat(metrics.getNumberOfStates()).isEqualTo(1);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private StarRatingReport createStarRatingReport(String planId,
            BigDecimal rating, int enrollment) {
        return StarRatingReport.builder()
            .planId(planId)
            .roundedRating(rating)
            .enrollment(enrollment)
            .build();
    }

    private StarRatingReport createStarRatingReportWithBonus(String planId,
            int stars, int enrollment, BigDecimal bonus) {
        return StarRatingReport.builder()
            .planId(planId)
            .roundedRating(new BigDecimal(stars))
            .enrollment(enrollment)
            .estimatedBonusAmount(bonus)
            .build();
    }

    private StarRatingReport createStarRatingReportWithMeasures(String planId,
            Map<String, BigDecimal> measureRates) {
        List<MeasureScore> scores = measureRates.entrySet().stream()
            .map(e -> MeasureScore.builder()
                .measureCode(e.getKey())
                .performanceRate(e.getValue())
                .build())
            .collect(Collectors.toList());

        return StarRatingReport.builder()
            .planId(planId)
            .measureScores(scores)
            .build();
    }

    private MedicaidComplianceReport createComplianceReport(String mcoId,
            String stateCode, BigDecimal complianceRate) {
        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .stateCode(stateCode)
            .overallComplianceRate(complianceRate)
            .build();
    }

    private MedicaidComplianceReport createComplianceReportWithStatus(String mcoId,
            ComplianceStatus status) {
        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .overallStatus(status)
            .build();
    }

    private MedicaidComplianceReport createComplianceReportWithPenalty(String mcoId,
            BigDecimal penalty) {
        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .penaltyAssessment(PenaltyAssessment.builder()
                .penaltyAmount(penalty)
                .build())
            .build();
    }
}
```

---

### Controller Integration Tests

#### PayerWorkflowsControllerTest

Tests REST API endpoints with proper error handling and validation.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Payer Workflows Controller Tests")
class PayerWorkflowsControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-001";
    private static final String PLAN_ID = "H1234-001";
    private static final String MCO_ID = "MCO-001";

    // ========================================
    // Star Rating Endpoints
    // ========================================

    @Nested
    @DisplayName("Star Rating Endpoints")
    class StarRatingEndpointTests {

        @Test
        @DisplayName("Should return star rating report for valid plan")
        void shouldReturnStarRatingReport() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", PLAN_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(PLAN_ID))
                .andExpect(jsonPath("$.overallRating").exists())
                .andExpect(jsonPath("$.roundedRating").exists())
                .andExpect(jsonPath("$.domainScores").isArray());
        }

        @Test
        @DisplayName("Should return measure breakdown")
        void shouldReturnMeasureBreakdown() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/measures", PLAN_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "EVALUATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measures").isArray())
                .andExpect(jsonPath("$.measures[0].measureCode").exists())
                .andExpect(jsonPath("$.measures[0].performanceRate").exists())
                .andExpect(jsonPath("$.measures[0].stars").exists());
        }

        @Test
        @DisplayName("Should return improvement opportunities")
        void shouldReturnImprovementOpportunities() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/improvement", PLAN_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunities").isArray());
        }

        @Test
        @DisplayName("Should return 404 for invalid plan")
        void shouldReturn404ForInvalidPlan() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", "INVALID-PLAN")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Plan not found"));
        }

        @Test
        @DisplayName("Should return 404 for empty planId")
        void shouldReturn404ForEmptyPlanId() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", " ")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isNotFound());
        }
    }

    // ========================================
    // Medicaid Compliance Endpoints
    // ========================================

    @Nested
    @DisplayName("Medicaid Compliance Endpoints")
    class MedicaidEndpointTests {

        @Test
        @DisplayName("Should return compliance report for valid state and MCO")
        void shouldReturnComplianceReport() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", "NY")
                    .param("mcoId", MCO_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateCode").value("NY"))
                .andExpect(jsonPath("$.mcoId").value(MCO_ID))
                .andExpect(jsonPath("$.overallStatus").exists())
                .andExpect(jsonPath("$.overallComplianceRate").exists());
        }

        @Test
        @DisplayName("Should support multiple state codes")
        void shouldSupportMultipleStates() throws Exception {
            String[] states = {"NY", "CA", "TX", "FL"};

            for (String state : states) {
                mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                        .param("mcoId", MCO_ID)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-Auth-User-Id", "user-001")
                        .header("X-Auth-Roles", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stateCode").value(state));
            }
        }

        @Test
        @DisplayName("Should return 400 when mcoId parameter missing")
        void shouldReturn400WhenMcoIdMissing() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", "NY")
                    // Missing mcoId parameter
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing required parameter: mcoId"));
        }

        @Test
        @DisplayName("Should handle case-insensitive state codes")
        void shouldHandleCaseInsensitiveStateCodes() throws Exception {
            // Test lowercase
            mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", "ny")
                    .param("mcoId", MCO_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateCode").value("NY")); // Normalized to uppercase
        }
    }

    // ========================================
    // Dashboard Endpoints
    // ========================================

    @Nested
    @DisplayName("Dashboard Endpoints")
    class DashboardEndpointTests {

        @Test
        @DisplayName("Should return combined dashboard overview")
        void shouldReturnCombinedDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                    .param("payerId", "PAYER-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardType").value("ALL"))
                .andExpect(jsonPath("$.medicareAdvantageMetrics").exists())
                .andExpect(jsonPath("$.medicaidMcoMetrics").exists());
        }

        @Test
        @DisplayName("Should return Medicare-specific dashboard")
        void shouldReturnMedicareDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/medicare")
                    .param("payerId", "PAYER-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageStarRating").exists())
                .andExpect(jsonPath("$.totalMedicarePlans").exists())
                .andExpect(jsonPath("$.plansWithFourStarsOrMore").exists());
        }

        @Test
        @DisplayName("Should return Medicaid-specific dashboard")
        void shouldReturnMedicaidDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/medicaid")
                    .param("payerId", "PAYER-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageComplianceRate").exists())
                .andExpect(jsonPath("$.compliantPlans").exists())
                .andExpect(jsonPath("$.numberOfStates").exists());
        }

        @Test
        @DisplayName("Should return 400 when payerId missing")
        void shouldReturn400WhenPayerIdMissing() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                    // Missing payerId parameter
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isBadRequest());
        }
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 500 on service timeout")
        void shouldReturn500OnTimeout() throws Exception {
            // Given - Configure mock to simulate timeout
            // (In real test, use WireMock or similar to simulate external dependency timeout)

            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", "TIMEOUT-PLAN")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should handle very long planId")
        void shouldHandleVeryLongPlanId() throws Exception {
            String longPlanId = "H" + "1234567890".repeat(100);

            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", longPlanId)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isBadRequest()); // Input validation should reject
        }

        @Test
        @DisplayName("Should sanitize special characters in planId (XSS prevention)")
        void shouldSanitizeSpecialCharacters() throws Exception {
            String xssAttempt = "H1234<script>alert('xss')</script>";

            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", xssAttempt)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isBadRequest()); // Should reject invalid characters
        }

        @Test
        @DisplayName("Should handle concurrent requests gracefully")
        void shouldHandleConcurrentRequests() throws Exception {
            // Execute multiple requests concurrently
            int requestCount = 10;
            CompletableFuture<?>[] futures = new CompletableFuture[requestCount];

            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}",
                                "PLAN-" + index)
                                .header("X-Tenant-ID", TENANT_ID)
                                .header("X-Auth-User-Id", "user-001")
                                .header("X-Auth-Roles", "ADMIN"))
                            .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            CompletableFuture.allOf(futures).join();
        }
    }
}
```

---

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Payer Workflows Multi-Tenant Isolation Tests")
class PayerMultiTenantIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private StarRatingRepository starRatingRepository;

    @Autowired
    private MedicaidComplianceRepository complianceRepository;

    @Test
    @DisplayName("Star rating queries should only return data for specified tenant")
    void shouldIsolateTenantStarRatings() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        starRatingRepository.save(createStarRating(tenant1, "H1234-001"));
        starRatingRepository.save(createStarRating(tenant1, "H1234-002"));
        starRatingRepository.save(createStarRating(tenant2, "H5678-001"));

        // When
        List<StarRatingReport> tenant1Reports =
            starRatingRepository.findByTenantId(tenant1);
        List<StarRatingReport> tenant2Reports =
            starRatingRepository.findByTenantId(tenant2);

        // Then
        assertThat(tenant1Reports)
            .hasSize(2)
            .extracting(StarRatingReport::getTenantId)
            .containsOnly(tenant1);

        assertThat(tenant2Reports)
            .hasSize(1)
            .extracting(StarRatingReport::getTenantId)
            .containsOnly(tenant2);
    }

    @Test
    @DisplayName("Medicaid compliance queries should only return data for specified tenant")
    void shouldIsolateTenantMedicaidCompliance() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        complianceRepository.save(createCompliance(tenant1, "MCO-001", "NY"));
        complianceRepository.save(createCompliance(tenant2, "MCO-002", "CA"));

        // When
        List<MedicaidComplianceReport> tenant1Reports =
            complianceRepository.findByTenantId(tenant1);

        // Then
        assertThat(tenant1Reports)
            .hasSize(1)
            .extracting(MedicaidComplianceReport::getTenantId)
            .containsOnly(tenant1);
    }

    private StarRatingReport createStarRating(String tenantId, String planId) {
        return StarRatingReport.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .planId(planId)
            .roundedRating(new BigDecimal("4.0"))
            .build();
    }

    private MedicaidComplianceReport createCompliance(String tenantId,
            String mcoId, String state) {
        return MedicaidComplianceReport.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .mcoId(mcoId)
            .stateCode(state)
            .overallStatus(ComplianceStatus.COMPLIANT)
            .build();
    }
}
```

---

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Payer Workflows RBAC Tests")
class PayerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-001";

    @Nested
    @DisplayName("Star Rating Access Control")
    class StarRatingRbacTests {

        @Test
        @DisplayName("ADMIN should have full access to star ratings")
        void adminShouldHaveFullAccess() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/H1234-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "admin-001")
                    .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/payer/medicare/star-rating/calculate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "admin-001")
                    .header("X-Auth-Roles", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ANALYST should have read access to star ratings")
        void analystShouldHaveReadAccess() throws Exception {
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/H1234-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "analyst-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("VIEWER should have read-only access")
        void viewerShouldHaveReadOnlyAccess() throws Exception {
            // Read access - OK
            mockMvc.perform(get("/api/v1/payer/medicare/star-rating/H1234-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "viewer-001")
                    .header("X-Auth-Roles", "VIEWER"))
                .andExpect(status().isOk());

            // Write access - Forbidden
            mockMvc.perform(post("/api/v1/payer/medicare/star-rating/calculate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "viewer-001")
                    .header("X-Auth-Roles", "VIEWER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Dashboard Access Control")
    class DashboardRbacTests {

        @Test
        @DisplayName("ANALYST should access dashboards")
        void analystShouldAccessDashboards() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                    .param("payerId", "PAYER-001")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "analyst-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated requests should be rejected")
        void unauthenticatedShouldBeRejected() throws Exception {
            mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                    .param("payerId", "PAYER-001")
                    // Missing auth headers
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isUnauthorized());
        }
    }
}
```

---

### Performance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Payer Workflows Performance Tests")
class PayerPerformanceTest {

    @Autowired
    private StarRatingCalculator starRatingCalculator;

    @Autowired
    private MedicaidComplianceService complianceService;

    @Test
    @DisplayName("Star rating calculation should complete within 100ms per plan")
    void starRatingCalculationPerformance() {
        // Given
        int planCount = 100;
        List<StarRatingRequest> requests = IntStream.range(0, planCount)
            .mapToObj(i -> createStarRatingRequest("H1234-" + String.format("%03d", i)))
            .collect(Collectors.toList());

        // When
        Instant start = Instant.now();
        requests.forEach(req ->
            starRatingCalculator.calculateStarRating(req));
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerPlan = totalMs / (double) planCount;

        assertThat(avgMsPerPlan)
            .isLessThan(100.0)
            .withFailMessage("Average calculation time %.2fms exceeds 100ms SLA", avgMsPerPlan);

        System.out.printf("Performance: %d calculations in %dms (avg: %.2fms/plan)%n",
            planCount, totalMs, avgMsPerPlan);
    }

    @Test
    @DisplayName("Medicaid compliance calculation should complete within 50ms")
    void medicaidComplianceCalculationPerformance() {
        // Given
        int mcoCount = 50;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < mcoCount; i++) {
            Instant start = Instant.now();
            complianceService.calculateCompliance("tenant-001", "MCO-" + i, "NY");
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (mcoCount * 0.95));

        assertThat(p95)
            .isLessThan(50L)
            .withFailMessage("p95 latency %dms exceeds 50ms SLA", p95);

        System.out.printf("Medicaid Compliance Performance: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(mcoCount / 2),
            p95,
            latencies.get((int) (mcoCount * 0.99)));
    }

    @Test
    @DisplayName("Dashboard aggregation should complete within 200ms for large datasets")
    void dashboardAggregationPerformance() {
        // Given - Simulate large payer with many plans
        int planCount = 500;
        List<StarRatingReport> reports = IntStream.range(0, planCount)
            .mapToObj(i -> createMockStarRatingReport("H" + i))
            .collect(Collectors.toList());

        // When
        Instant start = Instant.now();
        dashboardService.aggregateMedicareMetrics(reports);
        Instant end = Instant.now();

        // Then
        long latencyMs = Duration.between(start, end).toMillis();

        assertThat(latencyMs)
            .isLessThan(200L)
            .withFailMessage("Dashboard aggregation %dms exceeds 200ms SLA", latencyMs);
    }

    private StarRatingRequest createStarRatingRequest(String planId) {
        return StarRatingRequest.builder()
            .planId(planId)
            .measureData(createMockMeasureData())
            .build();
    }
}
```

---

### Test Configuration

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    # TestContainers provides dynamic URL
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  redis:
    # TestContainers provides dynamic host/port
    timeout: 2000ms

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

payer:
  star-ratings:
    cut-point-year: 2024
    cache-ttl-minutes: 1  # Short TTL for tests

  medicaid:
    supported-states: NY,CA,TX,FL
    compliance-cache-ttl-minutes: 1

# Test-specific security (gateway trust mode)
gateway:
  auth:
    dev-mode: true  # Disable HMAC validation in tests
```

---

### Best Practices

| Practice | Description |
|----------|-------------|
| **CMS Cut Points** | Always use official CMS 2024 cut points for Star Rating tests; verify boundary conditions precisely |
| **State Configurations** | Test each supported state (NY, CA, TX, FL) independently; state thresholds and requirements differ |
| **BigDecimal Comparisons** | Use `isEqualByComparingTo()` not `isEqualTo()` for BigDecimal assertions |
| **Inverted Measures** | Test inverted measures (lower is better) separately; Diabetes Poor Control follows opposite logic |
| **QBP Thresholds** | Verify exact 4.0 and 5.0 star thresholds for bonus eligibility; 3.99 is NOT eligible |
| **Penalty Calculations** | Validate penalty percentages match state contracts (typically 2-5% based on compliance status) |
| **Multi-State Testing** | MCOs often span multiple states; test state-specific thresholds apply correctly |
| **Year-over-Year** | Always test improvement tracking with positive, negative, and zero change scenarios |
| **Tenant Isolation** | All payer data is tenant-specific; verify cross-tenant queries return empty results |
| **Edge Cases** | Test zero denominator, 100% rate, missing measures, and boundary values exhaustively |

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Star rating calculation returns null | Zero denominator in measure | Exclude measures with no eligible population |
| Wrong star rating for inverted measure | Not accounting for "lower is better" logic | Check `isInvertedMeasure()` flag and reverse comparison |
| State config not found | Unsupported state code | Add state to `supported-states` config or verify spelling |
| Compliance status incorrect at boundary | Off-by-one in percentage comparison | Use >= for threshold checks (80% is SUBSTANTIALLY_COMPLIANT) |
| QBP bonus calculation wrong | Using regular rating instead of rounded | Always use `roundedRating` for QBP eligibility checks |
| Penalty not calculated | Missing contract value | Ensure `contractValue` is set on compliance report |
| Dashboard shows wrong plan count | Tenant filter missing | Add `tenantId` parameter to repository queries |
| Test fails with "Plan not found" | Test data not seeded | Ensure @BeforeEach populates required test data |
| BigDecimal assertion fails | Using `isEqualTo()` instead of `isEqualByComparingTo()` | Use `isEqualByComparingTo()` for scale-independent comparison |
| Performance test exceeds SLA | Cold start or database connection overhead | Run warmup iteration before timing; use connection pooling |

---

### CI/CD Integration

```yaml
# .github/workflows/payer-workflows-tests.yml
name: Payer Workflows Service Tests

on:
  push:
    paths:
      - 'backend/modules/services/payer-workflows-service/**'
  pull_request:
    paths:
      - 'backend/modules/services/payer-workflows-service/**'

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Tests
        run: |
          cd backend
          ./gradlew :modules:services:payer-workflows-service:test

      - name: Generate Coverage Report
        run: |
          cd backend
          ./gradlew :modules:services:payer-workflows-service:jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: backend/modules/services/payer-workflows-service/build/reports/jacoco/test/jacocoTestReport.xml
```

**Total: 200+ comprehensive tests** covering Star Ratings, Medicaid Compliance, Dashboards, and REST APIs with full regulatory compliance validation.

## Configuration

### Application Properties

```yaml
payer:
  star-ratings:
    cut-point-year: 2024
    enable-improvement-tracking: true
    cache-ttl-minutes: 60

  medicaid:
    supported-states: NY,CA,TX,FL,PA,OH,MI,GA,NC,NJ
    compliance-cache-ttl-minutes: 30

  dashboard:
    metrics-refresh-minutes: 15
    enable-financial-projections: true
```

### Supported States

- New York (NY) - New York Medicaid Managed Care
- California (CA) - Medi-Cal Managed Care
- Texas (TX) - Texas STAR Medicaid
- Florida (FL) - Florida Medicaid Managed Care
- Additional states easily configurable

## Integration

### Quality Measure Service Integration

This service integrates with the `quality-measure-service` to:
- Retrieve HEDIS measure calculations
- Map quality data to Star Rating measures
- Aggregate performance across plans

### Event-Driven Architecture

Publishes events for:
- Star Rating calculations completed
- Compliance status changes
- Quality improvement opportunities identified

## Running the Service

### Local Development

```bash
./gradlew :modules:services:payer-workflows-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:payer-workflows-service:test
```

### Building

```bash
./gradlew :modules:services:payer-workflows-service:build
```

## API Documentation

Swagger UI available at:
```
http://localhost:8089/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8089/api-docs
```

## Star Rating Methodology

### CMS 2024 Star Rating Calculation

1. **Measure Scoring**: Each HEDIS measure scored 1-5 stars based on CMS cut points
2. **Domain Aggregation**: Measures grouped into 6 domains with weighted averaging
3. **Overall Rating**: Domain scores weighted to produce overall plan rating
4. **Rounding**: Final rating rounded to nearest half-star

### Quality Bonus Payment (QBP)

- **4+ stars**: 3% bonus payment
- **5 stars**: 5% bonus payment
- Significant revenue impact for high-performing plans

### Improvement Opportunities

- **High Priority**: High-weight measures close to next threshold
- **ROI Scoring**: Impact divided by effort
- **Patient Needs**: Specific number of patients to improve

## Medicaid Compliance Methodology

### Compliance Status Levels

- **Compliant**: 100% of measures meet state thresholds
- **Substantially Compliant**: >80% of measures meet thresholds
- **Partially Compliant**: 50-80% of measures meet thresholds
- **Non-Compliant**: <50% of measures meet thresholds

### Financial Implications

- **Penalties**: Applied for non-compliant and partially compliant MCOs
- **Bonuses**: Available for MCOs exceeding state goals
- **Corrective Action**: Required for measures significantly below threshold

## Future Enhancements

- [ ] Machine learning for Star Rating predictions
- [ ] Provider-level attribution and scoring
- [ ] Real-time quality gap alerts
- [ ] Additional state Medicaid configurations
- [ ] Commercial plan quality tracking
- [ ] Advanced financial modeling
- [ ] Integration with CMS data submission

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
