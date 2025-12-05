package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for acknowledging/acting on a CDS recommendation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsAcknowledgeRequest {

    @NotNull(message = "Recommendation ID is required")
    private UUID recommendationId;

    @NotBlank(message = "Action is required")
    private String action; // ACKNOWLEDGE, ACCEPT, DECLINE, COMPLETE, DEFER, DISMISS

    /**
     * Reason for the action (required for DECLINE, DISMISS)
     */
    private String reason;

    /**
     * Additional notes
     */
    private String notes;

    /**
     * Outcome description (for COMPLETE action)
     */
    private String outcome;

    /**
     * Follow-up date (optional, for DEFER action)
     */
    private Instant followUpDate;

    /**
     * Follow-up notes
     */
    private String followUpNotes;

    /**
     * User performing the action (populated by service if not provided)
     */
    private String userId;
    private String userName;
    private String userRole;
}
