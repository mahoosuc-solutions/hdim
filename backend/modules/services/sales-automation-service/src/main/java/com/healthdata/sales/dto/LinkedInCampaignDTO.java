package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LinkedInCampaign;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for LinkedIn Campaign CRUD operations.
 * Maps to the LinkedInCampaign entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInCampaignDTO {

    private UUID id;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 200, message = "Campaign name must be less than 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    private CampaignStatus status;

    @Size(max = 2000, message = "Target criteria must be less than 2000 characters")
    private String targetCriteria;

    @Min(value = 1, message = "Daily limit must be at least 1")
    @Max(value = 100, message = "Daily limit must not exceed 100")
    private Integer dailyLimit;

    // Read-only metrics
    private Integer totalSent;
    private Integer totalAccepted;
    private Integer totalReplied;
    private Double acceptanceRate;

    // Metadata
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static LinkedInCampaignDTO fromEntity(LinkedInCampaign entity) {
        if (entity == null) return null;

        return LinkedInCampaignDTO.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .targetCriteria(entity.getTargetCriteria())
            .dailyLimit(entity.getDailyLimit())
            .totalSent(entity.getTotalSent())
            .totalAccepted(entity.getTotalAccepted())
            .totalReplied(entity.getTotalReplied())
            .acceptanceRate(entity.getAcceptanceRate())
            .createdBy(entity.getCreatedBy())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    /**
     * Convert DTO to entity (for create/update)
     */
    public LinkedInCampaign toEntity(UUID tenantId) {
        return LinkedInCampaign.builder()
            .id(this.id)
            .tenantId(tenantId)
            .name(this.name)
            .description(this.description)
            .status(this.status != null ? this.status : CampaignStatus.DRAFT)
            .targetCriteria(this.targetCriteria)
            .dailyLimit(this.dailyLimit != null ? this.dailyLimit : 25)
            .build();
    }

    /**
     * Update existing entity with DTO values
     */
    public void updateEntity(LinkedInCampaign entity) {
        if (this.name != null) entity.setName(this.name);
        if (this.description != null) entity.setDescription(this.description);
        if (this.targetCriteria != null) entity.setTargetCriteria(this.targetCriteria);
        if (this.dailyLimit != null) entity.setDailyLimit(this.dailyLimit);
        // Note: status is updated via dedicated activate/pause endpoints
    }
}
