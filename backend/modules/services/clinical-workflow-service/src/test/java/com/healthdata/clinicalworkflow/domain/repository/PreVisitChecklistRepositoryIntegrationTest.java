package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.config.BaseIntegrationTest;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@BaseIntegrationTest
@DisplayName("PreVisitChecklistRepository Integration Tests")
class PreVisitChecklistRepositoryIntegrationTest {

    @Autowired
    private PreVisitChecklistRepository checklistRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    private PreVisitChecklistEntity pendingChecklist;
    private PreVisitChecklistEntity inProgressChecklist;
    private PreVisitChecklistEntity completedChecklist;

    @BeforeEach
    void setUp() {
        checklistRepository.deleteAll();
        
        pendingChecklist = createChecklist(TENANT_ID, PATIENT_ID_1, "APT-001", "new-patient", "pending", 0);
        inProgressChecklist = createChecklist(TENANT_ID, PATIENT_ID_1, "APT-002", "follow-up", "in-progress", 50);
        completedChecklist = createChecklist(TENANT_ID, PATIENT_ID_2, "APT-003", "procedure-pre", "completed", 100);

        pendingChecklist = checklistRepository.save(pendingChecklist);
        inProgressChecklist = checklistRepository.save(inProgressChecklist);
        completedChecklist = checklistRepository.save(completedChecklist);
    }

    @Nested
    @DisplayName("Appointment Type Queries")
    class AppointmentTypeQueryTests {

        @Test
        @DisplayName("Should find checklists by appointment type and tenant")
        void shouldFindByAppointmentTypeAndTenant() {
            List<PreVisitChecklistEntity> newPatientChecklists = checklistRepository.findByAppointmentTypeAndTenant("new-patient", TENANT_ID);

            assertThat(newPatientChecklists).hasSize(1);
            assertThat(newPatientChecklists.get(0).getAppointmentType()).isEqualTo("new-patient");
        }

