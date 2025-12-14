package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.DocumentReferenceEntity;
import com.healthdata.fhir.persistence.DocumentReferenceRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for DocumentReferenceService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class DocumentReferenceServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final UUID DOC_REF_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ENCOUNTER_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    private static final String TYPE_CODE = "34133-9";
    private static final String TYPE_DISPLAY = "Summary of episode note";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private DocumentReferenceRepository documentReferenceRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private DocumentReferenceService documentReferenceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentReferenceService = new DocumentReferenceService(documentReferenceRepository, kafkaTemplate);
    }

    @Test
    void createDocumentReferenceShouldPersistAndPublishEvent() {
        // Given
        DocumentReference docRef = createFhirDocumentReference();
        DocumentReferenceEntity savedEntity = createDocumentReferenceEntity();

        when(documentReferenceRepository.save(any(DocumentReferenceEntity.class))).thenReturn(savedEntity);

        // When
        DocumentReference result = documentReferenceService.createDocumentReference(TENANT, docRef, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(DOC_REF_ID.toString());

        verify(documentReferenceRepository).save(any(DocumentReferenceEntity.class));
        verify(kafkaTemplate).send(eq("fhir.document-references.created"), eq(DOC_REF_ID.toString()), any());
    }

    @Test
    void createDocumentReferenceShouldAssignIdIfNotPresent() {
        // Given
        DocumentReference docRef = new DocumentReference();
        docRef.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        docRef.setSubject(new Reference("Patient/" + PATIENT_ID));
        docRef.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode(TYPE_CODE)
                        .setDisplay(TYPE_DISPLAY)));

        DocumentReferenceEntity savedEntity = DocumentReferenceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("current")
                .typeCode(TYPE_CODE)
                .build();

        when(documentReferenceRepository.save(any(DocumentReferenceEntity.class))).thenReturn(savedEntity);

        // When
        DocumentReference result = documentReferenceService.createDocumentReference(TENANT, docRef, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(documentReferenceRepository).save(any(DocumentReferenceEntity.class));
    }

    @Test
    void createDocumentReferenceShouldRejectMissingSubject() {
        // Given
        DocumentReference docRef = new DocumentReference();
        docRef.setId(DOC_REF_ID.toString());
        docRef.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        // No subject set

        // When/Then
        assertThatThrownBy(() -> documentReferenceService.createDocumentReference(TENANT, docRef, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void getDocumentReferenceShouldReturnFhirResource() {
        // Given
        DocumentReferenceEntity entity = createDocumentReferenceEntityWithJson();

        when(documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, DOC_REF_ID))
                .thenReturn(Optional.of(entity));

        // When
        Optional<DocumentReference> result = documentReferenceService.getDocumentReference(TENANT, DOC_REF_ID);

        // Then
        assertThat(result).isPresent();
        DocumentReference docRef = result.get();
        assertThat(docRef.getIdElement().getIdPart()).isEqualTo(DOC_REF_ID.toString());
        assertThat(docRef.getStatus().toCode()).isEqualTo("current");
    }

    @Test
    void getDocumentReferenceShouldReturnEmptyWhenNotFound() {
        // Given
        when(documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, DOC_REF_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<DocumentReference> result = documentReferenceService.getDocumentReference(TENANT, DOC_REF_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateDocumentReferenceShouldUpdateAndPublishEvent() {
        // Given
        DocumentReferenceEntity existingEntity = createDocumentReferenceEntityWithJson();
        DocumentReference updatedDocRef = createFhirDocumentReference();
        updatedDocRef.setStatus(Enumerations.DocumentReferenceStatus.SUPERSEDED);

        when(documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, DOC_REF_ID))
                .thenReturn(Optional.of(existingEntity));
        when(documentReferenceRepository.save(any(DocumentReferenceEntity.class))).thenReturn(existingEntity);

        // When
        DocumentReference result = documentReferenceService.updateDocumentReference(TENANT, DOC_REF_ID, updatedDocRef, "user-2");

        // Then
        assertThat(result).isNotNull();
        verify(documentReferenceRepository).save(any(DocumentReferenceEntity.class));
        verify(kafkaTemplate).send(eq("fhir.document-references.updated"), eq(DOC_REF_ID.toString()), any());
    }

    @Test
    void updateDocumentReferenceShouldThrowWhenNotFound() {
        // Given
        DocumentReference docRef = createFhirDocumentReference();
        when(documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, DOC_REF_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentReferenceService.updateDocumentReference(TENANT, DOC_REF_ID, docRef, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteDocumentReferenceShouldSoftDeleteAndPublishEvent() {
        // Given
        DocumentReferenceEntity entity = createDocumentReferenceEntityWithJson();
        when(documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, DOC_REF_ID))
                .thenReturn(Optional.of(entity));
        when(documentReferenceRepository.save(any(DocumentReferenceEntity.class))).thenReturn(entity);

        // When
        documentReferenceService.deleteDocumentReference(TENANT, DOC_REF_ID, "user-3");

        // Then
        ArgumentCaptor<DocumentReferenceEntity> captor = ArgumentCaptor.forClass(DocumentReferenceEntity.class);
        verify(documentReferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("fhir.document-references.deleted"), eq(DOC_REF_ID.toString()), any());
    }

    @Test
    void getDocumentsByPatientShouldReturnList() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(
                createDocumentReferenceEntityWithJson(),
                createDocumentReferenceEntityWithJson()
        );

        when(documentReferenceRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.getDocumentsByPatient(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void getCurrentDocumentsShouldReturnOnlyCurrent() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.findCurrentDocumentsForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.getCurrentDocuments(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getDocumentsByEncounterShouldReturnList() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT, ENCOUNTER_ID))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.getDocumentsByEncounter(TENANT, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getDocumentsByTypeShouldReturnFilteredList() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.findByTenantIdAndPatientIdAndTypeCodeAndDeletedAtIsNull(TENANT, PATIENT_ID, TYPE_CODE))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.getDocumentsByType(TENANT, PATIENT_ID, TYPE_CODE);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getLatestDocumentByTypeShouldReturnOptional() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.findLatestByType(eq(TENANT), eq(PATIENT_ID), eq(TYPE_CODE), any(PageRequest.class)))
                .thenReturn(entities);

        // When
        Optional<DocumentReference> result = documentReferenceService.getLatestDocumentByType(TENANT, PATIENT_ID, TYPE_CODE);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void searchDocumentsShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());
        Page<DocumentReferenceEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(documentReferenceRepository.searchDocuments(eq(TENANT), eq(PATIENT_ID), any(), eq("current"), any(), any(), any(), eq(pageable)))
                .thenReturn(entityPage);

        // When
        Page<DocumentReference> results = documentReferenceService.searchDocuments(
                TENANT, PATIENT_ID, null, "current", null, null, null, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchByDescriptionShouldReturnMatchingDocuments() {
        // Given
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.searchByDescription(TENANT, PATIENT_ID, "summary"))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.searchByDescription(TENANT, PATIENT_ID, "summary");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getDocumentsByDateRangeShouldReturnDocumentsInRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        Instant endDate = Instant.now();
        List<DocumentReferenceEntity> entities = List.of(createDocumentReferenceEntityWithJson());

        when(documentReferenceRepository.findByCreatedDateRange(TENANT, PATIENT_ID, startDate, endDate))
                .thenReturn(entities);

        // When
        List<DocumentReference> results = documentReferenceService.getDocumentsByDateRange(TENANT, PATIENT_ID, startDate, endDate);

        // Then
        assertThat(results).hasSize(1);
    }

    // Helper methods

    private DocumentReference createFhirDocumentReference() {
        DocumentReference docRef = new DocumentReference();
        docRef.setId(DOC_REF_ID.toString());
        docRef.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        docRef.setDocStatus(DocumentReference.ReferredDocumentStatus.FINAL);
        docRef.setSubject(new Reference("Patient/" + PATIENT_ID));
        docRef.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode(TYPE_CODE)
                        .setDisplay(TYPE_DISPLAY)));
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

    private DocumentReferenceEntity createDocumentReferenceEntity() {
        return DocumentReferenceEntity.builder()
                .id(DOC_REF_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("current")
                .docStatus("final")
                .typeCode(TYPE_CODE)
                .typeDisplay(TYPE_DISPLAY)
                .categoryCode("clinical-note")
                .categoryDisplay("Clinical Note")
                .createdDate(Instant.now())
                .indexedDate(Instant.now())
                .description("Patient discharge summary")
                .contentType("application/pdf")
                .contentUrl("http://example.org/documents/doc1.pdf")
                .contentSize(12345L)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private DocumentReferenceEntity createDocumentReferenceEntityWithJson() {
        DocumentReference docRef = createFhirDocumentReference();
        String json = JSON_PARSER.encodeResourceToString(docRef);

        return DocumentReferenceEntity.builder()
                .id(DOC_REF_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .resourceJson(json)
                .status("current")
                .docStatus("final")
                .typeCode(TYPE_CODE)
                .typeDisplay(TYPE_DISPLAY)
                .categoryCode("clinical-note")
                .categoryDisplay("Clinical Note")
                .createdDate(Instant.now())
                .indexedDate(Instant.now())
                .description("Patient discharge summary")
                .contentType("application/pdf")
                .contentUrl("http://example.org/documents/doc1.pdf")
                .contentSize(12345L)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
