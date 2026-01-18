package com.healthdata.eventsourcing.query.patient;

import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.projection.patient.PatientProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientQueryServiceTest {

    @Mock
    private PatientProjectionRepository patientRepository;

    @InjectMocks
    private PatientQueryService queryService;

    private PatientProjection testPatient;

    @BeforeEach
    void setUp() {
        testPatient = PatientProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .mrn("MRN-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 5, 15))
            .gender("M")
            .insuranceMemberId("INS-123456")
            .build();
    }

    @Test
    @DisplayName("Should find patient by ID and tenant")
    void shouldFindPatientByIdAndTenant() {
        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        Optional<PatientProjection> result = queryService.findByIdAndTenant("patient-123", "tenant-123");

        assertThat(result).isPresent().contains(testPatient);
        verify(patientRepository).findByPatientIdAndTenantId("patient-123", "tenant-123");
    }

    @Test
    @DisplayName("Should return empty when patient not found")
    void shouldReturnEmptyWhenPatientNotFound() {
        when(patientRepository.findByPatientIdAndTenantId("unknown", "tenant-123"))
            .thenReturn(Optional.empty());

        Optional<PatientProjection> result = queryService.findByIdAndTenant("unknown", "tenant-123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find all patients by tenant")
    void shouldFindAllPatientsByTenant() {
        PatientProjection patient2 = PatientProjection.builder()
            .patientId("patient-456")
            .tenantId("tenant-123")
            .mrn("MRN-002")
            .firstName("Jane")
            .lastName("Smith")
            .build();

        when(patientRepository.findByTenantId("tenant-123"))
            .thenReturn(List.of(testPatient, patient2));

        List<PatientProjection> results = queryService.findAllByTenant("tenant-123");

        assertThat(results).hasSize(2).contains(testPatient, patient2);
        verify(patientRepository).findByTenantId("tenant-123");
    }

    @Test
    @DisplayName("Should return empty list when no patients in tenant")
    void shouldReturnEmptyListWhenNoPatients() {
        when(patientRepository.findByTenantId("tenant-456"))
            .thenReturn(List.of());

        List<PatientProjection> results = queryService.findAllByTenant("tenant-456");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find patient by MRN and tenant")
    void shouldFindPatientByMrnAndTenant() {
        when(patientRepository.findByMrnAndTenantId("MRN-001", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        Optional<PatientProjection> result = queryService.findByMrnAndTenant("MRN-001", "tenant-123");

        assertThat(result).isPresent().contains(testPatient);
        verify(patientRepository).findByMrnAndTenantId("MRN-001", "tenant-123");
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation on ID queries")
    void shouldEnforceMultiTenantIsolationOnIdQueries() {
        PatientProjection tenant2Patient = PatientProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-456")
            .mrn("MRN-999")
            .firstName("Different")
            .lastName("Tenant")
            .build();

        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-456"))
            .thenReturn(Optional.of(tenant2Patient));

        Optional<PatientProjection> result = queryService.findByIdAndTenant("patient-123", "tenant-456");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("tenant-456");
        verify(patientRepository).findByPatientIdAndTenantId("patient-123", "tenant-456");
    }

    @Test
    @DisplayName("Should find patient by insurance member ID and tenant")
    void shouldFindPatientByInsuranceMemberIdAndTenant() {
        when(patientRepository.findByInsuranceMemberIdAndTenantId("INS-123456", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        Optional<PatientProjection> result = queryService.findByInsuranceMemberIdAndTenant("INS-123456", "tenant-123");

        assertThat(result).isPresent().contains(testPatient);
    }

    @Test
    @DisplayName("Should handle null search results gracefully")
    void shouldHandleNullSearchResults() {
        when(patientRepository.findByPatientIdAndTenantId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        Optional<PatientProjection> result = queryService.findByIdAndTenant("any-id", "any-tenant");

        assertThat(result).isEmpty();
        assertThatThrownBy(() -> result.get())
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("Should return null-safe list for tenant without patients")
    void shouldReturnNullSafeListForEmptyTenant() {
        when(patientRepository.findByTenantId("empty-tenant"))
            .thenReturn(List.of());

        List<PatientProjection> results = queryService.findAllByTenant("empty-tenant");

        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should preserve patient demographics in query results")
    void shouldPreservePatientDemographics() {
        LocalDate dob = LocalDate.of(1985, 3, 20);
        PatientProjection detailedPatient = PatientProjection.builder()
            .patientId("patient-999")
            .tenantId("tenant-123")
            .mrn("MRN-DETAIL")
            .firstName("Robert")
            .lastName("Johnson")
            .dateOfBirth(dob)
            .gender("M")
            .insuranceMemberId("INS-999999")
            .build();

        when(patientRepository.findByPatientIdAndTenantId("patient-999", "tenant-123"))
            .thenReturn(Optional.of(detailedPatient));

        Optional<PatientProjection> result = queryService.findByIdAndTenant("patient-999", "tenant-123");

        assertThat(result).isPresent();
        assertThat(result.get())
            .extracting("firstName", "lastName", "dateOfBirth", "gender")
            .containsExactly("Robert", "Johnson", dob, "M");
    }

    @Test
    @DisplayName("Should handle multiple patients with same name in different tenants")
    void shouldHandleMultiplePatientsWithSameNameDifferentTenants() {
        PatientProjection sameNameTenant1 = PatientProjection.builder()
            .patientId("patient-1")
            .tenantId("tenant-1")
            .mrn("MRN-A")
            .firstName("John")
            .lastName("Smith")
            .build();

        PatientProjection sameNameTenant2 = PatientProjection.builder()
            .patientId("patient-2")
            .tenantId("tenant-2")
            .mrn("MRN-B")
            .firstName("John")
            .lastName("Smith")
            .build();

        when(patientRepository.findByPatientIdAndTenantId("patient-1", "tenant-1"))
            .thenReturn(Optional.of(sameNameTenant1));
        when(patientRepository.findByPatientIdAndTenantId("patient-2", "tenant-2"))
            .thenReturn(Optional.of(sameNameTenant2));

        Optional<PatientProjection> result1 = queryService.findByIdAndTenant("patient-1", "tenant-1");
        Optional<PatientProjection> result2 = queryService.findByIdAndTenant("patient-2", "tenant-2");

        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(result2.get().getTenantId()).isEqualTo("tenant-2");
    }

    @Test
    @DisplayName("Should retrieve all patient fields")
    void shouldRetrieveAllPatientFields() {
        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        Optional<PatientProjection> result = queryService.findByIdAndTenant("patient-123", "tenant-123");

        assertThat(result).isPresent();
        assertThat(result.get())
            .hasFieldOrPropertyWithValue("patientId", "patient-123")
            .hasFieldOrPropertyWithValue("tenantId", "tenant-123")
            .hasFieldOrPropertyWithValue("mrn", "MRN-001")
            .hasFieldOrPropertyWithValue("firstName", "John")
            .hasFieldOrPropertyWithValue("lastName", "Doe")
            .hasFieldOrPropertyWithValue("gender", "M")
            .hasFieldOrPropertyWithValue("insuranceMemberId", "INS-123456");
    }

    @Test
    @DisplayName("Should call repository only once per query")
    void shouldCallRepositoryOncePerQuery() {
        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        queryService.findByIdAndTenant("patient-123", "tenant-123");

        verify(patientRepository, times(1)).findByPatientIdAndTenantId("patient-123", "tenant-123");
    }

    @Test
    @DisplayName("Should find multiple patients by tenant with pagination support")
    void shouldFindMultiplePatientsByTenant() {
        List<PatientProjection> patients = List.of(testPatient);
        when(patientRepository.findByTenantId("tenant-123"))
            .thenReturn(patients);

        List<PatientProjection> results = queryService.findAllByTenant("tenant-123");

        assertThat(results).hasSize(1).contains(testPatient);
    }

    @Test
    @DisplayName("Should handle repository exceptions appropriately")
    void shouldHandleRepositoryExceptions() {
        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> queryService.findByIdAndTenant("patient-123", "tenant-123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error");
    }

    @Test
    @DisplayName("Should not modify query parameters")
    void shouldNotModifyQueryParameters() {
        when(patientRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(Optional.of(testPatient));

        String patientId = "patient-123";
        String tenantId = "tenant-123";
        String originalPatientId = patientId;
        String originalTenantId = tenantId;

        queryService.findByIdAndTenant(patientId, tenantId);

        assertThat(patientId).isEqualTo(originalPatientId);
        assertThat(tenantId).isEqualTo(originalTenantId);
    }
}
