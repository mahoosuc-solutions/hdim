package com.healthdata.quality.service;

import com.healthdata.quality.dto.AddressCareGapRequest;
import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.service.notification.CareGapNotificationTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Care Gap Service
 * Manages care gaps including automatic creation from mental health screenings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapService {

    private final CareGapRepository repository;
    private final CareGapNotificationTrigger notificationTrigger;

    /**
     * Get all care gaps for a patient
     */
    public List<CareGapDTO> getPatientCareGaps(
        String tenantId,
        UUID patientId,
        String status,
        String category
    ) {
        List<CareGapEntity> gaps;

        if (status != null && !status.isBlank()) {
            CareGapEntity.Status statusEnum = CareGapEntity.Status.valueOf(status.toUpperCase());
            gaps = repository.findByTenantIdAndPatientIdAndStatusOrderByDueDateAsc(
                tenantId, patientId, statusEnum
            );
        } else if (category != null && !category.isBlank()) {
            CareGapEntity.GapCategory categoryEnum = CareGapEntity.GapCategory.valueOf(
                category.toUpperCase().replace("-", "_")
            );
            gaps = repository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
                tenantId, patientId, categoryEnum
            );
        } else {
            gaps = repository.findOpenCareGaps(tenantId, patientId);
        }

        return gaps.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Address a care gap
     */
    @Transactional
    public CareGapDTO addressCareGap(
        String tenantId,
        UUID gapId,
        AddressCareGapRequest request
    ) {
        CareGapEntity gap = repository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Care gap not found: " + gapId));

        // Verify tenant ownership
        if (!gap.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Care gap does not belong to tenant");
        }

        // Update status
        CareGapEntity.Status newStatus = CareGapEntity.Status.valueOf(request.getStatus().toUpperCase());
        gap.setStatus(newStatus);
        gap.setAddressedBy(request.getAddressedBy());
        gap.setAddressedNotes(request.getNotes());

        // Set addressed date if moving to addressed or closed
        if (newStatus == CareGapEntity.Status.ADDRESSED || newStatus == CareGapEntity.Status.CLOSED) {
            gap.setAddressedDate(Instant.now());
        }

        gap = repository.save(gap);

        log.info("Care gap {} addressed by {} with status {}", gapId, request.getAddressedBy(), newStatus);

        // Convert to DTO
        CareGapDTO gapDTO = mapToDTO(gap);

        // Trigger notification for addressed care gap
        try {
            notificationTrigger.onCareGapAddressed(tenantId, gapDTO);
        } catch (Exception e) {
            log.error("Failed to trigger care gap addressed notification for gap {}: {}",
                    gapId, e.getMessage(), e);
            // Don't fail the gap addressing if notification fails
        }

        return gapDTO;
    }

    /**
     * Auto-close a care gap based on FHIR resource evidence
     */
    @Transactional
    public void autoCloseCareGap(
        String tenantId,
        UUID gapId,
        String evidenceResourceType,
        String evidenceResourceId,
        String matchingCodes
    ) {
        CareGapEntity gap = repository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Care gap not found: " + gapId));

        // Verify tenant ownership
        if (!gap.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Care gap does not belong to tenant");
        }

        // Skip if already closed
        if (gap.getStatus() == CareGapEntity.Status.CLOSED ||
            gap.getStatus() == CareGapEntity.Status.DISMISSED) {
            log.debug("Care gap {} already closed/dismissed, skipping auto-close", gapId);
            return;
        }

        // Update care gap
        gap.setStatus(CareGapEntity.Status.CLOSED);
        gap.setAutoClosed(true);
        gap.setEvidenceResourceType(evidenceResourceType);
        gap.setEvidenceResourceId(evidenceResourceId);
        gap.setClosedAt(Instant.now());
        gap.setClosedBy("SYSTEM");

        // Update evidence text
        String evidenceText = String.format(
            "Auto-closed by matching FHIR resource: %s/%s (codes: %s) on %s",
            evidenceResourceType,
            evidenceResourceId,
            matchingCodes,
            gap.getClosedAt()
        );
        gap.setEvidence(gap.getEvidence() != null ?
            gap.getEvidence() + "\n" + evidenceText : evidenceText);

        repository.save(gap);

        log.info("Auto-closed care gap {} with evidence from {} {}",
            gapId, evidenceResourceType, evidenceResourceId);
    }

    /**
     * Create a mental health follow-up care gap from a positive screening
     * Called automatically by MentalHealthAssessmentService
     */
    @Transactional
    public void createMentalHealthFollowupGap(
        String tenantId,
        MentalHealthAssessmentEntity assessment
    ) {
        // Check if gap already exists
        String gapType = "mental-health-followup-" + assessment.getType().name().toLowerCase();

        if (repository.existsOpenCareGap(tenantId, assessment.getPatientId(), gapType)) {
            log.info("Mental health follow-up gap already exists for patient {}, type {}",
                assessment.getPatientId(), assessment.getType());
            return;
        }

        // Determine priority based on severity
        CareGapEntity.Priority priority = determinePriority(assessment);

        // Determine due date based on priority
        Instant dueDate = calculateDueDate(priority);

        // Create care gap
        CareGapEntity gap = CareGapEntity.builder()
            .tenantId(tenantId)
            .patientId(assessment.getPatientId())
            .category(CareGapEntity.GapCategory.MENTAL_HEALTH)
            .gapType(gapType)
            .title(buildTitle(assessment))
            .description(buildDescription(assessment))
            .priority(priority)
            .status(CareGapEntity.Status.OPEN)
            .qualityMeasure(getQualityMeasure(assessment))
            .recommendation(buildRecommendation(assessment))
            .evidence(buildEvidence(assessment))
            .dueDate(dueDate)
            .identifiedDate(assessment.getAssessmentDate())
            .build();

        gap = repository.save(gap);

        log.info("Created mental health follow-up care gap for patient {}: {} (Priority: {})",
            assessment.getPatientId(), gap.getTitle(), priority);

        // Trigger notification for identified care gap
        try {
            CareGapDTO gapDTO = mapToDTO(gap);
            notificationTrigger.onCareGapIdentified(tenantId, gapDTO);
        } catch (Exception e) {
            log.error("Failed to trigger care gap identification notification for gap {}: {}",
                    gap.getId(), e.getMessage(), e);
            // Don't fail the gap creation if notification fails
        }
    }

    /**
     * Determine priority based on assessment severity
     */
    private CareGapEntity.Priority determinePriority(MentalHealthAssessmentEntity assessment) {
        String severity = assessment.getSeverity();

        return switch (severity) {
            case "severe", "moderately-severe" -> CareGapEntity.Priority.URGENT;
            case "moderate", "positive" -> CareGapEntity.Priority.HIGH;
            case "mild" -> CareGapEntity.Priority.MEDIUM;
            default -> CareGapEntity.Priority.LOW;
        };
    }

    /**
     * Calculate due date based on priority
     */
    private Instant calculateDueDate(CareGapEntity.Priority priority) {
        return switch (priority) {
            case URGENT -> Instant.now().plus(7, ChronoUnit.DAYS);
            case HIGH -> Instant.now().plus(14, ChronoUnit.DAYS);
            case MEDIUM -> Instant.now().plus(30, ChronoUnit.DAYS);
            case LOW -> Instant.now().plus(90, ChronoUnit.DAYS);
        };
    }

    /**
     * Build gap title
     */
    private String buildTitle(MentalHealthAssessmentEntity assessment) {
        String assessmentName = switch (assessment.getType()) {
            case PHQ_9 -> "PHQ-9";
            case GAD_7 -> "GAD-7";
            case PHQ_2 -> "PHQ-2";
            default -> assessment.getType().name();
        };

        return String.format("%s Positive Screen - Follow-up Required", assessmentName);
    }

    /**
     * Build gap description
     */
    private String buildDescription(MentalHealthAssessmentEntity assessment) {
        return String.format(
            "Patient screened positive on %s with a score of %d/%d (%s). " +
            "Clinical follow-up is required to assess need for treatment or referral.",
            getAssessmentName(assessment.getType()),
            assessment.getScore(),
            assessment.getMaxScore(),
            assessment.getSeverity()
        );
    }

    /**
     * Build recommendation
     */
    private String buildRecommendation(MentalHealthAssessmentEntity assessment) {
        return switch (assessment.getType()) {
            case PHQ_9 -> "1. Clinical interview to confirm diagnosis\n" +
                         "2. Assess suicide risk (especially if Q9 > 0)\n" +
                         "3. Discuss treatment options (therapy, medication, both)\n" +
                         "4. Consider referral to behavioral health specialist";
            case GAD_7 -> "1. Clinical assessment to confirm anxiety disorder\n" +
                         "2. Evaluate functional impairment\n" +
                         "3. Discuss treatment options\n" +
                         "4. Consider referral to behavioral health specialist";
            case PHQ_2 -> "1. Administer full PHQ-9 assessment\n" +
                         "2. If PHQ-9 ≥10, proceed with clinical evaluation\n" +
                         "3. Document results in patient chart";
            default -> "Follow up with clinical assessment";
        };
    }

    /**
     * Build evidence string
     */
    private String buildEvidence(MentalHealthAssessmentEntity assessment) {
        return String.format(
            "%s score: %d/%d (%s) on %s",
            assessment.getType().name(),
            assessment.getScore(),
            assessment.getMaxScore(),
            assessment.getSeverity(),
            assessment.getAssessmentDate()
        );
    }

    /**
     * Get quality measure code
     */
    private String getQualityMeasure(MentalHealthAssessmentEntity assessment) {
        return switch (assessment.getType()) {
            case PHQ_9, PHQ_2 -> "CMS2";  // Depression screening and follow-up
            case GAD_7 -> "CMS2";         // Also qualifies for CMS2
            default -> null;
        };
    }

    /**
     * Get human-readable assessment name
     */
    private String getAssessmentName(MentalHealthAssessmentEntity.AssessmentType type) {
        return switch (type) {
            case PHQ_9 -> "Patient Health Questionnaire-9 (PHQ-9)";
            case GAD_7 -> "Generalized Anxiety Disorder-7 (GAD-7)";
            case PHQ_2 -> "Patient Health Questionnaire-2 (PHQ-2)";
            case PHQ_A -> "PHQ-9 Modified for Adolescents";
            case AUDIT_C -> "Alcohol Use Disorders Identification Test (AUDIT-C)";
            case DAST_10 -> "Drug Abuse Screening Test (DAST-10)";
            case PCL_5 -> "PTSD Checklist (PCL-5)";
            case MDQ -> "Mood Disorder Questionnaire (MDQ)";
            case CAGE_AID -> "CAGE Adapted to Include Drugs (CAGE-AID)";
        };
    }

    /**
     * Map entity to DTO
     */
    private CareGapDTO mapToDTO(CareGapEntity entity) {
        return CareGapDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .category(entity.getCategory().name().toLowerCase().replace("_", "-"))
            .gapType(entity.getGapType())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .priority(entity.getPriority().name().toLowerCase())
            .status(entity.getStatus().name().toLowerCase().replace("_", "-"))
            .qualityMeasure(entity.getQualityMeasure())
            .recommendation(entity.getRecommendation())
            .evidence(entity.getEvidence())
            .dueDate(entity.getDueDate())
            .identifiedDate(entity.getIdentifiedDate())
            .addressedDate(entity.getAddressedDate())
            .addressedBy(entity.getAddressedBy())
            .addressedNotes(entity.getAddressedNotes())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
