package com.healthdata.clinicalworkflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import com.healthdata.clinicalworkflow.domain.repository.PreVisitChecklistRepository;
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
}
