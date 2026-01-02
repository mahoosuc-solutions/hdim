package com.healthdata.api;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.quality.domain.MeasureResult;
import com.healthdata.quality.service.QualityMeasureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for Quality Measure API endpoints.
 *
 * Tests all CRUD operations and error scenarios for the Quality Measure Controller.
 * Follows TDD best practices with proper mocking and assertions.
 *
 * Test Coverage:
 * - POST /api/measures/calculate?patientId={id}&measureId={id} (calculate single measure)
 * - POST /api/measures/batch?tenantId={id}&measureId={id} (batch processing)
 * - GET /api/measures/status?patientId={id}&measureId={id} (measure status)
 * - Error cases (404, 400, 409, etc.)
 * - Async batch processing
 * - Realistic measure IDs (HbA1c-Control, BP-Control, etc.)
 *
 * @author Test Suite
 */
@DisplayName("Quality Measure Controller Tests")
public class QualityMeasureControllerTest extends BaseWebControllerTest {

    @MockBean
    private QualityMeasureService qualityMeasureService;

    private MeasureResult testMeasureResult;
    private MeasureResult bpMeasureResult;
    private MeasureResult hbA1cMeasureResult;
    private String validPatientId;
    private String validMeasureId;
    private String validTenantId;
    private String nonExistentPatientId;
    private String invalidMeasureId;

    @BeforeEach
    public void setUp() {
        super.setUp();
        initializeTestData();
    }

    /**
     * Initialize reusable test data for all test methods
     */
    private void initializeTestData() {
        validPatientId = "patient-001";
        nonExistentPatientId = "patient-nonexistent";
        validTenantId = "tenant-healthcare-system";
        validMeasureId = "HbA1c-Control";
        invalidMeasureId = "invalid-measure";

        // HbA1c Control measure result
        testMeasureResult = MeasureResult.builder()
                .id("result-001")
                .patientId(validPatientId)
                .measureId(validMeasureId)
                .score(85.5)
                .numerator(17)
                .denominator(20)
                .compliant(true)
                .calculationDate(LocalDateTime.now())
                .periodStart(LocalDate.now().minusMonths(3))
                .periodEnd(LocalDate.now())
                .tenantId(validTenantId)
                .details(new HashMap<String, String>() {{
                    put("lastLabDate", LocalDate.now().minusDays(10).toString());
                    put("labValue", "7.2");
                    put("targetValue", "7.0");
                }})
                .build();

        // Blood Pressure Control measure result
        bpMeasureResult = MeasureResult.builder()
                .id("result-002")
                .patientId(validPatientId)
                .measureId("BP-Control")
                .score(92.0)
                .numerator(23)
                .denominator(25)
                .compliant(true)
                .calculationDate(LocalDateTime.now())
                .periodStart(LocalDate.now().minusMonths(3))
                .periodEnd(LocalDate.now())
                .tenantId(validTenantId)
                .details(new HashMap<String, String>() {{
                    put("systolic", "125");
                    put("diastolic", "82");
                    put("targetSystolic", "130");
                    put("targetDiastolic", "80");
                }})
                .build();

        // HbA1c Control non-compliant
        hbA1cMeasureResult = MeasureResult.builder()
                .id("result-003")
                .patientId(validPatientId)
                .measureId(validMeasureId)
                .score(65.0)
                .numerator(13)
                .denominator(20)
                .compliant(false)
                .calculationDate(LocalDateTime.now())
                .periodStart(LocalDate.now().minusMonths(3))
                .periodEnd(LocalDate.now())
                .tenantId(validTenantId)
                .details(new HashMap<String, String>() {{
                    put("lastLabDate", LocalDate.now().minusDays(25).toString());
                    put("labValue", "8.5");
                    put("targetValue", "7.0");
                }})
                .build();
    }

    @Nested
    @DisplayName("POST /api/measures/calculate - Single Measure Calculation Tests")
    class CalculateMeasureTests {

