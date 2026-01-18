package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.query.careplan.CarePlanQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CarePlanController (Phase 1.8)
 * Tests REST endpoints for care plans with coordinator and status filtering
 */
@WebMvcTest(CarePlanController.class)
class CarePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarePlanQueryService carePlanQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    // ============ getCarePlansByPatient Tests ============

    @Test
    void shouldReturnCarePlans_WhenCarePlansExistForPatient() throws Exception {
        // Given
        List<CarePlanProjection> carePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build(),
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Hypertension Control")
                .status("active")
                .coordinatorId("coordinator-002")
                .startDate(LocalDate.of(2024, 3, 15))
                .endDate(LocalDate.of(2025, 3, 15))
                .goalCount(3)
                .build()
        );

        when(carePlanQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(carePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"))
            .andExpect(jsonPath("$[0].title").value("Diabetes Management Plan"))
            .andExpect(jsonPath("$[0].status").value("active"))
            .andExpect(jsonPath("$[0].goalCount").value(5))
            .andExpect(jsonPath("$[1].title").value("Hypertension Control"))
            .andExpect(jsonPath("$[1].goalCount").value(3));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoCarePlansForPatient() throws Exception {
        // Given
        when(carePlanQueryService.findByPatientAndTenant("patient-456", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-456")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-456", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissingForPatientCarePlans() throws Exception {
        // When & Then - No X-Tenant-ID header
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(carePlanQueryService, never())
            .findByPatientAndTenant(anyString(), anyString());
    }

    // ============ getCarePlansByCoordinator Tests ============

    @Test
    void shouldReturnCarePlans_WhenCarePlansAssignedToCoordinator() throws Exception {
        // Given
        List<CarePlanProjection> carePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build(),
            CarePlanProjection.builder()
                .patientId("patient-456")
                .title("Hypertension Control")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 3, 15))
                .endDate(LocalDate.of(2025, 3, 15))
                .goalCount(3)
                .build()
        );

        when(carePlanQueryService.findByTenantAndCoordinator(TENANT_ID, "coordinator-001"))
            .thenReturn(carePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/coordinator/{coordinatorId}", "coordinator-001")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].coordinatorId")
                .value(containsInAnyOrder("coordinator-001", "coordinator-001")))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"))
            .andExpect(jsonPath("$[1].patientId").value("patient-456"));

        verify(carePlanQueryService, times(1))
            .findByTenantAndCoordinator(TENANT_ID, "coordinator-001");
    }

    @Test
    void shouldReturnEmptyList_WhenCoordinatorHasNoCarePlans() throws Exception {
        // Given
        when(carePlanQueryService.findByTenantAndCoordinator(TENANT_ID, "coordinator-999"))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/coordinator/{coordinatorId}", "coordinator-999")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(carePlanQueryService, times(1))
            .findByTenantAndCoordinator(TENANT_ID, "coordinator-999");
    }

    // ============ getActiveCarePlans Tests ============

    @Test
    void shouldReturnActiveCarePlans_WhenActiveCarePlansExist() throws Exception {
        // Given
        List<CarePlanProjection> activePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build()
        );

        when(carePlanQueryService.findActiveCarePlansByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(activePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}/active", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("active"))
            .andExpect(jsonPath("$[0].title").value("Diabetes Management Plan"));

        verify(carePlanQueryService, times(1))
            .findActiveCarePlansByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoActiveCarePlans() throws Exception {
        // Given
        when(carePlanQueryService.findActiveCarePlansByPatientAndTenant("patient-789", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}/active", "patient-789")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(carePlanQueryService, times(1))
            .findActiveCarePlansByPatientAndTenant("patient-789", TENANT_ID);
    }

    // ============ getCarePlansByStatus Tests ============

    @Test
    void shouldReturnCarePlans_WhenStatusParameterProvided() throws Exception {
        // Given
        List<CarePlanProjection> carePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build(),
            CarePlanProjection.builder()
                .patientId("patient-456")
                .title("Hypertension Control")
                .status("active")
                .coordinatorId("coordinator-002")
                .startDate(LocalDate.of(2024, 3, 15))
                .endDate(LocalDate.of(2025, 3, 15))
                .goalCount(3)
                .build()
        );

        when(carePlanQueryService.findCarePlansByStatusAndTenant(TENANT_ID, "active"))
            .thenReturn(carePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans")
                .param("status", "active")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(carePlanQueryService, times(1))
            .findCarePlansByStatusAndTenant(TENANT_ID, "active");
    }

    @Test
    void shouldReturnAllCarePlans_WhenStatusParameterNotProvided() throws Exception {
        // Given
        List<CarePlanProjection> carePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build(),
            CarePlanProjection.builder()
                .patientId("patient-456")
                .title("Old Hypertension Plan")
                .status("completed")
                .coordinatorId("coordinator-002")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .goalCount(2)
                .build()
        );

        when(carePlanQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(carePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(carePlanQueryService, times(1))
            .findAllByTenant(TENANT_ID);
    }

    @Test
    void shouldReturnAllCarePlans_WhenStatusParameterEmpty() throws Exception {
        // Given
        List<CarePlanProjection> carePlans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build()
        );

        when(carePlanQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(carePlans);

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans")
                .param("status", "")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(carePlanQueryService, times(1))
            .findAllByTenant(TENANT_ID);
    }

    // ============ getCarePlanByTitle Tests ============

    @Test
    void shouldReturnCarePlan_WhenTitleFound() throws Exception {
        // Given
        CarePlanProjection carePlan = CarePlanProjection.builder()
            .patientId("patient-123")
            .title("Diabetes Management Plan")
            .status("active")
            .coordinatorId("coordinator-001")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .goalCount(5)
            .build();

        when(carePlanQueryService.findByPatientAndTenantAndTitle(
            "patient-123", TENANT_ID, "Diabetes Management Plan"))
            .thenReturn(Optional.of(carePlan));

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}/title/{title}",
                "patient-123", "Diabetes Management Plan")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.title").value("Diabetes Management Plan"))
            .andExpect(jsonPath("$.status").value("active"));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenantAndTitle("patient-123", TENANT_ID, "Diabetes Management Plan");
    }

    @Test
    void shouldReturn404_WhenTitleNotFound() throws Exception {
        // Given
        when(carePlanQueryService.findByPatientAndTenantAndTitle(
            "patient-123", TENANT_ID, "Nonexistent Plan"))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}/title/{title}",
                "patient-123", "Nonexistent Plan")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound());

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenantAndTitle("patient-123", TENANT_ID, "Nonexistent Plan");
    }

    // ============ Multi-tenant Isolation Tests ============

    @Test
    void shouldEnforceTenantIsolation_ForCarePlans() throws Exception {
        // Given
        List<CarePlanProjection> tenant1Plans = List.of(
            CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build()
        );

        when(carePlanQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(tenant1Plans);
        when(carePlanQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        // When & Then - tenant-001 sees care plan
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        // When & Then - tenant-002 does NOT see care plan (isolated)
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(carePlanQueryService)
            .findByPatientAndTenant("patient-123", "tenant-001");
        verify(carePlanQueryService)
            .findByPatientAndTenant("patient-123", "tenant-002");
    }

    // ============ Response Field Validation Tests ============

    @Test
    void shouldIncludeAllFields_InCarePlanResponse() throws Exception {
        // Given
        CarePlanProjection carePlan = CarePlanProjection.builder()
            .patientId("patient-123")
            .title("Diabetes Management Plan")
            .status("active")
            .coordinatorId("coordinator-001")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .goalCount(5)
            .build();

        when(carePlanQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of(carePlan));

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].patientId").exists())
            .andExpect(jsonPath("$[0].title").exists())
            .andExpect(jsonPath("$[0].status").exists())
            .andExpect(jsonPath("$[0].coordinatorId").exists())
            .andExpect(jsonPath("$[0].startDate").exists())
            .andExpect(jsonPath("$[0].endDate").exists())
            .andExpect(jsonPath("$[0].goalCount").exists());

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldMapGoalCountCorrectly_InResponse() throws Exception {
        // Given
        CarePlanProjection carePlan = CarePlanProjection.builder()
            .patientId("patient-123")
            .title("Diabetes Management Plan")
            .status("active")
            .coordinatorId("coordinator-001")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .goalCount(7)
            .build();

        when(carePlanQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of(carePlan));

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].goalCount").value(7));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    // ============ Content-Type Tests ============

    @Test
    void shouldReturnJsonContentType_ForCarePlans() throws Exception {
        // Given
        when(carePlanQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }
}
