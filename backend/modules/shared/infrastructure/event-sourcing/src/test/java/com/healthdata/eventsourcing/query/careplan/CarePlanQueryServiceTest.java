package com.healthdata.eventsourcing.query.careplan;

import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjectionRepository;
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
class CarePlanQueryServiceTest {

    @Mock
    private CarePlanProjectionRepository carePlanRepository;

    @InjectMocks
    private CarePlanQueryService queryService;

    private CarePlanProjection testCarePlan;

    @BeforeEach
    void setUp() {
        testCarePlan = CarePlanProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .title("Diabetes Management Plan")
            .status("active")
            .coordinatorId("coordinator-456")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .goalCount(3)
            .build();
    }

    @Test
    @DisplayName("Should find care plans by patient and tenant")
    void shouldFindCarePlansByPatientAndTenant() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1).contains(testCarePlan);
    }

    @Test
    @DisplayName("Should find care plans by coordinator")
    void shouldFindCarePlansByCoordinator() {
        when(carePlanRepository.findByTenantIdAndCoordinatorId("tenant-123", "coordinator-456"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByTenantAndCoordinator("tenant-123", "coordinator-456");
        assertThat(results).hasSize(1).contains(testCarePlan);
    }

    @Test
    @DisplayName("Should find active care plans by patient and tenant")
    void shouldFindActiveCarePlans() {
        when(carePlanRepository.findByTenantIdAndPatientIdAndStatus("tenant-123", "patient-123", "active"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findActiveCarePlansByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should find care plans by status")
    void shouldFindCarePlansByStatus() {
        when(carePlanRepository.findByTenantIdAndStatus("tenant-123", "active"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findCarePlansByStatusAndTenant("tenant-123", "active");
        assertThat(results).hasSize(1).allMatch(cp -> cp.getStatus().equals("active"));
    }

    @Test
    @DisplayName("Should find care plan by patient, tenant, and title")
    void shouldFindCarePlanByTitle() {
        when(carePlanRepository.findByTenantIdAndPatientIdAndTitle("tenant-123", "patient-123", "Diabetes Management Plan"))
            .thenReturn(Optional.of(testCarePlan));
        Optional<CarePlanProjection> result = queryService.findByPatientAndTenantAndTitle("patient-123", "tenant-123", "Diabetes Management Plan");
        assertThat(result).isPresent().contains(testCarePlan);
    }

    @Test
    @DisplayName("Should find all care plans by tenant")
    void shouldFindAllByTenant() {
        when(carePlanRepository.findByTenantId("tenant-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findAllByTenant("tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should preserve care plan title")
    void shouldPreserveCareplanTitle() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getTitle()).isEqualTo("Diabetes Management Plan");
    }

    @Test
    @DisplayName("Should preserve care plan status")
    void shouldPreserveCareplanStatus() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("Should preserve coordinator ID")
    void shouldPreserveCoordinatorId() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getCoordinatorId()).isEqualTo("coordinator-456");
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        CarePlanProjection tenant2Plan = CarePlanProjection.builder()
            .patientId("patient-789")
            .tenantId("tenant-456")
            .title("Hypertension Plan")
            .status("active")
            .coordinatorId("coordinator-999")
            .build();
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-456", "patient-789"))
            .thenReturn(List.of(tenant2Plan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-789", "tenant-456");
        assertThat(results.get(0).getTenantId()).isEqualTo("tenant-456");
    }

    @Test
    @DisplayName("Should return empty when no care plans found")
    void shouldReturnEmptyWhenNoCareplanFound() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "unknown"))
            .thenReturn(List.of());
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("unknown", "tenant-123");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle null end date")
    void shouldHandleNullEndDate() {
        CarePlanProjection noEndDatePlan = CarePlanProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .title("Ongoing Plan")
            .status("active")
            .coordinatorId("coordinator-456")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(null)
            .build();
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(noEndDatePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should handle repository exceptions")
    void shouldHandleRepositoryExceptions() {
        when(carePlanRepository.findByTenantIdAndPatientId(anyString(), anyString()))
            .thenThrow(new RuntimeException("Database error"));
        assertThatThrownBy(() -> queryService.findByPatientAndTenant("patient-123", "tenant-123"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should verify repository delegation")
    void shouldVerifyRepositoryDelegation() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        queryService.findByPatientAndTenant("patient-123", "tenant-123");
        verify(carePlanRepository).findByTenantIdAndPatientId("tenant-123", "patient-123");
    }

    @Test
    @DisplayName("Should handle multiple care plans")
    void shouldHandleMultipleCareplans() {
        CarePlanProjection plan2 = CarePlanProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .title("Hypertension Plan")
            .status("active")
            .coordinatorId("coordinator-456")
            .build();
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan, plan2));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should preserve goal count")
    void shouldPreserveGoalCount() {
        when(carePlanRepository.findByTenantIdAndPatientId("tenant-123", "patient-123"))
            .thenReturn(List.of(testCarePlan));
        List<CarePlanProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getGoalCount()).isEqualTo(3);
    }
}
