package com.healthdata.fhir.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CodeableConcept;
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

import com.healthdata.fhir.persistence.CarePlanEntity;
import com.healthdata.fhir.persistence.CarePlanRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR CarePlan resources.
 * Handles care coordination plans with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class CarePlanService {

    private static final Logger log = LoggerFactory.getLogger(CarePlanService.class);
    private static final String CACHE_NAME = "fhir-care-plans";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final CarePlanRepository carePlanRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new CarePlan resource
     */
    @Transactional
    public CarePlan createCarePlan(String tenantId, CarePlan carePlan, String createdBy) {
        log.debug("Creating care plan for tenant: {}", tenantId);

        // Ensure ID is set
        if (carePlan.getId() == null || carePlan.getId().isEmpty()) {
            carePlan.setId(UUID.randomUUID().toString());
        }

        // Validate patient reference
        UUID patientId = extractPatientId(carePlan);
        if (patientId == null) {
            throw new IllegalArgumentException("CarePlan must have a subject (patient) reference");
        }

        // Convert to entity
        CarePlanEntity entity = toEntity(carePlan, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        // Save
        entity = carePlanRepository.save(entity);
        log.info("Created care plan: id={}, tenant={}, patient={}, title={}",
                entity.getId(), tenantId, patientId, entity.getTitle());

        // Publish event
        publishEvent(entity, "created", createdBy);

        return carePlan;
    }

    /**
     * Get CarePlan by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<CarePlan> getCarePlan(String tenantId, UUID id) {
        log.debug("Fetching care plan: tenant={}, id={}", tenantId, id);
        return carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .map(this::toCarePlan);
    }

    /**
     * Update an existing CarePlan
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public CarePlan updateCarePlan(String tenantId, UUID id, CarePlan carePlan, String updatedBy) {
        log.debug("Updating care plan: tenant={}, id={}", tenantId, id);

        CarePlanEntity existing = carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("CarePlan not found: " + id));

        // Update entity
        carePlan.setId(id.toString());
        CarePlanEntity updated = toEntity(carePlan, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        carePlanRepository.save(updated);
        log.info("Updated care plan: id={}, tenant={}", id, tenantId);

        // Publish event
        publishEvent(updated, "updated", updatedBy);

        return carePlan;
    }

    /**
     * Soft delete a CarePlan
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteCarePlan(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting care plan: tenant={}, id={}", tenantId, id);

        CarePlanEntity entity = carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("CarePlan not found: " + id));

        entity.setDeletedAt(Instant.now());
        entity.setLastModifiedBy(deletedBy);
        carePlanRepository.save(entity);

        log.info("Deleted care plan: id={}, tenant={}", id, tenantId);
        publishEvent(entity, "deleted", deletedBy);
    }

    /**
     * Get all care plans for a patient
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByPatient(String tenantId, UUID patientId) {
        log.debug("Fetching care plans for patient: tenant={}, patient={}", tenantId, patientId);
        return carePlanRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenantId, patientId)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get active care plans for a patient
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getActiveCarePlans(String tenantId, UUID patientId) {
        log.debug("Fetching active care plans for patient: tenant={}, patient={}", tenantId, patientId);
        return carePlanRepository.findActiveCarePlansForPatient(tenantId, patientId, Instant.now())
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get primary (top-level) care plans for a patient
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getPrimaryCarePlans(String tenantId, UUID patientId) {
        log.debug("Fetching primary care plans for patient: tenant={}, patient={}", tenantId, patientId);
        return carePlanRepository.findPrimaryCarePlansForPatient(tenantId, patientId)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get care plans for an encounter
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByEncounter(String tenantId, UUID encounterId) {
        log.debug("Fetching care plans for encounter: tenant={}, encounter={}", tenantId, encounterId);
        return carePlanRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenantId, encounterId)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get care plans by category
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByCategory(String tenantId, UUID patientId, String categoryCode) {
        log.debug("Fetching care plans by category: tenant={}, patient={}, category={}", tenantId, patientId, categoryCode);
        return carePlanRepository.findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(tenantId, patientId, categoryCode)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get care plans addressing a specific condition
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByCondition(String tenantId, UUID conditionId) {
        log.debug("Fetching care plans for condition: tenant={}, condition={}", tenantId, conditionId);
        String conditionRef = "Condition/" + conditionId;
        return carePlanRepository.findByAddresses(tenantId, conditionRef)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get care plans with a specific goal
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByGoal(String tenantId, UUID goalId) {
        log.debug("Fetching care plans for goal: tenant={}, goal={}", tenantId, goalId);
        String goalRef = "Goal/" + goalId;
        return carePlanRepository.findByGoal(tenantId, goalRef)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get child care plans (part of a parent plan)
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getChildCarePlans(String tenantId, UUID parentCarePlanId) {
        log.debug("Fetching child care plans: tenant={}, parent={}", tenantId, parentCarePlanId);
        String parentRef = "CarePlan/" + parentCarePlanId;
        return carePlanRepository.findByTenantIdAndPartOfReferenceAndDeletedAtIsNull(tenantId, parentRef)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get care plans with activities
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansWithActivities(String tenantId, UUID patientId) {
        log.debug("Fetching care plans with activities: tenant={}, patient={}", tenantId, patientId);
        return carePlanRepository.findWithActivities(tenantId, patientId)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Get expiring care plans
     */
    @Transactional(readOnly = true)
    public List<CarePlan> getExpiringCarePlans(String tenantId, Instant startDate, Instant endDate) {
        log.debug("Fetching expiring care plans: tenant={}, between {} and {}", tenantId, startDate, endDate);
        return carePlanRepository.findExpiringCarePlans(tenantId, startDate, endDate)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    /**
     * Search care plans with filters
     */
    @Transactional(readOnly = true)
    public Page<CarePlan> searchCarePlans(String tenantId, UUID patientId, UUID encounterId,
                                           String status, String intent, String categoryCode,
                                           Pageable pageable) {
        log.debug("Searching care plans: tenant={}, patient={}", tenantId, patientId);
        return carePlanRepository.searchCarePlans(tenantId, patientId, encounterId,
                        status, intent, categoryCode, pageable)
                .map(this::toCarePlan);
    }

    /**
     * Search care plans by text
     */
    @Transactional(readOnly = true)
    public List<CarePlan> searchByText(String tenantId, UUID patientId, String searchTerm) {
        log.debug("Searching care plans by text: tenant={}, patient={}, term={}", tenantId, patientId, searchTerm);
        return carePlanRepository.searchByText(tenantId, patientId, searchTerm)
                .stream()
                .map(this::toCarePlan)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private CarePlanEntity toEntity(CarePlan carePlan, String tenantId) {
        UUID id = UUID.fromString(carePlan.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(carePlan);

        return CarePlanEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceJson(json)
                .patientId(extractPatientId(carePlan))
                .encounterId(extractEncounterId(carePlan))
                .status(extractStatus(carePlan))
                .intent(extractIntent(carePlan))
                .title(extractTitle(carePlan))
                .description(extractDescription(carePlan))
                .categoryCode(extractCategoryCode(carePlan))
                .categoryDisplay(extractCategoryDisplay(carePlan))
                .periodStart(extractPeriodStart(carePlan))
                .periodEnd(extractPeriodEnd(carePlan))
                .createdDate(extractCreatedDate(carePlan))
                .authorReference(extractAuthorReference(carePlan))
                .careTeamReferences(extractCareTeamReferences(carePlan))
                .activityCount(extractActivityCount(carePlan))
                .goalReferences(extractGoalReferences(carePlan))
                .addressesReferences(extractAddressesReferences(carePlan))
                .supportingInfoReferences(extractSupportingInfoReferences(carePlan))
                .replacesReference(extractReplacesReference(carePlan))
                .partOfReference(extractPartOfReference(carePlan))
                .build();
    }

    private CarePlan toCarePlan(CarePlanEntity entity) {
        return JSON_PARSER.parseResource(CarePlan.class, entity.getResourceJson());
    }

    // ==================== Field Extraction Methods ====================

    private UUID extractPatientId(CarePlan carePlan) {
        Reference subject = carePlan.getSubject();
        if (subject != null && subject.hasReference()) {
            String ref = subject.getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return UUID.fromString(ref.substring(8));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid patient UUID in care plan: {}", ref);
                }
            }
        }
        return null;
    }

    private UUID extractEncounterId(CarePlan carePlan) {
        if (carePlan.hasEncounter()) {
            Reference encounter = carePlan.getEncounter();
            if (encounter.hasReference() && encounter.getReference().startsWith("Encounter/")) {
                try {
                    return UUID.fromString(encounter.getReference().substring(10));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encounter UUID in care plan: {}", encounter.getReference());
                }
            }
        }
        return null;
    }

    private String extractStatus(CarePlan carePlan) {
        return carePlan.hasStatus() ? carePlan.getStatus().toCode() : null;
    }

    private String extractIntent(CarePlan carePlan) {
        return carePlan.hasIntent() ? carePlan.getIntent().toCode() : null;
    }

    private String extractTitle(CarePlan carePlan) {
        return carePlan.hasTitle() ? carePlan.getTitle() : null;
    }

    private String extractDescription(CarePlan carePlan) {
        return carePlan.hasDescription() ? carePlan.getDescription() : null;
    }

    private String extractCategoryCode(CarePlan carePlan) {
        if (carePlan.hasCategory() && !carePlan.getCategory().isEmpty()) {
            CodeableConcept category = carePlan.getCategoryFirstRep();
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractCategoryDisplay(CarePlan carePlan) {
        if (carePlan.hasCategory() && !carePlan.getCategory().isEmpty()) {
            CodeableConcept category = carePlan.getCategoryFirstRep();
            if (category.hasText()) {
                return category.getText();
            }
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private Instant extractPeriodStart(CarePlan carePlan) {
        if (carePlan.hasPeriod() && carePlan.getPeriod().hasStart()) {
            return carePlan.getPeriod().getStart().toInstant();
        }
        return null;
    }

    private Instant extractPeriodEnd(CarePlan carePlan) {
        if (carePlan.hasPeriod() && carePlan.getPeriod().hasEnd()) {
            return carePlan.getPeriod().getEnd().toInstant();
        }
        return null;
    }

    private Instant extractCreatedDate(CarePlan carePlan) {
        if (carePlan.hasCreated()) {
            return carePlan.getCreated().toInstant();
        }
        return null;
    }

    private String extractAuthorReference(CarePlan carePlan) {
        if (carePlan.hasAuthor()) {
            return carePlan.getAuthor().getReference();
        }
        return null;
    }

    private String extractCareTeamReferences(CarePlan carePlan) {
        if (carePlan.hasCareTeam() && !carePlan.getCareTeam().isEmpty()) {
            return carePlan.getCareTeam().stream()
                    .filter(Reference::hasReference)
                    .map(Reference::getReference)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private Integer extractActivityCount(CarePlan carePlan) {
        return carePlan.hasActivity() ? carePlan.getActivity().size() : 0;
    }

    private String extractGoalReferences(CarePlan carePlan) {
        if (carePlan.hasGoal() && !carePlan.getGoal().isEmpty()) {
            return carePlan.getGoal().stream()
                    .filter(Reference::hasReference)
                    .map(Reference::getReference)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String extractAddressesReferences(CarePlan carePlan) {
        if (carePlan.hasAddresses() && !carePlan.getAddresses().isEmpty()) {
            return carePlan.getAddresses().stream()
                    .filter(Reference::hasReference)
                    .map(Reference::getReference)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String extractSupportingInfoReferences(CarePlan carePlan) {
        if (carePlan.hasSupportingInfo() && !carePlan.getSupportingInfo().isEmpty()) {
            return carePlan.getSupportingInfo().stream()
                    .filter(Reference::hasReference)
                    .map(Reference::getReference)
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String extractReplacesReference(CarePlan carePlan) {
        if (carePlan.hasReplaces() && !carePlan.getReplaces().isEmpty()) {
            return carePlan.getReplacesFirstRep().getReference();
        }
        return null;
    }

    private String extractPartOfReference(CarePlan carePlan) {
        if (carePlan.hasPartOf() && !carePlan.getPartOf().isEmpty()) {
            return carePlan.getPartOfFirstRep().getReference();
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(CarePlanEntity entity, String eventType, String actor) {
        try {
            CarePlanEvent event = new CarePlanEvent(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getPatientId() != null ? entity.getPatientId().toString() : null,
                    entity.getStatus(),
                    entity.getCategoryCode(),
                    eventType,
                    Instant.now(),
                    actor
            );
            kafkaTemplate.send("fhir.care-plans." + eventType, entity.getId().toString(), event);
            log.debug("Published care plan event: type={}, id={}", eventType, entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish care plan event: type={}, id={}", eventType, entity.getId(), e);
        }
    }

    /**
     * Event record for Kafka publishing
     */
    public record CarePlanEvent(
            String id,
            String tenantId,
            String patientId,
            String status,
            String category,
            String eventType,
            Instant occurredAt,
            String actor
    ) {}
}
