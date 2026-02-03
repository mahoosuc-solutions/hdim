package com.healthdata.quality.persistence;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
 * Repository tests for PatientMeasureAssignmentRepository
 *
 * Tests custom query methods:
 * - findByIdAndTenantId
 * - findByTenantIdAndPatientId
 * - findActiveByPatient
 * - findActiveByPatientAndMeasure
 * - findEffectiveAssignments
 * - findByAutoAssigned
 * - countActiveByPatient
 *
 * Validates:
 * - Multi-tenant isolation (tenantId filtering)
 * - Active/inactive filtering
 * - Effective date range queries
 * - Auto-assigned vs manual assignment filtering
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("PatientMeasureAssignmentRepository Tests")
class PatientMeasureAssignmentRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PatientMeasureAssignmentRepository repository;

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
    @DisplayName("Should find assignment by ID and tenant ID")
    void shouldFindByIdAndTenantId() {
        // Given
        PatientMeasureAssignmentEntity assignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        entityManager.persist(assignment);
        entityManager.flush();

        // When
        Optional<PatientMeasureAssignmentEntity> result = repository.findByIdAndTenantId(
                assignment.getId(), tenant1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(assignment.getId());
        assertThat(result.get().getTenantId()).isEqualTo(tenant1);
    }

    @Test
    @DisplayName("Should not find assignment when tenant ID does not match (tenant isolation)")
    void shouldNotFindWhenTenantIdDoesNotMatch() {
        // Given
        PatientMeasureAssignmentEntity assignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        entityManager.persist(assignment);
        entityManager.flush();

        // When
        Optional<PatientMeasureAssignmentEntity> result = repository.findByIdAndTenantId(
                assignment.getId(), tenant2); // Different tenant

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findByTenantIdAndPatientId Tests
    // ========================================

    @Test
    @DisplayName("Should find all assignments for patient in tenant")
    void shouldFindByTenantIdAndPatientId() {
        // Given
        PatientMeasureAssignmentEntity assignment1 = createAssignment(
                tenant1, patient1, measure1, true, false);
        PatientMeasureAssignmentEntity assignment2 = createAssignment(
                tenant1, patient1, measure2, true, false);
        PatientMeasureAssignmentEntity assignment3 = createAssignment(
                tenant1, patient2, measure1, true, false); // Different patient
        entityManager.persist(assignment1);
        entityManager.persist(assignment2);
        entityManager.persist(assignment3);
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findByTenantIdAndPatientId(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PatientMeasureAssignmentEntity::getPatientId)
                .containsOnly(patient1);
    }

    // ========================================
    // findActiveByPatient Tests
    // ========================================

    @Test
    @DisplayName("Should find only active assignments for patient")
    void shouldFindActiveByPatient() {
        // Given
        PatientMeasureAssignmentEntity activeAssignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        PatientMeasureAssignmentEntity inactiveAssignment = createAssignment(
                tenant1, patient1, measure2, false, false);
        entityManager.persist(activeAssignment);
        entityManager.persist(inactiveAssignment);
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findActiveByPatient(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
        assertThat(result.get(0).getMeasureId()).isEqualTo(measure1);
    }

    @Test
    @DisplayName("Should enforce tenant isolation in active assignments")
    void shouldEnforceTenantIsolationInActiveAssignments() {
        // Given
        createAssignment(tenant1, patient1, measure1, true, false);
        createAssignment(tenant2, patient1, measure1, true, false); // Different tenant
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findActiveByPatient(
                tenant1, patient1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(tenant1);
    }

    // ========================================
    // findActiveByPatientAndMeasure Tests
    // ========================================

    @Test
    @DisplayName("Should find active assignment for specific patient and measure")
    void shouldFindActiveByPatientAndMeasure() {
        // Given
        PatientMeasureAssignmentEntity activeAssignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        createAssignment(tenant1, patient1, measure2, true, false); // Different measure
        entityManager.flush();

        // When
        Optional<PatientMeasureAssignmentEntity> result = repository.findActiveByPatientAndMeasure(
                tenant1, patient1, measure1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getMeasureId()).isEqualTo(measure1);
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Should not find inactive assignment for patient and measure")
    void shouldNotFindInactiveAssignment() {
        // Given
        createAssignment(tenant1, patient1, measure1, false, false);
        entityManager.flush();

        // When
        Optional<PatientMeasureAssignmentEntity> result = repository.findActiveByPatientAndMeasure(
                tenant1, patient1, measure1);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findEffectiveAssignments Tests
    // ========================================

    @Test
    @DisplayName("Should find assignments effective on specific date")
    void shouldFindEffectiveAssignments() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // Assignment effective now
        PatientMeasureAssignmentEntity currentAssignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        currentAssignment.setEffectiveFrom(yesterday);
        currentAssignment.setEffectiveUntil(tomorrow);
        entityManager.persist(currentAssignment);

        // Assignment not yet effective
        PatientMeasureAssignmentEntity futureAssignment = createAssignment(
                tenant1, patient1, measure2, true, false);
        futureAssignment.setEffectiveFrom(tomorrow);
        entityManager.persist(futureAssignment);

        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findEffectiveAssignments(
                tenant1, patient1, today);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMeasureId()).isEqualTo(measure1);
    }

    @Test
    @DisplayName("Should find assignments with null end date (ongoing assignments)")
    void shouldFindAssignmentsWithNullEndDate() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureAssignmentEntity assignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        assignment.setEffectiveFrom(today.minusDays(30));
        assignment.setEffectiveUntil(null); // No end date
        entityManager.persist(assignment);
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findEffectiveAssignments(
                tenant1, patient1, today);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should not find expired assignments")
    void shouldNotFindExpiredAssignments() {
        // Given
        LocalDate today = LocalDate.now();
        PatientMeasureAssignmentEntity expiredAssignment = createAssignment(
                tenant1, patient1, measure1, true, false);
        expiredAssignment.setEffectiveFrom(today.minusDays(60));
        expiredAssignment.setEffectiveUntil(today.minusDays(1)); // Expired yesterday
        entityManager.persist(expiredAssignment);

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findEffectiveAssignments(
                tenant1, patient1, today);

        // Then
        assertThat(result).isEmpty();
    }

    // ========================================
    // findByAutoAssigned Tests
    // ========================================

    @Test
    @DisplayName("Should find auto-assigned assignments")
    void shouldFindAutoAssignedAssignments() {
        // Given
        createAssignment(tenant1, patient1, measure1, true, true); // Auto-assigned
        createAssignment(tenant1, patient2, measure2, true, false); // Manual
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findByAutoAssigned(
                tenant1, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutoAssigned()).isTrue();
    }

    @Test
    @DisplayName("Should find manually assigned assignments")
    void shouldFindManuallyAssignedAssignments() {
        // Given
        createAssignment(tenant1, patient1, measure1, true, true); // Auto-assigned
        createAssignment(tenant1, patient2, measure2, true, false); // Manual
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findByAutoAssigned(
                tenant1, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutoAssigned()).isFalse();
    }

    @Test
    @DisplayName("Should only return active assignments when querying by auto-assigned")
    void shouldOnlyReturnActiveAssignmentsForAutoAssigned() {
        // Given
        createAssignment(tenant1, patient1, measure1, true, true); // Active auto-assigned
        createAssignment(tenant1, patient2, measure2, false, true); // Inactive auto-assigned
        entityManager.flush();

        // When
        List<PatientMeasureAssignmentEntity> result = repository.findByAutoAssigned(
                tenant1, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ========================================
    // countActiveByPatient Tests
    // ========================================

    @Test
    @DisplayName("Should count active assignments for patient")
    void shouldCountActiveAssignments() {
        // Given
        createAssignment(tenant1, patient1, measure1, true, false);
        createAssignment(tenant1, patient1, measure2, true, false);
        createAssignment(tenant1, patient1, UUID.randomUUID(), false, false); // Inactive
        entityManager.flush();

        // When
        long count = repository.countActiveByPatient(tenant1, patient1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero when no active assignments exist")
    void shouldReturnZeroWhenNoActiveAssignments() {
        // Given
        createAssignment(tenant1, patient1, measure1, false, false); // Inactive
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
        createAssignment(tenant1, patient1, measure1, true, false);
        createAssignment(tenant2, patient1, measure2, true, false); // Different tenant
        entityManager.flush();

        // When
        long count = repository.countActiveByPatient(tenant1, patient1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private PatientMeasureAssignmentEntity createAssignment(
            String tenantId,
            UUID patientId,
            UUID measureId,
            boolean active,
            boolean autoAssigned) {

        PatientMeasureAssignmentEntity assignment = new PatientMeasureAssignmentEntity();
        // Don't set ID - let JPA generate it via @GeneratedValue
        assignment.setTenantId(tenantId);
        assignment.setPatientId(patientId);
        assignment.setMeasureId(measureId);
        assignment.setAssignedBy(UUID.randomUUID());
        assignment.setAssignedAt(java.time.OffsetDateTime.now());
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setActive(active);
        assignment.setAutoAssigned(autoAssigned);
        assignment.setCreatedBy(UUID.randomUUID());
        assignment.setCreatedAt(java.time.OffsetDateTime.now());

        // Persist entity to database
        entityManager.persist(assignment);
        entityManager.flush();

        return assignment;
    }
}
