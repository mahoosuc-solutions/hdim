package com.healthdata.fhir.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
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

import com.healthdata.fhir.persistence.DiagnosticReportEntity;
import com.healthdata.fhir.persistence.DiagnosticReportRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR DiagnosticReport resources.
 * Handles lab reports, imaging results, and other diagnostic data with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class DiagnosticReportService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticReportService.class);
    private static final String CACHE_NAME = "fhir-diagnostic-reports";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new DiagnosticReport resource
     */
    @Transactional
    public DiagnosticReport createDiagnosticReport(String tenantId, DiagnosticReport report, String createdBy) {
        log.debug("Creating diagnostic report for tenant: {}", tenantId);

        // Ensure ID is set
        if (report.getId() == null || report.getId().isEmpty()) {
            report.setId(UUID.randomUUID().toString());
        }

        // Validate patient reference
        UUID patientId = extractPatientId(report);
        if (patientId == null) {
            throw new IllegalArgumentException("DiagnosticReport must have a subject (patient) reference");
        }

        // Convert to entity
        DiagnosticReportEntity entity = toEntity(report, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        // Save
        entity = diagnosticReportRepository.save(entity);
        log.info("Created diagnostic report: id={}, tenant={}, patient={}, code={}",
                entity.getId(), tenantId, patientId, entity.getCode());

        // Publish event
        publishEvent(entity, "created", createdBy);

        return report;
    }

    /**
     * Get DiagnosticReport by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<DiagnosticReport> getDiagnosticReport(String tenantId, UUID id) {
        log.debug("Fetching diagnostic report: tenant={}, id={}", tenantId, id);
        return diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .map(this::toDiagnosticReport);
    }

    /**
     * Update an existing DiagnosticReport
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public DiagnosticReport updateDiagnosticReport(String tenantId, UUID id, DiagnosticReport report, String updatedBy) {
        log.debug("Updating diagnostic report: tenant={}, id={}", tenantId, id);

        DiagnosticReportEntity existing = diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("DiagnosticReport not found: " + id));

        // Update entity
        report.setId(id.toString());
        DiagnosticReportEntity updated = toEntity(report, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        diagnosticReportRepository.save(updated);
        log.info("Updated diagnostic report: id={}, tenant={}", id, tenantId);

        // Publish event
        publishEvent(updated, "updated", updatedBy);

        return report;
    }

    /**
     * Soft delete a DiagnosticReport
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteDiagnosticReport(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting diagnostic report: tenant={}, id={}", tenantId, id);

        DiagnosticReportEntity entity = diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("DiagnosticReport not found: " + id));

        entity.setDeletedAt(Instant.now());
        entity.setLastModifiedBy(deletedBy);
        diagnosticReportRepository.save(entity);

        log.info("Deleted diagnostic report: id={}, tenant={}", id, tenantId);
        publishEvent(entity, "deleted", deletedBy);
    }

    /**
     * Get all diagnostic reports for a patient
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getReportsByPatient(String tenantId, UUID patientId) {
        log.debug("Fetching reports for patient: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(tenantId, patientId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get final diagnostic reports for a patient
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getFinalReports(String tenantId, UUID patientId) {
        log.debug("Fetching final reports for patient: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findFinalReportsForPatient(tenantId, patientId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get diagnostic reports for an encounter
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getReportsByEncounter(String tenantId, UUID encounterId) {
        log.debug("Fetching reports for encounter: tenant={}, encounter={}", tenantId, encounterId);
        return diagnosticReportRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(tenantId, encounterId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get lab reports for a patient
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getLabReports(String tenantId, UUID patientId) {
        log.debug("Fetching lab reports for patient: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findLabReportsForPatient(tenantId, patientId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get imaging reports for a patient
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getImagingReports(String tenantId, UUID patientId) {
        log.debug("Fetching imaging reports for patient: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findImagingReportsForPatient(tenantId, patientId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get pending/preliminary reports
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getPendingReports(String tenantId, UUID patientId) {
        log.debug("Fetching pending reports for patient: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findPendingReports(tenantId, patientId)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    /**
     * Get latest report of a specific type
     */
    @Transactional(readOnly = true)
    public Optional<DiagnosticReport> getLatestReportByCode(String tenantId, UUID patientId, String code) {
        log.debug("Fetching latest report of code: tenant={}, patient={}, code={}", tenantId, patientId, code);
        List<DiagnosticReportEntity> results = diagnosticReportRepository.findLatestByCode(
                tenantId, patientId, code, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(toDiagnosticReport(results.get(0)));
    }

    /**
     * Search diagnostic reports with filters
     */
    @Transactional(readOnly = true)
    public Page<DiagnosticReport> searchReports(String tenantId, UUID patientId, UUID encounterId,
                                                 String status, String code, String categoryCode,
                                                 Pageable pageable) {
        log.debug("Searching reports: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.searchReports(tenantId, patientId, encounterId,
                        status, code, categoryCode, pageable)
                .map(this::toDiagnosticReport);
    }

    /**
     * Get reports by date range
     */
    @Transactional(readOnly = true)
    public List<DiagnosticReport> getReportsByDateRange(String tenantId, UUID patientId,
                                                         Instant startDate, Instant endDate) {
        log.debug("Fetching reports by date range: tenant={}, patient={}", tenantId, patientId);
        return diagnosticReportRepository.findByEffectiveDateRange(tenantId, patientId, startDate, endDate)
                .stream()
                .map(this::toDiagnosticReport)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private DiagnosticReportEntity toEntity(DiagnosticReport report, String tenantId) {
        UUID id = UUID.fromString(report.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(report);

        return DiagnosticReportEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceJson(json)
                .patientId(extractPatientId(report))
                .encounterId(extractEncounterId(report))
                .status(extractStatus(report))
                .code(extractCode(report))
                .codeSystem(extractCodeSystem(report))
                .codeDisplay(extractCodeDisplay(report))
                .categoryCode(extractCategoryCode(report))
                .categoryDisplay(extractCategoryDisplay(report))
                .effectiveDatetime(extractEffectiveDatetime(report))
                .issuedDatetime(extractIssuedDatetime(report))
                .performerReference(extractPerformerReference(report))
                .conclusion(extractConclusion(report))
                .conclusionCodes(extractConclusionCodes(report))
                .resultCount(extractResultCount(report))
                .basedOnReference(extractBasedOnReference(report))
                .specimenReferences(extractSpecimenReferences(report))
                .presentedFormUrl(extractPresentedFormUrl(report))
                .presentedFormContentType(extractPresentedFormContentType(report))
                .build();
    }

    private DiagnosticReport toDiagnosticReport(DiagnosticReportEntity entity) {
        return JSON_PARSER.parseResource(DiagnosticReport.class, entity.getResourceJson());
    }

    // ==================== Field Extraction Methods ====================

    private UUID extractPatientId(DiagnosticReport report) {
        Reference subject = report.getSubject();
        if (subject != null && subject.hasReference()) {
            String ref = subject.getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return UUID.fromString(ref.substring(8));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid patient UUID in diagnostic report: {}", ref);
                }
            }
        }
        return null;
    }

    private UUID extractEncounterId(DiagnosticReport report) {
        if (report.hasEncounter()) {
            Reference encounter = report.getEncounter();
            if (encounter.hasReference() && encounter.getReference().startsWith("Encounter/")) {
                try {
                    return UUID.fromString(encounter.getReference().substring(10));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encounter UUID in diagnostic report: {}", encounter.getReference());
                }
            }
        }
        return null;
    }

    private String extractStatus(DiagnosticReport report) {
        return report.hasStatus() ? report.getStatus().toCode() : null;
    }

    private String extractCode(DiagnosticReport report) {
        if (report.hasCode()) {
            CodeableConcept code = report.getCode();
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                return code.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractCodeSystem(DiagnosticReport report) {
        if (report.hasCode()) {
            CodeableConcept code = report.getCode();
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                return code.getCodingFirstRep().getSystem();
            }
        }
        return null;
    }

    private String extractCodeDisplay(DiagnosticReport report) {
        if (report.hasCode()) {
            CodeableConcept code = report.getCode();
            if (code.hasText()) {
                return code.getText();
            }
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                return code.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractCategoryCode(DiagnosticReport report) {
        if (report.hasCategory() && !report.getCategory().isEmpty()) {
            CodeableConcept category = report.getCategoryFirstRep();
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractCategoryDisplay(DiagnosticReport report) {
        if (report.hasCategory() && !report.getCategory().isEmpty()) {
            CodeableConcept category = report.getCategoryFirstRep();
            if (category.hasText()) {
                return category.getText();
            }
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private Instant extractEffectiveDatetime(DiagnosticReport report) {
        if (report.hasEffectiveDateTimeType()) {
            return report.getEffectiveDateTimeType().getValue().toInstant();
        }
        return null;
    }

    private Instant extractIssuedDatetime(DiagnosticReport report) {
        if (report.hasIssued()) {
            return report.getIssued().toInstant();
        }
        return null;
    }

    private String extractPerformerReference(DiagnosticReport report) {
        if (report.hasPerformer() && !report.getPerformer().isEmpty()) {
            return report.getPerformerFirstRep().getReference();
        }
        return null;
    }

    private String extractConclusion(DiagnosticReport report) {
        return report.hasConclusion() ? report.getConclusion() : null;
    }

    private String extractConclusionCodes(DiagnosticReport report) {
        if (report.hasConclusionCode() && !report.getConclusionCode().isEmpty()) {
            return report.getConclusionCode().stream()
                    .filter(cc -> cc.hasCoding() && !cc.getCoding().isEmpty())
                    .map(cc -> cc.getCodingFirstRep().getCode())
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private Integer extractResultCount(DiagnosticReport report) {
        return report.hasResult() ? report.getResult().size() : 0;
    }

    private String extractBasedOnReference(DiagnosticReport report) {
        if (report.hasBasedOn() && !report.getBasedOn().isEmpty()) {
            return report.getBasedOnFirstRep().getReference();
        }
        return null;
    }

    private String extractSpecimenReferences(DiagnosticReport report) {
        if (report.hasSpecimen() && !report.getSpecimen().isEmpty()) {
            return report.getSpecimen().stream()
                    .filter(Reference::hasReference)
                    .map(Reference::getReference)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String extractPresentedFormUrl(DiagnosticReport report) {
        if (report.hasPresentedForm() && !report.getPresentedForm().isEmpty()) {
            Attachment attachment = report.getPresentedFormFirstRep();
            return attachment.hasUrl() ? attachment.getUrl() : null;
        }
        return null;
    }

    private String extractPresentedFormContentType(DiagnosticReport report) {
        if (report.hasPresentedForm() && !report.getPresentedForm().isEmpty()) {
            Attachment attachment = report.getPresentedFormFirstRep();
            return attachment.hasContentType() ? attachment.getContentType() : null;
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(DiagnosticReportEntity entity, String eventType, String actor) {
        try {
            DiagnosticReportEvent event = new DiagnosticReportEvent(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getPatientId() != null ? entity.getPatientId().toString() : null,
                    entity.getCode(),
                    entity.getCategoryCode(),
                    eventType,
                    Instant.now(),
                    actor
            );
            kafkaTemplate.send("fhir.diagnostic-reports." + eventType, entity.getId().toString(), event);
            log.debug("Published diagnostic report event: type={}, id={}", eventType, entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish diagnostic report event: type={}, id={}", eventType, entity.getId(), e);
        }
    }

    /**
     * Event record for Kafka publishing
     */
    public record DiagnosticReportEvent(
            String id,
            String tenantId,
            String patientId,
            String reportCode,
            String category,
            String eventType,
            Instant occurredAt,
            String actor
    ) {}
}
