package com.healthdata.fhir.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.PractitionerEntity;
import com.healthdata.fhir.persistence.PractitionerRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR Practitioner resources.
 * Handles healthcare provider data with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class PractitionerService {

    private static final Logger log = LoggerFactory.getLogger(PractitionerService.class);
    private static final String CACHE_NAME = "fhir-practitioners";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final PractitionerRepository practitionerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ==================== CRUD Operations ====================

    @Transactional
    public Practitioner createPractitioner(String tenantId, Practitioner practitioner, String createdBy) {
        log.debug("Creating practitioner for tenant: {}", tenantId);

        if (practitioner.getId() == null || practitioner.getId().isEmpty()) {
            practitioner.setId(UUID.randomUUID().toString());
        }

        PractitionerEntity entity = toEntity(practitioner, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        entity = practitionerRepository.save(entity);
        log.info("Created practitioner: id={}, tenant={}, name={} {}",
                entity.getId(), tenantId, entity.getGivenName(), entity.getFamilyName());

        publishEvent(entity, "created", createdBy);
        return practitioner;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<Practitioner> getPractitioner(String tenantId, UUID id) {
        log.debug("Fetching practitioner: tenant={}, id={}", tenantId, id);
        return practitionerRepository.findByTenantIdAndId(tenantId, id)
                .map(this::toPractitioner);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Practitioner updatePractitioner(String tenantId, UUID id, Practitioner practitioner, String updatedBy) {
        log.debug("Updating practitioner: tenant={}, id={}", tenantId, id);

        PractitionerEntity existing = practitionerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Practitioner not found: " + id));

        practitioner.setId(id.toString());
        PractitionerEntity updated = toEntity(practitioner, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        practitionerRepository.save(updated);
        log.info("Updated practitioner: id={}, tenant={}", id, tenantId);

        publishEvent(updated, "updated", updatedBy);
        return practitioner;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deletePractitioner(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting practitioner: tenant={}, id={}", tenantId, id);

        PractitionerEntity entity = practitionerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Practitioner not found: " + id));

        practitionerRepository.delete(entity);
        log.info("Deleted practitioner: id={}, tenant={}", id, tenantId);

        publishEvent(entity, "deleted", deletedBy);
    }

    // ==================== Search Operations ====================

    @Transactional(readOnly = true)
    public Page<Practitioner> searchPractitioners(String tenantId, Pageable pageable) {
        return practitionerRepository.findByTenantId(tenantId, pageable)
                .map(this::toPractitioner);
    }

    @Transactional(readOnly = true)
    public List<Practitioner> findByName(String tenantId, String name) {
        return practitionerRepository.findByTenantIdAndFamilyNameContainingIgnoreCase(tenantId, name)
                .stream()
                .map(this::toPractitioner)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Practitioner> findByIdentifier(String tenantId, String identifier) {
        return practitionerRepository.findByTenantIdAndIdentifierValue(tenantId, identifier)
                .map(this::toPractitioner);
    }

    @Transactional(readOnly = true)
    public List<Practitioner> findAll(String tenantId) {
        return practitionerRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toPractitioner)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private PractitionerEntity toEntity(Practitioner practitioner, String tenantId) {
        UUID id = UUID.fromString(practitioner.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(practitioner);

        return PractitionerEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceType("Practitioner")
                .resourceJson(json)
                .familyName(extractFamilyName(practitioner))
                .givenName(extractGivenName(practitioner))
                .prefix(extractPrefix(practitioner))
                .identifierValue(extractIdentifier(practitioner))
                .active(practitioner.hasActive() ? practitioner.getActive() : true)
                .qualificationCode(extractQualificationCode(practitioner))
                .build();
    }

    private Practitioner toPractitioner(PractitionerEntity entity) {
        return JSON_PARSER.parseResource(Practitioner.class, entity.getResourceJson());
    }

    // ==================== Field Extraction ====================

    private String extractFamilyName(Practitioner practitioner) {
        if (practitioner.hasName() && !practitioner.getName().isEmpty()) {
            HumanName name = practitioner.getNameFirstRep();
            return name.hasFamily() ? name.getFamily() : null;
        }
        return null;
    }

    private String extractGivenName(Practitioner practitioner) {
        if (practitioner.hasName() && !practitioner.getName().isEmpty()) {
            HumanName name = practitioner.getNameFirstRep();
            if (name.hasGiven() && !name.getGiven().isEmpty()) {
                return name.getGiven().get(0).getValue();
            }
        }
        return null;
    }

    private String extractPrefix(Practitioner practitioner) {
        if (practitioner.hasName() && !practitioner.getName().isEmpty()) {
            HumanName name = practitioner.getNameFirstRep();
            if (name.hasPrefix() && !name.getPrefix().isEmpty()) {
                return name.getPrefix().get(0).getValue();
            }
        }
        return null;
    }

    private String extractIdentifier(Practitioner practitioner) {
        if (practitioner.hasIdentifier() && !practitioner.getIdentifier().isEmpty()) {
            Identifier identifier = practitioner.getIdentifierFirstRep();
            return identifier.hasValue() ? identifier.getValue() : null;
        }
        return null;
    }

    private String extractQualificationCode(Practitioner practitioner) {
        if (practitioner.hasQualification() && !practitioner.getQualification().isEmpty()) {
            var qualification = practitioner.getQualificationFirstRep();
            if (qualification.hasCode() && qualification.getCode().hasCoding()) {
                return qualification.getCode().getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(PractitionerEntity entity, String action, String userId) {
        try {
            kafkaTemplate.send("fhir.practitioners." + action,
                    entity.getId().toString(),
                    java.util.Map.of(
                            "resourceType", "Practitioner",
                            "id", entity.getId().toString(),
                            "tenantId", entity.getTenantId(),
                            "action", action,
                            "userId", userId,
                            "timestamp", java.time.Instant.now().toString()
                    ));
        } catch (Exception e) {
            log.warn("Failed to publish practitioner event: {}", e.getMessage());
        }
    }
}
