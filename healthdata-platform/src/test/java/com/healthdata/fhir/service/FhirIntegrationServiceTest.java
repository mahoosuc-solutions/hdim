package com.healthdata.fhir.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.MedicationRequestRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for FhirIntegrationService
 * 35+ test methods covering all service functionality
 * Spring Boot 3.3.5 compatible with zero compilation errors
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FHIR Integration Service Tests")
class FhirIntegrationServiceTest {

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private MedicationRequestRepository medicationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FhirIntegrationService fhirIntegrationService;

    private String patientId;
    private String tenantId;
    private Map<String, Object> observationMap;
    private Map<String, Object> conditionMap;
    private Map<String, Object> medicationMap;

    @BeforeEach
    void setUp() {
        patientId = "patient-123";
        tenantId = "tenant-456";

        // Create sample observation map
        observationMap = createSampleObservationMap();

        // Create sample condition map
        conditionMap = createSampleConditionMap();

        // Create sample medication map
        medicationMap = createSampleMedicationMap();
    }

    // ==================== OBSERVATION IMPORT TESTS ====================

    @Nested
    @DisplayName("Import Observations")
    class ImportObservationsTests {

        @Test
        @DisplayName("Should import single observation successfully")
        void shouldImportSingleObservation() {
            // Arrange
            List<Map<String, Object>> observations = List.of(observationMap);
            Observation savedObs = createSampleObservation(patientId, tenantId);
            when(observationRepository.save(any(Observation.class))).thenReturn(savedObs);

            // Act
            List<Observation> result = fhirIntegrationService.importObservations(patientId, observations, tenantId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
            assertThat(result.get(0).getTenantId()).isEqualTo(tenantId);
            verify(observationRepository).save(any(Observation.class));
        }

        @Test
        @DisplayName("Should import multiple observations")
        void shouldImportMultipleObservations() {
            // Arrange
            List<Map<String, Object>> observations = List.of(
                observationMap,
                createSampleObservationMap(),
                createSampleObservationMap()
            );
            Observation savedObs = createSampleObservation(patientId, tenantId);
            when(observationRepository.save(any(Observation.class))).thenReturn(savedObs);

            // Act
            List<Observation> result = fhirIntegrationService.importObservations(patientId, observations, tenantId);

            // Assert
            assertThat(result).hasSize(3);
            verify(observationRepository, times(3)).save(any(Observation.class));
        }

        @Test
        @DisplayName("Should skip invalid observations")
        void shouldSkipInvalidObservations() {
            // Arrange
            Map<String, Object> invalidObs = new HashMap<>();
            invalidObs.put("resourceType", "Observation");
            // Missing required 'code' and 'status'

            List<Map<String, Object>> observations = List.of(invalidObs);

            // Act
            List<Observation> result = fhirIntegrationService.importObservations(patientId, observations, tenantId);

            // Assert
            assertThat(result).isEmpty();
            verify(observationRepository, never()).save(any(Observation.class));
        }

        @Test
        @DisplayName("Should handle empty observation list")
        void shouldHandleEmptyObservationList() {
            // Act
            List<Observation> result = fhirIntegrationService.importObservations(patientId, List.of(), tenantId);

            // Assert
            assertThat(result).isEmpty();
            verify(observationRepository, never()).save(any(Observation.class));
        }

        @Test
        @DisplayName("Should extract and set observation fields correctly")
        void shouldExtractObservationFieldsCorrectly() {
            // Arrange
            List<Map<String, Object>> observations = List.of(observationMap);
            ArgumentCaptor<Observation> captor = ArgumentCaptor.forClass(Observation.class);
            Observation savedObs = createSampleObservation(patientId, tenantId);
            when(observationRepository.save(any(Observation.class))).thenReturn(savedObs);

            // Act
            fhirIntegrationService.importObservations(patientId, observations, tenantId);

            // Assert
            verify(observationRepository).save(captor.capture());
            Observation capturedObs = captor.getValue();
            assertThat(capturedObs.getPatientId()).isEqualTo(patientId);
            assertThat(capturedObs.getTenantId()).isEqualTo(tenantId);
            assertThat(capturedObs.getStatus()).isEqualTo("final");
            assertThat(capturedObs.getCode()).isEqualTo("2345-7");
        }
    }

