package com.healthdata.cql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.dto.CqlLibraryRequest;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.test.CqlTestcontainersBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CQL Library Controller
 *
 * Tests all library management endpoints including:
 * - CRUD operations
 * - Version management
 * - Status lifecycle (DRAFT -> ACTIVE -> RETIRED)
 * - Search and filtering
 * - Compilation and validation
 * - Multi-tenant isolation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class CqlLibraryControllerIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String BASE_URL = "/api/v1/cql/libraries";
    private static final String TENANT_ID = "test-tenant";

    private CqlLibrary testLibrary;

    @BeforeEach
    void setUp() {
        testLibrary = new CqlLibrary(TENANT_ID, "DiabetesScreening", "1.0.0");
        testLibrary.setStatus("ACTIVE");
        testLibrary.setCqlContent("library DiabetesScreening version '1.0.0'");
        testLibrary.setDescription("HEDIS Diabetes Screening");
        testLibrary.setPublisher("NCQA");
        testLibrary = libraryRepository.save(testLibrary);
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new library")
    void testCreateLibrary() throws Exception {
        CqlLibraryRequest newLibrary = new CqlLibraryRequest("NewLibrary", "1.0.0", "library NewLibrary version '1.0.0'");
        newLibrary.setStatus("DRAFT");
        newLibrary.setDescription("Test library");

        String requestBody = objectMapper.writeValueAsString(newLibrary);

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.libraryName").value("NewLibrary"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create duplicate library")
    void testCreateDuplicateLibrary() throws Exception {
        // Try to create library with same name and version
        CqlLibrary duplicate = new CqlLibrary(TENANT_ID, "DiabetesScreening", "1.0.0");
        String requestBody = objectMapper.writeValueAsString(duplicate);

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(3)
    @DisplayName("Should get library by ID")
    void testGetLibraryById() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLibrary.getId().toString()))
                .andExpect(jsonPath("$.libraryName").value("DiabetesScreening"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 for non-existent library")
    void testGetNonExistentLibrary() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("Should get all libraries with pagination")
    void testGetAllLibraries() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.content[0].tenantId").value(TENANT_ID));
    }

    @Test
    @Order(6)
    @DisplayName("Should get library by name and version")
    void testGetLibraryByNameAndVersion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-name/DiabetesScreening/version/1.0.0")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryName").value("DiabetesScreening"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @Order(7)
    @DisplayName("Should get latest library version")
    void testGetLatestLibraryVersion() throws Exception {
        // Create multiple versions
        CqlLibrary v2 = new CqlLibrary(TENANT_ID, "VersionTest", "1.0.0");
        libraryRepository.save(v2);

        CqlLibrary v3 = new CqlLibrary(TENANT_ID, "VersionTest", "2.0.0");
        v3 = libraryRepository.save(v3);

        mockMvc.perform(get(BASE_URL + "/by-name/VersionTest/latest")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryName").value("VersionTest"))
                .andExpect(jsonPath("$.version").value("2.0.0"));
    }

    @Test
    @Order(8)
    @DisplayName("Should get all versions of a library")
    void testGetAllLibraryVersions() throws Exception {
        // Create multiple versions
        libraryRepository.save(new CqlLibrary(TENANT_ID, "MultiVersion", "1.0.0"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "MultiVersion", "1.1.0"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "MultiVersion", "2.0.0"));

        mockMvc.perform(get(BASE_URL + "/by-name/MultiVersion/versions")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].libraryName", everyItem(is("MultiVersion"))));
    }

    @Test
    @Order(9)
    @DisplayName("Should get libraries by status")
    void testGetLibrariesByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-status/ACTIVE")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @Order(10)
    @DisplayName("Should get active libraries")
    void testGetActiveLibraries() throws Exception {
        // Create a draft library
        CqlLibrary draft = new CqlLibrary(TENANT_ID, "DraftLibrary", "1.0.0");
        draft.setStatus("DRAFT");
        libraryRepository.save(draft);

        mockMvc.perform(get(BASE_URL + "/active")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @Order(11)
    @DisplayName("Should search libraries by name")
    void testSearchLibraries() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search")
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", "Diabetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].libraryName", containsString("Diabetes")));
    }

    @Test
    @Order(12)
    @DisplayName("Should update library")
    void testUpdateLibrary() throws Exception {
        CqlLibraryRequest updates = new CqlLibraryRequest("DiabetesScreening", "1.0.0", "library DiabetesScreening version '1.0.0'");
        updates.setDescription("Updated description");
        updates.setStatus("RETIRED");

        String requestBody = objectMapper.writeValueAsString(updates);

        mockMvc.perform(put(BASE_URL + "/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    @Order(13)
    @DisplayName("Should activate library")
    void testActivateLibrary() throws Exception {
        // Create a draft library
        CqlLibrary draft = new CqlLibrary(TENANT_ID, "ToActivate", "1.0.0");
        draft.setStatus("DRAFT");
        draft = libraryRepository.save(draft);

        mockMvc.perform(post(BASE_URL + "/" + draft.getId() + "/activate")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(14)
    @DisplayName("Should retire library")
    void testRetireLibrary() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + testLibrary.getId() + "/retire")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    @Order(15)
    @DisplayName("Should compile library")
    void testCompileLibrary() throws Exception {
        // Create library with CQL content
        CqlLibrary toCompile = new CqlLibrary(TENANT_ID, "CompileTest", "1.0.0");
        toCompile.setCqlContent("library CompileTest version '1.0.0'\ndefine TestExpression: true");
        toCompile = libraryRepository.save(toCompile);

        mockMvc.perform(post(BASE_URL + "/" + toCompile.getId() + "/compile")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to compile library without CQL content")
    void testCompileLibraryWithoutContent() throws Exception {
        CqlLibrary empty = new CqlLibrary(TENANT_ID, "EmptyLibrary", "1.0.0");
        empty = libraryRepository.save(empty);

        mockMvc.perform(post(BASE_URL + "/" + empty.getId() + "/compile")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(17)
    @DisplayName("Should validate library")
    void testValidateLibrary() throws Exception {
        // Create library with CQL content
        CqlLibrary toValidate = new CqlLibrary(TENANT_ID, "ValidateTest", "1.0.0");
        toValidate.setCqlContent("library ValidateTest version '1.0.0'\ndefine TestExpression: true");
        toValidate = libraryRepository.save(toValidate);

        mockMvc.perform(post(BASE_URL + "/" + toValidate.getId() + "/validate")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    @Order(18)
    @DisplayName("Should delete library (soft delete)")
    void testDeleteLibrary() throws Exception {
        CqlLibrary toDelete = new CqlLibrary(TENANT_ID, "ToDelete", "1.0.0");
        toDelete = libraryRepository.save(toDelete);

        mockMvc.perform(delete(BASE_URL + "/" + toDelete.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

        // Verify it's no longer accessible
        mockMvc.perform(get(BASE_URL + "/" + toDelete.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(19)
    @DisplayName("Should count libraries")
    void testCountLibraries() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(20)
    @DisplayName("Should count libraries by status")
    void testCountLibrariesByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count/by-status/ACTIVE")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @Order(21)
    @DisplayName("Should check if library exists")
    void testLibraryExists() throws Exception {
        mockMvc.perform(get(BASE_URL + "/exists")
                .header("X-Tenant-ID", TENANT_ID)
                .param("name", "DiabetesScreening")
                .param("version", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        mockMvc.perform(get(BASE_URL + "/exists")
                .header("X-Tenant-ID", TENANT_ID)
                .param("name", "NonExistent")
                .param("version", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    @Order(22)
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        // Create library for other tenant
        CqlLibrary otherLibrary = new CqlLibrary(otherTenant, "OtherTenantLibrary", "1.0.0");
        otherLibrary = libraryRepository.save(otherLibrary);

        // Try to access other tenant's library
        mockMvc.perform(get(BASE_URL + "/" + otherLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());

        // Verify own library is accessible
        mockMvc.perform(get(BASE_URL + "/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("Should handle library lifecycle transitions")
    void testLibraryLifecycle() throws Exception {
        // Create DRAFT library
        CqlLibraryRequest lifecycle = new CqlLibraryRequest("LifecycleTest", "1.0.0", "library LifecycleTest version '1.0.0'");
        lifecycle.setStatus("DRAFT");
        String requestBody = objectMapper.writeValueAsString(lifecycle);

        String createResponse = mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        CqlLibrary created = objectMapper.readValue(createResponse, CqlLibrary.class);

        // Activate it
        mockMvc.perform(post(BASE_URL + "/" + created.getId() + "/activate")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Retire it
        mockMvc.perform(post(BASE_URL + "/" + created.getId() + "/retire")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    @Order(24)
    @DisplayName("Should handle version comparison")
    void testVersionComparison() throws Exception {
        // Create multiple versions
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionCompare", "1.0.0"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionCompare", "1.0.1"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionCompare", "1.1.0"));
        libraryRepository.save(new CqlLibrary(TENANT_ID, "VersionCompare", "2.0.0"));

        mockMvc.perform(get(BASE_URL + "/by-name/VersionCompare/versions")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].version").value("2.0.0")); // Should be descending
    }

    @Test
    @Order(25)
    @DisplayName("Should handle special characters in library names")
    void testSpecialCharactersInLibraryName() throws Exception {
        CqlLibraryRequest special = new CqlLibraryRequest("Library-With-Dashes_And_Underscores", "1.0.0",
            "library \"Library-With-Dashes_And_Underscores\" version '1.0.0'");
        String requestBody = objectMapper.writeValueAsString(special);

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryName").value("Library-With-Dashes_And_Underscores"));
    }
}
