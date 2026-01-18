package com.healthdata.eventsourcing.projection.careplan;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("CarePlanProjectionService Tests")
class CarePlanProjectionServiceTest {

    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";
    private final String COORDINATOR_ID = "coordinator-789";

    @Mock
    private CarePlanProjectionRepository repository;

    private CarePlanProjectionService service;

    @BeforeEach
    void setUp() {
        service = new CarePlanProjectionService(repository);
    }

    @Test
    @DisplayName("Should save care plan projection")
    void shouldSaveCarePlanProjection() {
        CarePlanProjection projection = CarePlanProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .title("Diabetes Management")
            .status("active")
            .coordinatorId(COORDINATOR_ID)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .build();

        when(repository.save(any())).thenReturn(projection);

        CarePlanProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Diabetes Management");
        verify(repository).save(projection);
    }

    @Test
    @DisplayName("Should find active care plans for patient")
    void shouldFindActiveCarePlans() {
        List<CarePlanProjection> plans = List.of(
            CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .title("Diabetes Management")
                .status("active")
                .coordinatorId(COORDINATOR_ID)
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active"))
            .thenReturn(plans);

        List<CarePlanProjection> result = service.findActiveCarePlans(TENANT_ID, PATIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("Should find care plans by coordinator")
    void shouldFindCarePlansByCoordinator() {
        List<CarePlanProjection> plans = List.of(
            CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .title("Diabetes Management")
                .coordinatorId(COORDINATOR_ID)
                .build()
        );

        when(repository.findByTenantIdAndCoordinatorId(TENANT_ID, COORDINATOR_ID))
            .thenReturn(plans);

        List<CarePlanProjection> result = service.findByTenantAndCoordinator(TENANT_ID, COORDINATOR_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCoordinatorId()).isEqualTo(COORDINATOR_ID);
    }

    @Test
    @DisplayName("Should find all care plans for patient")
    void shouldFindAllCarePlansForPatient() {
        List<CarePlanProjection> plans = List.of(
            CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .title("Diabetes Management")
                .status("active")
                .build(),
            CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .title("Hypertension Management")
                .status("completed")
                .build()
        );

        when(repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(plans);

        List<CarePlanProjection> result = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        String otherTenant = "tenant-999";

        when(repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .title("Diabetes Management")
                .build()));

        when(repository.findByTenantIdAndPatientId(otherTenant, PATIENT_ID))
            .thenReturn(List.of());

        List<CarePlanProjection> result1 = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        List<CarePlanProjection> result2 = service.findByTenantAndPatient(otherTenant, PATIENT_ID);

        assertThat(result1).hasSize(1);
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("Should count care plans for patient")
    void shouldCountCarePlans() {
        when(repository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(2L);

        long count = service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should find care plan by title")
    void shouldFindCarePlanByTitle() {
        CarePlanProjection plan = CarePlanProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .title("Diabetes Management")
            .status("active")
            .build();

        when(repository.findByTenantIdAndPatientIdAndTitle(TENANT_ID, PATIENT_ID, "Diabetes Management"))
            .thenReturn(Optional.of(plan));

        Optional<CarePlanProjection> result = service.findByTenantPatientAndTitle(
            TENANT_ID, PATIENT_ID, "Diabetes Management");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Diabetes Management");
    }

    @Test
    @DisplayName("Should find care plans by status")
    void shouldFindCarePlansByStatus() {
        List<CarePlanProjection> plans = List.of(
            CarePlanProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .status("completed")
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "completed"))
            .thenReturn(plans);

        List<CarePlanProjection> result = service.findByTenantPatientAndStatus(
            TENANT_ID, PATIENT_ID, "completed");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("completed");
    }

    @Test
    @DisplayName("Should handle care plan with goals")
    void shouldHandleCarePlanWithGoals() {
        CarePlanProjection projection = CarePlanProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .title("Diabetes Management")
            .goalCount(5)
            .build();

        when(repository.save(any())).thenReturn(projection);

        CarePlanProjection result = service.saveProjection(projection);

        assertThat(result.getGoalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should count active care plans")
    void shouldCountActiveCarePlans() {
        when(repository.countByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active"))
            .thenReturn(1L);

        long count = service.countActiveCarePlans(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return empty when care plan not found")
    void shouldReturnEmptyWhenNotFound() {
        when(repository.findByTenantIdAndPatientIdAndTitle(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        Optional<CarePlanProjection> result = service.findByTenantPatientAndTitle(
            TENANT_ID, PATIENT_ID, "INVALID");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle care plan date ranges")
    void shouldHandleCarePlanDateRanges() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        CarePlanProjection projection = CarePlanProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .title("Diabetes Management")
            .startDate(startDate)
            .endDate(endDate)
            .build();

        when(repository.save(any())).thenReturn(projection);

        CarePlanProjection result = service.saveProjection(projection);

        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("Should verify repository called for all queries")
    void shouldVerifyRepositoryCalls() {
        service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        service.findActiveCarePlans(TENANT_ID, PATIENT_ID);
        service.findByTenantAndCoordinator(TENANT_ID, COORDINATOR_ID);
        service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        verify(repository).findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        verify(repository).findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active");
        verify(repository).findByTenantIdAndCoordinatorId(TENANT_ID, COORDINATOR_ID);
        verify(repository).countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
    }
}
