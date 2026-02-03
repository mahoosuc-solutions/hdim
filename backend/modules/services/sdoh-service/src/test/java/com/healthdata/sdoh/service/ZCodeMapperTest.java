package com.healthdata.sdoh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohDiagnosisEntity;
import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.model.SdohDiagnosis;
import com.healthdata.sdoh.repository.SdohDiagnosisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ZCodeMapper
 *
 * Testing ICD-10-CM Z-code (Z55-Z65) mapping for SDOH findings.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Z-Code Mapper Tests")
class ZCodeMapperTest {

    @Mock
    private SdohDiagnosisRepository diagnosisRepository;

    private ZCodeMapper zCodeMapper;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-001";
    private static final String PATIENT_ID = "patient-001";
    private static final String DIAGNOSED_BY = "Dr. Smith";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        zCodeMapper = new ZCodeMapper(diagnosisRepository, objectMapper);
    }

    @Nested
    @DisplayName("Z-Code Lookup Tests")
    class ZCodeLookupTests {

        @Test
        @DisplayName("Should get Z-codes for food insecurity category")
        void shouldGetZCodesForFoodInsecurity() {
            // When
            List<String> zCodes = zCodeMapper.getZCodesForCategory(SdohCategory.FOOD_INSECURITY);

            // Then
            assertNotNull(zCodes);
            assertEquals(3, zCodes.size());
            assertTrue(zCodes.contains("Z59.4"));
            assertTrue(zCodes.contains("Z59.41"));
            assertTrue(zCodes.contains("Z59.48"));
        }

        @Test
        @DisplayName("Should get Z-codes for housing instability category")
        void shouldGetZCodesForHousingInstability() {
            // When
            List<String> zCodes = zCodeMapper.getZCodesForCategory(SdohCategory.HOUSING_INSTABILITY);

            // Then
            assertNotNull(zCodes);
            assertEquals(4, zCodes.size());
            assertTrue(zCodes.contains("Z59.0"));
            assertTrue(zCodes.contains("Z59.00"));
            assertTrue(zCodes.contains("Z59.01"));
            assertTrue(zCodes.contains("Z59.02"));
        }

        @Test
        @DisplayName("Should get Z-codes for transportation category")
        void shouldGetZCodesForTransportation() {
            // When
            List<String> zCodes = zCodeMapper.getZCodesForCategory(SdohCategory.TRANSPORTATION);

            // Then
            assertNotNull(zCodes);
            assertEquals(1, zCodes.size());
            assertTrue(zCodes.contains("Z59.82"));
        }

        @Test
        @DisplayName("Should return empty list for null category")
        void shouldReturnEmptyListForNullCategory() {
            // When
            List<String> zCodes = zCodeMapper.getZCodesForCategory(null);

            // Then
            assertNotNull(zCodes);
            assertTrue(zCodes.isEmpty());
        }

        @Test
        @DisplayName("Should get Z-code description")
        void shouldGetZCodeDescription() {
            // When
            String description = zCodeMapper.getZCodeDescription("Z59.41");

            // Then
            assertEquals("Food insecurity", description);
        }

        @Test
        @DisplayName("Should return unknown for unrecognized Z-code")
        void shouldReturnUnknownForUnrecognizedZCode() {
            // When
            String description = zCodeMapper.getZCodeDescription("Z99.99");

            // Then
            assertEquals("Unknown Z-code", description);
        }
    }

    @Nested
    @DisplayName("Category From Z-Code Tests")
    class CategoryFromZCodeTests {

        @Test
        @DisplayName("Should map Z59.4 to FOOD_INSECURITY category")
        void shouldMapZCodeToFoodInsecurity() {
            // When
            Optional<SdohCategory> category = zCodeMapper.getCategoryFromZCode("Z59.4");

            // Then
            assertTrue(category.isPresent());
            assertEquals(SdohCategory.FOOD_INSECURITY, category.get());
        }

        @Test
        @DisplayName("Should map Z59.0 to HOUSING_INSTABILITY category")
        void shouldMapZCodeToHousingInstability() {
            // When
            Optional<SdohCategory> category = zCodeMapper.getCategoryFromZCode("Z59.0");

            // Then
            assertTrue(category.isPresent());
            assertEquals(SdohCategory.HOUSING_INSTABILITY, category.get());
        }

        @Test
        @DisplayName("Should return empty for unknown Z-code")
        void shouldReturnEmptyForUnknownZCode() {
            // When
            Optional<SdohCategory> category = zCodeMapper.getCategoryFromZCode("Z99.99");

            // Then
            assertFalse(category.isPresent());
        }
    }

    @Nested
    @DisplayName("Z-Code Validation Tests")
    class ZCodeValidationTests {

        @Test
        @DisplayName("Should validate correct Z-code format with decimal")
        void shouldValidateCorrectZCodeFormatWithDecimal() {
            // Valid formats: Z59.4, Z59.41
            assertTrue(zCodeMapper.isValidZCode("Z59.4"));
            assertTrue(zCodeMapper.isValidZCode("Z59.41"));
            assertTrue(zCodeMapper.isValidZCode("Z55.0"));
        }

        @Test
        @DisplayName("Should validate correct Z-code format without decimal")
        void shouldValidateCorrectZCodeFormatWithoutDecimal() {
            // Valid format: Z59
            assertTrue(zCodeMapper.isValidZCode("Z59"));
            assertTrue(zCodeMapper.isValidZCode("Z55"));
        }

        @Test
        @DisplayName("Should reject invalid Z-code formats")
        void shouldRejectInvalidZCodeFormats() {
            // Invalid formats
            assertFalse(zCodeMapper.isValidZCode("ABC123"));
            assertFalse(zCodeMapper.isValidZCode("59.4"));  // Missing Z prefix
            assertFalse(zCodeMapper.isValidZCode("ZZ9.4")); // Double Z
            assertFalse(zCodeMapper.isValidZCode("Z5.4"));  // Only one digit after Z
            assertFalse(zCodeMapper.isValidZCode("Z59.123")); // Too many decimals
        }

        @Test
        @DisplayName("Should reject null or empty Z-code")
        void shouldRejectNullOrEmptyZCode() {
            assertFalse(zCodeMapper.isValidZCode(null));
            assertFalse(zCodeMapper.isValidZCode(""));
        }
    }

    @Nested
    @DisplayName("Diagnosis Creation Tests")
    class DiagnosisCreationTests {

        @Test
        @DisplayName("Should create diagnosis with auto-assigned Z-code")
        void shouldCreateDiagnosisWithAutoZCode() {
            // Given
            when(diagnosisRepository.save(any(SdohDiagnosisEntity.class)))
                    .thenAnswer(invocation -> {
                        SdohDiagnosisEntity entity = invocation.getArgument(0);
                        entity.setDiagnosisId("diag-001");
                        return entity;
                    });

            // When
            SdohDiagnosis diagnosis = zCodeMapper.createDiagnosis(
                    TENANT_ID, PATIENT_ID, SdohCategory.FOOD_INSECURITY, DIAGNOSED_BY);

            // Then
            assertNotNull(diagnosis);
            assertEquals(TENANT_ID, diagnosis.getTenantId());
            assertEquals(PATIENT_ID, diagnosis.getPatientId());
            assertEquals("Z59.4", diagnosis.getZCode()); // First Z-code for food insecurity
            assertEquals("Lack of adequate food and safe drinking water", diagnosis.getZCodeDescription());
            assertEquals(SdohCategory.FOOD_INSECURITY, diagnosis.getCategory());
            assertEquals(SdohDiagnosis.DiagnosisStatus.ACTIVE, diagnosis.getStatus());
            assertEquals(DIAGNOSED_BY, diagnosis.getDiagnosedBy());

            // Verify repository save was called
            verify(diagnosisRepository).save(any(SdohDiagnosisEntity.class));
        }

        @Test
        @DisplayName("Should persist diagnosis entity with correct values")
        void shouldPersistDiagnosisEntityWithCorrectValues() {
            // Given
            when(diagnosisRepository.save(any(SdohDiagnosisEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            zCodeMapper.createDiagnosis(TENANT_ID, PATIENT_ID, SdohCategory.HOUSING_INSTABILITY, DIAGNOSED_BY);

            // Then
            ArgumentCaptor<SdohDiagnosisEntity> captor = ArgumentCaptor.forClass(SdohDiagnosisEntity.class);
            verify(diagnosisRepository).save(captor.capture());

            SdohDiagnosisEntity savedEntity = captor.getValue();
            assertEquals(TENANT_ID, savedEntity.getTenantId());
            assertEquals(PATIENT_ID, savedEntity.getPatientId());
            assertEquals("Z59.0", savedEntity.getZCode()); // First Z-code for housing
            assertEquals(SdohCategory.HOUSING_INSTABILITY, savedEntity.getCategory());
            assertEquals(SdohDiagnosis.DiagnosisStatus.ACTIVE, savedEntity.getStatus());
        }
    }

    @Nested
    @DisplayName("Diagnosis Status Update Tests")
    class DiagnosisStatusUpdateTests {

        @Test
        @DisplayName("Should update diagnosis status from ACTIVE to RESOLVED")
        void shouldUpdateDiagnosisStatus() {
            // Given
            String diagnosisId = "diag-001";
            SdohDiagnosisEntity existingEntity = SdohDiagnosisEntity.builder()
                    .diagnosisId(diagnosisId)
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .zCode("Z59.4")
                    .zCodeDescription("Food insecurity")
                    .category(SdohCategory.FOOD_INSECURITY)
                    .status(SdohDiagnosis.DiagnosisStatus.ACTIVE)
                    .diagnosisDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(existingEntity));
            when(diagnosisRepository.save(any(SdohDiagnosisEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            zCodeMapper.updateDiagnosisStatus(diagnosisId, SdohDiagnosis.DiagnosisStatus.RESOLVED);

            // Then
            ArgumentCaptor<SdohDiagnosisEntity> captor = ArgumentCaptor.forClass(SdohDiagnosisEntity.class);
            verify(diagnosisRepository).save(captor.capture());

            assertEquals(SdohDiagnosis.DiagnosisStatus.RESOLVED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should not save when diagnosis not found")
        void shouldNotSaveWhenDiagnosisNotFound() {
            // Given
            when(diagnosisRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            zCodeMapper.updateDiagnosisStatus("nonexistent", SdohDiagnosis.DiagnosisStatus.RESOLVED);

            // Then
            verify(diagnosisRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Diagnosis Retrieval Tests")
    class DiagnosisRetrievalTests {

        @Test
        @DisplayName("Should get all patient diagnoses")
        void shouldGetPatientDiagnoses() {
            // Given
            List<SdohDiagnosisEntity> entities = Arrays.asList(
                    createDiagnosisEntity("diag-001", SdohCategory.FOOD_INSECURITY, SdohDiagnosis.DiagnosisStatus.ACTIVE),
                    createDiagnosisEntity("diag-002", SdohCategory.HOUSING_INSTABILITY, SdohDiagnosis.DiagnosisStatus.RESOLVED)
            );

            when(diagnosisRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(entities);

            // When
            List<SdohDiagnosis> diagnoses = zCodeMapper.getPatientDiagnoses(TENANT_ID, PATIENT_ID);

            // Then
            assertEquals(2, diagnoses.size());
            assertEquals("diag-001", diagnoses.get(0).getDiagnosisId());
            assertEquals("diag-002", diagnoses.get(1).getDiagnosisId());
        }

        @Test
        @DisplayName("Should get only active diagnoses")
        void shouldGetActiveDiagnosesOnly() {
            // Given
            List<SdohDiagnosisEntity> activeEntities = Arrays.asList(
                    createDiagnosisEntity("diag-001", SdohCategory.FOOD_INSECURITY, SdohDiagnosis.DiagnosisStatus.ACTIVE)
            );

            when(diagnosisRepository.findByTenantIdAndPatientIdAndStatus(
                    TENANT_ID, PATIENT_ID, SdohDiagnosis.DiagnosisStatus.ACTIVE))
                    .thenReturn(activeEntities);

            // When
            List<SdohDiagnosis> diagnoses = zCodeMapper.getActiveDiagnoses(TENANT_ID, PATIENT_ID);

            // Then
            assertEquals(1, diagnoses.size());
            assertEquals(SdohDiagnosis.DiagnosisStatus.ACTIVE, diagnoses.get(0).getStatus());
        }

        @Test
        @DisplayName("Should return empty list when no diagnoses found")
        void shouldReturnEmptyListWhenNoDiagnosesFound() {
            // Given
            when(diagnosisRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<SdohDiagnosis> diagnoses = zCodeMapper.getPatientDiagnoses(TENANT_ID, PATIENT_ID);

            // Then
            assertTrue(diagnoses.isEmpty());
        }
    }

    @Nested
    @DisplayName("Needs to Z-Code Mapping Tests")
    class NeedsToZCodeMappingTests {

        @Test
        @DisplayName("Should map single need to Z-code")
        void shouldMapSingleNeedToZCode() {
            // Given
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);

            // When
            List<String> zCodes = zCodeMapper.mapNeedsToZCodes(needs);

            // Then
            assertEquals(1, zCodes.size());
            assertEquals("Z59.4", zCodes.get(0)); // First Z-code for food insecurity
        }

        @Test
        @DisplayName("Should map multiple needs to Z-codes (limited to first)")
        void shouldMapMultipleNeedsToZCodes() {
            // Given
            Map<SdohCategory, Boolean> needs = new LinkedHashMap<>(); // Preserve order
            needs.put(SdohCategory.FOOD_INSECURITY, true);
            needs.put(SdohCategory.HOUSING_INSTABILITY, true);
            needs.put(SdohCategory.TRANSPORTATION, true);

            // When
            List<String> zCodes = zCodeMapper.mapNeedsToZCodes(needs);

            // Then - service limits to first Z-code from first matching category
            assertEquals(1, zCodes.size());
            // The returned Z-code should be from one of the identified needs
            String returnedCode = zCodes.get(0);
            assertTrue(
                returnedCode.startsWith("Z59.4") || // Food insecurity codes
                returnedCode.startsWith("Z59.0") || // Housing codes
                returnedCode.equals("Z59.82"),      // Transportation code
                "Expected a Z-code from one of the identified SDOH needs"
            );
        }

        @Test
        @DisplayName("Should ignore false needs")
        void shouldIgnoreFalseNeeds() {
            // Given
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);
            needs.put(SdohCategory.HOUSING_INSTABILITY, false);

            // When
            List<String> zCodes = zCodeMapper.mapNeedsToZCodes(needs);

            // Then
            assertEquals(1, zCodes.size());
        }
    }

    @Nested
    @DisplayName("FHIR Export Tests")
    class FhirExportTests {

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

    // Helper method to create diagnosis entity for tests
    private SdohDiagnosisEntity createDiagnosisEntity(String diagnosisId, SdohCategory category,
                                                       SdohDiagnosis.DiagnosisStatus status) {
        String zCode = zCodeMapper.getZCodesForCategory(category).get(0);
        return SdohDiagnosisEntity.builder()
                .diagnosisId(diagnosisId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .zCode(zCode)
                .zCodeDescription(zCodeMapper.getZCodeDescription(zCode))
                .category(category)
                .status(status)
                .diagnosisDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