        @Test
        @DisplayName("Should handle non-existent appointment type")
        void shouldHandleNonExistentAppointmentType() {
            List<PreVisitChecklistEntity> found = checklistRepository.findByAppointmentTypeAndTenant("non-existent", TENANT_ID);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Completion Status Queries")
    class CompletionStatusQueryTests {

        @Test
        @DisplayName("Should find incomplete checklists by tenant")
        void shouldFindIncompleteChecklistsByTenant() {
            List<PreVisitChecklistEntity> incomplete = checklistRepository.findIncompleteChecklistsByTenant(TENANT_ID);

            assertThat(incomplete).hasSize(2); // pending and in-progress
            assertThat(incomplete).noneMatch(c -> c.getStatus().equals("completed"));
        }

        @Test
        @DisplayName("Should count incomplete checklists")
        void shouldCountIncompleteChecklists() {
            long count = checklistRepository.countIncompleteChecklists(TENANT_ID);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find checklists by status")
        void shouldFindChecklistsByStatus() {
            List<PreVisitChecklistEntity> pending = checklistRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, "pending");

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("Should find checklists with low completion")
        void shouldFindChecklistsWithLowCompletion() {
            List<PreVisitChecklistEntity> lowCompletion = checklistRepository.findChecklistsWithLowCompletion(TENANT_ID, 75.0);

            assertThat(lowCompletion).hasSize(1); // Only in-progress with 50%
            assertThat(lowCompletion.get(0).getCompletionPercentage()).isLessThan(new BigDecimal("75.0"));
        }
    }

    @Nested
    @DisplayName("Appointment-Based Queries")
    class AppointmentBasedQueryTests {

        @Test
        @DisplayName("Should find checklist by appointment ID and tenant")
        void shouldFindChecklistByAppointmentId() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findChecklistByAppointmentId("APT-001", TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getAppointmentId()).isEqualTo("APT-001");
        }

        @Test
        @DisplayName("Should not find checklist with wrong tenant")
        void shouldNotFindChecklistWithWrongTenant() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findChecklistByAppointmentId("APT-001", OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle null appointment ID")
        void shouldHandleNullAppointmentId() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findChecklistByAppointmentId(null, TENANT_ID);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Patient-Based Queries")
    class PatientBasedQueryTests {

        @Test
        @DisplayName("Should find all checklists for patient")
        void shouldFindAllChecklistsForPatient() {
            List<PreVisitChecklistEntity> checklists = checklistRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_ID, PATIENT_ID_1);

            assertThat(checklists).hasSize(2); // pending and in-progress
            assertThat(checklists).allMatch(c -> c.getPatientId().equals(PATIENT_ID_1));
        }

        @Test
        @DisplayName("Should handle patient with no checklists")
        void shouldHandlePatientWithNoChecklists() {
            UUID nonExistentPatient = UUID.randomUUID();
            List<PreVisitChecklistEntity> checklists = checklistRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_ID, nonExistentPatient);

            assertThat(checklists).isEmpty();
        }
    }

    @Nested
    @DisplayName("Staff Completion Queries")
    class StaffCompletionQueryTests {

        @Test
        @DisplayName("Should find checklists completed by specific staff")
        void shouldFindChecklistsCompletedBy() {
            completedChecklist.setCompletedBy("MA-001");
            checklistRepository.save(completedChecklist);

            List<PreVisitChecklistEntity> completed = checklistRepository.findChecklistsCompletedBy(TENANT_ID, "MA-001");

            assertThat(completed).hasSize(1);
            assertThat(completed.get(0).getCompletedBy()).isEqualTo("MA-001");
            assertThat(completed.get(0).getStatus()).isEqualTo("completed");
        }

        @Test
        @DisplayName("Should not include incomplete checklists")
        void shouldNotIncludeIncompleteChecklists() {
            inProgressChecklist.setCompletedBy("MA-001");
            checklistRepository.save(inProgressChecklist);

            List<PreVisitChecklistEntity> completed = checklistRepository.findChecklistsCompletedBy(TENANT_ID, "MA-001");

            assertThat(completed).isEmpty(); // inProgressChecklist is not completed
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate checklist data between tenants")
        void shouldIsolateChecklistDataBetweenTenants() {
            PreVisitChecklistEntity otherTenantChecklist = createChecklist(OTHER_TENANT, UUID.randomUUID(), "APT-999", "new-patient", "pending", 0);
            checklistRepository.save(otherTenantChecklist);

            List<PreVisitChecklistEntity> tenant1Checklists = checklistRepository.findIncompleteChecklistsByTenant(TENANT_ID);
            List<PreVisitChecklistEntity> tenant2Checklists = checklistRepository.findIncompleteChecklistsByTenant(OTHER_TENANT);

            assertThat(tenant1Checklists).noneMatch(c -> c.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Checklists).noneMatch(c -> c.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should count only tenant's own incomplete checklists")
        void shouldCountOnlyTenantOwnIncompleteChecklists() {
            PreVisitChecklistEntity otherTenantChecklist = createChecklist(OTHER_TENANT, UUID.randomUUID(), "APT-999", "new-patient", "pending", 0);
            checklistRepository.save(otherTenantChecklist);

            long tenant1Count = checklistRepository.countIncompleteChecklists(TENANT_ID);
            long tenant2Count = checklistRepository.countIncompleteChecklists(OTHER_TENANT);

            assertThat(tenant1Count).isEqualTo(2);
            assertThat(tenant2Count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not find checklist across tenants by appointment ID")
        void shouldNotFindChecklistAcrossTenantsByAppointmentId() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findChecklistByAppointmentId("APT-001", OTHER_TENANT);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve checklist by ID")
        void shouldSaveAndRetrieve() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findById(pendingChecklist.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should find checklist by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<PreVisitChecklistEntity> found = checklistRepository.findByIdAndTenantId(pendingChecklist.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should update checklist status")
        void shouldUpdateChecklistStatus() {
            pendingChecklist.setStatus("in-progress");
            pendingChecklist.setCompletionPercentage(new BigDecimal("25.0"));
            checklistRepository.save(pendingChecklist);

            Optional<PreVisitChecklistEntity> found = checklistRepository.findById(pendingChecklist.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo("in-progress");
            assertThat(found.get().getCompletionPercentage()).isEqualByComparingTo(new BigDecimal("25.0"));
        }
    }

    private PreVisitChecklistEntity createChecklist(String tenantId, UUID patientId, String appointmentId, 
                                                     String appointmentType, String status, double completionPct) {
        return PreVisitChecklistEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .appointmentType(appointmentType)
                .status(status)
                .completionPercentage(new BigDecimal(completionPct))
                .reviewMedicalHistory(false)
                .verifyInsurance(false)
                .updateDemographics(false)
                .reviewMedications(false)
                .reviewAllergies(false)
                .prepareVitalsEquipment(false)
                .reviewCareGaps(false)
                .obtainConsent(false)
                .build();
    }
}
