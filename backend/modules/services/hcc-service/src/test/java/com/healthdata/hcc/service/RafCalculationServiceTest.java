package com.healthdata.hcc.service;

import com.healthdata.hcc.persistence.*;
import com.healthdata.hcc.service.RafCalculationService.DemographicFactors;
import com.healthdata.hcc.service.RafCalculationService.RafCalculationResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RafCalculationService.
 * Tests RAF score calculation logic, HCC mapping, and hierarchy application.
 */
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
        // Set default blending weights (2025 transition: 33% V24, 67% V28)
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
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

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
            assertThat(result.getDiagnosisCount()).isEqualTo(1);

            verify(profileRepository).save(any(PatientHccProfileEntity.class));
        }

        @Test
        @DisplayName("Should calculate RAF with multiple diagnosis codes")
        void calculateRaf_withMultipleDiagnoses_shouldAggregateHccs() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.9", "I10", "J44.1");
            DemographicFactors factors = createDefaultDemographicFactors();

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.9")
                    .hccCodeV24("HCC19")
                    .hccCodeV28("HCC37")
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("I10")
                    .hccCodeV24(null) // Hypertension not HCC in V24
                    .hccCodeV28(null)
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("J44.1")
                    .hccCodeV24("HCC111")
                    .hccCodeV28("HCC280")
                    .build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(mappings);
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert
            assertThat(result.getDiagnosisCount()).isEqualTo(3);
            assertThat(result.getHccCountV24()).isEqualTo(2); // E11.9 and J44.1
            assertThat(result.getHccCountV28()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should apply blended weights correctly")
        void calculateRaf_shouldApplyBlendedWeights() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.9");
            DemographicFactors factors = createDefaultDemographicFactors();

            DiagnosisHccMapEntity mapping = DiagnosisHccMapEntity.builder()
                .icd10Code("E11.9")
                .hccCodeV24("HCC19")
                .hccCodeV28("HCC37")
                .build();

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(List.of(mapping));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

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
                .isCloseTo(expectedBlended.doubleValue(), org.assertj.core.data.Offset.offset(0.00001));
        }

        @Test
        @DisplayName("Should handle empty diagnosis list")
        void calculateRaf_withNoDiagnoses_shouldReturnBaselineScore() {
            // Arrange
            List<String> diagnosisCodes = Collections.emptyList();
            DemographicFactors factors = createDefaultDemographicFactors();

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(Collections.emptyList());
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert
            assertThat(result.getHccsV24()).isEmpty();
            assertThat(result.getHccsV28()).isEmpty();
            assertThat(result.getDiagnosisCount()).isEqualTo(0);
            // Should still have demographic baseline score
            assertThat(result.getRafScoreV24()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should update existing profile")
        void calculateRaf_withExistingProfile_shouldUpdate() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.9");
            DemographicFactors factors = createDefaultDemographicFactors();

            DiagnosisHccMapEntity mapping = DiagnosisHccMapEntity.builder()
                .icd10Code("E11.9")
                .hccCodeV24("HCC19")
                .hccCodeV28("HCC37")
                .build();

            PatientHccProfileEntity existingProfile = PatientHccProfileEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(java.time.LocalDate.now().getYear())
                .build();

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(List.of(mapping));
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.of(existingProfile));

            // Act
            rafCalculationService.calculateRaf(TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert
            verify(profileRepository).save(existingProfile);
        }
    }

    @Nested
    @DisplayName("HCC Hierarchy tests")
    class HccHierarchyTests {

        @Test
        @DisplayName("Should apply diabetes hierarchy - HCC17 trumps HCC18 and HCC19")
        void calculateRaf_withDiabetesHierarchy_shouldKeepHighestSeverity() {
            // Arrange
            List<String> diagnosisCodes = List.of("E11.65", "E11.9"); // Complications + without
            DemographicFactors factors = createDefaultDemographicFactors();

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.65")
                    .hccCodeV24("HCC17") // Acute complications
                    .hccCodeV28("HCC17")
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.9")
                    .hccCodeV24("HCC19") // Without complications
                    .hccCodeV28("HCC37")
                    .build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(mappings);
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert - HCC17 should trump HCC19 in V24
            assertThat(result.getHccsV24()).contains("HCC17");
            assertThat(result.getHccsV24()).doesNotContain("HCC19");
        }

        @Test
        @DisplayName("Should apply cancer hierarchy - HCC8 trumps lower severity")
        void calculateRaf_withCancerHierarchy_shouldKeepMetastatic() {
            // Arrange
            List<String> diagnosisCodes = List.of("C78.0", "C34.1"); // Metastatic + lung primary
            DemographicFactors factors = createDefaultDemographicFactors();

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("C78.0")
                    .hccCodeV24("HCC8") // Metastatic cancer
                    .hccCodeV28("HCC8")
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("C34.1")
                    .hccCodeV24("HCC9") // Lung cancer primary
                    .hccCodeV28("HCC9")
                    .build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(diagnosisCodes))
                .thenReturn(mappings);
            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(anyString(), any(UUID.class), anyInt()))
                .thenReturn(Optional.empty());

            // Act
            RafCalculationResult result = rafCalculationService.calculateRaf(
                TENANT_ID, PATIENT_ID, diagnosisCodes, factors);

            // Assert - HCC8 should trump HCC9
            assertThat(result.getHccsV24()).contains("HCC8");
            assertThat(result.getHccsV24()).doesNotContain("HCC9");
        }
    }

    @Nested
    @DisplayName("getCrosswalk() tests")
    class GetCrosswalkTests {

        @Test
        @DisplayName("Should return crosswalk for valid ICD-10 code")
        void getCrosswalk_withValidCode_shouldReturnMapping() {
            // Arrange
            String icd10Code = "E11.9";
            DiagnosisHccMapEntity mapping = DiagnosisHccMapEntity.builder()
                .icd10Code(icd10Code)
                .hccCodeV24("HCC19")
                .hccCodeV28("HCC37")
                .hccNameV24("Diabetes without Complication")
                .requiresSpecificity(false)
                .build();

            when(diagnosisMapRepository.findByIcd10Code(icd10Code))
                .thenReturn(Optional.of(mapping));

            // Act
            Optional<DiagnosisHccMapEntity> result = rafCalculationService.getCrosswalk(icd10Code);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getHccCodeV24()).isEqualTo("HCC19");
            assertThat(result.get().getHccCodeV28()).isEqualTo("HCC37");
        }

        @Test
        @DisplayName("Should return empty for unknown ICD-10 code")
        void getCrosswalk_withUnknownCode_shouldReturnEmpty() {
            // Arrange
            String icd10Code = "INVALID";

            when(diagnosisMapRepository.findByIcd10Code(icd10Code))
                .thenReturn(Optional.empty());

            // Act
            Optional<DiagnosisHccMapEntity> result = rafCalculationService.getCrosswalk(icd10Code);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("batchCrosswalk() tests")
    class BatchCrosswalkTests {

        @Test
        @DisplayName("Should return map of crosswalks for multiple codes")
        void batchCrosswalk_withMultipleCodes_shouldReturnMap() {
            // Arrange
            List<String> codes = List.of("E11.9", "I10", "J44.1");

            List<DiagnosisHccMapEntity> mappings = List.of(
                DiagnosisHccMapEntity.builder().icd10Code("E11.9").hccCodeV24("HCC19").build(),
                DiagnosisHccMapEntity.builder().icd10Code("J44.1").hccCodeV24("HCC111").build()
            );

            when(diagnosisMapRepository.findByIcd10Codes(codes))
                .thenReturn(mappings);

            // Act
            Map<String, DiagnosisHccMapEntity> result = rafCalculationService.batchCrosswalk(codes);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsKey("E11.9");
            assertThat(result).containsKey("J44.1");
            assertThat(result).doesNotContainKey("I10"); // No HCC mapping
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
