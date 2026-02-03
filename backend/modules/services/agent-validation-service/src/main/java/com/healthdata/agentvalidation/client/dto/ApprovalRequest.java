package com.healthdata.agentvalidation.client.dto;

import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating an approval request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    /**
     * Category of the approval (e.g., "AGENT_VALIDATION").
     */
    @Builder.Default
    private String category = "AGENT_VALIDATION";

    /**
     * Type of item being approved (e.g., "TEST_EXECUTION").
     */
    private String itemType;

    /**
     * ID of the item requiring approval.
     */
    private String itemId;

    /**
     * Title for the approval request.
     */
    private String title;

    /**
     * Description of what needs to be reviewed.
     */
    private String description;

    /**
     * Risk level of the item.
     */
    private String riskLevel;

    /**
     * Reason for flagging for review.
     */
    private String flagReason;

    /**
     * Additional context data.
     */
    private Map<String, Object> contextData;

    /**
     * Required role for approver.
     */
    @Builder.Default
    private String requiredRole = "QUALITY_OFFICER";

    /**
     * Priority of the review (1-5, 1 being highest).
     */
    @Builder.Default
    private int priority = 3;
}
