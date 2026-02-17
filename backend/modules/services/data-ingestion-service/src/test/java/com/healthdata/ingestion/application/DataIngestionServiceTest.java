package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.IngestionProgressResponse;
import com.healthdata.ingestion.api.v1.IngestionRequest;
import com.healthdata.ingestion.api.v1.IngestionResponse;
import com.healthdata.ingestion.client.CareGapIngestionClient;
import com.healthdata.ingestion.client.FhirIngestionClient;
import com.healthdata.ingestion.client.QualityMeasureIngestionClient;
import com.healthdata.ingestion.generator.SyntheticPatientGenerator;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataIngestionService.
 * Tests the data ingestion orchestration service functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Data Ingestion Service Tests")
@Tag("unit")
class DataIngestionServiceTest {

    @Mock
    private SyntheticPatientGenerator patientGenerator;

    @Mock
    private FhirIngestionClient fhirClient;

    @Mock
    private CareGapIngestionClient careGapClient;

    @Mock
    private QualityMeasureIngestionClient qualityMeasureClient;

    @Mock
    private ProgressTrackingService progressTrackingService;

    private DataIngestionService dataIngestionService;

    private static final String TEST_TENANT_ID = "test-tenant-001";

    @BeforeEach
    void setUp() {
        dataIngestionService = new DataIngestionService(
                patientGenerator,
                fhirClient,
                careGapClient,
                qualityMeasureClient,
                progressTrackingService
        );
    }

    private IngestionRequest createTestRequest(int patientCount) {
        return IngestionRequest.builder()
                .tenantId(TEST_TENANT_ID)
                .patientCount(patientCount)
                .includeCareGaps(false)
                .includeQualityMeasures(false)
                .build();
    }

    @Nested
    @DisplayName("Start Ingestion Tests")
    class StartIngestionTests {

        @Test
        @DisplayName("Should start ingestion and return session ID")
        void shouldStartIngestionAndReturnSessionId() {
            // Given
            IngestionRequest request = createTestRequest(10);

            // When
            IngestionResponse response = dataIngestionService.startIngestion(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isNotNull().isNotBlank();
            assertThat(response.getStatus()).isEqualTo("STARTED");
            assertThat(response.getMessage()).contains("10 patients");
            assertThat(response.getMessage()).contains(TEST_TENANT_ID);
        }

        @Test
        @DisplayName("Should initialize progress tracking on start")
        void shouldInitializeProgressTrackingOnStart() {
            // Given
            IngestionRequest request = createTestRequest(5);

            // When
            IngestionResponse response = dataIngestionService.startIngestion(request);

            // Then
            verify(progressTrackingService).initializeSession(anyString(), eq(request));
        }

        @Test
        @DisplayName("Should generate unique session IDs for each ingestion")
        void shouldGenerateUniqueSessionIds() {
            // Given
            IngestionRequest request1 = createTestRequest(10);
            IngestionRequest request2 = createTestRequest(20);

            // When
            IngestionResponse response1 = dataIngestionService.startIngestion(request1);
            IngestionResponse response2 = dataIngestionService.startIngestion(request2);

            // Then
            assertThat(response1.getSessionId()).isNotEqualTo(response2.getSessionId());
        }
    }

    @Nested
    @DisplayName("Get Progress Tests")
    class GetProgressTests {

        @Test
        @DisplayName("Should return progress for specific session")
        void shouldReturnProgressForSpecificSession() {
            // Given
            String sessionId = "test-session-123";
            IngestionProgressResponse expectedProgress = IngestionProgressResponse.builder()
                    .sessionId(sessionId)
                    .status("GENERATING")
                    .progressPercent(25)
                    .build();
            when(progressTrackingService.getProgress(sessionId)).thenReturn(expectedProgress);

            // When
            IngestionProgressResponse result = dataIngestionService.getProgress(sessionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isEqualTo(sessionId);
            assertThat(result.getStatus()).isEqualTo("GENERATING");
            assertThat(result.getProgressPercent()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should return latest session progress when sessionId is null")
        void shouldReturnLatestSessionProgressWhenSessionIdIsNull() {
            // Given - Start an ingestion first to set latestSessionId
            IngestionRequest request = createTestRequest(10);
            IngestionResponse startResponse = dataIngestionService.startIngestion(request);
            String latestSessionId = startResponse.getSessionId();

            IngestionProgressResponse expectedProgress = IngestionProgressResponse.builder()
                    .sessionId(latestSessionId)
                    .status("PERSISTING")
                    .progressPercent(50)
                    .build();
            when(progressTrackingService.getProgress(latestSessionId)).thenReturn(expectedProgress);

            // When
            IngestionProgressResponse result = dataIngestionService.getProgress(null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PERSISTING");
        }

        @Test
        @DisplayName("Should return NO_SESSION when no sessions exist")
        void shouldReturnNoSessionWhenNoSessionsExist() {
            // Given - Create fresh service with no sessions
            DataIngestionService freshService = new DataIngestionService(
                    patientGenerator, fhirClient, careGapClient, qualityMeasureClient, progressTrackingService);

            // When
            IngestionProgressResponse result = freshService.getProgress(null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("NO_SESSION");
            assertThat(result.getProgressPercent()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cancel Ingestion Tests")
    class CancelIngestionTests {

        @Test
        @DisplayName("Should return NOT_FOUND for unknown session")
        void shouldReturnNotFoundForUnknownSession() {
            // Given
            String unknownSessionId = "unknown-session-xyz";

            // When
            IngestionResponse response = dataIngestionService.cancelIngestion(unknownSessionId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("NOT_FOUND");
            assertThat(response.getMessage()).contains("Session not found");
            verify(progressTrackingService, never()).cancelSession(anyString());
        }
    }

    @Nested
    @DisplayName("Async Ingestion Tests")
    class AsyncIngestionTests {

        @Test
        @DisplayName("Should call patient generator with correct parameters")
        void shouldCallPatientGeneratorWithCorrectParameters() throws InterruptedException {
            // Given
            IngestionRequest request = createTestRequest(15);
            Bundle mockBundle = new Bundle();
            mockBundle.setType(Bundle.BundleType.COLLECTION);
            when(patientGenerator.generateCohort(15, TEST_TENANT_ID)).thenReturn(mockBundle);

            // When
            dataIngestionService.startIngestion(request);
            // Give async processing time to start (note: proper testing would use TestEventWaiter)
            Thread.sleep(100);

            // Then
            verify(patientGenerator, timeout(1000)).generateCohort(15, TEST_TENANT_ID);
        }
    }
}
