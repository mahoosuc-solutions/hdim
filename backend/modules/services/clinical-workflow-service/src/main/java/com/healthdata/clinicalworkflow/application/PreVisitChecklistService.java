package com.healthdata.clinicalworkflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.clinicalworkflow.api.v1.dto.CreateChecklistRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.ChecklistItemUpdateRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.CustomChecklistItemRequest;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import com.healthdata.clinicalworkflow.domain.repository.PreVisitChecklistRepository;
import com.healthdata.clinicalworkflow.infrastructure.exception.ResourceNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pre-Visit Checklist Service
 *
 * Manages pre-visit checklist workflow for Medical Assistants:
 * - Create appointment type-specific checklists
 * - Track checklist completion
 * - Add custom checklist items
 * - Calculate completion percentage
 *
 * HIPAA Compliance:
 * - All methods enforce multi-tenant isolation
 * - Audit logging via @Audited annotations (to be added at controller level)
 * - Cache TTL <= 5 minutes for PHI data
 *
 * Integration Points:
 * - FHIR Task: Creates tasks for checklist items
 * - Analytics: Checklist completion metrics
 * - MA Dashboard: Real-time checklist status
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PreVisitChecklistService {

    private final PreVisitChecklistRepository checklistRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create checklist
     *
     * Creates pre-visit checklist for appointment type.
     *
     * @param appointmentType the appointment type
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return created checklist
     */
    @Transactional
    public PreVisitChecklistEntity createChecklist(
            String appointmentType, UUID patientId, String tenantId) {
        log.debug("Creating checklist for appointment type {} for patient {} in tenant {}",
                appointmentType, patientId, tenantId);

        PreVisitChecklistEntity checklist = PreVisitChecklistEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentType(appointmentType)
                .reviewMedicalHistory(false)
                .verifyInsurance(false)
                .updateDemographics(false)
                .reviewMedications(false)
                .reviewAllergies(false)
                .prepareVitalsEquipment(false)
                .reviewCareGaps(false)
                .obtainConsent(false)
                .completionPercentage(BigDecimal.ZERO)
                .status("pending")
                .build();

        PreVisitChecklistEntity saved = checklistRepository.save(checklist);

        log.info("Checklist created: {} for appointment type {} in tenant {}",
                saved.getId(), appointmentType, tenantId);

        return saved;
    }

    /**
     * Create checklist with appointment ID
     *
     * @param appointmentType the appointment type
     * @param patientId the patient ID
     * @param appointmentId the appointment ID
     * @param tenantId the tenant ID
     * @return created checklist
     */
    @Transactional
    public PreVisitChecklistEntity createChecklistForAppointment(
            String appointmentType, UUID patientId, String appointmentId, String tenantId) {
        log.debug("Creating checklist for appointment {} in tenant {}", appointmentId, tenantId);

        PreVisitChecklistEntity checklist = createChecklist(appointmentType, patientId, tenantId);
        checklist.setAppointmentId(appointmentId);

        PreVisitChecklistEntity updated = checklistRepository.save(checklist);

        log.info("Checklist {} linked to appointment {} in tenant {}",
                updated.getId(), appointmentId, tenantId);

        return updated;
    }

    /**
     * Create checklist adapter (5c)
     *
     * Adapter method that processes CreateChecklistRequest and extracts required fields.
     *
     * @param tenantId the tenant ID
     * @param request the create checklist request
     * @param userId the user ID performing the action
     * @return created checklist entity
     * @throws IllegalArgumentException if patient ID format is invalid
     */
    @Transactional
    public PreVisitChecklistEntity createChecklist(
            String tenantId,
            CreateChecklistRequest request,
            String userId) {
        log.debug("Creating checklist for patient {} appointment type {} in tenant {}",
                request.getPatientId(), request.getAppointmentType(), tenantId);

        UUID patientId;
        try {
            patientId = UUID.fromString(request.getPatientId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + request.getPatientId());
        }

        return createChecklistForAppointment(
                request.getAppointmentType(),
                patientId,
                request.getEncounterId(),
                tenantId);
    }

    /**
     * Complete checklist item
     *
     * Marks a checklist item as completed and updates completion percentage.
     *
     * @param checklistId the checklist ID
     * @param itemName the item name
     * @param tenantId the tenant ID
     * @return updated checklist
     */
    @Transactional
    public PreVisitChecklistEntity completeChecklistItem(
            UUID checklistId, String itemName, String tenantId) {
        log.debug("Completing checklist item {} for checklist {} in tenant {}",
                itemName, checklistId, tenantId);

        PreVisitChecklistEntity checklist = checklistRepository
                .findByIdAndTenantId(checklistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found: " + checklistId));

        // Update specific item based on name
        boolean updated = switch (itemName.toLowerCase()) {
            case "reviewmedicalhistory", "review_medical_history" -> {
                checklist.setReviewMedicalHistory(true);
                yield true;
            }
            case "verifyinsurance", "verify_insurance" -> {
                checklist.setVerifyInsurance(true);
                yield true;
            }
            case "updatedemographics", "update_demographics" -> {
                checklist.setUpdateDemographics(true);
                yield true;
            }
            case "reviewmedications", "review_medications" -> {
                checklist.setReviewMedications(true);
                yield true;
            }
            case "reviewallergies", "review_allergies" -> {
                checklist.setReviewAllergies(true);
                yield true;
            }
            case "preparevitalsequipment", "prepare_vitals_equipment" -> {
                checklist.setPrepareVitalsEquipment(true);
                yield true;
            }
            case "reviewcaregaps", "review_care_gaps" -> {
                checklist.setReviewCareGaps(true);
                yield true;
            }
            case "obtainconsent", "obtain_consent" -> {
                checklist.setObtainConsent(true);
                yield true;
            }
            default -> false;
        };

        if (!updated) {
            throw new IllegalArgumentException("Unknown checklist item: " + itemName);
        }

        PreVisitChecklistEntity savedChecklist = checklistRepository.save(checklist);

        log.info("Checklist item {} completed for checklist {} in tenant {}",
                itemName, checklistId, tenantId);

        return savedChecklist;
    }

    /**
     * Complete checklist item adapter (5d)
     *
     * Adapter method that extracts itemCode from request and completes the item.
     *
     * @param tenantId the tenant ID
     * @param checklistId the checklist ID
     * @param request the checklist item update request
     * @param userId the user ID performing the action
     * @return updated checklist entity
     */
    @Transactional
    public PreVisitChecklistEntity completeChecklistItem(
            String tenantId,
            UUID checklistId,
            ChecklistItemUpdateRequest request,
            String userId) {
        log.debug("Completing checklist item {} for checklist {} in tenant {}",
                request.getItemCode(), checklistId, tenantId);

        return completeChecklistItem(checklistId, request.getItemCode(), tenantId);
    }

    /**
     * Get completion status
     *
     * @param checklistId the checklist ID
     * @param tenantId the tenant ID
     * @return completion status
     */
    public ChecklistCompletionStatus getCompletionStatus(UUID checklistId, String tenantId) {
        log.debug("Retrieving completion status for checklist {} in tenant {}",
                checklistId, tenantId);

        PreVisitChecklistEntity checklist = checklistRepository
                .findByIdAndTenantId(checklistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found: " + checklistId));

        return ChecklistCompletionStatus.builder()
                .checklistId(checklistId)
                .status(checklist.getStatus())
                .completionPercentage(checklist.getCompletionPercentage())
                .totalItems(checklist.getTotalItems())
                .completedItems(checklist.getCompletedItems())
                .build();
    }

    /**
     * Get checklist by appointment type
     *
     * @param appointmentType the appointment type
     * @param tenantId the tenant ID
     * @return list of checklists
     */
    @Cacheable(value = "checklistsByType", key = "#tenantId + ':' + #appointmentType")
    public List<PreVisitChecklistEntity> getChecklistByAppointmentType(
            String appointmentType, String tenantId) {
        log.debug("Retrieving checklists for appointment type {} in tenant {}",
                appointmentType, tenantId);

        return checklistRepository.findByAppointmentTypeAndTenant(appointmentType, tenantId);
    }

    /**
     * Get checklist template (5b)
     *
     * Returns a template entity with standard items for the appointment type.
     *
     * @param tenantId the tenant ID
     * @param appointmentType the appointment type
     * @return template checklist entity
     */
    @Cacheable(value = "checklistTemplate", key = "#tenantId + ':' + #appointmentType")
    public PreVisitChecklistEntity getChecklistTemplate(String tenantId, String appointmentType) {
        log.debug("Retrieving checklist template for appointment type {} in tenant {}",
                appointmentType, tenantId);

        return PreVisitChecklistEntity.builder()
                .tenantId(tenantId)
                .appointmentType(appointmentType)
                .reviewMedicalHistory(false)
                .verifyInsurance(false)
                .updateDemographics(false)
                .reviewMedications(false)
                .reviewAllergies(false)
                .prepareVitalsEquipment(false)
                .reviewCareGaps(false)
                .obtainConsent(false)
                .status("template")
                .build();
    }

    /**
     * Get checklist by patient
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return list of checklists for patient
     */
    public List<PreVisitChecklistEntity> getChecklistByPatient(UUID patientId, String tenantId) {
        log.debug("Retrieving checklists for patient {} in tenant {}", patientId, tenantId);

        return checklistRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(
                tenantId, patientId);
    }

    /**
     * Get patient checklist (5a)
     *
     * Retrieves the active (pending or in_progress) checklist for a patient.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID as String
     * @return active checklist for patient
     * @throws ResourceNotFoundException if no active checklist found
     * @throws IllegalArgumentException if patient ID format is invalid
     */
    public PreVisitChecklistEntity getPatientChecklist(String tenantId, String patientId) {
        log.debug("Retrieving active checklist for patient {} in tenant {}", patientId, tenantId);

        UUID pid;
        try {
            pid = UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + patientId);
        }

        List<PreVisitChecklistEntity> checklists = getChecklistByPatient(pid, tenantId);

        return checklists.stream()
                .filter(c -> "pending".equals(c.getStatus()) || "in_progress".equals(c.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active checklist for patient " + patientId, ""));
    }

    /**
     * Add custom item
     *
     * Adds custom checklist item to existing checklist.
     *
     * @param checklistId the checklist ID
     * @param taskName the custom task name
     * @param tenantId the tenant ID
     * @return updated checklist
     */
    @Transactional
    public PreVisitChecklistEntity addCustomItem(
            UUID checklistId, String taskName, String tenantId) {
        log.debug("Adding custom item {} to checklist {} in tenant {}",
                taskName, checklistId, tenantId);

        PreVisitChecklistEntity checklist = checklistRepository
                .findByIdAndTenantId(checklistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found: " + checklistId));

        // Get existing custom items or create new array
        ArrayNode customItems;
        if (checklist.getCustomItems() != null && checklist.getCustomItems().isArray()) {
            customItems = (ArrayNode) checklist.getCustomItems();
        } else {
            customItems = objectMapper.createArrayNode();
        }

        // Add new custom item
        ObjectNode newItem = objectMapper.createObjectNode();
        newItem.put("task", taskName);
        newItem.put("completed", false);
        customItems.add(newItem);

        checklist.setCustomItems(customItems);

        PreVisitChecklistEntity updated = checklistRepository.save(checklist);

        log.info("Custom item {} added to checklist {} in tenant {}",
                taskName, checklistId, tenantId);

        return updated;
    }

    /**
     * Add custom item adapter (5e)
     *
     * Adapter method that extracts displayName from request and adds custom item.
     *
     * @param tenantId the tenant ID
     * @param checklistId the checklist ID
     * @param request the custom checklist item request
     * @param userId the user ID performing the action
     * @return updated checklist entity
     */
    @Transactional
    public PreVisitChecklistEntity addCustomItem(
            String tenantId,
            UUID checklistId,
            CustomChecklistItemRequest request,
            String userId) {
        log.debug("Adding custom item {} to checklist {} in tenant {}",
                request.getDisplayName(), checklistId, tenantId);

        return addCustomItem(checklistId, request.getDisplayName(), tenantId);
    }

    /**
     * Get checklist progress
     *
     * @param checklistId the checklist ID
     * @param tenantId the tenant ID
     * @return checklist progress details
     */
    public ChecklistProgress getChecklistProgress(UUID checklistId, String tenantId) {
        log.debug("Retrieving progress for checklist {} in tenant {}", checklistId, tenantId);

        PreVisitChecklistEntity checklist = checklistRepository
                .findByIdAndTenantId(checklistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found: " + checklistId));

        return ChecklistProgress.builder()
                .checklistId(checklistId)
                .patientId(checklist.getPatientId())
                .appointmentType(checklist.getAppointmentType())
                .status(checklist.getStatus())
                .completionPercentage(checklist.getCompletionPercentage())
                .totalItems(checklist.getTotalItems())
                .completedItems(checklist.getCompletedItems())
                .createdAt(checklist.getCreatedAt())
                .completedAt(checklist.getCompletedAt())
                .build();
    }

    /**
     * Get checklist progress adapter (5f)
     *
     * Adapter method with tenantId first parameter order to match controller signature.
     *
     * @param tenantId the tenant ID
     * @param checklistId the checklist ID
     * @return checklist progress details
     */
    public ChecklistProgress getChecklistProgress(String tenantId, UUID checklistId) {
        log.debug("Retrieving progress for checklist {} in tenant {} (tenantId-first adapter)",
                checklistId, tenantId);

        return getChecklistProgress(checklistId, tenantId);
    }

    /**
     * Get checklist by ID
     *
     * @param checklistId the checklist ID
     * @param tenantId the tenant ID
     * @return checklist
     */
    public PreVisitChecklistEntity getChecklistById(UUID checklistId, String tenantId) {
        log.debug("Retrieving checklist {} in tenant {}", checklistId, tenantId);

        return checklistRepository.findByIdAndTenantId(checklistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found: " + checklistId));
    }

    /**
     * Get checklist by appointment ID
     *
     * @param appointmentId the appointment ID
     * @param tenantId the tenant ID
     * @return checklist
     */
    public PreVisitChecklistEntity getChecklistByAppointmentId(
            String appointmentId, String tenantId) {
        log.debug("Retrieving checklist for appointment {} in tenant {}",
                appointmentId, tenantId);

        return checklistRepository.findChecklistByAppointmentId(appointmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checklist not found for appointment: " + appointmentId));
    }

    /**
     * Get incomplete checklists
     *
     * @param tenantId the tenant ID
     * @return list of incomplete checklists
     */
    @Cacheable(value = "incompleteChecklists", key = "#tenantId")
    public List<PreVisitChecklistEntity> getIncompleteChecklists(String tenantId) {
        log.debug("Retrieving incomplete checklists for tenant {}", tenantId);

        return checklistRepository.findIncompleteChecklistsByTenant(tenantId);
    }

    /**
     * Count incomplete checklists
     *
     * @param tenantId the tenant ID
     * @return count of incomplete checklists
     */
    public long countIncompleteChecklists(String tenantId) {
        return checklistRepository.countIncompleteChecklists(tenantId);
    }

    /**
     * Mark checklist completed
     *
     * @param checklistId the checklist ID
     * @param completedBy staff who completed
     * @param tenantId the tenant ID
     * @return updated checklist
     */
    @Transactional
    public PreVisitChecklistEntity markCompleted(
            UUID checklistId, String completedBy, String tenantId) {
        log.debug("Marking checklist {} completed in tenant {}", checklistId, tenantId);

        PreVisitChecklistEntity checklist = getChecklistById(checklistId, tenantId);
        checklist.setStatus("completed");
        checklist.setCompletedBy(completedBy);
        checklist.setCompletedAt(Instant.now());

        PreVisitChecklistEntity updated = checklistRepository.save(checklist);

        log.info("Checklist {} marked completed in tenant {}", checklistId, tenantId);

        return updated;
    }

    /**
     * Get incomplete critical items (5g)
     *
     * Returns list of required/critical items that are not yet completed.
     *
     * @param tenantId the tenant ID
     * @param checklistId the checklist ID
     * @return list of incomplete critical checklist items
     */
    public List<ChecklistItemResponse> getIncompleteCriticalItems(
            String tenantId, UUID checklistId) {
        log.debug("Retrieving incomplete critical items for checklist {} in tenant {}",
                checklistId, tenantId);

        PreVisitChecklistEntity checklist = getChecklistById(checklistId, tenantId);

        List<ChecklistItemResponse> criticalItems = new ArrayList<>();

        // Add incomplete critical items
        if (!checklist.getReviewMedicalHistory()) {
            criticalItems.add(createItemResponse("Review Medical History", false));
        }
        if (!checklist.getVerifyInsurance()) {
            criticalItems.add(createItemResponse("Verify Insurance", false));
        }
        if (!checklist.getUpdateDemographics()) {
            criticalItems.add(createItemResponse("Update Demographics", false));
        }
        if (!checklist.getReviewMedications()) {
            criticalItems.add(createItemResponse("Review Medications", false));
        }
        if (!checklist.getReviewAllergies()) {
            criticalItems.add(createItemResponse("Review Allergies", false));
        }
        if (!checklist.getPrepareVitalsEquipment()) {
            criticalItems.add(createItemResponse("Prepare Vitals Equipment", false));
        }
        if (!checklist.getReviewCareGaps()) {
            criticalItems.add(createItemResponse("Review Care Gaps", false));
        }
        if (!checklist.getObtainConsent()) {
            criticalItems.add(createItemResponse("Obtain Consent", false));
        }

        log.info("Found {} incomplete critical items for checklist {} in tenant {}",
                criticalItems.size(), checklistId, tenantId);

        return criticalItems;
    }

    /**
     * Create checklist item response
     *
     * Helper method to create ChecklistItemResponse for critical items.
     *
     * @param name the item display name
     * @param completed whether the item is completed
     * @return checklist item response
     */
    private ChecklistItemResponse createItemResponse(String name, boolean completed) {
        return ChecklistItemResponse.builder()
                .name(name)
                .completed(completed)
                .required(true)
                .build();
    }

    /**
     * Checklist Completion Status DTO
     */
    @Data
    @Builder
    public static class ChecklistCompletionStatus {
        private UUID checklistId;
        private String status;
        private BigDecimal completionPercentage;
        private Integer totalItems;
        private Integer completedItems;
    }

    /**
     * Checklist Progress DTO
     */
    @Data
    @Builder
    public static class ChecklistProgress {
        private UUID checklistId;
        private UUID patientId;
        private String appointmentType;
        private String status;
        private BigDecimal completionPercentage;
        private Integer totalItems;
        private Integer completedItems;
        private Instant createdAt;
        private Instant completedAt;
    }

    /**
     * Checklist Item Response DTO
     *
     * Simplified response for critical checklist items.
     */
    @Data
    @Builder
    public static class ChecklistItemResponse {
        private String name;
        private boolean completed;
        private boolean required;
    }
}