    // ==================== CONDITION IMPORT TESTS ====================

    @Nested
    @DisplayName("Import Conditions")
    class ImportConditionsTests {

        @Test
        @DisplayName("Should import single condition successfully")
        void shouldImportSingleCondition() {
            // Arrange
            List<Map<String, Object>> conditions = List.of(conditionMap);
            Condition savedCond = createSampleCondition(patientId, tenantId);
            when(conditionRepository.save(any(Condition.class))).thenReturn(savedCond);

            // Act
            List<Condition> result = fhirIntegrationService.importConditions(patientId, conditions, tenantId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
            assertThat(result.get(0).getTenantId()).isEqualTo(tenantId);
            verify(conditionRepository).save(any(Condition.class));
        }

        @Test
        @DisplayName("Should import multiple conditions")
        void shouldImportMultipleConditions() {
            // Arrange
            List<Map<String, Object>> conditions = List.of(
                conditionMap,
                createSampleConditionMap(),
                createSampleConditionMap()
            );
            Condition savedCond = createSampleCondition(patientId, tenantId);
            when(conditionRepository.save(any(Condition.class))).thenReturn(savedCond);

            // Act
            List<Condition> result = fhirIntegrationService.importConditions(patientId, conditions, tenantId);

            // Assert
            assertThat(result).hasSize(3);
            verify(conditionRepository, times(3)).save(any(Condition.class));
        }

        @Test
        @DisplayName("Should skip invalid conditions")
        void shouldSkipInvalidConditions() {
            // Arrange
            Map<String, Object> invalidCond = new HashMap<>();
            invalidCond.put("resourceType", "Condition");
            // Missing required fields

            List<Map<String, Object>> conditions = List.of(invalidCond);

            // Act
            List<Condition> result = fhirIntegrationService.importConditions(patientId, conditions, tenantId);

            // Assert
            assertThat(result).isEmpty();
            verify(conditionRepository, never()).save(any(Condition.class));
        }

        @Test
        @DisplayName("Should extract condition fields correctly")
        void shouldExtractConditionFieldsCorrectly() {
            // Arrange
            List<Map<String, Object>> conditions = List.of(conditionMap);
            ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
            Condition savedCond = createSampleCondition(patientId, tenantId);
            when(conditionRepository.save(any(Condition.class))).thenReturn(savedCond);

            // Act
            fhirIntegrationService.importConditions(patientId, conditions, tenantId);

            // Assert
            verify(conditionRepository).save(captor.capture());
            Condition capturedCond = captor.getValue();
            assertThat(capturedCond.getPatientId()).isEqualTo(patientId);
            assertThat(capturedCond.getCode()).isEqualTo("44054006");
            assertThat(capturedCond.getClinicalStatus()).isEqualTo("active");
        }
    }

    // ==================== MEDICATION IMPORT TESTS ====================

    @Nested
    @DisplayName("Import Medications")
    class ImportMedicationsTests {

        @Test
        @DisplayName("Should import single medication successfully")
        void shouldImportSingleMedication() {
            // Arrange
            List<Map<String, Object>> medications = List.of(medicationMap);
            MedicationRequest savedMed = createSampleMedication(patientId, tenantId);
            when(medicationRepository.save(any(MedicationRequest.class))).thenReturn(savedMed);

            // Act
            List<MedicationRequest> result = fhirIntegrationService.importMedications(patientId, medications, tenantId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientId()).isEqualTo(patientId);
            assertThat(result.get(0).getTenantId()).isEqualTo(tenantId);
            verify(medicationRepository).save(any(MedicationRequest.class));
        }

