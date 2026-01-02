package com.healthdata.patient.integration;

import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.entity.PatientRiskScoreEntity;
import com.healthdata.patient.repository.PatientRiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PatientRiskScoreRepository.
 *
 * Tests CRUD operations, risk stratification data storage,
 * and multi-tenant isolation with real PostgreSQL via Testcontainers.
 */
@BaseIntegrationTest
@DisplayName("PatientRiskScoreRepository Integration Tests")
class PatientRiskScoreRepositoryIntegrationTest {

    @Autowired
    private PatientRiskScoreRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private PatientRiskScoreEntity testRiskScore;

    @BeforeEach
    void setUp() {
        testRiskScore = PatientRiskScoreEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .scoreType("HCC")
                .scoreValue(new BigDecimal("1.245"))
                .riskCategory("high")
                .calculationDate(Instant.now())
                .validUntil(Instant.now().plus(365, ChronoUnit.DAYS))
                .factors("[\"diabetes\", \"hypertension\", \"ckd\"]")
                .comorbidities("[\"E11.9\", \"I10\", \"N18.3\"]")
                .modelVersion("HCC-V28-2024")
                .build();
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save risk score")
        void shouldSaveRiskScore() {
            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getScoreType()).isEqualTo("HCC");
            assertThat(saved.getScoreValue()).isEqualByComparingTo(new BigDecimal("1.245"));
        }

