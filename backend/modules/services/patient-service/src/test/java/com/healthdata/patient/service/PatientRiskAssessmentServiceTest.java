package com.healthdata.patient.service;

import com.healthdata.patient.client.CareGapServiceClient;
import com.healthdata.patient.client.HccServiceClient;
import com.healthdata.patient.dto.PatientRiskAssessmentResponse;
import com.healthdata.patient.dto.PatientRiskAssessmentResponse.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PatientRiskAssessmentService.
 * Tests risk assessment calculation combining HCC profiles and care gap data.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Risk Assessment Service Tests")
class PatientRiskAssessmentServiceTest {

    @Mock
    private HccServiceClient hccServiceClient;

    @Mock
    private CareGapServiceClient careGapServiceClient;

    private PatientRiskAssessmentService riskAssessmentService;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final UUID PATIENT_UUID = UUID.fromString(PATIENT_ID);
    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        riskAssessmentService = new PatientRiskAssessmentService(hccServiceClient, careGapServiceClient);
    }

    @Nested
    @DisplayName("Get Risk Assessment Tests")
    class GetRiskAssessmentTests {

        @Test
        @DisplayName("Should return comprehensive risk assessment with all data available")
        void shouldReturnComprehensiveRiskAssessment() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("1.25"), // blended
                new BigDecimal("1.20"), // v24
                new BigDecimal("1.30"), // v28
                List.of("HCC18", "HCC85", "HCC111")
            );
            when(hccServiceClient.getPatientHccProfile(eq(TENANT_ID), eq(PATIENT_UUID), eq(CURRENT_YEAR)))
                .thenReturn(hccProfile);

            CareGapServiceClient.CareGapCountResponse careGaps = createCareGapCounts(5, 2, 1);
            when(careGapServiceClient.getCareGapCount(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(careGaps);

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(response.getRafScoreBlended()).isEqualByComparingTo(new BigDecimal("1.25"));
            assertThat(response.getRafScoreV24()).isEqualByComparingTo(new BigDecimal("1.20"));
            assertThat(response.getRafScoreV28()).isEqualByComparingTo(new BigDecimal("1.30"));
            assertThat(response.getHccCount()).isEqualTo(3);
            assertThat(response.getOpenCareGaps()).isEqualTo(5);
            assertThat(response.getHighPriorityCareGaps()).isEqualTo(2);
            assertThat(response.getDataAvailability().isHccDataAvailable()).isTrue();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should calculate HIGH risk level for RAF >= 2.0")
        void shouldCalculateHighRiskLevel() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("2.50"), null, null, List.of("HCC8", "HCC85", "HCC134")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(response.getRiskScore()).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("Should calculate MODERATE risk level for RAF between 1.0 and 2.0")
        void shouldCalculateModerateRiskLevel() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("1.50"), null, null, List.of("HCC18", "HCC111")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.MODERATE);
            assertThat(response.getRiskScore()).isBetween(40, 69);
        }

        @Test
        @DisplayName("Should calculate LOW risk level for RAF < 1.0")
        void shouldCalculateLowRiskLevel() {
            // Given - RAF of 0.50 will give a lower score
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("0.50"), null, null, List.of()
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then - RAF 0.50 → score ≈ 20 based on logarithmic scaling (score = 50 + 30*log2(RAF))
            // For LOW risk (RAF < 1.0), score should be < 50 (since RAF 1.0 → score 50)
            assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(response.getRiskScore()).isLessThan(50);
        }

        @Test
        @DisplayName("Should map HCC codes to chronic conditions")
        void shouldMapHccCodesToChronicConditions() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("1.80"), null, null,
                List.of("HCC18", "HCC85", "HCC111", "HCC55")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getChronicConditions()).isNotEmpty();
            assertThat(response.getChronicConditions()).contains(
                "Diabetes with Chronic Complications",
                "Congestive Heart Failure",
                "Chronic Obstructive Pulmonary Disease"
            );
        }

        @Test
        @DisplayName("Should limit top HCCs to 5")
        void shouldLimitTopHccsToFive() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("3.00"), null, null,
                List.of("HCC8", "HCC18", "HCC85", "HCC111", "HCC134", "HCC55", "HCC96")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getTopHccs()).hasSize(5);
            assertThat(response.getHccCount()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Graceful Fallback Tests")
    class GracefulFallbackTests {

        @Test
        @DisplayName("Should handle HCC service failure gracefully")
        void shouldHandleHccServiceFailure() {
            // Given
            when(hccServiceClient.getPatientHccProfile(any(), any(), any()))
                .thenThrow(new RuntimeException("HCC service unavailable"));
            when(careGapServiceClient.getCareGapCount(any(), any()))
                .thenReturn(createCareGapCounts(3, 1, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDataAvailability().isHccDataAvailable()).isFalse();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isTrue();
            assertThat(response.getRafScoreBlended()).isNull();
            assertThat(response.getOpenCareGaps()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle care gap service failure gracefully")
        void shouldHandleCareGapServiceFailure() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("1.25"), null, null, List.of("HCC18")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any()))
                .thenThrow(new RuntimeException("Care gap service unavailable"));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDataAvailability().isHccDataAvailable()).isTrue();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isFalse();
            assertThat(response.getRafScoreBlended()).isEqualByComparingTo(new BigDecimal("1.25"));
            assertThat(response.getOpenCareGaps()).isZero();
        }

        @Test
        @DisplayName("Should handle both services failing gracefully")
        void shouldHandleBothServicesFailure() {
            // Given
            when(hccServiceClient.getPatientHccProfile(any(), any(), any()))
                .thenThrow(new RuntimeException("HCC service unavailable"));
            when(careGapServiceClient.getCareGapCount(any(), any()))
                .thenThrow(new RuntimeException("Care gap service unavailable"));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDataAvailability().isHccDataAvailable()).isFalse();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isFalse();
            assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(response.getRiskScore()).isZero();
        }

        @Test
        @DisplayName("Should handle null HCC profile response")
        void shouldHandleNullHccProfile() {
            // Given
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(null);
            when(careGapServiceClient.getCareGapCount(any(), any()))
                .thenReturn(createCareGapCounts(2, 1, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDataAvailability().isHccDataAvailable()).isFalse();
            assertThat(response.getRafScoreBlended()).isNull();
            assertThat(response.getHccCount()).isZero();
        }

        @Test
        @DisplayName("Should handle null care gap response")
        void shouldHandleNullCareGapResponse() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = createHccProfile(
                new BigDecimal("1.10"), null, null, List.of("HCC19")
            );
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(null);

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isFalse();
            assertThat(response.getOpenCareGaps()).isZero();
        }
    }

    @Nested
    @DisplayName("Invalid Input Tests")
    class InvalidInputTests {

        @Test
        @DisplayName("Should handle invalid patient ID format")
        void shouldHandleInvalidPatientId() {
            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, "invalid-uuid");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo("invalid-uuid");
            assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(response.getRiskScore()).isZero();
            assertThat(response.getDataAvailability().isHccDataAvailable()).isFalse();
            assertThat(response.getDataAvailability().isCareGapDataAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("HCC Version Preference Tests")
    class HccVersionPreferenceTests {

        @Test
        @DisplayName("Should prefer V28 HCCs when both V24 and V28 are available")
        void shouldPreferV28HccsWhenBothAvailable() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = HccServiceClient.HccProfileResponse.builder()
                .patientId(PATIENT_UUID)
                .tenantId(TENANT_ID)
                .profileYear(CURRENT_YEAR)
                .rafScoreBlended(new BigDecimal("1.50"))
                .hccsV24(List.of("HCC17", "HCC18"))  // Older version
                .hccsV28(List.of("HCC85", "HCC111", "HCC134"))  // Newer version - should be used
                .build();
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getHccCount()).isEqualTo(3);  // V28 HCCs count
            assertThat(response.getTopHccs()).containsExactly("HCC85", "HCC111", "HCC134");
        }

        @Test
        @DisplayName("Should fall back to V24 HCCs when V28 is empty")
        void shouldFallBackToV24WhenV28Empty() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = HccServiceClient.HccProfileResponse.builder()
                .patientId(PATIENT_UUID)
                .tenantId(TENANT_ID)
                .profileYear(CURRENT_YEAR)
                .rafScoreBlended(new BigDecimal("1.20"))
                .hccsV24(List.of("HCC17", "HCC18"))
                .hccsV28(List.of())  // Empty V28
                .build();
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getHccCount()).isEqualTo(2);  // V24 HCCs count
            assertThat(response.getTopHccs()).containsExactly("HCC17", "HCC18");
        }
    }

    @Nested
    @DisplayName("Opportunity Metrics Tests")
    class OpportunityMetricsTests {

        @Test
        @DisplayName("Should include documentation gap opportunities")
        void shouldIncludeDocumentationGapOpportunities() {
            // Given
            HccServiceClient.HccProfileResponse hccProfile = HccServiceClient.HccProfileResponse.builder()
                .patientId(PATIENT_UUID)
                .tenantId(TENANT_ID)
                .profileYear(CURRENT_YEAR)
                .rafScoreBlended(new BigDecimal("1.25"))
                .hccsV28(List.of("HCC18"))
                .documentationGapCount(3)
                .potentialRafUplift(new BigDecimal("0.45"))
                .recaptureOpportunitiesCount(2)
                .build();
            when(hccServiceClient.getPatientHccProfile(any(), any(), any())).thenReturn(hccProfile);
            when(careGapServiceClient.getCareGapCount(any(), any())).thenReturn(createCareGapCounts(0, 0, 0));

            // When
            PatientRiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(response.getDocumentationGapCount()).isEqualTo(3);
            assertThat(response.getPotentialRafUplift()).isEqualByComparingTo(new BigDecimal("0.45"));
            assertThat(response.getRecaptureOpportunities()).isEqualTo(2);
        }
    }

    // ==================== Helper Methods ====================

    private HccServiceClient.HccProfileResponse createHccProfile(
            BigDecimal rafBlended, BigDecimal rafV24, BigDecimal rafV28, List<String> hccs) {
        return HccServiceClient.HccProfileResponse.builder()
            .patientId(PATIENT_UUID)
            .tenantId(TENANT_ID)
            .profileYear(CURRENT_YEAR)
            .rafScoreBlended(rafBlended)
            .rafScoreV24(rafV24)
            .rafScoreV28(rafV28)
            .hccsV28(hccs)
            .build();
    }

    private CareGapServiceClient.CareGapCountResponse createCareGapCounts(long open, long highPriority, long overdue) {
        return CareGapServiceClient.CareGapCountResponse.builder()
            .open(open)
            .highPriority(highPriority)
            .overdue(overdue)
            .build();
    }
}
