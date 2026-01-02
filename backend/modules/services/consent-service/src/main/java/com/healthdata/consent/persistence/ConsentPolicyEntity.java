package com.healthdata.consent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Consent Policy Entity
 *
 * Stores organization-level consent policies and rules.
 */
@Entity
@Table(name = "consent_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentPolicyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rules", nullable = false, columnDefinition = "TEXT")
    private String rules;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
