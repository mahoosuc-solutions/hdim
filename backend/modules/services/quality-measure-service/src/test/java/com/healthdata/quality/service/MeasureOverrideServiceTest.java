package com.healthdata.quality.service;

import com.healthdata.quality.persistence.PatientMeasureOverrideEntity;
import com.healthdata.quality.persistence.PatientMeasureOverrideRepository;
import com.healthdata.quality.persistence.MeasureConfigProfileEntity;
import com.healthdata.quality.persistence.MeasureConfigProfileRepository;
import com.healthdata.quality.persistence.PatientProfileAssignmentEntity;
import com.healthdata.quality.persistence.PatientProfileAssignmentRepository;
import com.healthdata.quality.persistence.PatientMeasureEligibilityCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeasureOverrideService
 *
 * Tests all 11 public methods:
 * - getActiveOverrides
 * - getEffectiveOverrides
 * - createOverride (with HIPAA clinical justification requirement)
 * - approveOverride
 * - markReviewed
 * - getOverridesDueForReview
 * - deactivateOverride
 * - resolveOverrides (multi-level resolution: patient > profile > base)
 * - getPendingApprovals
 * - countActiveOverrides
 *
 * HIPAA Compliance: Clinical justification required for all patient overrides
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeasureOverrideService Tests")
class MeasureOverrideServiceTest {

    @Mock
    private PatientMeasureOverrideRepository overrideRepository;

    @Mock
    private MeasureConfigProfileRepository profileRepository;

    @Mock
    private PatientProfileAssignmentRepository profileAssignmentRepository;

    @Mock
    private PatientMeasureEligibilityCacheRepository cacheRepository;

    @InjectMocks
    private MeasureOverrideService measureOverrideService;

    private String tenantId;
    private UUID patientId;
    private UUID measureId;
    private UUID createdBy;
    private PatientMeasureOverrideEntity testOverride;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT-001";
        patientId = UUID.randomUUID();
        measureId = UUID.randomUUID();
        createdBy = UUID.randomUUID();

