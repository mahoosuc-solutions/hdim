package com.healthdata.devops.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.devops.client.FhirServiceClient;
import com.healthdata.devops.model.FhirValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FhirDataValidationService.
 * Tests FHIR demo data validation for DevOps monitoring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FHIR Data Validation Service Tests")
@Tag("unit")
class FhirDataValidationServiceTest {

    @Mock
    private FhirServiceClient fhirClient;

    private FhirDataValidationService validationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validationService = new FhirDataValidationService(fhirClient);
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Resource Count Validation Tests")
    class ResourceCountValidationTests {

        @Test
        @DisplayName("Should pass when all resource counts meet minimum requirements")
        void shouldPassWhenAllResourceCountsMeetMinimum() {
            // Given - All counts meet minimum
            when(fhirClient.getResourceCount("Patient")).thenReturn(100);
            when(fhirClient.getResourceCount("Condition")).thenReturn(200);
            when(fhirClient.getResourceCount("Observation")).thenReturn(500);
            when(fhirClient.getResourceCount("MedicationRequest")).thenReturn(100);
            when(fhirClient.getResourceCount("Encounter")).thenReturn(150);
            when(fhirClient.getResourceCount("Procedure")).thenReturn(50);
            when(fhirClient.getResourceCount("Immunization")).thenReturn(60);
            when(fhirClient.getResourceCount("AllergyIntolerance")).thenReturn(40);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            List<FhirValidationResult.ResourceCountCheck> resourceChecks = result.getResourceCountChecks();
            assertThat(resourceChecks).isNotEmpty();

            long passedResourceChecks = resourceChecks.stream()
                    .filter(c -> "PASS".equals(c.getStatus()))
                    .count();
            assertThat(passedResourceChecks).isEqualTo(resourceChecks.size());
        }

        @Test
        @DisplayName("Should fail when resource counts are below minimum")
        void shouldFailWhenResourceCountsBelowMinimum() {
            // Given - Patient count is below minimum
            when(fhirClient.getResourceCount("Patient")).thenReturn(10); // Min is 50
            when(fhirClient.getResourceCount("Condition")).thenReturn(200);
            when(fhirClient.getResourceCount("Observation")).thenReturn(500);
            when(fhirClient.getResourceCount("MedicationRequest")).thenReturn(100);
            when(fhirClient.getResourceCount("Encounter")).thenReturn(150);
            when(fhirClient.getResourceCount("Procedure")).thenReturn(50);
            when(fhirClient.getResourceCount("Immunization")).thenReturn(60);
            when(fhirClient.getResourceCount("AllergyIntolerance")).thenReturn(40);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.ResourceCountCheck patientCheck = result.getResourceCountChecks().stream()
                    .filter(c -> "Patient".equals(c.getResourceType()))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.getStatus()).isEqualTo("FAIL");
            assertThat(result.getOverallStatus()).isEqualTo("FAIL");
        }

