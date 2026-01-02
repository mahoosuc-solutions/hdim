package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.MedicationAdministrationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationAdministration Controller Tests")
class MedicationAdministrationControllerTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private MedicationAdministrationService medicationAdministrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MedicationAdministrationController controller = new MedicationAdministrationController(medicationAdministrationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create medication administration")
    void shouldCreateMedicationAdministration() throws Exception {
        MedicationAdministration admin = buildMedicationAdministration(UUID.randomUUID());
        when(medicationAdministrationService.createMedicationAdministration(eq("tenant-1"), any(MedicationAdministration.class), eq("user")))
                .thenReturn(admin);

        mockMvc.perform(post("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(admin)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/MedicationAdministration/" + admin.getId()))
                .andExpect(content().string(containsString(admin.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreateError() throws Exception {
        when(medicationAdministrationService.createMedicationAdministration(
                eq("tenant-1"), any(MedicationAdministration.class), eq("user")))
                .thenThrow(new IllegalArgumentException("create failed"));

        mockMvc.perform(post("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationAdministration\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("create failed")));
    }

    @Test
    @DisplayName("Should get medication administration")
    void shouldGetMedicationAdministration() throws Exception {
        MedicationAdministration admin = buildMedicationAdministration(UUID.randomUUID());
        when(medicationAdministrationService.getMedicationAdministration("tenant-1", admin.getId()))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(get("/fhir/MedicationAdministration/{id}", admin.getId())
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(admin.getId())));
    }

    @Test
    @DisplayName("Should return not found when medication administration missing")
    void shouldReturnNotFoundWhenMedicationAdministrationMissing() throws Exception {
        when(medicationAdministrationService.getMedicationAdministration("tenant-1", "missing"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/MedicationAdministration/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update medication administration")
    void shouldUpdateMedicationAdministration() throws Exception {
        MedicationAdministration admin = buildMedicationAdministration(UUID.randomUUID());
        when(medicationAdministrationService.updateMedicationAdministration(eq("tenant-1"), eq(admin.getId()), any(MedicationAdministration.class), eq("user")))
                .thenReturn(admin);

        mockMvc.perform(put("/fhir/MedicationAdministration/{id}", admin.getId())
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                .content(JSON_PARSER.encodeResourceToString(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on update error")
    void shouldReturnBadRequestOnUpdateError() throws Exception {
        when(medicationAdministrationService.updateMedicationAdministration(
                eq("tenant-1"), eq("id-1"), any(MedicationAdministration.class), eq("user")))
                .thenThrow(new IllegalArgumentException("update failed"));

        mockMvc.perform(put("/fhir/MedicationAdministration/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationAdministration\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("update failed")));
    }

    @Test
    @DisplayName("Should return bad request on invalid JSON")
    void shouldReturnBadRequestOnInvalidJson() throws Exception {
        mockMvc.perform(post("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }

    @Test
    @DisplayName("Should return bad request on update parse failure")
    void shouldReturnBadRequestOnUpdateParseFailure() throws Exception {
        mockMvc.perform(put("/fhir/MedicationAdministration/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }

    @Test
    @DisplayName("Should return not found on update")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new MedicationAdministrationService.MedicationAdministrationNotFoundException("missing"))
                .when(medicationAdministrationService).updateMedicationAdministration(eq("tenant-1"), eq("missing"), any(MedicationAdministration.class), eq("user"));

        mockMvc.perform(put("/fhir/MedicationAdministration/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationAdministration\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete medication administration")
    void shouldDeleteMedicationAdministration() throws Exception {
        mockMvc.perform(delete("/fhir/MedicationAdministration/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete when missing")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new MedicationAdministrationService.MedicationAdministrationNotFoundException("missing"))
                .when(medicationAdministrationService).deleteMedicationAdministration("tenant-1", "missing", "user");

        mockMvc.perform(delete("/fhir/MedicationAdministration/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search medication administrations by patient")
    void shouldSearchMedicationAdministrationsByPatient() throws Exception {
        when(medicationAdministrationService.searchAdministrationsByPatient(eq("tenant-1"), eq("patient-1"), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on search error")
    void shouldReturnBadRequestOnSearchError() throws Exception {
        when(medicationAdministrationService.searchAdministrationsByPatient(eq("tenant-1"), eq("patient-1"), any()))
                .thenThrow(new IllegalStateException("search failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("search failed")));
    }

    @Test
    @DisplayName("Should search medication administrations by encounter")
    void shouldSearchMedicationAdministrationsByEncounter() throws Exception {
        when(medicationAdministrationService.searchAdministrationsByEncounter(eq("tenant-1"), eq("enc-1")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("encounter", "enc-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search medication administrations by patient and code")
    void shouldSearchMedicationAdministrationsByPatientAndCode() throws Exception {
        when(medicationAdministrationService.searchAdministrationsByPatientAndCode(eq("tenant-1"), eq("patient-1"), eq("CODE")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "CODE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request when search params missing")
    void shouldReturnBadRequestWhenSearchParamsMissing() throws Exception {
        mockMvc.perform(get("/fhir/MedicationAdministration")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patient or encounter parameter is required")));
    }

    @Test
    @DisplayName("Should return bad request on invalid date range")
    void shouldReturnBadRequestOnInvalidDateRange() throws Exception {
        mockMvc.perform(get("/fhir/MedicationAdministration/by-date")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("start", "bad")
                        .param("end", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }

    @Test
    @DisplayName("Should search medication administrations by date range")
    void shouldSearchMedicationAdministrationsByDateRange() throws Exception {
        when(medicationAdministrationService.getAdministrationsByDateRange(
                eq("tenant-1"), eq("patient-1"), any(), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration/by-date")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("start", "2025-01-01T00:00:00")
                .param("end", "2025-01-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on date range search error")
    void shouldReturnBadRequestOnDateRangeSearchError() throws Exception {
        when(medicationAdministrationService.getAdministrationsByDateRange(
                eq("tenant-1"), eq("patient-1"), any(), any()))
                .thenThrow(new IllegalStateException("date range failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/by-date")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("start", "2025-01-01T00:00:00")
                        .param("end", "2025-01-31T23:59:59"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("date range failed")));
    }

    @Test
    @DisplayName("Should get completed administrations")
    void shouldGetCompletedAdministrations() throws Exception {
        when(medicationAdministrationService.getCompletedAdministrationsByPatient("tenant-1", "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration/completed")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on completed administrations error")
    void shouldReturnBadRequestOnCompletedAdministrationsError() throws Exception {
        when(medicationAdministrationService.getCompletedAdministrationsByPatient("tenant-1", "patient-1"))
                .thenThrow(new IllegalStateException("completed failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/completed")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("completed failed")));
    }

    @Test
    @DisplayName("Should get in-progress administrations")
    void shouldGetInProgressAdministrations() throws Exception {
        when(medicationAdministrationService.getInProgressAdministrationsByPatient("tenant-1", "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration/in-progress")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on in-progress administrations error")
    void shouldReturnBadRequestOnInProgressAdministrationsError() throws Exception {
        when(medicationAdministrationService.getInProgressAdministrationsByPatient("tenant-1", "patient-1"))
                .thenThrow(new IllegalStateException("in-progress failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/in-progress")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("in-progress failed")));
    }

    @Test
    @DisplayName("Should get administrations by request")
    void shouldGetAdministrationsByRequest() throws Exception {
        when(medicationAdministrationService.getAdministrationHistoryByRequest("tenant-1", "request-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration/by-request")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("request", "request-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on administrations by request error")
    void shouldReturnBadRequestOnAdministrationsByRequestError() throws Exception {
        when(medicationAdministrationService.getAdministrationHistoryByRequest("tenant-1", "request-1"))
                .thenThrow(new IllegalStateException("request failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/by-request")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("request", "request-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("request failed")));
    }

    @Test
    @DisplayName("Should get administrations by lot number")
    void shouldGetAdministrationsByLotNumber() throws Exception {
        when(medicationAdministrationService.getAdministrationsByLotNumber("tenant-1", "lot-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationAdministration/by-lot")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("lot", "lot-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on administrations by lot number error")
    void shouldReturnBadRequestOnAdministrationsByLotNumberError() throws Exception {
        when(medicationAdministrationService.getAdministrationsByLotNumber("tenant-1", "lot-1"))
                .thenThrow(new IllegalStateException("lot failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/by-lot")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("lot", "lot-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("lot failed")));
    }

    @Test
    @DisplayName("Should return administered today status")
    void shouldReturnAdministeredTodayStatus() throws Exception {
        when(medicationAdministrationService.hasMedicationBeenAdministeredToday("tenant-1", "patient-1", "CODE"))
                .thenReturn(true);

        mockMvc.perform(get("/fhir/MedicationAdministration/administered-today")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "CODE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("administeredToday")));

        verify(medicationAdministrationService)
                .hasMedicationBeenAdministeredToday("tenant-1", "patient-1", "CODE");
    }

    @Test
    @DisplayName("Should return bad request on administered today error")
    void shouldReturnBadRequestOnAdministeredTodayError() throws Exception {
        when(medicationAdministrationService.hasMedicationBeenAdministeredToday("tenant-1", "patient-1", "CODE"))
                .thenThrow(new IllegalStateException("administered failed"));

        mockMvc.perform(get("/fhir/MedicationAdministration/administered-today")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "CODE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("administered failed")));
    }
    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/fhir/MedicationAdministration/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MedicationAdministration")));
    }

    private MedicationAdministration buildMedicationAdministration(UUID id) {
        MedicationAdministration admin = new MedicationAdministration();
        admin.setId(id.toString());
        admin.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        return admin;
    }
}
