package com.healthdata.payer.service;

import com.healthdata.payer.domain.RoiCalculation;
import com.healthdata.payer.dto.RoiCalculationRequest;
import com.healthdata.payer.dto.RoiCalculationResponse;
import com.healthdata.payer.repository.RoiCalculationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoiCalculationService.
 *
 * Validates that Java BigDecimal calculations produce identical results
 * to the frontend ROICalculator.tsx (lines 57-135).
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ROI Calculation Service Tests")
class RoiCalculationServiceTest {

    @Mock
    private RoiCalculationRepository repository;

    private RoiCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RoiCalculationService(repository);
    }

    // ===== Default Input Test (matches TSX defaults) =====

    @Test
    @DisplayName("Should match TSX defaults: 25K patients, ACO, 70%, 3.5 stars, 40h")
    void shouldMatchTsxDefaultInputs() {
        // Given: exact TSX default values
        RoiCalculationRequest request = RoiCalculationRequest.builder()
                .orgType("ACO")
                .patientPopulation(25000)
                .currentQualityScore(70.0)
                .currentStarRating(3.5)
                .manualReportingHours(40)
                .save(false)
                .build();

        // When
        RoiCalculationResponse result = service.calculate(request, "test-tenant");

        // Then: verify key calculations
        // baseImprovement = 0.25, gapFactor = (100-70)/30 = 1.0
        // projectedImprovement = 0.25 * 1.0 = 0.25
        // projectedScore = min(70 * 1.25, 95) = 87.5
        // qualityImprovement = 87.5 - 70 = 17.5
        assertThat(result.getQualityImprovement()).isEqualByComparingTo("17.5");
        assertThat(result.getProjectedScore()).isEqualByComparingTo("87.5");

        // starImprovement = (17.5/10) * 0.5 = 0.875 → rounded to 1 decimal = 0.9
        assertThat(result.getStarImprovement()).isEqualByComparingTo("0.9");

        // projectedStarRating: round((3.5 + 0.875) * 2) / 2 = round(8.75)/2 = 9/2 = 4.5
        assertThat(result.getProjectedStarRating()).isEqualByComparingTo("4.5");

        // qualityBonuses = star bonuses + shared savings
        // star: (1100 - 0) * 25000 * 0.3 = 8,250,000
        // shared: 17.5 * 75000 = 1,312,500
        // combined = 9,562,500
        assertThat(result.getQualityBonuses()).isEqualByComparingTo("9562500");

        // adminSavings = 40 * 0.67 * 75 * 12 = 24,120
        assertThat(result.getAdminSavings()).isEqualByComparingTo("24120");

        // gapClosureValue = 25000 * 0.3 * 0.35 * 105 = 275,625
        assertThat(result.getGapClosureValue()).isEqualByComparingTo("275625");

        // totalYear1Value = 9,562,500 + 24,120 + 275,625 = 9,862,245
        assertThat(result.getTotalYear1Value()).isEqualByComparingTo("9862245");

        // year1Investment = 36,000 (25K > 20K threshold)
        assertThat(result.getYear1Investment()).isEqualByComparingTo("36000");

        // year1ROI = (9,862,245 - 36,000) / 36,000 * 100 = 27,295.13 → rounded to 27295
        assertThat(result.getYear1ROI()).isEqualByComparingTo("27295");

        // paybackDays = (36,000 / 9,862,245) * 365 ≈ 1.33 → rounded to 1
        assertThat(result.getPaybackDays()).isEqualByComparingTo("1");

        // NPV should be positive
        assertThat(result.getThreeYearNPV().compareTo(BigDecimal.ZERO)).isPositive();
    }

    // ===== Org Type Parsing =====

    @ParameterizedTest
    @CsvSource({
        "ACO, ACO",
        "Health System, HEALTH_SYSTEM",
        "HEALTH_SYSTEM, HEALTH_SYSTEM",
        "HIE, HIE",
        "Payer, PAYER",
        "FQHC, FQHC"
    })
    @DisplayName("Should parse all org type variants")
    void shouldParseOrgTypes(String input, String expectedEnum) {
        assertThat(RoiCalculationService.parseOrgType(input).name()).isEqualTo(expectedEnum);
    }

    @Test
    @DisplayName("Should reject unknown org type")
    void shouldRejectUnknownOrgType() {
        assertThatThrownBy(() -> RoiCalculationService.parseOrgType("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown organization type");
    }

    // ===== Investment Tiers =====

    @ParameterizedTest
    @CsvSource({
        "5000, 24000",    // < 20K
        "15000, 24000",   // < 20K
        "25000, 36000",   // > 20K
        "50001, 48000",   // > 50K
        "100001, 60000"   // > 100K
    })
    @DisplayName("Should apply correct investment tier")
    void shouldApplyCorrectInvestmentTier(int population, int expectedInvestment) {
        RoiCalculationRequest request = RoiCalculationRequest.builder()
                .orgType("ACO")
                .patientPopulation(population)
                .currentQualityScore(70.0)
                .currentStarRating(3.5)
                .manualReportingHours(40)
                .save(false)
                .build();

        RoiCalculationResponse result = service.calculate(request, null);

        assertThat(result.getYear1Investment()).isEqualByComparingTo(String.valueOf(expectedInvestment));
    }

    // ===== Shared Savings Tiers =====

    @Test
    @DisplayName("Should use 25K shared savings for small populations")
    void shouldUseSmallSharedSavings() {
        RoiCalculationRequest request = buildRequest(5000, "ACO", 70.0, 3.5, 40);
        RoiCalculationResponse result = service.calculate(request, null);
        // With qualityImprovement=17.5, sharedSavings = 17.5 * 25000 = 437500
        // This is part of qualityBonuses (combined)
        assertThat(result.getQualityBonuses().compareTo(BigDecimal.ZERO)).isPositive();
    }

    // ===== Score Cap at 95 =====

    @Test
    @DisplayName("Should cap projected quality score at 95")
    void shouldCapProjectedScoreAt95() {
        // With Payer (0.28) and low score, improvement would push above 95
        RoiCalculationRequest request = buildRequest(25000, "Payer", 50.0, 2.0, 40);
        RoiCalculationResponse result = service.calculate(request, null);
        assertThat(result.getProjectedScore().doubleValue()).isLessThanOrEqualTo(95.0);
    }

    // ===== Star Rating Cap at 5.0 =====

    @Test
    @DisplayName("Should cap projected star rating at 5.0")
    void shouldCapProjectedStarRatingAt5() {
        // High starting star with large improvement
        RoiCalculationRequest request = buildRequest(25000, "Payer", 50.0, 4.5, 40);
        RoiCalculationResponse result = service.calculate(request, null);
        assertThat(result.getProjectedStarRating().doubleValue()).isLessThanOrEqualTo(5.0);
    }

    // ===== Save Behavior =====

    @Test
    @DisplayName("Should not save when save=false")
    void shouldNotSaveWhenFalse() {
        RoiCalculationRequest request = buildRequest(25000, "ACO", 70.0, 3.5, 40);
        request.setSave(false);

        RoiCalculationResponse result = service.calculate(request, "test-tenant");

        assertThat(result.getId()).isNull();
        assertThat(result.getShareUrl()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should save and return ID when save=true")
    void shouldSaveWhenTrue() {
        RoiCalculation saved = RoiCalculation.builder().build();
        saved.setId("test-uuid-123");
        when(repository.save(any(RoiCalculation.class))).thenReturn(saved);

        RoiCalculationRequest request = buildRequest(25000, "ACO", 70.0, 3.5, 40);
        request.setSave(true);
        request.setContactName("John Doe");
        request.setContactEmail("john@example.com");
        request.setContactCompany("Acme Health");

        RoiCalculationResponse result = service.calculate(request, "test-tenant");

        assertThat(result.getId()).isEqualTo("test-uuid-123");
        assertThat(result.getShareUrl()).contains("test-uuid-123");
        verify(repository).save(any(RoiCalculation.class));
    }

    // ===== All Org Types Produce Positive Results =====

    @ParameterizedTest
    @CsvSource({"ACO", "HEALTH_SYSTEM", "HIE", "PAYER", "FQHC"})
    @DisplayName("Should produce positive ROI for all org types")
    void shouldProducePositiveROIForAllOrgTypes(String orgType) {
        RoiCalculationRequest request = buildRequest(25000, orgType, 70.0, 3.5, 40);
        RoiCalculationResponse result = service.calculate(request, null);

        assertThat(result.getTotalYear1Value().compareTo(BigDecimal.ZERO)).isPositive();
        assertThat(result.getYear1ROI().compareTo(BigDecimal.ZERO)).isPositive();
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("Should handle zero manual reporting hours")
    void shouldHandleZeroReportingHours() {
        RoiCalculationRequest request = buildRequest(25000, "ACO", 70.0, 3.5, 0);
        RoiCalculationResponse result = service.calculate(request, null);
        assertThat(result.getAdminSavings()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Should handle high quality score (minimal room for improvement)")
    void shouldHandleHighQualityScore() {
        RoiCalculationRequest request = buildRequest(25000, "ACO", 92.0, 4.5, 40);
        RoiCalculationResponse result = service.calculate(request, null);
        // Room for improvement is small: (100-92)/30 = 0.267
        assertThat(result.getQualityImprovement().doubleValue()).isLessThan(10.0);
    }

    private RoiCalculationRequest buildRequest(int population, String orgType,
            double qualityScore, double starRating, int hours) {
        return RoiCalculationRequest.builder()
                .orgType(orgType)
                .patientPopulation(population)
                .currentQualityScore(qualityScore)
                .currentStarRating(starRating)
                .manualReportingHours(hours)
                .save(false)
                .build();
    }
}
