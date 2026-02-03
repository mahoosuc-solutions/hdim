package com.healthdata.quality.persistence;

import com.healthdata.quality.config.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for Care Team Assignment Repository
 *
 * Tests patient-specific care team assignment queries
 */
@BaseIntegrationTest
@DisplayName("Care Team Assignment Repository Tests")
class CareTeamAssignmentRepositoryTest {

    @Autowired
    private CareTeamAssignmentRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String PROVIDER_ID = "provider-456";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should find active care team assignments for patient")
    void shouldFindActiveCareTeamAssignmentsForPatient() {
        // Given: Multiple care team assignments
        CareTeamAssignmentEntity primary = createCareTeamAssignment(
            PATIENT_ID,
            "provider-1",
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity coordinator = createCareTeamAssignment(
            PATIENT_ID,
            "provider-2",
            "care-coordinator",
            2,
            true
        );

        CareTeamAssignmentEntity inactive = createCareTeamAssignment(
            PATIENT_ID,
            "provider-3",
            "specialist",
            3,
            false
        );

        repository.save(primary);
        repository.save(coordinator);
        repository.save(inactive);

        // When: Finding active assignments
        List<CareTeamAssignmentEntity> assignments = repository
            .findByTenantIdAndPatientIdAndActiveOrderByContactPriorityAsc(
                TENANT_ID,
                PATIENT_ID,
                true
            );

        // Then: Should return only active assignments, ordered by priority
        assertThat(assignments).hasSize(2);
        assertThat(assignments.get(0).getRole()).isEqualTo("primary-care-provider");
        assertThat(assignments.get(1).getRole()).isEqualTo("care-coordinator");
    }

    @Test
    @DisplayName("Should find care team members by role")
    void shouldFindCareTeamMembersByRole() {
        // Given: Multiple providers with different roles
        CareTeamAssignmentEntity primaryProvider = createCareTeamAssignment(
            PATIENT_ID,
            "provider-1",
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity specialist = createCareTeamAssignment(
            PATIENT_ID,
            "provider-2",
            "specialist",
            2,
            true
        );

        repository.save(primaryProvider);
        repository.save(specialist);

        // When: Finding primary care provider
        List<CareTeamAssignmentEntity> assignments = repository
            .findByTenantIdAndPatientIdAndRoleAndActiveOrderByContactPriorityAsc(
                TENANT_ID,
                PATIENT_ID,
                "primary-care-provider",
                true
            );

        // Then: Should return only matching role
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getProviderId()).isEqualTo("provider-1");
    }

    @Test
    @DisplayName("Should find primary care team member for role")
    void shouldFindPrimaryCareTeamMemberForRole() {
        // Given: Multiple coordinators, one marked as primary
        CareTeamAssignmentEntity primaryCoordinator = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-1")
            .providerName("John Doe")
            .role("care-coordinator")
            .contactPriority(1)
            .isPrimary(true)
            .active(true)
            .build();

        CareTeamAssignmentEntity backupCoordinator = createCareTeamAssignment(
            PATIENT_ID,
            "provider-2",
            "care-coordinator",
            2,
            true
        );

        repository.save(primaryCoordinator);
        repository.save(backupCoordinator);

        // When: Finding primary coordinator
        Optional<CareTeamAssignmentEntity> assignment = repository
            .findFirstByTenantIdAndPatientIdAndRoleAndActiveAndIsPrimaryOrderByContactPriorityAsc(
                TENANT_ID,
                PATIENT_ID,
                "care-coordinator",
                true,
                true
            );

        // Then: Should return primary coordinator
        assertThat(assignment).isPresent();
        assertThat(assignment.get().getProviderId()).isEqualTo("provider-1");
        assertThat(assignment.get().isPrimary()).isTrue();
    }

    @Test
    @DisplayName("Should find active assignments on specific date")
    void shouldFindActiveAssignmentsOnSpecificDate() {
        // Given: Assignments with different effective dates
        LocalDate today = LocalDate.now();
        LocalDate past = today.minusDays(30);
        LocalDate future = today.plusDays(30);

        CareTeamAssignmentEntity current = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-1")
            .role("primary-care-provider")
            .contactPriority(1)
            .active(true)
            .effectiveFrom(past)
            .effectiveTo(future)
            .build();

        CareTeamAssignmentEntity expired = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-2")
            .role("specialist")
            .contactPriority(2)
            .active(true)
            .effectiveFrom(past.minusDays(60))
            .effectiveTo(past.minusDays(1))
            .build();

        CareTeamAssignmentEntity notYetEffective = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-3")
            .role("care-coordinator")
            .contactPriority(3)
            .active(true)
            .effectiveFrom(future.plusDays(1))
            .effectiveTo(null)
            .build();

        repository.save(current);
        repository.save(expired);
        repository.save(notYetEffective);

        // When: Finding assignments effective today
        List<CareTeamAssignmentEntity> assignments = repository
            .findActiveAssignmentsOnDate(
                TENANT_ID,
                PATIENT_ID,
                today,
                true
            );

        // Then: Should return only current assignment
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getProviderId()).isEqualTo("provider-1");
    }

