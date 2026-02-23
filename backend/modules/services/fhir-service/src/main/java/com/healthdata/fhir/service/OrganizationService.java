package com.healthdata.fhir.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.OrganizationEntity;
import com.healthdata.fhir.persistence.OrganizationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR Organization resources.
 * Handles healthcare organization data with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    private static final String CACHE_NAME = "fhir-organizations";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final OrganizationRepository organizationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ==================== CRUD Operations ====================

    @Transactional
    public Organization createOrganization(String tenantId, Organization organization, String createdBy) {
        log.debug("Creating organization for tenant: {}", tenantId);

        if (organization.getId() == null || organization.getId().isEmpty()) {
            organization.setId(UUID.randomUUID().toString());
        }

        OrganizationEntity entity = toEntity(organization, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        entity = organizationRepository.save(entity);
        log.info("Created organization: id={}, tenant={}, name={}", entity.getId(), tenantId, entity.getName());

        publishEvent(entity, "created", createdBy);
        return organization;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<Organization> getOrganization(String tenantId, UUID id) {
        log.debug("Fetching organization: tenant={}, id={}", tenantId, id);
        return organizationRepository.findByTenantIdAndId(tenantId, id)
                .map(this::toOrganization);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Organization updateOrganization(String tenantId, UUID id, Organization organization, String updatedBy) {
        log.debug("Updating organization: tenant={}, id={}", tenantId, id);

        OrganizationEntity existing = organizationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        organization.setId(id.toString());
        OrganizationEntity updated = toEntity(organization, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        organizationRepository.save(updated);
        log.info("Updated organization: id={}, tenant={}", id, tenantId);

        publishEvent(updated, "updated", updatedBy);
        return organization;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteOrganization(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting organization: tenant={}, id={}", tenantId, id);

        OrganizationEntity entity = organizationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        organizationRepository.delete(entity);
        log.info("Deleted organization: id={}, tenant={}", id, tenantId);

        publishEvent(entity, "deleted", deletedBy);
    }

    // ==================== Search Operations ====================

    @Transactional(readOnly = true)
    public Page<Organization> searchOrganizations(String tenantId, Pageable pageable) {
        return organizationRepository.findByTenantId(tenantId, pageable)
                .map(this::toOrganization);
    }

    @Transactional(readOnly = true)
    public List<Organization> findByName(String tenantId, String name) {
        return organizationRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name)
                .stream()
                .map(this::toOrganization)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Organization> findByIdentifier(String tenantId, String identifier) {
        return organizationRepository.findByTenantIdAndIdentifierValue(tenantId, identifier)
                .map(this::toOrganization);
    }

    @Transactional(readOnly = true)
    public List<Organization> findAll(String tenantId) {
        return organizationRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toOrganization)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private OrganizationEntity toEntity(Organization organization, String tenantId) {
        UUID id = UUID.fromString(organization.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(organization);

        return OrganizationEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceType("Organization")
                .resourceJson(json)
                .name(organization.hasName() ? organization.getName() : null)
                .identifierValue(extractIdentifier(organization))
                .active(organization.hasActive() ? organization.getActive() : true)
                .typeCode(extractTypeCode(organization))
                .typeDisplay(extractTypeDisplay(organization))
                .build();
    }

    private Organization toOrganization(OrganizationEntity entity) {
        return JSON_PARSER.parseResource(Organization.class, entity.getResourceJson());
    }

    // ==================== Field Extraction ====================

    private String extractIdentifier(Organization organization) {
        if (organization.hasIdentifier() && !organization.getIdentifier().isEmpty()) {
            Identifier identifier = organization.getIdentifierFirstRep();
            return identifier.hasValue() ? identifier.getValue() : null;
        }
        return null;
    }

    private String extractTypeCode(Organization organization) {
        if (organization.hasType() && !organization.getType().isEmpty()) {
            CodeableConcept type = organization.getTypeFirstRep();
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractTypeDisplay(Organization organization) {
        if (organization.hasType() && !organization.getType().isEmpty()) {
            CodeableConcept type = organization.getTypeFirstRep();
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(OrganizationEntity entity, String action, String userId) {
        try {
            kafkaTemplate.send("fhir.organizations." + action,
                    entity.getId().toString(),
                    java.util.Map.of(
                            "resourceType", "Organization",
                            "id", entity.getId().toString(),
                            "tenantId", entity.getTenantId(),
                            "action", action,
                            "userId", userId,
                            "timestamp", java.time.Instant.now().toString()
                    ));
        } catch (Exception e) {
            log.warn("Failed to publish organization event: {}", e.getMessage());
        }
    }
}
