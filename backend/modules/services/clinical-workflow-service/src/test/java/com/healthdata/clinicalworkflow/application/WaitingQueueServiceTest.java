package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import com.healthdata.clinicalworkflow.domain.repository.WaitingQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    @Mock
    private WaitingQueueRepository queueRepository;

    @InjectMocks
    private WaitingQueueService queueService;

    private static final String TENANT_ID = "TENANT001";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private WaitingQueueEntity testQueueEntry;

    @BeforeEach
    void setUp() {
        testQueueEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .queuePosition(1)
                .priority("normal")
                .status("waiting")
                .build();
    }

    @Test
    void addToQueue_ShouldAddPatient_WhenNotInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());
        when(queueRepository.save(any())).thenReturn(testQueueEntry);
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(testQueueEntry));

        // When
        WaitingQueueEntity result = queueService.addToQueue(
                PATIENT_ID, "Apt123", TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        verify(queueRepository).save(any(WaitingQueueEntity.class));
    }

    @Test
    void addToQueue_ShouldThrowException_WhenAlreadyInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testQueueEntry));

        // When/Then
        assertThatThrownBy(() -> queueService.addToQueue(
                PATIENT_ID, "Apt123", TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in queue");
    }

    @Test
    void addToQueueWithPriority_ShouldSetPriority_WhenUrgent() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(Collections.emptyList());
        when(queueRepository.getEstimatedWaitTime(TENANT_ID, "urgent"))
                .thenReturn(null);

        // When
        WaitingQueueEntity result = queueService.addToQueueWithPriority(
                PATIENT_ID, "Apt123", "urgent", TENANT_ID);

        // Then
        assertThat(result.getPriority()).isEqualTo("urgent");
        assertThat(result.getEstimatedWaitMinutes()).isEqualTo(5); // Default for urgent
    }

    @Test
    void prioritizeQueue_ShouldReorderPatients_WhenCalled() {
        // Given
        WaitingQueueEntity urgent = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .priority("urgent")
                .status("waiting")
                .build();

        WaitingQueueEntity normal = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .priority("normal")
                .status("waiting")
                .build();

        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(urgent, normal)); // Already sorted
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        queueService.prioritizeQueue(TENANT_ID);

        // Then
        verify(queueRepository, times(2)).save(any(WaitingQueueEntity.class));
    }

    @Test
    void callPatient_ShouldUpdateStatus_WhenPatientInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testQueueEntry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        WaitingQueueEntity result = queueService.callPatient(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("called");
        assertThat(result.getCalledAt()).isNotNull();
    }

    @Test
    void callPatient_ShouldThrowException_WhenNotInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> queueService.callPatient(PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in queue");
    }

    @Test
    void removeFromQueue_ShouldMarkCompleted_WhenPatientInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testQueueEntry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(Collections.emptyList());

        // When
        queueService.removeFromQueue(PATIENT_ID, TENANT_ID);

        // Then
        verify(queueRepository).save(argThat(entry ->
                "completed".equals(entry.getStatus())));
    }

    @Test
    void calculateEstimatedWait_ShouldUseDefault_WhenNoHistoricalData() {
        // Given
        when(queueRepository.getEstimatedWaitTime(TENANT_ID, "urgent"))
                .thenReturn(null);

        // When
        Integer result = queueService.calculateEstimatedWait(TENANT_ID, "urgent");

        // Then
        assertThat(result).isEqualTo(5); // Default for urgent
    }

    @Test
    void calculateEstimatedWait_ShouldUseHistorical_WhenAvailable() {
        // Given
        when(queueRepository.getEstimatedWaitTime(TENANT_ID, "normal"))
                .thenReturn(25);

        // When
        Integer result = queueService.calculateEstimatedWait(TENANT_ID, "normal");

        // Then
        assertThat(result).isEqualTo(25);
    }

    @Test
    void getQueueStatus_ShouldReturnSummary() {
        // Given
        when(queueRepository.countWaitingPatients(TENANT_ID))
                .thenReturn(10L);
        when(queueRepository.findUrgentPatients(TENANT_ID))
                .thenReturn(List.of(testQueueEntry));
        when(queueRepository.getEstimatedWaitTime(TENANT_ID, "normal"))
                .thenReturn(30);

        // When
        WaitingQueueService.QueueStatus result = queueService.getQueueStatus(TENANT_ID);

        // Then
        assertThat(result.getTotalWaiting()).isEqualTo(10L);
        assertThat(result.getUrgentCount()).isEqualTo(1L);
        assertThat(result.getAverageWaitMinutes()).isEqualTo(30);
    }

    @Test
    void getWaitingPatients_ShouldReturnPatients() {
        // Given
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(testQueueEntry));

        // When
        List<WaitingQueueEntity> result = queueService.getWaitingPatients(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getNextPatient_ShouldReturnNextInQueue() {
        // Given
        when(queueRepository.findNextPatientInQueue(TENANT_ID))
                .thenReturn(Optional.of(testQueueEntry));

        // When
        WaitingQueueEntity result = queueService.getNextPatient(TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQueuePosition()).isEqualTo(1);
    }

    @Test
    void getNextPatient_ShouldReturnNull_WhenQueueEmpty() {
        // Given
        when(queueRepository.findNextPatientInQueue(TENANT_ID))
                .thenReturn(Optional.empty());

        // When
        WaitingQueueEntity result = queueService.getNextPatient(TENANT_ID);

        // Then
        assertThat(result).isNull();
    }
}
