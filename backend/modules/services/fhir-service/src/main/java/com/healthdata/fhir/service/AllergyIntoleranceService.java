package com.healthdata.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.fhir.persistence.AllergyIntoleranceEntity;
import com.healthdata.fhir.persistence.AllergyIntoleranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AllergyIntoleranceService {

    private final AllergyIntoleranceRepository allergyIntoleranceRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "allergyIntolerances", key = "#tenantId + ':' + #result.idElement.idPart"),
        @CacheEvict(value = "patientAllergies", key = "#tenantId + ':' + #allergyIntolerance.patient.referenceElement.idPart")
    })
    public AllergyIntolerance createAllergyIntolerance(String tenantId, AllergyIntolerance allergyIntolerance, String createdBy) {
        log.info("Creating AllergyIntolerance for tenant: {}", tenantId);

        // Generate ID if not present
        if (!allergyIntolerance.hasId()) {
            allergyIntolerance.setId(UUID.randomUUID().toString());
        }

        // Convert FHIR resource to entity
        AllergyIntoleranceEntity entity = toEntity(tenantId, allergyIntolerance, createdBy);

        // Save entity
        AllergyIntoleranceEntity saved = allergyIntoleranceRepository.save(entity);

        // Publish audit event
        publishAuditEvent(tenantId, "CREATE", saved.getId().toString(), createdBy);

        log.info("Created AllergyIntolerance: {} for patient: {}", saved.getId(), saved.getPatientId());
        return allergyIntolerance;
    }

    @Cacheable(value = "allergyIntolerances", key = "#tenantId + ':' + #id")
    @Transactional(readOnly = true)
    public Optional<AllergyIntolerance> getAllergyIntolerance(String tenantId, String id) {
        log.debug("Fetching AllergyIntolerance: {} for tenant: {}", id, tenantId);

        return allergyIntoleranceRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .map(this::toFhir);
    }

    @Caching(evict = {
        @CacheEvict(value = "allergyIntolerances", key = "#tenantId + ':' + #id"),
        @CacheEvict(value = "patientAllergies", key = "#tenantId + ':' + #allergyIntolerance.patient.referenceElement.idPart")
    })
    @Transactional
    public AllergyIntolerance updateAllergyIntolerance(String tenantId, String id, AllergyIntolerance allergyIntolerance, String modifiedBy) {
        log.info("Updating AllergyIntolerance: {} for tenant: {}", id, tenantId);

        AllergyIntoleranceEntity existing = allergyIntoleranceRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("AllergyIntolerance not found: " + id));

        // Update entity from FHIR resource
        updateEntityFromFhir(existing, allergyIntolerance, modifiedBy);

        AllergyIntoleranceEntity updated = allergyIntoleranceRepository.save(existing);

        // Publish audit event
        publishAuditEvent(tenantId, "UPDATE", updated.getId().toString(), modifiedBy);

        log.info("Updated AllergyIntolerance: {}", updated.getId());
        return allergyIntolerance;
    }

    @Caching(evict = {
        @CacheEvict(value = "allergyIntolerances", key = "#tenantId + ':' + #id"),
        @CacheEvict(value = "patientAllergies", allEntries = true)
    })
    @Transactional
    public void deleteAllergyIntolerance(String tenantId, String id, String deletedBy) {
        log.info("Deleting AllergyIntolerance: {} for tenant: {}", id, tenantId);

        AllergyIntoleranceEntity entity = allergyIntoleranceRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("AllergyIntolerance not found: " + id));

        allergyIntoleranceRepository.delete(entity);

        // Publish audit event
        publishAuditEvent(tenantId, "DELETE", id, deletedBy);

        log.info("Deleted AllergyIntolerance: {}", id);
    }

    @Cacheable(value = "patientAllergies", key = "#tenantId + ':' + #patientId")
    @Transactional(readOnly = true)
    public Bundle getAllergiesByPatient(String tenantId, String patientId, Pageable pageable) {
        log.debug("Fetching allergies for patient: {} in tenant: {}", patientId, tenantId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findByTenantIdAndPatientIdOrderByRecordedDateDesc(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getActiveAllergies(String tenantId, String patientId) {
        log.debug("Fetching active allergies for patient: {}", patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findActiveAllergiesByPatient(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getCriticalAllergies(String tenantId, String patientId) {
        log.debug("Fetching critical allergies for patient: {}", patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findCriticalAllergies(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getMedicationAllergies(String tenantId, String patientId) {
        log.debug("Fetching medication allergies for patient: {}", patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findMedicationAllergies(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getFoodAllergies(String tenantId, String patientId) {
        log.debug("Fetching food allergies for patient: {}", patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findFoodAllergies(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getAllergiesByCategory(String tenantId, String patientId, String category) {
        log.debug("Fetching {} allergies for patient: {}", category, patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findByCategory(tenantId, UUID.fromString(patientId), category);

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getConfirmedAllergies(String tenantId, String patientId) {
        log.debug("Fetching confirmed allergies for patient: {}", patientId);

        List<AllergyIntoleranceEntity> entities = allergyIntoleranceRepository
                .findConfirmedAllergies(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAllergy(String tenantId, String patientId, String allergyCode) {
        log.debug("Checking if patient {} has active allergy: {}", patientId, allergyCode);

        return allergyIntoleranceRepository.hasActiveAllergy(tenantId, UUID.fromString(patientId), allergyCode);
    }

    @Transactional(readOnly = true)
    public long countActiveAllergies(String tenantId, String patientId) {
        return allergyIntoleranceRepository.countActiveAllergies(tenantId, UUID.fromString(patientId));
    }

    @Transactional(readOnly = true)
    public long countCriticalAllergies(String tenantId, String patientId) {
        return allergyIntoleranceRepository.countByCriticality(tenantId, UUID.fromString(patientId), "high");
    }

    // Entity to FHIR conversion
    private AllergyIntolerance toFhir(AllergyIntoleranceEntity entity) {
        if (entity.getFhirResource() != null) {
            return jsonParser.parseResource(AllergyIntolerance.class, entity.getFhirResource());
        }

        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.setId(entity.getId().toString());

        // Clinical status
        if (entity.getClinicalStatus() != null) {
            allergyIntolerance.setClinicalStatus(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                            .setCode(entity.getClinicalStatus())));
        }

        // Verification status
        if (entity.getVerificationStatus() != null) {
            allergyIntolerance.setVerificationStatus(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                            .setCode(entity.getVerificationStatus())));
        }

        // Type
        if (entity.getType() != null) {
            allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.fromCode(entity.getType()));
        }

        // Category
        if (entity.getCategory() != null) {
            allergyIntolerance.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.fromCode(entity.getCategory()));
        }

        // Criticality
        if (entity.getCriticality() != null) {
            allergyIntolerance.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.fromCode(entity.getCriticality()));
        }

        // Code (allergen)
        if (entity.getCode() != null) {
            allergyIntolerance.setCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(entity.getCodeSystem())
                            .setCode(entity.getCode())
                            .setDisplay(entity.getCodeDisplay())));
        }

        // Patient reference
        allergyIntolerance.setPatient(new Reference("Patient/" + entity.getPatientId()));

        // Recorded date
        if (entity.getRecordedDate() != null) {
            allergyIntolerance.setRecordedDateElement(new DateTimeType(
                    Date.from(entity.getRecordedDate().atZone(ZoneId.systemDefault()).toInstant())));
        }

        // Reactions
        if (entity.getHasReactions() != null && entity.getHasReactions()) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent reaction =
                    new AllergyIntolerance.AllergyIntoleranceReactionComponent();

            if (entity.getReactionSeverity() != null) {
                reaction.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.fromCode(entity.getReactionSeverity()));
            }

            allergyIntolerance.addReaction(reaction);
        }

        return allergyIntolerance;
    }

    // FHIR to Entity conversion
    private AllergyIntoleranceEntity toEntity(String tenantId, AllergyIntolerance allergyIntolerance, String createdBy) {
        AllergyIntoleranceEntity entity = new AllergyIntoleranceEntity();
        entity.setId(UUID.fromString(allergyIntolerance.getIdElement().getIdPart()));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(createdBy);

        updateEntityFromFhir(entity, allergyIntolerance, createdBy);

        // Store FHIR resource as JSON
        entity.setFhirResource(jsonParser.encodeResourceToString(allergyIntolerance));

        return entity;
    }

    private void updateEntityFromFhir(AllergyIntoleranceEntity entity, AllergyIntolerance allergyIntolerance, String modifiedBy) {
        entity.setLastModifiedBy(modifiedBy);

        // Patient reference
        if (allergyIntolerance.hasPatient()) {
            String patientId = allergyIntolerance.getPatient().getReferenceElement().getIdPart();
            entity.setPatientId(UUID.fromString(patientId));
        }

        // Clinical status
        if (allergyIntolerance.hasClinicalStatus()) {
            entity.setClinicalStatus(allergyIntolerance.getClinicalStatus().getCodingFirstRep().getCode());
        }

        // Verification status
        if (allergyIntolerance.hasVerificationStatus()) {
            entity.setVerificationStatus(allergyIntolerance.getVerificationStatus().getCodingFirstRep().getCode());
        }

        // Type
        if (allergyIntolerance.hasType()) {
            entity.setType(allergyIntolerance.getType().toCode());
        }

        // Category
        if (allergyIntolerance.hasCategory() && !allergyIntolerance.getCategory().isEmpty()) {
            entity.setCategory(allergyIntolerance.getCategory().get(0).getValue().toCode());
        }

        // Criticality
        if (allergyIntolerance.hasCriticality()) {
            entity.setCriticality(allergyIntolerance.getCriticality().toCode());
        }

        // Code (allergen)
        if (allergyIntolerance.hasCode()) {
            Coding coding = allergyIntolerance.getCode().getCodingFirstRep();
            entity.setCode(coding.getCode());
            entity.setCodeSystem(coding.getSystem());
            entity.setCodeDisplay(coding.getDisplay());
        }

        // Recorded date
        if (allergyIntolerance.hasRecordedDate()) {
            LocalDateTime recordedDate = allergyIntolerance.getRecordedDate()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            entity.setRecordedDate(recordedDate);
        }

        // Reactions
        if (allergyIntolerance.hasReaction()) {
            entity.setHasReactions(true);
            AllergyIntolerance.AllergyIntoleranceReactionComponent reaction = allergyIntolerance.getReactionFirstRep();

            if (reaction.hasSeverity()) {
                entity.setReactionSeverity(reaction.getSeverity().toCode());
            }
        }

        // Update FHIR resource JSON
        entity.setFhirResource(jsonParser.encodeResourceToString(allergyIntolerance));
    }

    private Bundle createBundle(List<AllergyIntoleranceEntity> entities) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(entities.size());

        for (AllergyIntoleranceEntity entity : entities) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(toFhir(entity));
            entry.setFullUrl("AllergyIntolerance/" + entity.getId());
        }

        return bundle;
    }

    private void publishAuditEvent(String tenantId, String action, String resourceId, String userId) {
        try {
            String auditEvent = String.format(
                    "{\"tenantId\":\"%s\",\"action\":\"%s\",\"resourceType\":\"AllergyIntolerance\",\"resourceId\":\"%s\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                    tenantId, action, resourceId, userId, LocalDateTime.now()
            );
            kafkaTemplate.send("audit-events", auditEvent);
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }

    private LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
