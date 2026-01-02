package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for converting a lead to contact + opportunity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadConversionRequest {

    @NotBlank(message = "Account name is required")
    private String accountName;

    // If provided, use existing account instead of creating new one
    private UUID existingAccountId;

    @NotBlank(message = "Opportunity name is required")
    private String opportunityName;

    private BigDecimal opportunityAmount;
    private LocalDate expectedCloseDate;
    private String productTier;
    private Integer contractLengthMonths;

    // Optional: Owner assignment
    private UUID ownerUserId;
}