        @Test
        @DisplayName("Should import multiple medications")
        void shouldImportMultipleMedications() {
            // Arrange
            List<Map<String, Object>> medications = List.of(
                medicationMap,
                createSampleMedicationMap(),
                createSampleMedicationMap()
            );
            MedicationRequest savedMed = createSampleMedication(patientId, tenantId);
            when(medicationRepository.save(any(MedicationRequest.class))).thenReturn(savedMed);

            // Act
            List<MedicationRequest> result = fhirIntegrationService.importMedications(patientId, medications, tenantId);

            // Assert
            assertThat(result).hasSize(3);
            verify(medicationRepository, times(3)).save(any(MedicationRequest.class));
        }

        @Test
        @DisplayName("Should skip invalid medications")
        void shouldSkipInvalidMedications() {
            // Arrange
            Map<String, Object> invalidMed = new HashMap<>();
            invalidMed.put("resourceType", "MedicationRequest");
            // Missing required fields

            List<Map<String, Object>> medications = List.of(invalidMed);

            // Act
            List<MedicationRequest> result = fhirIntegrationService.importMedications(patientId, medications, tenantId);

            // Assert
            assertThat(result).isEmpty();
            verify(medicationRepository, never()).save(any(MedicationRequest.class));
        }
    }

    // ==================== EXPORT TESTS ====================

    @Nested
    @DisplayName("Export Patient Resources")
    class ExportPatientResourcesTests {

        @Test
        @DisplayName("Should export all patient resources as FHIR bundle")
        void shouldExportPatientResources() {
            // Arrange
            List<Observation> observations = List.of(createSampleObservation(patientId, tenantId));
            List<Condition> conditions = List.of(createSampleCondition(patientId, tenantId));
            List<MedicationRequest> medications = List.of(createSampleMedication(patientId, tenantId));

            when(observationRepository.findByPatientId(patientId)).thenReturn(observations);
            when(conditionRepository.findByPatientId(patientId)).thenReturn(conditions);
            when(medicationRepository.findByPatientId(patientId)).thenReturn(medications);

            // Act
            FhirIntegrationService.FhirBundle result = fhirIntegrationService.exportPatientResources(patientId, tenantId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getResourceType()).isEqualTo("Bundle");
            assertThat(result.getType()).isEqualTo("searchset");
            assertThat(result.getTotal()).isEqualTo(3);
            assertThat(result.getEntries()).hasSize(3);
        }

        @Test
        @DisplayName("Should handle export with no patient resources")
        void shouldHandleExportWithNoResources() {
            // Arrange
            when(observationRepository.findByPatientId(patientId)).thenReturn(List.of());
            when(conditionRepository.findByPatientId(patientId)).thenReturn(List.of());
            when(medicationRepository.findByPatientId(patientId)).thenReturn(List.of());

            // Act
            FhirIntegrationService.FhirBundle result = fhirIntegrationService.exportPatientResources(patientId, tenantId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getEntries()).isEmpty();
        }

        @Test
        @DisplayName("Should include correct resource types in bundle")
        void shouldIncludeCorrectResourceTypesInBundle() {
            // Arrange
            List<Observation> observations = List.of(createSampleObservation(patientId, tenantId));
            List<Condition> conditions = List.of(createSampleCondition(patientId, tenantId));
            List<MedicationRequest> medications = List.of(createSampleMedication(patientId, tenantId));

            when(observationRepository.findByPatientId(patientId)).thenReturn(observations);
            when(conditionRepository.findByPatientId(patientId)).thenReturn(conditions);
            when(medicationRepository.findByPatientId(patientId)).thenReturn(medications);

            // Act
            FhirIntegrationService.FhirBundle result = fhirIntegrationService.exportPatientResources(patientId, tenantId);

            // Assert
            assertThat(result.getEntries()).hasSize(3);
            boolean hasObservation = result.getEntries().stream()
                .anyMatch(e -> "Observation".equals(e.get("resourceType")));
            boolean hasCondition = result.getEntries().stream()
                .anyMatch(e -> "Condition".equals(e.get("resourceType")));
            boolean hasMedication = result.getEntries().stream()
                .anyMatch(e -> "MedicationRequest".equals(e.get("resourceType")));

            assertThat(hasObservation).isTrue();
            assertThat(hasCondition).isTrue();
            assertThat(hasMedication).isTrue();
        }
    }

