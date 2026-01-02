package com.healthdata.fhir.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.MedicationAdministrationEntity;
import com.healthdata.fhir.persistence.MedicationAdministrationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Service for managing FHIR MedicationAdministration resources.
 *
 * MedicationAdministration tracks the actual administration of medication to a patient,
 * as opposed to MedicationRequest (prescription) or MedicationDispense (supply).
 */
@Service
public class MedicationAdministrationService {

    private static final String CACHE_NAME = "fhir-medication-administrations";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Cache cache;

    public MedicationAdministrationService(
            MedicationAdministrationRepository medicationAdministrationRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager) {
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Transactional
    public MedicationAdministration createMedicationAdministration(String tenantId,
                                                                    MedicationAdministration medicationAdministration,
                                                                    String createdBy) {
        validateMedicationAdministration(medicationAdministration);

        UUID administrationId = ensureAdministrationId(medicationAdministration);
        medicationAdministration.setId(administrationId.toString());

        UUID patientId = extractPatientId(medicationAdministration);

        MedicationAdministrationEntity entity = toEntity(tenantId, administrationId, patientId, medicationAdministration);
        MedicationAdministrationEntity saved = medicationAdministrationRepository.save(entity);
        MedicationAdministration savedAdministration = fromEntity(saved);
        applyMeta(savedAdministration, saved);

        cachePut(tenantId, administrationId.toString(), savedAdministration);
        kafkaTemplate.send("fhir.medication-administrations.created", administrationId.toString(),
                new MedicationAdministrationEvent(administrationId.toString(), tenantId, patientId.toString(),
                        "CREATED", Instant.now(), createdBy));

        return savedAdministration;
    }

    @Transactional(readOnly = true)
    public Optional<MedicationAdministration> getMedicationAdministration(String tenantId, String administrationId) {
        UUID uuid = parseUuid(administrationId);
        MedicationAdministration cached = cacheGet(tenantId, administrationId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return medicationAdministrationRepository.findByTenantIdAndId(tenantId, uuid)
                .map(entity -> {
                    MedicationAdministration parsed = fromEntity(entity);
                    cachePut(tenantId, administrationId, parsed);
                    return parsed;
                });
    }

    @Transactional
    public MedicationAdministration updateMedicationAdministration(String tenantId, String administrationId,
                                                                   MedicationAdministration medicationAdministration,
                                                                   String updatedBy) {
        UUID uuid = parseUuid(administrationId);
        validateMedicationAdministration(medicationAdministration);

        medicationAdministration.setId(administrationId);
        UUID patientId = extractPatientId(medicationAdministration);

        MedicationAdministrationEntity entity = medicationAdministrationRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new MedicationAdministrationNotFoundException(administrationId));

        MedicationAdministrationEntity updated = entity.toBuilder()
                .resourceJson(JSON_PARSER.encodeResourceToString(medicationAdministration))
                .patientId(patientId)
                .encounterId(extractEncounterId(medicationAdministration))
                .medicationRequestId(extractMedicationRequestId(medicationAdministration))
                .medicationCode(extractMedicationCode(medicationAdministration))
                .medicationSystem(extractMedicationSystem(medicationAdministration))
                .medicationDisplay(extractMedicationDisplay(medicationAdministration))
                .status(medicationAdministration.getStatus() != null ? medicationAdministration.getStatus().toCode() : null)
                .category(extractCategory(medicationAdministration))
                .effectiveDateTime(extractEffectiveDateTime(medicationAdministration))
                .effectivePeriodStart(extractEffectivePeriodStart(medicationAdministration))
                .effectivePeriodEnd(extractEffectivePeriodEnd(medicationAdministration))
                .performerId(extractPerformerId(medicationAdministration))
                .reasonCode(extractReasonCode(medicationAdministration))
                .routeCode(extractRouteCode(medicationAdministration))
                .routeDisplay(extractRouteDisplay(medicationAdministration))
                .siteCode(extractSiteCode(medicationAdministration))
                .siteDisplay(extractSiteDisplay(medicationAdministration))
                .doseValue(extractDoseValue(medicationAdministration))
                .doseUnit(extractDoseUnit(medicationAdministration))
                .rateValue(extractRateValue(medicationAdministration))
                .rateUnit(extractRateUnit(medicationAdministration))
                .lastModifiedAt(Instant.now())
                .build();

        MedicationAdministrationEntity persisted = medicationAdministrationRepository.save(updated);
        MedicationAdministration savedAdministration = fromEntity(persisted);
        applyMeta(savedAdministration, persisted);

        cachePut(tenantId, administrationId, savedAdministration);
        kafkaTemplate.send("fhir.medication-administrations.updated", administrationId,
                new MedicationAdministrationEvent(administrationId, tenantId, patientId.toString(),
                        "UPDATED", Instant.now(), updatedBy));

        return savedAdministration;
    }

    @Transactional
    public void deleteMedicationAdministration(String tenantId, String administrationId, String deletedBy) {
        UUID uuid = parseUuid(administrationId);
        MedicationAdministrationEntity entity = medicationAdministrationRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new MedicationAdministrationNotFoundException(administrationId));

        medicationAdministrationRepository.delete(entity);
        cacheEvict(tenantId, administrationId);
        kafkaTemplate.send("fhir.medication-administrations.deleted", administrationId,
                new MedicationAdministrationEvent(administrationId, tenantId, entity.getPatientId().toString(),
                        "DELETED", Instant.now(), deletedBy));
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchAdministrationsByPatient(
            String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = parseUuid(patientId);
        Page<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(tenantId, patientUuid, pageable);

        return createBundle(entities.getContent(), entities.getTotalElements());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchAdministrationsByPatientAndCode(
            String tenantId, String patientId, String code) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findByTenantIdAndPatientIdAndMedicationCodeOrderByEffectiveDateTimeDesc(
                        tenantId, patientUuid, code);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchAdministrationsByEncounter(String tenantId, String encounterId) {
        UUID encounterUuid = parseUuid(encounterId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findByTenantIdAndEncounterIdOrderByEffectiveDateTimeDesc(tenantId, encounterUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getCompletedAdministrationsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findCompletedAdministrationsByPatient(tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getInProgressAdministrationsByPatient(String tenantId, String patientId) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findInProgressAdministrationsByPatient(tenantId, patientUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getAdministrationsByDateRange(
            String tenantId, String patientId, LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = parseUuid(patientId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findByPatientAndDateRange(tenantId, patientUuid, startDate, endDate);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getAdministrationHistoryByRequest(String tenantId, String requestId) {
        UUID requestUuid = parseUuid(requestId);
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findAdministrationHistoryByRequest(tenantId, requestUuid);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getAdministrationsByLotNumber(String tenantId, String lotNumber) {
        List<MedicationAdministrationEntity> entities = medicationAdministrationRepository
                .findByTenantIdAndLotNumberOrderByEffectiveDateTimeDesc(tenantId, lotNumber);

        return createBundle(entities, entities.size());
    }

    @Transactional(readOnly = true)
    public boolean hasMedicationBeenAdministeredToday(String tenantId, String patientId, String medicationCode) {
        UUID patientUuid = parseUuid(patientId);
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return medicationAdministrationRepository.hasMedicationBeenAdministeredToday(
                tenantId, patientUuid, medicationCode, startOfDay, endOfDay);
    }

    private org.hl7.fhir.r4.model.Bundle createBundle(
            List<MedicationAdministrationEntity> entities, long total) {
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        entities.stream()
                .map(entity -> {
                    MedicationAdministration administration = fromEntity(entity);
                    applyMeta(administration, entity);
                    org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry =
                            new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                    entry.setResource(administration);
                    entry.setFullUrl("MedicationAdministration/" + administration.getId());
                    return entry;
                })
                .forEach(bundle.getEntry()::add);

        return bundle;
    }

    private MedicationAdministrationEntity toEntity(String tenantId, UUID administrationId, UUID patientId,
                                                    MedicationAdministration medicationAdministration) {
        return MedicationAdministrationEntity.builder()
                .id(administrationId)
                .tenantId(tenantId)
                .resourceType(medicationAdministration.fhirType() != null ? medicationAdministration.fhirType() : "MedicationAdministration")
                .resourceJson(JSON_PARSER.encodeResourceToString(medicationAdministration))
                .patientId(patientId)
                .encounterId(extractEncounterId(medicationAdministration))
                .medicationRequestId(extractMedicationRequestId(medicationAdministration))
                .medicationCode(extractMedicationCode(medicationAdministration))
                .medicationSystem(extractMedicationSystem(medicationAdministration))
                .medicationDisplay(extractMedicationDisplay(medicationAdministration))
                .status(medicationAdministration.getStatus() != null ? medicationAdministration.getStatus().toCode() : null)
                .category(extractCategory(medicationAdministration))
                .effectiveDateTime(extractEffectiveDateTime(medicationAdministration))
                .effectivePeriodStart(extractEffectivePeriodStart(medicationAdministration))
                .effectivePeriodEnd(extractEffectivePeriodEnd(medicationAdministration))
                .performerId(extractPerformerId(medicationAdministration))
                .reasonCode(extractReasonCode(medicationAdministration))
                .routeCode(extractRouteCode(medicationAdministration))
                .routeDisplay(extractRouteDisplay(medicationAdministration))
                .siteCode(extractSiteCode(medicationAdministration))
                .siteDisplay(extractSiteDisplay(medicationAdministration))
                .doseValue(extractDoseValue(medicationAdministration))
                .doseUnit(extractDoseUnit(medicationAdministration))
                .rateValue(extractRateValue(medicationAdministration))
                .rateUnit(extractRateUnit(medicationAdministration))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private MedicationAdministration fromEntity(MedicationAdministrationEntity entity) {
        MedicationAdministration administration = (MedicationAdministration) JSON_PARSER.parseResource(entity.getResourceJson());
        administration.setId(entity.getId().toString());
        return administration;
    }

    private void applyMeta(MedicationAdministration administration, MedicationAdministrationEntity entity) {
        administration.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        if (entity.getLastModifiedAt() != null) {
            administration.getMeta().setLastUpdated(Date.from(entity.getLastModifiedAt()));
        }
    }

    private UUID ensureAdministrationId(MedicationAdministration administration) {
        if (administration.hasIdElement() && administration.getIdElement().getIdPart() != null) {
            return parseUuid(administration.getIdElement().getIdPart());
        }
        UUID generated = UUID.randomUUID();
        administration.setId(generated.toString());
        return generated;
    }

    private UUID extractPatientId(MedicationAdministration administration) {
        if (!administration.hasSubject() || !administration.getSubject().hasReference()) {
            throw new MedicationAdministrationValidationException("MedicationAdministration must have a subject (patient)");
        }
        String reference = administration.getSubject().getReference();
        String patientId = reference.replace("Patient/", "");
        return parseUuid(patientId);
    }

    private UUID extractEncounterId(MedicationAdministration administration) {
        if (administration.hasContext() && administration.getContext().hasReference()) {
            String reference = administration.getContext().getReference();
            if (reference.contains("Encounter/")) {
                String encounterId = reference.replace("Encounter/", "");
                try {
                    return parseUuid(encounterId);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private UUID extractMedicationRequestId(MedicationAdministration administration) {
        if (administration.hasRequest() && administration.getRequest().hasReference()) {
            String reference = administration.getRequest().getReference();
            String requestId = reference.replace("MedicationRequest/", "");
            try {
                return parseUuid(requestId);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String extractMedicationCode(MedicationAdministration administration) {
        if (administration.hasMedicationCodeableConcept()) {
            CodeableConcept medication = administration.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractMedicationSystem(MedicationAdministration administration) {
        if (administration.hasMedicationCodeableConcept()) {
            CodeableConcept medication = administration.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getSystem();
            }
        }
        return null;
    }

    private String extractMedicationDisplay(MedicationAdministration administration) {
        if (administration.hasMedicationCodeableConcept()) {
            CodeableConcept medication = administration.getMedicationCodeableConcept();
            if (medication.hasCoding() && !medication.getCoding().isEmpty()) {
                return medication.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractCategory(MedicationAdministration administration) {
        if (administration.hasCategory() && administration.getCategory().hasCoding()) {
            return administration.getCategory().getCodingFirstRep().getCode();
        }
        return null;
    }

    private LocalDateTime extractEffectiveDateTime(MedicationAdministration administration) {
        if (administration.hasEffectiveDateTimeType()) {
            DateTimeType effectiveDateTime = administration.getEffectiveDateTimeType();
            if (effectiveDateTime.getValue() != null) {
                return effectiveDateTime.getValue().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
            }
        }
        return null;
    }

    private LocalDateTime extractEffectivePeriodStart(MedicationAdministration administration) {
        if (administration.hasEffectivePeriod()) {
            Period period = administration.getEffectivePeriod();
            if (period.hasStart()) {
                return period.getStart().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
            }
        }
        return null;
    }

    private LocalDateTime extractEffectivePeriodEnd(MedicationAdministration administration) {
        if (administration.hasEffectivePeriod()) {
            Period period = administration.getEffectivePeriod();
            if (period.hasEnd()) {
                return period.getEnd().toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
            }
        }
        return null;
    }

    private String extractPerformerId(MedicationAdministration administration) {
        if (administration.hasPerformer() && !administration.getPerformer().isEmpty()) {
            MedicationAdministration.MedicationAdministrationPerformerComponent performer =
                    administration.getPerformerFirstRep();
            if (performer.hasActor() && performer.getActor().hasReference()) {
                return performer.getActor().getReference();
            }
        }
        return null;
    }

    private String extractReasonCode(MedicationAdministration administration) {
        if (administration.hasReasonCode() && !administration.getReasonCode().isEmpty()) {
            CodeableConcept reason = administration.getReasonCodeFirstRep();
            if (reason.hasCoding() && !reason.getCoding().isEmpty()) {
                return reason.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractRouteCode(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasRoute()) {
            CodeableConcept route = administration.getDosage().getRoute();
            if (route.hasCoding() && !route.getCoding().isEmpty()) {
                return route.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractRouteDisplay(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasRoute()) {
            CodeableConcept route = administration.getDosage().getRoute();
            if (route.hasCoding() && !route.getCoding().isEmpty()) {
                return route.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractSiteCode(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasSite()) {
            CodeableConcept site = administration.getDosage().getSite();
            if (site.hasCoding() && !site.getCoding().isEmpty()) {
                return site.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractSiteDisplay(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasSite()) {
            CodeableConcept site = administration.getDosage().getSite();
            if (site.hasCoding() && !site.getCoding().isEmpty()) {
                return site.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private Double extractDoseValue(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasDose()) {
            Quantity dose = administration.getDosage().getDose();
            if (dose.hasValue()) {
                return dose.getValue().doubleValue();
            }
        }
        return null;
    }

    private String extractDoseUnit(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasDose()) {
            Quantity dose = administration.getDosage().getDose();
            if (dose.hasUnit()) {
                return dose.getUnit();
            }
        }
        return null;
    }

    private Double extractRateValue(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasRateQuantity()) {
            Quantity rate = administration.getDosage().getRateQuantity();
            if (rate.hasValue()) {
                return rate.getValue().doubleValue();
            }
        }
        return null;
    }

    private String extractRateUnit(MedicationAdministration administration) {
        if (administration.hasDosage() && administration.getDosage().hasRateQuantity()) {
            Quantity rate = administration.getDosage().getRateQuantity();
            if (rate.hasUnit()) {
                return rate.getUnit();
            }
        }
        return null;
    }

    private void validateMedicationAdministration(MedicationAdministration administration) {
        if (!administration.hasSubject()) {
            throw new MedicationAdministrationValidationException("MedicationAdministration must have a subject");
        }
        if (!administration.hasMedicationCodeableConcept() && !administration.hasMedicationReference()) {
            throw new MedicationAdministrationValidationException("MedicationAdministration must have a medication");
        }
        if (!administration.hasStatus()) {
            throw new MedicationAdministrationValidationException("MedicationAdministration must have a status");
        }
        if (!administration.hasEffectiveDateTimeType() && !administration.hasEffectivePeriod()) {
            throw new MedicationAdministrationValidationException("MedicationAdministration must have an effective date/time or period");
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new MedicationAdministrationValidationException("ID must be a valid UUID");
        }
    }

    private void cachePut(String tenantId, String administrationId, MedicationAdministration administration) {
        if (cache != null) {
            cache.put(cacheKey(tenantId, administrationId), administration);
        }
    }

    private MedicationAdministration cacheGet(String tenantId, String administrationId) {
        if (cache == null) {
            return null;
        }
        return cache.get(cacheKey(tenantId, administrationId), MedicationAdministration.class);
    }

    private void cacheEvict(String tenantId, String administrationId) {
        if (cache != null) {
            cache.evict(cacheKey(tenantId, administrationId));
        }
    }

    private String cacheKey(String tenantId, String administrationId) {
        return tenantId + ":medadmin:" + administrationId;
    }

    public record MedicationAdministrationEvent(String id, String tenantId, String patientId, String type,
                                                Instant occurredAt, String actor) {
    }

    public static class MedicationAdministrationValidationException extends RuntimeException {
        public MedicationAdministrationValidationException(String message) {
            super(message);
        }
    }

    public static class MedicationAdministrationNotFoundException extends RuntimeException {
        public MedicationAdministrationNotFoundException(String id) {
            super("MedicationAdministration not found: " + id);
        }
    }
}
