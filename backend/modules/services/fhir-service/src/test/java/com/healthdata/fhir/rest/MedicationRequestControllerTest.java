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

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
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

import com.healthdata.fhir.service.MedicationRequestService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationRequest Controller Tests")
class MedicationRequestControllerTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private MedicationRequestService medicationRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MedicationRequestController controller = new MedicationRequestController(medicationRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create medication request")
    void shouldCreateMedicationRequest() throws Exception {
        MedicationRequest request = buildMedicationRequest(UUID.randomUUID());
        when(medicationRequestService.createMedicationRequest(eq("tenant-1"), any(MedicationRequest.class), eq("user")))
                .thenReturn(request);

        mockMvc.perform(post("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/MedicationRequest/" + request.getId()))
                .andExpect(content().string(containsString(request.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreateError() throws Exception {
        when(medicationRequestService.createMedicationRequest(eq("tenant-1"), any(MedicationRequest.class), eq("user")))
                .thenThrow(new IllegalArgumentException("create failed"));

        mockMvc.perform(post("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationRequest\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("create failed")));
    }

    @Test
    @DisplayName("Should return bad request on create invalid JSON")
    void shouldReturnBadRequestOnCreateInvalidJson() throws Exception {
        mockMvc.perform(post("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get medication request")
    void shouldGetMedicationRequest() throws Exception {
        MedicationRequest request = buildMedicationRequest(UUID.randomUUID());
        when(medicationRequestService.getMedicationRequest("tenant-1", request.getId())).thenReturn(Optional.of(request));

        mockMvc.perform(get("/fhir/MedicationRequest/{id}", request.getId())
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(request.getId())));
    }

    @Test
    @DisplayName("Should return not found when medication request missing")
    void shouldReturnNotFoundWhenMissing() throws Exception {
        when(medicationRequestService.getMedicationRequest("tenant-1", "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/MedicationRequest/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update medication request")
    void shouldUpdateMedicationRequest() throws Exception {
        MedicationRequest request = buildMedicationRequest(UUID.randomUUID());
        when(medicationRequestService.updateMedicationRequest(eq("tenant-1"), eq(request.getId()), any(MedicationRequest.class), eq("user")))
                .thenReturn(request);

        mockMvc.perform(put("/fhir/MedicationRequest/{id}", request.getId())
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                .content(JSON_PARSER.encodeResourceToString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on update error")
    void shouldReturnBadRequestOnUpdateError() throws Exception {
        when(medicationRequestService.updateMedicationRequest(eq("tenant-1"), eq("id-1"), any(MedicationRequest.class), eq("user")))
                .thenThrow(new IllegalArgumentException("update failed"));

        mockMvc.perform(put("/fhir/MedicationRequest/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationRequest\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("update failed")));
    }

    @Test
    @DisplayName("Should return bad request on update invalid JSON")
    void shouldReturnBadRequestOnUpdateInvalidJson() throws Exception {
        mockMvc.perform(put("/fhir/MedicationRequest/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found on update")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new MedicationRequestService.MedicationRequestNotFoundException("missing"))
                .when(medicationRequestService).updateMedicationRequest(eq("tenant-1"), eq("missing"), any(MedicationRequest.class), eq("user"));

        mockMvc.perform(put("/fhir/MedicationRequest/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"MedicationRequest\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete medication request")
    void shouldDeleteMedicationRequest() throws Exception {
        mockMvc.perform(delete("/fhir/MedicationRequest/{id}", "id-1")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete when missing")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new MedicationRequestService.MedicationRequestNotFoundException("missing"))
                .when(medicationRequestService).deleteMedicationRequest("tenant-1", "missing", "user");

        mockMvc.perform(delete("/fhir/MedicationRequest/{id}", "missing")
                        .header("X-Tenant-ID", "tenant-1")
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search medication requests by patient")
    void shouldSearchMedicationRequestsByPatient() throws Exception {
        when(medicationRequestService.searchMedicationRequestsByPatient(eq("tenant-1"), eq("patient-1"), any()))
                .thenReturn(new org.hl7.fhir.r4.model.Bundle());

        mockMvc.perform(get("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search medication requests by patient and code")
    void shouldSearchMedicationRequestsByPatientAndCode() throws Exception {
        when(medicationRequestService.searchMedicationRequestsByPatientAndCode("tenant-1", "patient-1", "med-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                .param("code", "med-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on search by code error")
    void shouldReturnBadRequestOnSearchByCodeError() throws Exception {
        when(medicationRequestService.searchMedicationRequestsByPatientAndCode("tenant-1", "patient-1", "med-1"))
                .thenThrow(new IllegalStateException("search code failed"));

        mockMvc.perform(get("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "med-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("search code failed")));
    }

    @Test
    @DisplayName("Should return bad request when patient missing")
    void shouldReturnBadRequestWhenPatientMissing() throws Exception {
        mockMvc.perform(get("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request on search exception")
    void shouldReturnBadRequestOnSearchException() throws Exception {
        doThrow(new IllegalArgumentException("bad search"))
                .when(medicationRequestService).searchMedicationRequestsByPatient(eq("tenant-1"), eq("patient-1"), any());

        mockMvc.perform(get("/fhir/MedicationRequest")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad search")));
    }

    @Test
    @DisplayName("Should return active medication requests")
    void shouldReturnActiveMedicationRequests() throws Exception {
        when(medicationRequestService.getActiveRequestsByPatient("tenant-1", "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationRequest/active")
                        .header("X-Tenant-ID", "tenant-1")
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on active requests error")
    void shouldReturnBadRequestOnActiveRequestsError() throws Exception {
        when(medicationRequestService.getActiveRequestsByPatient("tenant-1", "patient-1"))
                .thenThrow(new IllegalStateException("active failed"));

        mockMvc.perform(get("/fhir/MedicationRequest/active")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("active failed")));
    }

    @Test
    @DisplayName("Should return prescriptions")
    void shouldReturnPrescriptions() throws Exception {
        when(medicationRequestService.getPrescriptionsByPatient("tenant-1", "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationRequest/prescriptions")
                        .header("X-Tenant-ID", "tenant-1")
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on prescriptions error")
    void shouldReturnBadRequestOnPrescriptionsError() throws Exception {
        when(medicationRequestService.getPrescriptionsByPatient("tenant-1", "patient-1"))
                .thenThrow(new IllegalStateException("prescriptions failed"));

        mockMvc.perform(get("/fhir/MedicationRequest/prescriptions")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("prescriptions failed")));
    }

    @Test
    @DisplayName("Should return requests with refills")
    void shouldReturnRequestsWithRefills() throws Exception {
        when(medicationRequestService.getRequestsWithRefills("tenant-1", "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/MedicationRequest/with-refills")
                        .header("X-Tenant-ID", "tenant-1")
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on refills error")
    void shouldReturnBadRequestOnRefillsError() throws Exception {
        when(medicationRequestService.getRequestsWithRefills("tenant-1", "patient-1"))
                .thenThrow(new IllegalStateException("refills failed"));

        mockMvc.perform(get("/fhir/MedicationRequest/with-refills")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("refills failed")));
    }

    @Test
    @DisplayName("Should check has medication")
    void shouldCheckHasMedication() throws Exception {
        when(medicationRequestService.hasActiveMedication("tenant-1", "patient-1", "med-1"))
                .thenReturn(true);

        mockMvc.perform(get("/fhir/MedicationRequest/has-medication")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "med-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    @DisplayName("Should return bad request on has medication error")
    void shouldReturnBadRequestOnHasMedicationError() throws Exception {
        when(medicationRequestService.hasActiveMedication("tenant-1", "patient-1", "med-1"))
                .thenThrow(new IllegalStateException("has medication failed"));

        mockMvc.perform(get("/fhir/MedicationRequest/has-medication")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("patient", "patient-1")
                        .param("code", "med-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("has medication failed")));
    }
    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/fhir/MedicationRequest/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MedicationRequest")));
    }

    private MedicationRequest buildMedicationRequest(UUID id) {
        MedicationRequest request = new MedicationRequest();
        request.setId(id.toString());
        request.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        return request;
    }
}
