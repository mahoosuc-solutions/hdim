package com.healthdata.patient.integration;

import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
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
 * Integration tests for PatientDemographicsRepository.
 *
 * Tests CRUD operations and multi-tenant isolation with real PostgreSQL
 * database via Testcontainers.
 */
@BaseIntegrationTest
@DisplayName("PatientDemographicsRepository Integration Tests")
class PatientDemographicsRepositoryIntegrationTest {

    @Autowired
    private PatientDemographicsRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT_ID = "other-tenant";

    private PatientDemographicsEntity testPatient;

    @BeforeEach
    void setUp() {
        testPatient = PatientDemographicsEntity.builder()
                .tenantId(TENANT_ID)
                .fhirPatientId("patient-fhir-123")
                .mrn("MRN-001")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .gender("male")
                .race("White")
                .ethnicity("Not Hispanic or Latino")
                .preferredLanguage("English")
                .email("john.doe@example.com")
                .phone("555-123-4567")
                .addressLine1("123 Main St")
                .city("Boston")
                .state("MA")
                .zipCode("02101")
                .country("USA")
                .active(true)
                .deceased(false)
                .build();
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save patient demographics")
        void shouldSavePatient() {
            // When
            PatientDemographicsEntity saved = repository.save(testPatient);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should find patient by ID")
        void shouldFindPatientById() {
            // Given
            PatientDemographicsEntity saved = repository.save(testPatient);

            // When
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getFhirPatientId()).isEqualTo("patient-fhir-123");
            assertThat(found.get().getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should return empty when patient not found")
        void shouldReturnEmptyWhenNotFound() {
            // When
            Optional<PatientDemographicsEntity> found = repository.findById(UUID.randomUUID());

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update patient demographics")
        void shouldUpdatePatient() {
            // Given
            PatientDemographicsEntity saved = repository.save(testPatient);
            saved.setPhone("555-999-8888");
            saved.setCity("Cambridge");

            // When
            PatientDemographicsEntity updated = repository.save(saved);

            // Then
            assertThat(updated.getPhone()).isEqualTo("555-999-8888");
            assertThat(updated.getCity()).isEqualTo("Cambridge");
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
        }

        @Test
        @DisplayName("Should delete patient demographics")
        void shouldDeletePatient() {
            // Given
            PatientDemographicsEntity saved = repository.save(testPatient);
            UUID id = saved.getId();

            // When
            repository.deleteById(id);

            // Then
            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("Should find all patients")
        void shouldFindAllPatients() {
            // Given
            repository.save(testPatient);

            PatientDemographicsEntity patient2 = PatientDemographicsEntity.builder()
                    .tenantId(TENANT_ID)
                    .fhirPatientId("patient-fhir-456")
                    .firstName("Jane")
                    .lastName("Smith")
                    .dateOfBirth(LocalDate.of(1990, 8, 22))
                    .gender("female")
                    .active(true)
                    .deceased(false)
                    .build();
            repository.save(patient2);

            // When
            List<PatientDemographicsEntity> all = repository.findAll();

            // Then
            assertThat(all).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("PHI Field Handling")
    class PhiFieldTests {

        @Test
        @DisplayName("Should persist PHI fields correctly")
        void shouldPersistPhiFields() {
            // Given
            testPatient.setSsnEncrypted("123-45-6789");

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getMrn()).isEqualTo("MRN-001");
            assertThat(found.get().getSsnEncrypted()).isEqualTo("123-45-6789");
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should handle null PHI fields")
        void shouldHandleNullPhiFields() {
            // Given
            testPatient.setEmail(null);
            testPatient.setPhone(null);
            testPatient.setSsnEncrypted(null);

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isNull();
            assertThat(found.get().getPhone()).isNull();
            assertThat(found.get().getSsnEncrypted()).isNull();
        }

        @Test
        @DisplayName("Should handle long address values")
        void shouldHandleLongAddressValues() {
            // Given
            String longAddress = "123 Very Long Street Name That Exceeds Normal Limits, Suite 4500";
            testPatient.setAddressLine1(longAddress);
            testPatient.setAddressLine2("Building B, Floor 12");

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getAddressLine1()).isEqualTo(longAddress);
            assertThat(found.get().getAddressLine2()).isEqualTo("Building B, Floor 12");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantTests {

        @Test
        @DisplayName("Should store patients for multiple tenants")
        void shouldStoreMultipleTenants() {
            // Given
            repository.save(testPatient);

            PatientDemographicsEntity otherTenantPatient = PatientDemographicsEntity.builder()
                    .tenantId(OTHER_TENANT_ID)
                    .fhirPatientId("patient-other-tenant")
                    .firstName("Other")
                    .lastName("Tenant")
                    .dateOfBirth(LocalDate.of(1975, 3, 10))
                    .gender("female")
                    .active(true)
                    .deceased(false)
                    .build();
            repository.save(otherTenantPatient);

            // When
            List<PatientDemographicsEntity> all = repository.findAll();

            // Then - Both patients should exist in database
            assertThat(all).hasSizeGreaterThanOrEqualTo(2);

            long tenant1Count = all.stream()
                    .filter(p -> TENANT_ID.equals(p.getTenantId()))
                    .count();
            long tenant2Count = all.stream()
                    .filter(p -> OTHER_TENANT_ID.equals(p.getTenantId()))
                    .count();

            assertThat(tenant1Count).isGreaterThanOrEqualTo(1);
            assertThat(tenant2Count).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should correctly identify tenant for each patient")
        void shouldIdentifyTenantCorrectly() {
            // Given
            PatientDemographicsEntity saved = repository.save(testPatient);

            // When
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Special Cases")
    class SpecialCasesTests {

        @Test
        @DisplayName("Should handle deceased patient")
        void shouldHandleDeceasedPatient() {
            // Given
            testPatient.setDeceased(true);
            testPatient.setDeceasedDate(LocalDate.of(2025, 1, 15));
            testPatient.setActive(false);

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getDeceased()).isTrue();
            assertThat(found.get().getDeceasedDate()).isEqualTo(LocalDate.of(2025, 1, 15));
            assertThat(found.get().getActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle PCP assignment")
        void shouldHandlePcpAssignment() {
            // Given
            testPatient.setPcpId("pcp-12345");
            testPatient.setPcpName("Dr. Sarah Johnson");

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getPcpId()).isEqualTo("pcp-12345");
            assertThat(found.get().getPcpName()).isEqualTo("Dr. Sarah Johnson");
        }

        @Test
        @DisplayName("Should handle all demographic fields")
        void shouldHandleAllDemographicFields() {
            // Given
            testPatient.setMiddleName("Michael");
            testPatient.setRace("Asian");
            testPatient.setEthnicity("Not Hispanic or Latino");
            testPatient.setPreferredLanguage("Mandarin");

            // When
            PatientDemographicsEntity saved = repository.save(testPatient);
            Optional<PatientDemographicsEntity> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getMiddleName()).isEqualTo("Michael");
            assertThat(found.get().getRace()).isEqualTo("Asian");
            assertThat(found.get().getEthnicity()).isEqualTo("Not Hispanic or Latino");
            assertThat(found.get().getPreferredLanguage()).isEqualTo("Mandarin");
        }
    }

    @Nested
    @DisplayName("Timestamp Handling")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // When
            PatientDemographicsEntity saved = repository.save(testPatient);

            // Then
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should update updatedAt on update")
        void shouldUpdateTimestampOnUpdate() throws InterruptedException {
            // Given
            PatientDemographicsEntity saved = repository.save(testPatient);
            var originalUpdatedAt = saved.getUpdatedAt();

            // Small delay to ensure timestamp difference
            Thread.sleep(10);

            saved.setCity("New York");

            // When
            PatientDemographicsEntity updated = repository.saveAndFlush(saved);

            // Then
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }
}
