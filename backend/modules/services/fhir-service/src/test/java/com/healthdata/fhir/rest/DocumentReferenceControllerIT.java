package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.DocumentReferenceRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for DocumentReferenceController.
 * Tests REST API endpoints with full Spring context.
 */
@SpringBootTest(
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    },
    classes = {
        com.healthdata.fhir.FhirServiceApplication.class,
        com.healthdata.fhir.config.TestCacheConfiguration.class,
        com.healthdata.fhir.config.TestSecurityConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@ExtendWith(SpringExtension.class)
@WithMockUser(username = "testuser")
class DocumentReferenceControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentReferenceRepository documentReferenceRepository;

    @Autowired
    private PatientRepository patientRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @BeforeEach
    void setUp() {
        documentReferenceRepository.deleteAll();
        patientRepository.deleteAll();

        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("Test")
                .lastName("Patient")
                .birthDate(LocalDate.of(1980, 1, 1))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        patientRepository.save(patient);

        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    void createDocumentReferenceShouldReturn201() throws Exception {
        DocumentReference docRef = createValidDocumentReference();
        String docRefJson = jsonParser.encodeResourceToString(docRef);

        MvcResult result = mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(docRefJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        DocumentReference created = jsonParser.parseResource(DocumentReference.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getStatus()).isEqualTo(Enumerations.DocumentReferenceStatus.CURRENT);
    }

    @Test
    void getDocumentReferenceShouldReturn200() throws Exception {
        DocumentReference docRef = createValidDocumentReference();
        String docRefJson = jsonParser.encodeResourceToString(docRef);

        MvcResult createResult = mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(docRefJson))
            .andExpect(status().isCreated())
            .andReturn();

        DocumentReference created = jsonParser.parseResource(DocumentReference.class, createResult.getResponse().getContentAsString());
        String docRefId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/DocumentReference/{id}", docRefId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andExpect(jsonPath("$.id").value(docRefId))
            .andExpect(jsonPath("$.status").value("current"));
    }

    @Test
    void getDocumentReferenceNotFoundShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/fhir/DocumentReference/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateDocumentReferenceShouldReturn200() throws Exception {
        DocumentReference docRef = createValidDocumentReference();
        String docRefJson = jsonParser.encodeResourceToString(docRef);

        MvcResult createResult = mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(docRefJson))
            .andExpect(status().isCreated())
            .andReturn();

        DocumentReference created = jsonParser.parseResource(DocumentReference.class, createResult.getResponse().getContentAsString());
        String docRefId = created.getIdElement().getIdPart();

        created.setStatus(Enumerations.DocumentReferenceStatus.SUPERSEDED);
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/fhir/DocumentReference/{id}", docRefId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("superseded"));
    }

    @Test
    void deleteDocumentReferenceShouldReturn204() throws Exception {
        DocumentReference docRef = createValidDocumentReference();
        String docRefJson = jsonParser.encodeResourceToString(docRef);

        MvcResult createResult = mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(docRefJson))
            .andExpect(status().isCreated())
            .andReturn();

        DocumentReference created = jsonParser.parseResource(DocumentReference.class, createResult.getResponse().getContentAsString());
        String docRefId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/fhir/DocumentReference/{id}", docRefId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/DocumentReference/{id}", docRefId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        DocumentReference docRef1 = createValidDocumentReference();
        DocumentReference docRef2 = createValidDocumentReference();
        docRef2.setId(UUID.randomUUID().toString());
        docRef2.setDescription("Lab Results Report");

        mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(docRef1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(docRef2)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void filterByStatusShouldReturnMatching() throws Exception {
        DocumentReference currentDoc = createValidDocumentReference();
        DocumentReference supersededDoc = createValidDocumentReference();
        supersededDoc.setId(UUID.randomUUID().toString());
        supersededDoc.setStatus(Enumerations.DocumentReferenceStatus.SUPERSEDED);

        mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(currentDoc)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(supersededDoc)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("status", "current"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        DocumentReference docRef = createValidDocumentReference();
        String docRefJson = jsonParser.encodeResourceToString(docRef);

        MvcResult createResult = mockMvc.perform(post("/fhir/DocumentReference")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(docRefJson))
            .andExpect(status().isCreated())
            .andReturn();

        DocumentReference created = jsonParser.parseResource(DocumentReference.class, createResult.getResponse().getContentAsString());
        String docRefId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/DocumentReference/{id}", docRefId)
                .header("X-Tenant-ID", "different-tenant"))
            .andExpect(status().isNotFound());
    }

    private DocumentReference createValidDocumentReference() {
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        docRef.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        docRef.setDocStatus(DocumentReference.ReferredDocumentStatus.FINAL);
        docRef.setSubject(new Reference("Patient/" + PATIENT_ID));
        docRef.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("34133-9")
                        .setDisplay("Summary of episode note")));
        docRef.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/us/core/CodeSystem/us-core-documentreference-category")
                        .setCode("clinical-note")
                        .setDisplay("Clinical Note")));
        docRef.setDate(new Date());
        docRef.setDescription("Patient discharge summary");
        docRef.addContent()
                .setAttachment(new Attachment()
                        .setContentType("application/pdf")
                        .setUrl("http://example.org/documents/doc1.pdf")
                        .setTitle("Discharge Summary")
                        .setSize(12345));
        return docRef;
    }
}
