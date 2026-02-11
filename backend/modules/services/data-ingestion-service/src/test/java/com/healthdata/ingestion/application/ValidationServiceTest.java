package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.IngestionProgressResponse;
import com.healthdata.ingestion.client.CareGapIngestionClient;
import com.healthdata.ingestion.client.FhirIngestionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ValidationService.
 * Tests the AI-powered validation service for ingested data.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validation Service Tests")
@Tag("unit")
class ValidationServiceTest {

    @Mock
    private FhirIngestionClient fhirClient;

    @Mock
    private CareGapIngestionClient careGapClient;

    @Mock
    private ProgressTrackingService progressService;

    private ValidationService validationService;

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_TENANT_ID = "test-tenant-001";

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(fhirClient, careGapClient, progressService);
    }

    private void setupBasicMocks(long patientCount, long careGapCount) {
        // Setup progress
        IngestionProgressResponse progress = IngestionProgressResponse.builder()
                .sessionId(TEST_SESSION_ID)
                .status("COMPLETED")
                .patientsPersisted(patientCount)
                .careGapsCreated(careGapCount)
                .build();
        when(progressService.getProgress(TEST_SESSION_ID)).thenReturn(progress);

        // Setup audit trail with tenant ID
        Map<String, Object> auditTrail = new HashMap<>();
        auditTrail.put("tenantId", TEST_TENANT_ID);
        auditTrail.put("eventCount", 10);
        when(progressService.getAuditTrail(TEST_SESSION_ID)).thenReturn(auditTrail);

        // Setup service impact
        Map<String, Object> serviceImpact = new HashMap<>();
        serviceImpact.put("totalServiceCalls", 25);
        serviceImpact.put("fhirCalls", 15);
        serviceImpact.put("careGapCalls", 10);
        when(progressService.getServiceImpact(TEST_SESSION_ID)).thenReturn(serviceImpact);
    }

    @Nested
    @DisplayName("Patient Count Validation Tests")
    class PatientCountValidationTests {

        @Test
        @DisplayName("Should pass when patient count matches expected")
        void shouldPassWhenPatientCountMatchesExpected() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            assertThat(result).containsKey("checks");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> patientCheck = checks.stream()
                    .filter(c -> "Patient Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.get("passed")).isEqualTo(true);
            assertThat(patientCheck.get("actual")).isEqualTo(100);
        }

        @Test
        @DisplayName("Should pass when patient count exceeds expected")
        void shouldPassWhenPatientCountExceedsExpected() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(120); // More than expected
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> patientCheck = checks.stream()
                    .filter(c -> "Patient Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.get("passed")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should fail when patient count is below expected")
        void shouldFailWhenPatientCountIsBelowExpected() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(50); // Less than expected
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> patientCheck = checks.stream()
                    .filter(c -> "Patient Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.get("passed")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should handle FHIR client exception gracefully")
        void shouldHandleFhirClientExceptionGracefully() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenThrow(new RuntimeException("FHIR service unavailable"));
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> patientCheck = checks.stream()
                    .filter(c -> "Patient Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(patientCheck.get("passed")).isEqualTo(false);
            assertThat((String) patientCheck.get("message")).contains("Validation failed");
        }
    }

    @Nested
    @DisplayName("Care Gap Validation Tests")
    class CareGapValidationTests {

        @Test
        @DisplayName("Should pass when care gap count is within 10% variance")
        void shouldPassWhenCareGapCountIsWithinVariance() {
            // Given
            setupBasicMocks(100, 100);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(95); // 5% less, within 10%

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> careGapCheck = checks.stream()
                    .filter(c -> "Care Gap Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(careGapCheck.get("passed")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should fail when care gap count is outside 10% variance")
        void shouldFailWhenCareGapCountIsOutsideVariance() {
            // Given
            setupBasicMocks(100, 100);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(50); // 50% less, outside 10%

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> careGapCheck = checks.stream()
                    .filter(c -> "Care Gap Count".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(careGapCheck.get("passed")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("Overall Validation Report Tests")
    class OverallValidationReportTests {

        @Test
        @DisplayName("Should return PASSED status when all checks pass")
        void shouldReturnPassedStatusWhenAllChecksPass() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            assertThat(result.get("overallStatus")).isEqualTo("PASSED");
            assertThat(result.get("checksPerformed")).isEqualTo(4);
            assertThat(result.get("checksPassed")).isEqualTo(4L);
            assertThat((Double) result.get("passRate")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return FAILED status when any check fails")
        void shouldReturnFailedStatusWhenAnyCheckFails() {
            // Given
            setupBasicMocks(100, 100);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(50); // Will fail
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(100);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            assertThat(result.get("overallStatus")).isEqualTo("FAILED");
            assertThat((Long) result.get("checksPassed")).isLessThan(4L);
        }

        @Test
        @DisplayName("Should include AI analysis in report")
        void shouldIncludeAiAnalysisInReport() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            assertThat(result).containsKey("aiAnalysis");
            assertThat((String) result.get("aiAnalysis")).isNotBlank();
        }

        @Test
        @DisplayName("Should include recommendations in report")
        void shouldIncludeRecommendationsInReport() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            assertThat(result).containsKey("recommendations");
            @SuppressWarnings("unchecked")
            List<String> recommendations = (List<String>) result.get("recommendations");
            assertThat(recommendations).isNotEmpty();
        }

        @Test
        @DisplayName("Should provide specific recommendations for failed checks")
        void shouldProvideSpecificRecommendationsForFailedChecks() {
            // Given
            setupBasicMocks(100, 100);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(50); // Will fail
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(50); // Will fail

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<String> recommendations = (List<String>) result.get("recommendations");
            assertThat(recommendations).anyMatch(r -> r.contains("FHIR"));
            assertThat(recommendations).anyMatch(r -> r.contains("care gap"));
        }
    }

    @Nested
    @DisplayName("Audit Trail Validation Tests")
    class AuditTrailValidationTests {

        @Test
        @DisplayName("Should pass audit trail check when trail exists")
        void shouldPassAuditTrailCheckWhenTrailExists() {
            // Given
            setupBasicMocks(100, 30);
            when(fhirClient.countPatients(TEST_TENANT_ID)).thenReturn(100);
            when(careGapClient.countCareGaps(TEST_TENANT_ID)).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> auditCheck = checks.stream()
                    .filter(c -> "Audit Trail".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(auditCheck.get("passed")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should fail audit trail check when trail is empty")
        void shouldFailAuditTrailCheckWhenTrailIsEmpty() {
            // Given
            setupBasicMocks(100, 30);
            when(progressService.getAuditTrail(TEST_SESSION_ID)).thenReturn(new HashMap<>()); // Empty
            when(fhirClient.countPatients(anyString())).thenReturn(100);
            when(careGapClient.countCareGaps(anyString())).thenReturn(30);

            // When
            Map<String, Object> result = validationService.validateIngestion(TEST_SESSION_ID);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");

            Map<String, Object> auditCheck = checks.stream()
                    .filter(c -> "Audit Trail".equals(c.get("check")))
                    .findFirst()
                    .orElseThrow();

            assertThat(auditCheck.get("passed")).isEqualTo(false);
        }
    }
}
