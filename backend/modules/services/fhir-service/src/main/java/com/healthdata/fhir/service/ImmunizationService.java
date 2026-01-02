package com.healthdata.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.fhir.persistence.ImmunizationEntity;
import com.healthdata.fhir.persistence.ImmunizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ImmunizationService {

    private final ImmunizationRepository immunizationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "immunizations", key = "#tenantId + ':' + #result.idElement.idPart"),
        @CacheEvict(value = "patientImmunizations", key = "#tenantId + ':' + #immunization.patient.referenceElement.idPart")
    })
    public Immunization createImmunization(String tenantId, Immunization immunization, String createdBy) {
        log.info("Creating Immunization for tenant: {}", tenantId);

        if (!immunization.hasId()) {
            immunization.setId(UUID.randomUUID().toString());
        }

        ImmunizationEntity entity = toEntity(tenantId, immunization, createdBy);
        ImmunizationEntity saved = immunizationRepository.save(entity);

        publishAuditEvent(tenantId, "CREATE", saved.getId().toString(), createdBy);

        log.info("Created Immunization: {} for patient: {}", saved.getId(), saved.getPatientId());
        return immunization;
    }

    @Cacheable(value = "immunizations", key = "#tenantId + ':' + #id")
    @Transactional(readOnly = true)
    public Optional<Immunization> getImmunization(String tenantId, String id) {
        log.debug("Fetching Immunization: {} for tenant: {}", id, tenantId);
        return immunizationRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .map(this::toFhir);
    }

    @Caching(evict = {
        @CacheEvict(value = "immunizations", key = "#tenantId + ':' + #id"),
        @CacheEvict(value = "patientImmunizations", key = "#tenantId + ':' + #immunization.patient.referenceElement.idPart")
    })
    @Transactional
    public Immunization updateImmunization(String tenantId, String id, Immunization immunization, String modifiedBy) {
        log.info("Updating Immunization: {} for tenant: {}", id, tenantId);

        ImmunizationEntity existing = immunizationRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Immunization not found: " + id));

        updateEntityFromFhir(existing, immunization, modifiedBy);
        ImmunizationEntity updated = immunizationRepository.save(existing);

        publishAuditEvent(tenantId, "UPDATE", updated.getId().toString(), modifiedBy);

        log.info("Updated Immunization: {}", updated.getId());
        return immunization;
    }

    @Caching(evict = {
        @CacheEvict(value = "immunizations", key = "#tenantId + ':' + #id"),
        @CacheEvict(value = "patientImmunizations", allEntries = true)
    })
    @Transactional
    public void deleteImmunization(String tenantId, String id, String deletedBy) {
        log.info("Deleting Immunization: {} for tenant: {}", id, tenantId);

        ImmunizationEntity entity = immunizationRepository.findByTenantIdAndId(tenantId, UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Immunization not found: " + id));

        immunizationRepository.delete(entity);
        publishAuditEvent(tenantId, "DELETE", id, deletedBy);

        log.info("Deleted Immunization: {}", id);
    }

    @Cacheable(value = "patientImmunizations", key = "#tenantId + ':' + #patientId")
    @Transactional(readOnly = true)
    public Bundle getImmunizationsByPatient(String tenantId, String patientId, Pageable pageable) {
        log.debug("Fetching immunizations for patient: {} in tenant: {}", patientId, tenantId);

        List<ImmunizationEntity> entities = immunizationRepository
                .findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getCompletedImmunizations(String tenantId, String patientId) {
        log.debug("Fetching completed immunizations for patient: {}", patientId);

        List<ImmunizationEntity> entities = immunizationRepository
                .findCompletedImmunizations(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public Bundle getImmunizationsByVaccineCode(String tenantId, String patientId, String vaccineCode) {
        log.debug("Fetching immunizations for vaccine code: {} patient: {}", vaccineCode, patientId);

        List<ImmunizationEntity> entities = immunizationRepository
                .findByVaccineCode(tenantId, UUID.fromString(patientId), vaccineCode);

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public boolean hasImmunization(String tenantId, String patientId, String vaccineCode) {
        log.debug("Checking if patient {} has immunization: {}", patientId, vaccineCode);
        return immunizationRepository.hasImmunization(tenantId, UUID.fromString(patientId), vaccineCode);
    }

    @Transactional(readOnly = true)
    public long countByVaccineCode(String tenantId, String patientId, String vaccineCode) {
        return immunizationRepository.countByVaccineCode(tenantId, UUID.fromString(patientId), vaccineCode);
    }

    @Transactional(readOnly = true)
    public Bundle getVaccineSeries(String tenantId, String patientId, String vaccineCode) {
        log.debug("Fetching vaccine series for: {} patient: {}", vaccineCode, patientId);

        List<ImmunizationEntity> entities = immunizationRepository
                .findVaccineSeries(tenantId, UUID.fromString(patientId), vaccineCode);

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public boolean isSeriesComplete(String tenantId, String patientId, String vaccineCode, int requiredDoses) {
        return immunizationRepository.isSeriesComplete(tenantId, UUID.fromString(patientId), vaccineCode, requiredDoses);
    }

    @Transactional(readOnly = true)
    public Bundle getImmunizationsWithReactions(String tenantId, String patientId) {
        log.debug("Fetching immunizations with reactions for patient: {}", patientId);

        List<ImmunizationEntity> entities = immunizationRepository
                .findImmunizationsWithReactions(tenantId, UUID.fromString(patientId));

        return createBundle(entities);
    }

    @Transactional(readOnly = true)
    public long countCompletedImmunizations(String tenantId, String patientId) {
        return immunizationRepository.countCompletedImmunizations(tenantId, UUID.fromString(patientId));
    }

    // Entity to FHIR conversion
    private Immunization toFhir(ImmunizationEntity entity) {
        if (entity.getFhirResource() != null) {
            return jsonParser.parseResource(Immunization.class, entity.getFhirResource());
        }

        Immunization immunization = new Immunization();
        immunization.setId(entity.getId().toString());

        // Status
        if (entity.getStatus() != null) {
            immunization.setStatus(Immunization.ImmunizationStatus.fromCode(entity.getStatus()));
        }

        // Vaccine code (CVX)
        if (entity.getVaccineCode() != null) {
            immunization.setVaccineCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(entity.getVaccineSystem() != null ? entity.getVaccineSystem() : "http://hl7.org/fhir/sid/cvx")
                            .setCode(entity.getVaccineCode())
                            .setDisplay(entity.getVaccineDisplay())));
        }

        // Patient reference
        immunization.setPatient(new Reference("Patient/" + entity.getPatientId()));

        // Occurrence date
        if (entity.getOccurrenceDate() != null) {
            immunization.setOccurrence(new DateTimeType(
                    Date.from(entity.getOccurrenceDate().atStartOfDay(ZoneId.systemDefault()).toInstant())));
        }

        // Primary source
        if (entity.getPrimarySource() != null) {
            immunization.setPrimarySource(entity.getPrimarySource());
        }

        // Lot number
        if (entity.getLotNumber() != null) {
            immunization.setLotNumber(entity.getLotNumber());
        }

        // Expiration date
        if (entity.getExpirationDate() != null) {
            immunization.setExpirationDate(Date.from(entity.getExpirationDate()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        return immunization;
    }

    // FHIR to Entity conversion
    private ImmunizationEntity toEntity(String tenantId, Immunization immunization, String createdBy) {
        ImmunizationEntity entity = new ImmunizationEntity();
        entity.setId(UUID.fromString(immunization.getIdElement().getIdPart()));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(createdBy);

        updateEntityFromFhir(entity, immunization, createdBy);

        entity.setFhirResource(jsonParser.encodeResourceToString(immunization));

        return entity;
    }

    private void updateEntityFromFhir(ImmunizationEntity entity, Immunization immunization, String modifiedBy) {
        entity.setLastModifiedBy(modifiedBy);

        // Patient reference
        if (immunization.hasPatient()) {
            String patientId = immunization.getPatient().getReferenceElement().getIdPart();
            entity.setPatientId(UUID.fromString(patientId));
        }

        // Status
        if (immunization.hasStatus()) {
            entity.setStatus(immunization.getStatus().toCode());
        }

        // Vaccine code
        if (immunization.hasVaccineCode()) {
            Coding coding = immunization.getVaccineCode().getCodingFirstRep();
            entity.setVaccineCode(coding.getCode());
            entity.setVaccineSystem(coding.getSystem());
            entity.setVaccineDisplay(coding.getDisplay());
        }

        // Occurrence date
        if (immunization.hasOccurrenceDateTimeType()) {
            DateTimeType occurrenceDateTime = immunization.getOccurrenceDateTimeType();
            LocalDate occurrenceDate = occurrenceDateTime.getValue()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            entity.setOccurrenceDate(occurrenceDate);
        }

        // Primary source
        if (immunization.hasPrimarySource()) {
            entity.setPrimarySource(immunization.getPrimarySource());
        }

        // Lot number
        if (immunization.hasLotNumber()) {
            entity.setLotNumber(immunization.getLotNumber());
        }

        // Expiration date
        if (immunization.hasExpirationDate()) {
            LocalDate expirationDate = immunization.getExpirationDate()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            entity.setExpirationDate(expirationDate);
        }

        entity.setFhirResource(jsonParser.encodeResourceToString(immunization));
    }

    private Bundle createBundle(List<ImmunizationEntity> entities) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(entities.size());

        for (ImmunizationEntity entity : entities) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(toFhir(entity));
            entry.setFullUrl("Immunization/" + entity.getId());
        }

        return bundle;
    }

    private void publishAuditEvent(String tenantId, String action, String resourceId, String userId) {
        try {
            String auditEvent = String.format(
                    "{\"tenantId\":\"%s\",\"action\":\"%s\",\"resourceType\":\"Immunization\",\"resourceId\":\"%s\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                    tenantId, action, resourceId, userId, LocalDateTime.now()
            );
            kafkaTemplate.send("audit-events", auditEvent);
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }
}
