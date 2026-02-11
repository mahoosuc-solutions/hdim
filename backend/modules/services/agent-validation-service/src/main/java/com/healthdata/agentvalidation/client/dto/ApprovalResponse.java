package com.healthdata.agentvalidation.client.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for approval requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponse {

    /**
     * Unique ID of the approval request.
     */
    private String id;

    /**
     * Category of the approval.
     */
    private String category;

    /**
     * Type of item being approved.
     */
    private String itemType;

    /**
     * ID of the item requiring approval.
     */
    private String itemId;

    /**
     * Current status (PENDING, APPROVED, REJECTED).
     */
    private String status;

    /**
     * Title of the approval request.
     */
    private String title;

    /**
     * Description of the request.
     */
    private String description;

    /**
     * Risk level.
     */
    private String riskLevel;

    /**
     * User who created the request.
     */
    private String createdBy;

    /**
     * When the request was created.
     */
    private Instant createdAt;

    /**
     * User who approved/rejected.
     */
    private String reviewedBy;

    /**
     * When the review was completed.
     */
    private Instant reviewedAt;

    /**
     * Decision (APPROVED, REJECTED).
     */
    private String decision;

    /**
     * Reviewer's comments.
     */
    private String reviewComments;

    /**
     * Additional context data.
     */
    private Map<String, Object> contextData;
}
