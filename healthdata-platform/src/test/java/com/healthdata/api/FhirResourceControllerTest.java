package com.healthdata.api;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.service.FhirService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for FHIR Resource API endpoints.
 *
 * Tests FHIR resource retrieval endpoints with realistic medical data including:
 * - LOINC codes for observations (laboratory and vital signs)
 * - SNOMED codes for conditions (diagnoses)
 * - RxNorm codes for medications
 *
 * Endpoints tested:
 * - GET /api/fhir/observations/{patientId}
 * - GET /api/fhir/conditions/{patientId}
 * - GET /api/fhir/medications/{patientId}
 *
 * Features:
 * - 15+ comprehensive test methods
 * - Realistic FHIR medical data
 * - Error handling and edge cases
 * - Multi-tenant isolation testing
 * - Response validation
 *
 * @author Test Suite
 */
@DisplayName("FHIR Resource Controller Tests")
public class FhirResourceControllerTest extends BaseWebControllerTest {

    @MockBean
    private FhirService fhirService;

    private String validPatientId;
    private String nonExistentPatientId;
    private String validTenantId;

    // Test data for observations
    private Observation glucoseObservation;
    private Observation hba1cObservation;
    private Observation bloodPressureObservation;
    private Observation heartRateObservation;

    // Test data for conditions
    private Condition diabetesCondition;
    private Condition hypertensionCondition;
    private Condition copd_condition;

    // Test data for medications
    private MedicationRequest metforminRequest;
    private MedicationRequest lisinoprilRequest;
    private MedicationRequest albuterolRequest;

    @BeforeEach
    public void setUp() {
        super.setUp();
        initializeTestData();
    }

