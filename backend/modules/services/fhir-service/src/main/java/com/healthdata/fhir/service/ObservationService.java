package com.healthdata.fhir.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.ObservationEntity;
import com.healthdata.fhir.persistence.ObservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class ObservationService {

    private static final String CACHE_NAME = "fhir-observations";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final ObservationRepository observationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Cache cache;
    private final ObjectMapper objectMapper;

    public ObservationService(
            ObservationRepository observationRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager,
            ObjectMapper objectMapper) {
        this.observationRepository = observationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cache = cacheManager.getCache(CACHE_NAME);
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Observation createObservation(String tenantId, Observation observation, String createdBy) {
        validateObservation(observation);

        UUID observationId = ensureObservationId(observation);
        observation.setId(observationId.toString());

        UUID patientId = extractPatientId(observation);

        ObservationEntity entity = toEntity(tenantId, observationId, patientId, observation);
        ObservationEntity saved = observationRepository.save(entity);
        Observation savedObservation = fromEntity(saved);
        applyMeta(savedObservation, saved);

        cachePut(tenantId, observationId.toString(), savedObservation);
        kafkaTemplate.send("fhir.observations.created", observationId.toString(),
                buildResourceEvent(tenantId, patientId.toString(), "fhir.observations.created",
                        "Observation", observationId.toString(), createdBy, savedObservation));

        return savedObservation;
    }

    @Transactional(readOnly = true)
    public Optional<Observation> getObservation(String tenantId, String observationId) {
        UUID uuid = parseUuid(observationId);
        Observation cached = cacheGet(tenantId, observationId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return observationRepository.findByTenantIdAndId(tenantId, uuid)
                .map(entity -> {
                    Observation parsed = fromEntity(entity);
                    cachePut(tenantId, observationId, parsed);
                    return parsed;
                });
    }

    @Transactional
    public Observation updateObservation(String tenantId, String observationId,
                                        Observation observation, String updatedBy) {
        UUID uuid = parseUuid(observationId);
        validateObservation(observation);

        observation.setId(observationId);
        UUID patientId = extractPatientId(observation);

        ObservationEntity entity = observationRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new ObservationNotFoundException(observationId));

        ObservationEntity updated = entity.toBuilder()
                .resourceJson(JSON_PARSER.encodeResourceToString(observation))
                .patientId(patientId)
                .code(extractCode(observation))
                .codeSystem(extractCodeSystem(observation))
                .category(extractCategory(observation))
                .status(observation.getStatus() != null ? observation.getStatus().toCode() : null)
                .effectiveDateTime(extractEffectiveDateTime(observation))
                .valueQuantity(extractValueQuantity(observation))
                .valueUnit(extractValueUnit(observation))
                .valueString(extractValueString(observation))
                .lastModifiedAt(Instant.now())
                .build();

        ObservationEntity persisted = observationRepository.save(updated);
        Observation savedObservation = fromEntity(persisted);
        applyMeta(savedObservation, persisted);

        cachePut(tenantId, observationId, savedObservation);
        kafkaTemplate.send("fhir.observations.updated", observationId,
                buildResourceEvent(tenantId, patientId.toString(), "fhir.observations.updated",
                        "Observation", observationId, updatedBy, savedObservation));

        return savedObservation;
    }

    @Transactional
    public void deleteObservation(String tenantId, String observationId, String deletedBy) {
        UUID uuid = parseUuid(observationId);
        ObservationEntity entity = observationRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new ObservationNotFoundException(observationId));

        observationRepository.delete(entity);
        cacheEvict(tenantId, observationId);
        kafkaTemplate.send("fhir.observations.deleted", observationId,
                buildResourceEvent(tenantId, entity.getPatientId().toString(), "fhir.observations.deleted",
                        "Observation", observationId, deletedBy, null));
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchObservationsByPatient(
            String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = parseUuid(patientId);
        Page<ObservationEntity> entities = observationRepository
                .findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(tenantId, patientUuid, pageable);

        return createBundle(entities.getContent(), entities.getTotalElements());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchObservationsByPatientAndCode(
            String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        List<ObservationEntity> entities = observationRepository
                .findByTenantIdAndPatientIdAndCodeOrderByEffectiveDateTimeDesc(
                        tenantId, patientUuid, code);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchObservationsByPatientAndCategory(
            String tenantId, String patientId, String category) {
        UUID patientUuid = parseUuid(patientId);
        List<ObservationEntity> entities = observationRepository
                .findByTenantIdAndPatientIdAndCategoryOrderByEffectiveDateTimeDesc(
                        tenantId, patientUuid, category);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchObservationsByPatientAndDateRange(
            String tenantId, String patientId, LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = parseUuid(patientId);
        List<ObservationEntity> entities = observationRepository.findByPatientAndDateRange(
                tenantId, patientUuid, startDate, endDate);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getLabResultsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ObservationEntity> entities = observationRepository.findLabResultsByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getVitalSignsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ObservationEntity> entities = observationRepository.findVitalSignsByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public Optional<Observation> getLatestObservationByPatientAndCode(
            String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        return observationRepository.findLatestByPatientAndCode(tenantId, patientUuid, code)
                .map(this::fromEntity);
    }

    private org.hl7.fhir.r4.model.Bundle createBundle(
            List<ObservationEntity> entities, long total) {
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        entities.stream()
                .map(entity -> {
                    Observation observation = fromEntity(entity);
                    applyMeta(observation, entity);
                    org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry =
                            new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                    entry.setResource(observation);
                    entry.setFullUrl("Observation/" + observation.getId());
                    return entry;
                })
                .forEach(bundle.getEntry()::add);

        return bundle;
    }

    private ObservationEntity toEntity(String tenantId, UUID observationId, UUID patientId,
                                      Observation observation) {
        return ObservationEntity.builder()
                .id(observationId)
                .tenantId(tenantId)
                .resourceType(observation.fhirType() != null ? observation.fhirType() : "Observation")
                .resourceJson(JSON_PARSER.encodeResourceToString(observation))
                .patientId(patientId)
                .code(extractCode(observation))
                .codeSystem(extractCodeSystem(observation))
                .category(extractCategory(observation))
                .status(observation.getStatus() != null ? observation.getStatus().toCode() : null)
                .effectiveDateTime(extractEffectiveDateTime(observation))
                .valueQuantity(extractValueQuantity(observation))
                .valueUnit(extractValueUnit(observation))
                .valueString(extractValueString(observation))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private Observation fromEntity(ObservationEntity entity) {
        Observation observation = (Observation) JSON_PARSER.parseResource(entity.getResourceJson());
        observation.setId(entity.getId().toString());
        return observation;
    }

    private void applyMeta(Observation observation, ObservationEntity entity) {
        observation.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        if (entity.getLastModifiedAt() != null) {
            observation.getMeta().setLastUpdated(Date.from(entity.getLastModifiedAt()));
        }
    }

    private UUID ensureObservationId(Observation observation) {
        if (observation.hasIdElement() && observation.getIdElement().getIdPart() != null) {
            return parseUuid(observation.getIdElement().getIdPart());
        }
        UUID generated = UUID.randomUUID();
        observation.setId(generated.toString());
        return generated;
    }

    private UUID extractPatientId(Observation observation) {
        if (!observation.hasSubject() || !observation.getSubject().hasReference()) {
            throw new ObservationValidationException("Observation must have a subject (patient)");
        }
        String reference = observation.getSubject().getReference();
        String patientId = reference.replace("Patient/", "");
        return parseUuid(patientId);
    }

    private String extractCode(Observation observation) {
        if (!observation.hasCode() || !observation.getCode().hasCoding() ||
                observation.getCode().getCoding().isEmpty()) {
            return null;
        }
        Coding coding = observation.getCode().getCodingFirstRep();
        return coding.getCode();
    }

    private String extractCodeSystem(Observation observation) {
        if (!observation.hasCode() || !observation.getCode().hasCoding() ||
                observation.getCode().getCoding().isEmpty()) {
            return null;
        }
        Coding coding = observation.getCode().getCodingFirstRep();
        return coding.getSystem();
    }

    private String extractCategory(Observation observation) {
        if (!observation.hasCategory() || observation.getCategory().isEmpty()) {
            return null;
        }
        CodeableConcept category = observation.getCategoryFirstRep();
        if (!category.hasCoding() || category.getCoding().isEmpty()) {
            return null;
        }
        return category.getCodingFirstRep().getCode();
    }

    private LocalDateTime extractEffectiveDateTime(Observation observation) {
        if (observation.hasEffectiveDateTimeType()) {
            Date date = observation.getEffectiveDateTimeType().getValue();
            return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
        }
        return null;
    }

    private Double extractValueQuantity(Observation observation) {
        if (observation.hasValueQuantity()) {
            Quantity quantity = observation.getValueQuantity();
            if (quantity.hasValue()) {
                return quantity.getValue().doubleValue();
            }
        }
        return null;
    }

    private String extractValueUnit(Observation observation) {
        if (observation.hasValueQuantity()) {
            Quantity quantity = observation.getValueQuantity();
            if (quantity.hasUnit()) {
                return quantity.getUnit();
            }
        }
        return null;
    }

    private String extractValueString(Observation observation) {
        if (observation.hasValueStringType()) {
            StringType stringValue = observation.getValueStringType();
            return stringValue.getValue();
        }
        return null;
    }

    private void validateObservation(Observation observation) {
        if (!observation.hasSubject()) {
            throw new ObservationValidationException("Observation must have a subject");
        }
        if (!observation.hasCode()) {
            throw new ObservationValidationException("Observation must have a code");
        }
        if (!observation.hasStatus()) {
            throw new ObservationValidationException("Observation must have a status");
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ObservationValidationException("ID must be a valid UUID");
        }
    }

    private void cachePut(String tenantId, String observationId, Observation observation) {
        if (cache != null) {
            cache.put(cacheKey(tenantId, observationId), observation);
        }
    }

    private Observation cacheGet(String tenantId, String observationId) {
        if (cache == null) {
            return null;
        }
        return cache.get(cacheKey(tenantId, observationId), Observation.class);
    }

    private void cacheEvict(String tenantId, String observationId) {
        if (cache != null) {
            cache.evict(cacheKey(tenantId, observationId));
        }
    }

    private String cacheKey(String tenantId, String observationId) {
        return tenantId + ":obs:" + observationId;
    }

    private java.util.Map<String, Object> buildResourceEvent(
            String tenantId,
            String patientId,
            String eventType,
            String resourceType,
            String resourceId,
            String actor,
            Observation resource) {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("resourceType", resourceType);
        event.put("resourceId", resourceId);
        event.put("tenantId", tenantId);
        event.put("patientId", patientId);
        event.put("occurredAt", Instant.now().toString());
        event.put("actor", actor);
        if (resource != null) {
            String json = JSON_PARSER.encodeResourceToString(resource);
            try {
                event.put("resource", objectMapper.readValue(json, java.util.Map.class));
            } catch (Exception e) {
                // Fall back to raw JSON when mapping fails.
                event.put("resource", json);
            }
        }
        return event;
    }

    public record ObservationEvent(String id, String tenantId, String patientId, String type,
                                   Instant occurredAt, String actor) {
    }

    public static class ObservationValidationException extends RuntimeException {
        public ObservationValidationException(String message) {
            super(message);
        }
    }

    public static class ObservationNotFoundException extends RuntimeException {
        public ObservationNotFoundException(String id) {
            super("Observation not found: " + id);
        }
    }
}
