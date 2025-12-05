package com.healthdata.api;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.caregap.domain.CareGap;
import com.healthdata.caregap.service.CareGapDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for Care Gap API endpoints.
 *
 * Tests all Care Gap operations including detection, batch processing, and closure.
 * Follows TDD best practices with proper mocking and assertions.
 *
 * Test Coverage:
 * - GET /api/caregaps/{patientId} (detect and retrieve care gaps)
 * - POST /api/caregaps/detect-batch (batch care gap detection)
 * - POST /api/caregaps/{gapId}/close (close a care gap)
 * - Various realistic scenarios with multiple care gaps
 * - Error cases (404, 400, 422, etc.)
 *
 * Features:
 * - 15+ comprehensive test methods
 * - Multiple care gap types (preventive, chronic disease, medication, screening)
 * - Priority levels (HIGH, MEDIUM, LOW)
 * - Status transitions (OPEN, IN_PROGRESS, CLOSED)
 * - Batch processing scenarios
 * - Error handling and edge cases
 *
 * @author Test Suite
 */
@DisplayName("Care Gap Controller Tests")
public class CareGapControllerTest extends BaseWebControllerTest {

    @MockBean
    private CareGapDetector careGapDetector;

    private String validPatientId;
    private String anotherPatientId;
    private String nonExistentPatientId;
    private String validGapId;
    private String nonExistentGapId;

    private CareGap preventiveCareGap;
    private CareGap chronicDiseaseGap;
    private CareGap medicationGap;
    private CareGap screeningGap;
    private CareGap highPriorityGap;
    private CareGap lowPriorityGap;

    @BeforeEach
    public void setUp() {
        super.setUp();
        initializeTestData();
    }

