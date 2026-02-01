package com.healthdata.fhir.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import com.healthdata.fhir.audit.FhirAuditIntegration;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;
import com.healthdata.fhir.validation.PatientValidator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Slf4j
@Service
public class PatientService {

    private static final String CACHE_NAME = "fhir-patients";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final PatientRepository patientRepository;
    private final PatientValidator patientValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Cache cache;
    private final MeterRegistry meterRegistry;
    private final FhirAuditIntegration auditIntegration;

    // Query timing metrics for Issue #137
    private final Timer singlePatientQueryTimer;
    private final Timer batchPatientQueryTimer;
    private final Timer searchQueryTimer;

    public PatientService(
            PatientRepository patientRepository,
            PatientValidator patientValidator,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager,
            MeterRegistry meterRegistry,
            FhirAuditIntegration auditIntegration) {
        this.patientRepository = patientRepository;
        this.patientValidator = patientValidator;
        this.kafkaTemplate = kafkaTemplate;
        this.auditIntegration = auditIntegration;
        this.cache = cacheManager.getCache(CACHE_NAME);
        this.meterRegistry = meterRegistry;

        // Initialize metrics for query timing (Issue #137)
        this.singlePatientQueryTimer = Timer.builder("fhir.patient.query")
                .tag("type", "single")
                .description("Time for single patient query")
                .register(meterRegistry);
        this.batchPatientQueryTimer = Timer.builder("fhir.patient.query")
                .tag("type", "batch")
                .description("Time for batch patient query")
                .register(meterRegistry);
        this.searchQueryTimer = Timer.builder("fhir.patient.query")
                .tag("type", "search")
                .description("Time for patient search query")
                .register(meterRegistry);
    }

    /**
     * Create a patient with upsert semantics (create or update if exists).
     * This method is NOT transactional itself - it delegates to the transactional
     * doCreateOrUpdatePatient method, allowing for proper retry on concurrency exceptions.
     */
    public Patient createPatient(String tenantId, Patient patient, String createdBy) {
        PatientValidator.ValidationResult validation = patientValidator.validate(patient);
        if (!validation.isValid()) {
            throw new PatientValidationException(validation.message());
        }

        UUID patientId = ensurePatientId(patient);
        patient.setId(patientId.toString());

        // Use upsert pattern with retry for concurrent request handling
        // Each retry attempt gets a fresh transaction
        return createOrUpdatePatientWithRetry(tenantId, patient, patientId, createdBy, 3);
    }

    /**
     * Internal upsert implementation with retry logic for concurrent requests.
     * Handles OptimisticLockingFailureException by retrying with the newly-created entity.
     * NOT @Transactional - allows fresh transaction on each retry.
     */
    private Patient createOrUpdatePatientWithRetry(String tenantId, Patient patient, UUID patientId,
                                                    String createdBy, int retriesLeft) {
        try {
            return doCreateOrUpdatePatient(tenantId, patient, patientId, createdBy);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            if (retriesLeft > 0) {
                log.debug("Concurrent modification detected for patient {}, retrying ({} attempts left)",
                         patientId, retriesLeft);
                // Another transaction created the patient - retry to update it instead
                return createOrUpdatePatientWithRetry(tenantId, patient, patientId, createdBy, retriesLeft - 1);
            }
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (retriesLeft > 0 && e.getMessage() != null &&
                (e.getMessage().contains("duplicate key") || e.getMessage().contains("unique constraint"))) {
                log.debug("Duplicate key detected for patient {}, retrying as update ({} attempts left)",
                         patientId, retriesLeft);
                // Duplicate key - another transaction inserted first, retry as update
                return createOrUpdatePatientWithRetry(tenantId, patient, patientId, createdBy, retriesLeft - 1);
            }
            throw e;
        }
    }

