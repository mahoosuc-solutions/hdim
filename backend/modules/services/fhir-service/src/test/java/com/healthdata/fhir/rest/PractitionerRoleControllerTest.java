package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
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

import com.healthdata.fhir.service.PractitionerRoleService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for PractitionerRoleController.
 * Tests REST endpoints with mock service layer using standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PractitionerRole Controller Tests")
@Tag("unit")
class PractitionerRoleControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final UUID PRACTITIONER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private PractitionerRoleService practitionerRoleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PractitionerRoleController controller = new PractitionerRoleController(practitionerRoleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create practitioner role - 201 Created")
    void shouldCreatePractitionerRole() throws Exception {
        PractitionerRole role = createTestRole();
        when(practitionerRoleService.createPractitionerRole(eq(TENANT_ID), any(PractitionerRole.class), eq("user")))
                .thenReturn(role);

        mockMvc.perform(post("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(role)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(role.getId())));
    }

    @Test
    @DisplayName("Should get practitioner role by ID - 200 OK")
    void shouldGetPractitionerRole() throws Exception {
        UUID id = UUID.randomUUID();
        PractitionerRole role = createTestRole();
        role.setId(id.toString());
        when(practitionerRoleService.getPractitionerRole(TENANT_ID, id))
                .thenReturn(Optional.of(role));

        mockMvc.perform(get("/PractitionerRole/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(role.getId())));
    }

    @Test
    @DisplayName("Should return 404 when practitioner role not found")
    void shouldReturnNotFoundWhenRoleMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(practitionerRoleService.getPractitionerRole(TENANT_ID, id))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/PractitionerRole/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update practitioner role - 200 OK")
    void shouldUpdatePractitionerRole() throws Exception {
        UUID id = UUID.randomUUID();
        PractitionerRole role = createTestRole();
        role.setId(id.toString());
        when(practitionerRoleService.updatePractitionerRole(eq(TENANT_ID), eq(id), any(PractitionerRole.class), eq("user")))
                .thenReturn(role);

        mockMvc.perform(put("/PractitionerRole/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(role)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(role.getId())));
    }

    @Test
    @DisplayName("Should delete practitioner role - 204 No Content")
    void shouldDeletePractitionerRole() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/PractitionerRole/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search roles by practitioner reference")
    void shouldSearchRolesByPractitioner() throws Exception {
        PractitionerRole role = createTestRole();
        when(practitionerRoleService.findByPractitioner(TENANT_ID, PRACTITIONER_ID.toString()))
                .thenReturn(List.of(role));

        mockMvc.perform(get("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("practitioner", "Practitioner/" + PRACTITIONER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")));
    }

    @Test
    @DisplayName("Should search roles by role code")
    void shouldSearchRolesByRoleCode() throws Exception {
        PractitionerRole role = createTestRole();
        when(practitionerRoleService.findByRoleCode(TENANT_ID, "doctor"))
                .thenReturn(List.of(role));

        mockMvc.perform(get("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("role", "doctor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")));
    }

    @Test
    @DisplayName("Should search roles by identifier")
    void shouldSearchRolesByIdentifier() throws Exception {
        PractitionerRole role = createTestRole();
        when(practitionerRoleService.findByIdentifier(TENANT_ID, "ROLE-001"))
                .thenReturn(Optional.of(role));

        mockMvc.perform(get("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://healthdata.com|ROLE-001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\": 1")));
    }

    @Test
    @DisplayName("Should return empty bundle when identifier not found")
    void shouldReturnEmptyBundleWhenIdentifierNotFound() throws Exception {
        when(practitionerRoleService.findByIdentifier(TENANT_ID, "unknown"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://healthdata.com|unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\": 0")));
    }

    @Test
    @DisplayName("Should return paged results for default search")
    void shouldReturnPagedResultsForDefaultSearch() throws Exception {
        PractitionerRole role = createTestRole();
        when(practitionerRoleService.searchPractitionerRoles(eq(TENANT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(role)));

        mockMvc.perform(get("/PractitionerRole")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")));
    }

    // ==================== Helper Methods ====================

    private PractitionerRole createTestRole() {
        PractitionerRole role = new PractitionerRole();
        role.setId(UUID.randomUUID().toString());
        role.setActive(true);

        role.setPractitioner(new Reference("Practitioner/" + PRACTITIONER_ID)
                .setDisplay("Dr. Sarah Chen"));

        CodeableConcept roleCode = new CodeableConcept();
        roleCode.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/practitioner-role")
                .setCode("doctor")
                .setDisplay("Doctor");
        role.addCode(roleCode);

        return role;
    }
}
