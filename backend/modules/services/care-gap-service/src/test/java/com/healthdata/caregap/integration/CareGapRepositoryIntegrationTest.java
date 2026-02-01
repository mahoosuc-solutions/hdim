package com.healthdata.caregap.integration;

import com.healthdata.caregap.config.BaseIntegrationTest;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Care Gap Repository Integration Tests
 *
 * Tests database operations for care gap persistence with real PostgreSQL
 * via Testcontainers. Covers all repository queries including:
 * - Basic CRUD operations
 * - Tenant-filtered queries
 * - Status-based queries (open, closed, high priority)
 * - Measure and category queries
 * - Date-based queries (overdue, due in range)
 * - Analytics queries (counts, aggregations)
 */
@Tag("integration")
@BaseIntegrationTest
@DisplayName("CareGapRepository Integration Tests")
class CareGapRepositoryIntegrationTest {

    @Autowired
    private CareGapRepository careGapRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    private CareGapEntity openGap1;
    private CareGapEntity openGap2;
    private CareGapEntity closedGap;
    private CareGapEntity highPriorityGap;

    @BeforeEach
    void setUp() {
        // Create test care gaps
        openGap1 = createCareGap(TENANT_ID, PATIENT_ID_1, "HEDIS_CDC", "Diabetes A1C Control",
                "OPEN", "medium", "HEDIS", LocalDate.now().plusDays(30));
        openGap2 = createCareGap(TENANT_ID, PATIENT_ID_1, "HEDIS_BCS", "Breast Cancer Screening",
                "OPEN", "high", "HEDIS", LocalDate.now().plusDays(60));
        closedGap = createCareGap(TENANT_ID, PATIENT_ID_1, "HEDIS_CCS", "Cervical Cancer Screening",
                "CLOSED", "low", "HEDIS", LocalDate.now().minusDays(10));
        highPriorityGap = createCareGap(TENANT_ID, PATIENT_ID_2, "CMS_402", "Diabetes Prevention",
                "OPEN", "high", "CMS", LocalDate.now().plusDays(7));

        openGap1 = careGapRepository.save(openGap1);
        openGap2 = careGapRepository.save(openGap2);
        closedGap = careGapRepository.save(closedGap);
        closedGap.setClosedDate(Instant.now().minus(5, ChronoUnit.DAYS));
        closedGap = careGapRepository.save(closedGap);
        highPriorityGap = careGapRepository.save(highPriorityGap);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve care gap by ID")
        void shouldSaveAndRetrieve() {
            Optional<CareGapEntity> found = careGapRepository.findById(openGap1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getMeasureId()).isEqualTo("HEDIS_CDC");
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should find care gap by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<CareGapEntity> found = careGapRepository.findByIdAndTenantId(openGap1.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should not find care gap with wrong tenant")
        void shouldNotFindWithWrongTenant() {
            Optional<CareGapEntity> found = careGapRepository.findByIdAndTenantId(openGap1.getId(), OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update care gap")
        void shouldUpdate() {
            openGap1.setGapStatus("IN_PROGRESS");
            openGap1.setPriority("high");
            careGapRepository.save(openGap1);

            Optional<CareGapEntity> found = careGapRepository.findById(openGap1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getGapStatus()).isEqualTo("IN_PROGRESS");
            assertThat(found.get().getPriority()).isEqualTo("high");
        }

        @Test
        @DisplayName("Should delete care gap")
        void shouldDelete() {
            UUID id = openGap1.getId();
            careGapRepository.delete(openGap1);

            Optional<CareGapEntity> found = careGapRepository.findById(id);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should auto-generate timestamp on create")
        void shouldAutoGenerateTimestamp() {
            CareGapEntity newGap = createCareGap(TENANT_ID, PATIENT_ID_1, "TEST_001", "Test Measure",
                    "OPEN", "low", "HEDIS", LocalDate.now().plusDays(30));
            newGap.setCreatedAt(null);
            newGap.setUpdatedAt(null);

            CareGapEntity saved = careGapRepository.save(newGap);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Patient-Based Queries")
    class PatientBasedQueryTests {

        @Test
        @DisplayName("Should find all care gaps by tenant and patient")
        void shouldFindByTenantAndPatient() {
            List<CareGapEntity> gaps = careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID_1);

            assertThat(gaps).hasSize(3);
            assertThat(gaps).allMatch(g -> g.getPatientId().equals(PATIENT_ID_1));
        }

        @Test
        @DisplayName("Should not return other tenant's patient data")
        void shouldNotReturnOtherTenantPatientData() {
            // Create gap for other tenant with same patient ID
            CareGapEntity otherTenantGap = createCareGap(OTHER_TENANT, PATIENT_ID_1, "OTHER_001", "Other Measure",
                    "OPEN", "high", "HEDIS", LocalDate.now().plusDays(30));
            careGapRepository.save(otherTenantGap);

            List<CareGapEntity> gaps = careGapRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID_1);

            assertThat(gaps).noneMatch(g -> g.getTenantId().equals(OTHER_TENANT));
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find open care gaps for patient")
        void shouldFindOpenGaps() {
            List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_ID_1);

            assertThat(openGaps).hasSize(2);
            assertThat(openGaps).allMatch(g -> g.getGapStatus().equals("OPEN"));
        }

        @Test
        @DisplayName("Should find high priority open gaps")
        void shouldFindHighPriorityOpenGaps() {
            List<CareGapEntity> highPriorityGaps = careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_ID_1);

            assertThat(highPriorityGaps).hasSize(1);
            assertThat(highPriorityGaps.get(0).getPriority()).isEqualTo("high");
            assertThat(highPriorityGaps.get(0).getGapStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("Should find closed care gaps")
        void shouldFindClosedGaps() {
            List<CareGapEntity> closedGaps = careGapRepository.findClosedGapsByPatient(TENANT_ID, PATIENT_ID_1);

            assertThat(closedGaps).hasSize(1);
            assertThat(closedGaps.get(0).getGapStatus()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("Should find all open gaps for tenant")
        void shouldFindAllOpenGapsForTenant() {
            List<CareGapEntity> allOpenGaps = careGapRepository.findAllOpenGaps(TENANT_ID);

            assertThat(allOpenGaps).hasSize(3); // 2 for patient1 + 1 for patient2
            assertThat(allOpenGaps).allMatch(g -> g.getGapStatus().equals("OPEN"));
        }
    }

    @Nested
    @DisplayName("Measure-Based Queries")
    class MeasureBasedQueryTests {

        @Test
        @DisplayName("Should find care gaps by measure ID")
        void shouldFindByMeasure() {
            List<CareGapEntity> gaps = careGapRepository.findByMeasure(TENANT_ID, PATIENT_ID_1, "HEDIS_CDC");

            assertThat(gaps).hasSize(1);
            assertThat(gaps.get(0).getMeasureId()).isEqualTo("HEDIS_CDC");
        }

        @Test
        @DisplayName("Should find care gaps by measure category")
        void shouldFindByMeasureCategory() {
            List<CareGapEntity> hedisGaps = careGapRepository.findByMeasureCategory(TENANT_ID, PATIENT_ID_1, "HEDIS");

            assertThat(hedisGaps).hasSize(2); // openGap1 and openGap2 (closedGap excluded)
            assertThat(hedisGaps).allMatch(g -> g.getGapCategory().equals("HEDIS"));
        }

        @Test
        @DisplayName("Should find care gaps by measure year")
        void shouldFindByMeasureYear() {
            openGap1.setMeasureYear(2024);
            careGapRepository.save(openGap1);

            List<CareGapEntity> gaps = careGapRepository.findByMeasureYear(TENANT_ID, 2024);

            assertThat(gaps).hasSize(1);
            assertThat(gaps.get(0).getMeasureYear()).isEqualTo(2024);
        }
    }

    @Nested
    @DisplayName("Date-Based Queries")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should find overdue care gaps")
        void shouldFindOverdueGaps() {
            // Create an overdue gap
            CareGapEntity overdueGap = createCareGap(TENANT_ID, PATIENT_ID_1, "HEDIS_COL", "Colorectal Screening",
                    "OPEN", "high", "HEDIS", LocalDate.now().minusDays(7));
            careGapRepository.save(overdueGap);

            List<CareGapEntity> overdueGaps = careGapRepository.findOverdueGaps(TENANT_ID, LocalDate.now());

            assertThat(overdueGaps).hasSize(1);
            assertThat(overdueGaps.get(0).getDueDate()).isBefore(LocalDate.now());
        }

        @Test
        @DisplayName("Should find gaps due in date range")
        void shouldFindGapsDueInRange() {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(45);

            List<CareGapEntity> gaps = careGapRepository.findGapsDueInRange(TENANT_ID, PATIENT_ID_1, startDate, endDate);

            assertThat(gaps).hasSize(1); // Only openGap1 (due in 30 days)
            assertThat(gaps).allMatch(g ->
                !g.getDueDate().isBefore(startDate) && !g.getDueDate().isAfter(endDate));
        }

        @Test
        @DisplayName("Should find gaps identified in date range")
        void shouldFindGapsIdentifiedInRange() {
            Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.HOURS);

            List<CareGapEntity> gaps = careGapRepository.findGapsIdentifiedInRange(TENANT_ID, startDate, endDate);

            assertThat(gaps).hasSizeGreaterThanOrEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Analytics Queries")
    class AnalyticsQueryTests {

        @Test
        @DisplayName("Should count open gaps for patient")
        void shouldCountOpenGaps() {
            long count = careGapRepository.countOpenGaps(TENANT_ID, PATIENT_ID_1);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count high priority gaps")
        void shouldCountHighPriorityGaps() {
            long count = careGapRepository.countHighPriorityGaps(TENANT_ID, PATIENT_ID_1);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count overdue gaps")
        void shouldCountOverdueGaps() {
            // Create an overdue gap
            CareGapEntity overdueGap = createCareGap(TENANT_ID, PATIENT_ID_1, "HEDIS_COL", "Colorectal Screening",
                    "OPEN", "high", "HEDIS", LocalDate.now().minusDays(7));
            careGapRepository.save(overdueGap);

            long count = careGapRepository.countOverdueGaps(TENANT_ID, PATIENT_ID_1, LocalDate.now());

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count gaps by status")
        void shouldCountGapsByStatus() {
            List<Object[]> statusCounts = careGapRepository.countGapsByStatus(TENANT_ID);

            assertThat(statusCounts).isNotEmpty();
            // Should have OPEN and CLOSED
            assertThat(statusCounts).anyMatch(arr -> "OPEN".equals(arr[0]));
            assertThat(statusCounts).anyMatch(arr -> "CLOSED".equals(arr[0]));
        }

        @Test
        @DisplayName("Should count gaps by priority")
        void shouldCountGapsByPriority() {
            List<Object[]> priorityCounts = careGapRepository.countGapsByPriority(TENANT_ID);

            assertThat(priorityCounts).isNotEmpty();
        }

        @Test
        @DisplayName("Should count gaps by category")
        void shouldCountGapsByCategory() {
            List<Object[]> categoryCounts = careGapRepository.countGapsByCategory(TENANT_ID);

            assertThat(categoryCounts).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Boolean Check Queries")
    class BooleanCheckQueryTests {

        @Test
        @DisplayName("Should check if patient has open gap for measure")
        void shouldCheckHasOpenGapForMeasure() {
            boolean hasGap = careGapRepository.hasOpenGapForMeasure(TENANT_ID, PATIENT_ID_1, "HEDIS_CDC");

            assertThat(hasGap).isTrue();
        }

        @Test
        @DisplayName("Should return false for closed measure gap")
        void shouldReturnFalseForClosedMeasureGap() {
            boolean hasGap = careGapRepository.hasOpenGapForMeasure(TENANT_ID, PATIENT_ID_1, "HEDIS_CCS");

            assertThat(hasGap).isFalse(); // CCS is closed
        }

        @Test
        @DisplayName("Should check if patient has high priority gap")
        void shouldCheckHasHighPriorityGap() {
            boolean hasHighPriority = careGapRepository.hasHighPriorityGap(TENANT_ID, PATIENT_ID_1);

            assertThat(hasHighPriority).isTrue();
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate data between tenants")
        void shouldIsolateDataBetweenTenants() {
            // Create gaps for another tenant
            CareGapEntity otherTenantGap = createCareGap(OTHER_TENANT, PATIENT_ID_1, "OTHER_001", "Other Measure",
                    "OPEN", "high", "HEDIS", LocalDate.now().plusDays(30));
            careGapRepository.save(otherTenantGap);

            // Query for first tenant
            List<CareGapEntity> tenant1Gaps = careGapRepository.findAllOpenGaps(TENANT_ID);

            // Query for second tenant
            List<CareGapEntity> tenant2Gaps = careGapRepository.findAllOpenGaps(OTHER_TENANT);

            assertThat(tenant1Gaps).noneMatch(g -> g.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Gaps).noneMatch(g -> g.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not allow cross-tenant access via ID query")
        void shouldNotAllowCrossTenantAccessById() {
            // Try to access tenant1's gap using tenant2's query
            Optional<CareGapEntity> result = careGapRepository.findByIdAndTenantId(openGap1.getId(), OTHER_TENANT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should count only tenant's own gaps")
        void shouldCountOnlyTenantOwnGaps() {
            // Create gap for another tenant with same patient ID
            CareGapEntity otherTenantGap = createCareGap(OTHER_TENANT, PATIENT_ID_1, "OTHER_001", "Other Measure",
                    "OPEN", "high", "HEDIS", LocalDate.now().plusDays(30));
            careGapRepository.save(otherTenantGap);

            long tenant1Count = careGapRepository.countOpenGaps(TENANT_ID, PATIENT_ID_1);
            long tenant2Count = careGapRepository.countOpenGaps(OTHER_TENANT, PATIENT_ID_1);

            assertThat(tenant1Count).isEqualTo(2);
            assertThat(tenant2Count).isEqualTo(1);
        }
    }

    // Helper method
    private CareGapEntity createCareGap(String tenantId, UUID patientId, String measureId,
                                         String measureName, String status, String priority,
                                         String category, LocalDate dueDate) {
        return CareGapEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName(measureName)
                .gapStatus(status)
                .priority(priority)
                .gapCategory(category)
                .gapType("care-gap")
                .gapDescription("Test care gap for " + measureName)
                .dueDate(dueDate)
                .identifiedDate(Instant.now())
                .createdBy("test-system")
                .build();
    }
}