    /**
     * Transactional inner method for actual create/update operation.
     * Each call runs in its own transaction, enabling proper retry on failure.
     */
    @Transactional
    public Patient doCreateOrUpdatePatient(String tenantId, Patient patient, UUID patientId, String createdBy) {
        // Check if patient already exists (upsert pattern for demo seeding idempotency)
        Optional<PatientEntity> existingOpt = patientRepository.findByTenantIdAndId(tenantId, patientId);

        PatientEntity saved;
        String eventType;

        if (existingOpt.isPresent()) {
            // Update existing patient - preserve createdAt, update other fields
            PatientEntity existing = existingOpt.get();
            PatientEntity updated = existing.toBuilder()
                    .resourceJson(JSON_PARSER.encodeResourceToString(patient))
                    .firstName(extractFirstName(patient))
                    .lastName(extractLastName(patient))
                    .gender(patient.getGender() != null ? patient.getGender().toCode() : null)
                    .birthDate(patient.getBirthDate() != null
                            ? patient.getBirthDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
                            : null)
                    .lastModifiedAt(Instant.now())
                    .deletedAt(null)  // Clear soft-delete if previously deleted
                    .deletedBy(null)
                    .build();
            saved = patientRepository.save(updated);
            eventType = "UPDATED";
            log.debug("Updated existing patient {} in tenant {}", patientId, tenantId);
        } else {
            // Create new patient
            PatientEntity entity = toEntity(tenantId, patientId, patient);
            saved = patientRepository.save(entity);
            eventType = "CREATED";
            log.debug("Created new patient {} in tenant {}", patientId, tenantId);
        }

        Patient savedPatient = fromEntity(saved);
        applyMeta(savedPatient, saved);

        cachePut(tenantId, patientId.toString(), savedPatient);
        kafkaTemplate.send("fhir.patients." + eventType.toLowerCase(), patientId.toString(),
                new PatientEvent(patientId.toString(), tenantId, eventType, Instant.now(), createdBy));

        return savedPatient;
    }

    @Transactional(readOnly = true)
    public Optional<Patient> getPatient(String tenantId, String patientId) {
        return singlePatientQueryTimer.record(() -> {
            UUID uuid = parsePatientUuid(patientId);
            Patient cached = cacheGet(tenantId, patientId);
            if (cached != null) {
                meterRegistry.counter("fhir.patient.cache", "result", "hit").increment();
                return Optional.of(cached);
            }
            meterRegistry.counter("fhir.patient.cache", "result", "miss").increment();

            // Use findActiveByTenantIdAndId to exclude soft-deleted patients
            Optional<Patient> result = patientRepository.findActiveByTenantIdAndId(tenantId, uuid)
                    .map(entity -> {
                        Patient parsed = fromEntity(entity);
                        cachePut(tenantId, patientId, parsed);
                        
                        // Publish audit event
                        auditIntegration.publishFhirQueryEvent(
                                tenantId, "Patient", patientId,
                                "read", new HashMap<>(), 1, 0, "system"
                        );
                        
                        return parsed;
                    });
            
            if (result.isEmpty()) {
                // Audit failed access attempt
                auditIntegration.publishFhirQueryEvent(
                        tenantId, "Patient", patientId,
                        "read", new HashMap<>(), 0, 0, "system"
                );
            }
            
            return result;
        });
    }

    /**
     * Batch patient lookup for multiple IDs.
     * Issue #137: Optimize FHIR Queries for Primary Care Dashboard
     *
     * This method fetches multiple patients in a single database query,
     * significantly reducing latency compared to N individual queries.
     *
     * @param tenantId The tenant ID for multi-tenant isolation
     * @param patientIds List of patient IDs to fetch
     * @return Map of patientId -> Patient for found patients
     */
    @Transactional(readOnly = true)
    public Map<String, Patient> getPatientsByIds(String tenantId, List<String> patientIds) {
        return batchPatientQueryTimer.record(() -> {
            if (patientIds == null || patientIds.isEmpty()) {
                return new HashMap<>();
            }

            Map<String, Patient> result = new HashMap<>();
            List<UUID> uncachedIds = new ArrayList<>();

            // Check cache first
            for (String patientId : patientIds) {
                Patient cached = cacheGet(tenantId, patientId);
                if (cached != null) {
                    result.put(patientId, cached);
                    meterRegistry.counter("fhir.patient.cache", "result", "hit").increment();
                } else {
                    try {
                        UUID uuid = UUID.fromString(patientId);
                        uncachedIds.add(uuid);
                        meterRegistry.counter("fhir.patient.cache", "result", "miss").increment();
                    } catch (IllegalArgumentException ex) {
                        log.warn("Invalid patient ID format: {}", patientId);
                    }
                }
            }

            // Batch fetch uncached patients
            if (!uncachedIds.isEmpty()) {
                log.debug("Batch fetching {} patients from database", uncachedIds.size());
                List<PatientEntity> entities = patientRepository.findActiveByTenantIdAndIdIn(tenantId, uncachedIds);

                for (PatientEntity entity : entities) {
                    Patient patient = fromEntity(entity);
                    applyMeta(patient, entity);
                    String id = entity.getId().toString();
                    result.put(id, patient);
                    cachePut(tenantId, id, patient);
                }
            }

            log.debug("Batch query returned {} patients (cache hits: {}, db fetches: {})",
                    result.size(), patientIds.size() - uncachedIds.size(), uncachedIds.size());

            return result;
        });
    }

