package com.healthdata.fhir.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.ConditionEntity;
import com.healthdata.fhir.persistence.ConditionRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class ConditionService {

    private static final String CACHE_NAME = "fhir-conditions";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final ConditionRepository conditionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Cache cache;

    public ConditionService(
            ConditionRepository conditionRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager) {
        this.conditionRepository = conditionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Transactional
    public Condition createCondition(String tenantId, Condition condition, String createdBy) {
        validateCondition(condition);

        UUID conditionId = ensureConditionId(condition);
        condition.setId(conditionId.toString());

        UUID patientId = extractPatientId(condition);

        ConditionEntity entity = toEntity(tenantId, conditionId, patientId, condition);
        ConditionEntity saved = conditionRepository.save(entity);
        Condition savedCondition = fromEntity(saved);
        applyMeta(savedCondition, saved);

        cachePut(tenantId, conditionId.toString(), savedCondition);
        kafkaTemplate.send("fhir.conditions.created", conditionId.toString(),
                new ConditionEvent(conditionId.toString(), tenantId, patientId.toString(),
                        "CREATED", Instant.now(), createdBy));

        return savedCondition;
    }

    @Transactional(readOnly = true)
    public Optional<Condition> getCondition(String tenantId, String conditionId) {
        UUID uuid = parseUuid(conditionId);
        Condition cached = cacheGet(tenantId, conditionId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return conditionRepository.findByTenantIdAndId(tenantId, uuid)
                .map(entity -> {
                    Condition parsed = fromEntity(entity);
                    cachePut(tenantId, conditionId, parsed);
                    return parsed;
                });
    }

    @Transactional
    public Condition updateCondition(String tenantId, String conditionId,
                                    Condition condition, String updatedBy) {
        UUID uuid = parseUuid(conditionId);
        validateCondition(condition);

        condition.setId(conditionId);
        UUID patientId = extractPatientId(condition);

        ConditionEntity entity = conditionRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new ConditionNotFoundException(conditionId));

        ConditionEntity updated = entity.toBuilder()
                .resourceJson(JSON_PARSER.encodeResourceToString(condition))
                .patientId(patientId)
                .code(extractCode(condition))
                .codeSystem(extractCodeSystem(condition))
                .codeDisplay(extractCodeDisplay(condition))
                .category(extractCategory(condition))
                .clinicalStatus(extractClinicalStatus(condition))
                .verificationStatus(extractVerificationStatus(condition))
                .severity(extractSeverity(condition))
                .onsetDate(extractOnsetDate(condition))
                .abatementDate(extractAbatementDate(condition))
                .recordedDate(extractRecordedDate(condition))
                .lastModifiedAt(Instant.now())
                .build();

        ConditionEntity persisted = conditionRepository.save(updated);
        Condition savedCondition = fromEntity(persisted);
        applyMeta(savedCondition, persisted);

        cachePut(tenantId, conditionId, savedCondition);
        kafkaTemplate.send("fhir.conditions.updated", conditionId,
                new ConditionEvent(conditionId, tenantId, patientId.toString(),
                        "UPDATED", Instant.now(), updatedBy));

        return savedCondition;
    }

    @Transactional
    public void deleteCondition(String tenantId, String conditionId, String deletedBy) {
        UUID uuid = parseUuid(conditionId);
        ConditionEntity entity = conditionRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new ConditionNotFoundException(conditionId));

        conditionRepository.delete(entity);
        cacheEvict(tenantId, conditionId);
        kafkaTemplate.send("fhir.conditions.deleted", conditionId,
                new ConditionEvent(conditionId, tenantId, entity.getPatientId().toString(),
                        "DELETED", Instant.now(), deletedBy));
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchConditionsByPatient(
            String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = parseUuid(patientId);
        Page<ConditionEntity> entities = conditionRepository
                .findByTenantIdAndPatientIdOrderByRecordedDateDesc(tenantId, patientUuid, pageable);

        return createBundle(entities.getContent(), entities.getTotalElements());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchConditionsByPatientAndCode(
            String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository
                .findByTenantIdAndPatientIdAndCodeOrderByRecordedDateDesc(
                        tenantId, patientUuid, code);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchConditionsByPatientAndCategory(
            String tenantId, String patientId, String category) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository
                .findByTenantIdAndPatientIdAndCategoryOrderByRecordedDateDesc(
                        tenantId, patientUuid, category);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getActiveConditionsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository.findActiveConditionsByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getChronicConditionsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository.findChronicConditionsByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getDiagnosesByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository.findDiagnosesByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getProblemListByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<ConditionEntity> entities = conditionRepository.findProblemListByPatient(
                tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveCondition(String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        return conditionRepository.hasActiveCondition(tenantId, patientUuid, code);
    }

    private org.hl7.fhir.r4.model.Bundle createBundle(
            List<ConditionEntity> entities, long total) {
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        entities.stream()
                .map(entity -> {
                    Condition condition = fromEntity(entity);
                    applyMeta(condition, entity);
                    org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry =
                            new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                    entry.setResource(condition);
                    entry.setFullUrl("Condition/" + condition.getId());
                    return entry;
                })
                .forEach(bundle.getEntry()::add);

        return bundle;
    }

    private ConditionEntity toEntity(String tenantId, UUID conditionId, UUID patientId,
                                    Condition condition) {
        return ConditionEntity.builder()
                .id(conditionId)
                .tenantId(tenantId)
                .resourceType(condition.fhirType() != null ? condition.fhirType() : "Condition")
                .resourceJson(JSON_PARSER.encodeResourceToString(condition))
                .patientId(patientId)
                .code(extractCode(condition))
                .codeSystem(extractCodeSystem(condition))
                .codeDisplay(extractCodeDisplay(condition))
                .category(extractCategory(condition))
                .clinicalStatus(extractClinicalStatus(condition))
                .verificationStatus(extractVerificationStatus(condition))
                .severity(extractSeverity(condition))
                .onsetDate(extractOnsetDate(condition))
                .abatementDate(extractAbatementDate(condition))
                .recordedDate(extractRecordedDate(condition))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private Condition fromEntity(ConditionEntity entity) {
        Condition condition = (Condition) JSON_PARSER.parseResource(entity.getResourceJson());
        condition.setId(entity.getId().toString());
        return condition;
    }

    private void applyMeta(Condition condition, ConditionEntity entity) {
        condition.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        if (entity.getLastModifiedAt() != null) {
            condition.getMeta().setLastUpdated(Date.from(entity.getLastModifiedAt()));
        }
    }

    private UUID ensureConditionId(Condition condition) {
        if (condition.hasIdElement() && condition.getIdElement().getIdPart() != null) {
            return parseUuid(condition.getIdElement().getIdPart());
        }
        UUID generated = UUID.randomUUID();
        condition.setId(generated.toString());
        return generated;
    }

    private UUID extractPatientId(Condition condition) {
        if (!condition.hasSubject() || !condition.getSubject().hasReference()) {
            throw new ConditionValidationException("Condition must have a subject (patient)");
        }
        String reference = condition.getSubject().getReference();
        String patientId = reference.replace("Patient/", "");
        return parseUuid(patientId);
    }

    private String extractCode(Condition condition) {
        if (!condition.hasCode() || !condition.getCode().hasCoding() ||
                condition.getCode().getCoding().isEmpty()) {
            return null;
        }
        Coding coding = condition.getCode().getCodingFirstRep();
        return coding.getCode();
    }

    private String extractCodeSystem(Condition condition) {
        if (!condition.hasCode() || !condition.getCode().hasCoding() ||
                condition.getCode().getCoding().isEmpty()) {
            return null;
        }
        Coding coding = condition.getCode().getCodingFirstRep();
        return coding.getSystem();
    }

    private String extractCodeDisplay(Condition condition) {
        if (!condition.hasCode() || !condition.getCode().hasCoding() ||
                condition.getCode().getCoding().isEmpty()) {
            return null;
        }
        Coding coding = condition.getCode().getCodingFirstRep();
        return coding.getDisplay();
    }

    private String extractCategory(Condition condition) {
        if (!condition.hasCategory() || condition.getCategory().isEmpty()) {
            return null;
        }
        CodeableConcept category = condition.getCategoryFirstRep();
        if (!category.hasCoding() || category.getCoding().isEmpty()) {
            return null;
        }
        return category.getCodingFirstRep().getCode();
    }

    private String extractClinicalStatus(Condition condition) {
        if (condition.hasClinicalStatus() && condition.getClinicalStatus().hasCoding() &&
                !condition.getClinicalStatus().getCoding().isEmpty()) {
            return condition.getClinicalStatus().getCodingFirstRep().getCode();
        }
        return null;
    }

    private String extractVerificationStatus(Condition condition) {
        if (condition.hasVerificationStatus() && condition.getVerificationStatus().hasCoding() &&
                !condition.getVerificationStatus().getCoding().isEmpty()) {
            return condition.getVerificationStatus().getCodingFirstRep().getCode();
        }
        return null;
    }

    private String extractSeverity(Condition condition) {
        if (condition.hasSeverity() && condition.getSeverity().hasCoding() &&
                !condition.getSeverity().getCoding().isEmpty()) {
            return condition.getSeverity().getCodingFirstRep().getCode();
        }
        return null;
    }

    private LocalDate extractOnsetDate(Condition condition) {
        if (condition.hasOnsetDateTimeType()) {
            DateTimeType dateTime = condition.getOnsetDateTimeType();
            return dateTime.getValue().toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        }
        return null;
    }

    private LocalDate extractAbatementDate(Condition condition) {
        if (condition.hasAbatementDateTimeType()) {
            DateTimeType dateTime = condition.getAbatementDateTimeType();
            return dateTime.getValue().toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        }
        return null;
    }

    private LocalDate extractRecordedDate(Condition condition) {
        if (condition.hasRecordedDate()) {
            return condition.getRecordedDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        }
        return LocalDate.now();
    }

    private void validateCondition(Condition condition) {
        if (!condition.hasSubject()) {
            throw new ConditionValidationException("Condition must have a subject");
        }
        if (!condition.hasCode()) {
            throw new ConditionValidationException("Condition must have a code");
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ConditionValidationException("ID must be a valid UUID");
        }
    }

    private void cachePut(String tenantId, String conditionId, Condition condition) {
        if (cache != null) {
            cache.put(cacheKey(tenantId, conditionId), condition);
        }
    }

    private Condition cacheGet(String tenantId, String conditionId) {
        if (cache == null) {
            return null;
        }
        return cache.get(cacheKey(tenantId, conditionId), Condition.class);
    }

    private void cacheEvict(String tenantId, String conditionId) {
        if (cache != null) {
            cache.evict(cacheKey(tenantId, conditionId));
        }
    }

    private String cacheKey(String tenantId, String conditionId) {
        return tenantId + ":cond:" + conditionId;
    }

    public record ConditionEvent(String id, String tenantId, String patientId, String type,
                                 Instant occurredAt, String actor) {
    }

    public static class ConditionValidationException extends RuntimeException {
        public ConditionValidationException(String message) {
            super(message);
        }
    }

    public static class ConditionNotFoundException extends RuntimeException {
        public ConditionNotFoundException(String id) {
            super("Condition not found: " + id);
        }
    }
}