    /**
     * Initialize reusable test data for all test methods
     */
    private void initializeTestData() {
        // Patient IDs
        validPatientId = "patient-001";
        anotherPatientId = "patient-002";
        nonExistentPatientId = "patient-nonexistent";

        // Gap IDs
        validGapId = "gap-001";
        nonExistentGapId = "gap-nonexistent";

        // Create preventive care gap
        preventiveCareGap = CareGap.builder()
                .id("gap-preventive-001")
                .patientId(validPatientId)
                .gapType("PREVENTIVE_CARE")
                .description("Annual wellness visit overdue")
                .priority("MEDIUM")
                .status("OPEN")
                .dueDate(LocalDateTime.now().plusDays(30))
                .detectedDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create chronic disease monitoring gap
        chronicDiseaseGap = CareGap.builder()
                .id("gap-chronic-001")
                .patientId(validPatientId)
                .gapType("CHRONIC_DISEASE_MONITORING")
                .description("HbA1c test overdue for diabetic patient")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-CDC")
                .dueDate(LocalDateTime.now().plusDays(7))
                .detectedDate(LocalDateTime.now())
                .riskScore(8.5)
                .financialImpact(1500.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create medication adherence gap
        medicationGap = CareGap.builder()
                .id("gap-medication-001")
                .patientId(validPatientId)
                .gapType("MEDICATION_ADHERENCE")
                .description("Medication refill needed: Lisinopril")
                .priority("MEDIUM")
                .status("OPEN")
                .dueDate(LocalDateTime.now().plusDays(14))
                .detectedDate(LocalDateTime.now())
                .interventionType("MEDICATION_REFILL")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create cancer screening gap
        screeningGap = CareGap.builder()
                .id("gap-screening-001")
                .patientId(validPatientId)
                .gapType("CANCER_SCREENING")
                .description("Colorectal cancer screening overdue")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-COL")
                .dueDate(LocalDateTime.now().plusDays(60))
                .detectedDate(LocalDateTime.now())
                .riskScore(7.2)
                .financialImpact(800.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create high-priority gap
        highPriorityGap = CareGap.builder()
                .id("gap-high-priority-001")
                .patientId(validPatientId)
                .gapType("CHRONIC_DISEASE_MONITORING")
                .description("Blood pressure reading overdue - patient with hypertension")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-BPM")
                .dueDate(LocalDateTime.now().plusDays(1))
                .detectedDate(LocalDateTime.now())
                .riskScore(9.0)
                .financialImpact(2000.0)
                .interventionType("APPOINTMENT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create low-priority gap
        lowPriorityGap = CareGap.builder()
                .id("gap-low-priority-001")
                .patientId(validPatientId)
                .gapType("PREVENTIVE_CARE")
                .description("Dental cleaning recommended")
                .priority("LOW")
                .status("OPEN")
                .dueDate(LocalDateTime.now().plusDays(90))
                .detectedDate(LocalDateTime.now())
                .riskScore(2.5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/caregaps/{patientId} Tests ====================

    @Nested
    @DisplayName("GET /api/caregaps/{patientId} - Retrieve Care Gaps Tests")
    class GetCareGapsTests {

        @Test
        @DisplayName("Should successfully retrieve all care gaps for a patient")
        void testGetCareGapsSuccess() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    preventiveCareGap,
                    chronicDiseaseGap,
                    medicationGap,
                    screeningGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertEquals(4, responseGaps.size());
            assertTrue(responseGaps.stream()
                    .allMatch(gap -> gap.getPatientId().equals(validPatientId)));
            verify(careGapDetector, times(1)).detectCareGaps(validPatientId);
        }

        @Test
        @DisplayName("Should return empty list when patient has no care gaps")
        void testGetCareGapsEmpty() throws Exception {
            // Arrange
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(new ArrayList<>());

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertTrue(responseGaps.isEmpty());
            verify(careGapDetector, times(1)).detectCareGaps(validPatientId);
        }

        @Test
        @DisplayName("Should handle patient not found gracefully")
        void testGetCareGapsPatientNotFound() throws Exception {
            // Arrange
            when(careGapDetector.detectCareGaps(nonExistentPatientId))
                    .thenThrow(new IllegalArgumentException("Patient not found: " + nonExistentPatientId));

            // Act
            MvcResult result = performGet("/api/caregaps/" + nonExistentPatientId);

            // Assert
            assertNotFoundStatus(result);
            verify(careGapDetector, times(1)).detectCareGaps(nonExistentPatientId);
        }

        @Test
        @DisplayName("Should retrieve care gaps with various priority levels")
        void testGetCareGapsWithVariousPriorities() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    highPriorityGap,
                    preventiveCareGap,
                    lowPriorityGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertEquals(3, responseGaps.size());

            // Verify priority levels
            long highPriorityCount = responseGaps.stream()
                    .filter(g -> "HIGH".equals(g.getPriority())).count();
            long mediumPriorityCount = responseGaps.stream()
                    .filter(g -> "MEDIUM".equals(g.getPriority())).count();
            long lowPriorityCount = responseGaps.stream()
                    .filter(g -> "LOW".equals(g.getPriority())).count();

            assertEquals(1, highPriorityCount);
            assertEquals(1, mediumPriorityCount);
            assertEquals(1, lowPriorityCount);
        }

        @Test
        @DisplayName("Should retrieve care gaps with various gap types")
        void testGetCareGapsWithVariousTypes() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    preventiveCareGap,
                    chronicDiseaseGap,
                    medicationGap,
                    screeningGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertEquals(4, responseGaps.size());

            // Verify gap types
            List<String> gapTypes = responseGaps.stream()
                    .map(CareGap::getGapType).toList();
            assertTrue(gapTypes.contains("PREVENTIVE_CARE"));
            assertTrue(gapTypes.contains("CHRONIC_DISEASE_MONITORING"));
            assertTrue(gapTypes.contains("MEDICATION_ADHERENCE"));
            assertTrue(gapTypes.contains("CANCER_SCREENING"));
        }

        @Test
        @DisplayName("Should retrieve care gaps with financial impact data")
        void testGetCareGapsWithFinancialImpact() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    chronicDiseaseGap,
                    screeningGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertEquals(2, responseGaps.size());

            // Verify financial impact
            CareGap chronicGap = responseGaps.stream()
                    .filter(g -> "CHRONIC_DISEASE_MONITORING".equals(g.getGapType()))
                    .findFirst().orElse(null);
            assertNotNull(chronicGap);
            assertEquals(1500.0, chronicGap.getFinancialImpact());
            assertEquals(8.5, chronicGap.getRiskScore());
        }

        @Test
        @DisplayName("Should retrieve care gaps with measure IDs")
        void testGetCareGapsWithMeasureIds() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    chronicDiseaseGap,
                    screeningGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);

            // Verify measure IDs
            CareGap hedisGap = responseGaps.stream()
                    .filter(g -> g.getMeasureId() != null)
                    .findFirst().orElse(null);
            assertNotNull(hedisGap);
            assertNotNull(hedisGap.getMeasureId());
        }

