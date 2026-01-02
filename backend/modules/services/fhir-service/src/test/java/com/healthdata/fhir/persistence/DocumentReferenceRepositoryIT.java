package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for DocumentReferenceRepository.
 * Tests all custom query methods for finding and tracking clinical documents.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class DocumentReferenceRepositoryIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ENCOUNTER_ID = UUID.randomUUID();
    private static final String TYPE_DISCHARGE = "34133-9";
    private static final String TYPE_PROGRESS = "11506-3";

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private DocumentReferenceRepository documentReferenceRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("John")
                .lastName("Doe")
                .gender("male")
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();
        patientRepository.save(patient);
    }

    @Test
    void shouldPersistAndRetrieveDocument() {
        // Given
        DocumentReferenceEntity entity = createDocument("current", TYPE_DISCHARGE, "Discharge Summary", Instant.now());

        // When
        DocumentReferenceEntity saved = documentReferenceRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        DocumentReferenceEntity found = documentReferenceRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTypeCode()).isEqualTo(TYPE_DISCHARGE);
        assertThat(found.getStatus()).isEqualTo("current");
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createDocument("current", TYPE_DISCHARGE, "Discharge Summary", Instant.now().minusSeconds(60 * 60));
        createDocument("current", TYPE_PROGRESS, "Progress Note", Instant.now());

        // When
        List<DocumentReferenceEntity> results = documentReferenceRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTypeCode()).isEqualTo(TYPE_PROGRESS); // Most recent first
    }

    @Test
    void shouldFindCurrentDocuments() {
        // Given
        createDocument("current", TYPE_DISCHARGE, "Current doc", Instant.now());
        createDocument("superseded", TYPE_PROGRESS, "Old doc", Instant.now());
        createDocument("current", TYPE_PROGRESS, "Another current", Instant.now());

        // When
        List<DocumentReferenceEntity> current = documentReferenceRepository.findCurrentDocumentsForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(current).hasSize(2);
        assertThat(current).allMatch(d -> d.getStatus().equals("current"));
    }

    @Test
    void shouldFindByEncounter() {
        // Given
        DocumentReferenceEntity withEncounter = createDocument("current", TYPE_DISCHARGE, "With encounter", Instant.now());
        withEncounter.setEncounterId(ENCOUNTER_ID);
        documentReferenceRepository.save(withEncounter);

        createDocument("current", TYPE_PROGRESS, "Without encounter", Instant.now());

        // When
        List<DocumentReferenceEntity> results = documentReferenceRepository
                .findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEncounterId()).isEqualTo(ENCOUNTER_ID);
    }

    @Test
    void shouldFindByType() {
        // Given
        createDocument("current", TYPE_DISCHARGE, "Discharge 1", Instant.now());
        createDocument("current", TYPE_DISCHARGE, "Discharge 2", Instant.now());
        createDocument("current", TYPE_PROGRESS, "Progress Note", Instant.now());

        // When
        List<DocumentReferenceEntity> discharges = documentReferenceRepository
                .findByTenantIdAndPatientIdAndTypeCodeAndDeletedAtIsNull(TENANT_ID, PATIENT_ID, TYPE_DISCHARGE);

        // Then
        assertThat(discharges).hasSize(2);
        assertThat(discharges).allMatch(d -> d.getTypeCode().equals(TYPE_DISCHARGE));
    }

    @Test
    void shouldFindLatestByType() {
        // Given
        createDocument("current", TYPE_DISCHARGE, "Old discharge", Instant.now().minusSeconds(2 * 60 * 60));
        createDocument("current", TYPE_DISCHARGE, "Newer discharge", Instant.now().minusSeconds(60 * 60));
        createDocument("current", TYPE_DISCHARGE, "Latest discharge", Instant.now());

        // When
        List<DocumentReferenceEntity> latest = documentReferenceRepository
                .findLatestByType(TENANT_ID, PATIENT_ID, TYPE_DISCHARGE, PageRequest.of(0, 1));

        // Then
        assertThat(latest).hasSize(1);
        assertThat(latest.get(0).getDescription()).isEqualTo("Latest discharge");
    }

    @Test
    void shouldSearchByDescription() {
        // Given
        createDocument("current", TYPE_DISCHARGE, "Cardiac discharge summary", Instant.now());
        createDocument("current", TYPE_PROGRESS, "Regular checkup note", Instant.now());
        createDocument("current", TYPE_DISCHARGE, "Cardiac catheterization report", Instant.now());

        // When
        List<DocumentReferenceEntity> cardiacDocs = documentReferenceRepository
                .searchByDescription(TENANT_ID, PATIENT_ID, "cardiac");

        // Then
        assertThat(cardiacDocs).hasSize(2);
    }

    @Test
    void shouldFindByCreatedDateRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(45 * 24 * 60 * 60);
        Instant endDate = Instant.now().minusSeconds(15 * 24 * 60 * 60);

        createDocument("current", TYPE_DISCHARGE, "Old doc", Instant.now().minusSeconds(60 * 24 * 60 * 60));
        createDocument("current", TYPE_PROGRESS, "In range", Instant.now().minusSeconds(30 * 24 * 60 * 60));
        createDocument("current", TYPE_DISCHARGE, "Recent doc", Instant.now().minusSeconds(10 * 24 * 60 * 60));

        // When
        List<DocumentReferenceEntity> inRange = documentReferenceRepository
                .findByCreatedDateRange(TENANT_ID, PATIENT_ID, startDate, endDate);

        // Then
        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getDescription()).isEqualTo("In range");
    }

    @Test
    void shouldFindByCategory() {
        // Given
        DocumentReferenceEntity clinicalNote = createDocument("current", TYPE_PROGRESS, "Clinical note", Instant.now());
        clinicalNote.setCategoryCode("clinical-note");
        documentReferenceRepository.save(clinicalNote);

        DocumentReferenceEntity labResult = createDocument("current", TYPE_DISCHARGE, "Lab result", Instant.now());
        labResult.setCategoryCode("lab-result");
        documentReferenceRepository.save(labResult);

        // When
        List<DocumentReferenceEntity> clinicalNotes = documentReferenceRepository
                .findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(TENANT_ID, PATIENT_ID, "clinical-note");

        // Then
        assertThat(clinicalNotes).hasSize(1);
        assertThat(clinicalNotes.get(0).getCategoryCode()).isEqualTo("clinical-note");
    }

    @Test
    void shouldHandleMultiTenantIsolation() {
        // Given
        String tenant2 = "tenant-2";
        UUID patient2 = UUID.randomUUID();

        PatientEntity patient = PatientEntity.builder()
                .id(patient2)
                .tenantId(tenant2)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + patient2 + "\"}")
                .firstName("Jane")
                .lastName("Smith")
                .gender("female")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        patientRepository.save(patient);

        createDocument("current", TYPE_DISCHARGE, "Tenant 1 doc", Instant.now());

        UUID tenant2DocId = UUID.randomUUID();
        DocumentReferenceEntity tenant2Doc = DocumentReferenceEntity.builder()
                .id(tenant2DocId)
                .tenantId(tenant2)
                .patientId(patient2)
                .resourceJson("{\"resourceType\":\"DocumentReference\",\"id\":\"" + tenant2DocId + "\"}")
                .status("current")
                .typeCode(TYPE_DISCHARGE)
                .typeDisplay("Summary of episode note")
                .description("Tenant 2 doc")
                .createdDate(Instant.now())
                .indexedDate(Instant.now())
                .build();
        documentReferenceRepository.save(tenant2Doc);

        // When
        List<DocumentReferenceEntity> tenant1Results = documentReferenceRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, PATIENT_ID);
        List<DocumentReferenceEntity> tenant2Results = documentReferenceRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
    }

    private DocumentReferenceEntity createDocument(String status, String typeCode, String description, Instant createdDate) {
        UUID docId = UUID.randomUUID();
        DocumentReferenceEntity entity = DocumentReferenceEntity.builder()
                .id(docId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .resourceJson("{\"resourceType\":\"DocumentReference\",\"id\":\"" + docId + "\"}")
                .status(status)
                .docStatus("final")
                .typeCode(typeCode)
                .typeDisplay(typeCode.equals(TYPE_DISCHARGE) ? "Summary of episode note" : "Progress note")
                .categoryCode("clinical-note")
                .categoryDisplay("Clinical Note")
                .createdDate(createdDate)
                .indexedDate(createdDate)
                .description(description)
                .contentType("application/pdf")
                .contentUrl("http://example.org/docs/" + UUID.randomUUID() + ".pdf")
                .build();

        return documentReferenceRepository.save(entity);
    }
}