    // ==================== BATCH IMPORT TESTS ====================

    @Nested
    @DisplayName("Batch Import Bundle")
    class BatchImportBundleTests {

        @Test
        @DisplayName("Should batch import FHIR bundle successfully")
        void shouldBatchImportBundle() {
            // Arrange
            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("Bundle")
                .type("batch")
                .total(3)
                .entries(List.of(observationMap, conditionMap, medicationMap))
                .timestamp(LocalDateTime.now())
                .build();

            when(observationRepository.save(any(Observation.class))).thenReturn(createSampleObservation(patientId, tenantId));
            when(conditionRepository.save(any(Condition.class))).thenReturn(createSampleCondition(patientId, tenantId));
            when(medicationRepository.save(any(MedicationRequest.class))).thenReturn(createSampleMedication(patientId, tenantId));

            // Act
            FhirIntegrationService.BundleResult result = fhirIntegrationService.batchImport(tenantId, bundle);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getTotalEntries()).isEqualTo(3);
            assertThat(result.getSuccessCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should track import statistics")
        void shouldTrackImportStatistics() {
            // Arrange
            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("Bundle")
                .type("batch")
                .total(1)
                .entries(List.of(observationMap))
                .timestamp(LocalDateTime.now())
                .build();

            when(observationRepository.save(any(Observation.class))).thenReturn(createSampleObservation(patientId, tenantId));

            // Act
            FhirIntegrationService.BundleResult result = fhirIntegrationService.batchImport(tenantId, bundle);

            // Assert
            assertThat(result.getObservationsImported()).isGreaterThan(0);
            assertThat(result.getImportedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle batch with mixed valid and invalid entries")
        void shouldHandleMixedValidAndInvalidEntries() {
            // Arrange
            Map<String, Object> invalidObs = new HashMap<>();
            invalidObs.put("resourceType", "Observation");
            // Missing required fields

            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("Bundle")
                .type("batch")
                .total(2)
                .entries(List.of(observationMap, invalidObs))
                .timestamp(LocalDateTime.now())
                .build();

            when(observationRepository.save(any(Observation.class))).thenReturn(createSampleObservation(patientId, tenantId));

            // Act
            FhirIntegrationService.BundleResult result = fhirIntegrationService.batchImport(tenantId, bundle);

            // Assert
            assertThat(result.getSuccessCount()).isGreaterThanOrEqualTo(1);
            assertThat(result.getErrorCount()).isGreaterThanOrEqualTo(1);
        }
    }

    // ==================== VALIDATION TESTS ====================

    @Nested
    @DisplayName("Validate Observation")
    class ValidateObservationTests {

        @Test
        @DisplayName("Should validate correct observation")
        void shouldValidateCorrectObservation() {
            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateObservation(observationMap);

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject observation with wrong resource type")
        void shouldRejectWrongResourceType() {
            // Arrange
            Map<String, Object> invalidObs = new HashMap<>(observationMap);
            invalidObs.put("resourceType", "Patient");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateObservation(invalidObs);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject observation without code")
        void shouldRejectObservationWithoutCode() {
            // Arrange
            Map<String, Object> invalidObs = new HashMap<>(observationMap);
            invalidObs.remove("code");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateObservation(invalidObs);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject observation without status")
        void shouldRejectObservationWithoutStatus() {
            // Arrange
            Map<String, Object> invalidObs = new HashMap<>(observationMap);
            invalidObs.remove("status");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateObservation(invalidObs);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validate Condition")
    class ValidateConditionTests {

        @Test
        @DisplayName("Should validate correct condition")
        void shouldValidateCorrectCondition() {
            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateCondition(conditionMap);

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject condition without code")
        void shouldRejectConditionWithoutCode() {
            // Arrange
            Map<String, Object> invalidCond = new HashMap<>(conditionMap);
            invalidCond.remove("code");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateCondition(invalidCond);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject condition without subject")
        void shouldRejectConditionWithoutSubject() {
            // Arrange
            Map<String, Object> invalidCond = new HashMap<>(conditionMap);
            invalidCond.remove("subject");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateCondition(invalidCond);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject condition with invalid clinical status")
        void shouldRejectInvalidClinicalStatus() {
            // Arrange
            Map<String, Object> invalidCond = new HashMap<>(conditionMap);
            invalidCond.put("clinicalStatus", "invalid-status");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateCondition(invalidCond);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validate Medication")
    class ValidateMedicationTests {

        @Test
        @DisplayName("Should validate correct medication")
        void shouldValidateCorrectMedication() {
            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateMedication(medicationMap);

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject medication without status")
        void shouldRejectMedicationWithoutStatus() {
            // Arrange
            Map<String, Object> invalidMed = new HashMap<>(medicationMap);
            invalidMed.remove("status");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateMedication(invalidMed);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject medication with invalid status")
        void shouldRejectInvalidStatus() {
            // Arrange
            Map<String, Object> invalidMed = new HashMap<>(medicationMap);
            invalidMed.put("status", "invalid-status");

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateMedication(invalidMed);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validate Bundle")
    class ValidateBundleTests {

        @Test
        @DisplayName("Should validate correct bundle")
        void shouldValidateCorrectBundle() {
            // Arrange
            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("Bundle")
                .type("batch")
                .total(1)
                .entries(List.of(observationMap))
                .timestamp(LocalDateTime.now())
                .build();

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateBundle(bundle);

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject bundle with wrong resource type")
        void shouldRejectBundleWithWrongResourceType() {
            // Arrange
            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("NotABundle")
                .type("batch")
                .total(1)
                .entries(List.of(observationMap))
                .timestamp(LocalDateTime.now())
                .build();

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateBundle(bundle);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject empty bundle")
        void shouldRejectEmptyBundle() {
            // Arrange
            FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
                .resourceType("Bundle")
                .type("batch")
                .total(0)
                .entries(List.of())
                .timestamp(LocalDateTime.now())
                .build();

            // Act
            FhirIntegrationService.ValidationResult result = fhirIntegrationService.validateBundle(bundle);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }
    }

    // ==================== CODE SYSTEM MAPPING TESTS ====================

    @Nested
    @DisplayName("Code System Mappings")
    class CodeSystemMappingTests {

        @Test
        @DisplayName("Should map LOINC blood pressure code")
        void shouldMapLoincBloodPressure() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapLoincCode("85354-9");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("85354-9");
            assertThat(result.getSystem()).isEqualTo(FhirIntegrationService.LOINC_SYSTEM);
            assertThat(result.getDisplay()).isNotBlank();
            assertThat(result.getCategory()).isEqualTo("vital-signs");
        }

        @Test
        @DisplayName("Should map LOINC glucose code")
        void shouldMapLoincGlucose() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapLoincCode("2345-7");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("2345-7");
            assertThat(result.getCategory()).isEqualTo("laboratory");
        }

        @Test
        @DisplayName("Should handle unknown LOINC code")
        void shouldHandleUnknownLoincCode() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapLoincCode("999999");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("999999");
            assertThat(result.getSystem()).isEqualTo(FhirIntegrationService.LOINC_SYSTEM);
        }

        @Test
        @DisplayName("Should map SNOMED diabetes code")
        void shouldMapSnomedDiabetes() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapSnomedCode("44054006");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("44054006");
            assertThat(result.getSystem()).isEqualTo(FhirIntegrationService.SNOMED_SYSTEM);
            assertThat(result.getDisplay()).isNotBlank();
            assertThat(result.getCategory()).isEqualTo("condition");
        }

        @Test
        @DisplayName("Should map SNOMED hypertension code")
        void shouldMapSnomedHypertension() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapSnomedCode("38341003");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("38341003");
        }

        @Test
        @DisplayName("Should map RxNorm medication code")
        void shouldMapRxNormMedication() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapRxNormCode("849574");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("849574");
            assertThat(result.getSystem()).isEqualTo(FhirIntegrationService.RXNORM_SYSTEM);
            assertThat(result.getDisplay()).isNotBlank();
        }

        @Test
        @DisplayName("Should map custom code system")
        void shouldMapCustomCodeSystem() {
            // Act
            FhirIntegrationService.CodeSystemMapping result = fhirIntegrationService.mapCustomCodeSystem(
                "http://custom.system.org",
                "custom-code"
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("custom-code");
            assertThat(result.getSystem()).isEqualTo("http://custom.system.org");
            assertThat(result.getCategory()).isEqualTo("custom");
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> createSampleObservationMap() {
        Map<String, Object> obs = new HashMap<>();
        obs.put("resourceType", "Observation");
        obs.put("id", "obs-123");
        obs.put("status", "final");

        Map<String, Object> code = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", "http://loinc.org");
        coding.put("code", "2345-7");
        coding.put("display", "Glucose");
        codings.add(coding);
        code.put("coding", codings);
        obs.put("code", code);

        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + patientId);
        obs.put("subject", subject);

        obs.put("effectiveDateTime", "2024-01-15T10:30:00Z");

        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        List<Map<String, Object>> categoryCodes = new ArrayList<>();
        Map<String, Object> categoryCode = new HashMap<>();
        categoryCode.put("code", "laboratory");
        categoryCodes.add(categoryCode);
        category.put("coding", categoryCodes);
        categories.add(category);
        obs.put("category", categories);

        Map<String, Object> valueQuantity = new HashMap<>();
        valueQuantity.put("value", 100);
        valueQuantity.put("unit", "mg/dL");
        obs.put("valueQuantity", valueQuantity);

        return obs;
    }

    private Map<String, Object> createSampleConditionMap() {
        Map<String, Object> cond = new HashMap<>();
        cond.put("resourceType", "Condition");
        cond.put("id", "cond-123");
        cond.put("clinicalStatus", "active");
        cond.put("verificationStatus", "confirmed");

        Map<String, Object> code = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", "http://snomed.info/sct");
        coding.put("code", "44054006");
        coding.put("display", "Diabetes mellitus");
        codings.add(coding);
        code.put("coding", codings);
        cond.put("code", code);

        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + patientId);
        cond.put("subject", subject);

        cond.put("onsetDateTime", "2020-01-15T10:30:00Z");
        cond.put("recordedDate", "2024-01-15T10:30:00Z");

        return cond;
    }

    private Map<String, Object> createSampleMedicationMap() {
        Map<String, Object> med = new HashMap<>();
        med.put("resourceType", "MedicationRequest");
        med.put("id", "med-123");
        med.put("status", "active");
        med.put("intent", "order");

        Map<String, Object> medication = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", "http://www.nlm.nih.gov/research/umls/rxnorm");
        coding.put("code", "849574");
        coding.put("display", "Lisinopril");
        codings.add(coding);
        medication.put("coding", codings);
        med.put("medicationCodeableConcept", medication);

        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + patientId);
        med.put("subject", subject);

        med.put("authoredOn", "2024-01-15T10:30:00Z");

        return med;
    }

    private Observation createSampleObservation(String patientId, String tenantId) {
        return Observation.builder()
            .id("obs-123")
            .patientId(patientId)
            .tenantId(tenantId)
            .code("2345-7")
            .system("http://loinc.org")
            .display("Glucose")
            .status("final")
            .valueQuantity(new BigDecimal("100"))
            .valueUnit("mg/dL")
            .category("laboratory")
            .effectiveDate(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Condition createSampleCondition(String patientId, String tenantId) {
        return Condition.builder()
            .id("cond-123")
            .patientId(patientId)
            .tenantId(tenantId)
            .code("44054006")
            .display("Diabetes mellitus")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .category("problem-list-item")
            .onsetDate(LocalDateTime.now().minusYears(2))
            .recordedDate(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private MedicationRequest createSampleMedication(String patientId, String tenantId) {
        return MedicationRequest.builder()
            .id("med-123")
            .patientId(patientId)
            .tenantId(tenantId)
            .medicationCode("849574")
            .medicationDisplay("Lisinopril")
            .status("active")
            .intent("order")
            .priority("routine")
            .authoredOn(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
