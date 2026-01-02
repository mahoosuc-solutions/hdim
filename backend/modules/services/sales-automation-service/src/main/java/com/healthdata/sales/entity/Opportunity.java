package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Opportunity entity representing a sales deal
 */
@Entity
@Table(name = "opportunities", indexes = {
    @Index(name = "idx_opportunities_tenant", columnList = "tenant_id"),
    @Index(name = "idx_opportunities_account", columnList = "account_id"),
    @Index(name = "idx_opportunities_stage", columnList = "stage"),
    @Index(name = "idx_opportunities_close_date", columnList = "expected_close_date"),
    @Index(name = "idx_opportunities_zoho", columnList = "zoho_opportunity_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "primary_contact_id")
    private UUID primaryContactId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private OpportunityStage stage;

    @Column(name = "probability")
    private Integer probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lost_reason")
    private LostReason lostReason;

    @Column(name = "lost_reason_detail", length = 500)
    private String lostReasonDetail;

    @Column(name = "competitor", length = 255)
    private String competitor;

    @Column(name = "next_step", length = 500)
    private String nextStep;

    @Column(name = "product_tier", length = 50)
    private String productTier;

    @Column(name = "contract_length_months")
    private Integer contractLengthMonths;

    @Column(name = "zoho_opportunity_id", length = 100)
    private String zohoOpportunityId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculate weighted amount based on probability
     */
    public BigDecimal getWeightedAmount() {
        if (amount == null || probability == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(probability)).divide(BigDecimal.valueOf(100));
    }

    /**
     * Update probability based on stage
     */
    public void updateProbabilityFromStage() {
        if (stage != null) {
            this.probability = stage.getDefaultProbability();
        }
    }
}
