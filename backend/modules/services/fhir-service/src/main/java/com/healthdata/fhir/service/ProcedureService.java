package com.healthdata.fhir.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.ProcedureEntity;
import com.healthdata.fhir.persistence.ProcedureRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing FHIR Procedure resources.
 * Handles surgical procedures, interventions, and diagnostic procedures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcedureService {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    private final ProcedureRepository procedureRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create a new Procedure resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "procedures", key = "#tenantId + ':' + #result.idElement.idPart"),
            @CacheEvict(value = "proceduresByPatient", key = "#tenantId + ':' + #result.subject.reference")
    })
    public Procedure createProcedure(String tenantId, Procedure procedure, String createdBy) {
        log.info("Creating procedure for tenant: {}", tenantId);

        // Validate procedure
        validateProcedure(procedure);

        // Generate ID if not present
        UUID procedureId = ensureProcedureId(procedure);

        // Extract patient ID
        UUID patientId = extractPatientId(procedure);

        // Convert to entity
        ProcedureEntity entity = toEntity(tenantId, procedureId, patientId, procedure, createdBy, createdBy);

        // Save entity
        ProcedureEntity saved = procedureRepository.save(entity);
        log.info("Saved procedure with ID: {}", saved.getId());

        // Convert back to FHIR resource
        Procedure savedProcedure = toFhirResource(saved);

        // Publish event
        publishProcedureEvent("fhir.procedures.created", tenantId, procedureId.toString(), savedProcedure, createdBy);

        return savedProcedure;
    }

    /**
     * Get a Procedure by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "procedures", key = "#tenantId + ':' + #id")
    public Optional<Procedure> getProcedure(String tenantId, String id) {
        log.debug("Retrieving procedure {} for tenant: {}", id, tenantId);

        try {
            UUID procedureId = UUID.fromString(id);
            return procedureRepository.findByTenantIdAndId(tenantId, procedureId)
                    .map(this::toFhirResource);
        } catch (IllegalArgumentException e) {
            log.error("Invalid procedure ID format: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Update a Procedure resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "procedures", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "proceduresByPatient", allEntries = true)
    })
    public Procedure updateProcedure(String tenantId, String id, Procedure procedure, String modifiedBy) {
        log.info("Updating procedure {} for tenant: {}", id, tenantId);

        UUID procedureId = UUID.fromString(id);

        // Find existing procedure
        ProcedureEntity existing = procedureRepository.findByTenantIdAndId(tenantId, procedureId)
                .orElseThrow(() -> new ProcedureNotFoundException("Procedure not found: " + id));

        // Validate procedure
        validateProcedure(procedure);

        // Ensure ID matches
        procedure.setId(id);

        // Extract patient ID
        UUID patientId = extractPatientId(procedure);

        // Update entity
        existing.setResourceJson(JSON_PARSER.encodeResourceToString(procedure));
        existing.setPatientId(patientId);

        // Update extracted fields
        updateExtractedFields(existing, procedure);

        // Save entity
        ProcedureEntity updated = procedureRepository.save(existing);
        log.info("Updated procedure with ID: {}", updated.getId());

        // Convert back to FHIR resource
        Procedure updatedProcedure = toFhirResource(updated);

        // Publish event
        publishProcedureEvent("fhir.procedures.updated", tenantId, id, updatedProcedure, modifiedBy);

        return updatedProcedure;
    }

    /**
     * Delete a Procedure resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "procedures", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "proceduresByPatient", allEntries = true)
    })
    public void deleteProcedure(String tenantId, String id, String deletedBy) {
        log.info("Deleting procedure {} for tenant: {}", id, tenantId);

        UUID procedureId = UUID.fromString(id);

        ProcedureEntity procedure = procedureRepository.findByTenantIdAndId(tenantId, procedureId)
                .orElseThrow(() -> new ProcedureNotFoundException("Procedure not found: " + id));

        procedureRepository.delete(procedure);
        log.info("Deleted procedure with ID: {}", id);

        // Publish event
        publishProcedureEvent("fhir.procedures.deleted", tenantId, id, null, deletedBy);
    }

    /**
     * Search procedures by patient
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "proceduresByPatient", key = "#tenantId + ':' + #patientId")
    public Bundle searchProceduresByPatient(String tenantId, String patientId, Pageable pageable) {
        log.debug("Searching procedures for patient: {}, tenant: {}", patientId, tenantId);

        UUID patientUuid = UUID.fromString(patientId);
        Page<ProcedureEntity> page = procedureRepository.findByTenantIdAndPatientIdOrderByPerformedDateDesc(
                tenantId, patientUuid, pageable);

        return createBundle(page.getContent(), page.getTotalElements());
    }

    /**
     * Search procedures by patient and date range
     */
    @Transactional(readOnly = true)
    public Bundle searchProceduresByPatientAndDateRange(String tenantId, String patientId,
            LocalDate startDate, LocalDate endDate) {
        log.debug("Searching procedures for patient: {} in date range: {} to {}", patientId, startDate, endDate);

        UUID patientUuid = UUID.fromString(patientId);
        List<ProcedureEntity> procedures = procedureRepository.findByPatientAndDateRange(
                tenantId, patientUuid, startDate, endDate);

        return createBundle(procedures, procedures.size());
    }

    /**
     * Get completed procedures for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getCompletedProceduresByPatient(String tenantId, String patientId) {
        log.debug("Getting completed procedures for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<ProcedureEntity> procedures = procedureRepository.findCompletedProceduresByPatient(
                tenantId, patientUuid);

        return createBundle(procedures, procedures.size());
    }

    /**
     * Get surgical procedures for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getSurgicalProceduresByPatient(String tenantId, String patientId) {
        log.debug("Getting surgical procedures for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<ProcedureEntity> procedures = procedureRepository.findSurgicalProceduresByPatient(
                tenantId, patientUuid);

        return createBundle(procedures, procedures.size());
    }

    /**
     * Get diagnostic procedures for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getDiagnosticProceduresByPatient(String tenantId, String patientId) {
        log.debug("Getting diagnostic procedures for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<ProcedureEntity> procedures = procedureRepository.findDiagnosticProceduresByPatient(
                tenantId, patientUuid);

        return createBundle(procedures, procedures.size());
    }

    /**
     * Get procedures with complications for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getProceduresWithComplications(String tenantId, String patientId) {
        log.debug("Getting procedures with complications for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<ProcedureEntity> procedures = procedureRepository.findProceduresWithComplications(
                tenantId, patientUuid);

        return createBundle(procedures, procedures.size());
    }

    /**
     * Check if patient has specific procedure
     */
    @Transactional(readOnly = true)
    public boolean hasCompletedProcedure(String tenantId, String patientId, String procedureCode) {
        UUID patientUuid = UUID.fromString(patientId);
        return procedureRepository.hasCompletedProcedure(tenantId, patientUuid, procedureCode);
    }

    /**
     * Check if patient has procedure in date range
     */
    @Transactional(readOnly = true)
    public boolean hasProcedureInDateRange(String tenantId, String patientId,
            LocalDate startDate, LocalDate endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        return procedureRepository.hasProcedureInDateRange(tenantId, patientUuid, startDate, endDate);
    }

    // Helper methods

    private void validateProcedure(Procedure procedure) {
        if (procedure == null) {
            throw new IllegalArgumentException("Procedure cannot be null");
        }
        if (procedure.getSubject() == null || procedure.getSubject().getReference() == null) {
            throw new IllegalArgumentException("Procedure must have a patient reference");
        }
    }

    private UUID ensureProcedureId(Procedure procedure) {
        if (procedure.hasId()) {
            return UUID.fromString(procedure.getIdElement().getIdPart());
        }
        UUID newId = UUID.randomUUID();
        procedure.setId(newId.toString());
        return newId;
    }

    private UUID extractPatientId(Procedure procedure) {
        String patientRef = procedure.getSubject().getReference();
        String patientId = patientRef.contains("/") ? patientRef.substring(patientRef.lastIndexOf("/") + 1)
                : patientRef;
        return UUID.fromString(patientId);
    }

    private ProcedureEntity toEntity(String tenantId, UUID procedureId, UUID patientId,
            Procedure procedure, String createdBy, String modifiedBy) {
        ProcedureEntity entity = ProcedureEntity.builder()
                .id(procedureId)
                .tenantId(tenantId)
                .resourceType("Procedure")
                .resourceJson(JSON_PARSER.encodeResourceToString(procedure))
                .patientId(patientId)
                .createdBy(createdBy)
                .modifiedBy(modifiedBy)
                .build();

        updateExtractedFields(entity, procedure);
        return entity;
    }

    private void updateExtractedFields(ProcedureEntity entity, Procedure procedure) {
        // Extract procedure code
        if (procedure.hasCode() && procedure.getCode().hasCoding() && !procedure.getCode().getCoding().isEmpty()) {
            Coding procedureCoding = procedure.getCode().getCoding().get(0);
            entity.setProcedureCode(procedureCoding.getCode());
            entity.setProcedureSystem(procedureCoding.getSystem());
            entity.setProcedureDisplay(procedureCoding.getDisplay());
        }

        // Extract status
        if (procedure.hasStatus()) {
            entity.setStatus(procedure.getStatus().toCode());
        }

        // Extract category
        if (procedure.hasCategory() && procedure.getCategory().hasCoding()
                && !procedure.getCategory().getCoding().isEmpty()) {
            Coding category = procedure.getCategory().getCoding().get(0);
            entity.setCategoryCode(category.getCode());
            entity.setCategoryDisplay(category.getDisplay());
        }

        // Extract performed date/period
        if (procedure.hasPerformedDateTimeType()) {
            DateTimeType performedDateTime = procedure.getPerformedDateTimeType();
            LocalDate performedDate = dateToLocalDate(performedDateTime.getValue());
            entity.setPerformedDate(performedDate);
        } else if (procedure.hasPerformedPeriod()) {
            Period period = procedure.getPerformedPeriod();
            if (period.hasStart()) {
                entity.setPerformedStart(dateToLocalDateTime(period.getStart()));
                entity.setPerformedDate(dateToLocalDate(period.getStart()));
            }
            if (period.hasEnd()) {
                entity.setPerformedEnd(dateToLocalDateTime(period.getEnd()));
            }
        }

        // Extract performer (first performer)
        if (procedure.hasPerformer() && !procedure.getPerformer().isEmpty()) {
            Procedure.ProcedurePerformerComponent performer = procedure.getPerformer().get(0);
            if (performer.hasActor()) {
                Reference actor = performer.getActor();
                entity.setPerformerId(actor.getReference());
                entity.setPerformerDisplay(actor.getDisplay());
            }
            if (performer.hasFunction() && performer.getFunction().hasCoding()
                    && !performer.getFunction().getCoding().isEmpty()) {
                entity.setPerformerFunction(performer.getFunction().getCoding().get(0).getCode());
            }
        }

        // Extract location
        if (procedure.hasLocation()) {
            Reference location = procedure.getLocation();
            entity.setLocationId(location.getReference());
            entity.setLocationDisplay(location.getDisplay());
        }

        // Extract reason code (first reason)
        if (procedure.hasReasonCode() && !procedure.getReasonCode().isEmpty()) {
            CodeableConcept reasonCode = procedure.getReasonCode().get(0);
            if (reasonCode.hasCoding() && !reasonCode.getCoding().isEmpty()) {
                Coding reason = reasonCode.getCoding().get(0);
                entity.setReasonCode(reason.getCode());
                entity.setReasonSystem(reason.getSystem());
                entity.setReasonDisplay(reason.getDisplay());
            }
        }

        // Extract reason reference
        if (procedure.hasReasonReference() && !procedure.getReasonReference().isEmpty()) {
            entity.setReasonReference(procedure.getReasonReference().get(0).getReference());
        }

        // Extract body site (first body site)
        if (procedure.hasBodySite() && !procedure.getBodySite().isEmpty()) {
            CodeableConcept bodySite = procedure.getBodySite().get(0);
            if (bodySite.hasCoding() && !bodySite.getCoding().isEmpty()) {
                Coding bodySiteCoding = bodySite.getCoding().get(0);
                entity.setBodySiteCode(bodySiteCoding.getCode());
                entity.setBodySiteSystem(bodySiteCoding.getSystem());
                entity.setBodySiteDisplay(bodySiteCoding.getDisplay());
            }
        }

        // Extract outcome
        if (procedure.hasOutcome() && procedure.getOutcome().hasCoding()
                && !procedure.getOutcome().getCoding().isEmpty()) {
            Coding outcome = procedure.getOutcome().getCoding().get(0);
            entity.setOutcomeCode(outcome.getCode());
            entity.setOutcomeDisplay(outcome.getDisplay());
        }

        // Extract complication (first complication)
        if (procedure.hasComplication() && !procedure.getComplication().isEmpty()) {
            CodeableConcept complication = procedure.getComplication().get(0);
            if (complication.hasCoding() && !complication.getCoding().isEmpty()) {
                Coding complicationCoding = complication.getCoding().get(0);
                entity.setComplicationCode(complicationCoding.getCode());
                entity.setComplicationDisplay(complicationCoding.getDisplay());
            }
        }

        // Extract encounter reference
        if (procedure.hasEncounter()) {
            String encounterRef = procedure.getEncounter().getReference();
            if (encounterRef != null && encounterRef.contains("/")) {
                String encounterId = encounterRef.substring(encounterRef.lastIndexOf("/") + 1);
                try {
                    entity.setEncounterId(UUID.fromString(encounterId));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encounter ID format: {}", encounterId);
                }
            }
        }

        // Extract based on reference
        if (procedure.hasBasedOn() && !procedure.getBasedOn().isEmpty()) {
            entity.setBasedOnReference(procedure.getBasedOn().get(0).getReference());
        }

        // Extract part of reference
        if (procedure.hasPartOf() && !procedure.getPartOf().isEmpty()) {
            entity.setPartOfReference(procedure.getPartOf().get(0).getReference());
        }

        // Check for notes
        entity.setHasNotes(procedure.hasNote() && !procedure.getNote().isEmpty());
    }

    private Procedure toFhirResource(ProcedureEntity entity) {
        return (Procedure) JSON_PARSER.parseResource(entity.getResourceJson());
    }

    private Bundle createBundle(List<ProcedureEntity> procedures, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        List<Bundle.BundleEntryComponent> entries = procedures.stream()
                .map(entity -> {
                    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                    entry.setResource(toFhirResource(entity));
                    entry.setFullUrl("Procedure/" + entity.getId());
                    return entry;
                })
                .collect(Collectors.toList());

        bundle.setEntry(entries);
        return bundle;
    }

    private LocalDate dateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void publishProcedureEvent(
            String topic,
            String tenantId,
            String procedureId,
            Procedure procedure,
            String actor) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", topic);
            event.put("resourceType", "Procedure");
            event.put("resourceId", procedureId);
            event.put("tenantId", tenantId);
            if (procedure != null) {
                event.put("patientId", extractPatientId(procedure).toString());
            }
            event.put("occurredAt", java.time.Instant.now().toString());
            event.put("actor", actor);
            if (procedure != null) {
                String json = JSON_PARSER.encodeResourceToString(procedure);
                try {
                    event.put("resource", objectMapper.readValue(json, java.util.Map.class));
                } catch (Exception e) {
                    event.put("resource", json);
                }
            }
            kafkaTemplate.send(topic, tenantId + ":" + procedureId, event);
            log.debug("Published procedure event to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish procedure event", e);
        }
    }

    public static class ProcedureNotFoundException extends RuntimeException {
        public ProcedureNotFoundException(String message) {
            super(message);
        }
    }
}
