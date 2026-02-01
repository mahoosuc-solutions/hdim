package com.healthdata.clinicalworkflow.api.v1.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdata.clinicalworkflow.api.v1.dto.ChecklistItemResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.ChecklistProgressResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.ChecklistResponse;
import com.healthdata.clinicalworkflow.application.PreVisitChecklistService;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Mapper for converting PreVisitChecklistEntity to response DTOs.
 *
 * Handles type conversions:
 * - UUID → String (for patientId)
 * - Instant → LocalDateTime (for timestamps)
 * - BigDecimal → Double (for completion percentage)
 * - Boolean checklist items → ChecklistItemResponse list
 * - JsonNode custom items → ChecklistItemResponse list
 * - Progress calculation and aggregation
 *
 * HIPAA Compliance: No PHI caching - stateless mapper only
 * Multi-tenant: Preserves tenant_id for response filtering
 */
@Component
public class PreVisitChecklistMapper {

    /**
     * Convert PreVisitChecklistEntity to ChecklistResponse DTO
     *
     * @param entity PreVisitChecklistEntity from database
     * @return ChecklistResponse for API response
     */
    public ChecklistResponse toChecklistResponse(PreVisitChecklistEntity entity) {
        if (entity == null) {
            return null;
        }

        List<ChecklistItemResponse> items = buildChecklistItems(entity);
        String apiStatus = mapChecklistStatus(entity.getStatus());

        return ChecklistResponse.builder()
                .id(entity.getId())
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer if needed
                .encounterId(null)  // Can be derived from appointment if needed
                .appointmentType(entity.getAppointmentType())
                .status(apiStatus)
                .items(items)
                .totalItems(entity.getTotalItems())
                .completedItems(entity.getCompletedItems())
                .completionPercentage(entity.getCompletionPercentage() != null
                        ? entity.getCompletionPercentage().doubleValue()
                        : 0.0)
                .createdAt(instantToLocalDateTime(entity.getCreatedAt()))
                .updatedAt(instantToLocalDateTime(entity.getUpdatedAt()))
                .tenantId(entity.getTenantId())
                .build();
    }

    /**
     * Convert PreVisitChecklistEntity to ChecklistProgressResponse
     * Provides detailed progress statistics
     *
     * @param entity PreVisitChecklistEntity from database
     * @return ChecklistProgressResponse with progress details
     */
    public ChecklistProgressResponse toChecklistProgressResponse(PreVisitChecklistEntity entity) {
        if (entity == null) {
            return null;
        }

        Integer totalItems = entity.getTotalItems();
        Integer completedItems = entity.getCompletedItems();
        Integer incompleteItems = totalItems - completedItems;

        // Standard items are all required (8 standard items)
        Integer requiredItems = 8;
        Integer completedRequiredItems = countCompletedStandardItems(entity);

        double overallPercentage = entity.getCompletionPercentage() != null
                ? entity.getCompletionPercentage().doubleValue()
                : 0.0;

        double requiredPercentage = requiredItems > 0
                ? (completedRequiredItems * 100.0) / requiredItems
                : 0.0;

        boolean allRequiredComplete = completedRequiredItems.equals(requiredItems);
        boolean fullyComplete = completedItems.equals(totalItems);

        // Build category maps
        Map<String, Integer> completionByCategory = new HashMap<>();
        Map<String, Integer> totalByCategory = new HashMap<>();

        completionByCategory.put("ADMINISTRATIVE", countCompletedAdministrative(entity));
        completionByCategory.put("CLINICAL", countCompletedClinical(entity));
        completionByCategory.put("CUSTOM", countCompletedCustomItems(entity));

        totalByCategory.put("ADMINISTRATIVE", 4);  // verify_insurance, update_demographics, obtain_consent, prepare_vitals
        totalByCategory.put("CLINICAL", 4);  // review_medical_history, review_medications, review_allergies, review_care_gaps
        totalByCategory.put("CUSTOM", countCustomItems(entity));

        return ChecklistProgressResponse.builder()
                .totalItems(totalItems)
                .completedItems(completedItems)
                .incompleteItems(incompleteItems)
                .requiredItems(requiredItems)
                .completedRequiredItems(completedRequiredItems)
                .overallCompletionPercentage(overallPercentage)
                .requiredCompletionPercentage(requiredPercentage)
                .allRequiredComplete(allRequiredComplete)
                .fullyComplete(fullyComplete)
                .completionByCategory(completionByCategory)
                .totalByCategory(totalByCategory)
                .build();
    }

