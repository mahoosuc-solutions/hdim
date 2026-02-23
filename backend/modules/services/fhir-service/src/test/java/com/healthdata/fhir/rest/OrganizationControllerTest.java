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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Organization;
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

import com.healthdata.fhir.service.OrganizationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for OrganizationController.
 * Tests REST endpoints with mock service layer using standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Organization Controller Tests")
@Tag("unit")
class OrganizationControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private OrganizationService organizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrganizationController controller = new OrganizationController(organizationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create organization - 201 Created")
    void shouldCreateOrganization() throws Exception {
        Organization org = createTestOrganization();
        when(organizationService.createOrganization(eq(TENANT_ID), any(Organization.class), eq("user")))
                .thenReturn(org);

        mockMvc.perform(post("/Organization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(org)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(org.getId())));
    }

    @Test
    @DisplayName("Should get organization by ID - 200 OK")
    void shouldGetOrganization() throws Exception {
        UUID id = UUID.randomUUID();
        Organization org = createTestOrganization();
        org.setId(id.toString());
        when(organizationService.getOrganization(TENANT_ID, id))
                .thenReturn(Optional.of(org));

        mockMvc.perform(get("/Organization/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(org.getId())));
    }

    @Test
    @DisplayName("Should return 404 when organization not found")
    void shouldReturnNotFoundWhenOrganizationMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getOrganization(TENANT_ID, id))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/Organization/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update organization - 200 OK")
    void shouldUpdateOrganization() throws Exception {
        UUID id = UUID.randomUUID();
        Organization org = createTestOrganization();
        org.setId(id.toString());
        when(organizationService.updateOrganization(eq(TENANT_ID), eq(id), any(Organization.class), eq("user")))
                .thenReturn(org);

        mockMvc.perform(put("/Organization/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(org)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(org.getId())));
    }

    @Test
    @DisplayName("Should delete organization - 204 No Content")
    void shouldDeleteOrganization() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/Organization/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search organizations by name")
    void shouldSearchOrganizationsByName() throws Exception {
        Organization org = createTestOrganization();
        when(organizationService.findByName(TENANT_ID, "Main"))
                .thenReturn(List.of(org));

        mockMvc.perform(get("/Organization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("name", "Main"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Main Street")));
    }

    @Test
    @DisplayName("Should search organizations by identifier")
    void shouldSearchOrganizationsByIdentifier() throws Exception {
        Organization org = createTestOrganization();
        when(organizationService.findByIdentifier(TENANT_ID, "ORG-001"))
                .thenReturn(Optional.of(org));

        mockMvc.perform(get("/Organization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://healthdata.com|ORG-001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\": 1")));
    }

    @Test
    @DisplayName("Should return empty bundle when identifier not found")
    void shouldReturnEmptyBundleWhenIdentifierNotFound() throws Exception {
        when(organizationService.findByIdentifier(TENANT_ID, "unknown"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/Organization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("identifier", "http://healthdata.com|unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\": 0")));
    }

    @Test
    @DisplayName("Should return paged results for default search")
    void shouldReturnPagedResultsForDefaultSearch() throws Exception {
        Organization org = createTestOrganization();
        when(organizationService.searchOrganizations(eq(TENANT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        mockMvc.perform(get("/Organization")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")));
    }

    // ==================== Helper Methods ====================

    private Organization createTestOrganization() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID().toString());
        org.setActive(true);
        org.setName("Main Street Family Practice");

        org.addIdentifier()
                .setSystem("http://healthdata.com/fhir/identifier/organization")
                .setValue("ORG-001");

        CodeableConcept type = new CodeableConcept();
        type.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider");
        org.addType(type);

        return org;
    }
}
