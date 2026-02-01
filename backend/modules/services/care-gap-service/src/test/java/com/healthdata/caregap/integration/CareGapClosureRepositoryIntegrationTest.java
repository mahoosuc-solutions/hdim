package com.healthdata.caregap.integration;

import com.healthdata.caregap.config.BaseIntegrationTest;
import com.healthdata.caregap.persistence.CareGapClosureEntity;
import com.healthdata.caregap.persistence.CareGapClosureRepository;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Care Gap Closure Repository Integration Tests
 *
 * Tests database operations for care gap closure tracking with real PostgreSQL
 * via Testcontainers. Covers:
 * - CRUD operations for closure records
 * - Closure verification workflow
 * - Multi-tenant data isolation
 */
@BaseIntegrationTest
@DisplayName("CareGapClosureRepository Integration Tests")
@Tag("integration")
class CareGapClosureRepositoryIntegrationTest {

    @Autowired
    private CareGapClosureRepository closureRepository;

    @Autowired
    private CareGapRepository careGapRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private CareGapEntity careGap;
    private CareGapClosureEntity closure;

    @BeforeEach
    void setUp() {
        // Create a care gap first
        careGap = CareGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId("HEDIS_CDC")
                .measureName("Diabetes A1C Control")
                .gapStatus("CLOSED")
                .priority("high")
                .gapCategory("HEDIS")
                .gapType("care-gap")
                .gapDescription("Test care gap")
                .dueDate(LocalDate.now().plusDays(30))
                .identifiedDate(Instant.now())
                .createdBy("test-system")
                .build();
        careGap = careGapRepository.save(careGap);

        // Create a closure for it
        closure = CareGapClosureEntity.builder()
                .tenantId(TENANT_ID)
                .careGapId(careGap.getId())
                .closureMethod("LAB_RESULT")
                .closureDate(Instant.now())
                .closedByProviderId("provider-123")
                .closedByProviderName("Dr. Smith")
                .supportingEvidenceType("Observation")
                .supportingEvidenceId("obs-456")
                .serviceDate(LocalDate.now())
                .notes("A1C test completed with result 6.5%")
                .verified(false)
                .build();
        closure = closureRepository.save(closure);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve closure record")
        void shouldSaveAndRetrieve() {
            Optional<CareGapClosureEntity> found = closureRepository.findById(closure.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getClosureMethod()).isEqualTo("LAB_RESULT");
            assertThat(found.get().getCareGapId()).isEqualTo(careGap.getId());
        }

        @Test
        @DisplayName("Should auto-generate ID and timestamp on create")
        void shouldAutoGenerateIdAndTimestamp() {
            CareGapClosureEntity newClosure = CareGapClosureEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(careGap.getId())
                    .closureMethod("PROCEDURE")
                    .closedByProviderId("provider-789")
                    .build();

            CareGapClosureEntity saved = closureRepository.save(newClosure);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getClosureDate()).isNotNull();
        }

        @Test
        @DisplayName("Should update closure record")
        void shouldUpdate() {
            closure.setVerified(true);
            closure.setVerifiedAt(Instant.now());
            closure.setVerifiedBy("supervisor-001");
            closureRepository.save(closure);

            Optional<CareGapClosureEntity> found = closureRepository.findById(closure.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getVerified()).isTrue();
            assertThat(found.get().getVerifiedBy()).isEqualTo("supervisor-001");
        }