    /**
     * Convert PreVisitChecklistService.ChecklistProgress to ChecklistProgressResponse
     * Overloaded method for service-layer progress object
     *
     * @param progress ChecklistProgress from service layer
     * @return ChecklistProgressResponse with progress details
     */
    public ChecklistProgressResponse toChecklistProgressResponse(PreVisitChecklistService.ChecklistProgress progress) {
        if (progress == null) {
            return null;
        }

        Integer totalItems = progress.getTotalItems();
        Integer completedItems = progress.getCompletedItems();
        Integer incompleteItems = totalItems - completedItems;

        // Standard items are all required (8 standard items)
        Integer requiredItems = 8;

        double overallPercentage = progress.getCompletionPercentage() != null
                ? progress.getCompletionPercentage().doubleValue()
                : 0.0;

        double requiredPercentage = completedItems > 0
                ? (completedItems * 100.0) / requiredItems
                : 0.0;

        boolean allRequiredComplete = completedItems >= requiredItems;
        boolean fullyComplete = completedItems.equals(totalItems);

        return ChecklistProgressResponse.builder()
                .totalItems(totalItems)
                .completedItems(completedItems)
                .incompleteItems(incompleteItems)
                .requiredItems(requiredItems)
                .completedRequiredItems(completedItems)  // Simplified - service doesn't break down required vs optional
                .overallCompletionPercentage(overallPercentage)
                .requiredCompletionPercentage(requiredPercentage)
                .allRequiredComplete(allRequiredComplete)
                .fullyComplete(fullyComplete)
                .completionByCategory(new HashMap<>())  // Service progress doesn't include category breakdown
                .totalByCategory(new HashMap<>())
                .build();
    }

    /**
     * Convert service ChecklistItemResponse to API ChecklistItemResponse
     * Service layer uses its own inner class that needs conversion to API DTO
     *
     * @param serviceItem ChecklistItemResponse from service layer
     * @return ChecklistItemResponse DTO for API
     */
    public ChecklistItemResponse toChecklistItemResponse(
            PreVisitChecklistService.ChecklistItemResponse serviceItem) {
        if (serviceItem == null) {
            return null;
        }

        // Service ChecklistItemResponse only has: name, completed, required
        // Map to API DTO with generated fields for missing data
        return ChecklistItemResponse.builder()
                .id(UUID.randomUUID())  // Service doesn't provide ID
                .itemCode(convertNameToCode(serviceItem.getName()))
                .displayName(serviceItem.getName())
                .description(null)  // Service doesn't provide description
                .category("UNKNOWN")  // Service doesn't provide category
                .required(serviceItem.isRequired())
                .completed(serviceItem.isCompleted())
                .sequenceNumber(0)  // Service doesn't provide sequence
                .completedAt(null)  // Service doesn't track completion time
                .completedBy(null)  // Service doesn't track who completed
                .completionNotes(null)  // Service doesn't provide notes
                .build();
    }

