package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.config.BaseIntegrationTest;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vital Signs Record Repository Integration Tests
 *
 * Tests database operations for vital signs tracking with real PostgreSQL
 * via Testcontainers. Covers all repository queries including:
 * - Alert-based queries
 * - Patient vitals history
 * - Latest vitals retrieval
 * - Multi-tenant isolation
 */
@BaseIntegrationTest
@DisplayName("VitalSignsRecordRepository Integration Tests")
class VitalSignsRecordRepositoryIntegrationTest {

    @Autowired
    private VitalSignsRecordRepository vitalSignsRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    private VitalSignsRecordEntity normalVitals;
    private VitalSignsRecordEntity warningVitals;
    private VitalSignsRecordEntity criticalVitals;

    @BeforeEach
    void setUp() {
        vitalSignsRepository.deleteAll();
        
        // Create test vital signs records
        normalVitals = createVitals(TENANT_ID, PATIENT_ID_1, "120", "80", "72", "98.6", "normal", null);
        warningVitals = createVitals(TENANT_ID, PATIENT_ID_1, "140", "90", "85", "99.5", "warning", "Elevated BP and HR");
        criticalVitals = createVitals(TENANT_ID, PATIENT_ID_2, "180", "110", "120", "103.2", "critical", "Critical BP and Fever");

        normalVitals = vitalSignsRepository.save(normalVitals);
        warningVitals = vitalSignsRepository.save(warningVitals);
        criticalVitals = vitalSignsRepository.save(criticalVitals);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve vitals by ID")
        void shouldSaveAndRetrieve() {
            Optional<VitalSignsRecordEntity> found = vitalSignsRepository.findById(normalVitals.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
            assertThat(found.get().getAlertStatus()).isEqualTo("normal");
        }

        @Test
        @DisplayName("Should find vitals by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<VitalSignsRecordEntity> found = vitalSignsRepository.findByIdAndTenantId(normalVitals.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should auto-generate timestamps on create")
        void shouldAutoGenerateTimestamps() {
            VitalSignsRecordEntity newVitals = createVitals(TENANT_ID, PATIENT_ID_1, "118", "78", "70", "98.4", "normal", null);
            newVitals.setCreatedAt(null);
            newVitals.setUpdatedAt(null);

            VitalSignsRecordEntity saved = vitalSignsRepository.save(newVitals);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getRecordedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Alert-Based Queries")
    class AlertBasedQueryTests {

        @Test
        @DisplayName("Should find abnormal vitals by tenant and alert status")
        void shouldFindAbnormalVitalsByTenant() {
            List<VitalSignsRecordEntity> warnings = vitalSignsRepository.findAbnormalVitalsByTenant(TENANT_ID, "warning");

            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0).getAlertStatus()).isEqualTo("warning");
            assertThat(warnings.get(0).getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should find critical alerts")
        void shouldFindCriticalAlerts() {
            List<VitalSignsRecordEntity> critical = vitalSignsRepository.findAbnormalVitalsByTenant(TENANT_ID, "critical");

            assertThat(critical).hasSize(1);
            assertThat(critical.get(0).getAlertStatus()).isEqualTo("critical");
            assertThat(critical.get(0).getAlertMessage()).contains("Critical");
        }

        @Test
        @DisplayName("Should find vitals by alert status and tenant")
        void shouldFindByAlertStatusAndTenant() {
            List<VitalSignsRecordEntity> normalVitalsList = vitalSignsRepository.findByAlertStatusAndTenant("normal", TENANT_ID);

            assertThat(normalVitalsList).hasSize(1);
            assertThat(normalVitalsList.get(0).getAlertStatus()).isEqualTo("normal");
        }

        @Test
        @DisplayName("Should count critical alerts by tenant")
        void shouldCountCriticalAlertsByTenant() {
            long count = vitalSignsRepository.countCriticalAlertsByTenant(TENANT_ID);

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Patient Vitals History Queries")
    class PatientVitalsHistoryTests {

        @Test
        @DisplayName("Should find patient vitals history within date range")
        void shouldFindPatientVitalsHistory() {
            LocalDateTime from = LocalDateTime.now().minusHours(1);
            LocalDateTime to = LocalDateTime.now().plusHours(1);

            List<VitalSignsRecordEntity> history = vitalSignsRepository.findPatientVitalsHistory(
                PATIENT_ID_1, TENANT_ID, from, to
            );

            assertThat(history).hasSize(2); // normalVitals and warningVitals
            assertThat(history).allMatch(v -> v.getPatientId().equals(PATIENT_ID_1));
        }

        @Test
        @DisplayName("Should not return vitals outside date range")
        void shouldNotReturnVitalsOutsideDateRange() {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().minusDays(6);

            List<VitalSignsRecordEntity> history = vitalSignsRepository.findPatientVitalsHistory(
                PATIENT_ID_1, TENANT_ID, from, to
            );

            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("Should order vitals history by recorded time descending")
        void shouldOrderHistoryByTimeDesc() {
            // Create additional vitals at different times
            VitalSignsRecordEntity older = createVitals(TENANT_ID, PATIENT_ID_1, "115", "75", "68", "98.2", "normal", null);
            older.setRecordedAt(Instant.now().minusSeconds(7200));
            vitalSignsRepository.save(older);

            LocalDateTime from = LocalDateTime.now().minusHours(3);
            LocalDateTime to = LocalDateTime.now().plusHours(1);

            List<VitalSignsRecordEntity> history = vitalSignsRepository.findPatientVitalsHistory(
                PATIENT_ID_1, TENANT_ID, from, to
            );

            // Verify descending order
            for (int i = 0; i < history.size() - 1; i++) {
                assertThat(history.get(i).getRecordedAt())
                    .isAfterOrEqualTo(history.get(i + 1).getRecordedAt());
            }
        }

        @Test
        @DisplayName("Should find all vitals for patient")
        void shouldFindAllVitalsForPatient() {
            List<VitalSignsRecordEntity> vitals = vitalSignsRepository.findByTenantIdAndPatientIdOrderByRecordedAtDesc(TENANT_ID, PATIENT_ID_1);

            assertThat(vitals).hasSize(2);
            assertThat(vitals).allMatch(v -> v.getPatientId().equals(PATIENT_ID_1));
        }
    }

    @Nested
    @DisplayName("Latest Vitals Queries")
    class LatestVitalsTests {

        @Test
        @DisplayName("Should find latest vital for patient")
        void shouldFindLatestVitalForPatient() {
            // Create additional vitals at different times
            VitalSignsRecordEntity latest = createVitals(TENANT_ID, PATIENT_ID_1, "125", "82", "74", "98.8", "normal", null);
            latest.setRecordedAt(Instant.now().plusSeconds(1800));
            vitalSignsRepository.save(latest);

            Optional<VitalSignsRecordEntity> found = vitalSignsRepository.findLatestVitalForPatient(PATIENT_ID_1, TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getSystolicBp()).isEqualByComparingTo(new BigDecimal("125"));
        }

        @Test
        @DisplayName("Should handle patient with no vitals")
        void shouldHandlePatientWithNoVitals() {
            UUID nonExistentPatient = UUID.randomUUID();
            Optional<VitalSignsRecordEntity> found = vitalSignsRepository.findLatestVitalForPatient(nonExistentPatient, TENANT_ID);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Encounter-Based Queries")
    class EncounterBasedQueryTests {

        @Test
        @DisplayName("Should find vitals by encounter ID")
        void shouldFindByEncounterId() {
            normalVitals.setEncounterId("ENC-001");
            warningVitals.setEncounterId("ENC-001");
            vitalSignsRepository.save(normalVitals);
            vitalSignsRepository.save(warningVitals);

            List<VitalSignsRecordEntity> encounterVitals = vitalSignsRepository.findByTenantIdAndEncounterIdOrderByRecordedAtDesc(
                TENANT_ID, "ENC-001"
            );

            assertThat(encounterVitals).hasSize(2);
            assertThat(encounterVitals).allMatch(v -> "ENC-001".equals(v.getEncounterId()));
        }
    }

    @Nested
    @DisplayName("Staff-Based Queries")
    class StaffBasedQueryTests {

        @Test
        @DisplayName("Should find vitals recorded by specific staff")
        void shouldFindByRecordedBy() {
            normalVitals.setRecordedBy("MA-001");
            warningVitals.setRecordedBy("MA-001");
            criticalVitals.setRecordedBy("MA-002");
            vitalSignsRepository.save(normalVitals);
            vitalSignsRepository.save(warningVitals);
            vitalSignsRepository.save(criticalVitals);

            List<VitalSignsRecordEntity> ma001Vitals = vitalSignsRepository.findByTenantIdAndRecordedByOrderByRecordedAtDesc(
                TENANT_ID, "MA-001"
            );

            assertThat(ma001Vitals).hasSize(2);
            assertThat(ma001Vitals).allMatch(v -> "MA-001".equals(v.getRecordedBy()));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate data between tenants")
        void shouldIsolateDataBetweenTenants() {
            // Create vitals for another tenant
            VitalSignsRecordEntity otherTenantVitals = createVitals(OTHER_TENANT, PATIENT_ID_1, "130", "85", "78", "99.0", "normal", null);
            vitalSignsRepository.save(otherTenantVitals);

            // Query for first tenant
            List<VitalSignsRecordEntity> tenant1Vitals = vitalSignsRepository.findByTenantIdAndPatientIdOrderByRecordedAtDesc(TENANT_ID, PATIENT_ID_1);

            // Query for second tenant
            List<VitalSignsRecordEntity> tenant2Vitals = vitalSignsRepository.findByTenantIdAndPatientIdOrderByRecordedAtDesc(OTHER_TENANT, PATIENT_ID_1);

            assertThat(tenant1Vitals).noneMatch(v -> v.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Vitals).noneMatch(v -> v.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not allow cross-tenant access via ID query")
        void shouldNotAllowCrossTenantAccessById() {
            Optional<VitalSignsRecordEntity> result = vitalSignsRepository.findByIdAndTenantId(normalVitals.getId(), OTHER_TENANT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter abnormal vitals by tenant")
        void shouldFilterAbnormalVitalsByTenant() {
            // Create warning vitals for another tenant
            VitalSignsRecordEntity otherTenantWarning = createVitals(OTHER_TENANT, PATIENT_ID_1, "145", "92", "88", "99.8", "warning", "Elevated");
            vitalSignsRepository.save(otherTenantWarning);

            List<VitalSignsRecordEntity> tenant1Warnings = vitalSignsRepository.findAbnormalVitalsByTenant(TENANT_ID, "warning");
            List<VitalSignsRecordEntity> tenant2Warnings = vitalSignsRepository.findAbnormalVitalsByTenant(OTHER_TENANT, "warning");

            assertThat(tenant1Warnings).hasSize(1);
            assertThat(tenant2Warnings).hasSize(1);
            assertThat(tenant1Warnings).noneMatch(v -> v.getTenantId().equals(OTHER_TENANT));
        }

        @Test
        @DisplayName("Should count only tenant's own critical alerts")
        void shouldCountOnlyTenantOwnCriticalAlerts() {
            // Create critical vitals for another tenant
            VitalSignsRecordEntity otherTenantCritical = createVitals(OTHER_TENANT, PATIENT_ID_1, "190", "115", "130", "104.0", "critical", "Severe");
            vitalSignsRepository.save(otherTenantCritical);

            long tenant1Count = vitalSignsRepository.countCriticalAlertsByTenant(TENANT_ID);
            long tenant2Count = vitalSignsRepository.countCriticalAlertsByTenant(OTHER_TENANT);

            assertThat(tenant1Count).isEqualTo(1);
            assertThat(tenant2Count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle non-existent patient ID")
        void shouldHandleNonExistentPatientId() {
            UUID nonExistentId = UUID.randomUUID();
            Optional<VitalSignsRecordEntity> found = vitalSignsRepository.findLatestVitalForPatient(nonExistentId, TENANT_ID);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle non-existent alert status")
        void shouldHandleNonExistentAlertStatus() {
            List<VitalSignsRecordEntity> found = vitalSignsRepository.findByAlertStatusAndTenant("non-existent", TENANT_ID);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty date range")
        void shouldHandleEmptyDateRange() {
            LocalDateTime from = LocalDateTime.now().plusDays(1);
            LocalDateTime to = LocalDateTime.now().plusDays(2);

            List<VitalSignsRecordEntity> found = vitalSignsRepository.findPatientVitalsHistory(PATIENT_ID_1, TENANT_ID, from, to);

            assertThat(found).isEmpty();
        }
    }

    // Helper method
    private VitalSignsRecordEntity createVitals(String tenantId, UUID patientId, String systolic, String diastolic,
                                                 String heartRate, String temp, String alertStatus, String alertMessage) {
        return VitalSignsRecordEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .recordedBy("test-ma")
                .systolicBp(new BigDecimal(systolic))
                .diastolicBp(new BigDecimal(diastolic))
                .heartRate(new BigDecimal(heartRate))
                .temperatureF(new BigDecimal(temp))
                .oxygenSaturation(new BigDecimal("98"))
                .respirationRate(new BigDecimal("16"))
                .alertStatus(alertStatus)
                .alertMessage(alertMessage)
                .recordedAt(Instant.now())
                .build();
    }
}