    /**
     * Get patients by IDs and return as FHIR Bundle.
     * Issue #137: Optimize FHIR Queries for Primary Care Dashboard
     */
    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle getPatientsBundleByIds(String tenantId, List<String> patientIds) {
        Map<String, Patient> patients = getPatientsByIds(tenantId, patientIds);

        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal(patients.size());

        patients.values().forEach(patient -> {
            org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry =
                    new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
            entry.setResource(patient);
            entry.setFullUrl("Patient/" + patient.getId());
            bundle.addEntry(entry);
        });

        return bundle;
    }

    @Transactional
    public Patient updatePatient(String tenantId, String patientId, Patient patient, String updatedBy) {
        UUID uuid = parsePatientUuid(patientId);

        Patient entityPatient = patient;
        entityPatient.setId(patientId);

        PatientValidator.ValidationResult validation = patientValidator.validate(entityPatient);
        if (!validation.isValid()) {
            throw new PatientValidationException(validation.message());
        }

        PatientEntity entity = patientRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        PatientEntity updated = entity.toBuilder()
                .resourceJson(JSON_PARSER.encodeResourceToString(entityPatient))
                .firstName(extractFirstName(entityPatient))
                .lastName(extractLastName(entityPatient))
                .gender(entityPatient.getGender() != null ? entityPatient.getGender().toCode() : null)
                .birthDate(entityPatient.getBirthDate() != null
                        ? entityPatient.getBirthDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
                        : null)
                .lastModifiedAt(Instant.now())
                .build();

        PatientEntity persisted = patientRepository.save(updated);
        Patient savedPatient = fromEntity(persisted);
        applyMeta(savedPatient, persisted);

        cachePut(tenantId, patientId, savedPatient);
        kafkaTemplate.send("fhir.patients.updated", patientId,
                new PatientEvent(patientId, tenantId, "UPDATED", Instant.now(), updatedBy));

        return savedPatient;
    }

