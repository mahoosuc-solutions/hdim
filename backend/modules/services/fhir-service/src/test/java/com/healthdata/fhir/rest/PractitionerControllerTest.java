package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.PractitionerService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for PractitionerController.
 * Tests REST endpoints with mock service layer using standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Practitioner Controller Tests")
@Tag("unit")
class PractitionerControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private PractitionerService practitionerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PractitionerController controller = new PractitionerController(practitionerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create practitioner - 201 Created")
    void shouldCreatePractitioner() throws Exception {
        Practitioner practitioner = createTestPractitioner();
        when(practitionerService.createPractitioner(eq(TENANT_ID), any(Practitioner.class), eq("user")))
                .thenReturn(practitioner);

        mockMvc.perform(post("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(practitioner)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(practitioner.getId())));
    }

    @Test
    @DisplayName("Should get practitioner by ID - 200 OK")
    void shouldGetPractitioner() throws Exception {
        UUID id = UUID.randomUUID();
        Practitioner practitioner = createTestPractitioner();
        practitioner.setId(id.toString());
        when(practitionerService.getPractitioner(TENANT_ID, id))
                .thenReturn(Optional.of(practitioner));

        mockMvc.perform(get("/Practitioner/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(practitioner.getId())));
    }

    @Test
    @DisplayName("Should return 404 when practitioner not found")
    void shouldReturnNotFoundWhenPractitionerMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(practitionerService.getPractitioner(TENANT_ID, id))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/Practitioner/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update practitioner - 200 OK")
    void shouldUpdatePractitioner() throws Exception {
        UUID id = UUID.randomUUID();
        Practitioner practitioner = createTestPractitioner();
        practitioner.setId(id.toString());
        when(practitionerService.updatePractitioner(eq(TENANT_ID), eq(id), any(Practitioner.class), eq("user")))
                .thenReturn(practitioner);

        mockMvc.perform(put("/Practitioner/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(practitioner)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(practitioner.getId())));
    }

    @Test
    @DisplayName("Should delete practitioner - 204 No Content")
    void shouldDeletePractitioner() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/Practitioner/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search practitioners by name")
    void shouldSearchPractitionersByName() throws Exception {
        Practitioner practitioner = createTestPractitioner();
        when(practitionerService.findByName(TENANT_ID, "Chen"))
                .thenReturn(List.of(practitioner));

        mockMvc.perform(get("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("name", "Chen"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chen")));
    }

    @Test
    @DisplayName("Should search practitioners by identifier")
    void shouldSearchPractitionersByIdentifier() throws Exception {
        Practitioner practitioner = createTestPractitioner();
        when(practitionerService.findByIdentifier(TENANT_ID, "1234567890"))
                .thenReturn(Optional.of(practitioner));

        mockMvc.perform(get("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://hl7.org/fhir/sid/us-npi|1234567890"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chen")));
    }

    @Test
    @DisplayName("Should return empty bundle when identifier not found")
    void shouldReturnEmptyBundleWhenIdentifierNotFound() throws Exception {
        when(practitionerService.findByIdentifier(TENANT_ID, "unknown"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://hl7.org/fhir/sid/us-npi|unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\": 0")));
    }

    @Test
    @DisplayName("Should return paged results for default search")
    void shouldReturnPagedResultsForDefaultSearch() throws Exception {
        Practitioner practitioner = createTestPractitioner();
        when(practitionerService.searchPractitioners(eq(TENANT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(practitioner)));

        mockMvc.perform(get("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("_page", "0")
                        .param("_count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")));
    }

    @Test
    @DisplayName("Should use default user ID when X-User-ID header missing")
    void shouldUseDefaultUserIdWhenHeaderMissing() throws Exception {
        Practitioner practitioner = createTestPractitioner();
        when(practitionerService.createPractitioner(eq(TENANT_ID), any(Practitioner.class), eq("system")))
                .thenReturn(practitioner);

        mockMvc.perform(post("/Practitioner")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(practitioner)))
                .andExpect(status().isCreated());
    }

    // ==================== Helper Methods ====================

    private Practitioner createTestPractitioner() {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());
        practitioner.setActive(true);

        HumanName name = practitioner.addName();
        name.setFamily("Chen");
        name.addGiven("Sarah");
        name.addPrefix("Dr.");

        practitioner.addIdentifier()
                .setSystem("http://hl7.org/fhir/sid/us-npi")
                .setValue("1234567890");

        return practitioner;
    }
}
