package com.healthdata.fhir.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.EncounterEntity;
import com.healthdata.fhir.persistence.EncounterRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing FHIR Encounter resources.
 * Handles patient visits, hospitalizations, and encounters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterService {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    private final EncounterRepository encounterRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Create a new Encounter resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "encounters", key = "#tenantId + ':' + #result.idElement.idPart"),
            @CacheEvict(value = "encountersByPatient", key = "#tenantId + ':' + #result.subject.reference")
    })
    public Encounter createEncounter(String tenantId, Encounter encounter, String createdBy) {
        log.info("Creating encounter for tenant: {}", tenantId);

        // Validate encounter
        validateEncounter(encounter);

        // Generate ID if not present
        UUID encounterId = ensureEncounterId(encounter);

        // Extract patient ID
        UUID patientId = extractPatientId(encounter);

        // Convert to entity
        EncounterEntity entity = toEntity(tenantId, encounterId, patientId, encounter, createdBy, createdBy);

        // Save entity
        EncounterEntity saved = encounterRepository.save(entity);
        log.info("Saved encounter with ID: {}", saved.getId());

        // Convert back to FHIR resource
        Encounter savedEncounter = toFhirResource(saved);

        // Publish event
        publishEncounterEvent("fhir.encounters.created", tenantId, encounterId.toString(), savedEncounter);

        return savedEncounter;
    }

    /**
     * Get an Encounter by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "encounters", key = "#tenantId + ':' + #id")
    public Optional<Encounter> getEncounter(String tenantId, String id) {
        log.debug("Retrieving encounter {} for tenant: {}", id, tenantId);

        try {
            UUID encounterId = UUID.fromString(id);
            return encounterRepository.findByTenantIdAndId(tenantId, encounterId)
                    .map(this::toFhirResource);
        } catch (IllegalArgumentException e) {
            log.error("Invalid encounter ID format: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Update an Encounter resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "encounters", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "encountersByPatient", allEntries = true)
    })
    public Encounter updateEncounter(String tenantId, String id, Encounter encounter, String modifiedBy) {
        log.info("Updating encounter {} for tenant: {}", id, tenantId);

        UUID encounterId = UUID.fromString(id);

        // Find existing encounter
        EncounterEntity existing = encounterRepository.findByTenantIdAndId(tenantId, encounterId)
                .orElseThrow(() -> new EncounterNotFoundException("Encounter not found: " + id));

        // Validate encounter
        validateEncounter(encounter);

        // Ensure ID matches
        encounter.setId(id);

        // Extract patient ID
        UUID patientId = extractPatientId(encounter);

        // Update entity
        existing.setResourceJson(JSON_PARSER.encodeResourceToString(encounter));
        existing.setPatientId(patientId);

        // Update extracted fields
        updateExtractedFields(existing, encounter);

        // Save entity
        EncounterEntity updated = encounterRepository.save(existing);
        log.info("Updated encounter with ID: {}", updated.getId());

        // Convert back to FHIR resource
        Encounter updatedEncounter = toFhirResource(updated);

        // Publish event
        publishEncounterEvent("fhir.encounters.updated", tenantId, id, updatedEncounter);

        return updatedEncounter;
    }

    /**
     * Delete an Encounter resource
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "encounters", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "encountersByPatient", allEntries = true)
    })
    public void deleteEncounter(String tenantId, String id, String deletedBy) {
        log.info("Deleting encounter {} for tenant: {}", id, tenantId);

        UUID encounterId = UUID.fromString(id);

        EncounterEntity encounter = encounterRepository.findByTenantIdAndId(tenantId, encounterId)
                .orElseThrow(() -> new EncounterNotFoundException("Encounter not found: " + id));

        encounterRepository.delete(encounter);
        log.info("Deleted encounter with ID: {}", id);

        // Publish event
        publishEncounterEvent("fhir.encounters.deleted", tenantId, id, null);
    }

    /**
     * Search encounters by patient
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "encountersByPatient", key = "#tenantId + ':' + #patientId")
    public Bundle searchEncountersByPatient(String tenantId, String patientId, Pageable pageable) {
        log.debug("Searching encounters for patient: {}, tenant: {}", patientId, tenantId);

        UUID patientUuid = UUID.fromString(patientId);
        Page<EncounterEntity> page = encounterRepository.findByTenantIdAndPatientIdOrderByPeriodStartDesc(
                tenantId, patientUuid, pageable);

        return createBundle(page.getContent(), page.getTotalElements());
    }

    /**
     * Search encounters by patient and date range
     */
    @Transactional(readOnly = true)
    public Bundle searchEncountersByPatientAndDateRange(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Searching encounters for patient: {} in date range: {} to {}", patientId, startDate, endDate);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findByPatientAndDateRange(
                tenantId, patientUuid, startDate, endDate);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Get finished encounters for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getFinishedEncountersByPatient(String tenantId, String patientId) {
        log.debug("Getting finished encounters for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findFinishedEncountersByPatient(
                tenantId, patientUuid);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Get active encounters for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getActiveEncountersByPatient(String tenantId, String patientId) {
        log.debug("Getting active encounters for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findActiveEncountersByPatient(
                tenantId, patientUuid);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Get inpatient encounters for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getInpatientEncountersByPatient(String tenantId, String patientId) {
        log.debug("Getting inpatient encounters for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findInpatientEncountersByPatient(
                tenantId, patientUuid);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Get ambulatory encounters for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getAmbulatoryEncountersByPatient(String tenantId, String patientId) {
        log.debug("Getting ambulatory encounters for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findAmbulatoryEncountersByPatient(
                tenantId, patientUuid);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Get emergency encounters for a patient
     */
    @Transactional(readOnly = true)
    public Bundle getEmergencyEncountersByPatient(String tenantId, String patientId) {
        log.debug("Getting emergency encounters for patient: {}", patientId);

        UUID patientUuid = UUID.fromString(patientId);
        List<EncounterEntity> encounters = encounterRepository.findEmergencyEncountersByPatient(
                tenantId, patientUuid);

        return createBundle(encounters, encounters.size());
    }

    /**
     * Check if patient has encounter in date range
     */
    @Transactional(readOnly = true)
    public boolean hasEncounterInDateRange(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        return encounterRepository.hasEncounterInDateRange(tenantId, patientUuid, startDate, endDate);
    }

    /**
     * Count inpatient encounters in date range (for utilization measures)
     */
    @Transactional(readOnly = true)
    public long countInpatientEncounters(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        return encounterRepository.countInpatientEncountersInDateRange(
                tenantId, patientUuid, startDate, endDate);
    }

    /**
     * Count emergency encounters in date range
     */
    @Transactional(readOnly = true)
    public long countEmergencyEncounters(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        return encounterRepository.countEmergencyEncountersInDateRange(
                tenantId, patientUuid, startDate, endDate);
    }

    // Helper methods

    private void validateEncounter(Encounter encounter) {
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter cannot be null");
        }
        if (encounter.getSubject() == null || encounter.getSubject().getReference() == null) {
            throw new IllegalArgumentException("Encounter must have a patient reference");
        }
    }

    private UUID ensureEncounterId(Encounter encounter) {
        if (encounter.hasId()) {
            return UUID.fromString(encounter.getIdElement().getIdPart());
        }
        UUID newId = UUID.randomUUID();
        encounter.setId(newId.toString());
        return newId;
    }

    private UUID extractPatientId(Encounter encounter) {
        String patientRef = encounter.getSubject().getReference();
        String patientId = patientRef.contains("/") ? patientRef.substring(patientRef.lastIndexOf("/") + 1)
                : patientRef;
        return UUID.fromString(patientId);
    }

    private EncounterEntity toEntity(String tenantId, UUID encounterId, UUID patientId,
            Encounter encounter, String createdBy, String modifiedBy) {
        EncounterEntity entity = EncounterEntity.builder()
                .id(encounterId)
                .tenantId(tenantId)
                .resourceType("Encounter")
                .resourceJson(JSON_PARSER.encodeResourceToString(encounter))
                .patientId(patientId)
                .build();

        updateExtractedFields(entity, encounter);
        return entity;
    }

    private void updateExtractedFields(EncounterEntity entity, Encounter encounter) {
        // Extract encounter class
        if (encounter.hasClass_()) {
            entity.setEncounterClass(encounter.getClass_().getCode());
        }

        // Extract encounter type (first type)
        if (encounter.hasType() && !encounter.getType().isEmpty()
                && encounter.getType().get(0).hasCoding() && !encounter.getType().get(0).getCoding().isEmpty()) {
            Coding typeCoding = encounter.getType().get(0).getCoding().get(0);
            entity.setEncounterTypeCode(typeCoding.getCode());
            entity.setEncounterTypeSystem(typeCoding.getSystem());
            entity.setEncounterTypeDisplay(typeCoding.getDisplay());
        }

        // Extract status
        if (encounter.hasStatus()) {
            entity.setStatus(encounter.getStatus().toCode());
        }

        // Extract service type
        if (encounter.hasServiceType() && encounter.getServiceType().hasCoding()
                && !encounter.getServiceType().getCoding().isEmpty()) {
            Coding serviceType = encounter.getServiceType().getCoding().get(0);
            entity.setServiceTypeCode(serviceType.getCode());
            entity.setServiceTypeDisplay(serviceType.getDisplay());
        }

        // Extract priority
        if (encounter.hasPriority() && encounter.getPriority().hasCoding()
                && !encounter.getPriority().getCoding().isEmpty()) {
            entity.setPriority(encounter.getPriority().getCoding().get(0).getCode());
        }

        // Extract period
        if (encounter.hasPeriod()) {
            Period period = encounter.getPeriod();
            if (period.hasStart()) {
                entity.setPeriodStart(dateToLocalDateTime(period.getStart()));
            }
            if (period.hasEnd()) {
                entity.setPeriodEnd(dateToLocalDateTime(period.getEnd()));
            }
            // Calculate duration if both start and end are present
            if (period.hasStart() && period.hasEnd()) {
                long diffMinutes = (period.getEnd().getTime() - period.getStart().getTime()) / (60 * 1000);
                entity.setDurationMinutes((int) diffMinutes);
            }
        }

        // Extract reason (first reason)
        if (encounter.hasReasonCode() && !encounter.getReasonCode().isEmpty()
                && encounter.getReasonCode().get(0).hasCoding()
                && !encounter.getReasonCode().get(0).getCoding().isEmpty()) {
            Coding reasonCoding = encounter.getReasonCode().get(0).getCoding().get(0);
            entity.setReasonCode(reasonCoding.getCode());
            entity.setReasonSystem(reasonCoding.getSystem());
            entity.setReasonDisplay(reasonCoding.getDisplay());
        }

        // Extract location (first location)
        if (encounter.hasLocation() && !encounter.getLocation().isEmpty()) {
            Encounter.EncounterLocationComponent location = encounter.getLocation().get(0);
            if (location.hasLocation()) {
                Reference locationRef = location.getLocation();
                entity.setLocationId(locationRef.getReference());
                entity.setLocationDisplay(locationRef.getDisplay());
            }
        }

        // Extract participant (first participant)
        if (encounter.hasParticipant() && !encounter.getParticipant().isEmpty()) {
            Encounter.EncounterParticipantComponent participant = encounter.getParticipant().get(0);
            if (participant.hasIndividual()) {
                Reference participantRef = participant.getIndividual();
                entity.setParticipantId(participantRef.getReference());
                entity.setParticipantDisplay(participantRef.getDisplay());
            }
        }

        // Extract service provider
        if (encounter.hasServiceProvider()) {
            Reference serviceProvider = encounter.getServiceProvider();
            entity.setServiceProviderId(serviceProvider.getReference());
            entity.setServiceProviderDisplay(serviceProvider.getDisplay());
        }

        // Extract hospitalization details
        if (encounter.hasHospitalization()) {
            Encounter.EncounterHospitalizationComponent hosp = encounter.getHospitalization();
            if (hosp.hasAdmitSource() && hosp.getAdmitSource().hasCoding()
                    && !hosp.getAdmitSource().getCoding().isEmpty()) {
                entity.setAdmissionSource(hosp.getAdmitSource().getCoding().get(0).getCode());
            }
            if (hosp.hasDischargeDisposition() && hosp.getDischargeDisposition().hasCoding()
                    && !hosp.getDischargeDisposition().getCoding().isEmpty()) {
                entity.setDischargeDisposition(hosp.getDischargeDisposition().getCoding().get(0).getCode());
            }
        }
    }

    private Encounter toFhirResource(EncounterEntity entity) {
        return (Encounter) JSON_PARSER.parseResource(entity.getResourceJson());
    }

    private Bundle createBundle(List<EncounterEntity> encounters, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        List<Bundle.BundleEntryComponent> entries = encounters.stream()
                .map(entity -> {
                    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                    entry.setResource(toFhirResource(entity));
                    entry.setFullUrl("Encounter/" + entity.getId());
                    return entry;
                })
                .collect(Collectors.toList());

        bundle.setEntry(entries);
        return bundle;
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void publishEncounterEvent(String topic, String tenantId, String encounterId, Encounter encounter) {
        try {
            String payload = encounter != null ? JSON_PARSER.encodeResourceToString(encounter) : "{}";
            kafkaTemplate.send(topic, tenantId + ":" + encounterId, payload);
            log.debug("Published encounter event to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish encounter event", e);
        }
    }

    public static class EncounterNotFoundException extends RuntimeException {
        public EncounterNotFoundException(String message) {
            super(message);
        }
    }
}
