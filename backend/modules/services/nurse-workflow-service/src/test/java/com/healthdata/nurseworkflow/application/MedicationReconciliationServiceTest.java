package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.MedicationReconciliationEntity;
import com.healthdata.nurseworkflow.domain.repository.MedicationReconciliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MedicationReconciliationService
 *
 * Tests medication reconciliation workflow including:
 * - Starting reconciliation process
 * - Tracking discrepancies and medication changes
 * - Patient education and teach-back assessment
 * - Completion and follow-up scheduling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationReconciliationService")
class MedicationReconciliationServiceTest {

    @Mock
    private MedicationReconciliationRepository medRecRepository;

    @InjectMocks
    private MedicationReconciliationService medRecService;

    private String tenantId;
    private UUID patientId;
    private UUID reconcilerId;
    private MedicationReconciliationEntity testMedRec;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT001";
        patientId = UUID.randomUUID();
        reconcilerId = UUID.randomUUID();

        testMedRec = MedicationReconciliationEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .reconcilerId(reconcilerId)
            .status(MedicationReconciliationEntity.ReconciliationStatus.IN_PROGRESS)
            .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE)
            .medicationCount(5)
            .discrepancyCount(1)
            .patientEducationProvided(true)
            .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.GOOD)
            .startedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("should start medication reconciliation")
    void testStartReconciliation_Success() {
        // Given
        when(medRecRepository.save(any(MedicationReconciliationEntity.class)))
            .thenReturn(testMedRec);

        // When
        MedicationReconciliationEntity result = medRecService.startReconciliation(testMedRec);

        // Then
        assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("tenantId", tenantId)
            .hasFieldOrPropertyWithValue("patientId", patientId)
            .hasFieldOrPropertyWithValue("status",
                MedicationReconciliationEntity.ReconciliationStatus.IN_PROGRESS);

        verify(medRecRepository, times(1)).save(any(MedicationReconciliationEntity.class));
    }

    @Test
    @DisplayName("should complete medication reconciliation")
    void testCompleteReconciliation_Success() {
        // Given
        testMedRec.setStatus(MedicationReconciliationEntity.ReconciliationStatus.COMPLETED);
        testMedRec.setCompletedAt(Instant.now());

        when(medRecRepository.save(testMedRec))
            .thenReturn(testMedRec);

        // When
        MedicationReconciliationEntity result = medRecService.completeReconciliation(testMedRec);

        // Then
        assertThat(result.getStatus())
            .isEqualTo(MedicationReconciliationEntity.ReconciliationStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();

        verify(medRecRepository, times(1)).save(testMedRec);
    }

    @Test
    @DisplayName("should get medication reconciliation by ID")
    void testGetMedicationReconciliationById_Success() {
        // Given
        when(medRecRepository.findById(testMedRec.getId()))
            .thenReturn(Optional.of(testMedRec));

        // When
        Optional<MedicationReconciliationEntity> result =
            medRecService.getMedicationReconciliationById(testMedRec.getId());

        // Then
        assertThat(result)
            .isPresent()
            .hasValue(testMedRec);

        verify(medRecRepository, times(1)).findById(testMedRec.getId());
    }

    @Test
    @DisplayName("should get pending medication reconciliations")
    void testGetPendingReconciliations_Success() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicationReconciliationEntity> mockPage =
            new PageImpl<>(List.of(testMedRec), pageRequest, 1);

        when(medRecRepository.findPendingByTenant(tenantId, pageRequest))
            .thenReturn(mockPage);

        // When
        Page<MedicationReconciliationEntity> result =
            medRecService.getPendingReconciliations(tenantId, pageRequest);

        // Then
        assertThat(result)
            .isNotEmpty()
            .hasSize(1)
            .contains(testMedRec);
    }

    @Test
    @DisplayName("should get medication reconciliations by trigger type")
    void testGetReconciliationsByTriggerType_Success() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicationReconciliationEntity> mockPage =
            new PageImpl<>(List.of(testMedRec), pageRequest, 1);

        when(medRecRepository.findByTenantIdAndTriggerTypeOrderByStartedAtDesc(
            tenantId, MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE, pageRequest))
            .thenReturn(mockPage);

        // When
        Page<MedicationReconciliationEntity> result =
            medRecService.getReconciliationsByTriggerType(tenantId,
                MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE, pageRequest);

        // Then
        assertThat(result)
            .isNotEmpty()
            .extracting(MedicationReconciliationEntity::getTriggerType)
            .containsOnly(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE);
    }

    @Test
    @DisplayName("should get patient medication reconciliation history")
    void testGetPatientMedicationReconciliationHistory_Success() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicationReconciliationEntity> mockPage =
            new PageImpl<>(List.of(testMedRec), pageRequest, 1);

        when(medRecRepository.findByTenantIdAndPatientIdOrderByStartedAtDesc(
            tenantId, patientId, pageRequest))
            .thenReturn(mockPage);

        // When
        Page<MedicationReconciliationEntity> result =
            medRecService.getPatientMedicationReconciliationHistory(
                tenantId, patientId, pageRequest);

        // Then
        assertThat(result)
            .isNotEmpty()
            .hasSize(1);
    }

    @Test
    @DisplayName("should find medication reconciliations with poor understanding")
    void testFindPoorUnderstandingReconciliations_Success() {
        // Given
        MedicationReconciliationEntity poorUnderstanding =
            testMedRec.toBuilder()
                .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.POOR)
                .build();

        when(medRecRepository.findWithPoorUnderstanding(tenantId))
            .thenReturn(List.of(poorUnderstanding));

        // When
        List<MedicationReconciliationEntity> result =
            medRecService.findWithPoorUnderstanding(tenantId);

        // Then
        assertThat(result)
            .hasSize(1)
            .extracting(MedicationReconciliationEntity::getPatientUnderstanding)
            .containsOnly(MedicationReconciliationEntity.PatientUnderstanding.POOR);
    }

    @Test
    @DisplayName("should count pending medication reconciliations")
    void testCountPendingReconciliations_Success() {
        // Given
        when(medRecRepository.countByTenantIdAndStatusIn(
            tenantId,
            List.of(
                MedicationReconciliationEntity.ReconciliationStatus.REQUESTED,
                MedicationReconciliationEntity.ReconciliationStatus.IN_PROGRESS
            )))
            .thenReturn(3L);

        // When
        long result = medRecService.countPendingReconciliations(tenantId);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("should verify multi-tenant isolation")
    void testMultiTenantIsolation() {
        // Given - two different tenants
        String tenant1 = "TENANT001";
        String tenant2 = "TENANT002";
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MedicationReconciliationEntity> tenant1Page =
            new PageImpl<>(List.of(testMedRec), pageRequest, 1);
        Page<MedicationReconciliationEntity> tenant2Page =
            new PageImpl<>(List.of(), pageRequest, 0);

        when(medRecRepository.findByTenantIdAndPatientIdOrderByStartedAtDesc(
            tenant1, patientId, pageRequest))
            .thenReturn(tenant1Page);

        when(medRecRepository.findByTenantIdAndPatientIdOrderByStartedAtDesc(
            tenant2, patientId, pageRequest))
            .thenReturn(tenant2Page);

        // When
        Page<MedicationReconciliationEntity> result1 =
            medRecService.getPatientMedicationReconciliationHistory(
                tenant1, patientId, pageRequest);
        Page<MedicationReconciliationEntity> result2 =
            medRecService.getPatientMedicationReconciliationHistory(
                tenant2, patientId, pageRequest);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(0);
    }

    @Test
    @DisplayName("should update medication reconciliation")
    void testUpdateReconciliation_Success() {
        // Given
        testMedRec.setNotes("Patient educated on new medication side effects");
        when(medRecRepository.save(testMedRec))
            .thenReturn(testMedRec);

        // When
        MedicationReconciliationEntity result = medRecService.updateReconciliation(testMedRec);

        // Then
        assertThat(result.getNotes())
            .isEqualTo("Patient educated on new medication side effects");

        verify(medRecRepository, times(1)).save(testMedRec);
    }
}
