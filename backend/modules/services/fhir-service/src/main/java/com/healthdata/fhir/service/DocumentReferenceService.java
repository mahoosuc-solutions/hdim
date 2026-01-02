package com.healthdata.fhir.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.DocumentReferenceEntity;
import com.healthdata.fhir.persistence.DocumentReferenceRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR DocumentReference resources.
 * Handles clinical document references with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class DocumentReferenceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceService.class);
    private static final String CACHE_NAME = "fhir-document-references";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final DocumentReferenceRepository documentReferenceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new DocumentReference resource
     */
    @Transactional
    public DocumentReference createDocumentReference(String tenantId, DocumentReference docRef, String createdBy) {
        log.debug("Creating document reference for tenant: {}", tenantId);

        // Ensure ID is set
        if (docRef.getId() == null || docRef.getId().isEmpty()) {
            docRef.setId(UUID.randomUUID().toString());
        }

        // Validate patient reference
        UUID patientId = extractPatientId(docRef);
        if (patientId == null) {
            throw new IllegalArgumentException("DocumentReference must have a subject (patient) reference");
        }

        // Convert to entity
        DocumentReferenceEntity entity = toEntity(docRef, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        // Save
        entity = documentReferenceRepository.save(entity);
        log.info("Created document reference: id={}, tenant={}, patient={}, type={}",
                entity.getId(), tenantId, patientId, entity.getTypeCode());

        // Publish event
        publishEvent(entity, "created", createdBy);

        return docRef;
    }

    /**
     * Get DocumentReference by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<DocumentReference> getDocumentReference(String tenantId, UUID id) {
        log.debug("Fetching document reference: tenant={}, id={}", tenantId, id);
        return documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .map(this::toDocumentReference);
    }

    /**
     * Update an existing DocumentReference
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public DocumentReference updateDocumentReference(String tenantId, UUID id, DocumentReference docRef, String updatedBy) {
        log.debug("Updating document reference: tenant={}, id={}", tenantId, id);

        DocumentReferenceEntity existing = documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("DocumentReference not found: " + id));

        // Update entity
        docRef.setId(id.toString());
        DocumentReferenceEntity updated = toEntity(docRef, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setIndexedDate(existing.getIndexedDate());
        updated.setLastModifiedBy(updatedBy);

        documentReferenceRepository.save(updated);
        log.info("Updated document reference: id={}, tenant={}", id, tenantId);

        // Publish event
        publishEvent(updated, "updated", updatedBy);

        return docRef;
    }

    /**
     * Soft delete a DocumentReference
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteDocumentReference(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting document reference: tenant={}, id={}", tenantId, id);

        DocumentReferenceEntity entity = documentReferenceRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("DocumentReference not found: " + id));

        entity.setDeletedAt(Instant.now());
        entity.setLastModifiedBy(deletedBy);
        documentReferenceRepository.save(entity);

        log.info("Deleted document reference: id={}, tenant={}", id, tenantId);
        publishEvent(entity, "deleted", deletedBy);
    }

    /**
     * Get all document references for a patient
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> getDocumentsByPatient(String tenantId, UUID patientId) {
        log.debug("Fetching documents for patient: tenant={}, patient={}", tenantId, patientId);
        return documentReferenceRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenantId, patientId)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    /**
     * Get current (active) document references for a patient
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> getCurrentDocuments(String tenantId, UUID patientId) {
        log.debug("Fetching current documents for patient: tenant={}, patient={}", tenantId, patientId);
        return documentReferenceRepository.findCurrentDocumentsForPatient(tenantId, patientId)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    /**
     * Get document references for an encounter
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> getDocumentsByEncounter(String tenantId, UUID encounterId) {
        log.debug("Fetching documents for encounter: tenant={}, encounter={}", tenantId, encounterId);
        return documentReferenceRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenantId, encounterId)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    /**
     * Get document references by type
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> getDocumentsByType(String tenantId, UUID patientId, String typeCode) {
        log.debug("Fetching documents by type: tenant={}, patient={}, type={}", tenantId, patientId, typeCode);
        return documentReferenceRepository.findByTenantIdAndPatientIdAndTypeCodeAndDeletedAtIsNull(tenantId, patientId, typeCode)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    /**
     * Get latest document of a specific type
     */
    @Transactional(readOnly = true)
    public Optional<DocumentReference> getLatestDocumentByType(String tenantId, UUID patientId, String typeCode) {
        log.debug("Fetching latest document of type: tenant={}, patient={}, type={}", tenantId, patientId, typeCode);
        List<DocumentReferenceEntity> results = documentReferenceRepository.findLatestByType(
                tenantId, patientId, typeCode, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(toDocumentReference(results.get(0)));
    }

    /**
     * Search document references with filters
     */
    @Transactional(readOnly = true)
    public Page<DocumentReference> searchDocuments(String tenantId, UUID patientId, UUID encounterId,
                                                    String status, String typeCode, String categoryCode,
                                                    String contentType, Pageable pageable) {
        log.debug("Searching documents: tenant={}, patient={}", tenantId, patientId);
        return documentReferenceRepository.searchDocuments(tenantId, patientId, encounterId,
                        status, typeCode, categoryCode, contentType, pageable)
                .map(this::toDocumentReference);
    }

    /**
     * Search documents by description text
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> searchByDescription(String tenantId, UUID patientId, String searchTerm) {
        log.debug("Searching documents by description: tenant={}, patient={}, term={}", tenantId, patientId, searchTerm);
        return documentReferenceRepository.searchByDescription(tenantId, patientId, searchTerm)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    /**
     * Get documents created within a date range
     */
    @Transactional(readOnly = true)
    public List<DocumentReference> getDocumentsByDateRange(String tenantId, UUID patientId,
                                                            Instant startDate, Instant endDate) {
        log.debug("Fetching documents by date range: tenant={}, patient={}", tenantId, patientId);
        return documentReferenceRepository.findByCreatedDateRange(tenantId, patientId, startDate, endDate)
                .stream()
                .map(this::toDocumentReference)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private DocumentReferenceEntity toEntity(DocumentReference docRef, String tenantId) {
        UUID id = UUID.fromString(docRef.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(docRef);

        return DocumentReferenceEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceJson(json)
                .patientId(extractPatientId(docRef))
                .encounterId(extractEncounterId(docRef))
                .status(extractStatus(docRef))
                .docStatus(extractDocStatus(docRef))
                .typeCode(extractTypeCode(docRef))
                .typeSystem(extractTypeSystem(docRef))
                .typeDisplay(extractTypeDisplay(docRef))
                .categoryCode(extractCategoryCode(docRef))
                .categoryDisplay(extractCategoryDisplay(docRef))
                .createdDate(extractCreatedDate(docRef))
                .description(extractDescription(docRef))
                .securityLabels(extractSecurityLabels(docRef))
                .contentType(extractContentType(docRef))
                .contentUrl(extractContentUrl(docRef))
                .contentSize(extractContentSize(docRef))
                .contentHash(extractContentHash(docRef))
                .authorReference(extractAuthorReference(docRef))
                .custodianReference(extractCustodianReference(docRef))
                .relatesToCode(extractRelatesToCode(docRef))
                .relatesToTarget(extractRelatesToTarget(docRef))
                .build();
    }

    private DocumentReference toDocumentReference(DocumentReferenceEntity entity) {
        return JSON_PARSER.parseResource(DocumentReference.class, entity.getResourceJson());
    }

    // ==================== Field Extraction Methods ====================

    private UUID extractPatientId(DocumentReference docRef) {
        Reference subject = docRef.getSubject();
        if (subject != null && subject.hasReference()) {
            String ref = subject.getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return UUID.fromString(ref.substring(8));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid patient UUID in document reference: {}", ref);
                }
            }
        }
        return null;
    }

    private UUID extractEncounterId(DocumentReference docRef) {
        if (docRef.hasContext() && docRef.getContext().hasEncounter() &&
            !docRef.getContext().getEncounter().isEmpty()) {
            Reference encounter = docRef.getContext().getEncounterFirstRep();
            if (encounter.hasReference() && encounter.getReference().startsWith("Encounter/")) {
                try {
                    return UUID.fromString(encounter.getReference().substring(10));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encounter UUID in document reference: {}", encounter.getReference());
                }
            }
        }
        return null;
    }

    private String extractStatus(DocumentReference docRef) {
        return docRef.hasStatus() ? docRef.getStatus().toCode() : null;
    }

    private String extractDocStatus(DocumentReference docRef) {
        return docRef.hasDocStatus() ? docRef.getDocStatus().toCode() : null;
    }

    private String extractTypeCode(DocumentReference docRef) {
        if (docRef.hasType()) {
            CodeableConcept type = docRef.getType();
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractTypeSystem(DocumentReference docRef) {
        if (docRef.hasType()) {
            CodeableConcept type = docRef.getType();
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getSystem();
            }
        }
        return null;
    }

    private String extractTypeDisplay(DocumentReference docRef) {
        if (docRef.hasType()) {
            CodeableConcept type = docRef.getType();
            if (type.hasText()) {
                return type.getText();
            }
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractCategoryCode(DocumentReference docRef) {
        if (docRef.hasCategory() && !docRef.getCategory().isEmpty()) {
            CodeableConcept category = docRef.getCategoryFirstRep();
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractCategoryDisplay(DocumentReference docRef) {
        if (docRef.hasCategory() && !docRef.getCategory().isEmpty()) {
            CodeableConcept category = docRef.getCategoryFirstRep();
            if (category.hasText()) {
                return category.getText();
            }
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private Instant extractCreatedDate(DocumentReference docRef) {
        if (docRef.hasDate()) {
            return docRef.getDate().toInstant();
        }
        return null;
    }

    private String extractDescription(DocumentReference docRef) {
        return docRef.hasDescription() ? docRef.getDescription() : null;
    }

    private String extractSecurityLabels(DocumentReference docRef) {
        if (docRef.hasSecurityLabel() && !docRef.getSecurityLabel().isEmpty()) {
            return docRef.getSecurityLabel().stream()
                    .filter(cc -> cc.hasCoding() && !cc.getCoding().isEmpty())
                    .map(cc -> cc.getCodingFirstRep().getCode())
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String extractContentType(DocumentReference docRef) {
        if (docRef.hasContent() && !docRef.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = docRef.getContentFirstRep();
            if (content.hasAttachment()) {
                return content.getAttachment().getContentType();
            }
        }
        return null;
    }

    private String extractContentUrl(DocumentReference docRef) {
        if (docRef.hasContent() && !docRef.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = docRef.getContentFirstRep();
            if (content.hasAttachment()) {
                return content.getAttachment().getUrl();
            }
        }
        return null;
    }

    private Long extractContentSize(DocumentReference docRef) {
        if (docRef.hasContent() && !docRef.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = docRef.getContentFirstRep();
            if (content.hasAttachment() && content.getAttachment().hasSize()) {
                return (long) content.getAttachment().getSize();
            }
        }
        return null;
    }

    private String extractContentHash(DocumentReference docRef) {
        if (docRef.hasContent() && !docRef.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = docRef.getContentFirstRep();
            if (content.hasAttachment() && content.getAttachment().hasHash()) {
                return java.util.Base64.getEncoder().encodeToString(content.getAttachment().getHash());
            }
        }
        return null;
    }

    private String extractAuthorReference(DocumentReference docRef) {
        if (docRef.hasAuthor() && !docRef.getAuthor().isEmpty()) {
            return docRef.getAuthorFirstRep().getReference();
        }
        return null;
    }

    private String extractCustodianReference(DocumentReference docRef) {
        if (docRef.hasCustodian()) {
            return docRef.getCustodian().getReference();
        }
        return null;
    }

    private String extractRelatesToCode(DocumentReference docRef) {
        if (docRef.hasRelatesTo() && !docRef.getRelatesTo().isEmpty()) {
            return docRef.getRelatesToFirstRep().getCode().toCode();
        }
        return null;
    }

    private String extractRelatesToTarget(DocumentReference docRef) {
        if (docRef.hasRelatesTo() && !docRef.getRelatesTo().isEmpty()) {
            Reference target = docRef.getRelatesToFirstRep().getTarget();
            return target != null ? target.getReference() : null;
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(DocumentReferenceEntity entity, String eventType, String actor) {
        try {
            DocumentReferenceEvent event = new DocumentReferenceEvent(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getPatientId() != null ? entity.getPatientId().toString() : null,
                    entity.getTypeCode(),
                    eventType,
                    Instant.now(),
                    actor
            );
            kafkaTemplate.send("fhir.document-references." + eventType, entity.getId().toString(), event);
            log.debug("Published document reference event: type={}, id={}", eventType, entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish document reference event: type={}, id={}", eventType, entity.getId(), e);
        }
    }

    /**
     * Event record for Kafka publishing
     */
    public record DocumentReferenceEvent(
            String id,
            String tenantId,
            String patientId,
            String documentType,
            String eventType,
            Instant occurredAt,
            String actor
    ) {}
}
