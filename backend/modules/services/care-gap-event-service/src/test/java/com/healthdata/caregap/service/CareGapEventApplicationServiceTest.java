package com.healthdata.caregap.service;

import com.healthdata.caregap.api.v1.dto.CareGapEventResponse;
import com.healthdata.caregap.api.v1.dto.DetectGapRequest;
import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.PopulationHealthRepository;
import com.healthdata.caregap.projection.CareGapProjection;
import com.healthdata.caregap.projection.PopulationHealthProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

/**
 * Mock-based unit tests for CareGapEventApplicationService
 *
 * No database required - validates business logic only
 *
 * ★ Insight ─────────────────────────────────────
 * Care gap management involves tracking gap lifecycle across severity levels:
 *
 * 1. **Gap Detection** - Creates OPEN gaps with severity (CRITICAL, HIGH, MEDIUM, LOW)
 * 2. **Population Tracking** - Aggregates gaps by severity for population health view
 * 3. **Gap Closure** - Decrements open count, increments closed count
 * 4. **Closure Rate Calculation** - Measures intervention effectiveness
 *
 * All operations respect multi-tenant isolation via tenantId filtering.
 * ─────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class CareGapEventApplicationServiceTest {

    @Mock
    private CareGapEventHandler gapEventHandler;

    @Mock
    private CareGapProjectionRepository gapRepository;

    @Mock
    private PopulationHealthRepository populationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CareGapEventApplicationService applicationService;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-123";
    private static final String GAP_ID = "gap-456";
    private static final String GAP_CODE = "DIABETES_A1C";

    private DetectGapRequest detectGapRequest;

    @BeforeEach
    void setUp() {
        detectGapRequest = DetectGapRequest.builder()
            .patientId(PATIENT_ID)
            .gapCode(GAP_CODE)
            .severity("HIGH")
            .description("A1C test overdue")
            .build();
    }

    // ==================== detectGap() Tests ====================

    @Test
    void detectGap_ShouldReturnOPEN_WhenGapDetected() {
        // Given
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        CareGapEventResponse response = applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getGapCode()).isEqualTo(GAP_CODE);
        assertThat(response.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getDaysOpen()).isEqualTo(0);
    }

    @Test
    void detectGap_ShouldPublishKafkaEvent() {
        // Given
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        verify(kafkaTemplate).send(eq("gap.events"), eq(PATIENT_ID), any());
    }

    @Test
    void detectGap_ShouldDelegateToEventHandler() {
        // Given
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        verify(gapEventHandler, times(1)).handle(any(com.healthdata.caregap.event.CareGapDetectedEvent.class));
    }

    // ==================== Population Health - detectGap() ====================

    @Test
    void detectGap_ShouldCreatePopulationHealth_WhenNoneExists() {
        // Given - no existing population health
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.empty());

        detectGapRequest.setSeverity("CRITICAL");

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then - new population health created
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getTotalGapsOpen()).isEqualTo(1);
        assertThat(saved.getCriticalGaps()).isEqualTo(1);
        assertThat(saved.getHighGaps()).isEqualTo(0);
    }

    @Test
    void detectGap_ShouldIncrementCriticalCount_WhenSeverityCRITICAL() {
        // Given
        PopulationHealthProjection existing = new PopulationHealthProjection(TENANT_ID);
        existing.setTotalGapsOpen(10);
        existing.setCriticalGaps(2);
        existing.setHighGaps(5);
        existing.setMediumGaps(2);
        existing.setLowGaps(1);

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(existing));

        detectGapRequest.setSeverity("CRITICAL");

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(11);
        assertThat(saved.getCriticalGaps()).isEqualTo(3);  // incremented
        assertThat(saved.getHighGaps()).isEqualTo(5);      // unchanged
    }

    @Test
    void detectGap_ShouldIncrementHighCount_WhenSeverityHIGH() {
        // Given
        PopulationHealthProjection existing = new PopulationHealthProjection(TENANT_ID);
        existing.setTotalGapsOpen(10);
        existing.setCriticalGaps(2);
        existing.setHighGaps(5);

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(existing));

        detectGapRequest.setSeverity("HIGH");

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(11);
        assertThat(saved.getCriticalGaps()).isEqualTo(2);  // unchanged
        assertThat(saved.getHighGaps()).isEqualTo(6);      // incremented
    }

    @Test
    void detectGap_ShouldIncrementMediumCount_WhenSeverityMEDIUM() {
        // Given
        PopulationHealthProjection existing = new PopulationHealthProjection(TENANT_ID);
        existing.setTotalGapsOpen(10);
        existing.setMediumGaps(3);

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(existing));

        detectGapRequest.setSeverity("MEDIUM");

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(11);
        assertThat(saved.getMediumGaps()).isEqualTo(4);  // incremented
    }

    @Test
    void detectGap_ShouldIncrementLowCount_WhenSeverityLOW() {
        // Given
        PopulationHealthProjection existing = new PopulationHealthProjection(TENANT_ID);
        existing.setTotalGapsOpen(10);
        existing.setLowGaps(1);

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(existing));

        detectGapRequest.setSeverity("LOW");

        // When
        applicationService.detectGap(detectGapRequest, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(11);
        assertThat(saved.getLowGaps()).isEqualTo(2);  // incremented
    }

    // ==================== closeGap() Tests ====================

    @Test
    void closeGap_ShouldReturnCLOSED_WhenGapExists() {
        // Given
        CareGapProjection gap = mock(CareGapProjection.class);
        when(gap.getPatientId()).thenReturn(PATIENT_ID);
        when(gap.getGapCode()).thenReturn(GAP_CODE);
        when(gap.getSeverity()).thenReturn("HIGH");

        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(10);
        population.setHighGaps(5);
        population.setGapsClosed(20);

        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.of(gap));
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        CareGapEventResponse response = applicationService.closeGap(GAP_ID, TENANT_ID);

        // Then
        assertThat(response.getStatus()).isEqualTo("CLOSED");
        assertThat(response.getGapCode()).isEqualTo(GAP_CODE);
        assertThat(response.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(response.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void closeGap_ShouldThrowException_WhenGapNotFound() {
        // Given
        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> applicationService.closeGap(GAP_ID, TENANT_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Gap not found");
    }

    @Test
    void closeGap_ShouldPublishKafkaEvent() {
        // Given
        CareGapProjection gap = mock(CareGapProjection.class);
        when(gap.getPatientId()).thenReturn(PATIENT_ID);
        when(gap.getGapCode()).thenReturn(GAP_CODE);
        when(gap.getSeverity()).thenReturn("HIGH");

        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);

        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.of(gap));
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        applicationService.closeGap(GAP_ID, TENANT_ID);

        // Then
        verify(kafkaTemplate).send(eq("gap.events"), eq(PATIENT_ID), any());
    }

    // ==================== Population Health - closeGap() ====================

    @Test
    void closeGap_ShouldDecrementHighGaps_WhenSeverityHIGH() {
        // Given
        CareGapProjection gap = mock(CareGapProjection.class);
        when(gap.getPatientId()).thenReturn(PATIENT_ID);
        when(gap.getGapCode()).thenReturn(GAP_CODE);
        when(gap.getSeverity()).thenReturn("HIGH");

        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(10);
        population.setHighGaps(5);
        population.setGapsClosed(20);

        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.of(gap));
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        applicationService.closeGap(GAP_ID, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(9);   // decremented
        assertThat(saved.getHighGaps()).isEqualTo(4);        // decremented
        assertThat(saved.getGapsClosed()).isEqualTo(21);     // incremented
    }

    @Test
    void closeGap_ShouldDecrementCriticalGaps_WhenSeverityCRITICAL() {
        // Given
        CareGapProjection gap = mock(CareGapProjection.class);
        when(gap.getSeverity()).thenReturn("CRITICAL");
        when(gap.getPatientId()).thenReturn(PATIENT_ID);
        when(gap.getGapCode()).thenReturn(GAP_CODE);

        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(10);
        population.setCriticalGaps(3);
        population.setGapsClosed(15);

        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.of(gap));
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        applicationService.closeGap(GAP_ID, TENANT_ID);

        // Then
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(9);
        assertThat(saved.getCriticalGaps()).isEqualTo(2);  // decremented
        assertThat(saved.getGapsClosed()).isEqualTo(16);
    }

    @Test
    void closeGap_ShouldNotGoBelowZero_WhenCountAlreadyZero() {
        // Given - edge case: closing gap when count is already 0
        CareGapProjection gap = mock(CareGapProjection.class);
        when(gap.getSeverity()).thenReturn("MEDIUM");
        when(gap.getPatientId()).thenReturn(PATIENT_ID);
        when(gap.getGapCode()).thenReturn(GAP_CODE);

        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(0);  // Already 0
        population.setMediumGaps(0);     // Already 0
        population.setGapsClosed(0);

        when(gapRepository.findById(GAP_ID))
            .thenReturn(Optional.of(gap));
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        applicationService.closeGap(GAP_ID, TENANT_ID);

        // Then - should not go negative
        ArgumentCaptor<PopulationHealthProjection> captor = ArgumentCaptor.forClass(PopulationHealthProjection.class);
        verify(populationRepository).save(captor.capture());

        PopulationHealthProjection saved = captor.getValue();
        assertThat(saved.getTotalGapsOpen()).isEqualTo(0);   // stays at 0
        assertThat(saved.getMediumGaps()).isEqualTo(0);      // stays at 0
        assertThat(saved.getGapsClosed()).isEqualTo(1);      // still increments
    }

    // ==================== getPopulationHealth() Tests ====================

    @Test
    void getPopulationHealth_ShouldReturnMetrics_WhenDataExists() {
        // Given
        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(25);
        population.setCriticalGaps(5);
        population.setHighGaps(10);
        population.setMediumGaps(7);
        population.setLowGaps(3);
        population.setGapsClosed(50);
        population.calculateClosureRate();  // calculates 50/75 = 0.6666667

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        CareGapEventResponse response = applicationService.getPopulationHealth(TENANT_ID);

        // Then
        assertThat(response.getTotalGapsOpen()).isEqualTo(25);
        assertThat(response.getCriticalGaps()).isEqualTo(5);
        assertThat(response.getHighGaps()).isEqualTo(10);
        assertThat(response.getMediumGaps()).isEqualTo(7);
        assertThat(response.getLowGaps()).isEqualTo(3);
        assertThat(response.getGapsClosed()).isEqualTo(50);
        assertThat(response.getClosureRate()).isCloseTo(0.667f, within(0.001f));  // Use tolerance for floating point
    }

    @Test
    void getPopulationHealth_ShouldThrowException_WhenNoDataExists() {
        // Given
        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> applicationService.getPopulationHealth(TENANT_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No population health data found for tenant");
    }

    @Test
    void getPopulationHealth_ShouldHandleZeroGaps() {
        // Given - tenant with no gaps
        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(0);
        population.setCriticalGaps(0);
        population.setHighGaps(0);
        population.setMediumGaps(0);
        population.setLowGaps(0);
        population.setGapsClosed(0);
        population.calculateClosureRate();  // 0/0 = 0.0

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        CareGapEventResponse response = applicationService.getPopulationHealth(TENANT_ID);

        // Then
        assertThat(response.getTotalGapsOpen()).isEqualTo(0);
        assertThat(response.getClosureRate()).isEqualTo(0.0f);
    }

    @Test
    void getPopulationHealth_ShouldHandle100PercentClosure() {
        // Given - all gaps closed
        PopulationHealthProjection population = new PopulationHealthProjection(TENANT_ID);
        population.setTotalGapsOpen(0);
        population.setGapsClosed(100);
        population.calculateClosureRate();  // 100/100 = 1.0

        when(populationRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(population));

        // When
        CareGapEventResponse response = applicationService.getPopulationHealth(TENANT_ID);

        // Then
        assertThat(response.getClosureRate()).isEqualTo(1.0f);
        assertThat(response.getGapsClosed()).isEqualTo(100);
    }
}
