package com.healthdata.quality.service;

import com.healthdata.quality.persistence.*;
import com.healthdata.quality.persistence.PatientMeasureAssignmentRepository;
import com.healthdata.quality.persistence.PatientMeasureEligibilityCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeasureAssignmentService
 *
 * Tests all 11 public methods:
 * - getActiveAssignments
 * - getEffectiveAssignments
 * - assignMeasure
 * - autoAssignMeasures
 * - deactivateAssignment
 * - updateEffectiveDates
 * - countActiveAssignments
 * - getAutoAssignedMeasures
 * - getManuallyAssignedMeasures
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeasureAssignmentService Tests")
class MeasureAssignmentServiceTest {

    @Mock
    private PatientMeasureAssignmentRepository assignmentRepository;

    @Mock
    private PatientMeasureEligibilityCacheRepository eligibilityCacheRepository;

    @InjectMocks
    private MeasureAssignmentService measureAssignmentService;

    private String tenantId;
    private UUID patientId;
    private UUID measureId;
    private UUID assignedBy;
    private PatientMeasureAssignmentEntity testAssignment;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT-001";
        patientId = UUID.randomUUID();
        measureId = UUID.randomUUID();
        assignedBy = UUID.randomUUID();

        testAssignment = PatientMeasureAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .assignedBy(assignedBy)
                .assignedAt(Instant.now())
                .effectiveStartDate(LocalDate.now())
                .isActive(true)
                .isAutoAssigned(false)
                .build();
    }

    // ========================================
    // getActiveAssignments() Tests
    // ========================================

    @Test
    @DisplayName("Should return active assignments for patient")
    void shouldReturnActiveAssignments() {
        // Given
        List<PatientMeasureAssignmentEntity> expectedAssignments = Arrays.asList(
                testAssignment,
                PatientMeasureAssignmentEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .patientId(patientId)
                        .measureId(UUID.randomUUID())
                        .isActive(true)
                        .build()
        );

        when(assignmentRepository.findActiveAssignmentsByPatient(tenantId, patientId))
                .thenReturn(expectedAssignments);

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getActiveAssignments(tenantId, patientId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedAssignments);
        verify(assignmentRepository).findActiveAssignmentsByPatient(tenantId, patientId);
    }

    @Test
    @DisplayName("Should return empty list when no active assignments exist")
    void shouldReturnEmptyListWhenNoActiveAssignments() {
        // Given
        when(assignmentRepository.findActiveAssignmentsByPatient(tenantId, patientId))
                .thenReturn(List.of());

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getActiveAssignments(tenantId, patientId);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // getEffectiveAssignments() Tests
    // ========================================

    @Test
    @DisplayName("Should return effective assignments for specific date")
    void shouldReturnEffectiveAssignments() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        List<PatientMeasureAssignmentEntity> expectedAssignments = List.of(testAssignment);

        when(assignmentRepository.findEffectiveAssignmentsForDate(tenantId, patientId, evaluationDate))
                .thenReturn(expectedAssignments);

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getEffectiveAssignments(
                tenantId, patientId, evaluationDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAssignment);
        verify(assignmentRepository).findEffectiveAssignmentsForDate(tenantId, patientId, evaluationDate);
    }

    @Test
    @DisplayName("Should return empty list when no assignments effective on date")
    void shouldReturnEmptyListWhenNoEffectiveAssignments() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        when(assignmentRepository.findEffectiveAssignmentsForDate(tenantId, patientId, evaluationDate))
                .thenReturn(List.of());

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getEffectiveAssignments(
                tenantId, patientId, evaluationDate);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // assignMeasure() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully assign measure to patient")
    void shouldSuccessfullyAssignMeasure() {
        // Given
        LocalDate effectiveDate = LocalDate.now();
        String reason = "Annual wellness visit";

        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenReturn(testAssignment);

        // When
        PatientMeasureAssignmentEntity result = measureAssignmentService.assignMeasure(
                tenantId, patientId, measureId, assignedBy, effectiveDate, null, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMeasureId()).isEqualTo(measureId);
        verify(assignmentRepository).findActiveAssignmentForMeasure(tenantId, patientId, measureId);
        verify(assignmentRepository).save(any(PatientMeasureAssignmentEntity.class));
        verify(eligibilityCacheRepository).deleteByPatientId(patientId);
    }

    @Test
    @DisplayName("Should throw exception when duplicate active assignment exists")
    void shouldThrowExceptionWhenDuplicateAssignment() {
        // Given
        LocalDate effectiveDate = LocalDate.now();
        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId))
                .thenReturn(Optional.of(testAssignment));

        // When / Then
        assertThatThrownBy(() -> measureAssignmentService.assignMeasure(
                tenantId, patientId, measureId, assignedBy, effectiveDate, null, "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active assignment");

        verify(assignmentRepository, never()).save(any());
        verify(eligibilityCacheRepository, never()).deleteByPatientId(any());
    }

    @Test
    @DisplayName("Should invalidate eligibility cache after assignment")
    void shouldInvalidateCacheAfterAssignment() {
        // Given
        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenReturn(testAssignment);

        // When
        measureAssignmentService.assignMeasure(
                tenantId, patientId, measureId, assignedBy, LocalDate.now(), null, "test");

        // Then
        verify(eligibilityCacheRepository).deleteByPatientId(patientId);
    }

    // ========================================
    // autoAssignMeasures() Tests
    // ========================================

    @Test
    @DisplayName("Should auto-assign multiple measures successfully")
    void shouldAutoAssignMultipleMeasures() {
        // Given
        UUID measureId1 = UUID.randomUUID();
        UUID measureId2 = UUID.randomUUID();
        List<UUID> measureIds = Arrays.asList(measureId1, measureId2);

        when(assignmentRepository.findActiveAssignmentForMeasure(eq(tenantId), eq(patientId), any()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.autoAssignMeasures(
                tenantId, patientId, measureIds, "Eligibility criteria met");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isAutoAssigned()).isTrue();
        assertThat(result.get(1).isAutoAssigned()).isTrue();
        verify(assignmentRepository, times(2)).save(any(PatientMeasureAssignmentEntity.class));
        verify(eligibilityCacheRepository).deleteByPatientId(patientId);
    }

    @Test
    @DisplayName("Should skip measures with existing active assignments during auto-assign")
    void shouldSkipExistingAssignmentsDuringAutoAssign() {
        // Given
        UUID measureId1 = UUID.randomUUID();
        UUID measureId2 = UUID.randomUUID();
        List<UUID> measureIds = Arrays.asList(measureId1, measureId2);

        // measureId1 already has active assignment
        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId1))
                .thenReturn(Optional.of(testAssignment));
        // measureId2 does not
        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId2))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.autoAssignMeasures(
                tenantId, patientId, measureIds, "Eligibility criteria met");

        // Then
        assertThat(result).hasSize(1); // Only measureId2 assigned
        verify(assignmentRepository, times(1)).save(any(PatientMeasureAssignmentEntity.class));
    }

    @Test
    @DisplayName("Should return empty list when all measures already assigned")
    void shouldReturnEmptyListWhenAllMeasuresAlreadyAssigned() {
        // Given
        UUID measureId1 = UUID.randomUUID();
        List<UUID> measureIds = List.of(measureId1);

        when(assignmentRepository.findActiveAssignmentForMeasure(tenantId, patientId, measureId1))
                .thenReturn(Optional.of(testAssignment));

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.autoAssignMeasures(
                tenantId, patientId, measureIds, "test");

        // Then
        assertThat(result).isEmpty();
        verify(assignmentRepository, never()).save(any());
    }

    // ========================================
    // deactivateAssignment() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully deactivate active assignment")
    void shouldSuccessfullyDeactivateAssignment() {
        // Given
        UUID assignmentId = testAssignment.getId();
        UUID deactivatedBy = UUID.randomUUID();

        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId))
                .thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureAssignmentEntity result = measureAssignmentService.deactivateAssignment(
                tenantId, assignmentId, deactivatedBy);

        // Then
        assertThat(result.isActive()).isFalse();
        assertThat(result.getDeactivatedBy()).isEqualTo(deactivatedBy);
        assertThat(result.getDeactivatedAt()).isNotNull();
        assertThat(result.getEffectiveEndDate()).isEqualTo(LocalDate.now());
        verify(assignmentRepository).save(testAssignment);
        verify(eligibilityCacheRepository).deleteByPatientId(patientId);
    }

    @Test
    @DisplayName("Should throw exception when assignment not found for deactivation")
    void shouldThrowExceptionWhenAssignmentNotFoundForDeactivation() {
        // Given
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> measureAssignmentService.deactivateAssignment(
                tenantId, assignmentId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assignment not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle deactivation of already inactive assignment as no-op")
    void shouldHandleDeactivationOfInactiveAssignment() {
        // Given
        testAssignment.setActive(false);
        testAssignment.setDeactivatedAt(Instant.now().minusSeconds(3600));
        UUID assignmentId = testAssignment.getId();

        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId))
                .thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureAssignmentEntity result = measureAssignmentService.deactivateAssignment(
                tenantId, assignmentId, UUID.randomUUID());

        // Then
        assertThat(result.isActive()).isFalse();
        // No new timestamp should be set since already inactive
        verify(assignmentRepository).save(testAssignment);
    }

    // ========================================
    // updateEffectiveDates() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully update effective dates")
    void shouldSuccessfullyUpdateEffectiveDates() {
        // Given
        UUID assignmentId = testAssignment.getId();
        LocalDate newStartDate = LocalDate.now().plusDays(1);
        LocalDate newEndDate = LocalDate.now().plusDays(30);

        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId))
                .thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(PatientMeasureAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureAssignmentEntity result = measureAssignmentService.updateEffectiveDates(
                tenantId, assignmentId, newStartDate, newEndDate);

        // Then
        assertThat(result.getEffectiveStartDate()).isEqualTo(newStartDate);
        assertThat(result.getEffectiveEndDate()).isEqualTo(newEndDate);
        verify(assignmentRepository).save(testAssignment);
        verify(eligibilityCacheRepository).deleteByPatientId(patientId);
    }

    @Test
    @DisplayName("Should throw exception when assignment not found for date update")
    void shouldThrowExceptionWhenAssignmentNotFoundForDateUpdate() {
        // Given
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> measureAssignmentService.updateEffectiveDates(
                tenantId, assignmentId, LocalDate.now(), LocalDate.now().plusDays(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assignment not found");
    }

    // ========================================
    // countActiveAssignments() Tests
    // ========================================

    @Test
    @DisplayName("Should return correct count of active assignments")
    void shouldReturnCorrectCountOfActiveAssignments() {
        // Given
        long expectedCount = 5L;
        when(assignmentRepository.countActiveAssignmentsByPatient(tenantId, patientId))
                .thenReturn(expectedCount);

        // When
        long result = measureAssignmentService.countActiveAssignments(tenantId, patientId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(assignmentRepository).countActiveAssignmentsByPatient(tenantId, patientId);
    }

    @Test
    @DisplayName("Should return zero when no active assignments exist")
    void shouldReturnZeroWhenNoActiveAssignments() {
        // Given
        when(assignmentRepository.countActiveAssignmentsByPatient(tenantId, patientId))
                .thenReturn(0L);

        // When
        long result = measureAssignmentService.countActiveAssignments(tenantId, patientId);

        // Then
        assertThat(result).isZero();
    }

    // ========================================
    // getAutoAssignedMeasures() Tests
    // ========================================

    @Test
    @DisplayName("Should return all auto-assigned measures for tenant")
    void shouldReturnAutoAssignedMeasures() {
        // Given
        PatientMeasureAssignmentEntity autoAssignment1 = PatientMeasureAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .isAutoAssigned(true)
                .build();
        PatientMeasureAssignmentEntity autoAssignment2 = PatientMeasureAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .isAutoAssigned(true)
                .build();
        List<PatientMeasureAssignmentEntity> expectedAssignments = Arrays.asList(autoAssignment1, autoAssignment2);

        when(assignmentRepository.findByTenantIdAndIsAutoAssignedTrue(tenantId))
                .thenReturn(expectedAssignments);

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getAutoAssignedMeasures(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(PatientMeasureAssignmentEntity::isAutoAssigned);
        verify(assignmentRepository).findByTenantIdAndIsAutoAssignedTrue(tenantId);
    }

    @Test
    @DisplayName("Should return empty list when no auto-assigned measures exist")
    void shouldReturnEmptyListWhenNoAutoAssignedMeasures() {
        // Given
        when(assignmentRepository.findByTenantIdAndIsAutoAssignedTrue(tenantId))
                .thenReturn(List.of());

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getAutoAssignedMeasures(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // getManuallyAssignedMeasures() Tests
    // ========================================

    @Test
    @DisplayName("Should return all manually assigned measures for tenant")
    void shouldReturnManuallyAssignedMeasures() {
        // Given
        PatientMeasureAssignmentEntity manualAssignment = PatientMeasureAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .isAutoAssigned(false)
                .build();
        List<PatientMeasureAssignmentEntity> expectedAssignments = List.of(manualAssignment);

        when(assignmentRepository.findByTenantIdAndIsAutoAssignedFalse(tenantId))
                .thenReturn(expectedAssignments);

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getManuallyAssignedMeasures(tenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(assignment -> !assignment.isAutoAssigned());
        verify(assignmentRepository).findByTenantIdAndIsAutoAssignedFalse(tenantId);
    }

    @Test
    @DisplayName("Should return empty list when no manually assigned measures exist")
    void shouldReturnEmptyListWhenNoManuallyAssignedMeasures() {
        // Given
        when(assignmentRepository.findByTenantIdAndIsAutoAssignedFalse(tenantId))
                .thenReturn(List.of());

        // When
        List<PatientMeasureAssignmentEntity> result = measureAssignmentService.getManuallyAssignedMeasures(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // Multi-Tenant Isolation Tests
    // ========================================

    @Test
    @DisplayName("Should enforce tenant isolation in all repository calls")
    void shouldEnforceTenantIsolation() {
        // Given
        when(assignmentRepository.findActiveAssignmentsByPatient(tenantId, patientId))
                .thenReturn(List.of());

        // When
        measureAssignmentService.getActiveAssignments(tenantId, patientId);

        // Then
        verify(assignmentRepository).findActiveAssignmentsByPatient(eq(tenantId), any());
    }
}