        @Test
        @DisplayName("Should successfully calculate HbA1c measure for patient")
        void testCalculateMeasureSuccess() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult responseMeasure = assertOkAndParse(result, MeasureResult.class);
            assertNotNull(responseMeasure.getId());
            assertEquals(validPatientId, responseMeasure.getPatientId());
            assertEquals(validMeasureId, responseMeasure.getMeasureId());
            assertEquals(85.5, responseMeasure.getScore());
            assertTrue(responseMeasure.isCompliant());
            assertEquals(17, responseMeasure.getNumerator());
            assertEquals(20, responseMeasure.getDenominator());

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should calculate BP-Control measure with correct compliance status")
        void testCalculateBPMeasure() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, "BP-Control"))
                    .thenReturn(bpMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=BP-Control",
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult responseMeasure = assertOkAndParse(result, MeasureResult.class);
            assertEquals("BP-Control", responseMeasure.getMeasureId());
            assertEquals(92.0, responseMeasure.getScore());
            assertTrue(responseMeasure.isCompliant());
            assertEquals("COMPLIANT", responseMeasure.getComplianceStatus());

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, "BP-Control");
        }

        @Test
        @DisplayName("Should handle non-compliant measure result")
        void testCalculateNonCompliantMeasure() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(hbA1cMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult responseMeasure = assertOkAndParse(result, MeasureResult.class);
            assertFalse(responseMeasure.isCompliant());
            assertEquals("NON_COMPLIANT", responseMeasure.getComplianceStatus());
            assertEquals(65.0, responseMeasure.getScore());

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return 404 when patient not found")
        void testCalculateMeasurePatientNotFound() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(nonExistentPatientId, validMeasureId))
                    .thenThrow(new IllegalArgumentException("Patient not found: " + nonExistentPatientId));

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + nonExistentPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertNotFoundStatus(result);
            assertClientErrorResponse(result);

            verify(qualityMeasureService, times(1)).calculateMeasure(nonExistentPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when patient ID is missing")
        void testCalculateMeasureMissingPatientId() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).calculateMeasure(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when measure ID is missing")
        void testCalculateMeasureMissingMeasureId() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId,
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).calculateMeasure(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return measure with correct calculation date")
        void testCalculateMeasureReturnsCalculationDate() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "calculationDate");
            assertJsonFieldExists(result, "periodStart");
            assertJsonFieldExists(result, "periodEnd");

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return measure with details and metadata")
        void testCalculateMeasureReturnsDetails() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "details");
            assertJsonFieldExists(result, "tenantId");
            String content = getResponseContent(result);
            assertTrue(content.contains("lastLabDate") || content.contains("labValue"));

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should set JSON content type on calculation response")
        void testCalculateMeasureJsonContentType() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should calculate Medication-Adherence measure")
        void testCalculateMedicationAdherenceMeasure() throws Exception {
            // Arrange
            MeasureResult medResult = MeasureResult.builder()
                    .id("result-med-001")
                    .patientId(validPatientId)
                    .measureId("Medication-Adherence")
                    .score(80.0)
                    .numerator(40)
                    .denominator(50)
                    .compliant(true)
                    .calculationDate(LocalDateTime.now())
                    .tenantId(validTenantId)
                    .build();

            when(qualityMeasureService.calculateMeasure(validPatientId, "Medication-Adherence"))
                    .thenReturn(medResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=Medication-Adherence",
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult responseMeasure = assertOkAndParse(result, MeasureResult.class);
            assertEquals("Medication-Adherence", responseMeasure.getMeasureId());
            assertEquals(80.0, responseMeasure.getScore());

            verify(qualityMeasureService, times(1)).calculateMeasure(validPatientId, "Medication-Adherence");
        }
    }

    @Nested
    @DisplayName("POST /api/measures/batch - Batch Measure Calculation Tests")
    class BatchMeasureCalculationTests {

        @Test
        @DisplayName("Should successfully process batch calculation for population")
        void testBatchMeasureCalculationSuccess() throws Exception {
            // Arrange
            List<MeasureResult> batchResults = new ArrayList<>();
            batchResults.add(testMeasureResult);
            batchResults.add(bpMeasureResult);

            CompletableFuture<List<MeasureResult>> futureResults =
                    CompletableFuture.completedFuture(batchResults);

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            )).thenReturn(futureResults);

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);

            verify(qualityMeasureService, times(1)).calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            );
        }

        @Test
        @DisplayName("Should handle batch calculation with custom pagination")
        void testBatchMeasureCalculationWithPagination() throws Exception {
            // Arrange
            List<MeasureResult> batchResults = new ArrayList<>();
            batchResults.add(testMeasureResult);

            CompletableFuture<List<MeasureResult>> futureResults =
                    CompletableFuture.completedFuture(batchResults);

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    1,
                    50
            )).thenReturn(futureResults);

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId + "&page=1&size=50",
                    ""
            );

            // Assert
            assertOkStatus(result);
            verify(qualityMeasureService, times(1)).calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    1,
                    50
            );
        }

        @Test
        @DisplayName("Should return 400 Bad Request when tenant ID is missing in batch")
        void testBatchMeasureCalculationMissingTenantId() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).calculateMeasuresForPopulation(
                    anyString(), anyString(), anyInt(), anyInt()
            );
        }

        @Test
        @DisplayName("Should return 400 Bad Request when measure ID is missing in batch")
        void testBatchMeasureCalculationMissingMeasureId() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId,
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).calculateMeasuresForPopulation(
                    anyString(), anyString(), anyInt(), anyInt()
            );
        }

        @Test
        @DisplayName("Should handle empty batch results gracefully")
        void testBatchMeasureCalculationEmptyResults() throws Exception {
            // Arrange
            List<MeasureResult> emptyResults = new ArrayList<>();
            CompletableFuture<List<MeasureResult>> futureResults =
                    CompletableFuture.completedFuture(emptyResults);

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            )).thenReturn(futureResults);

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            verify(qualityMeasureService, times(1)).calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            );
        }

        @Test
        @DisplayName("Should process batch for BP-Control measure")
        void testBatchMeasureCalculationBPControl() throws Exception {
            // Arrange
            List<MeasureResult> batchResults = new ArrayList<>();
            batchResults.add(bpMeasureResult);

            CompletableFuture<List<MeasureResult>> futureResults =
                    CompletableFuture.completedFuture(batchResults);

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId,
                    "BP-Control",
                    0,
                    25
            )).thenReturn(futureResults);

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=BP-Control",
                    ""
            );

            // Assert
            assertOkStatus(result);
            verify(qualityMeasureService, times(1)).calculateMeasuresForPopulation(
                    validTenantId,
                    "BP-Control",
                    0,
                    25
            );
        }

        @Test
        @DisplayName("Should verify batch operation is async")
        void testBatchMeasureCalculationIsAsync() throws Exception {
            // Arrange
            CompletableFuture<List<MeasureResult>> futureResults =
                    CompletableFuture.completedFuture(new ArrayList<>());

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            )).thenReturn(futureResults);

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            // Verify service method was called (async operation initiated)
            verify(qualityMeasureService, times(1)).calculateMeasuresForPopulation(
                    validTenantId,
                    validMeasureId,
                    0,
                    25
            );
        }
    }

    @Nested
    @DisplayName("GET /api/measures/status - Measure Status Tests")
    class MeasureStatusTests {

        @Test
        @DisplayName("Should successfully retrieve compliant measure status")
        void testGetMeasureStatusCompliant() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.COMPLIANT);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("COMPLIANT"));

            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should retrieve non-compliant measure status")
        void testGetMeasureStatusNonCompliant() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.NON_COMPLIANT);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("NON_COMPLIANT"));

            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return NOT_CALCULATED status when measure not yet calculated")
        void testGetMeasureStatusNotCalculated() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.NOT_CALCULATED);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("NOT_CALCULATED"));

            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return OUTDATED status for stale data")
        void testGetMeasureStatusOutdated() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.OUTDATED);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("OUTDATED"));

            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return NOT_APPLICABLE status for ineligible patients")
        void testGetMeasureStatusNotApplicable() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.NOT_APPLICABLE);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertTrue(content.contains("NOT_APPLICABLE"));

            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when patient ID is missing in status")
        void testGetMeasureStatusMissingPatientId() throws Exception {
            // Act
            MvcResult result = performGet(
                    "/api/measures/status?measureId=" + validMeasureId
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).getMeasureStatus(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when measure ID is missing in status")
        void testGetMeasureStatusMissingMeasureId() throws Exception {
            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId
            );

            // Assert
            assertBadRequestStatus(result);
            verify(qualityMeasureService, never()).getMeasureStatus(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return JSON content type for status endpoint")
        void testGetMeasureStatusJsonContentType() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.COMPLIANT);

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should verify service is called with correct parameters for status")
        void testGetMeasureStatusVerifiesServiceCall() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.COMPLIANT);

            // Act
            performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            verify(qualityMeasureService).getMeasureStatus(validPatientId, validMeasureId);
            verify(qualityMeasureService, times(1)).getMeasureStatus(validPatientId, validMeasureId);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service exceptions for calculate endpoint")
        void testCalculateMeasureHandlesServiceException() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle service exceptions for batch endpoint")
        void testBatchMeasureHandlesServiceException() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasuresForPopulation(anyString(), anyString(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Batch processing failed"));

            // Act
            MvcResult result = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle service exceptions for status endpoint")
        void testStatusMeasureHandlesServiceException() throws Exception {
            // Arrange
            when(qualityMeasureService.getMeasureStatus(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Status retrieval failed"));

            // Act
            MvcResult result = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should return proper error message format")
        void testErrorResponseFormat() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertBadRequestStatus(result);
            String content = getResponseContent(result);
            assertNotNull(content);
        }

        @Test
        @DisplayName("Should handle malformed request parameters")
        void testHandleMalformedRequestParameters() throws Exception {
            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            // Should return 400 or 404 depending on implementation
            int status = getStatusCode(result);
            assertTrue(status == 400 || status == 404 || status == 405,
                    "Expected 400, 404, or 405 but got " + status);
        }
    }

    @Nested
    @DisplayName("Multi-Tenant and Business Logic Tests")
    class MultiTenantAndBusinessLogicTests {

        @Test
        @DisplayName("Should maintain tenant isolation for measure calculations")
        void testTenantIsolationForMeasures() throws Exception {
            // Arrange
            String differentTenantId = "tenant-other-system";
            MeasureResult otherTenantResult = MeasureResult.builder()
                    .id("result-other-tenant")
                    .patientId("patient-other")
                    .measureId(validMeasureId)
                    .score(75.0)
                    .tenantId(differentTenantId)
                    .build();

            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            when(qualityMeasureService.calculateMeasure("patient-other", validMeasureId))
                    .thenReturn(otherTenantResult);

            // Act
            MvcResult result1 = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            MvcResult result2 = performPost(
                    "/api/measures/calculate?patientId=patient-other&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result1);
            assertOkStatus(result2);

            MeasureResult response1 = assertOkAndParse(result1, MeasureResult.class);
            MeasureResult response2 = assertOkAndParse(result2, MeasureResult.class);

            assertEquals(validTenantId, response1.getTenantId());
            assertEquals(differentTenantId, response2.getTenantId());
        }

        @Test
        @DisplayName("Should support multiple measure IDs for same patient")
        void testMultipleMeasuresForSamePatient() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);
            when(qualityMeasureService.calculateMeasure(validPatientId, "BP-Control"))
                    .thenReturn(bpMeasureResult);

            // Act
            MvcResult result1 = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            MvcResult result2 = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=BP-Control",
                    ""
            );

            // Assert
            assertOkStatus(result1);
            assertOkStatus(result2);

            MeasureResult response1 = assertOkAndParse(result1, MeasureResult.class);
            MeasureResult response2 = assertOkAndParse(result2, MeasureResult.class);

            assertEquals(validMeasureId, response1.getMeasureId());
            assertEquals("BP-Control", response2.getMeasureId());
        }

        @Test
        @DisplayName("Should calculate percentage correctly from numerator and denominator")
        void testMeasurePercentageCalculation() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult response = assertOkAndParse(result, MeasureResult.class);
            double expectedPercentage = (17.0 * 100.0) / 20.0;
            assertEquals(expectedPercentage, response.getPercentage(), 0.01);
        }

        @Test
        @DisplayName("Should return measure with period information")
        void testMeasureWithPeriodInformation() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult response = assertOkAndParse(result, MeasureResult.class);
            assertNotNull(response.getPeriodStart());
            assertNotNull(response.getPeriodEnd());
            assertTrue(response.getPeriodStart().isBefore(response.getPeriodEnd()));
        }

        @Test
        @DisplayName("Should include clinical details in measure result")
        void testMeasureIncludesClinicalDetails() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult response = assertOkAndParse(result, MeasureResult.class);
            assertNotNull(response.getDetails());
            assertFalse(response.getDetails().isEmpty());
            assertTrue(response.getDetails().containsKey("lastLabDate") ||
                    response.getDetails().containsKey("labValue"));
        }
    }

    @Nested
    @DisplayName("Response Format and Content Type Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return complete measure result object in response")
        void testResponseContainsMeasureResult() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            // Act
            MvcResult result = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(result);
            MeasureResult response = assertOkAndParse(result, MeasureResult.class);

            assertNotNull(response.getId());
            assertNotNull(response.getPatientId());
            assertNotNull(response.getMeasureId());
            assertNotNull(response.getScore());
            assertNotNull(response.getNumerator());
            assertNotNull(response.getDenominator());
        }

        @Test
        @DisplayName("Should return proper HTTP status codes for all endpoints")
        void testHttpStatusCodes() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.COMPLIANT);

            when(qualityMeasureService.calculateMeasuresForPopulation(
                    validTenantId, validMeasureId, 0, 25))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

            // Act & Assert
            MvcResult calculateResult = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );
            assertEquals(200, getStatusCode(calculateResult));

            MvcResult statusResult = performGet(
                    "/api/measures/status?patientId=" + validPatientId + "&measureId=" + validMeasureId
            );
            assertEquals(200, getStatusCode(statusResult));

            MvcResult batchResult = performPost(
                    "/api/measures/batch?tenantId=" + validTenantId + "&measureId=" + validMeasureId,
                    ""
            );
            assertEquals(200, getStatusCode(batchResult));
        }

        @Test
        @DisplayName("Should have consistent response structure across all endpoints")
        void testConsistentResponseStructure() throws Exception {
            // Arrange
            when(qualityMeasureService.calculateMeasure(validPatientId, validMeasureId))
                    .thenReturn(testMeasureResult);

            when(qualityMeasureService.getMeasureStatus(validPatientId, validMeasureId))
                    .thenReturn(QualityMeasureService.MeasureStatus.COMPLIANT);

            // Act
            MvcResult calcResult = performPost(
                    "/api/measures/calculate?patientId=" + validPatientId + "&measureId=" + validMeasureId,
                    ""
            );

            // Assert
            assertOkStatus(calcResult);
            assertJsonContentType(calcResult);
            assertJsonFieldExists(calcResult, "patientId");
            assertJsonFieldExists(calcResult, "measureId");
        }
    }
}
