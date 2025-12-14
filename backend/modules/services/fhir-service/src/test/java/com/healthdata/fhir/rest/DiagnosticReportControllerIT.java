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

import com.healthdata.fhir.persistence.DiagnosticReportRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for DiagnosticReportController.
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
class DiagnosticReportControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String H2_URL = "jdbc:h2:mem:healthdata_fhir_diagreport_controller;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DiagnosticReportRepository diagnosticReportRepository;

    @Autowired
    private PatientRepository patientRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @BeforeEach
    void setUp() {
        diagnosticReportRepository.deleteAll();
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
    void createDiagnosticReportShouldReturn201() throws Exception {
        DiagnosticReport report = createValidDiagnosticReport();
        String reportJson = jsonParser.encodeResourceToString(report);

        MvcResult result = mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        DiagnosticReport created = jsonParser.parseResource(DiagnosticReport.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getStatus()).isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);
    }

    @Test
    void getDiagnosticReportShouldReturn200() throws Exception {
        DiagnosticReport report = createValidDiagnosticReport();
        String reportJson = jsonParser.encodeResourceToString(report);

        MvcResult createResult = mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andReturn();

        DiagnosticReport created = jsonParser.parseResource(DiagnosticReport.class, createResult.getResponse().getContentAsString());
        String reportId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/DiagnosticReport/{id}", reportId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andExpect(jsonPath("$.id").value(reportId))
            .andExpect(jsonPath("$.status").value("final"));
    }

    @Test
    void getDiagnosticReportNotFoundShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/fhir/DiagnosticReport/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateDiagnosticReportShouldReturn200() throws Exception {
        DiagnosticReport report = createValidDiagnosticReport();
        String reportJson = jsonParser.encodeResourceToString(report);

        MvcResult createResult = mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andReturn();

        DiagnosticReport created = jsonParser.parseResource(DiagnosticReport.class, createResult.getResponse().getContentAsString());
        String reportId = created.getIdElement().getIdPart();

        created.setStatus(DiagnosticReport.DiagnosticReportStatus.AMENDED);
        created.setConclusion("Updated conclusion with additional findings");
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/fhir/DiagnosticReport/{id}", reportId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("amended"));
    }

    @Test
    void deleteDiagnosticReportShouldReturn204() throws Exception {
        DiagnosticReport report = createValidDiagnosticReport();
        String reportJson = jsonParser.encodeResourceToString(report);

        MvcResult createResult = mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andReturn();

        DiagnosticReport created = jsonParser.parseResource(DiagnosticReport.class, createResult.getResponse().getContentAsString());
        String reportId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/fhir/DiagnosticReport/{id}", reportId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/DiagnosticReport/{id}", reportId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        DiagnosticReport report1 = createValidDiagnosticReport();
        DiagnosticReport report2 = createValidDiagnosticReport();
        report2.setId(UUID.randomUUID().toString());
        report2.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("24323-8")
                        .setDisplay("Comprehensive metabolic panel")));

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(report1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(report2)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/DiagnosticReport")
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
        DiagnosticReport finalReport = createValidDiagnosticReport();
        DiagnosticReport preliminaryReport = createValidDiagnosticReport();
        preliminaryReport.setId(UUID.randomUUID().toString());
        preliminaryReport.setStatus(DiagnosticReport.DiagnosticReportStatus.PRELIMINARY);

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(finalReport)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(preliminaryReport)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("status", "final"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void filterByCategoryShouldReturnMatching() throws Exception {
        DiagnosticReport labReport = createValidDiagnosticReport();
        DiagnosticReport imagingReport = createValidDiagnosticReport();
        imagingReport.setId(UUID.randomUUID().toString());
        imagingReport.getCategory().clear();
        imagingReport.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0074")
                        .setCode("RAD")
                        .setDisplay("Radiology")));

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(labReport)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(imagingReport)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("category", "LAB"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        DiagnosticReport report = createValidDiagnosticReport();
        String reportJson = jsonParser.encodeResourceToString(report);

        MvcResult createResult = mockMvc.perform(post("/fhir/DiagnosticReport")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andReturn();

        DiagnosticReport created = jsonParser.parseResource(DiagnosticReport.class, createResult.getResponse().getContentAsString());
        String reportId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/DiagnosticReport/{id}", reportId)
                .header("X-Tenant-ID", "different-tenant"))
            .andExpect(status().isNotFound());
    }

    private DiagnosticReport createValidDiagnosticReport() {
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        report.setSubject(new Reference("Patient/" + PATIENT_ID));
        report.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("58410-2")
                        .setDisplay("Complete Blood Count")));
        report.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0074")
                        .setCode("LAB")
                        .setDisplay("Laboratory")));
        report.setEffective(new DateTimeType(new Date()));
        report.setIssued(new Date());
        report.setConclusion("All values within normal limits");
        report.addPerformer(new Reference("Organization/lab-1").setDisplay("Main Lab"));
        return report;
    }
}
