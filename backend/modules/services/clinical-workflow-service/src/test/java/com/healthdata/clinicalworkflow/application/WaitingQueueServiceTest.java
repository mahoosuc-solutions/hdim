package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.QueueEntryRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.QueuePositionResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueStatusResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueWaitTimeResponse;
import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import com.healthdata.clinicalworkflow.domain.repository.WaitingQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
        WaitingQueueEntity result = queueService.addToQueueInternal(
                PATIENT_ID, "Apt123", TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        verify(queueRepository, atLeastOnce()).save(any(WaitingQueueEntity.class));
    }

    @Test
    void addToQueue_ShouldThrowException_WhenAlreadyInQueue() {
        // Given
        when(queueRepository.findPatientQueuePosition(PATIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(testQueueEntry));

        // When/Then
        assertThatThrownBy(() -> queueService.addToQueueInternal(
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
        WaitingQueueService.QueueStatus result = queueService.getQueueStatusInternal(TENANT_ID);

        // Then
        assertThat(result.getTotalWaiting()).isEqualTo(10);
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

    // ========== Tests for Controller-Facing DTO Methods (Tier 1 Fixes) ==========

    @Test
    void getQueueStatus_ShouldReturnQueueStatusResponse_WhenCalled() {
        // Given - 4a: Fix getQueueStatus return type (Line 72)
        WaitingQueueEntity urgent = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("CHECK_IN")
                .priority("urgent")
                .status("waiting")
                .queuePosition(1)
                .estimatedWaitMinutes(5)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity normal = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("VITALS")
                .priority("normal")
                .status("waiting")
                .queuePosition(2)
                .estimatedWaitMinutes(15)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(urgent, normal));

        // When
        QueueStatusResponse result = queueService.getQueueStatus(TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPatients()).isEqualTo(2);
        assertThat(result.getCheckInQueueCount()).isEqualTo(1);
        assertThat(result.getVitalsQueueCount()).isEqualTo(1);
        assertThat(result.getAverageWaitMinutes()).isEqualTo(10); // (5 + 15) / 2
        assertThat(result.getLongestWaitMinutes()).isEqualTo(15);
        assertThat(result.getQueueEntries()).hasSize(2);
        assertThat(result.getCountsByPriority()).containsKeys("urgent", "normal");
    }

    @Test
    void addToQueue_ShouldProcessQueueEntryRequest_WhenValidRequest() {
        // Given - 4b: Fix addToQueue adapter (Line 126)
        String patientIdStr = UUID.randomUUID().toString();
        QueueEntryRequest request = QueueEntryRequest.builder()
                .patientId(patientIdStr)
                .encounterId("ENC001")
                .priority("high")
                .visitType("urgent-care")
                .build();

        WaitingQueueEntity savedEntity = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.fromString(patientIdStr))
                .appointmentId("ENC001")
                .priority("high")
                .status("waiting")
                .queuePosition(1)
                .estimatedWaitMinutes(10)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findPatientQueuePosition(any(UUID.class), eq(TENANT_ID)))
                .thenReturn(Optional.empty());
        when(queueRepository.save(any())).thenReturn(savedEntity);
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(savedEntity));
        when(queueRepository.getEstimatedWaitTime(TENANT_ID, "high"))
                .thenReturn(10);

        // When
        QueuePositionResponse result = queueService.addToQueue(TENANT_ID, request, "USER001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(patientIdStr);
        assertThat(result.getEncounterId()).isEqualTo("ENC001");
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getPosition()).isEqualTo(1);
        verify(queueRepository, atLeastOnce()).save(any(WaitingQueueEntity.class));
    }

    @Test
    void addToQueue_ShouldThrowException_WhenInvalidPatientId() {
        // Given - 4b: Invalid UUID format
        QueueEntryRequest request = QueueEntryRequest.builder()
                .patientId("invalid-uuid")
                .encounterId("ENC001")
                .priority("normal")
                .build();

        // When/Then
        assertThatThrownBy(() -> queueService.addToQueue(TENANT_ID, request, "USER001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid patient ID format");
    }

    @Test
    void getPatientQueueInfo_ShouldReturnQueuePositionResponse_WhenPatientInQueue() {
        // Given - 4c: Fix getPatientQueueInfo parameter types (Line 158)
        String patientIdStr = UUID.randomUUID().toString();
        UUID patientUuid = UUID.fromString(patientIdStr);

        WaitingQueueEntity queueEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientUuid)
                .appointmentId("ENC001")
                .priority("normal")
                .status("waiting")
                .queuePosition(3)
                .estimatedWaitMinutes(25)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findPatientQueuePosition(patientUuid, TENANT_ID))
                .thenReturn(Optional.of(queueEntry));

        // When
        QueuePositionResponse result = queueService.getPatientQueueInfo(TENANT_ID, patientIdStr);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(patientIdStr);
        assertThat(result.getPosition()).isEqualTo(3);
        assertThat(result.getPatientsAhead()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo("WAITING");
    }

    @Test
    void getPatientQueueInfo_ShouldThrowException_WhenPatientNotInQueue() {
        // Given - 4c: Patient not found
        String patientIdStr = UUID.randomUUID().toString();
        UUID patientUuid = UUID.fromString(patientIdStr);

        when(queueRepository.findPatientQueuePosition(patientUuid, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> queueService.getPatientQueueInfo(TENANT_ID, patientIdStr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not in queue");
    }

    @Test
    void callPatient_ShouldReturnQueuePositionResponse_WhenPatientCalled() {
        // Given - 4d: Fix callPatient signature (Line 192)
        String patientIdStr = UUID.randomUUID().toString();
        UUID patientUuid = UUID.fromString(patientIdStr);

        WaitingQueueEntity queueEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientUuid)
                .appointmentId("ENC001")
                .priority("normal")
                .status("waiting")
                .queuePosition(1)
                .estimatedWaitMinutes(30)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findPatientQueuePosition(patientUuid, TENANT_ID))
                .thenReturn(Optional.of(queueEntry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        QueuePositionResponse result = queueService.callPatient(TENANT_ID, patientIdStr, "USER001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(patientIdStr);
        assertThat(result.getStatus()).isEqualTo("CALLED");
        assertThat(result.getCalledAt()).isNotNull();
        verify(queueRepository).save(argThat(entry ->
                "called".equals(entry.getStatus()) && entry.getCalledAt() != null));
    }

    @Test
    void removeFromQueue_ShouldCompleteRemoval_WhenPatientRemoved() {
        // Given - 4e: Fix removeFromQueue signature (Line 225)
        String patientIdStr = UUID.randomUUID().toString();
        UUID patientUuid = UUID.fromString(patientIdStr);

        WaitingQueueEntity queueEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientUuid)
                .appointmentId("ENC001")
                .priority("normal")
                .status("called")
                .queuePosition(1)
                .estimatedWaitMinutes(30)
                .enteredQueueAt(Instant.now())
                .calledAt(Instant.now())
                .build();

        when(queueRepository.findPatientQueuePosition(patientUuid, TENANT_ID))
                .thenReturn(Optional.of(queueEntry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(Collections.emptyList());

        // When
        queueService.removeFromQueue(TENANT_ID, patientIdStr, "USER001");

        // Then
        verify(queueRepository).save(argThat(entry ->
                "completed".equals(entry.getStatus()) && entry.getExitedQueueAt() != null));
    }

    @Test
    void getWaitTimes_ShouldReturnWaitTimeResponse_WhenCalled() {
        // Given - 4f: Add getWaitTimes method (Line 250)
        WaitingQueueEntity checkInEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("CHECK_IN")
                .priority("normal")
                .status("waiting")
                .estimatedWaitMinutes(10)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity vitalsEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("VITALS")
                .priority("normal")
                .status("waiting")
                .estimatedWaitMinutes(15)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity providerEntry = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("PROVIDER")
                .priority("high")
                .status("waiting")
                .estimatedWaitMinutes(20)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(checkInEntry, vitalsEntry, providerEntry));

        // When
        QueueWaitTimeResponse result = queueService.getWaitTimes(TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCheckInWaitMinutes()).isEqualTo(10);
        assertThat(result.getVitalsWaitMinutes()).isEqualTo(15);
        assertThat(result.getProviderWaitMinutes()).isEqualTo(20);
        assertThat(result.getCheckoutWaitMinutes()).isEqualTo(0); // No checkout patients
        assertThat(result.getTotalEstimatedMinutes()).isEqualTo(45);
        assertThat(result.getAverageWaitMinutes()).isEqualTo(11); // 45 / 4
    }

    @Test
    void getQueueByPriority_ShouldReturnGroupedMap_WhenCalled() {
        // Given - 4g: Fix getQueueByPriority return type (Line 276)
        WaitingQueueEntity urgent = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("CHECK_IN")
                .priority("urgent")
                .status("waiting")
                .queuePosition(1)
                .estimatedWaitMinutes(5)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity normal1 = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("VITALS")
                .priority("normal")
                .status("waiting")
                .queuePosition(2)
                .estimatedWaitMinutes(15)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity normal2 = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("PROVIDER")
                .priority("normal")
                .status("waiting")
                .queuePosition(3)
                .estimatedWaitMinutes(20)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(urgent, normal1, normal2));

        // When
        Map<String, List<QueuePositionResponse>> result = queueService.getQueueByPriority(
                TENANT_ID, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("urgent", "normal");
        assertThat(result.get("urgent")).hasSize(1);
        assertThat(result.get("normal")).hasSize(2);
        assertThat(result.get("urgent").get(0).getPriority()).isEqualTo("URGENT");
        assertThat(result.get("normal").get(0).getPosition()).isEqualTo(2);
    }

    @Test
    void reorderQueue_ShouldReturnQueueStatusResponse_AfterReordering() {
        // Given - 4h: Add reorderQueue method (Line 303)
        WaitingQueueEntity entry1 = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("CHECK_IN")
                .priority("normal")
                .status("waiting")
                .queuePosition(2)
                .estimatedWaitMinutes(15)
                .enteredQueueAt(Instant.now())
                .build();

        WaitingQueueEntity entry2 = WaitingQueueEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .appointmentId("VITALS")
                .priority("urgent")
                .status("waiting")
                .queuePosition(1)
                .estimatedWaitMinutes(5)
                .enteredQueueAt(Instant.now())
                .build();

        when(queueRepository.findWaitingPatientsByTenant(TENANT_ID))
                .thenReturn(List.of(entry2, entry1)); // Sorted by priority
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        QueueStatusResponse result = queueService.reorderQueue(TENANT_ID, "USER001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPatients()).isEqualTo(2);
        assertThat(result.getQueueEntries()).hasSize(2);
        verify(queueRepository, times(2)).save(any(WaitingQueueEntity.class));
    }
}