        @Test
        @DisplayName("Should warn when resource counts are between 50% and 100% of minimum")
        void shouldWarnWhenResourceCountsPartiallyMet() {
            // Given - Patient count is at 60% of minimum (warning range)
            when(fhirClient.getResourceCount("Patient")).thenReturn(30); // Min is 50, 30 is 60%
            when(fhirClient.getResourceCount("Condition")).thenReturn(200);
            when(fhirClient.getResourceCount("Observation")).thenReturn(500);
            when(fhirClient.getResourceCount("MedicationRequest")).thenReturn(100);
            when(fhirClient.getResourceCount("Encounter")).thenReturn(150);
            when(fhirClient.getResourceCount("Procedure")).thenReturn(50);
            when(fhirClient.getResourceCount("Immunization")).thenReturn(60);
            when(fhirClient.getResourceCount("AllergyIntolerance")).thenReturn(40);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.ResourceCountCheck patientCheck = result.getResourceCountChecks().stream()
                    .filter(c -> "Patient".equals(c.getResourceType()))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.getStatus()).isEqualTo("WARN");
        }
    }

    @Nested
    @DisplayName("Code System Validation Tests")
    class CodeSystemValidationTests {

        @Test
        @DisplayName("Should pass when required codes are present")
        void shouldPassWhenRequiredCodesPresent() {
            // Given
            setupResourceCountMocks(true);
            when(fhirClient.getResourceCountByCode("Condition", "44054006")).thenReturn(25);
            when(fhirClient.getResourceCountByCode("Condition", "59621000")).thenReturn(30);
            when(fhirClient.getResourceCountByCode("Observation", "4548-4")).thenReturn(50);
            when(fhirClient.getResourceCountByCode("Observation", "8480-6")).thenReturn(60);
            when(fhirClient.getResourceCountByCode("Observation", "44249-1")).thenReturn(40);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            List<FhirValidationResult.CodeSystemCheck> codeChecks = result.getCodeSystemChecks();
            assertThat(codeChecks).isNotEmpty();

            long passedCodeChecks = codeChecks.stream()
                    .filter(c -> "PASS".equals(c.getStatus()))
                    .count();
            assertThat(passedCodeChecks).isEqualTo(codeChecks.size());
        }

        @Test
        @DisplayName("Should fail when required codes are missing")
        void shouldFailWhenRequiredCodesMissing() {
            // Given
            setupResourceCountMocks(true);
            when(fhirClient.getResourceCountByCode("Condition", "44054006")).thenReturn(0); // Missing!
            when(fhirClient.getResourceCountByCode("Condition", "59621000")).thenReturn(30);
            when(fhirClient.getResourceCountByCode("Observation", "4548-4")).thenReturn(50);
            when(fhirClient.getResourceCountByCode("Observation", "8480-6")).thenReturn(60);
            when(fhirClient.getResourceCountByCode("Observation", "44249-1")).thenReturn(40);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.CodeSystemCheck diabetesCheck = result.getCodeSystemChecks().stream()
                    .filter(c -> "44054006".equals(c.getCode()))
                    .findFirst()
                    .orElseThrow();

            assertThat(diabetesCheck.getStatus()).isEqualTo("FAIL");
        }
    }

    @Nested
    @DisplayName("FHIR Compliance Validation Tests")
    class FhirComplianceValidationTests {

        @Test
        @DisplayName("Should pass when FHIR version is R4")
        void shouldPassWhenFhirVersionIsR4() throws Exception {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupPatientSamplesMock(true);

            JsonNode metadata = objectMapper.readTree("{\"fhirVersion\": \"4.0.1\", \"status\": \"active\"}");
            when(fhirClient.getMetadata()).thenReturn(metadata);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.ComplianceCheck versionCheck = result.getComplianceChecks().stream()
                    .filter(c -> "FHIR Version".equals(c.getCheckName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(versionCheck.getStatus()).isEqualTo("PASS");
        }

        @Test
        @DisplayName("Should warn when FHIR version is not R4")
        void shouldWarnWhenFhirVersionIsNotR4() throws Exception {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupPatientSamplesMock(true);

            JsonNode metadata = objectMapper.readTree("{\"fhirVersion\": \"3.0.1\", \"status\": \"active\"}");
            when(fhirClient.getMetadata()).thenReturn(metadata);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.ComplianceCheck versionCheck = result.getComplianceChecks().stream()
                    .filter(c -> "FHIR Version".equals(c.getCheckName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(versionCheck.getStatus()).isEqualTo("WARN");
        }

        @Test
        @DisplayName("Should fail when metadata is not accessible")
        void shouldFailWhenMetadataNotAccessible() {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupPatientSamplesMock(true);
            when(fhirClient.getMetadata()).thenReturn(null);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            FhirValidationResult.ComplianceCheck metadataCheck = result.getComplianceChecks().stream()
                    .filter(c -> "FHIR Metadata".equals(c.getCheckName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(metadataCheck.getStatus()).isEqualTo("FAIL");
        }
    }

    @Nested
    @DisplayName("Overall Validation Result Tests")
    class OverallValidationResultTests {

        @Test
        @DisplayName("Should generate validation ID and timestamp")
        void shouldGenerateValidationIdAndTimestamp() {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            assertThat(result.getValidationId()).isNotBlank();
            assertThat(result.getValidationTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate correct check totals")
        void shouldCalculateCorrectCheckTotals() {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            int expectedTotal = result.getResourceCountChecks().size() +
                    result.getCodeSystemChecks().size() +
                    result.getAuthenticityChecks().size() +
                    result.getComplianceChecks().size() +
                    result.getRelationshipChecks().size();

            assertThat(result.getTotalChecks()).isEqualTo(expectedTotal);
            assertThat(result.getPassedChecks() + result.getFailedChecks() + result.getWarningChecks())
                    .isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("Should include summary with resource counts")
        void shouldIncludeSummaryWithResourceCounts() {
            // Given
            setupResourceCountMocks(true);
            setupCodeSystemMocks(true);
            setupMetadataMock(true);
            setupPatientSamplesMock(true);

            // When
            FhirValidationResult result = validationService.validateDemoData();

            // Then
            assertThat(result.getSummary()).isNotNull();
            assertThat(result.getSummary()).containsKey("resourceCounts");
            assertThat(result.getSummary()).containsKey("totalResources");
        }
    }

    // Helper methods for setting up mocks
    private void setupResourceCountMocks(boolean meetMinimums) {
        int multiplier = meetMinimums ? 2 : 0;
        when(fhirClient.getResourceCount("Patient")).thenReturn(50 * multiplier);
        when(fhirClient.getResourceCount("Condition")).thenReturn(50 * multiplier);
        when(fhirClient.getResourceCount("Observation")).thenReturn(200 * multiplier);
        when(fhirClient.getResourceCount("MedicationRequest")).thenReturn(50 * multiplier);
        when(fhirClient.getResourceCount("Encounter")).thenReturn(50 * multiplier);
        when(fhirClient.getResourceCount("Procedure")).thenReturn(20 * multiplier);
        when(fhirClient.getResourceCount("Immunization")).thenReturn(30 * multiplier);
        when(fhirClient.getResourceCount("AllergyIntolerance")).thenReturn(20 * multiplier);
    }

    private void setupCodeSystemMocks(boolean codesPresent) {
        int count = codesPresent ? 20 : 0;
        when(fhirClient.getResourceCountByCode(anyString(), anyString())).thenReturn(count);
    }

    private void setupMetadataMock(boolean available) {
        if (available) {
            try {
                JsonNode metadata = objectMapper.readTree("{\"fhirVersion\": \"4.0.1\", \"status\": \"active\"}");
                when(fhirClient.getMetadata()).thenReturn(metadata);
            } catch (Exception e) {
                // Ignore
            }
        } else {
            when(fhirClient.getMetadata()).thenReturn(null);
        }
    }

    private void setupPatientSamplesMock(boolean withCompleteData) {
        try {
            String json = withCompleteData ?
                    "{\"entry\": [{\"resource\": {\"name\": [{\"family\": \"Doe\"}], \"birthDate\": \"1990-01-01\"}}]}" :
                    "{\"entry\": [{\"resource\": {}}]}";
            JsonNode patientBundle = objectMapper.readTree(json);
            when(fhirClient.getResourceSamples(anyString(), anyInt())).thenReturn(patientBundle);
        } catch (Exception e) {
            // Ignore
        }
    }
}
