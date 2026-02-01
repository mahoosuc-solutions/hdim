package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.query.careplan.CarePlanQueryService;
import com.healthdata.queryapi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CarePlanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CarePlanQueryService carePlanQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CarePlanController(carePlanQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnCarePlans_WhenCarePlansExist() throws Exception {
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

        when(carePlanQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(carePlans);

        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Diabetes Management Plan"));

        verify(carePlanQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(carePlanQueryService, never()).findByPatientAndTenant(anyString(), anyString());
    }

    @Test
    void shouldReturnCarePlans_WhenCarePlansAssignedToCoordinator() throws Exception {
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

        mockMvc.perform(get("/api/v1/care-plans/coordinator/{coordinatorId}", "coordinator-001")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(carePlanQueryService, times(1))
            .findByTenantAndCoordinator(TENANT_ID, "coordinator-001");
    }

    @Test
    void shouldReturnActiveCarePlans() throws Exception {
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

        when(carePlanQueryService.findActiveCarePlansByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(carePlans);

        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}/active", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("active"));

        verify(carePlanQueryService, times(1))
            .findActiveCarePlansByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldEnforceTenantIsolation_ForCarePlans() throws Exception {
        when(carePlanQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(List.of(CarePlanProjection.builder()
                .patientId("patient-123")
                .title("Diabetes Management Plan")
                .status("active")
                .coordinatorId("coordinator-001")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .goalCount(5)
                .build()));
        when(carePlanQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/care-plans/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
