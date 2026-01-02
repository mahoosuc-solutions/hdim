package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LostReason;
import com.healthdata.sales.entity.OpportunityStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityDTO {

    private UUID id;
    private UUID tenantId;

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    private UUID primaryContactId;

    @NotBlank(message = "Opportunity name is required")
    private String name;

    private String description;
    private BigDecimal amount;
    private OpportunityStage stage;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    private LostReason lostReason;
    private String lostReasonDetail;
    private String competitor;
    private String nextStep;
    private String productTier;
    private Integer contractLengthMonths;
    private String zohoOpportunityId;
    private UUID ownerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private BigDecimal weightedAmount;
}