    @Transactional
    public void deletePatient(String tenantId, String patientId, String deletedBy) {
        UUID uuid = parsePatientUuid(patientId);
        PatientEntity entity = patientRepository.findByTenantIdAndId(tenantId, uuid)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        // Soft delete for HIPAA compliance
        PatientEntity softDeleted = entity.toBuilder()
                .deletedAt(Instant.now())
                .deletedBy(deletedBy)
                .lastModifiedAt(Instant.now())
                .build();

        patientRepository.save(softDeleted);
        cacheEvict(tenantId, patientId);
        kafkaTemplate.send("fhir.patients.deleted", patientId,
                new PatientEvent(patientId, tenantId, "DELETED", Instant.now(), deletedBy));
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Bundle searchPatients(String tenantId, String nameFilter, int count) {
        String effectiveFilter = nameFilter != null ? nameFilter : "";
        List<PatientEntity> entities = patientRepository
                .findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(tenantId, effectiveFilter);

        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
        bundle.setTotal(entities.size());

        entities.stream()
                .limit(Math.max(count, 1))
                .map(entity -> {
                    Patient patient = fromEntity(entity);
                    applyMeta(patient, entity);
                    org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                    entry.setResource(patient);
                    entry.setFullUrl("Patient/" + patient.getId());
                    return entry;
                })
                .forEach(bundle.getEntry()::add);

        return bundle;
    }

    private PatientEntity toEntity(String tenantId, UUID patientId, Patient patient) {
        // Note: Do NOT set version here - leave it null for new entities.
        // The @Version field will be set to 0 by Hibernate on first persist.
        // Setting version=0 explicitly causes Hibernate to think this is a detached entity.
        return PatientEntity.builder()
                .id(patientId)
                .tenantId(tenantId)
                .resourceType(patient.fhirType() != null ? patient.fhirType() : "Patient")
                .resourceJson(JSON_PARSER.encodeResourceToString(patient))
                .firstName(extractFirstName(patient))
                .lastName(extractLastName(patient))
                .gender(patient.getGender() != null ? patient.getGender().toCode() : null)
                .birthDate(patient.getBirthDate() != null
                        ? patient.getBirthDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
                        : null)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                // version intentionally left null - will be set by @PrePersist
                .build();
    }

    private Patient fromEntity(PatientEntity entity) {
        Patient patient = (Patient) JSON_PARSER.parseResource(entity.getResourceJson());
        patient.setId(entity.getId().toString());

        // Enrich patient with entity columns if resource_json is minimal
        // This handles cases where seeding stored minimal JSON but populated columns
        enrichPatientFromEntity(patient, entity);

        return patient;
    }

    /**
     * Enriches a Patient resource with data from entity columns.
     * This is needed when resource_json is minimal (e.g., from demo seeding).
     */
    private void enrichPatientFromEntity(Patient patient, PatientEntity entity) {
        // Add name if not present and entity has name data
        if (!patient.hasName() && (entity.getFirstName() != null || entity.getLastName() != null)) {
            org.hl7.fhir.r4.model.HumanName name = patient.addName();
            name.setUse(org.hl7.fhir.r4.model.HumanName.NameUse.OFFICIAL);
            if (entity.getLastName() != null) {
                name.setFamily(entity.getLastName());
            }
            if (entity.getFirstName() != null) {
                name.addGiven(entity.getFirstName());
            }
        }

        // Add gender if not present
        if (!patient.hasGender() && entity.getGender() != null) {
            try {
                patient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.fromCode(entity.getGender()));
            } catch (Exception e) {
                log.debug("Could not parse gender: {}", entity.getGender());
            }
        }

        // Add birth date if not present
        if (!patient.hasBirthDate() && entity.getBirthDate() != null) {
            patient.setBirthDate(java.sql.Date.valueOf(entity.getBirthDate()));
        }
    }

    private void applyMeta(Patient patient, PatientEntity entity) {
        patient.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        if (entity.getLastModifiedAt() != null) {
            patient.getMeta().setLastUpdated(Date.from(entity.getLastModifiedAt()));
        }
    }

    private UUID ensurePatientId(Patient patient) {
        if (patient.hasIdElement() && patient.getIdElement().getIdPart() != null) {
            return parsePatientUuid(patient.getIdElement().getIdPart());
        }
        UUID generated = UUID.randomUUID();
        patient.setId(generated.toString());
        return generated;
    }

    private UUID parsePatientUuid(String patientId) {
        try {
            return UUID.fromString(patientId);
        } catch (IllegalArgumentException ex) {
            throw new PatientValidationException("Patient id must be a valid UUID");
        }
    }

    private void cachePut(String tenantId, String patientId, Patient patient) {
        if (cache != null) {
            cache.put(cacheKey(tenantId, patientId), patient);
        }
    }

    private Patient cacheGet(String tenantId, String patientId) {
        if (cache == null) {
            return null;
        }
        return cache.get(cacheKey(tenantId, patientId), Patient.class);
    }

    private void cacheEvict(String tenantId, String patientId) {
        if (cache != null) {
            cache.evict(cacheKey(tenantId, patientId));
        }
    }

    private String cacheKey(String tenantId, String patientId) {
        return tenantId + ":" + patientId;
    }

    private String extractFirstName(Patient patient) {
        if (!patient.hasName() || patient.getNameFirstRep().getGiven().isEmpty()) {
            return null;
        }
        return patient.getNameFirstRep().getGiven().get(0).getValue();
    }

    private String extractLastName(Patient patient) {
        if (!patient.hasName() || patient.getNameFirstRep().getFamily() == null) {
            return null;
        }
        return patient.getNameFirstRep().getFamily();
    }

    public record PatientEvent(String id, String tenantId, String type, Instant occurredAt, String actor) {
    }

    public static class PatientValidationException extends RuntimeException {
        public PatientValidationException(String message) {
            super(message);
        }
    }

    public static class PatientNotFoundException extends RuntimeException {
        public PatientNotFoundException(String id) {
            super("Patient not found: " + id);
        }
    }
}