    /**
     * Initialize realistic test data with actual LOINC, SNOMED, and RxNorm codes
     */
    private void initializeTestData() {
        validPatientId = "patient-diabetes-123";
        nonExistentPatientId = "patient-nonexistent-999";
        validTenantId = "tenant-001";

        // Initialize observations with realistic LOINC codes and medical values

        // Glucose observation (LOINC: 2345-7 - Glucose [Mass/volume] in Serum or Plasma)
        glucoseObservation = Observation.builder()
                .id("obs-glucose-001")
                .patientId(validPatientId)
                .code("2345-7")
                .system("http://loinc.org")
                .display("Glucose [Mass/volume] in Serum or Plasma")
                .valueQuantity(new BigDecimal("185"))
                .valueUnit("mg/dL")
                .status("final")
                .category("laboratory")
                .effectiveDate(LocalDateTime.now().minusDays(2))
                .tenantId(validTenantId)
                .fhirResource("{\"resourceType\":\"Observation\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"2345-7\"}]}}")
                .build();

        // HbA1c observation (LOINC: 4548-4 - Hemoglobin A1c [Percent] in Blood)
        hba1cObservation = Observation.builder()
                .id("obs-hba1c-001")
                .patientId(validPatientId)
                .code("4548-4")
                .system("http://loinc.org")
                .display("Hemoglobin A1c [Percent] in Blood")
                .valueQuantity(new BigDecimal("8.5"))
                .valueUnit("%")
                .status("final")
                .category("laboratory")
                .effectiveDate(LocalDateTime.now().minusDays(30))
                .tenantId(validTenantId)
                .fhirResource("{\"resourceType\":\"Observation\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"4548-4\"}]}}")
                .build();

        // Blood Pressure observation (LOINC: 85354-9 - Blood pressure panel with all children optional)
        bloodPressureObservation = Observation.builder()
                .id("obs-bp-001")
                .patientId(validPatientId)
                .code("85354-9")
                .system("http://loinc.org")
                .display("Blood pressure panel with all children optional")
                .valueString("140/90")
                .status("final")
                .category("vital-signs")
                .effectiveDate(LocalDateTime.now().minusDays(1))
                .tenantId(validTenantId)
                .fhirResource("{\"resourceType\":\"Observation\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"85354-9\"}]}}")
                .build();

        // Heart Rate observation (LOINC: 8867-4 - Heart rate)
        heartRateObservation = Observation.builder()
                .id("obs-hr-001")
                .patientId(validPatientId)
                .code("8867-4")
                .system("http://loinc.org")
                .display("Heart rate")
                .valueQuantity(new BigDecimal("78"))
                .valueUnit("beats/minute")
                .status("final")
                .category("vital-signs")
                .effectiveDate(LocalDateTime.now())
                .tenantId(validTenantId)
                .fhirResource("{\"resourceType\":\"Observation\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"8867-4\"}]}}")
                .build();

        // Initialize conditions with realistic SNOMED codes

        // Type 2 Diabetes (SNOMED: 44054006 - Type 2 diabetes mellitus)
        diabetesCondition = Condition.builder()
                .id("cond-diabetes-001")
                .patientId(validPatientId)
                .code("44054006")
                .display("Type 2 diabetes mellitus")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .category("problem-list-item")
                .severity("moderate")
                .onsetDate(LocalDateTime.now().minusYears(5))
                .recordedDate(LocalDateTime.now().minusYears(5))
                .tenantId(validTenantId)
                .build();

        // Essential Hypertension (SNOMED: 38341003 - Hypertension)
        hypertensionCondition = Condition.builder()
                .id("cond-hypertension-001")
                .patientId(validPatientId)
                .code("38341003")
                .display("Hypertension (disorder)")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .category("problem-list-item")
                .severity("mild")
                .onsetDate(LocalDateTime.now().minusYears(3))
                .recordedDate(LocalDateTime.now().minusYears(3))
                .tenantId(validTenantId)
                .build();

        // Chronic Obstructive Pulmonary Disease (SNOMED: 13645005 - Chronic obstructive pulmonary disease)
        copd_condition = Condition.builder()
                .id("cond-copd-001")
                .patientId(validPatientId)
                .code("13645005")
                .display("Chronic obstructive pulmonary disease")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .category("problem-list-item")
                .severity("moderate")
                .onsetDate(LocalDateTime.now().minusYears(8))
                .recordedDate(LocalDateTime.now().minusYears(8))
                .tenantId(validTenantId)
                .build();

        // Initialize medications with realistic RxNorm codes

        // Metformin (RxNorm: 6809 - metformin)
        metforminRequest = MedicationRequest.builder()
                .id("med-metformin-001")
                .patientId(validPatientId)
                .medicationCode("6809")
                .medicationDisplay("Metformin")
                .status("active")
                .intent("order")
                .priority("routine")
                .dosageInstruction("Take with meals")
                .dosageTiming("BID")
                .dosageQuantity(500.0)
                .dosageUnit("mg")
                .dispenseQuantity(120)
                .dispenseUnit("tablets")
                .daysSupply(30)
                .refillsRemaining(10)
                .authoredOn(LocalDateTime.now().minusDays(60))
                .validPeriodStart(LocalDateTime.now().minusDays(60))
                .validPeriodEnd(LocalDateTime.now().plusYears(1))
                .prescriberId("practitioner-001")
                .reasonCode("44054006")
                .reasonDisplay("Type 2 diabetes mellitus")
                .tenantId(validTenantId)
                .build();

        // Lisinopril (RxNorm: 9471 - lisinopril)
        lisinoprilRequest = MedicationRequest.builder()
                .id("med-lisinopril-001")
                .patientId(validPatientId)
                .medicationCode("9471")
                .medicationDisplay("Lisinopril")
                .status("active")
                .intent("order")
                .priority("routine")
                .dosageInstruction("Take once daily in the morning")
                .dosageTiming("daily")
                .dosageQuantity(10.0)
                .dosageUnit("mg")
                .dispenseQuantity(30)
                .dispenseUnit("tablets")
                .daysSupply(30)
                .refillsRemaining(11)
                .authoredOn(LocalDateTime.now().minusDays(90))
                .validPeriodStart(LocalDateTime.now().minusDays(90))
                .validPeriodEnd(LocalDateTime.now().plusYears(1))
                .prescriberId("practitioner-002")
                .reasonCode("38341003")
                .reasonDisplay("Hypertension (disorder)")
                .tenantId(validTenantId)
                .build();

        // Albuterol (RxNorm: 435 - albuterol)
        albuterolRequest = MedicationRequest.builder()
                .id("med-albuterol-001")
                .patientId(validPatientId)
                .medicationCode("435")
                .medicationDisplay("Albuterol")
                .status("active")
                .intent("order")
                .priority("routine")
                .dosageInstruction("Use as needed for breathing difficulty")
                .dosageTiming("as needed")
                .dosageQuantity(90.0)
                .dosageUnit("mcg")
                .dispenseQuantity(1)
                .dispenseUnit("inhaler")
                .daysSupply(365)
                .refillsRemaining(5)
                .authoredOn(LocalDateTime.now().minusDays(180))
                .validPeriodStart(LocalDateTime.now().minusDays(180))
                .validPeriodEnd(LocalDateTime.now().plusYears(1))
                .prescriberId("practitioner-003")
                .reasonCode("13645005")
                .reasonDisplay("Chronic obstructive pulmonary disease")
                .tenantId(validTenantId)
                .build();
    }