    @Test
    @DisplayName("Should find active assignments by role on specific date")
    void shouldFindActiveAssignmentsByRoleOnSpecificDate() {
        // Given: Assignments with different effective dates and roles
        LocalDate today = LocalDate.now();
        LocalDate past = today.minusDays(30);

        CareTeamAssignmentEntity currentProvider = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-1")
            .role("primary-care-provider")
            .contactPriority(1)
            .active(true)
            .effectiveFrom(past)
            .effectiveTo(null)
            .build();

        CareTeamAssignmentEntity currentCoordinator = CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .providerId("provider-2")
            .role("care-coordinator")
            .contactPriority(2)
            .active(true)
            .effectiveFrom(past)
            .effectiveTo(null)
            .build();

        repository.save(currentProvider);
        repository.save(currentCoordinator);

        // When: Finding primary care provider on today's date
        List<CareTeamAssignmentEntity> assignments = repository
            .findActiveAssignmentsByRoleOnDate(
                TENANT_ID,
                PATIENT_ID,
                "primary-care-provider",
                today,
                true
            );

        // Then: Should return only primary care provider
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getProviderId()).isEqualTo("provider-1");
    }

    @Test
    @DisplayName("Should check if patient has specific care team role")
    void shouldCheckIfPatientHasSpecificCareTeamRole() {
        // Given: Care team assignment
        CareTeamAssignmentEntity assignment = createCareTeamAssignment(
            PATIENT_ID,
            PROVIDER_ID,
            "psychiatrist",
            1,
            true
        );

        repository.save(assignment);

        // When: Checking if patient has psychiatrist
        boolean hasPsychiatrist = repository.existsByTenantIdAndPatientIdAndRoleAndActive(
            TENANT_ID,
            PATIENT_ID,
            "psychiatrist",
            true
        );

        boolean hasCardiologist = repository.existsByTenantIdAndPatientIdAndRoleAndActive(
            TENANT_ID,
            PATIENT_ID,
            "cardiologist",
            true
        );

        // Then: Should return true for psychiatrist, false for cardiologist
        assertThat(hasPsychiatrist).isTrue();
        assertThat(hasCardiologist).isFalse();
    }

    @Test
    @DisplayName("Should find all patients assigned to provider")
    void shouldFindAllPatientsAssignedToProvider() {
        // Given: Multiple patients assigned to same provider
        CareTeamAssignmentEntity patient1 = createCareTeamAssignment(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            PROVIDER_ID,
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity patient2 = createCareTeamAssignment(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            PROVIDER_ID,
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity otherProvider = createCareTeamAssignment(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "other-provider",
            "primary-care-provider",
            1,
            true
        );

        repository.save(patient1);
        repository.save(patient2);
        repository.save(otherProvider);

        // When: Finding all active assignments for provider
        List<CareTeamAssignmentEntity> assignments = repository
            .findByProviderIdAndActive(PROVIDER_ID, true);

        // Then: Should return both patients
        assertThat(assignments).hasSize(2);
        assertThat(assignments)
            .extracting(CareTeamAssignmentEntity::getPatientId)
            .containsExactlyInAnyOrder(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222")
            );
    }

    @Test
    @DisplayName("Should find patients by provider and role")
    void shouldFindPatientsByProviderAndRole() {
        // Given: Provider assigned to patients in different roles
        CareTeamAssignmentEntity primary = createCareTeamAssignment(
            UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444"),
            PROVIDER_ID,
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity specialist = createCareTeamAssignment(
            UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444"),
            PROVIDER_ID,
            "specialist",
            1,
            true
        );

        repository.save(primary);
        repository.save(specialist);

        // When: Finding patients where provider is primary care
        List<CareTeamAssignmentEntity> assignments = repository
            .findByTenantIdAndProviderIdAndRoleAndActive(
                TENANT_ID,
                PROVIDER_ID,
                "primary-care-provider",
                true
            );

        // Then: Should return only primary care assignments
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getPatientId())
            .isEqualTo(UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444"));
    }

    @Test
    @DisplayName("Should order care team assignments by contact priority")
    void shouldOrderCareTeamAssignmentsByContactPriority() {
        // Given: Multiple assignments with different priorities
        CareTeamAssignmentEntity lowPriority = createCareTeamAssignment(
            PATIENT_ID,
            "provider-3",
            "specialist",
            10,
            true
        );

        CareTeamAssignmentEntity highPriority = createCareTeamAssignment(
            PATIENT_ID,
            "provider-1",
            "primary-care-provider",
            1,
            true
        );

        CareTeamAssignmentEntity mediumPriority = createCareTeamAssignment(
            PATIENT_ID,
            "provider-2",
            "care-coordinator",
            5,
            true
        );

        repository.save(lowPriority);
        repository.save(highPriority);
        repository.save(mediumPriority);

        // When: Finding assignments
        List<CareTeamAssignmentEntity> assignments = repository
            .findByTenantIdAndPatientIdAndActiveOrderByContactPriorityAsc(
                TENANT_ID,
                PATIENT_ID,
                true
            );

        // Then: Should be ordered by priority (1, 5, 10)
        assertThat(assignments).hasSize(3);
        assertThat(assignments.get(0).getProviderId()).isEqualTo("provider-1");
        assertThat(assignments.get(1).getProviderId()).isEqualTo("provider-2");
        assertThat(assignments.get(2).getProviderId()).isEqualTo("provider-3");
    }

    // Helper method
    private CareTeamAssignmentEntity createCareTeamAssignment(
        UUID patientId,
        String providerId,
        String role,
        int contactPriority,
        boolean active
    ) {
        return CareTeamAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(patientId)
            .providerId(providerId)
            .providerName("Provider " + providerId)
            .role(role)
            .contactPriority(contactPriority)
            .active(active)
            .build();
    }
}
