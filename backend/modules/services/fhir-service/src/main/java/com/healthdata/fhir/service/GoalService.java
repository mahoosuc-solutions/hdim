package com.healthdata.fhir.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Goal;
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

import com.healthdata.fhir.persistence.GoalEntity;
import com.healthdata.fhir.persistence.GoalRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing FHIR Goal resources.
 * Handles patient health objectives with caching and event publishing.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    private static final String CACHE_NAME = "fhir-goals";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    private final GoalRepository goalRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new Goal resource
     */
    @Transactional
    public Goal createGoal(String tenantId, Goal goal, String createdBy) {
        log.debug("Creating goal for tenant: {}", tenantId);

        // Ensure ID is set
        if (goal.getId() == null || goal.getId().isEmpty()) {
            goal.setId(UUID.randomUUID().toString());
        }

        // Validate patient reference
        UUID patientId = extractPatientId(goal);
        if (patientId == null) {
            throw new IllegalArgumentException("Goal must have a subject (patient) reference");
        }

        // Convert to entity
        GoalEntity entity = toEntity(goal, tenantId);
        entity.setCreatedBy(createdBy);
        entity.setLastModifiedBy(createdBy);

        // Save
        entity = goalRepository.save(entity);
        log.info("Created goal: id={}, tenant={}, patient={}", entity.getId(), tenantId, patientId);

        // Publish event
        publishEvent(entity, "created", createdBy);

        return goal;
    }

    /**
     * Get Goal by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Optional<Goal> getGoal(String tenantId, UUID id) {
        log.debug("Fetching goal: tenant={}, id={}", tenantId, id);
        return goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .map(this::toGoal);
    }

    /**
     * Update an existing Goal
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public Goal updateGoal(String tenantId, UUID id, Goal goal, String updatedBy) {
        log.debug("Updating goal: tenant={}, id={}", tenantId, id);

        GoalEntity existing = goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));

        // Update entity
        goal.setId(id.toString());
        GoalEntity updated = toEntity(goal, tenantId);
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setLastModifiedBy(updatedBy);

        goalRepository.save(updated);
        log.info("Updated goal: id={}, tenant={}", id, tenantId);

        // Publish event
        publishEvent(updated, "updated", updatedBy);

        return goal;
    }

    /**
     * Soft delete a Goal
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #id")
    public void deleteGoal(String tenantId, UUID id, String deletedBy) {
        log.debug("Deleting goal: tenant={}, id={}", tenantId, id);

        GoalEntity entity = goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));

        entity.setDeletedAt(Instant.now());
        entity.setLastModifiedBy(deletedBy);
        goalRepository.save(entity);

        log.info("Deleted goal: id={}, tenant={}", id, tenantId);
        publishEvent(entity, "deleted", deletedBy);
    }

    /**
     * Get all goals for a patient
     */
    @Transactional(readOnly = true)
    public List<Goal> getGoalsByPatient(String tenantId, UUID patientId) {
        log.debug("Fetching goals for patient: tenant={}, patient={}", tenantId, patientId);
        return goalRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(tenantId, patientId)
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Get active goals for a patient
     */
    @Transactional(readOnly = true)
    public List<Goal> getActiveGoals(String tenantId, UUID patientId) {
        log.debug("Fetching active goals for patient: tenant={}, patient={}", tenantId, patientId);
        return goalRepository.findActiveGoalsForPatient(tenantId, patientId)
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue goals for a patient
     */
    @Transactional(readOnly = true)
    public List<Goal> getOverdueGoals(String tenantId, UUID patientId) {
        log.debug("Fetching overdue goals for patient: tenant={}, patient={}", tenantId, patientId);
        return goalRepository.findOverdueGoals(tenantId, patientId, LocalDate.now())
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Get high priority goals for a patient
     */
    @Transactional(readOnly = true)
    public List<Goal> getHighPriorityGoals(String tenantId, UUID patientId) {
        log.debug("Fetching high priority goals: tenant={}, patient={}", tenantId, patientId);
        return goalRepository.findHighPriorityGoals(tenantId, patientId)
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Get goals addressing a specific condition
     */
    @Transactional(readOnly = true)
    public List<Goal> getGoalsByCondition(String tenantId, UUID conditionId) {
        log.debug("Fetching goals for condition: tenant={}, condition={}", tenantId, conditionId);
        return goalRepository.findByTenantIdAndAddressesConditionIdAndDeletedAtIsNull(tenantId, conditionId)
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Get goals due within a date range
     */
    @Transactional(readOnly = true)
    public List<Goal> getGoalsDueInRange(String tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching goals due between {} and {}: tenant={}", startDate, endDate, tenantId);
        return goalRepository.findGoalsDueInRange(tenantId, startDate, endDate)
                .stream()
                .map(this::toGoal)
                .collect(Collectors.toList());
    }

    /**
     * Search goals with filters
     */
    @Transactional(readOnly = true)
    public Page<Goal> searchGoals(String tenantId, UUID patientId, String lifecycleStatus,
                                   String achievementStatus, String categoryCode, String priority,
                                   Pageable pageable) {
        log.debug("Searching goals: tenant={}, patient={}", tenantId, patientId);
        return goalRepository.searchGoals(tenantId, patientId, lifecycleStatus, achievementStatus,
                        categoryCode, priority, pageable)
                .map(this::toGoal);
    }

    // ==================== Conversion Methods ====================

    private GoalEntity toEntity(Goal goal, String tenantId) {
        UUID id = UUID.fromString(goal.getIdElement().getIdPart());
        String json = JSON_PARSER.encodeResourceToString(goal);

        return GoalEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .resourceJson(json)
                .patientId(extractPatientId(goal))
                .lifecycleStatus(extractLifecycleStatus(goal))
                .achievementStatus(extractAchievementStatus(goal))
                .priorityCode(extractPriorityCode(goal))
                .descriptionText(extractDescriptionText(goal))
                .categoryCode(extractCategoryCode(goal))
                .categoryDisplay(extractCategoryDisplay(goal))
                .startDate(extractStartDate(goal))
                .targetDate(extractTargetDate(goal))
                .statusDate(extractStatusDate(goal))
                .addressesConditionId(extractAddressesConditionId(goal))
                .expressedByReference(extractExpressedByReference(goal))
                .build();
    }

    private Goal toGoal(GoalEntity entity) {
        return JSON_PARSER.parseResource(Goal.class, entity.getResourceJson());
    }

    // ==================== Field Extraction Methods ====================

    private UUID extractPatientId(Goal goal) {
        Reference subject = goal.getSubject();
        if (subject != null && subject.hasReference()) {
            String ref = subject.getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return UUID.fromString(ref.substring(8));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid patient UUID in goal: {}", ref);
                }
            }
        }
        return null;
    }

    private String extractLifecycleStatus(Goal goal) {
        return goal.hasLifecycleStatus() ? goal.getLifecycleStatus().toCode() : null;
    }

    private String extractAchievementStatus(Goal goal) {
        if (goal.hasAchievementStatus()) {
            CodeableConcept achievement = goal.getAchievementStatus();
            if (achievement.hasCoding() && !achievement.getCoding().isEmpty()) {
                return achievement.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractPriorityCode(Goal goal) {
        if (goal.hasPriority()) {
            CodeableConcept priority = goal.getPriority();
            if (priority.hasCoding() && !priority.getCoding().isEmpty()) {
                return priority.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractDescriptionText(Goal goal) {
        if (goal.hasDescription()) {
            CodeableConcept description = goal.getDescription();
            if (description.hasText()) {
                return description.getText();
            }
            if (description.hasCoding() && !description.getCoding().isEmpty()) {
                return description.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private String extractCategoryCode(Goal goal) {
        if (goal.hasCategory() && !goal.getCategory().isEmpty()) {
            CodeableConcept category = goal.getCategoryFirstRep();
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getCode();
            }
        }
        return null;
    }

    private String extractCategoryDisplay(Goal goal) {
        if (goal.hasCategory() && !goal.getCategory().isEmpty()) {
            CodeableConcept category = goal.getCategoryFirstRep();
            if (category.hasText()) {
                return category.getText();
            }
            if (category.hasCoding() && !category.getCoding().isEmpty()) {
                return category.getCodingFirstRep().getDisplay();
            }
        }
        return null;
    }

    private LocalDate extractStartDate(Goal goal) {
        if (goal.hasStartDateType()) {
            DateType startDate = goal.getStartDateType();
            if (startDate.hasValue()) {
                return startDate.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    private LocalDate extractTargetDate(Goal goal) {
        if (goal.hasTarget() && !goal.getTarget().isEmpty()) {
            Goal.GoalTargetComponent target = goal.getTargetFirstRep();
            if (target.hasDueDateType()) {
                DateType dueDate = target.getDueDateType();
                if (dueDate.hasValue()) {
                    return dueDate.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        return null;
    }

    private LocalDate extractStatusDate(Goal goal) {
        if (goal.hasStatusDate()) {
            return goal.getStatusDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private UUID extractAddressesConditionId(Goal goal) {
        if (goal.hasAddresses() && !goal.getAddresses().isEmpty()) {
            Reference addresses = goal.getAddressesFirstRep();
            if (addresses.hasReference() && addresses.getReference().startsWith("Condition/")) {
                try {
                    return UUID.fromString(addresses.getReference().substring(10));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid condition UUID in goal: {}", addresses.getReference());
                }
            }
        }
        return null;
    }

    private String extractExpressedByReference(Goal goal) {
        if (goal.hasExpressedBy()) {
            return goal.getExpressedBy().getReference();
        }
        return null;
    }

    // ==================== Event Publishing ====================

    private void publishEvent(GoalEntity entity, String eventType, String actor) {
        try {
            GoalEvent event = new GoalEvent(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getPatientId() != null ? entity.getPatientId().toString() : null,
                    eventType,
                    Instant.now(),
                    actor
            );
            kafkaTemplate.send("fhir.goals." + eventType, entity.getId().toString(), event);
            log.debug("Published goal event: type={}, id={}", eventType, entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish goal event: type={}, id={}", eventType, entity.getId(), e);
        }
    }

    /**
     * Event record for Kafka publishing
     */
    public record GoalEvent(
            String id,
            String tenantId,
            String patientId,
            String type,
            Instant occurredAt,
            String actor
    ) {}
}
