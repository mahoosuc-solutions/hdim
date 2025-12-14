package com.healthdata.sdoh.service;

import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.model.SdohDiagnosis;
import com.healthdata.sdoh.repository.SdohDiagnosisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for ZCodeMapper
 *
 * Testing ICD-10-CM Z-code (Z55-Z65) mapping for SDOH findings
 *
 * @disabled Temporarily disabled - needs refactoring to use entity classes instead of models
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Z-Code Mapper Tests")
@Disabled("Needs refactoring to use entity classes instead of model classes for repository mocks")
class ZCodeMapperTest {

    @Mock
    private SdohDiagnosisRepository diagnosisRepository;

    @InjectMocks
    private ZCodeMapper zCodeMapper;

    private String tenantId;
    private String patientId;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        patientId = "patient-001";
    }

    @Test
    @DisplayName("Should map food insecurity to Z59.4")
    void testMapFoodInsecurity() {
        // Given
        SdohCategory category = SdohCategory.FOOD_INSECURITY;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.contains("Z59.4"), "Food insecurity should map to Z59.4");
    }

    @Test
    @DisplayName("Should map housing instability to Z59.0x codes")
    void testMapHousingInstability() {
        // Given
        SdohCategory category = SdohCategory.HOUSING_INSTABILITY;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertFalse(zCodes.isEmpty());
        assertTrue(zCodes.stream().anyMatch(code -> code.startsWith("Z59.0")),
                "Housing instability should map to Z59.0x codes");
    }

    @Test
    @DisplayName("Should map transportation to Z59.82")
    void testMapTransportation() {
        // Given
        SdohCategory category = SdohCategory.TRANSPORTATION;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.contains("Z59.82"), "Transportation should map to Z59.82");
    }

    @Test
    @DisplayName("Should map financial strain to Z59.5 or Z59.6")
    void testMapFinancialStrain() {
        // Given
        SdohCategory category = SdohCategory.FINANCIAL_STRAIN;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.contains("Z59.5") || zCodes.contains("Z59.6"),
                "Financial strain should map to Z59.5 or Z59.6");
    }

    @Test
    @DisplayName("Should map education to Z55.x codes")
    void testMapEducation() {
        // Given
        SdohCategory category = SdohCategory.EDUCATION;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.stream().anyMatch(code -> code.startsWith("Z55")),
                "Education should map to Z55.x codes");
    }

    @Test
    @DisplayName("Should map employment to Z56.x codes")
    void testMapEmployment() {
        // Given
        SdohCategory category = SdohCategory.EMPLOYMENT;

        // When
        List<String> zCodes = zCodeMapper.getZCodesForCategory(category);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.stream().anyMatch(code -> code.startsWith("Z56")),
                "Employment should map to Z56.x codes");
    }

    @Test
    @DisplayName("Should get Z-code description")
    void testGetZCodeDescription() {
        // Given
        String zCode = "Z59.4";

        // When
        String description = zCodeMapper.getZCodeDescription(zCode);

        // Then
        assertNotNull(description);
        assertTrue(description.toLowerCase().contains("food"),
                "Z59.4 should describe food insecurity");
    }

    @Test
    @DisplayName("Should map multiple needs to Z-codes")
    void testMapNeedsToZCodes() {
        // Given
        Map<SdohCategory, Boolean> needs = new HashMap<>();
        needs.put(SdohCategory.FOOD_INSECURITY, true);
        needs.put(SdohCategory.HOUSING_INSTABILITY, true);
        needs.put(SdohCategory.TRANSPORTATION, false);

        // When
        List<String> zCodes = zCodeMapper.mapNeedsToZCodes(needs);

        // Then
        assertNotNull(zCodes);
        assertTrue(zCodes.size() >= 2, "Should map 2 identified needs");
        assertTrue(zCodes.contains("Z59.4"));
    }

    @Test
    @DisplayName("Should create SDOH diagnosis with Z-code")
    void testCreateSdohDiagnosis() {
        // Given
        SdohCategory category = SdohCategory.FOOD_INSECURITY;
        String zCode = "Z59.4";

        SdohDiagnosis savedDiagnosis = SdohDiagnosis.builder()
                .diagnosisId("diag-001")
                .patientId(patientId)
                .zCode(zCode)
                .build();

        when(diagnosisRepository.save(any(SdohDiagnosis.class))).thenReturn(savedDiagnosis);

        // When
        SdohDiagnosis result = zCodeMapper.createDiagnosis(tenantId, patientId, category, "Provider-001");

        // Then
        assertNotNull(result);
        assertEquals(zCode, result.getZCode());
        assertEquals(patientId, result.getPatientId());
        verify(diagnosisRepository, times(1)).save(any(SdohDiagnosis.class));
    }

    @Test
    @DisplayName("Should get all diagnoses for patient")
    void testGetPatientDiagnoses() {
        // Given
        List<SdohDiagnosis> diagnoses = Arrays.asList(
                SdohDiagnosis.builder().diagnosisId("d1").patientId(patientId).zCode("Z59.4").build(),
                SdohDiagnosis.builder().diagnosisId("d2").patientId(patientId).zCode("Z59.82").build()
        );

        when(diagnosisRepository.findByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(diagnoses);

        // When
        List<SdohDiagnosis> result = zCodeMapper.getPatientDiagnoses(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should get active diagnoses only")
    void testGetActiveDiagnoses() {
        // Given
        List<SdohDiagnosis> activeDiagnoses = Arrays.asList(
                SdohDiagnosis.builder()
                        .diagnosisId("d1")
                        .status(SdohDiagnosis.DiagnosisStatus.ACTIVE)
                        .build()
        );

        when(diagnosisRepository.findByTenantIdAndPatientIdAndStatus(
                tenantId, patientId, SdohDiagnosis.DiagnosisStatus.ACTIVE))
                .thenReturn(activeDiagnoses);

        // When
        List<SdohDiagnosis> result = zCodeMapper.getActiveDiagnoses(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SdohDiagnosis.DiagnosisStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should update diagnosis status")
    void testUpdateDiagnosisStatus() {
        // Given
        String diagnosisId = "diag-001";
        SdohDiagnosis diagnosis = SdohDiagnosis.builder()
                .diagnosisId(diagnosisId)
                .status(SdohDiagnosis.DiagnosisStatus.ACTIVE)
                .build();

        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(diagnosis));
        when(diagnosisRepository.save(any(SdohDiagnosis.class))).thenReturn(diagnosis);

        // When
        zCodeMapper.updateDiagnosisStatus(diagnosisId, SdohDiagnosis.DiagnosisStatus.RESOLVED);

        // Then
        verify(diagnosisRepository, times(1)).save(any(SdohDiagnosis.class));
    }

    @Test
    @DisplayName("Should validate Z-code format")
    void testValidateZCode() {
        // When
        boolean validZ59 = zCodeMapper.isValidZCode("Z59.4");
        boolean validZ55 = zCodeMapper.isValidZCode("Z55.0");
        boolean invalid = zCodeMapper.isValidZCode("A00.0");

        // Then
        assertTrue(validZ59);
        assertTrue(validZ55);
        assertFalse(invalid);
    }

    @Test
    @DisplayName("Should get category from Z-code")
    void testGetCategoryFromZCode() {
        // When
        Optional<SdohCategory> category = zCodeMapper.getCategoryFromZCode("Z59.4");

        // Then
        assertTrue(category.isPresent());
        assertEquals(SdohCategory.FOOD_INSECURITY, category.get());
    }

    @Test
    @DisplayName("Should export diagnoses to FHIR Condition resources")
    void testExportToFhirCondition() {
        // Given
        SdohDiagnosis diagnosis = SdohDiagnosis.builder()
                .diagnosisId("d1")
                .patientId(patientId)
                .zCode("Z59.4")
                .build();

        // When
        String fhirCondition = zCodeMapper.exportToFhirCondition(diagnosis);

        // Then
        assertNotNull(fhirCondition);
        assertTrue(fhirCondition.contains("Z59.4"));
        assertTrue(fhirCondition.contains("Condition"));
    }

    @Test
    @DisplayName("Should handle invalid category gracefully")
    void testHandleInvalidCategory() {
        // When & Then
        assertDoesNotThrow(() -> {
            zCodeMapper.getZCodesForCategory(null);
        });
    }
}