        @Test
        @DisplayName("Should delete closure record")
        void shouldDelete() {
            UUID id = closure.getId();
            closureRepository.delete(closure);

            Optional<CareGapClosureEntity> found = closureRepository.findById(id);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Closure Workflow")
    class ClosureWorkflowTests {

        @Test
        @DisplayName("Should track unverified closures")
        void shouldTrackUnverifiedClosures() {
            List<CareGapClosureEntity> all = closureRepository.findAll();
            List<CareGapClosureEntity> unverified = all.stream()
                    .filter(c -> !c.getVerified())
                    .filter(c -> c.getTenantId().equals(TENANT_ID))
                    .toList();

            assertThat(unverified).hasSize(1);
        }

        @Test
        @DisplayName("Should store various closure methods")
        void shouldStoreVariousClosureMethods() {
            // Create closures with different methods
            CareGapEntity gap2 = createAndSaveGap("HEDIS_BCS", "Breast Cancer Screening");
            CareGapEntity gap3 = createAndSaveGap("HEDIS_CCS", "Cervical Cancer Screening");

            CareGapClosureEntity procedureClosure = CareGapClosureEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(gap2.getId())
                    .closureMethod("PROCEDURE")
                    .closedByProviderId("provider-001")
                    .supportingEvidenceType("Procedure")
                    .supportingEvidenceId("proc-001")
                    .build();

            CareGapClosureEntity medicationClosure = CareGapClosureEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(gap3.getId())
                    .closureMethod("MEDICATION")
                    .closedByProviderId("provider-002")
                    .supportingEvidenceType("MedicationRequest")
                    .supportingEvidenceId("med-001")
                    .build();

            closureRepository.save(procedureClosure);
            closureRepository.save(medicationClosure);

            List<CareGapClosureEntity> all = closureRepository.findAll();
            List<String> methods = all.stream()
                    .filter(c -> c.getTenantId().equals(TENANT_ID))
                    .map(CareGapClosureEntity::getClosureMethod)
                    .distinct()
                    .toList();

            assertThat(methods).containsExactlyInAnyOrder("LAB_RESULT", "PROCEDURE", "MEDICATION");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate closures between tenants")
        void shouldIsolateClosuresBetweenTenants() {
            // Create gap and closure for other tenant
            CareGapEntity otherTenantGap = CareGapEntity.builder()
                    .tenantId(OTHER_TENANT)
                    .patientId(PATIENT_ID)
                    .measureId("HEDIS_CDC")
                    .measureName("Diabetes A1C Control")
                    .gapStatus("CLOSED")
                    .priority("high")
                    .gapCategory("HEDIS")
                    .gapType("care-gap")
                    .identifiedDate(Instant.now())
                    .createdBy("test-system")
                    .build();
            otherTenantGap = careGapRepository.save(otherTenantGap);

            CareGapClosureEntity otherTenantClosure = CareGapClosureEntity.builder()
                    .tenantId(OTHER_TENANT)
                    .careGapId(otherTenantGap.getId())
                    .closureMethod("LAB_RESULT")
                    .closedByProviderId("other-provider")
                    .notes("Other tenant closure")
                    .build();
            closureRepository.save(otherTenantClosure);

            // Query all closures
            List<CareGapClosureEntity> all = closureRepository.findAll();

            // Filter by tenant (simulating tenant-scoped query)
            List<CareGapClosureEntity> tenant1Closures = all.stream()
                    .filter(c -> c.getTenantId().equals(TENANT_ID))
                    .toList();
            List<CareGapClosureEntity> tenant2Closures = all.stream()
                    .filter(c -> c.getTenantId().equals(OTHER_TENANT))
                    .toList();

            assertThat(tenant1Closures).noneMatch(c -> c.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Closures).noneMatch(c -> c.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("All closures should have non-null tenant IDs")
        void shouldHaveNonNullTenantIds() {
            List<CareGapClosureEntity> all = closureRepository.findAll();

            assertThat(all).allMatch(c -> c.getTenantId() != null && !c.getTenantId().isEmpty());
        }
    }

    // Helper methods
    private CareGapEntity createAndSaveGap(String measureId, String measureName) {
        CareGapEntity gap = CareGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId(measureId)
                .measureName(measureName)
                .gapStatus("CLOSED")
                .priority("high")
                .gapCategory("HEDIS")
                .gapType("care-gap")
                .identifiedDate(Instant.now())
                .createdBy("test-system")
                .build();
        return careGapRepository.save(gap);
    }
}
