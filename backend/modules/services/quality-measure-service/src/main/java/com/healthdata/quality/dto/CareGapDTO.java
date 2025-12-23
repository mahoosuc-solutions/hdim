package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for care gaps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapDTO {

    /**
     * Care gap ID
     */
    private String id;

    /**
     * Patient FHIR ID
     */
    private UUID patientId;

    /**
     * Gap category (preventive-care, chronic-disease, mental-health, medication, screening)
     */
    private String category;

    /**
     * Gap type (specific gap identifier)
     */
    private String gapType;

    /**
     * Human-readable title
     */
    private String title;

    /**
     * Detailed description
     */
    private String description;

    /**
     * Priority level (urgent, high, medium, low)
     */
    private String priority;

    /**
     * Current status (open, in-progress, addressed, closed, dismissed)
     */
    private String status;

    /**
     * Associated quality measure (CMS2, etc.)
     */
    private String qualityMeasure;

    /**
     * Clinical recommendation
     */
    private String recommendation;

    /**
     * Evidence supporting the gap (e.g., "PHQ-9 score: 15")
     */
    private String evidence;

    /**
     * Due date for addressing the gap
     */
    private Instant dueDate;

    /**
     * Date the gap was identified
     */
    private Instant identifiedDate;

    /**
     * Date the gap was addressed
     */
    private Instant addressedDate;

    /**
     * Provider who addressed the gap
     */
    private String addressedBy;

    /**
     * Notes from provider
     */
    private String addressedNotes;

    /**
     * Date the record was created
     */
    private Instant createdAt;

    /**
     * Date the record was last updated
     */
    private Instant updatedAt;
}