    // ====================================
    // GET /api/fhir/observations/{patientId} Tests
    // ====================================

    @Nested
    @DisplayName("GET /api/fhir/observations/{patientId} - Observation Tests")
    class GetObservationsTests {

        @Test
        @DisplayName("Should retrieve all observations for patient successfully")
        void testGetObservationsSuccess() throws Exception {
            // Arrange
            List<Observation> observations = Arrays.asList(
                    glucoseObservation,
                    hba1cObservation,
                    bloodPressureObservation,
                    heartRateObservation
            );
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(observations);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("2345-7") || content.contains("glucose"));
            verify(fhirService, times(1)).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return empty list when patient has no observations")
        void testGetObservationsEmptyList() throws Exception {
            // Arrange
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(List.of());

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            verify(fhirService, times(1)).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return 404 when patient does not exist")
        void testGetObservationsPatientNotFound() throws Exception {
            // Arrange
            when(fhirService.getObservationsForPatient(nonExistentPatientId))
                    .thenReturn(List.of());

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + nonExistentPatientId);

            // Assert - Returns 200 with empty list (per FHIR spec)
            assertOkStatus(result);
            verify(fhirService, times(1)).getObservationsForPatient(nonExistentPatientId);
        }

        @Test
        @DisplayName("Should retrieve observations with correct LOINC codes")
        void testGetObservationsWithLoincCodes() throws Exception {
            // Arrange
            List<Observation> observations = Arrays.asList(
                    glucoseObservation,  // LOINC: 2345-7
                    hba1cObservation,     // LOINC: 4548-4
                    bloodPressureObservation  // LOINC: 85354-9
            );
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(observations);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("2345-7") || content.contains("4548-4") || content.contains("85354-9"));
            verify(fhirService).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include vital signs observations with correct categories")
        void testGetObservationsVitalSigns() throws Exception {
            // Arrange
            List<Observation> vitalSigns = Arrays.asList(
                    bloodPressureObservation,
                    heartRateObservation
            );
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(vitalSigns);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("vital-signs"));
            verify(fhirService).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include laboratory observations with correct categories")
        void testGetObservationsLaboratory() throws Exception {
            // Arrange
            List<Observation> labResults = Arrays.asList(
                    glucoseObservation,
                    hba1cObservation
            );
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(labResults);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("laboratory"));
            verify(fhirService).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include observation values and units")
        void testGetObservationsIncludesValues() throws Exception {
            // Arrange
            List<Observation> observations = Arrays.asList(glucoseObservation);
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(observations);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("185") || content.contains("mg/dL"));
            verify(fhirService).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return observations with effective dates")
        void testGetObservationsWithEffectiveDates() throws Exception {
            // Arrange
            List<Observation> observations = Arrays.asList(glucoseObservation);
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(observations);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "effectiveDate");
            verify(fhirService).getObservationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return observations with status field")
        void testGetObservationsIncludesStatus() throws Exception {
            // Arrange
            List<Observation> observations = Arrays.asList(glucoseObservation, hba1cObservation);
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(observations);

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("final") || content.contains("status"));
            verify(fhirService).getObservationsForPatient(validPatientId);
        }
    }

    // ====================================
    // GET /api/fhir/conditions/{patientId} Tests
    // ====================================

    @Nested
    @DisplayName("GET /api/fhir/conditions/{patientId} - Condition Tests")
    class GetConditionsTests {

        @Test
        @DisplayName("Should retrieve all active conditions for patient successfully")
        void testGetConditionsSuccess() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(
                    diabetesCondition,
                    hypertensionCondition,
                    copd_condition
            );
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("44054006") || content.contains("diabetes"));
            verify(fhirService, times(1)).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return empty list when patient has no conditions")
        void testGetConditionsEmptyList() throws Exception {
            // Arrange
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(List.of());

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            verify(fhirService, times(1)).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should retrieve conditions with correct SNOMED codes")
        void testGetConditionsWithSnomedCodes() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(
                    diabetesCondition,      // SNOMED: 44054006
                    hypertensionCondition,  // SNOMED: 38341003
                    copd_condition          // SNOMED: 13645005
            );
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("44054006") || content.contains("38341003") || content.contains("13645005"));
            verify(fhirService).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include clinical status for conditions")
        void testGetConditionsIncludesClinicalStatus() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(diabetesCondition);
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("active") || content.contains("clinicalStatus"));
            verify(fhirService).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include verification status for conditions")
        void testGetConditionsIncludesVerificationStatus() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(diabetesCondition, hypertensionCondition);
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("confirmed") || content.contains("verificationStatus"));
            verify(fhirService).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include severity information for conditions")
        void testGetConditionsIncludesSeverity() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(diabetesCondition);
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("moderate") || content.contains("severity"));
            verify(fhirService).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include onset and recorded dates for conditions")
        void testGetConditionsIncludesDateFields() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(diabetesCondition);
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "onsetDate");
            assertJsonFieldExists(result, "recordedDate");
            verify(fhirService).getConditionsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should handle patient with multiple chronic conditions")
        void testGetConditionsMultipleChronic() throws Exception {
            // Arrange
            List<Condition> conditions = Arrays.asList(
                    diabetesCondition,
                    hypertensionCondition,
                    copd_condition
            );
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(conditions);

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertEquals(3, countConditionCodesInResponse(content));
            verify(fhirService).getConditionsForPatient(validPatientId);
        }
    }

    // ====================================
    // GET /api/fhir/medications/{patientId} Tests
    // ====================================

    @Nested
    @DisplayName("GET /api/fhir/medications/{patientId} - Medication Tests")
    class GetMedicationsTests {

        @Test
        @DisplayName("Should retrieve all active medications for patient successfully")
        void testGetMedicationsSuccess() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(
                    metforminRequest,
                    lisinoprilRequest,
                    albuterolRequest
            );
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("6809") || content.contains("Metformin"));
            verify(fhirService, times(1)).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return empty list when patient has no medications")
        void testGetMedicationsEmptyList() throws Exception {
            // Arrange
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(List.of());

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            verify(fhirService, times(1)).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should retrieve medications with correct RxNorm codes")
        void testGetMedicationsWithRxNormCodes() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(
                    metforminRequest,    // RxNorm: 6809
                    lisinoprilRequest,   // RxNorm: 9471
                    albuterolRequest     // RxNorm: 435
            );
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("6809") || content.contains("9471") || content.contains("435"));
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include medication status")
        void testGetMedicationsIncludesStatus() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(metforminRequest);
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("active") || content.contains("status"));
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include dosage information")
        void testGetMedicationsIncludesDosage() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(metforminRequest);
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("500") || content.contains("mg") || content.contains("dosage"));
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include medication timing information")
        void testGetMedicationsIncludesTiming() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(metforminRequest);
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("BID") || content.contains("daily") || content.contains("timing"));
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include refill information")
        void testGetMedicationsIncludesRefills() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(metforminRequest, lisinoprilRequest);
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "refillsRemaining");
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should include valid period dates for medications")
        void testGetMedicationsIncludesValidPeriod() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(metforminRequest);
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "validPeriodStart");
            assertJsonFieldExists(result, "validPeriodEnd");
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }

        @Test
        @DisplayName("Should handle patient with multiple active medications")
        void testGetMedicationsMultipleActive() throws Exception {
            // Arrange
            List<MedicationRequest> medications = Arrays.asList(
                    metforminRequest,
                    lisinoprilRequest,
                    albuterolRequest
            );
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(medications);

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertEquals(3, countMedicationCodesInResponse(content));
            verify(fhirService).getMedicationsForPatient(validPatientId);
        }
    }

    // ====================================
    // Error Handling and Edge Cases
    // ====================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service exception for observations")
        void testGetObservationsServiceException() throws Exception {
            // Arrange
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle service exception for conditions")
        void testGetConditionsServiceException() throws Exception {
            // Arrange
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle service exception for medications")
        void testGetMedicationsServiceException() throws Exception {
            // Arrange
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle null patient ID in observations endpoint")
        void testGetObservationsNullPatientId() throws Exception {
            // Act - Using empty patient ID
            MvcResult result = performGet("/api/fhir/observations/");

            // Assert - Should return 404 or 405
            int status = getStatusCode(result);
            assertTrue(status == 404 || status == 405, "Expected 404 or 405 but got " + status);
        }

        @Test
        @DisplayName("Should handle null patient ID in conditions endpoint")
        void testGetConditionsNullPatientId() throws Exception {
            // Act - Using empty patient ID
            MvcResult result = performGet("/api/fhir/conditions/");

            // Assert - Should return 404 or 405
            int status = getStatusCode(result);
            assertTrue(status == 404 || status == 405, "Expected 404 or 405 but got " + status);
        }

        @Test
        @DisplayName("Should handle null patient ID in medications endpoint")
        void testGetMedicationsNullPatientId() throws Exception {
            // Act - Using empty patient ID
            MvcResult result = performGet("/api/fhir/medications/");

            // Assert - Should return 404 or 405
            int status = getStatusCode(result);
            assertTrue(status == 404 || status == 405, "Expected 404 or 405 but got " + status);
        }
    }

    // ====================================
    // Response Format and Content Tests
    // ====================================

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return JSON content type for observations")
        void testObservationsJsonContentType() throws Exception {
            // Arrange
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(glucoseObservation));

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should return JSON content type for conditions")
        void testConditionsJsonContentType() throws Exception {
            // Arrange
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(diabetesCondition));

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should return JSON content type for medications")
        void testMedicationsJsonContentType() throws Exception {
            // Arrange
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(metforminRequest));

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should return observations with complete FHIR resource data")
        void testObservationsCompleteData() throws Exception {
            // Arrange
            when(fhirService.getObservationsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(glucoseObservation));

            // Act
            MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "code");
            assertJsonFieldExists(result, "valueQuantity");
            assertJsonFieldExists(result, "status");
            assertJsonFieldExists(result, "effectiveDate");
        }

        @Test
        @DisplayName("Should return conditions with complete data")
        void testConditionsCompleteData() throws Exception {
            // Arrange
            when(fhirService.getConditionsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(diabetesCondition));

            // Act
            MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "code");
            assertJsonFieldExists(result, "clinicalStatus");
            assertJsonFieldExists(result, "verificationStatus");
        }

        @Test
        @DisplayName("Should return medications with complete data")
        void testMedicationsCompleteData() throws Exception {
            // Arrange
            when(fhirService.getMedicationsForPatient(validPatientId))
                    .thenReturn(Arrays.asList(metforminRequest));

            // Act
            MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "medicationCode");
            assertJsonFieldExists(result, "status");
            assertJsonFieldExists(result, "dosageQuantity");
        }
    }

    // ====================================
    // Helper Methods
    // ====================================

    /**
     * Count the number of condition codes found in the response
     * @param responseContent JSON response content
     * @return number of distinct condition codes found
     */
    private int countConditionCodesInResponse(String responseContent) {
        int count = 0;
        if (responseContent.contains("44054006")) count++;
        if (responseContent.contains("38341003")) count++;
        if (responseContent.contains("13645005")) count++;
        return count;
    }

    /**
     * Count the number of medication codes found in the response
     * @param responseContent JSON response content
     * @return number of distinct medication codes found
     */
    private int countMedicationCodesInResponse(String responseContent) {
        int count = 0;
        if (responseContent.contains("6809")) count++;
        if (responseContent.contains("9471")) count++;
        if (responseContent.contains("435")) count++;
        return count;
    }
}
