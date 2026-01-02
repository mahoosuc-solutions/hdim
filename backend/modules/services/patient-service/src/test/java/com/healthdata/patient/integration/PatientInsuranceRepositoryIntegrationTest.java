package com.healthdata.patient.integration;

import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.entity.PatientInsuranceEntity;
import com.healthdata.patient.repository.PatientInsuranceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PatientInsuranceRepository.
 *
 * Tests CRUD operations, insurance coverage queries, and
 * multi-tenant isolation with real PostgreSQL via Testcontainers.
 */
@BaseIntegrationTest
@DisplayName("PatientInsuranceRepository Integration Tests")
class PatientInsuranceRepositoryIntegrationTest {

    @Autowired
    private PatientInsuranceRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private PatientInsuranceEntity testInsurance;

    @BeforeEach
    void setUp() {
        testInsurance = PatientInsuranceEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .coverageType("medical")
                .payerName("Blue Cross Blue Shield")
                .payerId("BCBS-001")
                .planName("Gold PPO Plan")
                .memberId("XYZ123456789")
                .groupNumber("GRP-987654")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .isPrimary(true)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save insurance coverage")
        void shouldSaveInsurance() {
            // When
            PatientInsuranceEntity saved = repository.save(testInsurance);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getPayerName()).isEqualTo("Blue Cross Blue Shield");
            assertThat(saved.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("Should find insurance by ID")
        void shouldFindInsuranceById() {
            // Given
            PatientInsuranceEntity saved = repository.save(testInsurance);

            // When
            Optional<PatientInsuranceEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getMemberId()).isEqualTo("XYZ123456789");
            assertThat(found.get().getPlanName()).isEqualTo("Gold PPO Plan");
        }

        @Test
        @DisplayName("Should update insurance coverage")
        void shouldUpdateInsurance() {
            // Given
            PatientInsuranceEntity saved = repository.save(testInsurance);
            saved.setPlanName("Platinum PPO Plan");
            saved.setTerminationDate(LocalDate.of(2025, 12, 31));

            // When
            PatientInsuranceEntity updated = repository.save(saved);

            // Then
            assertThat(updated.getPlanName()).isEqualTo("Platinum PPO Plan");
            assertThat(updated.getTerminationDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        }

        @Test
        @DisplayName("Should delete insurance coverage")
        void shouldDeleteInsurance() {
            // Given
            PatientInsuranceEntity saved = repository.save(testInsurance);
            UUID id = saved.getId();

            // When
            repository.deleteById(id);

            // Then
            assertThat(repository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Insurance Coverage Types")
    class CoverageTypeTests {

        @Test
        @DisplayName("Should store primary insurance")
        void shouldStorePrimaryInsurance() {
            // Given
            testInsurance.setIsPrimary(true);

            // When
            PatientInsuranceEntity saved = repository.save(testInsurance);

            // Then
            assertThat(saved.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("Should store secondary insurance")
        void shouldStoreSecondaryInsurance() {
            // Given - Save primary first
            repository.save(testInsurance);

            // Secondary insurance
            PatientInsuranceEntity secondary = PatientInsuranceEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .coverageType("medical")
                    .payerName("Aetna")
                    .payerId("AETNA-002")
                    .planName("Silver HMO")
                    .memberId("AETNA987654")
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .isPrimary(false)
                    .active(true)
                    .build();

            // When
            PatientInsuranceEntity saved = repository.save(secondary);

            // Then
            assertThat(saved.getIsPrimary()).isFalse();
            assertThat(saved.getPayerName()).isEqualTo("Aetna");
        }

        @Test
        @DisplayName("Should store different coverage types")
        void shouldStoreDifferentCoverageTypes() {
            // Given
            repository.save(testInsurance); // Medical

            PatientInsuranceEntity dental = PatientInsuranceEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .coverageType("dental")
                    .payerName("Delta Dental")
                    .planName("Premium Dental")
                    .memberId("DELTA123")
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .isPrimary(true)
                    .active(true)
                    .build();

            PatientInsuranceEntity vision = PatientInsuranceEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .coverageType("vision")
                    .payerName("VSP")
                    .planName("Vision Plus")
                    .memberId("VSP456")
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .isPrimary(true)
                    .active(true)
                    .build();

            // When
            repository.save(dental);
            repository.save(vision);

            // Then
            List<PatientInsuranceEntity> all = repository.findAll();
            assertThat(all).hasSizeGreaterThanOrEqualTo(3);

            assertThat(all.stream().map(PatientInsuranceEntity::getCoverageType))
                    .contains("medical", "dental", "vision");
        }
    }

    @Nested
    @DisplayName("Insurance Dates")
    class InsuranceDateTests {

        @Test
        @DisplayName("Should store effective date")
        void shouldStoreEffectiveDate() {
            // Given
            LocalDate effectiveDate = LocalDate.of(2024, 6, 1);
            testInsurance.setEffectiveDate(effectiveDate);

            // When
            PatientInsuranceEntity saved = repository.save(testInsurance);
            Optional<PatientInsuranceEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEffectiveDate()).isEqualTo(effectiveDate);
        }

        @Test
        @DisplayName("Should store termination date")
        void shouldStoreTerminationDate() {
            // Given
            LocalDate terminationDate = LocalDate.of(2025, 5, 31);
            testInsurance.setTerminationDate(terminationDate);

            // When
            PatientInsuranceEntity saved = repository.save(testInsurance);
            Optional<PatientInsuranceEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTerminationDate()).isEqualTo(terminationDate);
        }

        @Test
        @DisplayName("Should handle null termination date for active coverage")
        void shouldHandleNullTerminationDate() {
            // Given
            testInsurance.setTerminationDate(null);

            // When
            PatientInsuranceEntity saved = repository.save(testInsurance);
            Optional<PatientInsuranceEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTerminationDate()).isNull();
            assertThat(found.get().getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Inactive Coverage")
    class InactiveCoverageTests {

        @Test
        @DisplayName("Should mark coverage as inactive")
        void shouldMarkAsInactive() {
            // Given
            PatientInsuranceEntity saved = repository.save(testInsurance);
            saved.setActive(false);
            saved.setTerminationDate(LocalDate.now().minusDays(30));

            // When
            PatientInsuranceEntity updated = repository.save(saved);
            Optional<PatientInsuranceEntity> found = repository.findById(updated.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getActive()).isFalse();
            assertThat(found.get().getTerminationDate()).isBefore(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantTests {

        @Test
        @DisplayName("Should store insurance for different tenants")
        void shouldStoreDifferentTenants() {
            // Given
            repository.save(testInsurance);

            PatientInsuranceEntity otherTenantInsurance = PatientInsuranceEntity.builder()
                    .tenantId("other-tenant")
                    .patientId(UUID.randomUUID())
                    .coverageType("medical")
                    .payerName("United Healthcare")
                    .memberId("UHC999")
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .isPrimary(true)
                    .active(true)
                    .build();
            repository.save(otherTenantInsurance);

            // When
            List<PatientInsuranceEntity> all = repository.findAll();

            // Then
            assertThat(all).hasSizeGreaterThanOrEqualTo(2);
            assertThat(all.stream().map(PatientInsuranceEntity::getTenantId).distinct())
                    .contains(TENANT_ID, "other-tenant");
        }
    }
}
