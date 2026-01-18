package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.query.condition.ConditionQueryService;
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
class ConditionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConditionQueryService conditionQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConditionController(conditionQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnConditions_WhenConditionsExist() throws Exception {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()
        );

        when(conditionQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(conditions);

        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].icdCode").value("E11.9"));

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(conditionQueryService, never()).findByPatientAndTenant(anyString(), anyString());
    }

    @Test
    void shouldReturnConditions_WhenIcdCodeFound() throws Exception {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId("patient-456")
                .icdCode("E11.9")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2016, 3, 10))
                .build()
        );

        when(conditionQueryService.findByIcdCodeAndTenant("E11.9", TENANT_ID))
            .thenReturn(conditions);

        mockMvc.perform(get("/api/v1/conditions/icd/{icdCode}", "E11.9")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(conditionQueryService, times(1))
            .findByIcdCodeAndTenant("E11.9", TENANT_ID);
    }

    @Test
    void shouldReturnActiveConditions() throws Exception {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()
        );

        when(conditionQueryService.findActiveConditionsByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(conditions);

        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}/active", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("active"));

        verify(conditionQueryService, times(1))
            .findActiveConditionsByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldEnforceTenantIsolation_ForConditions() throws Exception {
        when(conditionQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(List.of(ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()));
        when(conditionQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
