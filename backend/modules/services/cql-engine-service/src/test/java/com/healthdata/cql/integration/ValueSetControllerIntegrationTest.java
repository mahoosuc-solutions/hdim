package com.healthdata.cql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.dto.ValueSetRequest;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.repository.ValueSetRepository;
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
 * Integration tests for ValueSet Controller
 *
 * Tests all value set management endpoints including:
 * - CRUD operations
 * - Code system filtering (SNOMED, LOINC, RxNorm)
 * - OID-based lookups
 * - Version management
 * - Code membership checks
 * - Multi-tenant isolation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class ValueSetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValueSetRepository valueSetRepository;

    private static final String BASE_URL = "/api/v1/cql/valuesets";
    private static final String TENANT_ID = "test-tenant";
    private static final String DIABETES_OID = "2.16.840.1.113883.3.464.1003.103.12.1001";

    private ValueSet testValueSet;

    @BeforeEach
    void setUp() {
        testValueSet = new ValueSet(TENANT_ID, DIABETES_OID, "Diabetes", "SNOMED");
        testValueSet.setVersion("2023-01");
        testValueSet.setCodes("[\"44054006\", \"73211009\"]");
        testValueSet.setDescription("Diabetes diagnoses");
        testValueSet.setPublisher("NCQA");
        testValueSet.setStatus("ACTIVE");
        testValueSet = valueSetRepository.save(testValueSet);
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new value set")
    void testCreateValueSet() throws Exception {
        ValueSetRequest newValueSet = new ValueSetRequest("2.16.840.1.113883.3.464.1003.198.12.1012",
            "Hypertension", "SNOMED");
        newValueSet.setVersion("2023-01");
        newValueSet.setCodes("[\"38341003\", \"59621000\"]");
        newValueSet.setDescription("Hypertension diagnoses");
        newValueSet.setStatus("ACTIVE");

        String requestBody = objectMapper.writeValueAsString(newValueSet);

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Hypertension"))
                .andExpect(jsonPath("$.codeSystem").value("SNOMED"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    @Order(2)
    @DisplayName("Should get value set by ID")
    void testGetValueSetById() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + testValueSet.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testValueSet.getId().toString()))
                .andExpect(jsonPath("$.name").value("Diabetes"))
                .andExpect(jsonPath("$.codeSystem").value("SNOMED"))
                .andExpect(jsonPath("$.oid").value(DIABETES_OID));
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for non-existent value set")
    void testGetNonExistentValueSet() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("Should get all value sets with pagination")
    void testGetAllValueSets() throws Exception {
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
    @Order(5)
    @DisplayName("Should get value set by OID")
    void testGetValueSetByOid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-oid/" + DIABETES_OID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oid").value(DIABETES_OID))
                .andExpect(jsonPath("$.name").value("Diabetes"));
    }

    @Test
    @Order(6)
    @DisplayName("Should get value set by OID and version")
    void testGetValueSetByOidAndVersion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-oid/" + DIABETES_OID + "/version/2023-01")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oid").value(DIABETES_OID))
                .andExpect(jsonPath("$.version").value("2023-01"));
    }

    @Test
    @Order(7)
    @DisplayName("Should get latest version of value set by OID")
    void testGetLatestValueSetVersion() throws Exception {
        // Create multiple versions
        ValueSet v1 = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.1003.999", "Test", "SNOMED");
        v1.setVersion("2022-01");
        valueSetRepository.save(v1);

        ValueSet v2 = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.1003.999", "Test", "SNOMED");
        v2.setVersion("2023-01");
        valueSetRepository.save(v2);

        mockMvc.perform(get(BASE_URL + "/by-oid/2.16.840.1.113883.3.464.1003.999/latest")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2023-01"));
    }

    @Test
    @Order(8)
    @DisplayName("Should get all versions of a value set")
    void testGetAllValueSetVersions() throws Exception {
        String oid = "2.16.840.1.113883.3.464.1003.888";
        ValueSet vs1 = new ValueSet(TENANT_ID, oid, "MultiVersion", "SNOMED");
        vs1.setVersion("2021-01");
        valueSetRepository.save(vs1);
        ValueSet vs2 = new ValueSet(TENANT_ID, oid, "MultiVersion", "SNOMED");
        vs2.setVersion("2022-01");
        valueSetRepository.save(vs2);
        ValueSet vs3 = new ValueSet(TENANT_ID, oid, "MultiVersion", "SNOMED");
        vs3.setVersion("2023-01");
        valueSetRepository.save(vs3);

        mockMvc.perform(get(BASE_URL + "/by-oid/" + oid + "/versions")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].oid", everyItem(is(oid))));
    }

    @Test
    @Order(9)
    @DisplayName("Should get value set by name")
    void testGetValueSetByName() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-name/Diabetes")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Diabetes"));
    }

    @Test
    @Order(10)
    @DisplayName("Should get value sets by code system - SNOMED")
    void testGetValueSetsByCodeSystemSnomed() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-code-system/SNOMED")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].codeSystem", everyItem(is("SNOMED"))));
    }

    @Test
    @Order(11)
    @DisplayName("Should get SNOMED value sets")
    void testGetSnomedValueSets() throws Exception {
        mockMvc.perform(get(BASE_URL + "/snomed")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].codeSystem", everyItem(is("SNOMED"))));
    }

    @Test
    @Order(12)
    @DisplayName("Should get LOINC value sets")
    void testGetLoincValueSets() throws Exception {
        // Create LOINC value set
        ValueSet loinc = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.1003.198.12.1015",
            "HbA1c Tests", "LOINC");
        loinc.setCodes("[\"4548-4\", \"17856-6\"]");
        valueSetRepository.save(loinc);

        mockMvc.perform(get(BASE_URL + "/loinc")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].codeSystem", everyItem(is("LOINC"))));
    }

    @Test
    @Order(13)
    @DisplayName("Should get RxNorm value sets")
    void testGetRxNormValueSets() throws Exception {
        // Create RxNorm value set
        ValueSet rxnorm = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.1003.196.12.1001",
            "Diabetes Medications", "RxNorm");
        rxnorm.setCodes("[\"860975\", \"860971\"]");
        valueSetRepository.save(rxnorm);

        mockMvc.perform(get(BASE_URL + "/rxnorm")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].codeSystem", everyItem(is("RxNorm"))));
    }

    @Test
    @Order(14)
    @DisplayName("Should get common code system value sets")
    void testGetCommonCodeSystemValueSets() throws Exception {
        // Create value sets for different code systems
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.999.1", "Test1", "SNOMED"));
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.999.2", "Test2", "LOINC"));
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.999.3", "Test3", "RxNorm"));

        mockMvc.perform(get(BASE_URL + "/common")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(3))));
    }

    @Test
    @Order(15)
    @DisplayName("Should get active value sets")
    void testGetActiveValueSets() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @Order(16)
    @DisplayName("Should search value sets by name")
    void testSearchValueSets() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search")
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", "Diabetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name", containsString("Diabetes")));
    }

    @Test
    @Order(17)
    @DisplayName("Should get value sets by OID prefix")
    void testGetValueSetsByOidPrefix() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-oid-prefix/2.16.840.1.113883.3.464")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @Order(18)
    @DisplayName("Should update value set")
    void testUpdateValueSet() throws Exception {
        ValueSetRequest updates = new ValueSetRequest(DIABETES_OID, "Diabetes", "SNOMED");
        updates.setDescription("Updated description");
        updates.setStatus("RETIRED");

        String requestBody = objectMapper.writeValueAsString(updates);

        mockMvc.perform(put(BASE_URL + "/" + testValueSet.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    @Order(19)
    @DisplayName("Should activate value set")
    void testActivateValueSet() throws Exception {
        ValueSet draft = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.777", "ToActivate", "SNOMED");
        draft.setStatus("DRAFT");
        draft = valueSetRepository.save(draft);

        mockMvc.perform(post(BASE_URL + "/" + draft.getId() + "/activate")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(20)
    @DisplayName("Should retire value set")
    void testRetireValueSet() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + testValueSet.getId() + "/retire")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    @Order(21)
    @DisplayName("Should delete value set (soft delete)")
    void testDeleteValueSet() throws Exception {
        ValueSet toDelete = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.888", "ToDelete", "SNOMED");
        toDelete = valueSetRepository.save(toDelete);

        mockMvc.perform(delete(BASE_URL + "/" + toDelete.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

        // Verify it's no longer accessible
        mockMvc.perform(get(BASE_URL + "/" + toDelete.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(22)
    @DisplayName("Should count value sets")
    void testCountValueSets() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(23)
    @DisplayName("Should count value sets by code system")
    void testCountValueSetsByCodeSystem() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count/by-code-system/SNOMED")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @Order(24)
    @DisplayName("Should check if value set exists by OID")
    void testValueSetExistsByOid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/exists/by-oid/" + DIABETES_OID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        mockMvc.perform(get(BASE_URL + "/exists/by-oid/2.16.840.1.999999.999")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    @Order(25)
    @DisplayName("Should check if code exists in value set")
    void testCodeExistsInValueSet() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + testValueSet.getId() + "/contains-code/44054006")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    @Order(26)
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        // Create value set for other tenant
        ValueSet otherValueSet = new ValueSet(otherTenant, "2.16.840.1.113883.3.464.666", "OtherTenant", "SNOMED");
        otherValueSet = valueSetRepository.save(otherValueSet);

        // Try to access other tenant's value set
        mockMvc.perform(get(BASE_URL + "/" + otherValueSet.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());

        // Verify own value set is accessible
        mockMvc.perform(get(BASE_URL + "/" + testValueSet.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(27)
    @DisplayName("Should handle multiple code systems")
    void testMultipleCodeSystems() throws Exception {
        // Create value sets for different code systems
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.111", "ICD10", "ICD-10-CM"));
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.222", "CPT", "CPT"));
        valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.333", "HCPCS", "HCPCS"));

        // Verify all are retrievable
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(3))));
    }
}