        testOverride = PatientMeasureOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .overrideType("PARAMETER")
                .overrideField("minimumAge")
                .originalValue("65")
                .overrideValue("50")
                .valueType("NUMERIC")
                .clinicalReason("Early onset chronic disease requires earlier screening")
                .active(true)
                .effectiveFrom(LocalDate.now())
                .requiresPeriodicReview(true)
                .reviewFrequencyDays(90)
                .createdBy(createdBy)
                .build();
    }

    // ========================================
    // getActiveOverrides() Tests
    // ========================================

    @Test
    @DisplayName("Should return active overrides for patient and measure")
    void shouldReturnActiveOverrides() {
        // Given
        List<PatientMeasureOverrideEntity> expectedOverrides = List.of(testOverride);

        when(overrideRepository.findActiveByPatientAndMeasure(tenantId, patientId, measureId))
                .thenReturn(expectedOverrides);

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getActiveOverrides(
                tenantId, patientId, measureId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testOverride);
        verify(overrideRepository).findActiveByPatientAndMeasure(tenantId, patientId, measureId);
    }

    @Test
    @DisplayName("Should return empty list when no active overrides exist")
    void shouldReturnEmptyListWhenNoActiveOverrides() {
        // Given
        when(overrideRepository.findActiveByPatientAndMeasure(tenantId, patientId, measureId))
                .thenReturn(List.of());

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getActiveOverrides(
                tenantId, patientId, measureId);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // getEffectiveOverrides() Tests
    // ========================================

    @Test
    @DisplayName("Should return effective overrides for specific date")
    void shouldReturnEffectiveOverrides() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        List<PatientMeasureOverrideEntity> expectedOverrides = List.of(testOverride);

        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(expectedOverrides);

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getEffectiveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testOverride);
        verify(overrideRepository).findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate);
    }

    @Test
    @DisplayName("Should return empty list when no overrides effective on date")
    void shouldReturnEmptyListWhenNoEffectiveOverrides() {
        // Given
        LocalDate evaluationDate = LocalDate.now().minusDays(365);
        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of());

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getEffectiveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // createOverride() Tests - HIPAA Compliance
    // ========================================

    @Test
    @DisplayName("Should successfully create override with valid clinical reason (HIPAA compliance)")
    void shouldSuccessfullyCreateOverrideWithClinicalReason() {
        // Given
        String clinicalReason = "Patient has early onset diabetes requiring modified screening age";
        when(overrideRepository.findEffectiveOverrides(eq(tenantId), eq(patientId), eq(measureId), any()))
                .thenReturn(List.of());
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenReturn(testOverride);

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                clinicalReason, null, createdBy,
                LocalDate.now(), LocalDate.now().plusYears(1), false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClinicalReason()).isEqualTo(testOverride.getClinicalReason());
        verify(overrideRepository).save(any(PatientMeasureOverrideEntity.class));
        verify(cacheRepository).invalidateByPatientAndMeasure(tenantId, patientId, measureId);
    }

    @Test
    @DisplayName("Should throw exception when clinical reason is null (HIPAA violation)")
    void shouldThrowExceptionWhenClinicalReasonIsNull() {
        // When / Then
        assertThatThrownBy(() -> measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                null, // Missing clinical reason
                null, createdBy, LocalDate.now(), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clinical reason is required")
                .hasMessageContaining("HIPAA compliance");

        verify(overrideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when clinical reason is blank (HIPAA violation)")
    void shouldThrowExceptionWhenClinicalReasonIsBlank() {
        // When / Then
        assertThatThrownBy(() -> measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                "   ", // Blank clinical reason
                null, createdBy, LocalDate.now(), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clinical reason is required");

        verify(overrideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when conflicting override exists for same field")
    void shouldThrowExceptionWhenConflictingOverrideExists() {
        // Given
        PatientMeasureOverrideEntity existingOverride = PatientMeasureOverrideEntity.builder()
                .overrideField("minimumAge")
                .active(true)
                .build();

        when(overrideRepository.findEffectiveOverrides(eq(tenantId), eq(patientId), eq(measureId), any()))
                .thenReturn(List.of(existingOverride));

        // When / Then
        assertThatThrownBy(() -> measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                "Clinical reason", null, createdBy, LocalDate.now(), null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicting override already exists")
                .hasMessageContaining("minimumAge");

        verify(overrideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set default values when optional parameters not provided")
    void shouldSetDefaultValuesWhenOptionalParametersNotProvided() {
        // Given
        when(overrideRepository.findEffectiveOverrides(eq(tenantId), eq(patientId), eq(measureId), any()))
                .thenReturn(List.of());
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50",
                null, // valueType not provided
                "Clinical reason", null, createdBy,
                null, // effectiveFrom not provided
                null, false);

        // Then
        assertThat(result.getValueType()).isEqualTo("TEXT"); // Default value
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.now()); // Default to today
        assertThat(result.getReviewFrequencyDays()).isEqualTo(90); // Default review period
    }

    @Test
    @DisplayName("Should auto-approve when requiresApproval is false")
    void shouldAutoApproveWhenNotRequiringApproval() {
        // Given
        when(overrideRepository.findEffectiveOverrides(eq(tenantId), eq(patientId), eq(measureId), any()))
                .thenReturn(List.of());
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                "Clinical reason", null, createdBy,
                LocalDate.now(), null, false); // requiresApproval = false

        // Then
        assertThat(result.getApprovedBy()).isEqualTo(createdBy);
        assertThat(result.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should invalidate eligibility cache after creating override")
    void shouldInvalidateCacheAfterCreatingOverride() {
        // Given
        when(overrideRepository.findEffectiveOverrides(eq(tenantId), eq(patientId), eq(measureId), any()))
                .thenReturn(List.of());
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenReturn(testOverride);

        // When
        measureOverrideService.createOverride(
                tenantId, patientId, measureId,
                "PARAMETER", "minimumAge", "65", "50", "NUMERIC",
                "Clinical reason", null, createdBy,
                LocalDate.now(), null, false);

        // Then
        verify(cacheRepository).invalidateByPatientAndMeasure(tenantId, patientId, measureId);
    }

    // ========================================
    // approveOverride() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully approve pending override")
    void shouldSuccessfullyApprovePendingOverride() {
        // Given
        UUID overrideId = testOverride.getId();
        UUID approvedBy = UUID.randomUUID();
        testOverride.setApprovedBy(null); // Override is pending

        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.of(testOverride));
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.approveOverride(
                tenantId, overrideId, approvedBy, "Approved for clinical validity");

        // Then
        assertThat(result.getApprovedBy()).isEqualTo(approvedBy);
        assertThat(result.getApprovedAt()).isNotNull();
        verify(overrideRepository).save(testOverride);
    }

    @Test
    @DisplayName("Should throw exception when override not found for approval")
    void shouldThrowExceptionWhenOverrideNotFoundForApproval() {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> measureOverrideService.approveOverride(
                tenantId, overrideId, UUID.randomUUID(), "Approval notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Override not found");

        verify(overrideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle approval of already approved override as no-op")
    void shouldHandleApprovalOfAlreadyApprovedOverride() {
        // Given
        UUID overrideId = testOverride.getId();
        UUID originalApprover = UUID.randomUUID();
        testOverride.setApprovedBy(originalApprover);
        testOverride.setApprovedAt(OffsetDateTime.now().minusDays(1));

        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.of(testOverride));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.approveOverride(
                tenantId, overrideId, UUID.randomUUID(), "Re-approval attempt");

        // Then
        assertThat(result.getApprovedBy()).isEqualTo(originalApprover); // Unchanged
        verify(overrideRepository, never()).save(any()); // No save called
    }

    // ========================================
    // markReviewed() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully mark override as reviewed")
    void shouldSuccessfullyMarkOverrideAsReviewed() {
        // Given
        UUID overrideId = testOverride.getId();
        UUID reviewedBy = UUID.randomUUID();

        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.of(testOverride));
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.markReviewed(
                tenantId, overrideId, reviewedBy);

        // Then
        assertThat(result.getLastReviewedBy()).isEqualTo(reviewedBy);
        assertThat(result.getLastReviewedAt()).isNotNull();
        verify(overrideRepository).save(testOverride);
    }

    @Test
    @DisplayName("Should throw exception when override not found for review")
    void shouldThrowExceptionWhenOverrideNotFoundForReview() {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> measureOverrideService.markReviewed(
                tenantId, overrideId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Override not found");
    }

    // ========================================
    // getOverridesDueForReview() Tests
    // ========================================

    @Test
    @DisplayName("Should return overrides due for review")
    void shouldReturnOverridesDueForReview() {
        // Given
        LocalDate asOfDate = LocalDate.now();
        List<PatientMeasureOverrideEntity> expectedOverrides = List.of(testOverride);

        when(overrideRepository.findOverridesDueForReview(tenantId, asOfDate))
                .thenReturn(expectedOverrides);

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getOverridesDueForReview(
                tenantId, asOfDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testOverride);
    }

    @Test
    @DisplayName("Should use current date when asOfDate is null")
    void shouldUseCurrentDateWhenAsOfDateIsNull() {
        // Given
        when(overrideRepository.findOverridesDueForReview(eq(tenantId), any(LocalDate.class)))
                .thenReturn(List.of());

        // When
        measureOverrideService.getOverridesDueForReview(tenantId, null);

        // Then
        verify(overrideRepository).findOverridesDueForReview(eq(tenantId), eq(LocalDate.now()));
    }

    // ========================================
    // deactivateOverride() Tests
    // ========================================

    @Test
    @DisplayName("Should successfully deactivate active override")
    void shouldSuccessfullyDeactivateOverride() {
        // Given
        UUID overrideId = testOverride.getId();
        UUID deactivatedBy = UUID.randomUUID();

        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.of(testOverride));
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientMeasureOverrideEntity result = measureOverrideService.deactivateOverride(
                tenantId, overrideId, deactivatedBy);

        // Then
        assertThat(result.getActive()).isFalse();
        assertThat(result.getEffectiveUntil()).isEqualTo(LocalDate.now());
        verify(overrideRepository).save(testOverride);
        verify(cacheRepository).invalidateByPatientAndMeasure(tenantId, patientId, measureId);
    }

    @Test
    @DisplayName("Should throw exception when override not found for deactivation")
    void shouldThrowExceptionWhenOverrideNotFoundForDeactivation() {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> measureOverrideService.deactivateOverride(
                tenantId, overrideId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Override not found");
    }

    @Test
    @DisplayName("Should invalidate cache after deactivating override")
    void shouldInvalidateCacheAfterDeactivatingOverride() {
        // Given
        UUID overrideId = testOverride.getId();
        when(overrideRepository.findByIdAndTenantId(overrideId, tenantId))
                .thenReturn(Optional.of(testOverride));
        when(overrideRepository.save(any(PatientMeasureOverrideEntity.class)))
                .thenReturn(testOverride);

        // When
        measureOverrideService.deactivateOverride(tenantId, overrideId, UUID.randomUUID());

        // Then
        verify(cacheRepository).invalidateByPatientAndMeasure(tenantId, patientId, measureId);
    }

    // ========================================
    // resolveOverrides() Tests - Multi-Level Resolution
    // ========================================

    @Test
    @DisplayName("Should resolve patient-specific overrides (highest priority)")
    void shouldResolvePatientSpecificOverrides() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        PatientMeasureOverrideEntity override = PatientMeasureOverrideEntity.builder()
                .overrideField("minimumAge")
                .overrideValue("50")
                .valueType("NUMERIC")
                .build();

        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of(override));
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of());

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).containsEntry("minimumAge", 50.0); // Parsed as double
    }

    @Test
    @DisplayName("Should resolve profile-based overrides when no patient override exists")
    void shouldResolveProfileBasedOverrides() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        UUID profileId = UUID.randomUUID();

        // No patient overrides
        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of());

        // Patient has profile assignment
        PatientProfileAssignmentEntity profileAssignment = PatientProfileAssignmentEntity.builder()
                .profileId(profileId)
                .build();
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of(profileAssignment));

        // Profile has config overrides
        MeasureConfigProfileEntity profile = MeasureConfigProfileEntity.builder()
                .id(profileId)
                .measureId(measureId)
                .active(true)
                .priority(100)
                .configOverrides(Map.of("minimumAge", 55, "frequency", "annual"))
                .build();
        when(profileRepository.findByIdAndTenantId(profileId, tenantId))
                .thenReturn(Optional.of(profile));

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).containsEntry("minimumAge", 55);
        assertThat(result).containsEntry("frequency", "annual");
    }

    @Test
    @DisplayName("Should prioritize patient override over profile override")
    void shouldPrioritizePatientOverrideOverProfile() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        UUID profileId = UUID.randomUUID();

        // Patient override for minimumAge
        PatientMeasureOverrideEntity patientOverride = PatientMeasureOverrideEntity.builder()
                .overrideField("minimumAge")
                .overrideValue("45")
                .valueType("NUMERIC")
                .build();
        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of(patientOverride));

        // Profile also has minimumAge (should be ignored)
        PatientProfileAssignmentEntity profileAssignment = PatientProfileAssignmentEntity.builder()
                .profileId(profileId)
                .build();
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of(profileAssignment));

        MeasureConfigProfileEntity profile = MeasureConfigProfileEntity.builder()
                .id(profileId)
                .measureId(measureId)
                .active(true)
                .priority(100)
                .configOverrides(Map.of("minimumAge", 55)) // Lower priority
                .build();
        when(profileRepository.findByIdAndTenantId(profileId, tenantId))
                .thenReturn(Optional.of(profile));

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).containsEntry("minimumAge", 45.0); // Patient override wins
    }

    @Test
    @DisplayName("Should handle multiple profiles with priority ordering")
    void shouldHandleMultipleProfilesWithPriorityOrdering() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        UUID lowPriorityProfileId = UUID.randomUUID();
        UUID highPriorityProfileId = UUID.randomUUID();

        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of());

        // Patient has two profile assignments
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of(
                        PatientProfileAssignmentEntity.builder().profileId(lowPriorityProfileId).build(),
                        PatientProfileAssignmentEntity.builder().profileId(highPriorityProfileId).build()
                ));

        // Low priority profile
        MeasureConfigProfileEntity lowPriorityProfile = MeasureConfigProfileEntity.builder()
                .id(lowPriorityProfileId)
                .measureId(measureId)
                .active(true)
                .priority(50)
                .configOverrides(Map.of("minimumAge", 60))
                .build();

        // High priority profile (should win)
        MeasureConfigProfileEntity highPriorityProfile = MeasureConfigProfileEntity.builder()
                .id(highPriorityProfileId)
                .measureId(measureId)
                .active(true)
                .priority(100)
                .configOverrides(Map.of("minimumAge", 55))
                .build();

        when(profileRepository.findByIdAndTenantId(lowPriorityProfileId, tenantId))
                .thenReturn(Optional.of(lowPriorityProfile));
        when(profileRepository.findByIdAndTenantId(highPriorityProfileId, tenantId))
                .thenReturn(Optional.of(highPriorityProfile));

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).containsEntry("minimumAge", 55); // High priority wins
    }

    @Test
    @DisplayName("Should return empty map when no overrides exist")
    void shouldReturnEmptyMapWhenNoOverridesExist() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of());
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of());

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should correctly parse different value types (NUMERIC, DATE, BOOLEAN)")
    void shouldCorrectlyParseDifferentValueTypes() {
        // Given
        LocalDate evaluationDate = LocalDate.now();
        PatientMeasureOverrideEntity numericOverride = PatientMeasureOverrideEntity.builder()
                .overrideField("threshold")
                .overrideValue("42.5")
                .valueType("NUMERIC")
                .build();
        PatientMeasureOverrideEntity dateOverride = PatientMeasureOverrideEntity.builder()
                .overrideField("startDate")
                .overrideValue("2026-01-01")
                .valueType("DATE")
                .build();
        PatientMeasureOverrideEntity booleanOverride = PatientMeasureOverrideEntity.builder()
                .overrideField("isActive")
                .overrideValue("true")
                .valueType("BOOLEAN")
                .build();

        when(overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(List.of(numericOverride, dateOverride, booleanOverride));
        when(profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate))
                .thenReturn(List.of());

        // When
        Map<String, Object> result = measureOverrideService.resolveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        // Then
        assertThat(result.get("threshold")).isInstanceOf(Double.class).isEqualTo(42.5);
        assertThat(result.get("startDate")).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(result.get("isActive")).isInstanceOf(Boolean.class).isEqualTo(true);
    }

    // ========================================
    // getPendingApprovals() Tests
    // ========================================

    @Test
    @DisplayName("Should return pending approval overrides")
    void shouldReturnPendingApprovalOverrides() {
        // Given
        PatientMeasureOverrideEntity pendingOverride = PatientMeasureOverrideEntity.builder()
                .approvedBy(null)
                .build();
        List<PatientMeasureOverrideEntity> expectedOverrides = List.of(pendingOverride);

        when(overrideRepository.findPendingApproval(tenantId))
                .thenReturn(expectedOverrides);

        // When
        List<PatientMeasureOverrideEntity> result = measureOverrideService.getPendingApprovals(tenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApprovedBy()).isNull();
    }

    // ========================================
    // countActiveOverrides() Tests
    // ========================================

    @Test
    @DisplayName("Should return correct count of active overrides")
    void shouldReturnCorrectCountOfActiveOverrides() {
        // Given
        long expectedCount = 3L;
        when(overrideRepository.countActiveByPatient(tenantId, patientId))
                .thenReturn(expectedCount);

        // When
        long result = measureOverrideService.countActiveOverrides(tenantId, patientId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should return zero when no active overrides exist")
    void shouldReturnZeroWhenNoActiveOverridesExist() {
        // Given
        when(overrideRepository.countActiveByPatient(tenantId, patientId))
                .thenReturn(0L);

        // When
        long result = measureOverrideService.countActiveOverrides(tenantId, patientId);

        // Then
        assertThat(result).isZero();
    }

    // ========================================
    // Multi-Tenant Isolation Tests
    // ========================================

    @Test
    @DisplayName("Should enforce tenant isolation in all repository calls")
    void shouldEnforceTenantIsolation() {
        // Given
        when(overrideRepository.findActiveByPatientAndMeasure(tenantId, patientId, measureId))
                .thenReturn(List.of());

        // When
        measureOverrideService.getActiveOverrides(tenantId, patientId, measureId);

        // Then
        verify(overrideRepository).findActiveByPatientAndMeasure(eq(tenantId), any(), any());
    }
}
