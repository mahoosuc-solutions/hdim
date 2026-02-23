package com.healthdata.fhir.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.PractitionerRoleEntity;
import com.healthdata.fhir.persistence.PractitionerRoleRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR PractitionerRole resources.
 * Links practitioners to organizations with specific roles and specialties.
 */
@Service
@RequiredArgsConstructor
public class PractitionerRoleService {

    private static final Logger log = LoggerFactory.getLogger(PractitionerRoleService.class);
    private static final String CACHE_NAME = "fhir-practitioner-roles";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final PractitionerRoleRepository practitionerRoleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ==================== CRUD Operations ====================

    @Transactional
    public PractitionerRole createPractitionerRole(String tenantId, PractitionerRole role, String createdBy) {
        log.debug("Creating practitioner role for tenant: {}", tenantId);

        if (role.getId() == null || role.getId().isEmpty()) {
            role.setId(UUID.randomUUID().toString());
        }

        PractitionerRoleEntity entity = toEntity(role, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        entity = practitionerRoleRepository.save(entity);
        log.info("Created practitioner role: id={}, tenant={}, role={}",
                entity.getId(), tenantId, entity.getRoleDisplay());

        publishEvent(entity, "created", createdBy);
        return role;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<PractitionerRole> getPractitionerRole(String tenantId, UUID id) {
        log.debug("Fetching practitioner role: tenant={}, id={}", tenantId, id);
        return practitionerRoleRepository.findByTenantIdAndId(tenantId, id)
                .map(this::toPractitionerRole);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public PractitionerRole updatePractitionerRole(String tenantId, UUID id, PractitionerRole role, String updatedBy) {
        log.debug("Updating practitioner role: tenant={}, id={}", tenantId, id);

        PractitionerRoleEntity existing = practitionerRoleRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("PractitionerRole not found: " + id));

        role.setId(id.toString());
        PractitionerRoleEntity updated = toEntity(role, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        practitionerRoleRepository.save(updated);
        log.info("Updated practitioner role: id={}, tenant={}", id, tenantId);

        publishEvent(updated, "updated", updatedBy);
        return role;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deletePractitionerRole(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting practitioner role: tenant={}, id={}", tenantId, id);

        PractitionerRoleEntity entity = practitionerRoleRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("PractitionerRole not found: " + id));

        practitionerRoleRepository.delete(entity);
        log.info("Deleted practitioner role: id={}, tenant={}", id, tenantId);

        publishEvent(entity, "deleted", deletedBy);
    }

    // ==================== Search Operations ====================

    @Transactional(readOnly = true)
    public Page<PractitionerRole> searchPractitionerRoles(String tenantId, Pageable pageable) {
        return practitionerRoleRepository.findByTenantId(tenantId, pageable)
                .map(this::toPractitionerRole);
    }

    @Transactional(readOnly = true)
    public List<PractitionerRole> findByPractitioner(String tenantId, String practitionerId) {
        return practitionerRoleRepository.findByTenantIdAndPractitionerId(tenantId, practitionerId)
                .stream()
                .map(this::toPractitionerRole)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PractitionerRole> findByRoleCode(String tenantId, String roleCode) {
        return practitionerRoleRepository.findByTenantIdAndRoleCode(tenantId, roleCode)
                .stream()
                .map(this::toPractitionerRole)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PractitionerRole> findByIdentifier(String tenantId, String identifier) {
        return practitionerRoleRepository.findByTenantIdAndIdentifierValue(tenantId, identifier)
                .map(this::toPractitionerRole);
    }

    @Transactional(readOnly = true)
    public List<PractitionerRole> findAll(String tenantId) {
        return practitionerRoleRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toPractitionerRole)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private PractitionerRoleEntity toEntity(PractitionerRole role, String tenantId) {
        UUID id = UUID.fromString(role.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(role);

        return PractitionerRoleEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceType("PractitionerRole")
                .resourceJson(json)
                .practitionerId(extractPractitionerId(role))
                .organizationId(extractOrganizationId(role))
                .roleCode(extractRoleCode(role))
                .roleDisplay(extractRoleDisplay(role))
                .specialtyCode(extractSpecialtyCode(role))
                .specialtyDisplay(extractSpecialtyDisplay(role))
                .identifierValue(extractIdentifier(role))
                .active(role.hasActive() ? role.getActive() : true)
                .build();
    }

    private PractitionerRole toPractitionerRole(PractitionerRoleEntity entity) {
        return JSON_PARSER.parseResource(PractitionerRole.class, entity.getResourceJson());
    }

    // ==================== Field Extraction ====================

    private String extractPractitionerId(PractitionerRole role) {
        if (role.hasPractitioner()) {
            Reference ref = role.getPractitioner();
            if (ref.hasReference()) {
                String refStr = ref.getReference();
                return refStr.contains("/") ? refStr.substring(refStr.lastIndexOf("/") + 1) : refStr;
            }
        }
        return null;
    }

    private String extractOrganizationId(PractitionerRole role) {
        if (role.hasOrganization()) {
            Reference ref = role.getOrganization();
            if (ref.hasReference()) {
                String refStr = ref.getReference();
                return refStr.contains("/") ? refStr.substring(refStr.lastIndexOf("/") + 1) : refStr;
            }
        }
        return null;
    }

    private String extractRoleCode(PractitionerRole role) {
        if (role.hasCode() && !role.getCode().isEmpty()) {
            CodeableConcept code = role.getCodeFirstRep();
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                return code.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractRoleDisplay(PractitionerRole role) {
        if (role.hasCode() && !role.getCode().isEmpty()) {
            CodeableConcept code = role.getCodeFirstRep();
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                return code.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractSpecialtyCode(PractitionerRole role) {
        if (role.hasSpecialty() && !role.getSpecialty().isEmpty()) {
            CodeableConcept specialty = role.getSpecialtyFirstRep();
            if (specialty.hasCoding() && !specialty.getCoding().isEmpty()) {
                return specialty.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractSpecialtyDisplay(PractitionerRole role) {
        if (role.hasSpecialty() && !role.getSpecialty().isEmpty()) {
            CodeableConcept specialty = role.getSpecialtyFirstRep();
            if (specialty.hasCoding() && !specialty.getCoding().isEmpty()) {
                return specialty.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractIdentifier(PractitionerRole role) {
        if (role.hasIdentifier() && !role.getIdentifier().isEmpty()) {
            Identifier identifier = role.getIdentifierFirstRep();
            return identifier.hasValue() ? identifier.getValue() : null;
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(PractitionerRoleEntity entity, String action, String userId) {
        try {
            kafkaTemplate.send("fhir.practitioner-roles." + action,
                    entity.getId().toString(),
                    java.util.Map.of(
                            "resourceType", "PractitionerRole",
                            "id", entity.getId().toString(),
                            "tenantId", entity.getTenantId(),
                            "action", action,
                            "userId", userId,
                            "timestamp", java.time.Instant.now().toString()
                    ));
        } catch (Exception e) {
            log.warn("Failed to publish practitioner role event: {}", e.getMessage());
        }
    }
}
