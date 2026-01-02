package com.healthdata.fhir.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
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

import com.healthdata.fhir.persistence.CoverageEntity;
import com.healthdata.fhir.persistence.CoverageRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR Coverage resources.
 * Handles insurance/coverage data with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class CoverageService {

    private static final Logger log = LoggerFactory.getLogger(CoverageService.class);
    private static final String CACHE_NAME = "fhir-coverages";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final CoverageRepository coverageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new Coverage resource
     */
    @Transactional
    public Coverage createCoverage(String tenantId, Coverage coverage, String createdBy) {
        log.debug("Creating coverage for tenant: {}", tenantId);

        // Ensure ID is set
        if (coverage.getId() == null || coverage.getId().isEmpty()) {
            coverage.setId(UUID.randomUUID().toString());
        }

        // Validate patient reference
        UUID patientId = extractPatientId(coverage);
        if (patientId == null) {
            throw new IllegalArgumentException("Coverage must have a beneficiary (patient) reference");
        }

        // Convert to entity
        CoverageEntity entity = toEntity(coverage, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        // Save
        entity = coverageRepository.save(entity);
        log.info("Created coverage: id={}, tenant={}, patient={}", entity.getId(), tenantId, patientId);

        // Publish event
        publishEvent(entity, "created", createdBy);

        return coverage;
    }

    /**
     * Get Coverage by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<Coverage> getCoverage(String tenantId, UUID id) {
        log.debug("Fetching coverage: tenant={}, id={}", tenantId, id);
        return coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .map(this::toCoverage);
    }

    /**
     * Update an existing Coverage
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Coverage updateCoverage(String tenantId, UUID id, Coverage coverage, String updatedBy) {
        log.debug("Updating coverage: tenant={}, id={}", tenantId, id);

        CoverageEntity existing = coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Coverage not found: " + id));

        // Update entity
        coverage.setId(id.toString());
        CoverageEntity updated = toEntity(coverage, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        coverageRepository.save(updated);
        log.info("Updated coverage: id={}, tenant={}", id, tenantId);

        // Publish event
        publishEvent(updated, "updated", updatedBy);

        return coverage;
    }

    /**
     * Soft delete a Coverage
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteCoverage(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting coverage: tenant={}, id={}", tenantId, id);

        CoverageEntity entity = coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Coverage not found: " + id));

        entity.setDeletedAt(Instant.now());
        entity.setLastModifiedBy(deletedBy);
        coverageRepository.save(entity);

        log.info("Deleted coverage: id={}, tenant={}", id, tenantId);
        publishEvent(entity, "deleted", deletedBy);
    }

    /**
     * Get all coverages for a patient
     */
    @Transactional(readOnly = true)
    public List<Coverage> getCoveragesByPatient(String tenantId, UUID patientId) {
        log.debug("Fetching coverages for patient: tenant={}, patient={}", tenantId, patientId);
        return coverageRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(tenantId, patientId)
                .stream()
                .map(this::toCoverage)
                .collect(Collectors.toList());
    }

    /**
     * Get active coverages for a patient
     */
    @Transactional(readOnly = true)
    public List<Coverage> getActiveCoverages(String tenantId, UUID patientId) {
        log.debug("Fetching active coverages for patient: tenant={}, patient={}", tenantId, patientId);
        return coverageRepository.findActiveCoveragesForPatient(tenantId, patientId, Instant.now())
                .stream()
                .map(this::toCoverage)
                .collect(Collectors.toList());
    }

    /**
     * Get primary coverage for a patient
     */
    @Transactional(readOnly = true)
    public Optional<Coverage> getPrimaryCoverage(String tenantId, UUID patientId) {
        log.debug("Fetching primary coverage for patient: tenant={}, patient={}", tenantId, patientId);
        return coverageRepository.findPrimaryCoverage(tenantId, patientId, Instant.now())
                .map(this::toCoverage);
    }

    /**
     * Check if patient has active coverage
     */
    @Transactional(readOnly = true)
    public boolean hasActiveCoverage(String tenantId, UUID patientId) {
        return coverageRepository.hasActiveCoverage(tenantId, patientId, Instant.now());
    }

    /**
     * Search coverages with filters
     */
    @Transactional(readOnly = true)
    public Page<Coverage> searchCoverages(String tenantId, UUID patientId, String status,
                                           String typeCode, String subscriberId, String payorReference,
                                           Pageable pageable) {
        log.debug("Searching coverages: tenant={}, patient={}, status={}", tenantId, patientId, status);
        return coverageRepository.searchCoverages(tenantId, patientId, status, typeCode,
                        subscriberId, payorReference, pageable)
                .map(this::toCoverage);
    }

    /**
     * Get coverages by subscriber ID
     */
    @Transactional(readOnly = true)
    public List<Coverage> getCoveragesBySubscriberId(String tenantId, String subscriberId) {
        log.debug("Fetching coverages by subscriber: tenant={}, subscriber={}", tenantId, subscriberId);
        return coverageRepository.findByTenantIdAndSubscriberIdAndDeletedAtIsNull(tenantId, subscriberId)
                .stream()
                .map(this::toCoverage)
                .collect(Collectors.toList());
    }

    /**
     * Get coverages expiring within a date range
     */
    @Transactional(readOnly = true)
    public List<Coverage> getExpiringCoverages(String tenantId, Instant startDate, Instant endDate) {
        log.debug("Fetching expiring coverages: tenant={}, between {} and {}", tenantId, startDate, endDate);
        return coverageRepository.findExpiringCoverages(tenantId, startDate, endDate)
                .stream()
                .map(this::toCoverage)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private CoverageEntity toEntity(Coverage coverage, String tenantId) {
        UUID id = UUID.fromString(coverage.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(coverage);

        return CoverageEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceJson(json)
                .patientId(extractPatientId(coverage))
                .status(extractStatus(coverage))
                .typeCode(extractTypeCode(coverage))
                .typeDisplay(extractTypeDisplay(coverage))
                .subscriberId(extractSubscriberId(coverage))
                .groupNumber(extractGroupNumber(coverage))
                .payorReference(extractPayorReference(coverage))
                .payorDisplay(extractPayorDisplay(coverage))
                .periodStart(extractPeriodStart(coverage))
                .periodEnd(extractPeriodEnd(coverage))
                .dependent(extractDependent(coverage))
                .relationshipCode(extractRelationshipCode(coverage))
                .coverageOrder(extractOrder(coverage))
                .network(extractNetwork(coverage))
                .build();
    }

    private Coverage toCoverage(CoverageEntity entity) {
        return JSON_PARSER.parseResource(Coverage.class, entity.getResourceJson());
    }

    // ==================== Field Extraction Methods ====================

    private UUID extractPatientId(Coverage coverage) {
        Reference beneficiary = coverage.getBeneficiary();
        if (beneficiary != null && beneficiary.hasReference()) {
            String ref = beneficiary.getReference();
            // Extract ID from "Patient/uuid" format
            if (ref.startsWith("Patient/")) {
                try {
                    return UUID.fromString(ref.substring(8));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid patient UUID in coverage: {}", ref);
                }
            }
        }
        return null;
    }

    private String extractStatus(Coverage coverage) {
        return coverage.hasStatus() ? coverage.getStatus().toCode() : null;
    }

    private String extractTypeCode(Coverage coverage) {
        if (coverage.hasType()) {
            CodeableConcept type = coverage.getType();
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractTypeDisplay(Coverage coverage) {
        if (coverage.hasType()) {
            CodeableConcept type = coverage.getType();
            if (type.hasText()) {
                return type.getText();
            }
            if (type.hasCoding() && !type.getCoding().isEmpty()) {
                return type.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractSubscriberId(Coverage coverage) {
        if (coverage.hasSubscriberId()) {
            return coverage.getSubscriberId();
        }
        // Also check identifiers
        for (Identifier identifier : coverage.getIdentifier()) {
            if ("member".equalsIgnoreCase(identifier.getType().getCodingFirstRep().getCode()) ||
                "subscriber".equalsIgnoreCase(identifier.getType().getCodingFirstRep().getCode())) {
                return identifier.getValue();
            }
        }
        return null;
    }

    private String extractGroupNumber(Coverage coverage) {
        // Group number is typically in Coverage.class.value
        for (Coverage.ClassComponent classComp : coverage.getClass_()) {
            CodeableConcept type = classComp.getType();
            if (type.hasCoding()) {
                for (Coding coding : type.getCoding()) {
                    if ("group".equalsIgnoreCase(coding.getCode())) {
                        return classComp.getValue();
                    }
                }
            }
        }
        return null;
    }

    private String extractPayorReference(Coverage coverage) {
        if (coverage.hasPayor() && !coverage.getPayor().isEmpty()) {
            Reference payor = coverage.getPayorFirstRep();
            return payor.getReference();
        }
        return null;
    }

    private String extractPayorDisplay(Coverage coverage) {
        if (coverage.hasPayor() && !coverage.getPayor().isEmpty()) {
            Reference payor = coverage.getPayorFirstRep();
            return payor.getDisplay();
        }
        return null;
    }

    private Instant extractPeriodStart(Coverage coverage) {
        if (coverage.hasPeriod() && coverage.getPeriod().hasStart()) {
            return coverage.getPeriod().getStart().toInstant();
        }
        return null;
    }

    private Instant extractPeriodEnd(Coverage coverage) {
        if (coverage.hasPeriod() && coverage.getPeriod().hasEnd()) {
            return coverage.getPeriod().getEnd().toInstant();
        }
        return null;
    }

    private String extractDependent(Coverage coverage) {
        return coverage.hasDependent() ? coverage.getDependent() : null;
    }

    private String extractRelationshipCode(Coverage coverage) {
        if (coverage.hasRelationship()) {
            CodeableConcept relationship = coverage.getRelationship();
            if (relationship.hasCoding() && !relationship.getCoding().isEmpty()) {
                return relationship.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private Integer extractOrder(Coverage coverage) {
        return coverage.hasOrder() ? coverage.getOrder() : null;
    }

    private String extractNetwork(Coverage coverage) {
        return coverage.hasNetwork() ? coverage.getNetwork() : null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(CoverageEntity entity, String eventType, String actor) {
        try {
            CoverageEvent event = new CoverageEvent(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getPatientId() != null ? entity.getPatientId().toString() : null,
                    eventType,
                    Instant.now(),
                    actor
            );
            kafkaTemplate.send("fhir.coverages." + eventType, entity.getId().toString(), event);
            log.debug("Published coverage event: type={}, id={}", eventType, entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish coverage event: type={}, id={}", eventType, entity.getId(), e);
        }
    }

    /**
     * Event record for Kafka publishing
     */
    public record CoverageEvent(
            String id,
            String tenantId,
            String patientId,
            String type,
            Instant occurredAt,
            String actor
    ) {}
}
