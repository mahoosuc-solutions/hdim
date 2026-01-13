package com.healthdata.caregap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.caregap.client.CqlEngineServiceClient;
import com.healthdata.caregap.client.PatientServiceClient;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CareGapIdentificationService.
 * Tests care gap identification, closure, and statistics calculation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Identification Service Tests")
class CareGapIdentificationServiceTest {

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private CqlEngineServiceClient cqlEngineServiceClient;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CareGapAuditIntegration careGapAuditIntegration;

    @Captor
    private ArgumentCaptor<CareGapEntity> gapCaptor;

    @Captor
    private ArgumentCaptor<String> kafkaMessageCaptor;

    private CareGapIdentificationService service;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String CREATED_BY = "test-user";

    @BeforeEach
    void setUp() {
        service = new CareGapIdentificationService(
                careGapRepository,
                patientServiceClient,
                cqlEngineServiceClient,
                kafkaTemplate,
                careGapAuditIntegration
        );
    }

    @Nested
    @DisplayName("Identify All Care Gaps Tests")
    class IdentifyAllCareGapsTests {

        @Test
        @DisplayName("Should identify gaps from multiple CQL libraries")
        void shouldIdentifyGapsFromMultipleLibraries() {
            // Given
            String librariesJson = "[{\"name\":\"HEDIS_CDC_A1C\"},{\"name\":\"HEDIS_BCS\"}]";
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn(librariesJson);

            // Mock CQL results - first library has gap, second does not
            when(cqlEngineServiceClient.evaluateCql(eq(TENANT_ID), eq("HEDIS_CDC_A1C"), eq(PATIENT_UUID), isNull()))
                    .thenReturn("{\"hasGap\":true,\"measureId\":\"CDC_A1C\",\"priority\":\"high\"}");
            when(cqlEngineServiceClient.evaluateCql(eq(TENANT_ID), eq("HEDIS_BCS"), eq(PATIENT_UUID), isNull()))
                    .thenReturn("{\"hasGap\":false}");

            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> {
                CareGapEntity entity = inv.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            List<CareGapEntity> gaps = service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            // Then
            assertThat(gaps).hasSize(1);
            assertThat(gaps.get(0).getGapCategory()).isEqualTo("HEDIS");
            verify(careGapRepository, times(1)).save(any(CareGapEntity.class));
        }

        @Test
        @DisplayName("Should return empty list when no libraries have gaps")
        void shouldReturnEmptyWhenNoGaps() {
            // Given
            String librariesJson = "[{\"name\":\"HEDIS_CDC\"}]";
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn(librariesJson);
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn("{\"hasGap\":false}");
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            List<CareGapEntity> gaps = service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            // Then
            assertThat(gaps).isEmpty();
            verify(careGapRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should publish gap identification event")
        void shouldPublishGapIdentificationEvent() {
            // Given
            String librariesJson = "[{\"name\":\"HEDIS_CDC\"}]";
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn(librariesJson);
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn("{\"hasGap\":true,\"measureId\":\"CDC\"}");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            // Then
            verify(kafkaTemplate).send(eq("care-gap-identified"), kafkaMessageCaptor.capture());
            String message = kafkaMessageCaptor.getValue();
            assertThat(message).contains(TENANT_ID);
            assertThat(message).contains(PATIENT_UUID.toString());
        }

        @Test
        @DisplayName("Should handle CQL evaluation errors gracefully")
        void shouldHandleCqlEvaluationErrors() {
            // Given
            String librariesJson = "[{\"name\":\"HEDIS_CDC\"},{\"name\":\"HEDIS_BCS\"}]";
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn(librariesJson);
            when(cqlEngineServiceClient.evaluateCql(eq(TENANT_ID), eq("HEDIS_CDC"), eq(PATIENT_UUID), isNull()))
                    .thenThrow(new RuntimeException("CQL Engine unavailable"));
            when(cqlEngineServiceClient.evaluateCql(eq(TENANT_ID), eq("HEDIS_BCS"), eq(PATIENT_UUID), isNull()))
                    .thenReturn("{\"hasGap\":true,\"measureId\":\"BCS\"}");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            List<CareGapEntity> gaps = service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            // Then - should still get gaps from successful library
            assertThat(gaps).hasSize(1);
            assertThat(gaps.get(0).getMeasureId()).isEqualTo("BCS");
        }

        @Test
        @DisplayName("Should return empty list when libraries JSON is invalid")
        void shouldHandleInvalidLibrariesJson() {
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn("not-json");
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            List<CareGapEntity> gaps = service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            assertThat(gaps).isEmpty();
            verify(cqlEngineServiceClient, never()).evaluateCql(anyString(), anyString(), any(UUID.class), any());
        }

        @Test
        @DisplayName("Should swallow Kafka errors when publishing identification events")
        void shouldSwallowKafkaErrorsOnIdentify() {
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn("[]");
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenThrow(new RuntimeException("kafka down"));

            List<CareGapEntity> gaps = service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            assertThat(gaps).isEmpty();
        }
    }

    @Nested
    @DisplayName("Identify Care Gaps for Library Tests")
    class IdentifyGapsForLibraryTests {

        @Test
        @DisplayName("Should create gap when CQL indicates hasGap=true")
        void shouldCreateGapWhenHasGapTrue() {
            // Given
            String cqlResult = "{\"hasGap\":true,\"measureId\":\"HEDIS_CDC_A1C\",\"measureName\":\"Diabetes A1C Control\",\"priority\":\"high\",\"gapDescription\":\"A1C test not performed\"}";
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn(cqlResult);
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "HEDIS_CDC_A1C", CREATED_BY);

            // Then
            assertThat(gaps).hasSize(1);
            verify(careGapRepository).save(gapCaptor.capture());
            CareGapEntity savedGap = gapCaptor.getValue();
            assertThat(savedGap.getMeasureId()).isEqualTo("HEDIS_CDC_A1C");
            assertThat(savedGap.getPriority()).isEqualTo("high");
            assertThat(savedGap.getGapDescription()).isEqualTo("A1C test not performed");
        }

        @Test
        @DisplayName("Should create gap when inNumerator=false")
        void shouldCreateGapWhenNotInNumerator() {
            // Given
            String cqlResult = "{\"inNumerator\":false,\"measureId\":\"CMS130\"}";
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn(cqlResult);
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "CMS_COLORECTAL", CREATED_BY);

            // Then
            assertThat(gaps).hasSize(1);
        }

        @Test
        @DisplayName("Should not create gap when inNumerator=true")
        void shouldNotCreateGapWhenInNumerator() {
            // Given
            String cqlResult = "{\"inNumerator\":true,\"measureId\":\"CMS130\"}";
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn(cqlResult);

            // When
            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "CMS_COLORECTAL", CREATED_BY);

            // Then
            assertThat(gaps).isEmpty();
            verify(careGapRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should extract HEDIS measure category from library name")
        void shouldExtractHedisMeasureCategory() {
            // Given - HEDIS prefix
            when(cqlEngineServiceClient.evaluateCql(anyString(), eq("HEDIS_BCS"), any(UUID.class), isNull()))
                    .thenReturn("{\"hasGap\":true}");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.identifyCareGapsForLibrary(TENANT_ID, PATIENT_UUID, "HEDIS_BCS", CREATED_BY);

            // Then
            verify(careGapRepository).save(gapCaptor.capture());
            assertThat(gapCaptor.getValue().getGapCategory()).isEqualTo("HEDIS");
        }

        @Test
        @DisplayName("Should extract CMS measure category from library name")
        void shouldExtractCmsMeasureCategory() {
            // Given - CMS prefix
            when(cqlEngineServiceClient.evaluateCql(anyString(), eq("CMS_130"), any(UUID.class), isNull()))
                    .thenReturn("{\"hasGap\":true}");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.identifyCareGapsForLibrary(TENANT_ID, PATIENT_UUID, "CMS_130", CREATED_BY);

            // Then
            verify(careGapRepository).save(gapCaptor.capture());
            assertThat(gapCaptor.getValue().getGapCategory()).isEqualTo("CMS");
        }

        @Test
        @DisplayName("Should set default values when CQL result missing fields")
        void shouldSetDefaultValues() {
            // Given
            String cqlResult = "{\"hasGap\":true}"; // Minimal result
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn(cqlResult);
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.identifyCareGapsForLibrary(TENANT_ID, PATIENT_UUID, "CUSTOM_MEASURE", CREATED_BY);

            // Then
            verify(careGapRepository).save(gapCaptor.capture());
            CareGapEntity gap = gapCaptor.getValue();
            assertThat(gap.getPriority()).isEqualTo("medium"); // Default priority
            assertThat(gap.getRiskScore()).isEqualTo(0.5); // Default risk
            assertThat(gap.getGapCategory()).isEqualTo("custom"); // Default category
            assertThat(gap.getGapDescription()).isEqualTo("Care gap identified"); // Default description
        }

        @Test
        @DisplayName("Should handle invalid CQL result JSON")
        void shouldHandleInvalidCqlResultJson() {
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn("invalid-json");

            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "HEDIS_CDC", CREATED_BY);

            assertThat(gaps).isEmpty();
        }

        @Test
        @DisplayName("Should map recommendation fields and handle invalid due date")
        void shouldMapRecommendationFields() {
            String cqlResult = """
                {"inNumerator":false,"measureId":"M1","measureName":"Measure One",
                 "gapReason":"reason","priority":"high","riskScore":0.9,
                 "dueDate":"not-a-date","recommendation":"rec",
                 "recommendationType":"screening","recommendedAction":"action"}
                """;
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), isNull()))
                    .thenReturn(cqlResult);
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                    TENANT_ID, PATIENT_UUID, "CMS_TEST", CREATED_BY);

            assertThat(gaps).hasSize(1);
            CareGapEntity gap = gaps.get(0);
            assertThat(gap.getMeasureName()).isEqualTo("Measure One");
            assertThat(gap.getGapReason()).isEqualTo("reason");
            assertThat(gap.getPriority()).isEqualTo("high");
            assertThat(gap.getRiskScore()).isEqualTo(0.9);
            assertThat(gap.getDueDate()).isEqualTo(LocalDate.now().plusDays(90));
            assertThat(gap.getRecommendation()).isEqualTo("rec");
            assertThat(gap.getRecommendationType()).isEqualTo("screening");
            assertThat(gap.getRecommendedAction()).isEqualTo("action");
        }
    }

    @Nested
    @DisplayName("Close Care Gap Tests")
    class CloseCareGapTests {

        @Test
        @DisplayName("Should close care gap successfully")
        void shouldCloseCareGapSuccessfully() {
            // Given
            UUID gapId = UUID.randomUUID();
            CareGapEntity existingGap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_UUID)
                    .measureId("CDC_A1C")
                    .gapStatus("open")
                    .build();

            when(careGapRepository.findByIdAndTenantId(gapId, TENANT_ID))
                    .thenReturn(Optional.of(existingGap));
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            CareGapEntity closedGap = service.closeCareGap(
                    TENANT_ID, gapId, "clinician-1", "A1C test performed", "Lab order completed");

            // Then
            assertThat(closedGap.getGapStatus()).isEqualTo("CLOSED");
            assertThat(closedGap.getClosedBy()).isEqualTo("clinician-1");
            assertThat(closedGap.getClosureReason()).isEqualTo("A1C test performed");
            assertThat(closedGap.getClosureAction()).isEqualTo("Lab order completed");
            assertThat(closedGap.getClosedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should publish gap closure event")
        void shouldPublishGapClosureEvent() {
            // Given
            UUID gapId = UUID.randomUUID();
            CareGapEntity existingGap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TENANT_ID)
                    .gapStatus("open")
                    .build();

            when(careGapRepository.findByIdAndTenantId(gapId, TENANT_ID))
                    .thenReturn(Optional.of(existingGap));
            when(careGapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            service.closeCareGap(TENANT_ID, gapId, "user-1", "Resolved", null);

            // Then
            verify(kafkaTemplate).send(eq("care-gap-closed"), kafkaMessageCaptor.capture());
            String message = kafkaMessageCaptor.getValue();
            assertThat(message).contains(gapId.toString());
            assertThat(message).contains("user-1");
        }

        @Test
        @DisplayName("Should throw when gap not found")
        void shouldThrowWhenGapNotFound() {
            // Given
            UUID gapId = UUID.randomUUID();
            when(careGapRepository.findByIdAndTenantId(gapId, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.closeCareGap(TENANT_ID, gapId, "user", "reason", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Care gap not found");
        }

        @Test
        @DisplayName("Should swallow Kafka errors when publishing closure events")
        void shouldSwallowKafkaErrorsOnClose() {
            UUID gapId = UUID.randomUUID();
            CareGapEntity existingGap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_UUID)
                    .measureId("M1")
                    .gapStatus("open")
                    .build();

            when(careGapRepository.findByIdAndTenantId(gapId, TENANT_ID))
                    .thenReturn(Optional.of(existingGap));
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenThrow(new RuntimeException("kafka down"));

            CareGapEntity result = service.closeCareGap(TENANT_ID, gapId, "user", "reason", "action");

            assertThat(result.getGapStatus()).isEqualTo("CLOSED");
        }
    }

    @Nested
    @DisplayName("Refresh Care Gaps Tests")
    class RefreshCareGapsTests {

        @Test
        @DisplayName("Should close gaps that are no longer present")
        void shouldCloseResolvedGaps() {
            // Given
            CareGapEntity existingGap = CareGapEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_UUID)
                    .measureId("OLD_MEASURE")
                    .gapStatus("open")
                    .build();

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(existingGap));

            // New evaluation finds no gaps
            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn("[]");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            service.refreshCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            // Then
            verify(careGapRepository).save(gapCaptor.capture());
            CareGapEntity closedGap = gapCaptor.getValue();
            assertThat(closedGap.getGapStatus()).isEqualTo("CLOSED");
            assertThat(closedGap.getClosureReason()).contains("resolved");
        }

        @Test
        @DisplayName("Should keep gaps that are still present")
        void shouldKeepCurrentGaps() {
            CareGapEntity existingGap = CareGapEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_UUID)
                    .measureId("CURRENT_MEASURE")
                    .gapStatus("open")
                    .build();

            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(existingGap));

            when(cqlEngineServiceClient.getAvailableLibraries()).thenReturn("[{\"name\":\"CURRENT_MEASURE\"}]");
            when(cqlEngineServiceClient.evaluateCql(eq(TENANT_ID), eq("CURRENT_MEASURE"), eq(PATIENT_UUID), isNull()))
                    .thenReturn("{\"hasGap\":true,\"measureId\":\"CURRENT_MEASURE\"}");
            when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            List<CareGapEntity> gaps = service.refreshCareGaps(TENANT_ID, PATIENT_UUID, CREATED_BY);

            assertThat(gaps).hasSize(1);
            verify(careGapRepository, never()).save(existingGap);
        }
    }

    @Nested
    @DisplayName("Get Open Care Gaps Tests")
    class GetOpenCareGapsTests {

        @Test
        @DisplayName("Should return open gaps for patient")
        void shouldReturnOpenGaps() {
            // Given
            List<CareGapEntity> mockGaps = List.of(
                    CareGapEntity.builder().id(UUID.randomUUID()).measureId("CDC").gapStatus("open").build(),
                    CareGapEntity.builder().id(UUID.randomUUID()).measureId("BCS").gapStatus("open").build()
            );
            when(careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_UUID))
                    .thenReturn(mockGaps);

            // When
            List<CareGapEntity> result = service.getOpenCareGaps(TENANT_ID, PATIENT_UUID);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get High Priority Care Gaps Tests")
    class GetHighPriorityCareGapsTests {

        @Test
        @DisplayName("Should return high priority gaps")
        void shouldReturnHighPriorityGaps() {
            // Given
            CareGapEntity highPriorityGap = CareGapEntity.builder()
                    .id(UUID.randomUUID())
                    .measureId("CRITICAL_MEASURE")
                    .priority("high")
                    .gapStatus("open")
                    .build();
            when(careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_UUID))
                    .thenReturn(List.of(highPriorityGap));

            // When
            List<CareGapEntity> result = service.getHighPriorityCareGaps(TENANT_ID, PATIENT_UUID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPriority()).isEqualTo("high");
        }
    }

    @Nested
    @DisplayName("Get Care Gap Stats Tests")
    class GetCareGapStatsTests {

        @Test
        @DisplayName("Should calculate care gap statistics")
        void shouldCalculateStats() {
            // Given
            when(careGapRepository.countOpenGaps(TENANT_ID, PATIENT_UUID)).thenReturn(5L);
            when(careGapRepository.countHighPriorityGaps(TENANT_ID, PATIENT_UUID)).thenReturn(2L);
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(1L);

            // When
            CareGapIdentificationService.CareGapStats stats = service.getCareGapStats(TENANT_ID, PATIENT_UUID);

            // Then
            assertThat(stats.openGapsCount()).isEqualTo(5);
            assertThat(stats.highPriorityCount()).isEqualTo(2);
            assertThat(stats.overdueCount()).isEqualTo(1);
            assertThat(stats.hasOpenGaps()).isTrue();
            assertThat(stats.hasHighPriorityGaps()).isTrue();
        }

        @Test
        @DisplayName("Should handle zero gaps")
        void shouldHandleZeroGaps() {
            // Given
            when(careGapRepository.countOpenGaps(TENANT_ID, PATIENT_UUID)).thenReturn(0L);
            when(careGapRepository.countHighPriorityGaps(TENANT_ID, PATIENT_UUID)).thenReturn(0L);
            when(careGapRepository.countOverdueGaps(eq(TENANT_ID), eq(PATIENT_UUID), any(LocalDate.class)))
                    .thenReturn(0L);

            // When
            CareGapIdentificationService.CareGapStats stats = service.getCareGapStats(TENANT_ID, PATIENT_UUID);

            // Then
            assertThat(stats.openGapsCount()).isZero();
            assertThat(stats.hasOpenGaps()).isFalse();
            assertThat(stats.hasHighPriorityGaps()).isFalse();
        }
    }
}