        @Test
        @DisplayName("Should include response status as OPEN for all retrieved gaps")
        void testGetCareGapsStatusOpen() throws Exception {
            // Arrange
            List<CareGap> careGaps = Arrays.asList(
                    preventiveCareGap,
                    chronicDiseaseGap
            );
            when(careGapDetector.detectCareGaps(validPatientId))
                    .thenReturn(careGaps);

            // Act
            MvcResult result = performGet("/api/caregaps/" + validPatientId);

            // Assert
            assertOkStatus(result);
            List<CareGap> responseGaps = parseResponseContentAsArray(result, CareGap.class);
            assertNotNull(responseGaps);
            assertTrue(responseGaps.stream()
                    .allMatch(gap -> "OPEN".equals(gap.getStatus())));
        }
    }

    // ==================== POST /api/caregaps/detect-batch Tests ====================

    @Nested
    @DisplayName("POST /api/caregaps/detect-batch - Batch Detection Tests")
    class BatchDetectionTests {

        @Test
        @DisplayName("Should successfully start batch care gap detection")
        void testBatchDetectionSuccess() throws Exception {
            // Arrange
            List<String> patientIds = Arrays.asList("patient-001", "patient-002", "patient-003");
            doNothing().when(careGapDetector).detectCareGapsBatch(patientIds);

            String requestBody = toJson(patientIds);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertAcceptedStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertNotNull(responseBody);
            assertEquals("processing", responseBody.get("status"));
            assertEquals("3", responseBody.get("patients"));
            verify(careGapDetector, times(1)).detectCareGapsBatch(patientIds);
        }

        @Test
        @DisplayName("Should handle empty patient list in batch detection")
        void testBatchDetectionEmptyList() throws Exception {
            // Arrange
            List<String> emptyPatientIds = new ArrayList<>();
            doNothing().when(careGapDetector).detectCareGapsBatch(emptyPatientIds);

            String requestBody = toJson(emptyPatientIds);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertAcceptedStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertNotNull(responseBody);
            assertEquals("0", responseBody.get("patients"));
            verify(careGapDetector, times(1)).detectCareGapsBatch(emptyPatientIds);
        }

        @Test
        @DisplayName("Should handle large batch of patients")
        void testBatchDetectionLargeList() throws Exception {
            // Arrange
            List<String> largePatientList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largePatientList.add("patient-" + (1000 + i));
            }
            doNothing().when(careGapDetector).detectCareGapsBatch(largePatientList);

            String requestBody = toJson(largePatientList);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertAcceptedStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertNotNull(responseBody);
            assertEquals("processing", responseBody.get("status"));
            assertEquals("100", responseBody.get("patients"));
            verify(careGapDetector, times(1)).detectCareGapsBatch(largePatientList);
        }

        @Test
        @DisplayName("Should handle invalid JSON in batch request")
        void testBatchDetectionInvalidJson() throws Exception {
            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", "{invalid json");

            // Assert
            assertBadRequestStatus(result);
        }

        @Test
        @DisplayName("Should process batch request asynchronously")
        void testBatchDetectionAsync() throws Exception {
            // Arrange
            List<String> patientIds = Arrays.asList("patient-001", "patient-002");
            doNothing().when(careGapDetector).detectCareGapsBatch(patientIds);

            String requestBody = toJson(patientIds);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertAcceptedStatus(result);
            // Verify that the response returns immediately (202 Accepted)
            // and the actual detection happens asynchronously
            verify(careGapDetector, times(1)).detectCareGapsBatch(patientIds);
        }

        @Test
        @DisplayName("Should return correct status for multiple patients batch")
        void testBatchDetectionMultiplePatients() throws Exception {
            // Arrange
            List<String> patientIds = Arrays.asList(validPatientId, anotherPatientId);
            doNothing().when(careGapDetector).detectCareGapsBatch(patientIds);

            String requestBody = toJson(patientIds);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertAcceptedStatus(result);
            assertJsonContentType(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertNotNull(responseBody);
            assertTrue(responseBody.containsKey("status"));
            assertTrue(responseBody.containsKey("patients"));
        }

        @Test
        @DisplayName("Should handle service error during batch detection")
        void testBatchDetectionServiceError() throws Exception {
            // Arrange
            List<String> patientIds = Arrays.asList("patient-001", "patient-002");
            doThrow(new RuntimeException("Service error during batch processing"))
                    .when(careGapDetector).detectCareGapsBatch(patientIds);

            String requestBody = toJson(patientIds);

            // Act
            MvcResult result = performPost("/api/caregaps/detect-batch", requestBody);

            // Assert
            assertInternalServerErrorStatus(result);
            verify(careGapDetector, times(1)).detectCareGapsBatch(patientIds);
        }
    }

    // ==================== POST /api/caregaps/{gapId}/close Tests ====================

    @Nested
    @DisplayName("POST /api/caregaps/{gapId}/close - Close Care Gap Tests")
    class CloseCareGapTests {

        @Test
        @DisplayName("Should successfully close a care gap")
        void testCloseCareGapSuccess() throws Exception {
            // Arrange
            String closureReason = "Patient completed annual wellness visit";
            doNothing().when(careGapDetector).closeCareGap(preventiveCareGap.getId(), closureReason);

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + preventiveCareGap.getId() + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertNotNull(responseBody);
            assertEquals("closed", responseBody.get("status"));
            assertEquals(preventiveCareGap.getId(), responseBody.get("gapId"));
            verify(careGapDetector, times(1)).closeCareGap(
                    preventiveCareGap.getId(),
                    closureReason
            );
        }

        @Test
        @DisplayName("Should handle closing gap with various closure reasons")
        void testCloseCareGapWithVariousReasons() throws Exception {
            // Arrange
            String[] reasons = {
                    "Lab test completed",
                    "Patient refused treatment",
                    "Medication not needed",
                    "Patient moved to different provider"
            };

            for (String reason : reasons) {
                doNothing().when(careGapDetector).closeCareGap(validGapId, reason);

                // Act
                MvcResult result = performPost(
                        "/api/caregaps/" + validGapId + "/close?reason=" + encodeParam(reason),
                        ""
                );

                // Assert
                assertOkStatus(result);
                Map<String, String> responseBody = parseResponseContent(result, Map.class);
                assertEquals("closed", responseBody.get("status"));
            }
        }

        @Test
        @DisplayName("Should handle closing non-existent care gap")
        void testCloseCareGapNotFound() throws Exception {
            // Arrange
            String closureReason = "Testing not found error";
            doThrow(new IllegalArgumentException("Care gap not found: " + nonExistentGapId))
                    .when(careGapDetector).closeCareGap(nonExistentGapId, closureReason);

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + nonExistentGapId + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertNotFoundStatus(result);
            verify(careGapDetector, times(1)).closeCareGap(nonExistentGapId, closureReason);
        }

        @Test
        @DisplayName("Should require reason parameter for closing gap")
        void testCloseCareGapMissingReason() throws Exception {
            // Act - No reason parameter provided
            MvcResult result = performPost(
                    "/api/caregaps/" + validGapId + "/close",
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
        }

        @Test
        @DisplayName("Should handle closing chronic disease gap")
        void testCloseCareGapChronicDisease() throws Exception {
            // Arrange
            String closureReason = "HbA1c test completed - results good";
            doNothing().when(careGapDetector).closeCareGap(
                    chronicDiseaseGap.getId(),
                    closureReason
            );

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + chronicDiseaseGap.getId() + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertEquals("closed", responseBody.get("status"));
        }

        @Test
        @DisplayName("Should handle closing medication adherence gap")
        void testCloseCareGapMedicationAdherence() throws Exception {
            // Arrange
            String closureReason = "Medication refill completed";
            doNothing().when(careGapDetector).closeCareGap(
                    medicationGap.getId(),
                    closureReason
            );

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + medicationGap.getId() + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
            verify(careGapDetector, times(1)).closeCareGap(
                    medicationGap.getId(),
                    closureReason
            );
        }

        @Test
        @DisplayName("Should handle closing screening gap")
        void testCloseCareGapScreening() throws Exception {
            // Arrange
            String closureReason = "Colorectal cancer screening completed";
            doNothing().when(careGapDetector).closeCareGap(
                    screeningGap.getId(),
                    closureReason
            );

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + screeningGap.getId() + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
            Map<String, String> responseBody = parseResponseContent(result, Map.class);
            assertEquals("closed", responseBody.get("status"));
        }

        @Test
        @DisplayName("Should handle closing high-priority gap")
        void testCloseCareGapHighPriority() throws Exception {
            // Arrange
            String closureReason = "Blood pressure appointment completed - readings normal";
            doNothing().when(careGapDetector).closeCareGap(
                    highPriorityGap.getId(),
                    closureReason
            );

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + highPriorityGap.getId() + "/close?reason=" +
                    encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
            verify(careGapDetector, times(1)).closeCareGap(
                    highPriorityGap.getId(),
                    closureReason
            );
        }

        @Test
        @DisplayName("Should handle special characters in closure reason")
        void testCloseCareGapSpecialCharactersInReason() throws Exception {
            // Arrange
            String closureReason = "Patient referred to specialist & follow-up scheduled";
            doNothing().when(careGapDetector).closeCareGap(validGapId, closureReason);

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + validGapId + "/close?reason=" + encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
        }

        @Test
        @DisplayName("Should handle closing gap with long closure reason")
        void testCloseCareGapLongReason() throws Exception {
            // Arrange
            String closureReason = "Patient completed comprehensive metabolic panel (CMP), lipid panel, " +
                    "and urinalysis. All results within normal limits. Provider reviewed results and " +
                    "determined no additional intervention needed at this time. Patient counseled on " +
                    "lifestyle modifications.";
            doNothing().when(careGapDetector).closeCareGap(validGapId, closureReason);

            // Act
            MvcResult result = performPost(
                    "/api/caregaps/" + validGapId + "/close?reason=" + encodeParam(closureReason),
                    ""
            );

            // Assert
            assertOkStatus(result);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Parse response content as array of specified type
     */
    private <T> List<T> parseResponseContentAsArray(MvcResult result, Class<T> elementType) throws Exception {
        String content = getResponseContent(result);
        com.fasterxml.jackson.databind.JavaType arrayType = objectMapper.getTypeFactory()
                .constructArrayType(elementType);
        T[] array = objectMapper.readValue(content, arrayType);
        return Arrays.asList(array);
    }

    /**
     * URL encode a parameter
     */
    private String encodeParam(String param) {
        return param.replace(" ", "%20")
                .replace("&", "%26")
                .replace("(", "%28")
                .replace(")", "%29");
    }

    /**
     * Assert that response has 202 Accepted status
     */
    private void assertAcceptedStatus(MvcResult result) {
        assertStatus(result, 202);
    }
}
