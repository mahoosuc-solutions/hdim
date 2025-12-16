package com.healthdata.hcc.controller;

import com.healthdata.hcc.persistence.*;
import com.healthdata.hcc.service.RafCalculationService;
import com.healthdata.hcc.service.RafCalculationService.DemographicFactors;
import com.healthdata.hcc.service.RafCalculationService.RafCalculationResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HccController.
 * Tests REST API endpoints for HCC risk adjustment operations.
 */
@ExtendWith(MockitoExtension.class)
class HccControllerTest {

    @Mock
    private RafCalculationService rafCalculationService;

    @Mock
    private PatientHccProfileRepository profileRepository;

    @Mock
    private DocumentationGapRepository gapRepository;

    @InjectMocks
    private HccController hccController;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Calculate RAF Endpoint")
    class CalculateRafTests {

        @Test
        @DisplayName("Should calculate RAF for valid request")
        void calculateRaf_withValidRequest_shouldReturnOk() {
            // Arrange
            HccController.RafCalculationRequest request = createRafRequest();
            RafCalculationResult expectedResult = createRafResult();

            when(rafCalculationService.calculateRaf(anyString(), any(UUID.class), anyList(), any()))
                .thenReturn(expectedResult);

            // Act
            ResponseEntity<RafCalculationResult> response =
                hccController.calculateRaf(TENANT_ID, PATIENT_ID, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(response.getBody().getRafScoreV24()).isNotNull();
            assertThat(response.getBody().getRafScoreV28()).isNotNull();
            assertThat(response.getBody().getRafScoreBlended()).isNotNull();

            verify(rafCalculationService).calculateRaf(
                eq(TENANT_ID),
                eq(PATIENT_ID),
                eq(request.getDiagnosisCodes()),
                any(DemographicFactors.class)
            );
        }

        @Test
        @DisplayName("Should handle multiple diagnosis codes")
        void calculateRaf_withMultipleDiagnoses_shouldProcessAll() {
            // Arrange
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(List.of("E11.9", "I10", "J44.1", "N18.3"));
            request.setAge(72);
            request.setSex("F");
            request.setDualEligible(true);

            RafCalculationResult expectedResult = RafCalculationResult.builder()
                .patientId(PATIENT_ID)
                .diagnosisCount(4)
                .hccCountV24(3)
                .hccCountV28(3)
                .rafScoreV24(BigDecimal.valueOf(1.567))
                .rafScoreV28(BigDecimal.valueOf(1.423))
                .rafScoreBlended(BigDecimal.valueOf(1.471))
                .build();

            when(rafCalculationService.calculateRaf(anyString(), any(UUID.class), anyList(), any()))
                .thenReturn(expectedResult);

            // Act
            ResponseEntity<RafCalculationResult> response =
                hccController.calculateRaf(TENANT_ID, PATIENT_ID, request);

            // Assert
            assertThat(response.getBody().getDiagnosisCount()).isEqualTo(4);
            assertThat(response.getBody().getHccCountV24()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle dual eligible patients")
        void calculateRaf_withDualEligible_shouldPassCorrectFactors() {
            // Arrange
            HccController.RafCalculationRequest request = createRafRequest();
            request.setDualEligible(true);

            when(rafCalculationService.calculateRaf(anyString(), any(UUID.class), anyList(), any()))
                .thenReturn(createRafResult());

            // Act
            hccController.calculateRaf(TENANT_ID, PATIENT_ID, request);

            // Assert
            verify(rafCalculationService).calculateRaf(
                eq(TENANT_ID),
                eq(PATIENT_ID),
                anyList(),
                argThat(factors -> factors.isDualEligible() == true)
            );
        }

        @Test
        @DisplayName("Should handle institutionalized patients")
        void calculateRaf_withInstitutionalized_shouldPassCorrectFactors() {
            // Arrange
            HccController.RafCalculationRequest request = createRafRequest();
            request.setInstitutionalized(true);

            when(rafCalculationService.calculateRaf(anyString(), any(UUID.class), anyList(), any()))
                .thenReturn(createRafResult());

            // Act
            hccController.calculateRaf(TENANT_ID, PATIENT_ID, request);

            // Assert
            verify(rafCalculationService).calculateRaf(
                eq(TENANT_ID),
                eq(PATIENT_ID),
                anyList(),
                argThat(factors -> factors.isInstitutionalized() == true)
            );
        }
    }

    @Nested
    @DisplayName("Get Profile Endpoint")
    class GetProfileTests {

        @Test
        @DisplayName("Should return profile when exists")
        void getProfile_whenExists_shouldReturnOk() {
            // Arrange
            int year = 2025;
            PatientHccProfileEntity profile = createPatientProfile(year);

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, year))
                .thenReturn(Optional.of(profile));

            // Act
            ResponseEntity<PatientHccProfileEntity> response =
                hccController.getProfile(TENANT_ID, PATIENT_ID, year);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(response.getBody().getProfileYear()).isEqualTo(year);
        }

        @Test
        @DisplayName("Should return 404 when profile does not exist")
        void getProfile_whenNotExists_shouldReturn404() {
            // Arrange
            int year = 2025;

            when(profileRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, year))
                .thenReturn(Optional.empty());

            // Act
            ResponseEntity<PatientHccProfileEntity> response =
                hccController.getProfile(TENANT_ID, PATIENT_ID, year);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("Get Crosswalk Endpoint")
    class GetCrosswalkTests {

        @Test
        @DisplayName("Should return crosswalk for valid ICD-10 codes")
        void getCrosswalk_withValidCodes_shouldReturnMappings() {
            // Arrange
            List<String> icd10Codes = List.of("E11.9", "I10", "J44.1");
            Map<String, DiagnosisHccMapEntity> expectedMap = new HashMap<>();
            expectedMap.put("E11.9", createDiagnosisMapping("E11.9", "HCC19", "HCC37"));
            expectedMap.put("J44.1", createDiagnosisMapping("J44.1", "HCC111", "HCC280"));

            when(rafCalculationService.batchCrosswalk(icd10Codes))
                .thenReturn(expectedMap);

            // Act
            ResponseEntity<?> response = hccController.getCrosswalk(icd10Codes);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should handle empty code list")
        void getCrosswalk_withEmptyList_shouldReturnEmptyMap() {
            // Arrange
            List<String> icd10Codes = Collections.emptyList();
            when(rafCalculationService.batchCrosswalk(icd10Codes))
                .thenReturn(Collections.emptyMap());

            // Act
            ResponseEntity<?> response = hccController.getCrosswalk(icd10Codes);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Get Documentation Gaps Endpoint")
    class GetDocumentationGapsTests {

        @Test
        @DisplayName("Should return gaps for patient")
        void getDocumentationGaps_whenGapsExist_shouldReturnList() {
            // Arrange
            int year = 2025;
            List<DocumentationGapEntity> gaps = List.of(
                createDocumentationGap("E11.9", DocumentationGapEntity.GapType.UNSPECIFIED),
                createDocumentationGap("I50.9", DocumentationGapEntity.GapType.UNSPECIFIED)
            );

            when(gapRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, year))
                .thenReturn(gaps);

            // Act
            ResponseEntity<List<DocumentationGapEntity>> response =
                hccController.getDocumentationGaps(TENANT_ID, PATIENT_ID, year);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getCurrentIcd10()).isEqualTo("E11.9");
        }

        @Test
        @DisplayName("Should return empty list when no gaps")
        void getDocumentationGaps_whenNoGaps_shouldReturnEmptyList() {
            // Arrange
            int year = 2025;
            when(gapRepository.findByTenantIdAndPatientIdAndProfileYear(TENANT_ID, PATIENT_ID, year))
                .thenReturn(Collections.emptyList());

            // Act
            ResponseEntity<List<DocumentationGapEntity>> response =
                hccController.getDocumentationGaps(TENANT_ID, PATIENT_ID, year);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get High-Value Opportunities Endpoint")
    class GetHighValueOpportunitiesTests {

        @Test
        @DisplayName("Should return high-value opportunities")
        void getHighValueOpportunities_shouldReturnProfiles() {
            // Arrange
            int year = 2025;
            BigDecimal minUplift = BigDecimal.valueOf(0.1);

            List<PatientHccProfileEntity> opportunities = List.of(
                createPatientProfile(year),
                createPatientProfile(year)
            );

            when(profileRepository.findHighValueOpportunities(TENANT_ID, year, minUplift))
                .thenReturn(opportunities);

            // Act
            ResponseEntity<List<PatientHccProfileEntity>> response =
                hccController.getHighValueOpportunities(TENANT_ID, year, minUplift);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Should apply default min uplift")
        void getHighValueOpportunities_withDefaultMinUplift_shouldUse0Point1() {
            // Arrange
            int year = 2025;
            when(profileRepository.findHighValueOpportunities(eq(TENANT_ID), eq(year), any()))
                .thenReturn(Collections.emptyList());

            // Act
            hccController.getHighValueOpportunities(TENANT_ID, year, BigDecimal.valueOf(0.1));

            // Assert
            verify(profileRepository).findHighValueOpportunities(
                eq(TENANT_ID),
                eq(year),
                eq(BigDecimal.valueOf(0.1))
            );
        }
    }

    // Helper methods
    private HccController.RafCalculationRequest createRafRequest() {
        HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
        request.setDiagnosisCodes(List.of("E11.9", "I10"));
        request.setAge(65);
        request.setSex("M");
        request.setDualEligible(false);
        request.setInstitutionalized(false);
        return request;
    }

    private RafCalculationResult createRafResult() {
        return RafCalculationResult.builder()
            .patientId(PATIENT_ID)
            .diagnosisCount(2)
            .hccCountV24(1)
            .hccCountV28(1)
            .rafScoreV24(BigDecimal.valueOf(1.234))
            .rafScoreV28(BigDecimal.valueOf(1.123))
            .rafScoreBlended(BigDecimal.valueOf(1.160))
            .hccsV24(List.of("HCC19"))
            .hccsV28(List.of("HCC37"))
            .build();
    }

    private PatientHccProfileEntity createPatientProfile(int year) {
        return PatientHccProfileEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .profileYear(year)
            .rafScoreV24(BigDecimal.valueOf(1.234))
            .rafScoreV28(BigDecimal.valueOf(1.123))
            .rafScoreBlended(BigDecimal.valueOf(1.160))
            .hccsV24(List.of("HCC19", "HCC111"))
            .hccsV28(List.of("HCC37", "HCC280"))
            .build();
    }

    private DiagnosisHccMapEntity createDiagnosisMapping(String icd10, String hccV24, String hccV28) {
        return DiagnosisHccMapEntity.builder()
            .icd10Code(icd10)
            .hccCodeV24(hccV24)
            .hccCodeV28(hccV28)
            .icd10Description("Test diagnosis")
            .build();
    }

    private DocumentationGapEntity createDocumentationGap(String icd10, DocumentationGapEntity.GapType gapType) {
        return DocumentationGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .profileYear(2025)
            .currentIcd10(icd10)
            .gapType(gapType)
            .priority("MEDIUM")
            .status(DocumentationGapEntity.GapStatus.OPEN)
            .rafImpactV24(BigDecimal.valueOf(0.15))
            .rafImpactV28(BigDecimal.valueOf(0.12))
            .rafImpactBlended(BigDecimal.valueOf(0.13))
            .build();
    }
}