        @Test
        @DisplayName("Should find risk score by ID")
        void shouldFindRiskScoreById() {
            // Given
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // When
            Optional<PatientRiskScoreEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getRiskCategory()).isEqualTo("high");
            assertThat(found.get().getModelVersion()).isEqualTo("HCC-V28-2024");
        }

        @Test
        @DisplayName("Should update risk score")
        void shouldUpdateRiskScore() {
            // Given
            PatientRiskScoreEntity saved = repository.save(testRiskScore);
            saved.setScoreValue(new BigDecimal("1.567"));
            saved.setRiskCategory("very_high");

            // When
            PatientRiskScoreEntity updated = repository.save(saved);

            // Then
            assertThat(updated.getScoreValue()).isEqualByComparingTo(new BigDecimal("1.567"));
            assertThat(updated.getRiskCategory()).isEqualTo("very_high");
        }

        @Test
        @DisplayName("Should delete risk score")
        void shouldDeleteRiskScore() {
            // Given
            PatientRiskScoreEntity saved = repository.save(testRiskScore);
            UUID id = saved.getId();

            // When
            repository.deleteById(id);

            // Then
            assertThat(repository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Risk Score Types")
    class RiskScoreTypeTests {

        @Test
        @DisplayName("Should store HCC risk score")
        void shouldStoreHccRiskScore() {
            // Given
            testRiskScore.setScoreType("HCC");
            testRiskScore.setScoreValue(new BigDecimal("1.456"));

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getScoreType()).isEqualTo("HCC");
            assertThat(saved.getScoreValue()).isEqualByComparingTo(new BigDecimal("1.456"));
        }

        @Test
        @DisplayName("Should store CDPS risk score")
        void shouldStoreCdpsRiskScore() {
            // Given
            PatientRiskScoreEntity cdpsScore = PatientRiskScoreEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .scoreType("CDPS")
                    .scoreValue(new BigDecimal("2.134"))
                    .riskCategory("medium")
                    .modelVersion("CDPS-2024")
                    .build();

            // When
            PatientRiskScoreEntity saved = repository.save(cdpsScore);

            // Then
            assertThat(saved.getScoreType()).isEqualTo("CDPS");
            assertThat(saved.getScoreValue()).isEqualByComparingTo(new BigDecimal("2.134"));
        }

        @Test
        @DisplayName("Should store ACG risk score")
        void shouldStoreAcgRiskScore() {
            // Given
            PatientRiskScoreEntity acgScore = PatientRiskScoreEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .scoreType("ACG")
                    .scoreValue(new BigDecimal("0.987"))
                    .riskCategory("low")
                    .modelVersion("ACG-v12")
                    .build();

            // When
            PatientRiskScoreEntity saved = repository.save(acgScore);

            // Then
            assertThat(saved.getScoreType()).isEqualTo("ACG");
            assertThat(saved.getRiskCategory()).isEqualTo("low");
        }
    }

    @Nested
    @DisplayName("Risk Categories")
    class RiskCategoryTests {

        @Test
        @DisplayName("Should store low risk category")
        void shouldStoreLowRisk() {
            // Given
            testRiskScore.setRiskCategory("low");
            testRiskScore.setScoreValue(new BigDecimal("0.5"));

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getRiskCategory()).isEqualTo("low");
        }

        @Test
        @DisplayName("Should store medium risk category")
        void shouldStoreMediumRisk() {
            // Given
            testRiskScore.setRiskCategory("medium");
            testRiskScore.setScoreValue(new BigDecimal("1.0"));

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getRiskCategory()).isEqualTo("medium");
        }

        @Test
        @DisplayName("Should store high risk category")
        void shouldStoreHighRisk() {
            // Given
            testRiskScore.setRiskCategory("high");
            testRiskScore.setScoreValue(new BigDecimal("2.5"));

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getRiskCategory()).isEqualTo("high");
        }

        @Test
        @DisplayName("Should store very high risk category")
        void shouldStoreVeryHighRisk() {
            // Given
            testRiskScore.setRiskCategory("very_high");
            testRiskScore.setScoreValue(new BigDecimal("4.0"));

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getRiskCategory()).isEqualTo("very_high");
        }
    }

    @Nested
    @DisplayName("Risk Factors and Comorbidities")
    class FactorsAndComorbiditiesTests {

        @Test
        @DisplayName("Should store risk factors as JSON")
        void shouldStoreRiskFactors() {
            // Given
            String factors = "[\"diabetes\", \"hypertension\", \"obesity\", \"smoking\"]";
            testRiskScore.setFactors(factors);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);
            Optional<PatientRiskScoreEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getFactors()).isEqualTo(factors);
        }

        @Test
        @DisplayName("Should store comorbidities as JSON")
        void shouldStoreComorbidities() {
            // Given
            String comorbidities = "[\"E11.9\", \"I10\", \"E66.9\", \"F17.200\"]";
            testRiskScore.setComorbidities(comorbidities);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);
            Optional<PatientRiskScoreEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getComorbidities()).isEqualTo(comorbidities);
        }

        @Test
        @DisplayName("Should handle null factors")
        void shouldHandleNullFactors() {
            // Given
            testRiskScore.setFactors(null);
            testRiskScore.setComorbidities(null);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getFactors()).isNull();
            assertThat(saved.getComorbidities()).isNull();
        }
    }

    @Nested
    @DisplayName("Validity Period")
    class ValidityPeriodTests {

        @Test
        @DisplayName("Should store calculation date")
        void shouldStoreCalculationDate() {
            // Given
            Instant calculationDate = Instant.now().minus(1, ChronoUnit.HOURS);
            testRiskScore.setCalculationDate(calculationDate);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getCalculationDate()).isEqualTo(calculationDate);
        }

        @Test
        @DisplayName("Should store valid until date")
        void shouldStoreValidUntilDate() {
            // Given
            Instant validUntil = Instant.now().plus(90, ChronoUnit.DAYS);
            testRiskScore.setValidUntil(validUntil);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);
            Optional<PatientRiskScoreEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getValidUntil()).isEqualTo(validUntil);
        }

        @Test
        @DisplayName("Should handle null valid until for ongoing scores")
        void shouldHandleNullValidUntil() {
            // Given
            testRiskScore.setValidUntil(null);

            // When
            PatientRiskScoreEntity saved = repository.save(testRiskScore);

            // Then
            assertThat(saved.getValidUntil()).isNull();
        }
    }

    @Nested
    @DisplayName("Multiple Scores Per Patient")
    class MultipleScoresTests {

        @Test
        @DisplayName("Should store multiple risk scores for same patient")
        void shouldStoreMultipleScores() {
            // Given
            repository.save(testRiskScore); // HCC score

            PatientRiskScoreEntity cdpsScore = PatientRiskScoreEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .scoreType("CDPS")
                    .scoreValue(new BigDecimal("1.8"))
                    .riskCategory("medium")
                    .modelVersion("CDPS-2024")
                    .build();
            repository.save(cdpsScore);

            PatientRiskScoreEntity readmissionScore = PatientRiskScoreEntity.builder()
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .scoreType("LACE")
                    .scoreValue(new BigDecimal("12"))
                    .riskCategory("high")
                    .modelVersion("LACE-2024")
                    .build();
            repository.save(readmissionScore);

            // When
            List<PatientRiskScoreEntity> all = repository.findAll();

            // Then
            List<PatientRiskScoreEntity> patientScores = all.stream()
                    .filter(s -> PATIENT_ID.equals(s.getPatientId()))
                    .toList();

            assertThat(patientScores).hasSizeGreaterThanOrEqualTo(3);
            assertThat(patientScores.stream().map(PatientRiskScoreEntity::getScoreType))
                    .contains("HCC", "CDPS", "LACE");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantTests {

        @Test
        @DisplayName("Should store risk scores for different tenants")
        void shouldStoreDifferentTenants() {
            // Given
            repository.save(testRiskScore);

            PatientRiskScoreEntity otherTenantScore = PatientRiskScoreEntity.builder()
                    .tenantId("other-tenant")
                    .patientId(UUID.randomUUID())
                    .scoreType("HCC")
                    .scoreValue(new BigDecimal("0.8"))
                    .riskCategory("low")
                    .build();
            repository.save(otherTenantScore);

            // When
            List<PatientRiskScoreEntity> all = repository.findAll();

            // Then
            assertThat(all).hasSizeGreaterThanOrEqualTo(2);
            assertThat(all.stream().map(PatientRiskScoreEntity::getTenantId).distinct())
                    .contains(TENANT_ID, "other-tenant");
        }
    }
}
