package com.healthdata.quality.persistence;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for PatientMeasureOverrideRepository
 *
 * Tests custom query methods:
 * - findByIdAndTenantId
 * - findByTenantIdAndPatientId
 * - findActiveByPatient
 * - findActiveByPatientAndMeasure
 * - findEffectiveOverrides
 * - findOverridesDueForReview
 * - findByOverrideType
 * - findPendingApproval
 * - countActiveByPatient
 *
 * Validates:
 * - Multi-tenant isolation (tenantId filtering)
 * - Active/inactive filtering
 * - Effective date range queries
 * - Approval workflow filtering
 * - Periodic review scheduling
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("PatientMeasureOverrideRepository Tests")
class PatientMeasureOverrideRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PatientMeasureOverrideRepository repository;

    private String tenant1;
    private String tenant2;
    private UUID patient1;
    private UUID patient2;
    private UUID measure1;
    private UUID measure2;

    @BeforeEach
    void setUp() {
        tenant1 = "TENANT-001";
        tenant2 = "TENANT-002";
        patient1 = UUID.randomUUID();
        patient2 = UUID.randomUUID();
        measure1 = UUID.randomUUID();
        measure2 = UUID.randomUUID();
    }

    // ========================================
    // findByIdAndTenantId Tests
    // ========================================

    @Test
    @DisplayName("Should find override by ID and tenant ID")
    void shouldFindByIdAndTenantId() {
        // Given
        PatientMeasureOverrideEntity override = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        entityManager.persist(override);
        entityManager.flush();

        // When
        Optional<PatientMeasureOverrideEntity> result = repository.findByIdAndTenantId(
                override.getId(), tenant1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(override.getId());
        assertThat(result.get().getTenantId()).isEqualTo(tenant1);
    }

    @Test
    @DisplayName("Should not find override when tenant ID does not match (tenant isolation)")
    void shouldNotFindWhenTenantIdDoesNotMatch() {
        // Given
        PatientMeasureOverrideEntity override = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        entityManager.persist(override);
        entityManager.flush();

        // When
        Optional<PatientMeasureOverrideEntity> result = repository.findByIdAndTenantId(
                override.getId(), tenant2); // Different tenant

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findByTenantIdAndPatientId Tests
    // ========================================

    @Test
    @DisplayName("Should find all overrides for patient in tenant")
    void shouldFindByTenantIdAndPatientId() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant1, patient1, measure2, "THRESHOLD", true);
        createOverride(tenant1, patient2, measure1, "PARAMETER", true); // Different patient
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findByTenantIdAndPatientId(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PatientMeasureOverrideEntity::getPatientId)
                .containsOnly(patient1);
    }

    // ========================================
    // findActiveByPatient Tests
    // ========================================

    @Test
    @DisplayName("Should find only active overrides for patient")
    void shouldFindActiveByPatient() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant1, patient1, measure2, "PARAMETER", false); // Inactive
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findActiveByPatient(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
        assertThat(result.get(0).getMeasureId()).isEqualTo(measure1);
    }

    @Test
    @DisplayName("Should enforce tenant isolation in active overrides")
    void shouldEnforceTenantIsolationInActiveOverrides() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant2, patient1, measure1, "PARAMETER", true); // Different tenant
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findActiveByPatient(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(tenant1);
    }

    // ========================================
    // findActiveByPatientAndMeasure Tests
    // ========================================

    @Test
    @DisplayName("Should find active overrides for specific patient and measure")
    void shouldFindActiveByPatientAndMeasure() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant1, patient1, measure2, "PARAMETER", true); // Different measure
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findActiveByPatientAndMeasure(
                tenant1, patient1, measure1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMeasureId()).isEqualTo(measure1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("Should not find inactive overrides for patient and measure")
    void shouldNotFindInactiveOverrides() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", false);
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findActiveByPatientAndMeasure(
                tenant1, patient1, measure1);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findEffectiveOverrides Tests
    // ========================================

    @Test
    @DisplayName("Should find overrides effective on specific date")
    void shouldFindEffectiveOverrides() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // Override effective now
        PatientMeasureOverrideEntity currentOverride = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        currentOverride.setEffectiveFrom(yesterday);
        currentOverride.setEffectiveUntil(tomorrow);
        entityManager.persist(currentOverride);

        // Override not yet effective
        PatientMeasureOverrideEntity futureOverride = createOverride(
                tenant1, patient1, measure1, "THRESHOLD", true);
        futureOverride.setEffectiveFrom(tomorrow);
        entityManager.persist(futureOverride);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findEffectiveOverrides(
                tenant1, patient1, measure1, today);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOverrideType()).isEqualTo("PARAMETER");
    }

    @Test
    @DisplayName("Should find overrides with null end date (ongoing overrides)")
    void shouldFindOverridesWithNullEndDate() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureOverrideEntity override = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        override.setEffectiveFrom(today.minusDays(30));
        override.setEffectiveUntil(null); // No end date
        entityManager.persist(override);
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findEffectiveOverrides(
                tenant1, patient1, measure1, today);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should not find expired overrides")
    void shouldNotFindExpiredOverrides() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureOverrideEntity expiredOverride = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        expiredOverride.setEffectiveFrom(today.minusDays(90));
        expiredOverride.setEffectiveUntil(today.minusDays(1)); // Expired yesterday
        entityManager.persist(expiredOverride);
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findEffectiveOverrides(
                tenant1, patient1, measure1, today);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findOverridesDueForReview Tests
    // ========================================

    @Test
    @DisplayName("Should find overrides due for review")
    void shouldFindOverridesDueForReview() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureOverrideEntity dueOverride = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        dueOverride.setRequiresPeriodicReview(true);
        dueOverride.setNextReviewDate(today.minusDays(1)); // Due yesterday
        entityManager.persist(dueOverride);

        PatientMeasureOverrideEntity notDueOverride = createOverride(
                tenant1, patient2, measure2, "PARAMETER", true);
        notDueOverride.setRequiresPeriodicReview(true);
        notDueOverride.setNextReviewDate(today.plusDays(30)); // Not due yet
        entityManager.persist(notDueOverride);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findOverridesDueForReview(
                tenant1, today);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(patient1);
    }

    @Test
    @DisplayName("Should only return active overrides due for review")
    void shouldOnlyReturnActiveOverridesDueForReview() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureOverrideEntity activeOverride = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        activeOverride.setRequiresPeriodicReview(true);
        activeOverride.setNextReviewDate(today);
        entityManager.persist(activeOverride);

        PatientMeasureOverrideEntity inactiveOverride = createOverride(
                tenant1, patient2, measure2, "PARAMETER", false);
        inactiveOverride.setRequiresPeriodicReview(true);
        inactiveOverride.setNextReviewDate(today);
        entityManager.persist(inactiveOverride);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findOverridesDueForReview(
                tenant1, today);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("Should order overrides by review date")
    void shouldOrderOverridesByReviewDate() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureOverrideEntity override1 = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        override1.setRequiresPeriodicReview(true);
        override1.setNextReviewDate(today.minusDays(5));
        entityManager.persist(override1);

        PatientMeasureOverrideEntity override2 = createOverride(
                tenant1, patient2, measure2, "PARAMETER", true);
        override2.setRequiresPeriodicReview(true);
        override2.setNextReviewDate(today.minusDays(10)); // Older
        entityManager.persist(override2);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findOverridesDueForReview(
                tenant1, today);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNextReviewDate()).isEqualTo(today.minusDays(10)); // Oldest first
        assertThat(result.get(1).getNextReviewDate()).isEqualTo(today.minusDays(5));
    }

    // ========================================
    // findByOverrideType Tests
    // ========================================

    @Test
    @DisplayName("Should find overrides by type")
    void shouldFindByOverrideType() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant1, patient2, measure2, "THRESHOLD", true);
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findByOverrideType(
                tenant1, "PARAMETER");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOverrideType()).isEqualTo("PARAMETER");
    }

    @Test
    @DisplayName("Should only return active overrides when querying by type")
    void shouldOnlyReturnActiveOverridesForType() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true); // Active
        createOverride(tenant1, patient2, measure2, "PARAMETER", false); // Inactive
        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findByOverrideType(
                tenant1, "PARAMETER");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ========================================
    // findPendingApproval Tests
    // ========================================

    @Test
    @DisplayName("Should find overrides pending approval")
    void shouldFindPendingApproval() {
        // Given
        PatientMeasureOverrideEntity pendingOverride = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        pendingOverride.setApprovedBy(null); // Not approved
        entityManager.persist(pendingOverride);

        PatientMeasureOverrideEntity approvedOverride = createOverride(
                tenant1, patient2, measure2, "PARAMETER", true);
        approvedOverride.setApprovedBy(UUID.randomUUID()); // Approved
        approvedOverride.setApprovedAt(OffsetDateTime.now());
        entityManager.persist(approvedOverride);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findPendingApproval(tenant1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApprovedBy()).isNull();
    }

    @Test
    @DisplayName("Should only return active overrides pending approval")
    void shouldOnlyReturnActiveOverridesPendingApproval() {
        // Given
        PatientMeasureOverrideEntity activePending = createOverride(
                tenant1, patient1, measure1, "PARAMETER", true);
        activePending.setApprovedBy(null);
        entityManager.persist(activePending);

        PatientMeasureOverrideEntity inactivePending = createOverride(
                tenant1, patient2, measure2, "PARAMETER", false);
        inactivePending.setApprovedBy(null);
        entityManager.persist(inactivePending);

        entityManager.flush();

        // When
        List<PatientMeasureOverrideEntity> result = repository.findPendingApproval(tenant1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ========================================
    // countActiveByPatient Tests
    // ========================================

    @Test
    @DisplayName("Should count active overrides for patient")
    void shouldCountActiveOverrides() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant1, patient1, measure2, "THRESHOLD", true);
        createOverride(tenant1, patient1, UUID.randomUUID(), "PARAMETER", false); // Inactive
        entityManager.flush();

        // When
        long count = repository.countActiveByPatient(tenant1, patient1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero when no active overrides exist")
    void shouldReturnZeroWhenNoActiveOverrides() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", false); // Inactive
        entityManager.flush();

        // When
        long count = repository.countActiveByPatient(tenant1, patient1);

        // Then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in count")
    void shouldEnforceTenantIsolationInCount() {
        // Given
        createOverride(tenant1, patient1, measure1, "PARAMETER", true);
        createOverride(tenant2, patient1, measure2, "PARAMETER", true); // Different tenant
        entityManager.flush();

        // When
        long count = repository.countActiveByPatient(tenant1, patient1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private PatientMeasureOverrideEntity createOverride(
            String tenantId,
            UUID patientId,
            UUID measureId,
            String overrideType,
            boolean active) {

        PatientMeasureOverrideEntity override = new PatientMeasureOverrideEntity();
        // Don't set ID - let JPA generate it via @GeneratedValue
        override.setTenantId(tenantId);
        override.setPatientId(patientId);
        override.setMeasureId(measureId);
        override.setOverrideType(overrideType);
        override.setOverrideField("testField");
        override.setOriginalValue("originalValue");
        override.setOverrideValue("overrideValue");
        override.setValueType("TEXT");
        override.setClinicalReason("Clinical reason for HIPAA compliance");
        override.setActive(active);
        override.setEffectiveFrom(LocalDate.now());
        override.setRequiresPeriodicReview(false);
        override.setCreatedBy(UUID.randomUUID());
        override.setCreatedAt(OffsetDateTime.now());

        // Persist entity to database
        entityManager.persist(override);
        entityManager.flush();

        return override;
    }
}