    /**
     * Convert item name to code format (e.g., "Verify Insurance" -> "VERIFY_INSURANCE")
     */
    private String convertNameToCode(String name) {
        if (name == null) {
            return "UNKNOWN";
        }
        return name.toUpperCase().replace(" ", "_");
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Build list of ChecklistItemResponse from entity fields
     * Combines standard boolean fields and custom JSON items
     */
    private List<ChecklistItemResponse> buildChecklistItems(PreVisitChecklistEntity entity) {
        List<ChecklistItemResponse> items = new ArrayList<>();
        int sequence = 1;

        // Administrative items
        items.add(createStandardItem(
                UUID.randomUUID(),
                "VERIFY_INSURANCE",
                "Verify Insurance",
                "Verify patient insurance coverage and benefits",
                "ADMINISTRATIVE",
                true,
                entity.getVerifyInsurance(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "UPDATE_DEMOGRAPHICS",
                "Update Demographics",
                "Confirm and update patient demographics",
                "ADMINISTRATIVE",
                true,
                entity.getUpdateDemographics(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "OBTAIN_CONSENT",
                "Obtain Consent",
                "Collect patient consent for treatment",
                "ADMINISTRATIVE",
                true,
                entity.getObtainConsent(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "PREPARE_VITALS_EQUIPMENT",
                "Prepare Vitals Equipment",
                "Set up equipment for vital signs measurement",
                "ADMINISTRATIVE",
                true,
                entity.getPrepareVitalsEquipment(),
                sequence++
        ));

        // Clinical items
        items.add(createStandardItem(
                UUID.randomUUID(),
                "REVIEW_MEDICAL_HISTORY",
                "Review Medical History",
                "Review patient medical history and prior visits",
                "CLINICAL",
                true,
                entity.getReviewMedicalHistory(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "REVIEW_MEDICATIONS",
                "Review Medications",
                "Review current medications and reconcile",
                "CLINICAL",
                true,
                entity.getReviewMedications(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "REVIEW_ALLERGIES",
                "Review Allergies",
                "Review and confirm patient allergies",
                "CLINICAL",
                true,
                entity.getReviewAllergies(),
                sequence++
        ));

        items.add(createStandardItem(
                UUID.randomUUID(),
                "REVIEW_CARE_GAPS",
                "Review Care Gaps",
                "Review quality measure care gaps",
                "CLINICAL",
                true,
                entity.getReviewCareGaps(),
                sequence++
        ));

        // Custom items from JSON
        if (entity.getCustomItems() != null && entity.getCustomItems().isArray()) {
            for (JsonNode customItem : entity.getCustomItems()) {
                String task = customItem.has("task") ? customItem.get("task").asText() : "Custom Task";
                boolean completed = customItem.has("completed") && customItem.get("completed").asBoolean();

                items.add(createStandardItem(
                        UUID.randomUUID(),
                        "CUSTOM_" + sequence,
                        task,
                        "Custom checklist item",
                        "CUSTOM",
                        false,  // Custom items are not required
                        completed,
                        sequence++
                ));
            }
        }

        return items;
    }

    /**
     * Create a ChecklistItemResponse from standard fields
     */
    private ChecklistItemResponse createStandardItem(
            UUID id,
            String itemCode,
            String displayName,
            String description,
            String category,
            boolean required,
            boolean completed,
            int sequence) {

        return ChecklistItemResponse.builder()
                .id(id)
                .itemCode(itemCode)
                .displayName(displayName)
                .description(description)
                .category(category)
                .required(required)
                .completed(completed)
                .sequenceNumber(sequence)
                .completedAt(null)  // Not tracked in entity, can be enhanced
                .completedBy(null)  // Not tracked in entity, can be enhanced
                .completionNotes(null)
                .build();
    }

    /**
     * Map entity status to API status
     * Entity: pending, in-progress, completed
     * API: NOT_STARTED, IN_PROGRESS, COMPLETED
     */
    private String mapChecklistStatus(String entityStatus) {
        if (entityStatus == null) {
            return "NOT_STARTED";
        }

        return switch (entityStatus.toLowerCase()) {
            case "pending" -> "NOT_STARTED";
            case "in-progress" -> "IN_PROGRESS";
            case "completed" -> "COMPLETED";
            default -> "NOT_STARTED";
        };
    }

    /**
     * Count completed standard items (8 total)
     */
    private Integer countCompletedStandardItems(PreVisitChecklistEntity entity) {
        int count = 0;
        if (Boolean.TRUE.equals(entity.getReviewMedicalHistory())) count++;
        if (Boolean.TRUE.equals(entity.getVerifyInsurance())) count++;
        if (Boolean.TRUE.equals(entity.getUpdateDemographics())) count++;
        if (Boolean.TRUE.equals(entity.getReviewMedications())) count++;
        if (Boolean.TRUE.equals(entity.getReviewAllergies())) count++;
        if (Boolean.TRUE.equals(entity.getPrepareVitalsEquipment())) count++;
        if (Boolean.TRUE.equals(entity.getReviewCareGaps())) count++;
        if (Boolean.TRUE.equals(entity.getObtainConsent())) count++;
        return count;
    }

    /**
     * Count completed administrative items
     */
    private Integer countCompletedAdministrative(PreVisitChecklistEntity entity) {
        int count = 0;
        if (Boolean.TRUE.equals(entity.getVerifyInsurance())) count++;
        if (Boolean.TRUE.equals(entity.getUpdateDemographics())) count++;
        if (Boolean.TRUE.equals(entity.getObtainConsent())) count++;
        if (Boolean.TRUE.equals(entity.getPrepareVitalsEquipment())) count++;
        return count;
    }

    /**
     * Count completed clinical items
     */
    private Integer countCompletedClinical(PreVisitChecklistEntity entity) {
        int count = 0;
        if (Boolean.TRUE.equals(entity.getReviewMedicalHistory())) count++;
        if (Boolean.TRUE.equals(entity.getReviewMedications())) count++;
        if (Boolean.TRUE.equals(entity.getReviewAllergies())) count++;
        if (Boolean.TRUE.equals(entity.getReviewCareGaps())) count++;
        return count;
    }

    /**
     * Count completed custom items from JSON
     */
    private Integer countCompletedCustomItems(PreVisitChecklistEntity entity) {
        if (entity.getCustomItems() == null || !entity.getCustomItems().isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode item : entity.getCustomItems()) {
            if (item.has("completed") && item.get("completed").asBoolean()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count total custom items
     */
    private Integer countCustomItems(PreVisitChecklistEntity entity) {
        if (entity.getCustomItems() == null || !entity.getCustomItems().isArray()) {
            return 0;
        }
        return entity.getCustomItems().size();
    }

    /**
     * Convert UUID to String with null safety
     */
    private String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Convert Instant to LocalDateTime with null safety
     * Uses system default timezone
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;
    }
}
