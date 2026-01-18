package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.query.condition.ConditionQueryService;
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
 * Unit tests for ConditionController (Phase 1.8)
 * Tests REST endpoints for conditions with ICD-10 filtering and status filtering
 */
@WebMvcTest(ConditionController.class)
class ConditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConditionQueryService conditionQueryService;

    private static final String TENANT_ID = "tenant-001";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    // ============ getConditionsByPatient Tests ============

    @Test
    void shouldReturnConditions_WhenConditionsExistForPatient() throws Exception {
        // Given
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus without complications")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("I10")
                .description("Essential (primary) hypertension")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2018, 6, 15))
                .build()
        );

        when(conditionQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"))
            .andExpect(jsonPath("$[0].icdCode").value("E11.9"))
            .andExpect(jsonPath("$[0].status").value("active"))
            .andExpect(jsonPath("$[1].icdCode").value("I10"));

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoConditionsForPatient() throws Exception {
        // Given
        when(conditionQueryService.findByPatientAndTenant("patient-456", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-456")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-456", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissingForPatientConditions() throws Exception {
        // When & Then - No X-Tenant-ID header
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123"))
            .andExpect(status().isBadRequest());

        verify(conditionQueryService, never())
            .findByPatientAndTenant(anyString(), anyString());
    }

    // ============ getConditionsByIcdCode Tests ============

    @Test
    void shouldReturnConditions_WhenIcdCodeFound() throws Exception {
        // Given
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus without complications")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId("patient-456")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus without complications")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2016, 3, 10))
                .build()
        );

        when(conditionQueryService.findByIcdCodeAndTenant("E11.9", TENANT_ID))
            .thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/icd/{icdCode}", "E11.9")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].icdCode").value(containsInAnyOrder("E11.9", "E11.9")))
            .andExpect(jsonPath("$[0].patientId").value("patient-123"))
            .andExpect(jsonPath("$[1].patientId").value("patient-456"));

        verify(conditionQueryService, times(1))
            .findByIcdCodeAndTenant("E11.9", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenIcdCodeNotFound() throws Exception {
        // Given
        when(conditionQueryService.findByIcdCodeAndTenant("Z99.9", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/icd/{icdCode}", "Z99.9")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(conditionQueryService, times(1))
            .findByIcdCodeAndTenant("Z99.9", TENANT_ID);
    }

    @Test
    void shouldReturn400_WhenTenantHeaderMissingForIcdCode() throws Exception {
        // When & Then - No X-Tenant-ID header
        mockMvc.perform(get("/api/v1/conditions/icd/{icdCode}", "E11.9"))
            .andExpect(status().isBadRequest());

        verify(conditionQueryService, never())
            .findByIcdCodeAndTenant(anyString(), anyString());
    }

    // ============ getActiveConditions Tests ============

    @Test
    void shouldReturnActiveConditions_WhenActiveConditionsExist() throws Exception {
        // Given
        List<ConditionProjection> activeConditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()
        );

        when(conditionQueryService.findActiveConditionsByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(activeConditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}/active", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("active"))
            .andExpect(jsonPath("$[0].icdCode").value("E11.9"));

        verify(conditionQueryService, times(1))
            .findActiveConditionsByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenNoActiveConditions() throws Exception {
        // Given
        when(conditionQueryService.findActiveConditionsByPatientAndTenant("patient-789", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}/active", "patient-789")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(conditionQueryService, times(1))
            .findActiveConditionsByPatientAndTenant("patient-789", TENANT_ID);
    }

    // ============ getConditionsByStatus Tests ============

    @Test
    void shouldReturnConditions_WhenStatusParameterProvided() throws Exception {
        // Given
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId("patient-456")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2016, 3, 10))
                .build()
        );

        when(conditionQueryService.findCarePlansByStatusAndTenant(TENANT_ID, "active"))
            .thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions")
                .param("status", "active")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(conditionQueryService, times(1))
            .findCarePlansByStatusAndTenant(TENANT_ID, "active");
    }

    @Test
    void shouldReturnAllConditions_WhenStatusParameterNotProvided() throws Exception {
        // Given
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId("patient-456")
                .icdCode("I10")
                .description("Essential hypertension")
                .status("inactive")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2010, 5, 20))
                .build()
        );

        when(conditionQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(conditionQueryService, times(1))
            .findAllByTenant(TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList_WhenStatusParameterEmpty() throws Exception {
        // Given
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()
        );

        when(conditionQueryService.findAllByTenant(TENANT_ID))
            .thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/v1/conditions")
                .param("status", "")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(conditionQueryService, times(1))
            .findAllByTenant(TENANT_ID);
    }

    // ============ Multi-tenant Isolation Tests ============

    @Test
    void shouldEnforceTenantIsolation_ForConditions() throws Exception {
        // Given
        List<ConditionProjection> tenant1Conditions = List.of(
            ConditionProjection.builder()
                .patientId("patient-123")
                .icdCode("E11.9")
                .description("Type 2 diabetes mellitus")
                .status("active")
                .verificationStatus("confirmed")
                .onsetDate(LocalDate.of(2015, 1, 1))
                .build()
        );

        when(conditionQueryService.findByPatientAndTenant("patient-123", "tenant-001"))
            .thenReturn(tenant1Conditions);
        when(conditionQueryService.findByPatientAndTenant("patient-123", "tenant-002"))
            .thenReturn(List.of());

        // When & Then - tenant-001 sees condition
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        // When & Then - tenant-002 does NOT see condition (isolated)
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, "tenant-002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(conditionQueryService)
            .findByPatientAndTenant("patient-123", "tenant-001");
        verify(conditionQueryService)
            .findByPatientAndTenant("patient-123", "tenant-002");
    }

    // ============ Response Field Validation Tests ============

    @Test
    void shouldIncludeAllFields_InConditionResponse() throws Exception {
        // Given
        ConditionProjection condition = ConditionProjection.builder()
            .patientId("patient-123")
            .icdCode("E11.9")
            .description("Type 2 diabetes mellitus without complications")
            .status("active")
            .verificationStatus("confirmed")
            .onsetDate(LocalDate.of(2015, 1, 1))
            .build();

        when(conditionQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of(condition));

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].patientId").exists())
            .andExpect(jsonPath("$[0].icdCode").exists())
            .andExpect(jsonPath("$[0].status").exists())
            .andExpect(jsonPath("$[0].verificationStatus").exists())
            .andExpect(jsonPath("$[0].onsetDate").exists());

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    @Test
    void shouldMapIcdCodeCorrectly_InResponse() throws Exception {
        // Given
        ConditionProjection condition = ConditionProjection.builder()
            .patientId("patient-123")
            .icdCode("E11.9")
            .description("Type 2 diabetes mellitus without complications")
            .status("active")
            .verificationStatus("confirmed")
            .onsetDate(LocalDate.of(2015, 1, 1))
            .build();

        when(conditionQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of(condition));

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].icdCode").value("E11.9"));

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }

    // ============ Content-Type Tests ============

    @Test
    void shouldReturnJsonContentType_ForConditions() throws Exception {
        // Given
        when(conditionQueryService.findByPatientAndTenant("patient-123", TENANT_ID))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/conditions/patient/{patientId}", "patient-123")
                .header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));

        verify(conditionQueryService, times(1))
            .findByPatientAndTenant("patient-123", TENANT_ID);
    }
}
