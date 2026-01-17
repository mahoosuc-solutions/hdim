package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.config.BaseIntegrationTest;
import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Patient Check-In Repository Integration Tests
 *
 * Tests database operations for patient check-in tracking with real PostgreSQL
 * via Testcontainers. Covers all repository queries including:
 * - Patient-based queries
 * - Date-based queries
 * - Appointment-based queries
 * - Multi-tenant isolation
 */
@BaseIntegrationTest
@DisplayName("PatientCheckInRepository Integration Tests")
class PatientCheckInRepositoryIntegrationTest {

    @Autowired
    private PatientCheckInRepository checkInRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    private PatientCheckInEntity checkIn1;
    private PatientCheckInEntity checkIn2;
    private PatientCheckInEntity checkIn3;

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAll();
        
        // Create test check-ins
        checkIn1 = createCheckIn(TENANT_ID, PATIENT_ID_1, "APT-001", "checked-in", true, true, true);
        checkIn2 = createCheckIn(TENANT_ID, PATIENT_ID_1, "APT-002", "waiting", false, false, false);
        checkIn3 = createCheckIn(TENANT_ID, PATIENT_ID_2, "APT-003", "in-room", true, false, true);

        checkIn1 = checkInRepository.save(checkIn1);
        checkIn2 = checkInRepository.save(checkIn2);
        checkIn3 = checkInRepository.save(checkIn3);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve check-in by ID")
        void shouldSaveAndRetrieve() {
            Optional<PatientCheckInEntity> found = checkInRepository.findById(checkIn1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
            assertThat(found.get().getAppointmentId()).isEqualTo("APT-001");
        }

        @Test
        @DisplayName("Should find check-in by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByIdAndTenantId(checkIn1.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should not find check-in with wrong tenant")
        void shouldNotFindWithWrongTenant() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByIdAndTenantId(checkIn1.getId(), OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update check-in")
        void shouldUpdate() {
            checkIn1.setStatus("in-room");
            checkIn1.setInsuranceVerified(true);
            checkInRepository.save(checkIn1);

            Optional<PatientCheckInEntity> found = checkInRepository.findById(checkIn1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo("in-room");
            assertThat(found.get().getInsuranceVerified()).isTrue();
        }

        @Test
        @DisplayName("Should auto-generate timestamps on create")
        void shouldAutoGenerateTimestamps() {
            PatientCheckInEntity newCheckIn = createCheckIn(TENANT_ID, PATIENT_ID_1, "APT-004", "checked-in", false, false, false);
            newCheckIn.setCreatedAt(null);
            newCheckIn.setUpdatedAt(null);

            PatientCheckInEntity saved = checkInRepository.save(newCheckIn);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getCheckInTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Patient-Based Queries")
    class PatientBasedQueryTests {

        @Test
        @DisplayName("Should find check-in by patient ID and tenant")
        void shouldFindByPatientIdAndTenant() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByPatientIdAndTenantId(PATIENT_ID_1, TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should return most recent check-in for patient")
        void shouldReturnMostRecentCheckIn() {
            // Create additional check-in for same patient
            PatientCheckInEntity recentCheckIn = createCheckIn(TENANT_ID, PATIENT_ID_1, "APT-005", "checked-in", false, false, false);
            recentCheckIn.setCheckInTime(Instant.now().plus(1, ChronoUnit.HOURS));
            checkInRepository.save(recentCheckIn);

            Optional<PatientCheckInEntity> found = checkInRepository.findByPatientIdAndTenantId(PATIENT_ID_1, TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getAppointmentId()).isEqualTo("APT-005");
        }

        @Test
        @DisplayName("Should find all check-ins for patient")
        void shouldFindAllCheckInsForPatient() {
            List<PatientCheckInEntity> checkIns = checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(TENANT_ID, PATIENT_ID_1);

            assertThat(checkIns).hasSize(2);
            assertThat(checkIns).allMatch(c -> c.getPatientId().equals(PATIENT_ID_1));
        }

        @Test
        @DisplayName("Should count check-ins for patient")
        void shouldCountCheckInsForPatient() {
            long count = checkInRepository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID_1);

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Date-Based Queries")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should find today's check-ins by tenant")
        void shouldFindTodayCheckInsByTenant() {
            List<PatientCheckInEntity> todayCheckIns = checkInRepository.findTodayCheckInsByTenant(TENANT_ID, LocalDate.now());

            assertThat(todayCheckIns).hasSize(3);
            assertThat(todayCheckIns).allMatch(c -> c.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not return check-ins from other dates")
        void shouldNotReturnCheckInsFromOtherDates() {
            List<PatientCheckInEntity> yesterdayCheckIns = checkInRepository.findTodayCheckInsByTenant(TENANT_ID, LocalDate.now().minusDays(1));

            assertThat(yesterdayCheckIns).isEmpty();
        }

        @Test
        @DisplayName("Should count check-ins by tenant and date")
        void shouldCountCheckInsByTenantAndDate() {
            long count = checkInRepository.countCheckInsByTenantAndDate(TENANT_ID, LocalDate.now());

            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Appointment-Based Queries")
    class AppointmentBasedQueryTests {

        @Test
        @DisplayName("Should find check-in by appointment ID")
        void shouldFindByAppointmentId() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByAppointmentId("APT-001", TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getAppointmentId()).isEqualTo("APT-001");
        }

        @Test
        @DisplayName("Should not find check-in with wrong tenant for appointment")
        void shouldNotFindWithWrongTenantForAppointment() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByAppointmentId("APT-001", OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle null appointment ID")
        void shouldHandleNullAppointmentId() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByAppointmentId(null, TENANT_ID);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find check-ins by status")
        void shouldFindByStatus() {
            List<PatientCheckInEntity> checkedInPatients = checkInRepository.findByTenantIdAndStatusOrderByCheckInTimeDesc(TENANT_ID, "checked-in");

            assertThat(checkedInPatients).hasSize(1);
            assertThat(checkedInPatients.get(0).getStatus()).isEqualTo("checked-in");
        }

        @Test
        @DisplayName("Should find pending insurance verification")
        void shouldFindPendingInsuranceVerification() {
            List<PatientCheckInEntity> pending = checkInRepository.findPendingInsuranceVerification(TENANT_ID);

            assertThat(pending).hasSize(2); // checkIn2 and checkIn3
            assertThat(pending).allMatch(c -> !c.getInsuranceVerified());
        }
    }

    @Nested
    @DisplayName("Limit-Based Queries")
    class LimitBasedQueryTests {

        @Test
        @DisplayName("Should find recent check-ins with limit")
        void shouldFindRecentCheckInsWithLimit() {
            List<PatientCheckInEntity> recent = checkInRepository.findRecentCheckIns(TENANT_ID, 2);

            assertThat(recent).hasSize(2);
        }

        @Test
        @DisplayName("Should return all check-ins when limit exceeds count")
        void shouldReturnAllWhenLimitExceedsCount() {
            List<PatientCheckInEntity> recent = checkInRepository.findRecentCheckIns(TENANT_ID, 10);

            assertThat(recent).hasSize(3);
        }

        @Test
        @DisplayName("Should order recent check-ins by time descending")
        void shouldOrderRecentCheckInsByTimeDesc() {
            List<PatientCheckInEntity> recent = checkInRepository.findRecentCheckIns(TENANT_ID, 3);

            assertThat(recent).hasSize(3);
            // Verify descending order
            for (int i = 0; i < recent.size() - 1; i++) {
                assertThat(recent.get(i).getCheckInTime())
                    .isAfterOrEqualTo(recent.get(i + 1).getCheckInTime());
            }
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate data between tenants")
        void shouldIsolateDataBetweenTenants() {
            // Create check-in for another tenant
            PatientCheckInEntity otherTenantCheckIn = createCheckIn(OTHER_TENANT, PATIENT_ID_1, "APT-999", "checked-in", false, false, false);
            checkInRepository.save(otherTenantCheckIn);

            // Query for first tenant
            List<PatientCheckInEntity> tenant1CheckIns = checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(TENANT_ID, PATIENT_ID_1);

            // Query for second tenant
            List<PatientCheckInEntity> tenant2CheckIns = checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(OTHER_TENANT, PATIENT_ID_1);

            assertThat(tenant1CheckIns).noneMatch(c -> c.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2CheckIns).noneMatch(c -> c.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not allow cross-tenant access via ID query")
        void shouldNotAllowCrossTenantAccessById() {
            Optional<PatientCheckInEntity> result = checkInRepository.findByIdAndTenantId(checkIn1.getId(), OTHER_TENANT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should count only tenant's own check-ins")
        void shouldCountOnlyTenantOwnCheckIns() {
            // Create check-in for another tenant
            PatientCheckInEntity otherTenantCheckIn = createCheckIn(OTHER_TENANT, PATIENT_ID_1, "APT-999", "checked-in", false, false, false);
            checkInRepository.save(otherTenantCheckIn);

            long tenant1Count = checkInRepository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID_1);
            long tenant2Count = checkInRepository.countByTenantIdAndPatientId(OTHER_TENANT, PATIENT_ID_1);

            assertThat(tenant1Count).isEqualTo(2);
            assertThat(tenant2Count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter today's check-ins by tenant")
        void shouldFilterTodayCheckInsByTenant() {
            // Create check-in for another tenant
            PatientCheckInEntity otherTenantCheckIn = createCheckIn(OTHER_TENANT, PATIENT_ID_1, "APT-999", "checked-in", false, false, false);
            checkInRepository.save(otherTenantCheckIn);

            List<PatientCheckInEntity> tenant1Today = checkInRepository.findTodayCheckInsByTenant(TENANT_ID, LocalDate.now());
            List<PatientCheckInEntity> tenant2Today = checkInRepository.findTodayCheckInsByTenant(OTHER_TENANT, LocalDate.now());

            assertThat(tenant1Today).hasSize(3);
            assertThat(tenant2Today).hasSize(1);
            assertThat(tenant1Today).noneMatch(c -> c.getTenantId().equals(OTHER_TENANT));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle non-existent patient ID")
        void shouldHandleNonExistentPatientId() {
            UUID nonExistentId = UUID.randomUUID();
            Optional<PatientCheckInEntity> found = checkInRepository.findByPatientIdAndTenantId(nonExistentId, TENANT_ID);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle non-existent appointment ID")
        void shouldHandleNonExistentAppointmentId() {
            Optional<PatientCheckInEntity> found = checkInRepository.findByAppointmentId("NON-EXISTENT", TENANT_ID);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty status search")
        void shouldHandleEmptyStatusSearch() {
            List<PatientCheckInEntity> found = checkInRepository.findByTenantIdAndStatusOrderByCheckInTimeDesc(TENANT_ID, "non-existent-status");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle zero limit in recent query")
        void shouldHandleZeroLimit() {
            List<PatientCheckInEntity> found = checkInRepository.findRecentCheckIns(TENANT_ID, 0);

            assertThat(found).isEmpty();
        }
    }

    // Helper method
    private PatientCheckInEntity createCheckIn(String tenantId, UUID patientId, String appointmentId,
                                                String status, boolean insuranceVerified,
                                                boolean demographicsUpdated, boolean consentObtained) {
        return PatientCheckInEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .checkInTime(Instant.now())
                .checkedInBy("test-ma")
                .status(status)
                .insuranceVerified(insuranceVerified)
                .demographicsUpdated(demographicsUpdated)
                .consentObtained(consentObtained)
                .build();
    }
}
