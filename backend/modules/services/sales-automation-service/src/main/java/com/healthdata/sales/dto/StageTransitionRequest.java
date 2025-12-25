package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LostReason;
import com.healthdata.sales.entity.OpportunityStage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for opportunity stage transitions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageTransitionRequest {

    @NotNull(message = "Target stage is required")
    private OpportunityStage targetStage;

    // Optional fields for stage transition
    private String notes;
    private String nextStep;

    // For closed-lost
    private LostReason lostReason;
    private String lostReasonDetail;
    private String competitor;

    // For closed-won
    private BigDecimal finalAmount;
    private LocalDate closeDate;
    private Integer contractLengthMonths;

    // Workflow options
    private Boolean createFollowUpTask;
    private String followUpTaskDescription;
    private Integer followUpDays;
    private Boolean sendNotification;
}
