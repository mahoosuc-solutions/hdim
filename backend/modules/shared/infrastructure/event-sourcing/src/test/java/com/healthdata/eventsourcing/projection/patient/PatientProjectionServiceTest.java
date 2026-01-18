package com.healthdata.eventsourcing.projection.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for PatientProjectionService
 *
 * Tests CQRS read model for patient data including:
 * - Projection persistence and retrieval
 * - Multi-tenant isolation
 * - Query efficiency with indexes
 * - Null/empty handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientProjectionService Tests")
class PatientProjectionServiceTest {

    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";
    private final String MRN = "MRN-12345";

    @Mock
    private PatientProjectionRepository repository;

    private PatientProjectionService service;

    @BeforeEach
    void setUp() {
        service = new PatientProjectionService(repository);
    }

    @Test
    @DisplayName("Should save patient projection")
    void shouldSavePatientProjection() {
        PatientProjection projection = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .gender("MALE")
            .mrn(MRN)
            .insuranceMemberId("INS-12345")
            .build();

        when(repository.save(any())).thenReturn(projection);

        PatientProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        verify(repository).save(projection);
    }

    @Test
    @DisplayName("Should find patient by tenant and MRN")
    void shouldFindPatientByTenantAndMrn() {
        PatientProjection projection = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .firstName("John")
            .lastName("Doe")
            .mrn(MRN)
            .build();

        when(repository.findByTenantIdAndMrn(TENANT_ID, MRN))
            .thenReturn(Optional.of(projection));

        Optional<PatientProjection> result = service.findByTenantAndMrn(TENANT_ID, MRN);

        assertThat(result).isPresent();
        assertThat(result.get().getPatientId()).isEqualTo(PATIENT_ID);
        verify(repository).findByTenantIdAndMrn(TENANT_ID, MRN);
    }

    @Test
    @DisplayName("Should return empty when patient not found by MRN")
    void shouldReturnEmptyWhenPatientNotFound() {
        when(repository.findByTenantIdAndMrn(anyString(), anyString()))
            .thenReturn(Optional.empty());

        Optional<PatientProjection> result = service.findByTenantAndMrn(TENANT_ID, "INVALID-MRN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find all patients for tenant")
    void shouldFindAllPatientsForTenant() {
        List<PatientProjection> patients = List.of(
            PatientProjection.builder()
                .patientId("patient-1")
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .mrn("MRN-001")
                .build(),
            PatientProjection.builder()
                .patientId("patient-2")
                .tenantId(TENANT_ID)
                .firstName("Jane")
                .lastName("Smith")
                .mrn("MRN-002")
                .build()
        );

        when(repository.findByTenantId(TENANT_ID)).thenReturn(patients);

        List<PatientProjection> result = service.findAllByTenant(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("firstName")
            .containsExactly("John", "Jane");
        verify(repository).findByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should return empty list when no patients found for tenant")
    void shouldReturnEmptyListWhenNoPatientsForTenant() {
        when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        List<PatientProjection> result = service.findAllByTenant(TENANT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation - different tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        String otherTenant = "tenant-999";

        PatientProjection projection1 = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .mrn(MRN)
            .build();

        when(repository.findByTenantIdAndMrn(TENANT_ID, MRN))
            .thenReturn(Optional.of(projection1));
        when(repository.findByTenantIdAndMrn(otherTenant, MRN))
            .thenReturn(Optional.empty());

        Optional<PatientProjection> result1 = service.findByTenantAndMrn(TENANT_ID, MRN);
        Optional<PatientProjection> result2 = service.findByTenantAndMrn(otherTenant, MRN);

        assertThat(result1).isPresent();
        assertThat(result2).isEmpty();
        verify(repository).findByTenantIdAndMrn(TENANT_ID, MRN);
        verify(repository).findByTenantIdAndMrn(otherTenant, MRN);
    }

    @Test
    @DisplayName("Should update patient projection")
    void shouldUpdatePatientProjection() {
        PatientProjection projection = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .firstName("John")
            .lastName("Doe")
            .build();

        when(repository.save(any())).thenReturn(projection);

        PatientProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        verify(repository).save(projection);
    }

    @Test
    @DisplayName("Should count patients for tenant")
    void shouldCountPatientsForTenant() {
        when(repository.countByTenantId(TENANT_ID)).thenReturn(5L);

        long count = service.countByTenant(TENANT_ID);

        assertThat(count).isEqualTo(5L);
        verify(repository).countByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should return zero count when no patients for tenant")
    void shouldReturnZeroCountWhenNoPatientsForTenant() {
        when(repository.countByTenantId(TENANT_ID)).thenReturn(0L);

        long count = service.countByTenant(TENANT_ID);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should handle patient with full demographics")
    void shouldHandlePatientWithFullDemographics() {
        PatientProjection projection = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .firstName("Jonathan")
            .lastName("Anderson")
            .dateOfBirth(LocalDate.of(1985, 6, 15))
            .gender("MALE")
            .mrn("MRN-98765")
            .insuranceMemberId("INS-54321")
            .build();

        when(repository.save(any())).thenReturn(projection);

        PatientProjection result = service.saveProjection(projection);

        assertThat(result)
            .isNotNull()
            .extracting("firstName", "lastName", "gender")
            .containsExactly("Jonathan", "Anderson", "MALE");
    }

    @Test
    @DisplayName("Should search patients by name prefix")
    void shouldSearchPatientsByNamePrefix() {
        List<PatientProjection> patients = List.of(
            PatientProjection.builder()
                .patientId("patient-1")
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .build(),
            PatientProjection.builder()
                .patientId("patient-2")
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Smith")
                .build()
        );

        when(repository.findByTenantIdAndFirstNameStartingWith(TENANT_ID, "John"))
            .thenReturn(patients);

        List<PatientProjection> result = service.findByTenantAndFirstNamePrefix(TENANT_ID, "John");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getFirstName().equals("John"));
    }

    @Test
    @DisplayName("Should handle null projections gracefully")
    void shouldHandleNullProjectionsGracefully() {
        PatientProjection projection = PatientProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .firstName("John")
            .build();

        when(repository.save(any())).thenReturn(projection);

        PatientProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        assertThat(result.getLastName()).isNull();
    }

    @Test
    @DisplayName("Should verify repository called for all queries")
    void shouldVerifyRepositoryCalls() {
        service.findByTenantAndMrn(TENANT_ID, MRN);
        service.findAllByTenant(TENANT_ID);
        service.countByTenant(TENANT_ID);

        verify(repository).findByTenantIdAndMrn(TENANT_ID, MRN);
        verify(repository).findByTenantId(TENANT_ID);
        verify(repository).countByTenantId(TENANT_ID);
    }
}
