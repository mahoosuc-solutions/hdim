package com.healthdata.sdoh.entity;

import com.healthdata.sdoh.model.ResourceReferral;
import com.healthdata.sdoh.model.SdohCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_referrals", schema = "sdoh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReferralEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String referralId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SdohCategory category;

    @Column(columnDefinition = "TEXT")
    private String referralReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceReferral.ReferralStatus status;

    private String referredBy;
    private LocalDateTime referralDate;
    private LocalDateTime contactDate;

    @Column(columnDefinition = "TEXT")
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
