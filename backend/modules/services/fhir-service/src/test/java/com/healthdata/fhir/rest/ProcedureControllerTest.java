package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.ProcedureService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Procedure Controller Tests")
@Tag("integration")
class ProcedureControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private ProcedureService procedureService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProcedureController controller = new ProcedureController(procedureService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create procedure")
    void shouldCreateProcedure() throws Exception {
        Procedure procedure = buildProcedure(UUID.randomUUID());
        when(procedureService.createProcedure(eq(TENANT_ID), any(Procedure.class), eq("user")))
                .thenReturn(procedure);

        mockMvc.perform(post("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(procedure)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/Procedure/" + procedure.getId()))
                .andExpect(content().string(containsString(procedure.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreateError() throws Exception {
        when(procedureService.createProcedure(eq(TENANT_ID), any(Procedure.class), eq("user")))
                .thenThrow(new IllegalArgumentException("create failure"));

        mockMvc.perform(post("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Procedure\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("create failure")));
    }

    @Test
    @DisplayName("Should return bad request on create with invalid JSON")
    void shouldReturnBadRequestOnCreateInvalidJson() throws Exception {
        mockMvc.perform(post("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get procedure")
    void shouldGetProcedure() throws Exception {
        Procedure procedure = buildProcedure(UUID.randomUUID());
        when(procedureService.getProcedure(TENANT_ID, procedure.getId())).thenReturn(Optional.of(procedure));

        mockMvc.perform(get("/fhir/Procedure/{id}", procedure.getId())
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(procedure.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing procedure")
    void shouldReturnNotFound() throws Exception {
        when(procedureService.getProcedure(TENANT_ID, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/Procedure/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update procedure")
    void shouldUpdateProcedure() throws Exception {
        Procedure procedure = buildProcedure(UUID.randomUUID());
        when(procedureService.updateProcedure(eq(TENANT_ID), eq(procedure.getId()), any(Procedure.class), eq("user")))
                .thenReturn(procedure);

        mockMvc.perform(put("/fhir/Procedure/{id}", procedure.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(procedure)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(procedure.getId())));
    }

    @Test
    @DisplayName("Should return bad request on update error")
    void shouldReturnBadRequestOnUpdateError() throws Exception {
        when(procedureService.updateProcedure(eq(TENANT_ID), eq("id-1"), any(Procedure.class), eq("user")))
                .thenThrow(new IllegalArgumentException("update failure"));

        mockMvc.perform(put("/fhir/Procedure/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Procedure\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("update failure")));
    }

    @Test
    @DisplayName("Should return bad request on update invalid JSON")
    void shouldReturnBadRequestOnUpdateInvalidJson() throws Exception {
        mockMvc.perform(put("/fhir/Procedure/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found on update")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new ProcedureService.ProcedureNotFoundException("missing"))
                .when(procedureService).updateProcedure(eq(TENANT_ID), eq("missing"), any(Procedure.class), eq("user"));

        mockMvc.perform(put("/fhir/Procedure/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Procedure\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete procedure")
    void shouldDeleteProcedure() throws Exception {
        mockMvc.perform(delete("/fhir/Procedure/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete when missing")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new ProcedureService.ProcedureNotFoundException("missing"))
                .when(procedureService).deleteProcedure(TENANT_ID, "missing", "user");

        mockMvc.perform(delete("/fhir/Procedure/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search procedures by patient")
    void shouldSearchProceduresByPatient() throws Exception {
        when(procedureService.searchProceduresByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search procedures by patient and date range")
    void shouldSearchProceduresByPatientAndDateRange() throws Exception {
        when(procedureService.searchProceduresByPatientAndDateRange(
                TENANT_ID, "patient-1", LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2025-01-01")
                        .param("date-end", "2025-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request when patient missing")
    void shouldReturnBadRequestWhenPatientMissing() throws Exception {
        mockMvc.perform(get("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patient parameter is required")));
    }

    @Test
    @DisplayName("Should return bad request when search throws exception")
    void shouldReturnBadRequestOnSearchException() throws Exception {
        doThrow(new IllegalArgumentException("bad search"))
                .when(procedureService).searchProceduresByPatient(eq(TENANT_ID), eq("patient-1"), any());

        mockMvc.perform(get("/fhir/Procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad search")));
    }

    @Test
    @DisplayName("Should get completed procedures")
    void shouldGetCompletedProcedures() throws Exception {
        when(procedureService.getCompletedProceduresByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure/completed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on completed procedures error")
    void shouldReturnBadRequestOnCompletedProceduresError() throws Exception {
        when(procedureService.getCompletedProceduresByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("completed failure"));

        mockMvc.perform(get("/fhir/Procedure/completed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("completed failure")));
    }

    @Test
    @DisplayName("Should get surgical procedures")
    void shouldGetSurgicalProcedures() throws Exception {
        when(procedureService.getSurgicalProceduresByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure/surgical")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on surgical procedures error")
    void shouldReturnBadRequestOnSurgicalProceduresError() throws Exception {
        when(procedureService.getSurgicalProceduresByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("surgical failure"));

        mockMvc.perform(get("/fhir/Procedure/surgical")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("surgical failure")));
    }

    @Test
    @DisplayName("Should get diagnostic procedures")
    void shouldGetDiagnosticProcedures() throws Exception {
        when(procedureService.getDiagnosticProceduresByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure/diagnostic")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on diagnostic procedures error")
    void shouldReturnBadRequestOnDiagnosticProceduresError() throws Exception {
        when(procedureService.getDiagnosticProceduresByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("diagnostic failure"));

        mockMvc.perform(get("/fhir/Procedure/diagnostic")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("diagnostic failure")));
    }

    @Test
    @DisplayName("Should get procedures with complications")
    void shouldGetProceduresWithComplications() throws Exception {
        when(procedureService.getProceduresWithComplications(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Procedure/with-complications")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on procedures with complications error")
    void shouldReturnBadRequestOnProceduresWithComplicationsError() throws Exception {
        when(procedureService.getProceduresWithComplications(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("complications failure"));

        mockMvc.perform(get("/fhir/Procedure/with-complications")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("complications failure")));
    }

    @Test
    @DisplayName("Should check has procedure")
    void shouldCheckHasProcedure() throws Exception {
        when(procedureService.hasCompletedProcedure(TENANT_ID, "patient-1", "PROC")).thenReturn(true);

        mockMvc.perform(get("/fhir/Procedure/has-procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "PROC"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    @DisplayName("Should return bad request on has procedure error")
    void shouldReturnBadRequestOnHasProcedureError() throws Exception {
        when(procedureService.hasCompletedProcedure(TENANT_ID, "patient-1", "PROC"))
                .thenThrow(new IllegalStateException("has procedure failure"));

        mockMvc.perform(get("/fhir/Procedure/has-procedure")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "PROC"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("has procedure failure")));
    }

    @Test
    @DisplayName("Should check has procedure in range")
    void shouldCheckHasProcedureInRange() throws Exception {
        when(procedureService.hasProcedureInDateRange(
                TENANT_ID, "patient-1", LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31")))
                .thenReturn(true);

        mockMvc.perform(get("/fhir/Procedure/has-procedure-in-range")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2025-01-01")
                        .param("date-end", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    @DisplayName("Should return bad request on has procedure in range error")
    void shouldReturnBadRequestOnHasProcedureInRangeError() throws Exception {
        when(procedureService.hasProcedureInDateRange(
                TENANT_ID, "patient-1", LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31")))
                .thenThrow(new IllegalStateException("range failure"));

        mockMvc.perform(get("/fhir/Procedure/has-procedure-in-range")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2025-01-01")
                        .param("date-end", "2025-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("range failure")));
    }
    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/fhir/Procedure/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Procedure")));
    }

    private Procedure buildProcedure(UUID id) {
        Procedure procedure = new Procedure();
        procedure.setId(id.toString());
        procedure.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        return procedure;
    }
}
