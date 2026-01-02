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
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.MedicationRequestEntity;
import com.healthdata.fhir.persistence.MedicationRequestRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class MedicationRequestService {

    private static final String CACHE_NAME = "fhir-medication-requests";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final MedicationRequestRepository medicationRequestRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Cache cache;

    public MedicationRequestService(
            MedicationRequestRepository medicationRequestRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager) {
        this.medicationRequestRepository = medicationRequestRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Transactional
    public MedicationRequest createMedicationRequest(String tenantId, MedicationRequest medicationRequest,
                                                     String createdBy) {
        validateMedicationRequest(medicationRequest);

        UUID requestId = ensureRequestId(medicationRequest);
        medicationRequest.setId(requestId.toString());

        UUID patientId = extractPatientId(medicationRequest);

        MedicationRequestEntity entity = toEntity(tenantId, requestId, patientId, medicationRequest);
        MedicationRequestEntity saved = medicationRequestRepository.save(entity);
        MedicationRequest savedRequest = fromEntity(saved);
        applyMeta(savedRequest, saved);

        cachePut(tenantId, requestId.toString(), savedRequest);
        kafkaTemplate.send("fhir.medication-requests.created", requestId.toString(),
                new MedicationRequestEvent(requestId.toString(), tenantId, patientId.toString(),
                        "CREATED", Instant.now(), createdBy));

        return savedRequest;
    }

    @Transactional(readOnly = true)
    public Optional<MedicationRequest> getMedicationRequest(String tenantId, String requestId) {
        UUID uuid = parseUuid(requestId);
        MedicationRequest cached = cacheGet(tenantId, requestId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return medicationRequestRepository.findByTenantIdAndId(tenantId, uuid)
                .map(entity -> {
                    MedicationRequest parsed = fromEntity(entity);
                    cachePut(tenantId, requestId, parsed);
                    return parsed;
                });
    }

    @Transactional
    public MedicationRequest updateMedicationRequest(String tenantId, String requestId,
                                                    MedicationRequest medicationRequest, String updatedBy) {
        UUID uuid = parseUuid(requestId);
        validateMedicationRequest(medicationRequest);

        medicationRequest.setId(requestId);
        UUID patientId = extractPatientId(medicationRequest);

        MedicationRequestEntity entity = medicationRequestRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new MedicationRequestNotFoundException(requestId));

        MedicationRequestEntity updated = entity.toBuilder()
                .resourceJson(JSON_PARSER.encodeResourceToString(medicationRequest))
                .patientId(patientId)
                .medicationCode(extractMedicationCode(medicationRequest))
                .medicationSystem(extractMedicationSystem(medicationRequest))
                .medicationDisplay(extractMedicationDisplay(medicationRequest))
                .status(medicationRequest.getStatus() != null ? medicationRequest.getStatus().toCode() : null)
                .intent(medicationRequest.getIntent() != null ? medicationRequest.getIntent().toCode() : null)
                .category(extractCategory(medicationRequest))
                .priority(medicationRequest.getPriority() != null ? medicationRequest.getPriority().toCode() : null)
                .authoredOn(extractAuthoredOn(medicationRequest))
                .requesterId(extractRequesterId(medicationRequest))
                .dosageInstruction(extractDosageInstruction(medicationRequest))
                .dispenseQuantity(extractDispenseQuantity(medicationRequest))
                .dispenseUnit(extractDispenseUnit(medicationRequest))
                .numberOfRepeatsAllowed(extractNumberOfRepeatsAllowed(medicationRequest))
                .lastModifiedAt(Instant.now())
                .build();

        MedicationRequestEntity persisted = medicationRequestRepository.save(updated);
        MedicationRequest savedRequest = fromEntity(persisted);
        applyMeta(savedRequest, persisted);

        cachePut(tenantId, requestId, savedRequest);
        kafkaTemplate.send("fhir.medication-requests.updated", requestId,
                new MedicationRequestEvent(requestId, tenantId, patientId.toString(),
                        "UPDATED", Instant.now(), updatedBy));

        return savedRequest;
    }

    @Transactional
    public void deleteMedicationRequest(String tenantId, String requestId, String deletedBy) {
        UUID uuid = parseUuid(requestId);
        MedicationRequestEntity entity = medicationRequestRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new MedicationRequestNotFoundException(requestId));

        medicationRequestRepository.delete(entity);
        cacheEvict(tenantId, requestId);
        kafkaTemplate.send("fhir.medication-requests.deleted", requestId,
                new MedicationRequestEvent(requestId, tenantId, entity.getPatientId().toString(),
                        "DELETED", Instant.now(), deletedBy));
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchMedicationRequestsByPatient(
            String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = parseUuid(patientId);
        Page<MedicationRequestEntity> entities = medicationRequestRepository
                .findByTenantIdAndPatientIdOrderByAuthoredOnDesc(tenantId, patientUuid, pageable);

        return createBundle(entities.getContent(), entities.getTotalElements());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchMedicationRequestsByPatientAndCode(
            String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationRequestEntity> entities = medicationRequestRepository
                .findByTenantIdAndPatientIdAndMedicationCodeOrderByAuthoredOnDesc(
                        tenantId, patientUuid, code);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getActiveRequestsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationRequestEntity> entities = medicationRequestRepository
                .findActiveRequestsByPatient(tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getPrescriptionsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationRequestEntity> entities = medicationRequestRepository
                .findPrescriptionsByPatient(tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getRequestsWithRefills(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationRequestEntity> entities = medicationRequestRepository
                .findRequestsWithRefills(tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMedication(String tenantId, String patientId, String medicationCode) {
        UUID patientUuid = parseUuid(patientId);
        return medicationRequestRepository.hasActiveMedication(tenantId, patientUuid, medicationCode);
    }

    private org.hl7.fhir.r4.model.Bundle createBundle(
            List<MedicationRequestEntity> entities, long total) {
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        entities.stream()
                .map(entity -> {
                    MedicationRequest request = fromEntity(entity);
                    applyMeta(request, entity);
                    org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry =
                            new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                    entry.setResource(request);
                    entry.setFullUrl("MedicationRequest/" + request.getId());
                    return entry;
                })
                .forEach(bundle.getEntry()::add);

        return bundle;
    }

    private MedicationRequestEntity toEntity(String tenantId, UUID requestId, UUID patientId,
                                            MedicationRequest medicationRequest) {
        return MedicationRequestEntity.builder()
                .id(requestId)
                .tenantId(tenantId)
                .resourceType(medicationRequest.fhirType() != null ? medicationRequest.fhirType() : "MedicationRequest")
                .resourceJson(JSON_PARSER.encodeResourceToString(medicationRequest))
                .patientId(patientId)
                .medicationCode(extractMedicationCode(medicationRequest))
                .medicationSystem(extractMedicationSystem(medicationRequest))
                .medicationDisplay(extractMedicationDisplay(medicationRequest))
                .status(medicationRequest.getStatus() != null ? medicationRequest.getStatus().toCode() : null)
                .intent(medicationRequest.getIntent() != null ? medicationRequest.getIntent().toCode() : null)
                .category(extractCategory(medicationRequest))
                .priority(medicationRequest.getPriority() != null ? medicationRequest.getPriority().toCode() : null)
                .authoredOn(extractAuthoredOn(medicationRequest))
                .requesterId(extractRequesterId(medicationRequest))
                .dosageInstruction(extractDosageInstruction(medicationRequest))
                .dispenseQuantity(extractDispenseQuantity(medicationRequest))
                .dispenseUnit(extractDispenseUnit(medicationRequest))
                .numberOfRepeatsAllowed(extractNumberOfRepeatsAllowed(medicationRequest))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private MedicationRequest fromEntity(MedicationRequestEntity entity) {
        MedicationRequest request = (MedicationRequest) JSON_PARSER.parseResource(entity.getResourceJson());
        request.setId(entity.getId().toString());
        return request;
    }

    private void applyMeta(MedicationRequest request, MedicationRequestEntity entity) {
        request.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        if (entity.getLastModifiedAt() != null) {
            request.getMeta().setLastUpdated(Date.from(entity.getLastModifiedAt()));
        }
    }

    private UUID ensureRequestId(MedicationRequest request) {
        if (request.hasIdElement() && request.getIdElement().getIdPart() != null) {
            return parseUuid(request.getIdElement().getIdPart());
        }
        UUID generated = UUID.randomUUID();
        request.setId(generated.toString());
        return generated;
    }

    private UUID extractPatientId(MedicationRequest request) {
        if (!request.hasSubject() || !request.getSubject().hasReference()) {
            throw new MedicationRequestValidationException("MedicationRequest must have a subject (patient)");
        }
        String reference = request.getSubject().getReference();
        String patientId = reference.replace("Patient/", "");
        return parseUuid(patientId);
    }

    private String extractMedicationCode(MedicationRequest request) {
        if (request.hasMedicationCodeableConcept()) {
            CodeableConcept medication = request.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractMedicationSystem(MedicationRequest request) {
        if (request.hasMedicationCodeableConcept()) {
            CodeableConcept medication = request.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getSystem();
            }
        }
        return null;
    }

    private String extractMedicationDisplay(MedicationRequest request) {
        if (request.hasMedicationCodeableConcept()) {
            CodeableConcept medication = request.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractCategory(MedicationRequest request) {
        if (request.hasCategory() && !request.getCategory().isEmpty()) {
            CodeableConcept category = request.getCategoryFirstRep();
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private LocalDateTime extractAuthoredOn(MedicationRequest request) {
        if (request.hasAuthoredOn()) {
            return request.getAuthoredOn().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
        }
        return LocalDateTime.now();
    }

    private String extractRequesterId(MedicationRequest request) {
        if (request.hasRequester() && request.getRequester().hasReference()) {
            return request.getRequester().getReference();
        }
        return null;
    }

    private String extractDosageInstruction(MedicationRequest request) {
        if (request.hasDosageInstruction() && !request.getDosageInstruction().isEmpty()) {
            Dosage dosage = request.getDosageInstructionFirstRep();
            if (dosage.hasText()) {
                return dosage.getText();
            }
        }
        return null;
    }

    private Double extractDispenseQuantity(MedicationRequest request) {
        if (request.hasDispenseRequest() && request.getDispenseRequest().hasQuantity()) {
            Quantity quantity = request.getDispenseRequest().getQuantity();
            if (quantity.hasValue()) {
                return quantity.getValue().doubleValue();
            }
        }
        return null;
    }

    private String extractDispenseUnit(MedicationRequest request) {
        if (request.hasDispenseRequest() && request.getDispenseRequest().hasQuantity()) {
            Quantity quantity = request.getDispenseRequest().getQuantity();
            if (quantity.hasUnit()) {
                return quantity.getUnit();
            }
        }
        return null;
    }

    private Integer extractNumberOfRepeatsAllowed(MedicationRequest request) {
        if (request.hasDispenseRequest() && request.getDispenseRequest().hasNumberOfRepeatsAllowed()) {
            return request.getDispenseRequest().getNumberOfRepeatsAllowed();
        }
        return null;
    }

    private void validateMedicationRequest(MedicationRequest request) {
        if (!request.hasSubject()) {
            throw new MedicationRequestValidationException("MedicationRequest must have a subject");
        }
        if (!request.hasMedicationCodeableConcept() && !request.hasMedicationReference()) {
            throw new MedicationRequestValidationException("MedicationRequest must have a medication");
        }
        if (!request.hasStatus()) {
            throw new MedicationRequestValidationException("MedicationRequest must have a status");
        }
        if (!request.hasIntent()) {
            throw new MedicationRequestValidationException("MedicationRequest must have an intent");
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new MedicationRequestValidationException("ID must be a valid UUID");
        }
    }

    private void cachePut(String tenantId, String requestId, MedicationRequest request) {
        if (cache != null) {
            cache.put(cacheKey(tenantId, requestId), request);
        }
    }

    private MedicationRequest cacheGet(String tenantId, String requestId) {
        if (cache == null) {
            return null;
        }
        return cache.get(cacheKey(tenantId, requestId), MedicationRequest.class);
    }

    private void cacheEvict(String tenantId, String requestId) {
        if (cache != null) {
            cache.evict(cacheKey(tenantId, requestId));
        }
    }

    private String cacheKey(String tenantId, String requestId) {
        return tenantId + ":medrx:" + requestId;
    }

    public record MedicationRequestEvent(String id, String tenantId, String patientId, String type,
                                        Instant occurredAt, String actor) {
    }

    public static class MedicationRequestValidationException extends RuntimeException {
        public MedicationRequestValidationException(String message) {
            super(message);
        }
    }

    public static class MedicationRequestNotFoundException extends RuntimeException {
        public MedicationRequestNotFoundException(String id) {
            super("MedicationRequest not found: " + id);
        }
    }
}
