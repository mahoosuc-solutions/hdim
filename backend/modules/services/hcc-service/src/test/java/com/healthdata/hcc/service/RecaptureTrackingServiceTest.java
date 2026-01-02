package com.healthdata.hcc.service;

import com.healthdata.hcc.persistence.*;
import com.healthdata.hcc.service.RecaptureTrackingService.RecaptureRateSummary;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecaptureTrackingService.
 * Tests HCC recapture opportunity detection, lifecycle tracking, and reporting.
 */
@ExtendWith(MockitoExtension.class)
class RecaptureTrackingServiceTest {

    @Mock
    private RecaptureOpportunityRepository recaptureRepository;

    @Mock
    private PatientHccProfileRepository profileRepository;

    @Mock
    private DiagnosisHccMapRepository diagnosisMapRepository;

    @InjectMocks
    private RecaptureTrackingService recaptureTrackingService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final int CURRENT_YEAR = 2025;
    private static final int PRIOR_YEAR = 2024;

    @Nested
    @DisplayName("identifyRecaptureOpportunities() tests")
    class IdentifyRecaptureOpportunitiesTests {

        @Test
        @DisplayName("Should identify chronic HCCs not recaptured")
        void identifyOpportunities_withChronicHccsNotRecaptured_shouldCreateOpportunities() {
            // Arrange
            PatientHccProfileEntity priorProfile = createPriorProfile(
                List.of("HCC17", "HCC85", "HCC111", "HCC137") // All chronic
            );
            PatientHccProfileEntity currentProfile = createCurrentProfile(
                List.of("HCC17") // Only HCC17 recaptured
            );

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, PRIOR_YEAR))
                .thenReturn(Optional.of(priorProfile));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, CURRENT_YEAR))
                .thenReturn(Optional.of(currentProfile));
            when(recaptureRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.identifyRecaptureOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(opportunities).hasSize(3); // HCC85, HCC111, HCC137 not recaptured
            assertThat(opportunities).allMatch(o -> !o.getIsRecaptured());
            assertThat(opportunities).anyMatch(o -> o.getHccCode().equals("HCC85"));
            assertThat(opportunities).anyMatch(o -> o.getHccCode().equals("HCC111"));
            assertThat(opportunities).anyMatch(o -> o.getHccCode().equals("HCC137"));

            verify(recaptureRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should ignore non-chronic HCCs")
        void identifyOpportunities_withNonChronicHccs_shouldNotCreateOpportunities() {
            // Arrange
            PatientHccProfileEntity priorProfile = createPriorProfile(
                List.of("HCC1", "HCC2", "HCC3") // Not in chronic list
            );

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, PRIOR_YEAR))
                .thenReturn(Optional.of(priorProfile));
            // Don't stub current year call - service returns empty list early

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.identifyRecaptureOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(opportunities).isEmpty();
        }

        @Test
        @DisplayName("Should handle no prior year profile")
        void identifyOpportunities_withNoPriorProfile_shouldReturnEmpty() {
            // Arrange
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, PRIOR_YEAR))
                .thenReturn(Optional.empty());

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.identifyRecaptureOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(opportunities).isEmpty();
        }

        @Test
        @DisplayName("Should update profile with recapture summary")
        void identifyOpportunities_shouldUpdateProfileSummary() {
            // Arrange
            PatientHccProfileEntity priorProfile = createPriorProfile(List.of("HCC85", "HCC111"));
            PatientHccProfileEntity currentProfile = createCurrentProfile(Collections.emptyList());

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, PRIOR_YEAR))
                .thenReturn(Optional.of(priorProfile));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, CURRENT_YEAR))
                .thenReturn(Optional.of(currentProfile));
            when(recaptureRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            recaptureTrackingService.identifyRecaptureOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            verify(profileRepository).save(argThat(p ->
                p.getRecaptureOpportunitiesCount() == 2 &&
                p.getRecaptureRafValue() != null
            ));
        }

        @Test
        @DisplayName("Should set priority based on RAF value")
        void identifyOpportunities_shouldSetPriorityByRafValue() {
            // Arrange
            PatientHccProfileEntity priorProfile = createPriorProfile(List.of("HCC85"));

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, PRIOR_YEAR))
                .thenReturn(Optional.of(priorProfile));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, CURRENT_YEAR))
                .thenReturn(Optional.empty());
            when(recaptureRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.identifyRecaptureOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(opportunities).isNotEmpty();
            assertThat(opportunities).allMatch(o -> o.getPriority() != null);
        }
    }

    @Nested
    @DisplayName("getOpenOpportunities() tests")
    class GetOpenOpportunitiesTests {

        @Test
        @DisplayName("Should return only non-recaptured opportunities")
        void getOpenOpportunities_shouldFilterByRecaptureStatus() {
            // Arrange
            List<RecaptureOpportunityEntity> allOpportunities = List.of(
                createOpportunity("HCC85", false),
                createOpportunity("HCC111", false),
                createOpportunity("HCC17", true)  // Already recaptured
            );

            when(recaptureRepository.findByTenantIdAndPatientIdAndCurrentYear(TENANT_ID, PATIENT_ID, CURRENT_YEAR))
                .thenReturn(allOpportunities);

            // Act
            List<RecaptureOpportunityEntity> open =
                recaptureTrackingService.getOpenOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(open).hasSize(2);
            assertThat(open).allMatch(o -> !o.getIsRecaptured());
        }

        @Test
        @DisplayName("Should return empty when all recaptured")
        void getOpenOpportunities_whenAllRecaptured_shouldReturnEmpty() {
            // Arrange
            List<RecaptureOpportunityEntity> allRecaptured = List.of(
                createOpportunity("HCC85", true),
                createOpportunity("HCC111", true)
            );

            when(recaptureRepository.findByTenantIdAndPatientIdAndCurrentYear(TENANT_ID, PATIENT_ID, CURRENT_YEAR))
                .thenReturn(allRecaptured);

            // Act
            List<RecaptureOpportunityEntity> open =
                recaptureTrackingService.getOpenOpportunities(TENANT_ID, PATIENT_ID, CURRENT_YEAR);

            // Assert
            assertThat(open).isEmpty();
        }
    }

    @Nested
    @DisplayName("getHighValueOpportunities() tests")
    class GetHighValueOpportunitiesTests {

        @Test
        @DisplayName("Should retrieve high-value opportunities from repository")
        void getHighValueOpportunities_shouldCallRepositoryWithMinValue() {
            // Arrange
            BigDecimal minRafValue = BigDecimal.valueOf(0.3);
            List<RecaptureOpportunityEntity> highValue = List.of(
                createOpportunity("HCC85", false),
                createOpportunity("HCC111", false)
            );

            when(recaptureRepository.findHighValueOpportunities(TENANT_ID, CURRENT_YEAR, minRafValue))
                .thenReturn(highValue);

            // Act
            List<RecaptureOpportunityEntity> opportunities =
                recaptureTrackingService.getHighValueOpportunities(TENANT_ID, CURRENT_YEAR, minRafValue);

            // Assert
            assertThat(opportunities).hasSize(2);
            verify(recaptureRepository).findHighValueOpportunities(TENANT_ID, CURRENT_YEAR, minRafValue);
        }
    }

    @Nested
    @DisplayName("markRecaptured() tests")
    class MarkRecapturedTests {

        @Test
        @DisplayName("Should mark opportunity as recaptured with ICD-10 code")
        void markRecaptured_shouldUpdateStatusAndCode() {
            // Arrange
            UUID opportunityId = UUID.randomUUID();
            String recapturedIcd10 = "E11.65";

            RecaptureOpportunityEntity opportunity = createOpportunity("HCC17", false);
            opportunity.setId(opportunityId);

            when(recaptureRepository.findById(opportunityId))
                .thenReturn(Optional.of(opportunity));
            when(recaptureRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RecaptureOpportunityEntity recaptured =
                recaptureTrackingService.markRecaptured(opportunityId, recapturedIcd10);

            // Assert
            assertThat(recaptured.getIsRecaptured()).isTrue();
            assertThat(recaptured.getRecapturedIcd10()).isEqualTo(recapturedIcd10);
            assertThat(recaptured.getRecapturedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when opportunity not found")
        void markRecaptured_withInvalidId_shouldThrowException() {
            // Arrange
            UUID opportunityId = UUID.randomUUID();
            when(recaptureRepository.findById(opportunityId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                recaptureTrackingService.markRecaptured(opportunityId, "E11.65")
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opportunity not found");
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

            List<Object[]> statsList = new ArrayList<>();
            statsList.add(stats);

            when(recaptureRepository.getRecaptureStatsByTenant(TENANT_ID, CURRENT_YEAR))
                .thenReturn(statsList);

            // Act
            RecaptureRateSummary summary =
                recaptureTrackingService.calculateRecaptureRate(TENANT_ID, CURRENT_YEAR);

            // Assert
            assertThat(summary.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(summary.getYear()).isEqualTo(CURRENT_YEAR);
            assertThat(summary.getTotalOpportunities()).isEqualTo(10);
            assertThat(summary.getRecapturedCount()).isEqualTo(7);
            assertThat(summary.getPendingCount()).isEqualTo(3);
            assertThat(summary.getRecaptureRate()).isCloseTo(70.0, org.assertj.core.data.Offset.offset(0.01));
            assertThat(summary.getTotalRafAtRisk()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
            assertThat(summary.getRafSecured()).isEqualByComparingTo(BigDecimal.valueOf(1.05));
            assertThat(summary.getRafPending()).isEqualByComparingTo(BigDecimal.valueOf(0.45));
        }

        @Test
        @DisplayName("Should handle zero opportunities")
        void calculateRecaptureRate_withNoOpportunities_shouldReturnZeroRate() {
            // Arrange
            when(recaptureRepository.getRecaptureStatsByTenant(TENANT_ID, CURRENT_YEAR))
                .thenReturn(Collections.emptyList());

            // Act
            RecaptureRateSummary summary =
                recaptureTrackingService.calculateRecaptureRate(TENANT_ID, CURRENT_YEAR);

            // Assert
            assertThat(summary.getTotalOpportunities()).isEqualTo(0);
            assertThat(summary.getRecaptureRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should aggregate multiple stat rows")
        void calculateRecaptureRate_withMultipleRows_shouldAggregate() {
            // Arrange
            List<Object[]> stats = List.of(
                new Object[]{5L, 3L, BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.5)},
                new Object[]{3L, 2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3)}
            );

            when(recaptureRepository.getRecaptureStatsByTenant(TENANT_ID, CURRENT_YEAR))
                .thenReturn(stats);

            // Act
            RecaptureRateSummary summary =
                recaptureTrackingService.calculateRecaptureRate(TENANT_ID, CURRENT_YEAR);

            // Assert
            assertThat(summary.getTotalOpportunities()).isEqualTo(8);
            assertThat(summary.getRecapturedCount()).isEqualTo(5);
            assertThat(summary.getTotalRafAtRisk()).isEqualByComparingTo(BigDecimal.valueOf(1.3));
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

            when(recaptureRepository.findPendingByTenant(TENANT_ID, CURRENT_YEAR))
                .thenReturn(opportunities);

            // Act
            Map<String, List<RecaptureOpportunityEntity>> worklist =
                recaptureTrackingService.getRecaptureWorklist(TENANT_ID, CURRENT_YEAR);

            // Assert
            assertThat(worklist).containsKeys("HIGH", "MEDIUM", "LOW");
            assertThat(worklist.get("HIGH")).hasSize(2);
            assertThat(worklist.get("MEDIUM")).hasSize(1);
            assertThat(worklist.get("LOW")).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty map when no pending opportunities")
        void getRecaptureWorklist_withNoPending_shouldReturnEmpty() {
            // Arrange
            when(recaptureRepository.findPendingByTenant(TENANT_ID, CURRENT_YEAR))
                .thenReturn(Collections.emptyList());

            // Act
            Map<String, List<RecaptureOpportunityEntity>> worklist =
                recaptureTrackingService.getRecaptureWorklist(TENANT_ID, CURRENT_YEAR);

            // Assert
            assertThat(worklist).isEmpty();
        }
    }

    // Helper methods
    private PatientHccProfileEntity createPriorProfile(List<String> hccs) {
        return PatientHccProfileEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .profileYear(PRIOR_YEAR)
            .hccsV28(hccs)
            .diagnosisCodes(List.of("E11.9", "I50.9"))
            .rafScoreV24(BigDecimal.valueOf(1.234))
            .rafScoreV28(BigDecimal.valueOf(1.123))
            .build();
    }

    private PatientHccProfileEntity createCurrentProfile(List<String> hccs) {
        return PatientHccProfileEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .profileYear(CURRENT_YEAR)
            .hccsV28(hccs)
            .rafScoreV24(BigDecimal.valueOf(1.000))
            .rafScoreV28(BigDecimal.valueOf(0.950))
            .build();
    }

    private RecaptureOpportunityEntity createOpportunity(String hccCode, boolean isRecaptured) {
        return RecaptureOpportunityEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .priorYear(PRIOR_YEAR)
            .currentYear(CURRENT_YEAR)
            .hccCode(hccCode)
            .hccName(getHccName(hccCode))
            .rafValueV24(BigDecimal.valueOf(0.302))
            .rafValueV28(BigDecimal.valueOf(0.285))
            .isRecaptured(isRecaptured)
            .recapturedIcd10(isRecaptured ? "E11.65" : null)
            .recapturedAt(isRecaptured ? LocalDateTime.now() : null)
            .priority("MEDIUM")
            .clinicalGuidance("Document MEAT criteria")
            .build();
    }

    private RecaptureOpportunityEntity createOpportunityWithPriority(String hccCode, String priority) {
        RecaptureOpportunityEntity opportunity = createOpportunity(hccCode, false);
        opportunity.setPriority(priority);
        return opportunity;
    }

    private String getHccName(String hccCode) {
        return switch (hccCode) {
            case "HCC17" -> "Diabetes with Acute Complications";
            case "HCC19" -> "Diabetes without Complication";
            case "HCC85" -> "Congestive Heart Failure";
            case "HCC111" -> "Chronic Obstructive Pulmonary Disease";
            default -> "HCC " + hccCode;
        };
    }
}
