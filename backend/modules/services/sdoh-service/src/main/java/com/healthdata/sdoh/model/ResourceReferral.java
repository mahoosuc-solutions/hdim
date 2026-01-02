package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Patient referral to community resource
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReferral {
    private String referralId;
    private String patientId;
    private String tenantId;
    private String resourceId;
    private SdohCategory category;
    private String referralReason;
    private ReferralStatus status;
    private String referredBy;
    private LocalDateTime referralDate;
    private LocalDateTime contactDate;
    private String outcome;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ReferralStatus {
        PENDING,
        CONTACTED,
        SCHEDULED,
        COMPLETED,
        DECLINED,
        CANCELLED
    }
}
